# Android App Module Guide

`androidApp` is currently a thin Android application shell that hosts shared Compose UI from `:shared`.

- Preserve the Android app -> shared dependency direction.
- Keep Android-specific setup here when it is truly platform/application responsibility.
- Do not move Android platform APIs into `shared/src/commonMain`.
- Do not introduce large legacy Views/Fragments architecture unless a scoped refresher issue explicitly asks for it.
- Use AndroidX/Compose patterns already present in the module.
- Validate Android app changes with `./gradlew :androidApp:assembleDebug`; add `./gradlew :androidApp:lintDebug` or `./gradlew check` when the change warrants broader validation.
