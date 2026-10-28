import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

// 发布签名配置：密钥库与密码保存在仓库根目录的 keystore.properties（已 gitignore，不入库）。
// 该文件不存在时（例如 CI 刚拉下代码）release 构建仍可产出未签名包，不会直接失败。
val releaseKeystoreProps: Map<String, String>? = run {
  val propsFile = rootProject.file("keystore.properties")
  if (!propsFile.exists()) return@run null
  propsFile.readLines()
    .mapNotNull { line ->
      val trimmed = line.trim()
      if (trimmed.isEmpty() || trimmed.startsWith("#")) return@mapNotNull null
      val separator = trimmed.indexOf('=')
      if (separator <= 0) return@mapNotNull null
      trimmed.substring(0, separator).trim() to trimmed.substring(separator + 1).trim()
    }
    .toMap()
}

android {
  namespace = "com.fzu.wuyu"
  compileSdk = 37
  defaultConfig {
    applicationId = "com.fzu.wuyu"
    minSdk = 24
    targetSdk = 37
    versionCode = 1
    versionName = "1.0"
  }

  signingConfigs {
    releaseKeystoreProps?.let { props ->
      create("release") {
        storeFile = props["storeFile"]?.let { rootProject.file(it) }
        storePassword = props["storePassword"]
        keyAlias = props["keyAlias"]
        keyPassword = props["keyPassword"]
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfigs.findByName("release")?.let { signingConfig = it }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    aidl = false
    buildConfig = false
    shaders = false
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  // 编译工具链用本机安装的 JDK 17（Eclipse Adoptium Temurin 17.0.20）。
  // 它装在非标准路径 D:\Software\Eclipse Adoptium\...，Gradle 自动发现不了，
  // 已在 gradle.properties 中用 org.gradle.java.installations.paths 注册。
  // 注意：若将来升级/重装 JDK 17，目录名中的版本号会变，需同步改那一行。
  jvmToolchain(17)
  compilerOptions {
    jvmTarget = JvmTarget.JVM_17
  }
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Material 图标（core 轻量包：Add/Delete/Edit/ArrowBack/KeyboardArrow*/MoreVert，
  // 不含 extended 大包以控制 APK 体积）
  implementation(libs.androidx.compose.material.icons.core)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Kotlin serialization：本地备份 JSON 的序列化/反序列化
  implementation(libs.kotlinx.serialization.json)

  // Room (local database)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)
}
