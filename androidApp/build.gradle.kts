import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

// The canonical release metadata, read from product.properties by the root build. The Android
// application no longer keeps a versionCode/versionName pair of its own.
val productVersion = rootProject.extra["productVersion"] as String
val productBuildNumber = rootProject.extra["productBuildNumber"] as Int

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "org.artkachenko.kmp_learning_app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.artkachenko.kmp_learning_app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = productBuildNumber
        versionName = productVersion
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    lint {
        // These three detectors report that a newer release of a dependency exists. They
        // contact the dependency repositories on every run, so their output changes when
        // somebody else publishes rather than when this repository changes — which is not
        // something a pull-request gate should be sensitive to. Dependency currency is a
        // deliberate decision made in `gradle/libs.versions.toml`, not a lint finding.
        disable += setOf("NewerVersionAvailable", "GradleDependency", "AndroidGradlePluginVersion")
    }
}
