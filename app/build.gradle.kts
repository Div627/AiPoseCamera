import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// User-authorized beta credentials stay outside source control.
val betaCredentials = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun betaKey(name: String): String {
    if (!providers.gradleProperty("privateBetaCredentials").map { it.toBoolean() }.getOrElse(false)) return "\"\""
    val key = betaCredentials.getProperty(name, "").trim()
    require(key.isNotBlank()) { "Missing beta model credential: $name" }
    return "\"" + key.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""
}
android {
    namespace = "com.aipose.camera"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aipose.camera"
        minSdk = 26
        targetSdk = 34
        versionCode = 24
        versionName = "0.1.0-beta.11"
        ndk { abiFilters += listOf("arm64-v8a") }

        buildConfigField("String", "DEEPSEEK_KEY", betaKey("DEEPSEEK_API_KEY"))
        buildConfigField("String", "QWEN_KEY", betaKey("QWEN_API_KEY"))
    }

    buildTypes {
        release {
            // Preserve the installed beta signing identity while delivering optimized code.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        buildConfig = true
    }
    androidResources { noCompress += listOf("task", "tflite") }
    packaging {
        resources.excludes += "META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // CameraX
    val camerax = "1.3.4"
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")
    implementation("androidx.camera:camera-video:$camerax")

    implementation("androidx.media3:media3-transformer:1.5.1")
    implementation("androidx.media3:media3-effect:1.5.1")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("org.commonmark:commonmark:0.24.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.24.0")

    // MediaPipe Pose Landmarker（端上姿态检测）
    implementation("com.google.mediapipe:tasks-vision:0.10.32") {
        // Annotation processors are build-time tools, not Android runtime dependencies.
        exclude(group = "com.google.auto.value", module = "auto-value")
    }

    // ML Kit 条码扫描（扫码更新）
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    // LLM（OpenAI 兼容协议：DeepSeek / 通义千问）
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
