# Architecture Overview

How the application is composed, which hosts run it, and how curriculum content is modelled.

Sibling notes: [assessment](assessment.md) · [progress](progress.md) · [practice selection](practice-selection.md) · [recommendations](recommendations.md) · [practice builder](practice-builder.md) · [persistence](persistence.md) · [study progress](study-progress.md)

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

The shell exposes four areas — Learn, Interview, Progress, and Mistakes —
through `AppTopLevelDestination`, which maps each to its `AppRoute`. Learn is the visible
label of the area whose internal identity is still `AppTopLevelDestination.TOPICS` and
`AppRoute.Topics`: E21-01 renamed the presentation only, because renaming the route, the
enum constant, the back-stack key, and the `TopicBrowser` classes would have been migration
churn with no product value. Saved Questions is deliberately not a fifth: it is
`AppRoute.SavedQuestions`, a detail pushed onto the Learn stack from a static entry in
the Topic Browser, because saved Questions are learner-curated curriculum content and
belong beside Topic detail and the Practice Builder.

`AppNavigator` owns navigation state and gives **each area its own back stack**. A
single shared stack meant switching away from a detail discarded it, so returning to
an area dropped the learner back at its root; per-area stacks leave each area exactly
where it was left. Back leaves the current area's detail first, then returns to the
start area (Topics), and only then reports the event unconsumed so the host can close
the app. Re-selecting the area already shown returns it to its root.

Which screens keep the navigation control is decided by `AppRoute.showsAreaNavigation()`,
and the rule is one sentence: **normal application mode everywhere except while a question
is actually being answered.** Browsing, reading, configuring, and reviewing are all normal
mode — including the topic and progress-topic details, Saved Questions, the Practice
Builder, the Learning Unit and Lesson study destinations, and both result screens — because
hiding the control on every detail trapped the learner inside an area until they pressed
back. Focus mode is the two assessment-taking routes and their in-progress attempts, where
a one-tap move to another area would abandon unanswered work.

A result screen is deliberately normal mode. The attempt is persisted and scored before
either result route is reached, so there is nothing left to interrupt and a learner reading
their answers back is browsing; leaving them in focus mode meant finishing a practice run
left the app without navigation until they pressed back, which is the arbitrary
appear/disappear behaviour the rule exists to remove.

`AppNavigationScaffold` places that control adaptively: below
`AppNavigationRailBreakpoint` (600.dp, the Material compact/medium boundary) a
`NavigationBar` runs along the bottom edge; at or above it a `NavigationRail` runs down
the leading edge. The decision is made from the measured window width rather than from
the platform, because the same host can be either size — a desktop or browser window can
be dragged narrow.

`AppShellViewModel` supplies the one piece of state the control itself needs: the
unresolved mistake count, badged onto the Mistakes item. The Progress dashboard reports the
same count and routes into the same destination — see [Progress](progress.md) — because a
dashboard that names a queue of questions the learner has already got wrong and then
declines to open it stops one step short of answering "what should I work on next?".

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

`getActiveUnits` is the fourth query, added by E22-05: every ACTIVE Unit in the
document, in global authored order. It exists because sequence across Topics is
information only the undivided document has — concatenating the per-Topic reads
would order Units by Topic rather than as the author wrote them — and Continue
Learning walks exactly that sequence. Both active-Unit queries are derived from
one filtered list inside the loaded document, so they can never disagree about
which Units are ACTIVE.

The Topic Browser is the first consumer of that contract. `TopicBrowserViewModel`
takes `LearningContentRepository` through Koin and, once the assessment catalogue
has loaded, counts `getActiveUnitsByTopic` per Topic into
`TopicBrowserItemUiModel.learningUnitCount`. That count is strictly optional
enrichment: the assessment curriculum remains the authoritative catalogue and is
the only input that can produce Loading, Empty, or Error, so an unreadable
learning document costs a row its availability badge and nothing else. The count
is nullable on purpose — `null` is availability nobody could read, `0` is a
successful read that found no authored material, and only a positive count
renders a badge. It is publisher-owned availability only, and it stays that way:
E22-04 deliberately added no aggregate study progress to the browser, for the
reasons recorded in [study progress](study-progress.md).

