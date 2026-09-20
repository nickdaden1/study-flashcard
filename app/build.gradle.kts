plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.study.flashcard"
    // compileSdk 35 do core-ktx 1.15.0 yêu cầu; targetSdk giữ 34 (hành vi runtime không đổi)
    compileSdk = 35

    defaultConfig {
        applicationId = "com.study.flashcard"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        // Debug chỉ build arm64 để APK nhẹ (máy test + Test Lab đều arm64)
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        // Bật R8 + shrink ngay từ Phase 1 để đo size thật
        // (POI/PdfBox ở Phase 2 sẽ làm APK phình — theo dõi từ sớm)
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/*.kotlin_module",
                // Apache POI mang file ký số trùng nhau -> loại để tránh DuplicateFileException
                "META-INF/*.RSA",
                "META-INF/*.SF",
                "META-INF/*.DSA"
            )
        }
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)

    // Phase 2: data layer
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.security.crypto)
    implementation(libs.pdfbox.android)
    implementation(libs.poi.ooxml) {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    implementation(libs.kx.serialization.json)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Phase 3: AI layer (Gemini REST qua Retrofit + OkHttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kx.converter)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
