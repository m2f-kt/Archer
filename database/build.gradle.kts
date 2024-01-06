import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.sqlDelight)
    id(libs.plugins.com.vanniktech.maven.publish.get().pluginId)
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

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_1_8.toString()
            }
        }
    }

    iosArm64()
    iosSimulatorArm64()

    sqldelight {
        databases {
            create(name = "CacheExpirationDatabase") {
                packageName.set("com.m2f.archer.sqldelight")
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
            implementation(libs.startup)
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

