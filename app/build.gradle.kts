plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "in.zerotax.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "in.zerotax"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0-AY2026-27"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Restrict release NDK architectures to physical ARM devices (arm64-v8a & armeabi-v7a)
        // Eliminates bulky x86/x86_64 desktop emulator binaries from native SQLCipher (.so) libs,
        // shrinking APK size by ~60% (~18MB to ~7MB).
        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
        }

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/*.kotlin_module",
                "META-INF/*.version",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "/META-INF/{AL2.0,LGPL2.1}",
                "DebugProbesKt.bin"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:tax-engine"))
    implementation(project(":core:security"))
    implementation(project(":core:database"))
    implementation(project(":feature:pdf-unlocker"))
    implementation(project(":feature:ai-extractor"))
    implementation(project(":feature:portal-bridge"))
    implementation(project(":feature:aa-aggregator"))

    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.biometric:biometric-ktx:1.2.0-alpha05")

    // SMS Retriever 1-Tap OTP
    implementation("com.google.android.gms:play-services-auth-api-phone:18.1.0")

    // ML Kit & Document Scanner Vision Engine
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:text-recognition-devanagari:16.0.1")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // CameraX Foundation
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // PDFBox & Background Workers & SSE
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
