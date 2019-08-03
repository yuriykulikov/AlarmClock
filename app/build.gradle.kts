buildscript {
    dependencies {
        classpath(kotlin("gradle-plugin", version = "1.3.31"))
    }
}

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

    val fileFilter = listOf("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*", "**/*Test*.*", "android/**/*.*")

    val developDebug = "developDebug"

    sourceDirectories.setFrom(files(listOf(
            "$projectDir/src/main/java",
            "$projectDir/src/main/kotlin"
    )))
    classDirectories.setFrom(files(listOf(
            fileTree("dir" to "$buildDir/intermediates/javac/$developDebug", "excludes" to fileFilter),
            fileTree("dir" to "$buildDir/tmp/kotlin-classes/$developDebug", "excludes" to fileFilter)
    )))

    // execution data from both unit and instrumentation tests
    executionData.setFrom(fileTree(
            "dir" to project.buildDir,
            "includes" to listOf(
                    // unit tests
                    "jacoco/test${"developDebug".capitalize()}UnitTest.exec",
                    // instrumentation tests
                    "outputs/code_coverage/${developDebug}AndroidTest/connected/**/*.ec"
            )
    ))

    dependsOn("test${"developDebug".capitalize()}UnitTest")
    dependsOn("connected${"developDebug".capitalize()}AndroidTest")
}

tasks.withType(Test::class.java) {
    (this.extensions.getByName("jacoco") as JacocoTaskExtension).isIncludeNoLocationClasses = true
}

val acraEmail = project.rootProject.file("local.properties")
        .let { if (it.exists()) it.readLines() else emptyList() }
        .filterNot { it.startsWith("#") }
        .map { line -> line.substringBefore("=") to line.substringAfter("=") }
        .toMap()
        .getOrDefault("acra.email", "")

android {
    compileSdkVersion(28)
    defaultConfig {
        applicationId = "com.better.alarm"
        minSdkVersion(15)
        targetSdkVersion(28)
        testApplicationId = "com.better.alarm.test"
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
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
    }

    adbOptions {
        timeOutInMs = 20 * 60 * 1000  // 20 minutes
        installOptions("-d", "-t")
    }

    dexOptions {
        preDexLibraries = System.getenv("TRAVIS") != "true"
    }
}

dependencies {
    // App dependencies
    implementation(kotlin("stdlib", version = "1.3.30"))
    implementation("com.android.support:support-annotations:28.0.0")
    implementation("ch.acra:acra:4.6.1")
    implementation("com.melnykov:floatingactionbutton:1.2.0")
    implementation("io.reactivex.rxjava2:rxjava:2.2.0")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.0")
    implementation("com.f2prateek.rx.preferences2:rx-preferences:2.0.0")
    implementation("com.android.support:support-v4:26.1.0") {}

    // Testing-only dependencies
    testImplementation("net.wuerl.kotlin:assertj-core-kotlin:0.1.1")
    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:2.23.4")

    androidTestImplementation("com.squareup.assertj:assertj-android:1.1.1")
    // Force usage of support annotations in the test app, since it is internally used by the runner module.
    androidTestImplementation("com.android.support:support-annotations:28.0.0")
    androidTestImplementation("com.android.support.test:runner:1.0.2") {}
    androidTestImplementation("com.android.support.test:rules:1.0.2") {}
    androidTestImplementation("com.android.support.test.espresso:espresso-core:3.0.2")
    androidTestImplementation("com.bartoszlipinski:cortado:1.2.0")
    //for some tests which do not work with espresso on travis
    androidTestImplementation("com.jayway.android.robotium:robotium-solo:5.2.1")
}