plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
}

android {
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.ToDoo"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true // Включаем поддержку Compose
    }

    namespace = "com.example.ToDoo"
}


dependencies {
    // Jetpack Compose dependencies
    implementation("androidx.compose.ui:ui:1.4.0")
    implementation("androidx.compose.material3:material3:1.1.0")  // Зависимость для Material3
    implementation("androidx.compose.ui:ui-tooling-preview:1.4.0")

    // Core AndroidX libraries
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.0.1")

    // OkHttp for network calls
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    // Gson for working with JSON
    implementation("com.google.code.gson:gson:2.8.8")

    // Coroutines for background tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")

    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

