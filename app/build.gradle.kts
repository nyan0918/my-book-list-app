import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.mybooksapplication"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.mybooksapplication"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val propertiesFile = project.rootProject.file("local.properties")
        val properties = Properties().apply{
            load(FileInputStream(propertiesFile))
        }

        buildConfigField ("String", "BOOKS_API_KEY", "\"${properties["BOOKS_API_KEY"]}\"")
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Navigation Compose と Hilt の連携用
    implementation(libs.androidx.hilt.navigation.compose)

    // Lifecycle ViewModel Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Image Loading
    implementation(libs.coil.compose)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // ML Kit (Barcode)
    implementation(libs.barcode.scanning)

    // Retrofit / Moshi (API)
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.logging.interceptor)

    // Room (DB)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Test dependencies
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("app.cash.turbine:turbine:0.12.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
}

tasks.register<JacocoReport>("jacocoTestReport") {
    // 単体テストタスクに依存させる（テスト実行 -> レポート作成の流れを作る）
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)   // CIツール用
        html.required.set(true)  // ブラウザ確認用
    }

    // 【重要】カバレッジ計測から除外するファイル（Kotlin/Android特有の生成ファイル）
    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        // DataBinding / ViewBinding
        "**/databinding/*",
        "**/generated/callback/*",
        "**/*Binding.class",
        "**/*BindingImpl.class",
        // Dagger / Hilt (DIを使っている場合)
        "**/Dagger*.*",
        "**/*_Factory.*",
        "**/*_MembersInjector.*",
        "**/*_HiltModules*.*",
        // Kotlinの生成クラス対策
        "**/*\$Lambda$*.*", // ラムダ式
        "**/*\$inlined$*.*" // インライン関数
    )

    // コンパイルされたクラスファイルの場所
    // Kotlinのクラスファイルは "tmp/kotlin-classes/debug" に出力されることが多いです
    val debugTree = fileTree(
        mapOf(
            "dir" to layout.buildDirectory.dir("tmp/kotlin-classes/debug").get().asFile,
            "excludes" to fileFilter
        )
    )
    val javaDebugTree = fileTree(
        mapOf(
            "dir" to layout.buildDirectory.dir("intermediates/javac/debug/classes").get().asFile,
            "excludes" to fileFilter
        )
    )

    // ソースコードの場所
    val mainSrc = layout.projectDirectory.dir("src/main/java").asFile

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree, javaDebugTree))

    // テスト実行結果データ(.exec)の場所
    executionData.setFrom(
        fileTree(
            mapOf(
                "dir" to layout.buildDirectory.asFile,
                "includes" to listOf("**/*.exec", "**/*.ec")
            )
        )
    )
}