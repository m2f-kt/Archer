@file:Suppress("UnstableApiUsage")

import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.com.vanniktech.maven.publish)
}

tasks.withType(KotlinJvmCompile::class).configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

tasks.withType(KotlinJsCompile::class).configureEach {
    compilerOptions {
        freeCompilerArgs.addAll("-Xklib-enable-signature-clash-checks=false")
    }
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }

    jvm()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    js(IR) {
        browser()
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.all {
            // Add linker flag for SQLite. See:
            // https://github.com/touchlab/SQLiter/issues/77
            linkerOpts("-lsqlite3")
        }
    }

    compilerOptions {
        freeCompilerArgs.set(listOf("-Xcontext-parameters"))
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.database)
            implementation(libs.time)
            implementation(libs.bundles.arrow)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlin.serilization)
        }
        commonTest.dependencies {
            implementation(libs.bundles.test)
        }

        jsMain.dependencies {
            implementation("org.jetbrains.kotlin:kotlinx-atomicfu-runtime")
        }

        androidUnitTest.dependencies {
            implementation(libs.sqlDelight.driver.sqlite)
        }

        jvmTest.dependencies {
            implementation(libs.sqlDelight.driver.sqlite)
        }
    }
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "*SharedPreferences*",
                    "*ToDataSource*",
                    "*StringToSerializableBijection*",
                    "*QueriesRepoKt*"
                )
            }
        }
        verify {
            rule("at least 90% coverage") {
                minBound(90)
            }
        }
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
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}
