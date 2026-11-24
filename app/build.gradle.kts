plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val remoteVersion = "1.0.0-alpha24"

android {
    namespace = "com.zivkesten.remotelab"
    compileSdk { version = release(37) { minorApiLevel = 1 } }

    defaultConfig {
        applicationId = "com.zivkesten.remotelab"
        minSdk = 29 // remote-creation-compose requires 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    lint { abortOnError = false }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.11.01"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Remote Compose: creation side (writes documents) and player side (renders them)
    implementation("androidx.compose.remote:remote-core:$remoteVersion")
    implementation("androidx.compose.remote:remote-creation:$remoteVersion")
    implementation("androidx.compose.remote:remote-creation-core:$remoteVersion")
    implementation("androidx.compose.remote:remote-creation-android:$remoteVersion")
    implementation("androidx.compose.remote:remote-creation-compose:$remoteVersion")
    implementation("androidx.compose.remote:remote-player-core:$remoteVersion")
    implementation("androidx.compose.remote:remote-player-view:$remoteVersion")
}
