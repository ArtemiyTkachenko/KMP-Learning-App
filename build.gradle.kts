plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.androidxRoom3) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

// The canonical product metadata, read once here and shared with every module that needs it.
//
// `product.properties` is the single definition of the visible product name, the product version
// and the build number; Xcode `#include`s the same file. Modules read these extras instead of
// declaring release values of their own, so Android, Desktop, iOS and the in-app About section
// cannot drift apart. No plugin is involved: this is a properties file and eleven lines of Gradle.
val productMetadata = java.util.Properties().apply {
    rootProject.file("product.properties").inputStream().use { load(it) }
}

fun productMetadata(name: String): String =
    requireNotNull(productMetadata.getProperty(name)?.trim()?.takeIf(String::isNotEmpty)) {
        "product.properties is missing $name"
    }

extra["productName"] = productMetadata("PRODUCT_NAME")
extra["productVersion"] = productMetadata("MARKETING_VERSION")
extra["productBuildNumber"] = productMetadata("CURRENT_PROJECT_VERSION").toInt()
