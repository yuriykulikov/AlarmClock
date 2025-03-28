plugins {
  id("com.android.application")
  kotlin("android")
  jacoco
  kotlin("plugin.serialization")
}

jacoco { toolVersion = "0.8.8" }

// ./gradlew test connectedDevelopDebugAndroidTest jacocoTestReport
// task must be created, examples in Kotlin which call tasks.jacocoTestReport do not work
tasks.create("jacocoTestReport", JacocoReport::class.java) {
  group = "Reporting"
  description = "Generate Jacoco coverage reports."

  reports {
    xml.required.set(true)
    html.required.set(true)
  }

  val fileFilter =
      listOf(
          "**/R.class",
          "**/R$*.class",
          "**/BuildConfig.*",
          "**/Manifest*.*",
          "**/*Test*.*",
          "android/**/*.*",
          "**/*\$\$serializer.class",
      )

  val developDebug = "developDebug"

  sourceDirectories.setFrom(
      files(listOf("$projectDir/src/main/java", "$projectDir/src/main/kotlin")))

  classDirectories.setFrom(
      files(
          listOf(
              fileTree("$buildDir/intermediates/javac/$developDebug") { exclude(fileFilter) },
              fileTree("$buildDir/tmp/kotlin-classes/$developDebug") { exclude(fileFilter) },
          )))

  // execution data from both unit and instrumentation tests
  executionData.setFrom(
      fileTree(project.buildDir) {
        include(
            // unit tests
            "jacoco/test${"developDebug".capitalize()}UnitTest.exec",
            // instrumentation tests
            "outputs/code_coverage/${developDebug}AndroidTest/connected/**/*.ec",
        )
      })

  // dependsOn("test${"developDebug".capitalize()}UnitTest")
  // dependsOn("connected${"developDebug".capitalize()}AndroidTest")
}

tasks.withType(Test::class.java) {
  (this.extensions.getByName("jacoco") as JacocoTaskExtension).apply {
    isIncludeNoLocationClasses = true
    excludes = listOf("jdk.internal.*")
  }

  systemProperty("org.slf4j.simpleLogger.logFile", "System.out")
  systemProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace")
}

val acraEmail =
    project.rootProject
        .file("local.properties")
        .let { if (it.exists()) it.readLines() else emptyList() }
        .firstOrNull { it.startsWith("acra.email") }
        ?.substringAfter("=")
        ?: System.getenv()["ACRA_EMAIL"] ?: ""

@Suppress("UnstableApiUsage")
android {
  compileSdk = 33
  defaultConfig {
    versionCode = 31601
    versionName = "3.16.01"
    applicationId = "com.better.alarm"
    minSdk = 21
    targetSdk = 33
    testApplicationId = "com.better.alarm.test"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    multiDexEnabled = true
  }
  namespace = "com.better.alarm"
  testNamespace = "com.better.alarm.debug"
  signingConfigs {
    create("release") {
      storeFile = file("upload-keystore.jks")
      storePassword = System.getenv("UPLOAD_KEYSTORE_PASSWORD")
      keyAlias = System.getenv("UPLOAD_KEY_ALIAS")
      keyPassword = System.getenv("UPLOAD_KEY_PASSWORD")
    }
  }
  buildTypes {
    getByName("debug") {
      enableUnitTestCoverage = true
      enableAndroidTestCoverage = true
      applicationIdSuffix = ".debug"
      buildConfigField("String", "ACRA_EMAIL", "\"$acraEmail\"")
    }
    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.txt")
      buildConfigField("String", "ACRA_EMAIL", "\"$acraEmail\"")
      getByName("release") { signingConfig = signingConfigs.getByName("release") }
    }
  }

  flavorDimensions.add("default")

  productFlavors {
    create("develop") { applicationId = "com.better.alarm" }
    create("premium") { applicationId = "com.premium.alarm" }
  }

  installation {
    timeOutInMs = 20 * 60 * 1000 // 20 minutes
    installOptions("-d", "-t")
  }

  useLibrary("android.test.runner")
  useLibrary("android.test.base")
  useLibrary("android.test.mock")

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  testOptions { unitTests.isReturnDefaultValues = true }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  kotlinOptions {
    freeCompilerArgs =
        freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn" + "-opt-in=kotlin.Experimental"

    jvmTarget = "1.8"
  }
}

dependencies {
  val coroutinesVersion = "1.7.3"
  val serializationVersion = "1.6.2"
  implementation("ch.acra:acra-mail:5.12.0")
  implementation("com.melnykov:floatingactionbutton:1.3.0")
  implementation("io.reactivex.rxjava2:rxjava:2.2.21")
  implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:1.7.3")
  implementation("io.insert-koin:koin-android:3.5.3")
  implementation("androidx.fragment:fragment:1.6.2")
  // TODO remove this when we don't use it anymore
  implementation("androidx.preference:preference:1.2.1")
  // resolves duplicate class caused by androidx.preference:preference:1.2.0
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
  // implementation("androidx.lifecycle:lifecycle-viewmodel:2.5.1")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:$coroutinesVersion")
  implementation("com.google.android.material:material:1.8.0")
  implementation("org.slf4j:slf4j-api:1.7.36")
  implementation("com.github.tony19:logback-android:3.0.0")
  implementation("androidx.multidex:multidex:2.0.1")
  implementation("androidx.datastore:datastore:1.0.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$serializationVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

  testImplementation("net.wuerl.kotlin:assertj-core-kotlin:0.2.1")
  testImplementation("junit:junit:4.13.2")
  testImplementation("io.mockk:mockk:1.13.17")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
  testImplementation("org.slf4j:slf4j-simple:2.0.17")

  val androidxTest = "1.6.1"
  androidTestImplementation("com.squareup.assertj:assertj-android:1.2.0")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
  androidTestImplementation("androidx.test:runner:$androidxTest")
  androidTestImplementation("androidx.test:rules:$androidxTest")
  androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
