import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension
import org.jetbrains.kotlin.gradle.targets.web.yarn.BaseYarnRootExtension

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

// The tracked Kotlin Yarn lockfiles are the npm dependency graph of the JS and Wasm builds:
// `kotlin-js-store/yarn.lock` for JS and `kotlin-js-store/wasm/yarn.lock` for Wasm. Builds consume
// them and never rewrite them; a dependency change that alters npm resolution runs
// `kotlinUpgradeYarnLock` / `kotlinWasmUpgradeYarnLock` and commits the result with that change.
//  - FAIL: resolution that disagrees with the committed lockfile breaks the build.
//  - reportNewYarnLock: a missing lockfile is reported, not silently created and accepted.
//  - no auto-replace: an ordinary build never overwrites the accepted lockfile.
fun BaseYarnRootExtension.enforceCommittedLockfile() {
    yarnLockMismatchReport = YarnLockMismatchReport.FAIL
    reportNewYarnLock = true
    yarnLockAutoReplace = false
}

plugins.withType<YarnPlugin> { the<YarnRootExtension>().enforceCommittedLockfile() }
plugins.withType<WasmYarnPlugin> { the<WasmYarnRootExtension>().enforceCommittedLockfile() }

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
