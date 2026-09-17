# Product Identity And Versioning

The visible product, its release metadata, and the application icon. For the build files
that consume these see [Gradle](gradle.md); for the commands see
[validation](validation.md).

## Visible Product Name

The product is called **Android Engineering Lab**. Kotlin Multiplatform is how it is
built, not what it is about, so the scaffold name `KMP-Learning-App` is no longer shown to
anyone.

The rename is visible-only. These are stable technical identities and deliberately keep
the old spelling, because changing one orphans an installed copy of the app or its data
while giving a user nothing:

| Identifier | Value |
| --- | --- |
| Git repository | `KMP-Learning-App` |
| Gradle root project | `KMP-Learning-App` |
| Kotlin package / Android namespace | `org.artkachenko.kmp_learning_app` |
| Android `applicationId` | `org.artkachenko.kmp_learning_app` |
| iOS `PRODUCT_BUNDLE_IDENTIFIER` | `org.artkachenko.kmp_learning_app.KMP-Learning-App` |
| macOS distribution `bundleID` | `org.artkachenko.kmp_learning_app` |
| Desktop data directory | `~/.kmp-learning-app` |
| Room database file | `curriculum.db` |
| Curriculum, Question, Unit and Lesson IDs | unchanged |

Historical records — backlog entries, older documentation describing what the project used
to be called — keep the old name. They are statements about the past.

## Canonical Product Metadata

`product.properties`, at the repository root, is the one definition of the product name,
the product version and the build number:

```properties
PRODUCT_NAME=Android Engineering Lab
MARKETING_VERSION=0.1.0
CURRENT_PROJECT_VERSION=1
```

It is written so Gradle and Xcode can both read it **verbatim**: every setting is
`NAME=value`, and the only comment form is `//`, which xcconfig treats as a comment and a
Java Properties reader parses as a key nothing asks for. That is why there is no generated
bridge file and nothing to keep in sync.

Who reads it:

| Consumer | How |
| --- | --- |
| The root `build.gradle.kts` | Loads the file once and exposes `productName`, `productVersion` and `productBuildNumber` as `rootProject.extra` |
| Android | `versionName = productVersion`, `versionCode = productBuildNumber` in `androidApp/build.gradle.kts` |
| Desktop | `packageVersion = productVersion` in `desktopApp/build.gradle.kts` |
| iOS | `iosApp/Configuration/Config.xcconfig` does `#include "../../product.properties"`, which supplies `PRODUCT_NAME`, `MARKETING_VERSION` and `CURRENT_PROJECT_VERSION` directly |
| Shared UI and the Desktop window title | `:shared:generateProductMetadata` writes `ProductMetadata` into `commonMain` from the same values |

No module declares a release value of its own, and no Kotlin file contains a version
literal. `ProductMetadataContractTest` asserts the arrangement rather than comparing
copies: it fails if `Config.xcconfig` stops including the canonical file, if it re-assigns
one of the three canonical settings locally, or if a stable identifier moved with the
visible name.

### Changing the version

Edit `product.properties`. Nothing else. Then re-run the narrow checks — the Gradle tasks
take the file as a build input, so Android, Desktop and `ProductMetadata` all follow, and
Xcode reads it on its next build.

## Versioning Policy

`MAJOR.MINOR.PATCH`, currently `0.1.0` with build number `1`.

- **MINOR** increments for a product addition while the product is pre-1.0.
- **PATCH** increments for a fix to an already-released version.
- **Build number** is a monotonically increasing positive integer, independent of the
  product version. It never decreases and is not reset by a version change.
- **1.0.0 is reserved** for a stable release that is declared deliberately, not reached by
  accumulating minors.

Deliberately not implemented, and out of scope until there is a reason: Git tagging,
changelog generation, GitHub Releases, store publishing, automatic version bumping, and
signing or release infrastructure.

### Known constraint: macOS native packaging is blocked pre-1.0

