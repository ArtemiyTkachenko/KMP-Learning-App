# Android App Module Guide

`androidApp` is a thin Android application shell hosting shared Compose UI from `:shared`.
It owns Android application concerns only: the Activity entry point, the manifest,
launcher resources, app id, SDK configuration, Koin startup, and Android preview setup.

- Preserve the `androidApp -> :shared` dependency direction, and keep Android platform
  APIs out of `shared/src/commonMain`. See [KMP boundaries](../docs/development/kmp.md).
- Keep Android-specific setup here only when it is genuinely a platform or application
  responsibility; product behavior belongs in `:shared`.
- Use the AndroidX/Compose patterns already present in the module. Compose composition,
  navigation, and adaptive layout conventions are documented in
  [architecture overview](../docs/architecture/overview.md).
- Do not introduce a large legacy Views/Fragments architecture unless a scoped refresher
  issue explicitly asks for it.
- Validate with `./gradlew :androidApp:assembleDebug`, adding
  `./gradlew :androidApp:lintDebug` when the change warrants it. See
  [validation](../docs/development/validation.md).
