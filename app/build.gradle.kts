plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.xsubtitle"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.xsubtitle"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        // Vosk 的 native 库需要这些 ABI；如只想减小包体，可只保留 arm64-v8a
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }

    // 产物文件名固定为「实时翻译.apk」
    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = "实时翻译.apk"
        }
    }
}

dependencies {
    // 界面
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // 视频播放（Media3 / ExoPlayer）
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")

    // 离线语音识别（Vosk，自带 Kaldi 模型推理，纯本地、无需联网）
    implementation("com.alphacephei:vosk-android:0.3.47")

    // 在线翻译（可选，仅在“翻译模式”开启且联网时用到）
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // 协程，便于后台识别不阻塞 UI
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // lifecycleScope（协程绑定 Activity 生命周期）
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
}