The same read now also fetches `getActiveUnits`, in the same `runCatching` and
published in the same step, so the availability markers and Continue Learning
always describe one read of one document. E22-05 added the Continue Learning card
to this screen as a third piece of optional enrichment; it is described in
[study progress](study-progress.md).

Topic Detail is the second consumer, and E21-02 corrected its state model to
make room for it. A Topic is now two independent capabilities — study and
practice — so `TopicDetailUiState.Content` means only that the Topic exists. The
old `NoQuestions` state is gone: it collapsed a Topic with zero ACTIVE Questions
into a terminal message, which would have hidden authored study material on a
studyable but unpractisable Topic. Practice availability is read from
`Content.topicQuestionCount` instead, and `topicPracticeScope()` returns `null`
at zero, so the removed state's guarantee — that an empty scope is never
startable — is preserved as a count check rather than as a screen identity.

Study material reaches that screen through `TopicLearningUnitsUiState`, which is
carried inside `Content` and has its own `Loading`, `Available(units)`, and
`Unavailable`. Three states rather than a nullable list, because
`Available(emptyList())` — the repository answered, this Topic has no authored
Units — and `Unavailable` — nobody could read the document — must never render as
each other. `TopicDetailViewModel` loads the curriculum first, renders, and only
then reads `getActiveUnitsByTopic`, so the Topic is visible and practiceable
while study material is still resolving and stays so if it never arrives. Units
are mapped to `LearningUnitItemUiModel` (id, title, summary, ACTIVE Lesson count)
in repository order, which is authored pedagogical order and is never re-sorted.
Selecting a Unit emits its stable Unit ID through a callback the shell turns into
`AppRoute.LearningUnit`; the parameter stays optional so the screen can also be
rendered outside the shell, where the cards are informational content.

### Topic Detail's three pages

Topic Detail presents those capabilities as three tabs — Study, Practice,
Subtopics — behind a Material 3 `PrimaryTabRow` and a `HorizontalPager`, directly
under the top bar. It was previously one `LazyColumn` holding all three sections
in order, which meant a learner had to scroll to discover that a Topic even had
Subtopics, and which forced a fixed `SubtopicItemOffset`: everything above the
Subtopic rows was deliberately crammed into a single lazy item so a Subtopic
opened from search still landed at `index + 1`. The Subtopics list now holds
Subtopics and nothing else, so the row's index in the state *is* its index in the
list and the offset is gone.

The selected tab is marked by a filled pill rather than by Material's underline
indicator, reusing the `secondaryContainer` treatment the navigation bar already
gives the current area so "this is what you are looking at" reads the same
everywhere. The indicator slot is left empty because `TabRow` places it over the
tabs, where a filled shape would cover its own label; the tab's hover, focus, and
press layer is clipped to the same pill, which matters on the pointer hosts where
hover is a resting state rather than a flash. The pager's fling is pinned to
`PagerSnapDistance.atMost(1)` with a positional threshold below Compose's 0.5
default, so a swipe commits to the neighbouring capability rather than depending
on how hard it was thrown.

Which tab is selected is presentation state and is deliberately neither route nor
ViewModel state. `AppRoute.Topic` gained no field: it already carries an optional
`subtopicId`, and that is the whole initial-selection policy — a Topic opened
with one starts on Subtopics and scrolls to the row, a Topic opened without one
starts on Study. The selection is not even a value of its own. `rememberPagerState`
is the single source of truth, so a tapped tab and a swiped page cannot disagree,
and it is `rememberSaveable`-backed by construction, which is what makes the
selection survive ordinary recreation without anything being serialised. Each
page's scroll state is hoisted beside it, because a pager composes only the pages
in view: hoisting is what lets a learner scroll deep into the Subtopics, check
the Practice page, and come back to where they were.

Each page then carries the content that page is for. Study leads with the Topic's own
lesson-weighted study aggregate — `TopicStudyProgress.summary`, which the derivation
had always produced and nothing had ever rendered — as one figure and one meter above
the Unit list, the same shape `UnitStudyProgress` gives the Unit overview one level
down. The Units themselves state their fractions in words only: a meter per row plus the
header's turned a curriculum into a stack of bars, each redrawing the sentence above it.
Practice keeps its summary card and one filled action, with the targeted shortcuts moved
*inside* the card, where the weak verdict and coverage counts that justify them are — the
arrangement `PerformanceCard` already uses for its action slot on the progress screens.

