import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.androidxRoom3)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
}

// The canonical product metadata, read from product.properties by the root build.
val productName = rootProject.extra["productName"] as String
val productVersion = rootProject.extra["productVersion"] as String
val productBuildNumber = rootProject.extra["productBuildNumber"] as Int

/**
 * Generates the single Kotlin view of the canonical product metadata.
 *
 * The shared About section needs the product name and version at runtime, and the Desktop host
 * needs the name for its window title. Generating them from `product.properties` is what keeps the
 * in-app value and the four hosts' package metadata the same value rather than five literals that
 * happen to agree. It is a task rather than a resource because the values are build inputs, so a
 * changed `product.properties` re-runs this and nothing else.
 */
val generateProductMetadata = tasks.register("generateProductMetadata") {
    val outputDirectory = layout.buildDirectory.dir("generated/productMetadata/commonMain/kotlin")
    // Read into locals here, at configuration time, so the execution-time action captures three
    // plain values rather than this build script. Declaring them as inputs is what makes the task
    // up to date exactly while the canonical values are.
    val name = productName
    val version = productVersion
    val buildNumber = productBuildNumber
    inputs.property("productName", name)
    inputs.property("productVersion", version)
    inputs.property("productBuildNumber", buildNumber)
    outputs.dir(outputDirectory)

    doLast {
        val packageDirectory = outputDirectory.get().asFile
            .resolve("org/artkachenko/kmp_learning_app/product")
        packageDirectory.mkdirs()
        packageDirectory.resolve("ProductMetadata.kt").writeText(
            """
            package org.artkachenko.kmp_learning_app.product

            /**
             * The product's visible identity and release metadata.
             *
             * Generated from `product.properties` by `:shared:generateProductMetadata`. Do not edit,
             * and do not restate these values anywhere: the About section, the Desktop window title,
             * the Android versionName/versionCode, the Desktop package version and the iOS marketing
             * and build versions all come from that one file.
             */
            public object ProductMetadata {
                /** The user-visible product name. */
                public const val NAME: String = "$name"

                /** The MAJOR.MINOR.PATCH product version. */
                public const val VERSION: String = "$version"

                /** The monotonically increasing build number. */
                public const val BUILD_NUMBER: Int = $buildNumber
            }

            """.trimIndent(),
        )
    }
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    jvm()
    
    js {
        browser()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    android {
       namespace = "org.artkachenko.kmp_learning_app.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        commonMain {
            // The generated ProductMetadata, compiled into commonMain so every host and the
            // shared About section read the same canonical values.
            kotlin.srcDir(generateProductMetadata)
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.koin.android)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)
            implementation(libs.androidx.room3.runtime)
            implementation(libs.androidx.sqlite.async)
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        iosMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
        }
        jvmMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
        }
        jvmTest.dependencies {
            implementation(libs.androidx.room3.testing)
            implementation(libs.compose.uiTestJunit4)
            implementation(compose.desktop.currentOs)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.kotlinx.coroutines.test)
        }
        webMain.dependencies {
            implementation(libs.androidx.sqlite.web)
            implementation(project(":sqliteWasmWorker"))
        }
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
    add("kspAndroid", libs.androidx.room3.compiler)
    add("kspJvm", libs.androidx.room3.compiler)
    add("kspIosArm64", libs.androidx.room3.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room3.compiler)
    add("kspJs", libs.androidx.room3.compiler)
    add("kspWasmJs", libs.androidx.room3.compiler)
}
