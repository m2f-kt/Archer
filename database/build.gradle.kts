import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
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
        publishAllLibraryVariants()
        compilations.all {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_17.toString()
            }
        }
    }

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_17.toString()
            }
        }
    }

    iosArm64()
    iosSimulatorArm64()

    js {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    tasks.withType<KotlinJsCompile>().configureEach {
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xklib-enable-signature-clash-checks=false",
        )
    }

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

        jsMain.dependencies {
            implementation(libs.web.worker.driver)
            api(npm("sql.js", "1.6.2"))
            api(devNpm("copy-webpack-plugin", "9.1.0"))
        }
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
