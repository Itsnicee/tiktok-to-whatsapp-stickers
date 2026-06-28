plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.thenicebott.tiktokstickers"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.thenicebott.tiktokstickers"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Usado en AndroidManifest.xml vía manifest placeholder para que
        // el authority del ContentProvider quede atado al applicationId
        // sin tener que escribirlo repetido en dos lugares.
        manifestPlaceholders["contentProviderAuthority"] = "$applicationId.stickercontentprovider"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Conversión y manejo de imágenes (WebP, recorte, redimensionado).
    // El soporte WebP de Android es nativo desde API 14+ para escribir
    // estáticos vía android.graphics.Bitmap.CompressFormat.WEBP_LOSSY,
    // pero Android NO soporta escribir WebP ANIMADO de forma nativa.
    // webp-android (bindings JNI a libwebp real) cubre ese hueco: permite
    // decodificar el .awebp que entrega TikTok frame por frame, y
    // re-codificar ya recortado a 512x512 como WebP animado válido para
    // WhatsApp (que exige el formato VP8X/ANIM, no cualquier WebP animado).
    implementation("com.aureusapps.android:webp-android:1.1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
