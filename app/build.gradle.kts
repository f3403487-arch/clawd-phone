plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.clawd.phone"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.clawd.phone"
        minSdk = 29
        targetSdk = 34
        versionCode = 16
        versionName = "4.5"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
