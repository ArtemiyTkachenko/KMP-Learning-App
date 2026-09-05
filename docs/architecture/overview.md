# Architecture Overview

How the application is composed, which hosts run it, and how curriculum content is modelled.

Sibling notes: [assessment](assessment.md) · [progress](progress.md) · [practice selection](practice-selection.md) · [recommendations](recommendations.md) · [practice builder](practice-builder.md) · [persistence](persistence.md)

## Application Composition

`App()` wraps `AppShell()` in `AppTheme` and takes no dependencies of its own;
everything the shell needs is either navigation state it owns or a ViewModel
resolved from Koin at a destination boundary.

The local curriculum data graph uses Koin because E07 introduced concrete
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

The shell exposes four areas — Topics, Interview, Progress, and Mistakes —
through `AppTopLevelDestination`, which maps each to its `AppRoute`. Saved Questions is
deliberately not a fifth: it is `AppRoute.SavedQuestions`, a detail pushed onto the Topics
stack from a static entry in the Topic Browser, because saved Questions are learner-curated
curriculum content and belong beside Topic detail and the Practice Builder.

`AppNavigator` owns navigation state and gives **each area its own back stack**. A
single shared stack meant switching away from a detail discarded it, so returning to
an area dropped the learner back at its root; per-area stacks leave each area exactly
where it was left. Back leaves the current area's detail first, then returns to the
start area (Topics), and only then reports the event unconsumed so the host can close
the app. Re-selecting the area already shown returns it to its root.

Which screens keep the navigation control is decided by `AppRoute.showsAreaNavigation()`:
browsing screens — including the topic and progress-topic details, and Saved Questions —
keep it, because
hiding it on every detail trapped the learner inside an area until they pressed back.
Screens that own the learner's full attention (an assessment in progress, and its
result) hide it and rely on their own back affordance.

`AppNavigationScaffold` places that control adaptively: below
`AppNavigationRailBreakpoint` (600.dp, the Material compact/medium boundary) a
`NavigationBar` runs along the bottom edge; at or above it a `NavigationRail` runs down
the leading edge. The decision is made from the measured window width rather than from
the platform, because the same host can be either size — a desktop or browser window can
be dragged narrow.

`AppShellViewModel` supplies the one piece of state the control itself needs: the
unresolved mistake count, badged onto the Mistakes item. The Progress dashboard reports
the same count as plain text rather than as a second button, so one destination has one
control.

Navigation motion is declared in `AppNavigationTransitions` rather than left to
Navigation 3's defaults, which animate on Android but resolve to `EnterTransition.None`
on desktop, iOS, and web.

Shared presentation ViewModels are resolved from the Koin Compose module at the
Navigation 3 destination boundary. Parameterized destinations pass only stable
route data into their ViewModels, while Navigation 3 entry-scoped ViewModel
ownership remains intact: each back-stack entry receives its own
ViewModelStore, and the ViewModel is cleared when that entry is removed.

All runtime hosts share `AppRoot(initialize)` in shared `commonMain`. It owns the
startup loading, failure, and retry states around the platform initializer and
then enters `App()`. Hosts start Koin before composition and supply only their
own initializer, so a failed initialization cannot leave a host without
content. `App()` keeps its own `MaterialTheme` so it remains usable directly in
tests and previews that bypass `AppRoot`.

### Runtime Host Coverage

`App()` and the common product graph are used by every configured runtime host:

| Host | Koin graph started by | Database builder | Runnable |
| --- | --- | --- | --- |
| Android | `KmpLearningApplication` -> `startAndroidLocalDataGraph` | `CurriculumDatabase.android.kt` | yes |
| Desktop (JVM) | `desktopApp/main.kt` -> `startDesktopLocalDataGraph` | `CurriculumDatabase.jvm.kt` | yes |
| iOS | `MainViewController` -> `startIosLocalDataGraph` | `CurriculumDatabase.ios.kt`, bundled SQLite | yes |
| Web JS | `webApp/main.kt` -> `startWebLocalDataGraph` | `CurriculumDatabase.web.kt`, SQLite worker/OPFS | yes |
| Web Wasm | `webApp/main.kt` -> `startWebLocalDataGraph` | `CurriculumDatabase.web.kt`, SQLite worker/OPFS | yes |

