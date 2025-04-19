package com.example.ToDoo

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import okhttp3.MediaType.Companion.toMediaType

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private val CHANNEL_ID = "todo_channel"
    private val geminiApiKey = "API" // ← вставь свой ключ!
    private lateinit var textToSpeech: TextToSpeech
    private val notificationId = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textToSpeech = TextToSpeech(this, this)

        requestPermissionsIfNeeded()
        registerReceiver(notificationReceiver, IntentFilter("ACTION_TEXT"))
        registerReceiver(notificationReceiver, IntentFilter("ACTION_VOICE"))
        showPersistentNotification()

        val taskInput: EditText = findViewById(R.id.taskInput)
        val btnAddTask: Button = findViewById(R.id.btnAddTask)
        val btnVoiceInput: Button = findViewById(R.id.btnVoiceInput)

        btnAddTask.setOnClickListener {
            val text = taskInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendToGemini(text)
                taskInput.text.clear()
            } else {
                speak("Пожалуйста, введите задачу")
            }
        }

        btnVoiceInput.setOnClickListener {
            startVoiceRecognition()
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_CALENDAR)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 101)
        }
    }

    private fun showPersistentNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ToDoo Задачи",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val textIntent = Intent("ACTION_TEXT")
        val textPending = PendingIntent.getBroadcast(
            this, 0, textIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val voiceIntent = Intent("ACTION_VOICE")
        val voicePending = PendingIntent.getBroadcast(
            this, 1, voiceIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("ToDoo")
                .setContentText("Добавьте новую задачу")
                .setOngoing(true)
                .addAction(R.drawable.ic_edit, "Текстом", textPending)
                .addAction(R.drawable.ic_mic, "Голосом", voicePending)
                .build()

            NotificationManagerCompat.from(this).notify(notificationId, notification)
        }
    }

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "ACTION_TEXT" -> showTextInputDialog()
                "ACTION_VOICE" -> startVoiceRecognition()
            }
        }
    }

    private fun showTextInputDialog() {
        val input = EditText(this)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Введите задачу")
            .setView(input)
            .setPositiveButton("Отправить") { _, _ ->
                val task = input.text.toString().trim()
                if (task.isNotEmpty()) sendToGemini(task)
            }
            .setNegativeButton("Отмена", null)
            .create()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        }

        dialog.show()
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            result?.let { sendToGemini(it) }
        }
    }

    private fun sendToGemini(taskText: String) {
        val client = OkHttpClient()

        val json = JSONObject().apply {
            val contents = JSONArray().apply {
                put(
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", taskText)))
                )
            }
            put("contents", contents)
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$geminiApiKey")
            .post(RequestBody.create("application/json".toMediaType(), json.toString()))
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { speak("Ошибка подключения к Gemini") }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val responseText = JSONObject(body ?: "{}")
                    .optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text", null)

                if (!responseText.isNullOrEmpty()) {
                    runOnUiThread {
                        speak(responseText)
                        extractDateFromText(responseText)?.let { date ->
                            addTaskToCalendar(responseText, date)
                        }
                    }
                } else {
                    runOnUiThread {
                        speak("Ответ от Gemini пустой")
                    }
                }
            }
        })
    }

    private fun extractDateFromText(text: String): Date? {
        val regex =
            "\\d{1,2}\\s(января|февраля|марта|апреля|мая|июня|июля|августа|сентября|октября|ноября|декабря)\\s\\d{4}"
        val matcher = Pattern.compile(regex).matcher(text)
        return if (matcher.find()) {
            val dateStr = matcher.group(0)
            val format = SimpleDateFormat("d MMMM yyyy", Locale("ru", "RU"))
            format.parse(dateStr)
        } else null
    }

    private fun addTaskToCalendar(description: String, date: Date) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, date.time)
                put(CalendarContract.Events.DTEND, date.time + 3600000)
                put(CalendarContract.Events.TITLE, description)
                put(CalendarContract.Events.DESCRIPTION, "Создано через ToDoo")
                put(CalendarContract.Events.CALENDAR_ID, 1)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }

            val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            if (uri != null) speak("Задача добавлена в календарь")
            else speak("Ошибка при добавлении задачи в календарь")
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_CALENDAR), 102)
            speak("Нужно разрешение для календаря")
        }
    }

    private fun speak(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale("ru", "RU"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "TTS: Язык не поддерживается", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "TTS: Ошибка инициализации", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        textToSpeech.shutdown()
        unregisterReceiver(notificationReceiver)
        super.onDestroy()
    }
}
