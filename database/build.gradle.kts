import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.sqlDelight)
    alias(libs.plugins.com.vanniktech.maven.publish)
}

kotlin {

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_1_8.toString()
            }
        }
    }

    js {
        browser()
        binaries.executable()
    }

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_1_8.toString()
            }
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sqldelight {
        databases {
            create(name = "CacheExpirationDatabase") {
                packageName.set("com.m2f.archer.sqldelight")
                generateAsync.set(true)
            }
        }
    }

    tasks.withType(KotlinCompile::class).configureEach {
        compilerOptions {
            freeCompilerArgs.addAll("-Xexpect-actual-classes")
        }

        kotlinOptions.freeCompilerArgs += "-Xexpect-actual-classes"
    }


    tasks.withType(KotlinCompile::class).configureEach {
        compilerOptions {
            freeCompilerArgs.addAll("-Xexpect-actual-classes")
        }

        kotlinOptions.freeCompilerArgs += "-Xexpect-actual-classes"
    }

    sourceSets {

        commonMain.dependencies {
            implementation(libs.time)
            implementation(libs.bundles.arrow)
            implementation(libs.startup)
            implementation(libs.bundles.kotest)
        }

        androidMain.dependencies {
            implementation(libs.sqlDelight.driver.android)
        }

        iosMain.dependencies {
            implementation(libs.sqlDelight.driver.native)
        }

        jvmMain.dependencies {
            implementation(libs.sqlDelight.driver.sqlite)
        }

        jvmTest.dependencies {
            implementation(libs.kotest.runnerJUnit5)
        }

        jsMain.dependencies {
            implementation(libs.web.worker.driver)
            implementation(devNpm("copy-webpack-plugin", "9.1.0"))
            implementation(npm("@cashapp/sqldelight-sqljs-worker", "2.0.1"))
            implementation(npm("sql.js", "1.8.0"))
        }
    }
}

android {
    namespace = "com.m2f.archer"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
}

