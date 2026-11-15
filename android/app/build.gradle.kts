plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val apiBase = providers.gradleProperty("yeopjari.apiBase").getOrElse("http://10.0.2.2:8080")
val usePleosSdk = providers.gradleProperty("yeopjari.usePleosSdk").getOrElse("false").toBoolean()
val pleosDeps = providers.gradleProperty("yeopjari.pleosDeps").getOrElse("")
    .split(",").map { it.trim() }.filter { it.isNotEmpty() }

android {
    namespace = "com.yeopjari.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yeopjari.app"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "API_BASE", "\"$apiBase\"")
        buildConfigField("boolean", "USE_PLEOS_SDK", usePleosSdk.toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 대회 제출용: 별도 키 없이 바로 설치되도록 디버그 키로 서명한다 (스토어 배포 전에는 릴리스 키로 바꾼다)
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core"))

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Pleos Connect SDK (Vehicle, NaviHelper). gradle.properties 의 yeopjari.pleosDeps 로 넣는다.
    pleosDeps.forEach { implementation(it) }
}
