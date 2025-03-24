buildscript {
  repositories { google() }
  dependencies { dependencies { classpath("com.android.tools.build:gradle:7.3.1") } }
}

plugins {
  jacoco
  id("com.diffplug.spotless") version "7.0.2"
  val kotlin = "1.9.22"
  kotlin("plugin.serialization") version kotlin apply false
  kotlin("android") version kotlin apply false
}

allprojects {
  repositories {
    mavenCentral()
    google()
  }
}

spotless {
  kotlin {
    target("build.gradle.kts")
    ktfmt()
    lineEndings = com.diffplug.spotless.LineEnding.UNIX
  }
}

/** Applies spotless to app projects */
subprojects {
  // afterEvaluate is required for android projects
  afterEvaluate {
    apply(plugin = "com.diffplug.spotless")
    spotless {
      kotlin {
        target("build.gradle.kts", "**/*.kt")
        targetExclude("$buildDir/**/*.kt")
        targetExclude("**/test/resources/**/*.kt")
        ktfmt()
        lineEndings = com.diffplug.spotless.LineEnding.UNIX
      }
    }

    spotless {
      java {
        target("**/*.java")
        targetExclude("$buildDir/**/*.java")
        googleJavaFormat()
        lineEndings = com.diffplug.spotless.LineEnding.UNIX
      }
    }
  }
}