The unseen shortcut is now offered only for a partially covered scope. `hasUnseenQuestions`
is true of an untouched scope's whole bank, so unseen practice and ordinary practice drew
from one identical pool and the offer appeared on the Topic and on every Subtopic beneath
it at once, distinguishing nothing; requiring something to have been attempted is what
makes it mean "the part you have not reached yet". It carries the remaining count, and the
model property is untouched — it describes coverage, and this is a presentation decision
about when an action is worth showing. A Subtopic row likewise no longer prints both
"0 of N explored" and "Not studied yet", the second being a strict subset of the first.

Terminal states stay outside the pager. Loading, a Topic that does not exist, and
a failed curriculum read are statements about the Topic and still replace the
whole screen; a tab with nothing in it is a statement about one capability and
shows a quiet empty message instead — and, where the empty state is a successful read
rather than a failure, a `ScreenAction` naming the capability the Topic does have. A
failed learning read keeps the plain message: practice is not the answer to "the material
could not be loaded". That is a change from the previous rule,
where an empty section was simply absent — a blank tab reads as broken, and the
tab row is fixed at three so the capabilities stay discoverable and the page
indices stay constant. Nothing about practice scopes, presets, learning-context
derivation, study progress, or Learning Unit navigation changed with the split:
the same callbacks still emit the same `AssessmentScope`s and `PracticePreset`s,
and Continue Learning still navigates straight to its Lesson rather than through
the new Study tab.

E21-03 completes the study path as ordinary detail navigation on the Learn stack:

```text
AppRoute.Topics -> AppRoute.Topic -> AppRoute.LearningUnit -> AppRoute.LearningLesson
```

Both routes carry stable IDs only — the Unit route a Unit ID, the Lesson route a
Unit ID *and* a Lesson ID — and every title, summary, and Lesson list is resolved
from `LearningContentRepository` on arrival rather than serialized into the back
stack. The Lesson route carries its parent because `LearningLessonViewModel`
resolves the Lesson *through* the Unit's authored Lessons: containment is what
makes the parent relationship true, so a valid Lesson paired with the wrong Unit
becomes a controlled `NotFound` instead of opening another Unit's Lesson. It also
gives E21-04 the ordering context previous/next navigation needs.

Both destinations filter for `ContentStatus.ACTIVE` themselves. `getUnitById` and
`getLessonById` deliberately resolve retired content, so browsing eligibility is
presentation's rule rather than the repository's: a deprecated Unit, a deprecated
Lesson, and a Unit/Lesson mismatch all reach the learner as unavailable, while a
document that could not be read is a separate, retryable `Error`. The Unit
overview lists ACTIVE Lessons in authored order and never sorts them, and an
ACTIVE Unit with no current Lessons renders as an empty overview rather than an
error.

E21-04 makes the Lesson destination the reading surface. `LearningLessonUiState.Content`
carries the authored `LearningSection` and `SourceReference` values directly rather than
a parallel UI hierarchy: those models are already presentation-independent, so mirroring
every block variant would duplicate the document model without adding a boundary. The
rule runs one way only — no colour, spacing, or icon may enter `LearningBlock`,
`LearningDepth`, or `LearningCalloutKind`, and every such choice lives in
`LearningLessonBlocks.kt`. That file renders Sections in authored order (never regrouped
by depth), with `CORE`/`PRACTICAL`/`SENIOR` shown as localized learner-facing labels and
the marker drawn once per run of same-depth Sections. Its `when` over `LearningBlock` is
exhaustive with no `else`, so a future authored variant fails to compile until it has a
renderer. Paragraph text is rendered as plain text: no Markdown, HTML, or inline-formatting
parsing exists anywhere in the reader. Code and Comparison blocks each scroll horizontally
inside their own container, so content wider than the window never widens the page, and the
whole Comparison shares one scroll state so its columns stay under their headers. Sources
are optional trailing reference material shown by authored title and opened through
Compose's shared `LocalUriHandler`; a rejected URI is reported beside the link rather than
crashing, and no Source URL ever becomes navigation state.