`jpackage` refuses an `--app-version` whose first component is `0`, because that value
becomes `CFBundleVersion` and Apple requires it to start at 1 or higher. So while the
product version is `0.x.y`, `:desktopApp:createDistributable`, `:desktopApp:packageDmg`
and the tasks downstream of them fail with:

```text
The first number in an app-version cannot be zero or negative.
```

This is deliberately **not** worked around. The two available workarounds are to state a
macOS-only version literal, which is the drift this whole arrangement exists to prevent, or
to claim `1.0.0` in a shipped bundle while the product is `0.1.0`, which is worse than not
shipping a bundle. Native installer packaging is out of scope until the product is released
deliberately, and the constraint disappears at `1.0.0`.

Everything else in the macOS configuration is correct and was verified by building the app
image against a temporary `1.0.0`: `CFBundleName` is `Android Engineering Lab`,
`CFBundleIdentifier` is the unchanged `org.artkachenko.kmp_learning_app`, and the bundle
carries the generated `.icns`. `:desktopApp:assemble` and `:desktopApp:run` are unaffected
by the constraint — the running application is the product identity, the installer is not.

`packageMsi` and `packageDeb` cannot be built on macOS at all: `jpackage` only produces
installers for the host operating system. Their configuration — the package name slug and
the `.ico`/`.png` icons — is checked by `AppIconAssetTest` rather than by a build.

## Application Icon

The mark is an isometric cube built from three layers, drawn in white and two lighter
indigos from the product palette on the indigo `primary` plate. The layers are seams inside
one silhouette rather than three separate slabs, so the mark degrades gracefully: at
launcher size the layering reads, and at a 16px favicon the seams fall below a pixel and
what remains is still a clean cube.

**The canonical source is `tools/icon/`**, not any host asset:

| File | Role |
| --- | --- |
| `tools/icon/app_icon.py` | The geometry and palette, stated once |
| `tools/icon/render_app_icons.py` | Writes every host asset from that geometry |
| `tools/icon/app-icon.svg` | The master rendering, emitted for inspection |

The mark is authored on the 108×108 grid Android adaptive icons use — the space with the
strictest constraint — and reaches 30.4 of the 33.0 radius a launcher mask is guaranteed
not to cut into. Hosts that need a pre-masked square render the same design zoomed by
108⁄72, which is exactly what a launcher shows of an adaptive icon.

### Regenerating the assets

```sh
python3 tools/icon/render_app_icons.py
```

It needs only Python 3 and, for the macOS `.icns`, `iconutil`, which ships with macOS. It
overwrites every derived asset:

| Host | Assets |
| --- | --- |
| Android | `drawable/ic_launcher_{background,foreground,monochrome}.xml` (adaptive, including the themed-icon monochrome layer) and `mipmap-{m,h,x,xx,xxx}hdpi/ic_launcher{,_round}.png` for launchers below API 26 |
| iOS | `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png` — the existing catalogue, updated in place; there is exactly one AppIcon set |
| Desktop | `desktopApp/icons/app-icon.{icns,ico}` for the Dmg and Msi formats, and `desktopApp/src/main/resources/app-icon.png`, which the window loads from the classpath and the Deb package reuses |
| Web | `webApp/src/webMain/resources/favicon.svg` and `favicon-32.png`, linked from `index.html`; no web app manifest and no service worker |

Never edit a derived asset by hand — change the geometry and re-run, so every host keeps
the same mark. `AppIconAssetTest` checks that the files exist, that their dimensions are
right and that each host's configuration still points at them. It deliberately does not
compare screenshots: whether the mark looks right is a judgement made by looking at it, and
a golden image would fail on an anti-aliasing difference while saying nothing about the
design.

## Appearance Preference

The one user setting, reached from the Settings destination inside the Learn stack. It is
documented with the rest of the theme in
[Material Design 3](material-design.md) and its ownership in
[architecture overview](../architecture/overview.md).
