plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
}

android {
  namespace = "com.example.traccarcam"
  compileSdk = 34

  defaultConfig {
    applicationId = "com.example.traccarcam"
    minSdk = 24
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"
  }

  buildFeatures {
    viewBinding = true
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

dependencies {
  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.appcompat:appcompat:1.7.0")
  implementation("com.google.android.material:material:1.12.0")
  implementation("androidx.activity:activity-ktx:1.9.2")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

  // Location
  implementation("com.google.android.gms:play-services-location:21.3.0")
  // HTTP
  implementation("com.squareup.okhttp3:okhttp:4.12.0")

  // RTMP camera (legacy rtplibrary API that includes RtmpCamera2 + OpenGlView)
  implementation("com.github.pedroSG94.RootEncoder:rtplibrary:2.2.6")
}
