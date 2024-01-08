plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotest.multiplatform) apply false
    alias(libs.plugins.kotlinx.kover) apply false
    alias(libs.plugins.coverallsjacoco)
    alias(libs.plugins.binary.compatibility.validator) apply false
    alias(libs.plugins.com.vanniktech.maven.publish) apply false
}

val sources: List<File> = subprojects.flatMap { project ->
    file("${project.projectDir}/src/").walkBottomUp().maxDepth(2)
        .filter { it.path.contains("kotlin", ignoreCase = true) }
        .filter { it.path.contains("main", ignoreCase = true) }
        .toSet()
        .toList()
}

coverallsJacoco {
    reportPath = "${layout.projectDirectory}/archer-core/build/reports/kover/report.xml"
    reportSourceSets = sources
}

val GROUP: String by project
val VERSION_NAME: String by project

allprojects {

    group = GROUP
    version= VERSION_NAME

    extensions.findByType<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension>()?.apply {
        sourceSets.all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
    }
}
