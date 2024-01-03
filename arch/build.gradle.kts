@file:Suppress("UnstableApiUsage")

import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotest.multiplatform)
    alias(libs.plugins.kotlinx.kover)
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

    jvm()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

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
}

mavenPublishing {
    coordinates("com.m2f-kt", "archer", "0.0.1-dev03")

    publishToMavenCentral(SonatypeHost.S01)
    signAllPublications()

    pom {
        name.set("Archer")
        description.set("A KMP library to generate clean architecture components using Arrow")
        inceptionYear.set("2023")
        url.set("https://github.com/m2f-kt/Archer")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("Atternatt")
                name.set("Marc Moreno Ferrer")
                url.set("https://github.com/Atternatt/")
            }
        }
        scm {
            url.set("https://github.com/m2f-kt/Archer/tree/main")
            connection.set("scm:git:github.com/m2f-kt/Archer.git")
            developerConnection.set("scm:git:ssh://github.com/m2f-kt/Archer.git")
        }
    }
}
