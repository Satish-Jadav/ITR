plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "in.zerotax.feature.aiextractor"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:tax-engine"))
    implementation(project(":core:security"))

    compileOnly("com.google.ai.edge.aicore:aicore:1.0.0-alpha01")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
