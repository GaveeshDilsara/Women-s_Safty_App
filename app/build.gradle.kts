plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.home"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.home"
        minSdk = 26
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.google.android.gms:play-services-maps:18.0.2")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.karumi:dexter:6.2.3")
    implementation("com.google.android.libraries.places:places:2.6.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // CameraX core library
    implementation("androidx.camera:camera-core:1.2.2")
    implementation("androidx.camera:camera-camera2:1.2.2")

    // CameraX Lifecycle library
    implementation("androidx.camera:camera-lifecycle:1.2.2")

    // CameraX Video recording library
    implementation("androidx.camera:camera-video:1.2.2")

    // CameraX View library
    implementation("androidx.camera:camera-view:1.3.0")

    // Optionally, CameraX Extensions library for effects like HDR
    implementation("androidx.camera:camera-extensions:1.3.0")

    // Kotlin script runtime
    implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.9.10")
}
