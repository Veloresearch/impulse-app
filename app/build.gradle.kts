import com.android.build.api.variant.FilterConfiguration
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val appVersion = "0.2.1"

/** 0.2.0 is 200, 1.4.12 is 10412; each ABI build adds its digit in the millions on top. */
val versionBase = appVersion.split('.').fold(0) { code, part -> code * 100 + part.toInt() }
val abiDigits = mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2, "x86" to 3, "x86_64" to 4)

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}

fun secret(key: String): String? = System.getenv(key)?.takeIf { it.isNotBlank() } ?: localProperties.getProperty(key)?.takeIf { it.isNotBlank() }

android {
    namespace = "com.velocity.impulse"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.velocity.impulse"
        minSdk = 26
        targetSdk = 35
        versionCode = versionBase
        versionName = appVersion
    }

    signingConfigs {
        val store = secret("IMPULSE_KEYSTORE")
        if (store != null) {
            create("release") {
                storeFile = file(store)
                storePassword = secret("IMPULSE_KEYSTORE_PASSWORD")
                keyAlias = secret("IMPULSE_KEY_ALIAS")
                keyPassword = secret("IMPULSE_KEY_PASSWORD")
            }
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include(*abiDigits.keys.toTypedArray())
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            // Without the IMPULSE_* keys a release build is signed with the debug key and cannot update a published install.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val abi = output.filters.find { it.filterType == FilterConfiguration.FilterType.ABI }?.identifier ?: return@forEach
            abiDigits[abi]?.let { output.versionCode.set(it * 1_000_000 + versionBase) }
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("dev.chrisbanes.haze:haze:1.0.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("androidx.camera:camera-core:1.4.0")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")
    implementation("com.google.zxing:core:3.5.3")
}
