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

Shared presentation ViewModels are resolved from the Koin Compose module at the
Navigation 3 destination boundary. Parameterized destinations pass only stable
route data into their ViewModels, while Navigation 3 entry-scoped ViewModel
ownership remains intact: each back-stack entry receives its own
ViewModelStore, and the ViewModel is cleared when that entry is removed.

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

`AssessmentRepository` is the domain-facing boundary for durable
`TestAttempt` snapshots. The local implementation delegates to
`AssessmentAttemptStore`, keeping Room entities and DAOs below the repository
interface. Retake creation is separate orchestration:
`AssessmentRetakeService` loads a completed source attempt, starts a new
assessment with the same `AssessmentConfig`, saves the new in-progress
`TestAttempt`, and leaves the source attempt unchanged. Retakes intentionally
use fresh selection without guaranteeing that questions differ from the
original run.

The completed E08 graph is composed with the same classic Koin DSL as the
curriculum graph: `AssessmentQuestionSelector` depends on `CurriculumRepository`,
`AssessmentEngine` depends on the selector, `AssessmentRepository` persists
`TestAttempt` snapshots through `AssessmentAttemptStore`, and
`AssessmentRetakeService` coordinates the repository plus engine. There is no
separate assessment startup initializer because assessment services are lazy
capabilities; Android still awaits curriculum import before entering `App()`.
E09 and E10 should depend on `CurriculumRepository`, `AssessmentRepository`,
`AssessmentEngine`, and `AssessmentRetakeService`, not Room DAOs or entities.

Focused practice follows `FocusedPracticeViewModel -> AssessmentEngine ->
AssessmentSession`, with the initial and per-answer `TestAttempt` snapshots
saved through `AssessmentRepository` before the UI advances. The final answer
leaves the attempt in `IN_PROGRESS` and exposes `ReadyToComplete`; explicit
completion and result review belong to E09-04.

E09-04 completes the retained session through `AssessmentEngine`, persists the
completed attempt before replacing focused-practice navigation with a stable
attempt-ID result route, and loads historical review through
`AssessmentRepository` plus `CurriculumRepository.getQuestionById`. Deprecated
or missing historical questions are represented per review item without
changing the durable score. Retake behavior remains deferred to E09-05.

Topic detail screens use a Material 3 top app bar for back navigation, with the
navigation icon invoking the existing Navigation 3 back-stack pop. Detail and
practice destinations should keep this phone-style toolbar affordance instead
of rendering a standalone text Back button in page content.