Previous/next is the current Unit's ACTIVE Lessons in authored order — not global Lesson
order and not `relatedLessonIds` — derived in `LearningLessonViewModel` from the same
filtered list that enforces containment, so a retired Lesson is neither openable nor a
waypoint between two current ones. The first Lesson exposes no Previous and the last no
Next; the controls are absent rather than disabled. The screen emits only a stable Lesson
ID, and the shell answers with `replaceTop` rather than `push`: a Unit is read end to end,
so pushing would leave a learner N Back presses from the Unit overview, and Back must keep
meaning "leave the reader" while Previous means "show the earlier sibling". The reader's
scroll state is keyed on the Lesson ID so a replaced route starts at the top.

Both routes are Learn details rather than a fifth area, so
`showsAreaNavigation()` keeps the navigation control on them, switching areas
preserves the open Unit or Lesson, and re-selecting Learn still returns the stack
to its root. Back is an ordinary `popBack()` from Lesson to Unit to the
originating Topic; nothing reconstructs a route.

E21-06 connects the study path to practice without adding a second quiz engine.
Both reading surfaces offer "Practice this unit", and both push
`AppRoute.PracticeBuilderLearningUnit(unitId)` onto the Learn stack — the Lesson
reader uses the *owning* Unit ID from the route it is rendering, so finishing a
Lesson practises the whole Unit rather than that Lesson. The runtime flow is:

```text
Learning Unit or Lesson -> AppRoute.PracticeBuilderLearningUnit(unitId)
  -> PracticeTargetResolver reads the current Unit
  -> primarySubtopicIds of its ACTIVE Lessons, deduplicated
  -> AssessmentScope.Subtopics -> AssessmentConfig.Focused
  -> the existing focused assessment, result, and retake
```

Supporting concepts never enter that scope, deprecated Lessons contribute
nothing, cross-Topic primary concepts are kept, and the Unit's identity is not
persisted in assessment history — `TestAttempt.config` records the concepts the
run actually asked about, so re-authoring a Unit cannot change a finished
attempt. `PracticeBuilderTarget`, the resolution states, and the missing/empty
handling are documented in [the Practice Builder](practice-builder.md);
`LearningUnitPracticeIntegrationTest` runs the shipped Compose Unit through the
production graph over the bundled curriculum, which is what proves the derived
scope reaches Questions that actually exist.

`LearningReaderJourneyIntegrationTest` drives the whole reading path over the
real `App()` on the shipped learning document, including that reading on replaces
the Lesson entry so one Back press still leaves the reader for its Unit.
`LearningContentEndToEndTest` verifies that whole path on the shipped content —
resource, loader, repository — including authored Unit and Lesson order, stable
identity, Sources, structured blocks, and the cross-Topic supporting concept the
bundled Compose Unit really uses. CI additionally runs
`tools/learning_question_coverage.py --check`, so a learning mapping or Question
change that leaves the committed coverage snapshot stale fails the build.

E21-07 closes the epic with a verification pass rather than a feature. The
product path below is exercised end to end on shipped content — no fixture
stands in for a Unit, a Lesson, or the Question bank behind the builder:

```text
bundled learning content -> Learn browser -> Unit -> Lesson reader
  -> Unit practice -> generic focused assessment -> persisted attempt
  -> result / retake
```

`LearningProductionContentJourneyTest` drives the reading half through the real
`App()` at a phone-shaped window: every structured block type the shipped Unit
authors is seen rendered, a shipped Source reaches the host's `UriHandler` with
its authored URL, the reader's controls are asserted as operable controls
carrying their visible labels rather than as test tags, and "Practice this unit"
reaches the builder from both reading surfaces with area navigation intact.
`LearningUnitPracticeIntegrationTest` carries the same Unit through configure,
start, persist, complete, review, and retake, and states that the retake keeps
the multi-Subtopic scope rather than broadening to the Topic its concepts share.

The ownership boundary the epic establishes, and where it currently stops:

- Lesson content is publisher-owned. It ships in the bundle, is never persisted,
  and is re-resolved on arrival so a re-authored Unit is read as it currently
  reads.
- Assessment history is learner-owned. It records the concepts a run asked
  about, never the Unit that suggested it, so `TestAttempt` carries no Learning
  Unit or Lesson identity and no Room migration was needed to practise a Unit.
- Reading a Lesson still persists nothing. There is no last-read position and no
  activity signal, because opening a Lesson is not studying it.

E22 fills the remaining gap without moving that boundary. Study state is
explicit and learner-owned: `studied_lesson` records the stable Lesson IDs the
learner marked, described in [persistence](persistence.md), and
`StudyProgressDerivation` intersects them with the current ACTIVE hierarchy to
derive Unit and Topic progress on demand, persisting no aggregate. What studied
means, and what happens to a record when a Lesson is re-authored or retired, is
the [study progress](study-progress.md) contract.

E22-04 surfaces that state on the Learn stack. `StudyProgressStateHolder` is a
single app-scoped projection of the study table, registered in
`topicStudyPresentationModule` beside `SavedQuestionStateHolder` and observed by
all three Learn ViewModels — which is what lets a Lesson marked in the reader
update the Unit overview and Topic Detail still alive beneath it, without either
being rebuilt or its publisher document re-read. The Lesson reader carries the
one explicit, reversible mark control; the Unit overview and Topic Detail carry
read-only indicators. Every surface models study state as
`StudyProgressUiState` — `Loading`, `Available`, `Unavailable` — nested inside
its content state, so an unreadable study record costs an indicator rather than a
screen and never touches practice. Nothing is drawn before it is persisted and
read back. Continue Learning is not implemented yet.

## Dates, Counts, And Early-Data States

The app stores every timestamp as a `kotlin.time.Instant`, which is the right thing to
persist and the wrong thing to show: `Instant.toString()` produces
`2026-09-11T12:23:46.872Z`, which states the moment in UTC to the millisecond and answers
nothing a learner asked. Presentation state therefore carries the instant itself —
`CompletedAttemptUiModel.completedAt`, `InterviewAttemptUiModel.completedAt` — and the
phrasing happens in the composable, where the reader's zone and the reader's idea of
"today" are available.

`ui/time` holds one convention: `Today · 13:23` while the day is near enough to name, and
`11 Sep 2026 · 13:23` once it is genuinely a date. Only today and yesterday are named,
because "3 days ago" is harder to place than a date. Every part of it, month abbreviations
and separator included, is a string resource.

The split inside `ui/time` is the load-bearing decision. `localUtcOffset` is an
`expect`/`actual` — four one-line implementations over `java.util.TimeZone`, `NSTimeZone`,
and the browser's `Date` — because `kotlin.time` models an instant but carries no zone
database, and pulling in `kotlinx-datetime` (plus an npm package on the two browser
targets) to answer one question every host already answers was not a trade worth making.
Everything above it — which local day an instant falls on, whether that day is today, how
it reads — is ordinary arithmetic in `commonMain` and takes its offset and its "now" as
arguments, so the rules are pinned by tests that state a zone rather than inheriting the
build agent's.

The same honesty rule applies to counts. Where two figures say the same absence they are
stated once: an untouched Topic card reads `Not started · 28 questions` rather than
`0 of 28 explored` above `Not studied yet`. This is only the all-zero case — the moment
either figure is non-zero the two are genuinely independent, because real historical
accuracy can sit beside zero current coverage after the Questions it was earned on were
retired, and both are then reported normally.

**Learning-unit counts on Topic cards are real, and unevenly present on purpose.** Only
`android_ui` and `async_reactive` currently publish authored Learning Units — six each — so
fifteen of the seventeen Topics carry no unit badge. That is content availability rather
than a rendering bug: `TopicBrowserViewModel` counts `getActiveUnitsByTopic` for every
Topic through the same call, and `learningUnitCount` distinguishes three states —
`null` (the learning document could not be read), `0` (no authored Units yet), and a
positive count. The badge is drawn only for the third, because "0 learning units" is noise
on most rows of a seventeen-Topic list and a guess when the document is unreadable. Nothing
fabricates a count to make the list look uniform.
