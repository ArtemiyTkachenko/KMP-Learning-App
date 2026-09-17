package org.artkachenko.kmp_learning_app

import java.io.File
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.artkachenko.kmp_learning_app.product.ProductMetadata

/**
 * The canonical product metadata, checked against the places a host could quietly restate it.
 *
 * Android's `versionName`/`versionCode`, the Desktop package version and the generated
 * [ProductMetadata] all read `product.properties` through Gradle, so they cannot drift — there is
 * nothing to compare. Xcode is the interesting case: it consumes the same file by `#include`ing it,
 * which works precisely because the file is valid xcconfig as well as valid Java Properties. That
 * arrangement is easy to undo by accident — a locally re-added `MARKETING_VERSION=1.0` in
 * `Config.xcconfig` would silently win — so these tests assert the arrangement itself rather than
 * comparing two copies of "0.1.0".
 */
internal class ProductMetadataContractTest {

    @Test
    fun theGeneratedMetadataIsTheCanonicalFile() {
        val canonical = canonicalProductMetadata()

        assertEquals(canonical.getProperty("PRODUCT_NAME"), ProductMetadata.NAME)
        assertEquals(canonical.getProperty("MARKETING_VERSION"), ProductMetadata.VERSION)
        assertEquals(
            canonical.getProperty("CURRENT_PROJECT_VERSION"),
            ProductMetadata.BUILD_NUMBER.toString(),
        )
    }

    /** The pre-1.0 policy the versioning document states: MAJOR.MINOR.PATCH, build number >= 1. */
    @Test
    fun theCanonicalVersionIsSemanticAndTheBuildNumberIsPositive() {
        assertTrue(
            Regex("""^\d+\.\d+\.\d+$""").matches(ProductMetadata.VERSION),
            "Product version ${ProductMetadata.VERSION} is not MAJOR.MINOR.PATCH",
        )
        assertTrue(ProductMetadata.BUILD_NUMBER >= 1, "Build numbers start at 1 and increase")
    }

