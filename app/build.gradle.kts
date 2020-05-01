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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    useLibrary("android.test.runner")
    useLibrary("android.test.base")
    useLibrary("android.test.mock")
}

dependencies {
    // App dependencies
    implementation(kotlin("stdlib", version = "1.3.30"))
    implementation("ch.acra:acra-mail:5.5.0")
    implementation("com.melnykov:floatingactionbutton:1.2.0")
    implementation("io.reactivex.rxjava2:rxjava:2.2.0")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.0")
    implementation("com.f2prateek.rx.preferences2:rx-preferences:2.0.0")
    implementation("com.android.support:support-v4:26.1.0") {}

    implementation("org.koin:koin-core:2.0.1")
    implementation("org.koin:koin-core-ext:2.0.1")
    testImplementation("org.koin:koin-test:2.0.1")

    val fragment_version = "1.2.2"
    implementation("androidx.fragment:fragment:$fragment_version")
    implementation("androidx.fragment:fragment-ktx:$fragment_version")
    implementation("androidx.fragment:fragment-testing:$fragment_version")

    val preference_version = "1.1.0"
    implementation("androidx.preference:preference:$preference_version")
    implementation("androidx.preference:preference-ktx:$preference_version")

    // Testing-only dependencies
    testImplementation("net.wuerl.kotlin:assertj-core-kotlin:0.1.1")
    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:2.23.4")
    testImplementation("io.mockk:mockk:1.9.3")

    androidTestImplementation("com.squareup.assertj:assertj-android:1.1.1")
    androidTestImplementation("com.bartoszlipinski:cortado:1.2.0")
    //for some tests which do not work with espresso on travis
    androidTestImplementation("com.jayway.android.robotium:robotium-solo:5.2.1")

    androidTestImplementation("androidx.test:core:1.2.0")
    androidTestImplementation("androidx.test:runner:1.2.0")
    androidTestImplementation("androidx.test:rules:1.2.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.1")
    androidTestImplementation("androidx.test.ext:truth:1.2.0")
    androidTestImplementation("com.google.truth:truth:0.44")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.2.0")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.2.0")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.2.0")
    androidTestImplementation("androidx.test.espresso:espresso-accessibility:3.2.0")
    androidTestImplementation("androidx.test.espresso:espresso-web:3.2.0")
    androidTestImplementation("androidx.test.espresso.idling:idling-concurrent:3.2.0")
    androidTestImplementation("androidx.test.espresso:espresso-idling-resource:3.2.0")
}