@file:Suppress("UnstableApiUsage")

import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotest.multiplatform)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.com.vanniktech.maven.publish)
}

kotlin {
    androidTarget {
        publishAllLibraryVariants()
        compilations.all {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_1_8.toString()
            }
        }
    }

    jvm()

    iosArm64()
    iosSimulatorArm64()

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.all {
            // Add linker flag for SQLite. See:
            // https://github.com/touchlab/SQLiter/issues/77
            linkerOpts("-lsqlite3")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.database)
            implementation(libs.bundles.arrow)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.time)
        }
        commonTest.dependencies {
            implementation(libs.bundles.kotest)
        }

        jvmTest.dependencies {
            implementation(libs.kotest.runnerJUnit5)
        }
    }

}


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        languageVersion = "1.9"
        apiVersion = "1.9"
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    filter {
        isFailOnNoMatchingTests = false
    }
    testLogging {
        showExceptions = true
        showStandardStreams = true
        events = setOf(
            FAILED,
            PASSED
        )
        exceptionFormat = FULL
    }
}

android {
    namespace = "com.m2f.archer"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

val VERSION_NAME: String by project
val GROUP: String by project

mavenPublishing {
    coordinates(GROUP, "archer", VERSION_NAME)
}