    @Test
    fun xcodeReadsTheCanonicalFileAndDefinesNoReleaseValuesOfItsOwn() {
        val config = repositoryFile("iosApp/Configuration/Config.xcconfig").readText()

        assertTrue(
            config.lineSequence().any { it.trim() == """#include "../../product.properties"""" },
            "Config.xcconfig must #include the canonical product metadata",
        )
        // Anything Xcode sets after the include wins, so the three canonical settings must not be
        // assigned here at all. `$(PRODUCT_NAME)` references are fine; assignments are not.
        val ownAssignments = config.lineSequence()
            .map(String::trim)
            .filter { line -> '=' in line && line.substringBefore('=') in CanonicalSettings }
            .toList()
        assertEquals(
            emptyList(),
            ownAssignments,
            "Config.xcconfig re-defines canonical release metadata",
        )
    }

    /** The visible rename must not have moved a stable technical identifier with it. */
    @Test
    fun theRenameLeftTheStableIdentifiersAlone() {
        assertTrue(
            repositoryFile("iosApp/Configuration/Config.xcconfig").readText().contains(
                "PRODUCT_BUNDLE_IDENTIFIER=org.artkachenko.kmp_learning_app.KMP-Learning-App",
            ),
            "The iOS bundle identifier must not change with the product name",
        )
        assertTrue(
            repositoryFile("androidApp/build.gradle.kts").readText()
                .contains("""applicationId = "org.artkachenko.kmp_learning_app""""),
            "The Android application ID must not change with the product name",
        )
        assertTrue(
            repositoryFile("desktopApp/build.gradle.kts").readText()
                .contains("""bundleID = "org.artkachenko.kmp_learning_app""""),
            "The macOS bundle ID must be stated explicitly so packageName cannot change it",
        )
    }

    private companion object {
        val CanonicalSettings = setOf(
            "PRODUCT_NAME",
            "MARKETING_VERSION",
            "CURRENT_PROJECT_VERSION",
        )
    }
}

/**
 * The icon assets, checked for existence, format and the configuration that points at them.
 *
 * Deliberately not a screenshot comparison. Whether the mark looks right is a judgement made by
 * looking at it, and a golden image of a generated icon would fail on an anti-aliasing difference
 * while saying nothing about the design. What is worth automating is the part that breaks silently:
 * a host referring to an asset that is not there, or a launcher asset in the wrong size.
 */
internal class AppIconAssetTest {

    @Test
    fun androidUsesAdaptiveIconLayersThatExist() {
        listOf("ic_launcher.xml", "ic_launcher_round.xml").forEach { name ->
            val adaptive = repositoryFile("androidApp/src/main/res/mipmap-anydpi-v26/$name")
            assertTrue(adaptive.isFile, "$name is missing")
            val xml = adaptive.readText()
            listOf(
                "ic_launcher_background" to "background",
                "ic_launcher_foreground" to "foreground",
                "ic_launcher_monochrome" to "monochrome",
            ).forEach { (drawable, layer) ->
                assertTrue(
                    xml.contains("""<$layer android:drawable="@drawable/$drawable" />"""),
                    "$name is missing its $layer layer",
                )
                assertTrue(
                    repositoryFile("androidApp/src/main/res/drawable/$drawable.xml").isFile,
                    "$drawable.xml is missing",
                )
            }
        }
    }

    @Test
    fun androidLegacyMipmapsExistAtEveryDensityAndTheExpectedSize() {
        mapOf("mdpi" to 48, "hdpi" to 72, "xhdpi" to 96, "xxhdpi" to 144, "xxxhdpi" to 192)
            .forEach { (density, size) ->
                listOf("ic_launcher.png", "ic_launcher_round.png").forEach { name ->
                    val icon = repositoryFile("androidApp/src/main/res/mipmap-$density/$name")
                    assertTrue(icon.isFile, "mipmap-$density/$name is missing")
                    assertEquals(
                        size to size,
                        pngSize(icon),
                        "mipmap-$density/$name is the wrong size",
                    )
                }
            }
    }

    @Test
    fun theManifestStillReferencesTheLauncherIcons() {
        val manifest = repositoryFile("androidApp/src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("""android:icon="@mipmap/ic_launcher""""))
        assertTrue(manifest.contains("""android:roundIcon="@mipmap/ic_launcher_round""""))
    }

    /** The existing AppIcon set is updated rather than duplicated, so there must be exactly one. */
    @Test
    fun iosHasOneAppIconSetHoldingA1024PxMaster() {
        val appIconSets = repositoryFile("iosApp").walkTopDown()
            .filter { it.isDirectory && it.name.endsWith(".appiconset") }
            .toList()
        assertEquals(1, appIconSets.size, "Expected exactly one AppIcon set, found $appIconSets")

        val appIconSet = appIconSets.single()
        assertEquals("AppIcon.appiconset", appIconSet.name)
        assertTrue(
            appIconSet.resolve("Contents.json").readText()
                .contains(""""filename" : "app-icon-1024.png""""),
            "The asset catalogue does not reference the icon file",
        )
        assertEquals(1024 to 1024, pngSize(appIconSet.resolve("app-icon-1024.png")))
    }

    @Test
    fun desktopPackagingAndWindowIconsExistInTheFormatsTheBuildAsksFor() {
        val build = repositoryFile("desktopApp/build.gradle.kts").readText()

        mapOf(
            "icons/app-icon.icns" to "macOS",
            "icons/app-icon.ico" to "windows",
            // Linux packaging reuses the classpath PNG rather than a second copy of it.
            "src/main/resources/app-icon.png" to "linux",
        ).forEach { (path, platform) ->
            assertTrue(
                build.contains("""iconFile.set(project.file("$path"))"""),
                "The $platform distribution does not point at $path",
            )
            assertTrue(repositoryFile("desktopApp/$path").isFile, "$path is missing")
        }

        // The window icon is a classpath resource, loaded by name in main.kt.
        val windowIcon = repositoryFile("desktopApp/src/main/resources/app-icon.png")
        assertTrue(windowIcon.isFile, "The desktop window icon is missing")
        assertEquals(512 to 512, pngSize(windowIcon))
    }

    @Test
    fun theWebHostNamesTheProductAndPointsAtFaviconsThatExist() {
        val index = repositoryFile("webApp/src/webMain/resources/index.html").readText()

        assertTrue(index.contains("<title>${ProductMetadata.NAME}</title>"))
        assertTrue(index.contains("""href="favicon.svg""""))
        assertTrue(index.contains("""href="favicon-32.png""""))
        assertTrue(repositoryFile("webApp/src/webMain/resources/favicon.svg").isFile)
        assertEquals(
            32 to 32,
            pngSize(repositoryFile("webApp/src/webMain/resources/favicon-32.png")),
        )
    }

    /** Every checked-in asset is derived from the one master, which has to be checked in too. */
    @Test
    fun theCanonicalIconSourceIsCheckedIn() {
        assertTrue(repositoryFile("tools/icon/app_icon.py").isFile)
        assertTrue(repositoryFile("tools/icon/render_app_icons.py").isFile)
        assertTrue(repositoryFile("tools/icon/app-icon.svg").isFile)
    }
}

/** The repository root, found by walking up from wherever the test task set the working directory. */
private fun repositoryRoot(): File {
    var directory: File? = File(".").absoluteFile
    while (directory != null) {
        if (directory.resolve("product.properties").isFile) return directory
        directory = directory.parentFile
    }
    error("Could not locate the repository root from ${File(".").absolutePath}")
}

private fun repositoryFile(path: String): File = repositoryRoot().resolve(path)

private fun canonicalProductMetadata(): Properties =
    Properties().apply {
        repositoryFile("product.properties").inputStream().use { load(it) }
    }

/** Width and height from a PNG's IHDR chunk, which is always the first one. */
private fun pngSize(file: File): Pair<Int, Int> {
    val header = file.readBytes()
    require(header.size > 24) { "${file.name} is not a PNG" }
    fun intAt(offset: Int) = (0..3).fold(0) { value, index ->
        (value shl 8) or (header[offset + index].toInt() and 0xFF)
    }
    return intAt(16) to intAt(20)
}
