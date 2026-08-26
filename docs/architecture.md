# Architecture Notes

## Application Composition

Shared UI dependencies that are local to the current Compose shell are still
assembled manually at `App()` and passed into `AppShell()` as an explicit
parameter object.

The local curriculum data graph now uses Koin because E07 introduced concrete
runtime dependencies that need platform-aware composition: `CurriculumDatabase`,
`CurriculumImporter`, `CurriculumDataInitializer`, and
`CurriculumRepository`. Koin is started by the Android `Application`, combines
the shared curriculum module with the Android database module, and uses the
classic DSL only. Koin annotations, compiler plugins, Compose injection, and
ViewModel DSLs are deferred until a real requirement appears.

Android startup now follows:

```text
Application
  -> start Koin
MainActivity
  -> await local curriculum initialization
  -> App()
```

`CurriculumRepository` is the application-facing data boundary intended for
E08 assessment-engine work. Runtime reads should depend on that interface
rather than on Room entities, DAOs, or the local repository implementation.

ViewModel factories are supplied from the composition root, but actual ViewModel
creation remains inside the Navigation 3 entry's `viewModel { ... }` call. That
keeps construction policy centralized while preserving Navigation 3 entry-scoped
ViewModel ownership: each back-stack entry receives its own ViewModelStore, and
the ViewModel is cleared when that entry is removed.

## Curriculum Content Model

The curriculum content contract lives in shared `commonMain` code as immutable
Kotlin models with flat Topic, Subtopic, and Question collections linked by
stable string IDs. The flat shape is intentional: it keeps content identity
independent from display text and avoids coupling the model to a future database
or import format.

Substantive content validation lives in `CurriculumValidator` so validation can
report multiple authoring errors for a complete curriculum instead of failing
object construction on the first malformed item. Serialization and local
persistence are handled by the E07 data path while Room-specific metadata stays
out of the curriculum domain models.

## Assessment Domain

The shared assessment model defines focused and mixed assessment configuration,
question attempts, answer identity state, attempt lifecycle, timestamps, and
score summaries without depending on Room, Koin, Compose, Android, or
`CurriculumRepository`. E08 uses `CurriculumRepository` for question selection
and keeps scoring/session behavior separate from persistence.
Question selection follows `AssessmentConfig -> AssessmentQuestionSelector ->
CurriculumRepository`; richer mixed-assessment balancing remains deferred to E10.
The runtime `AssessmentSession` keeps the selected `Question` objects for
scoring, while `TestAttempt` remains the stable-ID attempt record persisted by
the local attempt store without embedding curriculum content.
