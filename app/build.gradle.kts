plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// CI passes the GitHub Actions run number so each published build gets a
// distinct, monotonically increasing versionCode/versionName. Locally this
// just falls back to 1 / "1.0-dev".
val ciBuildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "").toIntOrNull()

android {
    namespace = "it.mattia.glucoseglyph"
    compileSdk = 35

    defaultConfig {
        applicationId = "it.mattia.glucoseglyph"
        minSdk = 34
        targetSdk = 35
        versionCode = ciBuildNumber ?: 1
        versionName = ciBuildNumber?.let { "1.0.$it" } ?: "1.0-dev"
    }

    signingConfigs {
        // A fixed, repo-committed debug keystore (NOT the per-machine one Android
        // Studio/Gradle auto-generates). Every CI run otherwise gets its own random
        // debug key, so each downloaded APK has a different signature and Android
        // refuses to install it over the previous one ("signatures don't match").
        // This is a debug-only key with the standard well-known debug password; it
        // must never be used to sign a release build.
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
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
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(files("libs/glyphsdk_0606.aar"))

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-service:2.9.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(platform("androidx.compose:compose-bom:2025.06.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
