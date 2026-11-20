import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(name: String): String = System.getenv(name) ?: localProps.getProperty(name) ?: ""

// ---- Penandatanganan rilis --------------------------------------------------
// Diisi dari env var (CI) atau local.properties (lokal). Lihat README "Signing rilis".
val releaseKeystorePath = secret("VELORIS_KEYSTORE_PATH")
val releaseStorePassword = secret("VELORIS_KEYSTORE_PASSWORD")
val releaseKeyAlias = secret("VELORIS_KEY_ALIAS")
val releaseKeyPassword = secret("VELORIS_KEY_PASSWORD")
val releaseKeystoreFile: File? = releaseKeystorePath.takeIf { it.isNotBlank() }?.let { rootProject.file(it) }
val releaseSigningReady = releaseKeystoreFile?.isFile == true &&
    releaseStorePassword.isNotBlank() && releaseKeyAlias.isNotBlank() && releaseKeyPassword.isNotBlank()

// Di CI, build rilis TIDAK boleh diam-diam ditandatangani debug key.
val isCi = System.getenv("CI") == "true"
val requestsRelease = gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }
if (requestsRelease && !releaseSigningReady) {
    if (isCi) {
        throw GradleException("Signing rilis belum lengkap di CI (VELORIS_KEYSTORE_PATH / PASSWORD / KEY_ALIAS / KEY_PASSWORD).")
    }
    logger.warn("⚠ Build rilis lokal memakai debug key: signing rilis belum dikonfigurasi.")
}

android {
    namespace = "com.veloris.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.veloris.app"
        minSdk = 26
        targetSdk = 35
        // Di CI diisi nomor run (selalu naik). Lokal: 1.
        versionCode = secret("VELORIS_VERSION_CODE").toIntOrNull() ?: 1
        versionName = "2.0.0"
        buildConfigField("String", "YT_API_KEY", "\"${secret("YT_API_KEY")}\"")
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"${secret("GOOGLE_CLIENT_ID")}\"")
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"${secret("SPOTIFY_CLIENT_ID")}\"")
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                // Skema tanda tangan APK: v1 (JAR) untuk kompatibilitas lama,
                // v2 untuk Android 7+, v3 untuk rotasi kunci.
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(bom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
}