Each startup function installs `curriculumDataModule`, `learningContentModule`,
`assessmentDataModule`, `savedQuestionDataModule`, and
`topicStudyPresentationModule` plus exactly one platform database module.
The host then composes its thin platform root, which delegates initialization to
the common `AppRoot` state machine. Database creation and platform storage stay
below the shared repository boundary; `App()` does not start Koin or select a
database.

The JS and Wasm executables share the Room builder and Koin module in `webMain`.
Only worker construction differs in `jsMain` and `wasmJsMain`. The repository's
`sqliteWasmWorker` module packages the official-style SQLite WASM worker used by
`WebWorkerSQLiteDriver`; the worker opens the stable `curriculum.db` in OPFS.
The browser must support OPFS and SharedArrayBuffer in a secure,
cross-origin-isolated context. The development webpack server supplies COOP and
COEP headers; production hosting must do the same.

Kotlin/Native iOS compilations remain disabled on the Linux CI runner. iOS
framework linking and simulator runtime verification are therefore local macOS
checks rather than Linux CI guarantees.

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

`Question.selectionMode` is authored curriculum data and flows through local
persistence into assessment presentation. It is deliberately independent from
`correctAnswerIds`: interaction controls must not reveal answer-key cardinality,
and scoring continues to compare selected and correct answer-ID sets exactly.

## Learning Content Model

Explanatory study material is a second authored curriculum, bundled as its own
Compose resource and deliberately kept apart from the assessment curriculum. The
two have different hierarchies and different lifecycles, so they are never merged
into one document:

```text
initial_curriculum.json          learning_curriculum.json
  -> CurriculumJsonCodec           -> LearningCurriculumJsonCodec
  -> Curriculum                    -> LearningCurriculum
  -> CurriculumValidator           -> LearningCurriculumValidator (against Curriculum)
  -> CurriculumImporter            -> LearningContentLoader
  -> Room                          -> validated in-memory document
  -> LocalCurriculumRepository     -> BundledLearningContentRepository
  -> CurriculumRepository          -> LearningContentRepository
```

Learning content is publisher-owned static content, so it is not persisted:
there are no learning-content Room tables, no migration, and no startup
initializer. `LearningContentLoader` validates the learning document against the
bundled base `Curriculum` rather than the imported Room copy, so authored Topic
and Subtopic references are checked against the exact taxonomy they were written
against, and validation stays off the persistence path.

Validation is all-or-nothing. A bundle that fails to decode or fails validation
raises `LearningContentLoadException` carrying a `LearningContentLoadFailure`,
which keeps a malformed bundle distinguishable from a valid bundle that contains
no Units. `BundledLearningContentRepository` loads and validates once per
instance behind a mutex and answers later queries from immutable indexes, so a
failed load leaves no partial content behind.

`getActiveUnitsByTopic` is the browsing eligibility surface and returns only
ACTIVE Units for a home Topic in authored order; `getUnitById` and
`getLessonById` resolve stable identity regardless of status. This mirrors the
split `CurriculumRepository` already makes between active selection and
historical resolution. A Unit's home Topic decides where it is browsed and does
not constrain the Topics its Lessons reference.

`LearningContentEndToEndTest` verifies that whole path on the shipped content —
resource, loader, repository — including authored Unit and Lesson order, stable
identity, Sources, structured blocks, and the cross-Topic supporting concept the
bundled Compose Unit really uses. CI additionally runs
`tools/learning_question_coverage.py --check`, so a learning mapping or Question
change that leaves the committed coverage snapshot stale fails the build.
