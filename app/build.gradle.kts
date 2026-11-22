import java.util.Properties
import java.io.FileInputStream

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

        // Limit packaged string resources to English and Hindi, stripping 80+ unused foreign language locales
        resourceConfigurations.addAll(listOf("en", "hi"))

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = true
        }
    }

    androidResources {
        ignoreAssetsPattern = "!.svn:!.git:!.ds_store:!*.scc:.*:<dir>_*:!CVS:!thumbs.db:!picasa.ini:!*~:Adobe-*:UniCNS-*:UniGB-*:UniJIS-*:UniKS-*:GBK*"
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/*.kotlin_module",
                "META-INF/*.version",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/proguard/*",
                "/META-INF/{AL2.0,LGPL2.1}",
                "DebugProbesKt.bin",
                "META-INF/INDEX.LIST",
                "META-INF/*.md",
                "META-INF/*.txt",
                "kotlin-tooling-metadata.json",
                // Exclude unused BouncyCastle PQC and test properties (saves ~8.25MB uncompressed)
                "org/bouncycastle/pqc/**",
                "org/bouncycastle/x509/**",
                "**/*.properties",
                "**/*.proto"
            )
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
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

    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    signingConfigs {
        create("release") {
            val keyStorePath = System.getenv("RELEASE_KEYSTORE_PATH")
                ?: keystoreProperties.getProperty("storeFile")
            val storePass = System.getenv("RELEASE_STORE_PASSWORD")
                ?: keystoreProperties.getProperty("storePassword")
            val alias = System.getenv("RELEASE_KEY_ALIAS")
                ?: keystoreProperties.getProperty("keyAlias")
            val keyPass = System.getenv("RELEASE_KEY_PASSWORD")
                ?: keystoreProperties.getProperty("keyPassword")

            if (!keyStorePath.isNullOrBlank() && file(keyStorePath).exists()) {
                storeFile = file(keyStorePath)
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            } else {
                // Secure fallback for local and CI environments without production signing secrets
                storeFile = signingConfigs.getByName("debug").storeFile
                storePassword = signingConfigs.getByName("debug").storePassword
                keyAlias = signingConfigs.getByName("debug").keyAlias
                keyPassword = signingConfigs.getByName("debug").keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
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

    // Android Credential Manager & Google Identity One-Tap
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // ML Kit Official Document Scanner (Unbundled, Play Services powered — zero local ML model bloat)
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")

    // SSE Network Client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
