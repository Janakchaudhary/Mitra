import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// GitHub-hosted runners are ephemeral. Never rely on their generated
// ~/.android/debug.keystore for installable APK artifacts, because a different
// certificate prevents Android from treating a later APK as an update.
//
// For main/dispatch CI builds the workflow restores one persistent keystore and
// exposes it through these environment variables. Local builds without them
// continue to use Android's normal local debug signing key.
val mitraKeystorePath = providers.environmentVariable("MITRA_KEYSTORE_PATH").orNull
val mitraKeystorePassword = providers.environmentVariable("MITRA_KEYSTORE_PASSWORD").orNull
val mitraKeyAlias = providers.environmentVariable("MITRA_KEY_ALIAS").orNull
val mitraKeyPassword = providers.environmentVariable("MITRA_KEY_PASSWORD").orNull

val hasMitraSigning = listOf(
    mitraKeystorePath,
    mitraKeystorePassword,
    mitraKeyAlias,
    mitraKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "com.mitra.learning"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mitra.learning"
        minSdk = 26
        targetSdk = 35
        versionCode = 33
        versionName = "0.18.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (hasMitraSigning) {
            create("mitraPersistent") {
                storeFile = file(mitraKeystorePath!!)
                storePassword = mitraKeystorePassword
                keyAlias = mitraKeyAlias
                keyPassword = mitraKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // CI installable artifacts use the persistent key. Developers who
            // build locally without CI secrets keep the normal debug key.
            if (hasMitraSigning) {
                signingConfig = signingConfigs.getByName("mitraPersistent")
            }
        }

        release {
            isMinifyEnabled = false
            if (hasMitraSigning) {
                signingConfig = signingConfigs.getByName("mitraPersistent")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        // LiteRT-LM and Tesseract both ship the shared C++ runtime for some ABIs.
        // Keep one identical runtime during APK packaging instead of failing on duplicates.
        jniLibs.pickFirsts += "**/libc++_shared.so"
    }
}


kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}


dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.9")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation-layout")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("androidx.datastore:datastore-preferences:1.1.7")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    // Optional parent-imported on-device language model. LiteRT-LM 0.14.0 is
    // compiled with Kotlin 2.3 metadata, so the project Kotlin/Compose plugins
    // are intentionally aligned to Kotlin 2.3.21. Do not suppress metadata
    // checks: a real compiler/library mismatch can otherwise fail at runtime.
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.14.0")

    // Fully offline book preparation: PDF text extraction first, then Gujarati/English OCR
    // for scanned pages. Gujarati and English traineddata are bundled in assets/tessdata.
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("cz.adaptech.tesseract4android:tesseract4android:4.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
