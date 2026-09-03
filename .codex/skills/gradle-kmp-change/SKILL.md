---
name: gradle-kmp-change
description: Safely modify Gradle and Kotlin Multiplatform build configuration for this repository. Use for build.gradle.kts, settings.gradle.kts, libs.versions.toml, source-set dependencies, targets, plugins, or Gradle properties; not for ordinary Kotlin code changes.
---

# Gradle KMP Change

## Use When

Changing Gradle build files, settings, the version catalog, plugins, targets, source-set
dependency declarations, module dependencies, or build properties.

## Do Not Use When

The change is ordinary Kotlin code that merely happens to live in a Gradle project.

## Workflow

1. Read the affected build files and `gradle/libs.versions.toml` before editing.
2. Identify the narrowest module and source set that needs the change.
3. Make the change, keeping versions in the catalog and platform dependencies out of
   `commonMain`.
4. Verify the source-set dependency matches the target that consumes it, and that
   dependency direction is still application shell -> `:shared`.
5. Validate with the narrowest task, then widen if build behavior changed across modules.

## Project References

- [Gradle](../../../docs/development/gradle.md) — build layout, rules, and the
  dependency-addition procedure.
- [KMP boundaries](../../../docs/development/kmp.md) — modules, targets, source sets.
- [Validation](../../../docs/development/validation.md) — commands and known non-fatal
  build warnings.

## Output

Explain why any new plugin, target, dependency, or source-set dependency is needed, name
the commands run, and report any non-fatal build warnings observed.
