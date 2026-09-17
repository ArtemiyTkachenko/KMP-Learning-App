import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.composeCompiler)
}

// The canonical product metadata, read from product.properties by the root build. The desktop
// distribution no longer keeps a package version of its own.
val productName = rootProject.extra["productName"] as String
val productVersion = rootProject.extra["productVersion"] as String

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "org.artkachenko.kmp_learning_app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            // The visible distribution name. It used to be the reverse-DNS identifier, which the
            // macOS bundle ID then defaulted to; the ID is stated explicitly below so the rename
            // stays visible-only.
            packageName = productName
            packageVersion = productVersion
            description = "Android engineering interview preparation."

            macOS {
                // Unchanged by the rename: this is the app's stable technical identity on macOS,
                // and it no longer follows packageName.
                bundleID = "org.artkachenko.kmp_learning_app"
                iconFile.set(project.file("icons/app-icon.icns"))
            }
            windows {
                iconFile.set(project.file("icons/app-icon.ico"))
            }
            linux {
                // Debian package names cannot contain spaces or upper case, so the visible name
                // has a deterministic slug here rather than being rejected by jpackage.
                packageName = "android-engineering-lab"
                // The same PNG the window loads from the classpath, rather than a second copy of
                // it: Linux packaging wants a plain PNG and so does the window.
                iconFile.set(project.file("src/main/resources/app-icon.png"))
            }
        }
    }
}
