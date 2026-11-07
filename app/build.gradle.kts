plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ar.com.laclave.cinetv"
    compileSdk = 35
    defaultConfig {
        applicationId = "ar.com.laclave.cinetv"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    buildTypes {
        release { isMinifyEnabled = false }
        debug { applicationIdSuffix = ".debug" }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(platform("androidx.compose:compose-bom:2025.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.media3:media3-exoplayer:1.6.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.6.1")
    implementation("androidx.media3:media3-ui:1.6.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.6.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.datastore:datastore-preferences:1.1.4")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
