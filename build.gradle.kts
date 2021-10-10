buildscript {
  val kotlin = "1.5.21"

  allprojects { extra.apply { set("kotlin", kotlin) } }

  repositories {
    google()
    mavenCentral()
    maven { url = uri("https://plugins.gradle.org/m2/") }
  }

  dependencies {
    classpath("com.android.tools.build:gradle:7.0.2")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin")
  }
}

plugins { id("com.diffplug.spotless") version "5.14.2" }

allprojects {
  repositories {
    google()
    mavenCentral()
    maven { url = uri("https://plugins.gradle.org/m2/") }
  }

  apply(plugin = "com.diffplug.spotless")
  spotless {
    kotlin {
      target("**/*.kt", "**/*.kts")
      targetExclude("$buildDir/**/*.kt")
      targetExclude("/gen/**/*.kt")
      targetExclude("/src/test/resources/**/*.kt")
      ktfmt()
      lineEndings = com.diffplug.spotless.LineEnding.UNIX
    }

    java {
      target("**/*.java")
      targetExclude("$buildDir/**/*.java")
      targetExclude("/gen/**/*.java")
      targetExclude("/src/test/resources/**/*.java")
      googleJavaFormat()
      lineEndings = com.diffplug.spotless.LineEnding.UNIX
    }
  }
}
