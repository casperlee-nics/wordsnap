plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.wordsnap.app"
    compileSdk = 36

    defaultConfig {
        // Play 上架用的套件名稱（上架後不能改）；程式碼的 namespace 維持 com.wordsnap.app
        applicationId = "com.makeitfunstudio.wordsnap"
        minSdk = 26
        targetSdk = 36
        versionCode = 58
        versionName = "1.0.1"
        // 測試存檔（9 職業 LV60、道具各 9 個）：上架版關掉，debug 和 releaseTest 打開
        buildConfigField("boolean", "TEST_SAVES", "false")
    }

    buildTypes {
        // 按 ▶ 的除錯版也有測試存檔
        debug {
            buildConfigField("boolean", "TEST_SAVES", "true")
        }
        release {
            // R8：把用不到的程式碼和資源拿掉、程式碼最佳化（dex 程式碼最佳化），省記憶體也跑得比較快
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        // 上架前自己試 R8 版用：跟 release 一樣有最佳化，但用除錯金鑰簽名，可以直接按 ▶ 裝到手機
        create("releaseTest") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
            buildConfigField("boolean", "TEST_SAVES", "true")
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
    // 字典檔不壓縮：App 可以直接知道檔案大小，安裝字典時顯示進度
    androidResources {
        noCompress += "db"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // 本機資料庫：單字存在手機裡
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // 拍照辨識文字：ML Kit 中文模型（中英文都認得，模型內建在 App 裡，離線可用）
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")

    // 英翻中：ML Kit 離線翻譯（第一次使用時下載約 30 MB 的翻譯檔）
    implementation("com.google.mlkit:translate:17.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // 雲端存檔：Google Play 遊戲的「已儲存的遊戲」
    implementation("com.google.android.gms:play-services-games-v2:20.1.2")

    // 每日複習提醒
    implementation("androidx.work:work-runtime-ktx:2.10.0")
}
