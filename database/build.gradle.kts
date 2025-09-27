import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.sqlDelight)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.com.vanniktech.maven.publish)
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
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

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.all {
            // Add linker flag for SQLite. See:
            // https://github.com/touchlab/SQLiter/issues/77
            linkerOpts("-lsqlite3")
        }
    }

    js(IR) {
        browser()
    }

    tasks.withType<KotlinJsCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll("-Xklib-enable-signature-clash-checks=false")
        }
    }

    sourceSets {

        commonMain.dependencies {
            implementation(libs.time)
            implementation(libs.bundles.arrow)
        }

        androidMain.dependencies {
            implementation(libs.startup)
            implementation(libs.sqlDelight.driver.android)
        }

        iosMain.dependencies {
            implementation(libs.sqlDelight.driver.native)
        }

        jvmMain.dependencies {
            implementation(libs.sqlDelight.driver.sqlite)
        }

        jsMain.dependencies {
            api(libs.web.worker.driver)
            implementation(npm("@cashapp/sqldelight-sqljs-worker", "2.0.0"))
            implementation(npm("sql.js", "1.8.0"))
            api(devNpm("copy-webpack-plugin", "9.1.0"))
        }
    }
}

sqldelight {
    databases {
        create("CacheExpirationDatabase") {
            packageName.set("com.m2f.archer.sqldelight")
            generateAsync.set(true)
        }
    }
}

tasks.withType(KotlinCompile::class).configureEach {
    compilerOptions {
        freeCompilerArgs.addAll("-Xexpect-actual-classes")
    }
}

android {
    namespace = "com.m2f.archer.database"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
