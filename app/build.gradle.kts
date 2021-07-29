plugins {
    id("com.android.application")
    kotlin("android")
    jacoco
}

jacoco {
    toolVersion = "0.8.3"
}

// ./gradlew test connectedDevelopDebugAndroidTest jacocoTestReport
// task must be created, examples in Kotlin which call tasks.jacocoTestReport do not work
tasks.create("jacocoTestReport", JacocoReport::class.java) {
    group = "Reporting"
    description = "Generate Jacoco coverage reports."

    reports {
        xml.isEnabled = true
        html.isEnabled = true
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*"
    )

    val developDebug = "developDebug"

    sourceDirectories.setFrom(
        files(
            listOf(
                "$projectDir/src/main/java",
                "$projectDir/src/main/kotlin"
            )
        )
    )
    classDirectories.setFrom(
        files(
            listOf(
                fileTree(
                    "dir" to "$buildDir/intermediates/javac/$developDebug",
                    "excludes" to fileFilter
                ),
                fileTree(
                    "dir" to "$buildDir/tmp/kotlin-classes/$developDebug",
                    "excludes" to fileFilter
                )
            )
        )
    )

    // execution data from both unit and instrumentation tests
    executionData.setFrom(
        fileTree(
            "dir" to project.buildDir,
            "includes" to listOf(
                // unit tests
                "jacoco/test${"developDebug".capitalize()}UnitTest.exec",
                // instrumentation tests
                "outputs/code_coverage/${developDebug}AndroidTest/connected/**/*.ec"
            )
        )
    )

    // dependsOn("test${"developDebug".capitalize()}UnitTest")
    // dependsOn("connected${"developDebug".capitalize()}AndroidTest")
}

tasks.withType(Test::class.java) {
    (this.extensions.getByName("jacoco") as JacocoTaskExtension).apply {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

val acraEmail = project.rootProject.file("local.properties")
    .let { if (it.exists()) it.readLines() else emptyList() }
    .firstOrNull { it.startsWith("acra.email") }
    ?.substringAfter("=")
    ?: System.getenv()["ACRA_EMAIL"]
    ?: ""

android {
    compileSdkVersion(29)
    defaultConfig {
        versionCode = 30902
        versionName = "3.09.02"
        applicationId = "com.better.alarm"
        minSdkVersion(16)
        targetSdkVersion(29)
        testApplicationId = "com.better.alarm.test"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }
    buildTypes {
        getByName("debug") {
            isTestCoverageEnabled = true
            buildConfigField("String", "ACRA_EMAIL", "\"$acraEmail\"")
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.txt")
            buildConfigField("String", "ACRA_EMAIL", "\"$acraEmail\"")
        }
    }
    flavorDimensions("default")
    productFlavors {
        create("develop") {
            applicationId = "com.better.alarm"
        }
        create("premium") {
            applicationId = "com.premium.alarm"
        }
    }

    lintOptions {
        isAbortOnError = false
        isCheckReleaseBuilds = false
    }

    adbOptions {
        timeOutInMs = 20 * 60 * 1000  // 20 minutes
        installOptions("-d", "-t")
    }

    dexOptions {
        preDexLibraries = System.getenv("TRAVIS") != "true"
    }

    useLibrary("android.test.runner")
    useLibrary("android.test.base")
    useLibrary("android.test.mock")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        useIR = true
    }
}

dependencies {
    // App dependencies
    implementation(kotlin("stdlib", version = project.extra["kotlin"] as String))
    implementation("ch.acra:acra-mail:5.5.0")
    implementation("com.melnykov:floatingactionbutton:1.2.0")
    implementation("io.reactivex.rxjava2:rxjava:2.2.19")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("org.koin:koin-core:2.1.5")
    implementation("androidx.fragment:fragment:1.2.5")
    implementation("androidx.preference:preference:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.3")
    implementation("com.google.android.material:material:1.1.0")
}

dependencies {
    testImplementation("net.wuerl.kotlin:assertj-core-kotlin:0.1.1")
    testImplementation("junit:junit:4.13")
    testImplementation("org.mockito:mockito-core:2.23.4")
    testImplementation("io.mockk:mockk:1.10.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.4.3")
}

dependencies {
    androidTestImplementation("com.squareup.assertj:assertj-android:1.1.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
    androidTestImplementation("androidx.test:runner:1.3.0")
    androidTestImplementation("androidx.test:rules:1.3.0")
    // androidx.test.ext.junit.rules.ActivityScenarioRule
    // androidx.test.ext.junit.runners.AndroidJUnit4
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
}