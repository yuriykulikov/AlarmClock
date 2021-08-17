buildscript {
    val kotlin = "1.5.21"

    allprojects {
        extra.apply {
            set("kotlin", kotlin)
        }
    }

    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }

    dependencies {
        classpath("com.android.tools.build:gradle:7.0.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}