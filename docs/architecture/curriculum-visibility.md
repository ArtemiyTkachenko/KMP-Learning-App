# Curriculum Visibility

How optional curriculum content — today, Kotlin Multiplatform — is shown or hidden at
runtime without deleting anything. This is the canonical description of the final system.
How it was migrated there, content table by content table and step by step, is the
historical record in the
[KMP content-separation audit](../quality/kmp-content-separation-audit.md).

Sibling notes: [overview](overview.md) · [assessment](assessment.md) ·
[progress](progress.md) · [practice selection](practice-selection.md) ·
[recommendations](recommendations.md) · [practice builder](practice-builder.md) ·
[persistence](persistence.md) · [study progress](study-progress.md)

## The Model: Topic Is The Visibility Boundary

The learner has one switch, *Include Kotlin Multiplatform content*, and it controls one
thing: whether the Topic `kmp` is visible. It is OFF on a fresh install. With it OFF the
app is an Android Engineering curriculum; with it ON, Kotlin Multiplatform is one more Topic
with its own Questions and Learning Units.

```text
Topic is the visibility boundary.
```

Every piece of authored content already has exactly one home Topic: a Question and a
Subtopic carry `topicId`, and a Learning Unit carries its home `topicId`. Topic membership
is therefore the whole classification, and there are no secondary markers:

- no `Question.isKmp`, `Subtopic.isKmp` or `LearningUnit.isKmp`;
- no KMP flag on a route, a `TestAttempt`, a saved Question or a study record.

`CurriculumVisibility` holds the set of hidden Topic IDs, and
`CurriculumVisibility.from(includeKmpContent)` is the **only production code that names
`"kmp"`**, as a private constant. Everything else asks one question:

```kotlin
visibility.isTopicVisible(topicId)
```

That is sufficient because the content is authored so that nothing outside the Topic
depends on it. The next section lists the guarantees that make Topic membership complete.

## Authored-Content Guarantees

A Topic can only be hidden cleanly if the rest of the curriculum stands without it. These
guarantees are enforced by bundled-content tests in `shared/src/jvmTest`, not by the runtime
validators. "`kmp` is optional" is this product's authoring policy rather than a property of
any curriculum, and checking it at build time costs nothing at runtime.

| Guarantee | Enforced by |
| --- | --- |
| A Question's Subtopic belongs to the Question's Topic, so `question.topicId` alone classifies it | The generic `CurriculumValidator` rule `SUBTOPIC_TOPIC_MISMATCH`, asserted clean by `InitialCurriculumSmokeTest` |
| A Lesson in a `kmp` Unit has only `kmp` Subtopics as primary | `BundledLearningCurriculumTest.kmpUnitLessonsPractiseOnlyKmpOwnedSubtopics` |
| A Lesson in a non-`kmp` Unit references no `kmp` Subtopic, primary or supporting | `BundledLearningCurriculumTest.coreLessonsMapNoKmpSubtopics` |
| A non-`kmp` Lesson does not link to a Lesson of a `kmp` Unit | `BundledLearningCurriculumTest.coreLessonsLinkToNoKmpLesson` |
| Learner-visible Android/core text contains no KMP vocabulary | `KmpContentLeakTest`, with the scanner pinned by `KmpVocabularyTest` |

The leak scan reads what a learner reads: Question text, options and explanations
(DEPRECATED Questions included, because attempt history still shows them), and Unit and
Lesson titles, summaries, section titles and every block kind. Source titles, source URLs,
IDs and other authoring metadata are not scanned. AndroidX source paths such as
`.../commonMain/...` are references, not teaching, so they are not leaks. The vocabulary
list and the reasoning behind it are in the audit's
[Step 4](../quality/kmp-content-separation-audit.md#step-4-where-the-invariants-live).

The tests derive KMP Units, Lessons and Subtopics from Topic ownership. A future KMP Unit
is therefore covered without being named.

Because a core Subtopic never gains or loses its primary Lesson when `kmp` is hidden,
Mistake Review's Subtopic → Lesson link is stable across both settings.

## The Preference

```text
AppPreferenceStorage               platform key-value store, one per host
    ↓
KmpContentPreferenceStore          owns the key and the two tokens
    ↓
CurriculumVisibilityStateHolder    Koin single, application lifetime
```

| Contract | Value |
| --- | --- |
| Storage | The host's `AppPreferenceStorage` (the same store as the theme), **not Room** |
| Key | `content.include_kmp` |
| Tokens | `on`, `off` |
| Absent (fresh install) | OFF |
| Unknown token | OFF |
| Writes | Both `on` and `off` are written explicitly, every time |

The theme preference uses absence to mean *follow the system*, which is a real choice. Here
absence only means *never chosen*. If the product default ever became ON, a learner who
deliberately turned the content off must stay off. That is only possible if their OFF was
stored, so it always is.

`CurriculumVisibilityStateHolder` exposes `includeKmpContent` and
`visibility: StateFlow<CurriculumVisibility>`. Like `AppearanceStateHolder`, it reads storage
once, synchronously, in its constructor, so the first curriculum read already sees the
learner's choice and KMP content never flashes on screen. `setIncludeKmpContent` updates the
state first, then writes synchronously. The holder is application-scoped, because the
repository decorators, the history projection, the Topic Browser, the destination guards and
the back-stack pruning pass all share it and it has to outlive every destination.

Settings does not own the preference. `SettingsDestination` resolves the two app-scoped
holders and calls them directly, and there is no Settings ViewModel. The app has exactly two
independent application preferences — theme and curriculum visibility — each with its own
store and holder, and no generic settings framework. See
[Application Preferences](overview.md#application-preferences).

## Runtime Visibility Graph

```text
                          content.include_kmp
                                  │
                                  ▼
                   CurriculumVisibilityStateHolder
                                  │
                        CurriculumVisibility
                                  │
              ┌───────────────────┼────────────────────┐
              ▼                   ▼                    ▼
       ACTIVE curriculum     ACTIVE Units       completed history
              │                   │                    │
 VisibleCurriculumRepository  VisibleLearning   VisibleAssessmentHistory
                              ContentRepository
              │                   │                    │
              └─────────┬─────────┴──────────┬─────────┘
                        ▼                    ▼
               current learning and    derived learner state:
               assessment eligibility  progress, mistakes,
                                       recommendations
                        │                    │
                        └─────────┬──────────┘
                                  ▼
                            UI and routes
                                  │
                       identity-route guards
                                  │
                         back-stack pruning
```

Historical identity resolvers and raw persistence sit underneath this unchanged. Nothing in
the graph deletes or rewrites a stored record.

Filtering happens at these three seams, **below** every selection and derivation, not in
screens. Everything downstream inherits a smaller curriculum and a projected history:

```text
visible curriculum  +  projected history
                    ↓
Progress · Mistakes · Recommended Next · Continue Studying
Assessment selection · Interview history · Result summaries
```

No selection policy, progress derivation, mistake derivation or recommendation policy
contains a Topic-visibility check of its own.

## Eligibility Reads And Identity Reads

This is the central design decision. Both repository interfaces already had two kinds of
read, and visibility applies to exactly one of them.

| Kind | Methods | Answers | Visibility |
| --- | --- | --- | --- |
| Eligibility (current content) | `CurriculumRepository.getActive*`, `LearningContentRepository.getActiveUnits`, `getActiveUnitsByTopic` | *What can the learner use now?* | **Filtered** |
| Identity (historical) | `getTopicById`, `getSubtopicById`, `getQuestionsByIds`, `getUnitById`, `getLessonById` | *What does this stable, persisted ID refer to?* | **Unfiltered** |

Filtering identity reads would be wrong in four ways:

- historical attempts would become unreadable, because their Questions would stop
  resolving;
- saved Questions would look deleted, because a hidden one would be indistinguishable from
  one the curriculum no longer holds;
- study records could no longer resolve their Lessons;
- hiding would become indistinguishable from content deletion. The history projection
  itself needs to resolve a hidden Question in order to know that it is hidden.

Identity reads are not browsing eligibility, and nothing should treat them as such. A
destination that *opens* content by identity applies current visibility itself — see
[Identity-Addressed Routes](#identity-addressed-routes).

## Repository Decoration And DI

The raw sources are bound by their concrete types only. The interfaces that application
code asks for are bound in `curriculumVisibilityModule`, to decorators that wrap them:

| Raw implementation (bound in) | Application-facing interface | Bound to |
| --- | --- | --- |
| `LocalCurriculumRepository` (`curriculumDataModule`) | `CurriculumRepository` | `VisibleCurriculumRepository` |
| `BundledLearningContentRepository` (`learningContentModule`) | `LearningContentRepository` | `VisibleLearningContentRepository` |
| `AssessmentHistoryStore` (`assessmentDataModule`) | `CompletedAssessmentHistory`, for history consumers | `VisibleAssessmentHistory` |

`curriculumVisibilityModule` also binds `KmpContentPreferenceStore` and
`CurriculumVisibilityStateHolder`. All four runtime host startup functions (Android,
Desktop, iOS, web) install it beside `appearanceModule`, and it adds no platform code.
Binding the interfaces only there means a graph without the module fails to resolve them,
rather than silently handing out unfiltered eligibility.

`AssessmentHistoryStore` still implements `CompletedAssessmentHistory` itself. It is the raw
authority for reading, caching, invalidating and failing, and `VisibleAssessmentHistory`
delegates to it. History consumers receive the projection by explicit injection:
`AssessmentQuestionSelector` and `LearningProgressService` in `assessmentDataModule`,
`MistakeReviewService` in `topicStudyPresentationModule`, plus the app-scoped observers
(Progress, Mistake Review, interview history, the navigation badge, Topic Browser and Topic
Detail). The raw store is still injected where only its `invalidate()` is needed, such as
`CompleteAssessment`.

Both decorators:

- filter every ACTIVE read by the content's Topic, keeping repository order;
- short-circuit an ACTIVE read scoped to a hidden Topic to an empty list;
- pass every identity read through unchanged;
- read `visibility.value` per call, so a change applies to the next read;
- override every member explicitly rather than delegating with `by`. A member added to the
  interface later cannot then silently bypass the filter.

## Historical Assessment Projection

`VisibleAssessmentHistory` turns raw completed history into what the learner may currently
see. The rule itself is `VisibleHistoryProjection.visibleAttempts`, and every view of
history runs it:

1. Resolve every Question ID in the history in **one batched** `getQuestionsByIds` (the
   identity read), and only when something is hidden.
2. Classify each answer by its Question's **current** Topic, so a re-homed Question is
   attributed to where it lives now.
3. Drop answers whose Question belongs to a hidden Topic.
4. Drop an attempt that has no visible answer left.
5. Otherwise keep the attempt's ID, configuration, status, timestamps and answer order, and
   recompute its score over the visible answers.
6. Take correctness from the persisted `QuestionAnswerState.Answered.isCorrect`. Answers are
   never re-scored against the current answer key.

An attempt with nothing hidden is returned as the same instance.

**Unresolved IDs are kept.** A Question that no longer resolves has no known Topic. Missing
metadata is not evidence that it belongs to a hidden Topic, and dropping it would turn a
curriculum gap into lost history. Downstream derivations handle unresolved IDs as they
always have.

**The projection is never persisted.** A *projected completed attempt* is an in-memory copy.
The *persisted attempt* is the `TestAttempt` that `AssessmentRepository` returns, and it is
never changed to match a projection.

If Question metadata cannot be read, the observable projection publishes
`AssessmentHistory.Failed`. Neither of the alternatives is safe: the unprojected attempts
could show hidden content, and an older projection could belong to another visibility.
Invalidating the raw store is the retry. The one-shot `completedAttempts()` throws instead.

### Reactive history

```text
VisibleAssessmentHistory.history = combine(AssessmentHistoryStore.history, visibility)
                                     → project
```

Completed history is projected again whenever raw history settles a refresh or visibility
changes. A visibility change therefore:

- re-projects history, and every app-scoped history observer updates from that emission;
- reads no attempt table, because nothing in the database changed;
- does **not** call `AssessmentHistoryStore.invalidate()`. Raw invalidation stays reserved
  for assessment completion and explicit retry.

The projection is shared with `replay = 1` rather than held as a `StateFlow`, which keeps
the store's contract that every settled refresh is an emission, even an unchanged one.

`VisibleAssessmentHistory.snapshots` publishes each projection paired with the visibility
it was made under (`VisibleHistorySnapshot`), and `history` is that flow with the visibility
dropped. The pairing exists for the Topic Browser. On a change, its catalogue reload and the
history re-projection finish in no fixed order, so it could otherwise show an Android-only
catalogue beside Continue Studying, Recommended Next or row context derived from
KMP-visible history. The browser renders history-derived guidance only while the snapshot's
visibility equals its catalogue's visibility, and treats a mismatch as history not yet
arrived. Topic Detail needs no gate: its enrichment is scoped to its own Topic, which is
either unaffected or `NotFound`.

## Hide Is Not Delete

Turning KMP OFF changes one preference key. It does not write, delete or rewrite:

```text
test attempts · question attempts · saved Questions · Lesson study records
```

Turning KMP ON re-exposes everything from those same unchanged records. Mistake state is
derived from the full answer sequence, so a KMP mistake returns exactly as it was.

**Saved Question**

```text
KMP ON  → saved and visible in Saved Questions
KMP OFF → still saved; omitted from Saved Questions
KMP ON  → visible and still saved
```

**Studied Lesson**

```text
KMP ON  → Lesson marked studied
KMP OFF → Lesson absent from the current Learn curriculum; lesson_study row unchanged
KMP ON  → Lesson returns, still studied
```

**Historical attempt**

```text
KMP ON  → the full attempt
KMP OFF → the projected visible subset, score recomputed (or absent if nothing is visible)
KMP ON  → the full attempt again
```

Navigation is the one thing that does not come back: routes pruned from a back stack are
not recreated when content is shown again. See [Back-stack pruning](#back-stack-pruning).

## Current Curriculum Browsing

The Topic Browser catalogue is sectioned in its UI state, not partitioned in the composable:

```text
Android Engineering
Kotlin Multiplatform
```

- `CurriculumSection { AndroidEngineering, KotlinMultiplatform }` names the sections, and
  declaration order is presentation order.
- `CurriculumVisibility.sectionOf(topicId)` assigns a Topic to its section, using the same
  private constant as `from`.
- Android Engineering always shows its heading, even as the only section, so the label does
  not change meaning with a setting (audit decision D-3).
- Kotlin Multiplatform exists only when visible content exists. With KMP OFF the decorator
  has already removed `kmp`, so the section is simply empty and omitted. There is no
  `includeKmp` check in the ViewModel or the UI.
- Topic order inside a section is authored repository order.
- Search stays flat (Topic and Subtopic matches) over the currently visible catalogue.

`TopicBrowserViewModel` and `TopicDetailViewModel` cache ACTIVE reads, so both observe
`visibility` and reload through their existing load paths when it changes. The existing
load-generation contract discards a slower read made under the old visibility. The search
query is ViewModel state outside the catalogue, so it survives the reload. Each reload also
asks the shared `StudyProgressStateHolder` to `refresh()`. A KMP Topic Detail becomes the
existing `NotFound` while hidden and returns to `Content` when shown again.

## Learning Content And Continue Learning

- With KMP OFF, `VisibleLearningContentRepository` removes the `kmp` Units from
  `getActiveUnits` and `getActiveUnitsByTopic`. `getUnitById` and `getLessonById` still
  resolve them.
- Continue Learning walks the visible ACTIVE Units in global authored order. With KMP OFF it
  never targets a KMP Lesson, and "complete" means every Android Lesson is studied.
- With KMP ON, the authored sequence is every core Unit followed by the two KMP Units, so
  Continue Learning reaches KMP only after the Android sequence.
- Study records stay raw. `StudyProgressStateHolder` holds the complete persisted studied set
  and is not filtered. Unit and Topic study progress, and Continue Learning, join it against
  the Units the visible repository returned. See
  [study progress](study-progress.md#curriculum-visibility).

## Derived Learner State

Progress, mistakes, recommendations and selection need no KMP logic because both of their
inputs are already narrowed:

| Consumer | Visible curriculum via | Projected history via |
| --- | --- | --- |
| Progress dashboard and Topic progress | ACTIVE coverage denominator | `LearningProgressService`, `ProgressStateHolder` |
| Mistake Review, its badge, the mistake → Lesson link | `getActiveUnits` for the link | `MistakeReviewService`, `MistakeReviewStateHolder`, `AppShellViewModel` |
| Recommended Next, Continue Studying, row learning context | ACTIVE catalogue | Topic Browser and Topic Detail snapshots |
| Practice selection, every source | ACTIVE candidate reads | `AssessmentQuestionSelector`'s `CompletedAssessmentHistory` |
| Interview history | — | `InterviewHistoryStateHolder` |

The details are in [progress](progress.md), [recommendations](recommendations.md) and
[practice selection](practice-selection.md). Two consequences are worth stating here:

- A KMP-only newest attempt disappears from the projection, so Continue Studying falls
  through to the next older visible context, with no special case.
- In a Mixed interview with KMP ON, `kmp` is one ordinary Topic in the existing round-robin,
  with no weighting (audit decision D-1). With KMP OFF it is absent.

## Saved Questions

Saved Questions filter at content resolution, not in the holder:

- `SavedQuestionStateHolder` keeps every raw saved identity. The result and Mistake cards use
  it to show whether the Question on screen is saved, so it must stay complete.
- `SavedQuestionContentResolver.resolve(savedQuestions, visibility)` applies visibility
  after the identity read:
  - a resolved Question of a hidden Topic is **omitted**;
  - an ID that does not resolve stays `Missing`, because its Topic is unknown;
  - a DEPRECATED Question of a visible Topic stays reviewable, since status plays no part.
- The on-screen count counts only visible entries, both resolved and missing. If every saved
  Question is hidden, the screen is `Empty`.
- A visibility change re-resolves the same saved list. It does not read or write the saved
  table.

This differs from assessment history on purpose. History is projected at an app-scoped seam
because many derivations consume it. Saved state is consumed as raw identity by several
surfaces and resolved into content by one.

## In-Progress And Completed Attempts

Assessment visibility has two rules, and the difference between them is deliberate:

```text
IN_PROGRESS  → atomic
             → any hidden Question makes the session unavailable

COMPLETED    → a historical record
             → safely projected to its visible Questions
```

**New attempts** select through `VisibleCurriculumRepository` and `VisibleAssessmentHistory`,
so a newly created attempt cannot contain hidden content.

**An in-progress attempt** owns its persisted Question sequence. `AssessmentSessionLoader`
resolves it through the identity read. If any Question that resolved belongs to a hidden
Topic, the result is `ContentUnavailable`, which the taking screen shows as `Unavailable`
with no Retry. The attempt is not partially filtered, because numbering, the first
unanswered position, completion and the stored score all count the whole persisted sequence.
Removing a Question for presentation would make the session disagree with storage. The
attempt stays stored unchanged and resumes once the content is visible again.
`AssessmentTakingViewModel` also withdraws a session already on screen as soon as one of its
Questions becomes hidden.

**A completed result** is projected. Hidden Questions are omitted from the review, the score
is recomputed from persisted correctness, the visible transcript keeps its original order,
the Mixed Topic breakdown has no row for a hidden Topic, and a notice states how many
Questions are hidden (audit decision D-2). A result with no visible Question is
`Unavailable`, not `AttemptNotFound`: the attempt exists and returns unchanged when shown.
See [assessment](assessment.md#curriculum-visibility).

## Identity-Addressed Routes

Repository decorators are not enough on their own. A route carries stable IDs, and a route
can be:

- retained on another area's back stack while Settings changes;
- restored after process recreation;
- opened before the setting changed;
- reached through a historical record, such as an interview-history row.

A destination that resolves content through an identity read would show hidden content in
all of these cases. Two layers prevent that.

### Destination guards

Destination guards are the correctness boundary. Every destination that opens content by
identity checks current visibility itself, and observes `visibility` while it is alive:

| Destination | Hidden content becomes |
| --- | --- |
| Topic Detail | `NotFound`, through its ACTIVE reads. No explicit guard needed |
| Learning Unit, Learning Lesson | The existing `NotFound`. A Lesson is judged by its owning Unit's home Topic |
| Practice Builder (Topic, Subtopic, Unit target) | `TargetUnavailable` |
| Progress Topic | `Unavailable` (a visible Topic with no observations stays `Empty`) |
| Focused and Mixed results | `Unavailable` when nothing is visible, otherwise projected |
| Assessment attempt (taking) | `Unavailable` |
| Saved Questions | Hidden entries omitted |

Each guarded ViewModel compares an emission with the visibility its newest load was
requested under, so the `StateFlow`'s replayed value does not trigger a second startup read,
and its existing job cancellation stops an old-visibility read from landing last.

### Back-stack pruning

Pruning is navigation cleanup on top of the guards, so that Back and area switching do not
lead into content that is now hidden. `AppRouteVisibilityResolver.classify(route,
visibility)` returns one of:

| Classification | Meaning | Pruned |
| --- | --- | --- |
| `Visible` | Content resolves and its Topic is visible | No |
| `KnownHidden` | Content resolves and belongs to a hidden Topic | **Yes** |
| `Unknown` | A missing ID, a failed read, an unresolved Question, or a result route whose attempt has not completed | No |

`Unknown` is kept because a failed lookup, a stale identity or incomplete metadata is not
proof of hidden ownership. Such a route reaches its destination's existing NotFound or
Error handling. Ownership always comes from current content, never from how an ID is
spelled. A result route is `KnownHidden` only when the projection leaves nothing of the
attempt.

Pruning rules:

- `pruneRoutesHiddenBy` validates every top-level area's retained stack, not only the area
  on screen.
- Each stack is cut from its **first** `KnownHidden` entry through its top. Entries above it
  were reached through it, so their path no longer exists.
- Roots always remain, and nothing is reconstructed.
- If `Settings` is on top of a pruned stack, it stays open and is rebased directly onto the
  root, so the learner is not thrown out of Settings by the switch they just changed. Back
  then returns to the Topics root.
- The pass runs for the initial, possibly restored, stacks and again on every change.
  `collectLatest` cancels a pass that is still classifying, so a result computed under an
  obsolete visibility cannot prune under a newer one.
- Turning KMP on does not resurrect pruned routes.
- Pruning changes navigation state only. It never cancels or deletes an attempt, unsaves a
  Question, unmarks a Lesson, or rewrites a result.

`AppNavigator` provides only structural operations (`detailRoutes()`, `pruneFrom(invalid)`).
No repository or visibility reaches it. The overview describes where this fits in
[navigation](overview.md#curriculum-visibility-and-the-back-stacks).

## Hidden, Deprecated, Missing, Error

Four states look alike on screen and mean different things:

| State | Meaning | Example consequence |
| --- | --- | --- |
| **Hidden** | Content resolves, but current visibility excludes its Topic | A hidden saved Question is omitted; a hidden Unit route is `NotFound`; a hidden result is `Unavailable` |
| **Deprecated** | Content resolves historically but is not ACTIVE | A DEPRECATED visible saved Question stays reviewable; a DEPRECATED Unit is not browsable |
| **Missing** | A stable identity no longer resolves at all | A missing saved Question stays `Missing`; an unresolved history answer is kept by the projection |
| **Error** | The read itself failed | Route classification is `Unknown` and the route is not pruned; screens show their retryable `Error` |

These distinctions explain the behaviours that look asymmetric. *Missing* is never treated
as *hidden*, because hiding needs positive knowledge of ownership. *Hidden* is never treated
as *missing*, because the content still exists and returns.

## Verification

The documented claims are covered by existing tests (all in `shared/src`; the domain tests
in `commonTest`, everything else in `jvmTest`):

| Area | Tests |
| --- | --- |
| Content boundary and leaks | `BundledLearningCurriculumTest` (the three structural invariants), `KmpContentLeakTest`, `KmpVocabularyTest`, `InitialCurriculumSmokeTest` |
| Preference | `KmpContentPreferenceStoreTest`, `CurriculumVisibilityStateHolderTest`, `JvmAppPreferenceStorageTest`, `DesktopLocalDataPathTest` (the production default) |
| Repository decorators | `VisibleCurriculumRepositoryTest`, `VisibleLearningContentRepositoryTest` |
| History projection | `VisibleHistoryProjectionTest`, `VisibleAssessmentHistoryTest`, `VisibilityDerivationTest` |
| Settings | `SettingsScreenTest`, `SettingsNavigationIntegrationTest` |
| Topic Browser and Topic Detail | `TopicBrowserVisibilityTest`, `TopicBrowserVisibilityIntegrationTest`, `TopicBrowserSectionsScreenTest`, `TopicDetailVisibilityTest` |
| Destination guards | `DestinationVisibilityGuardTest` |
| Route classification and pruning | `AppRouteVisibilityTest`, `AppNavigatorTest`, `AppNavigatorRestorationTest` |
| Saved Questions and in-progress sessions | `SavedAndSessionVisibilityTest` |
| Results | `ResultVisibilityTest` |
| Production graph, end to end | `CurriculumVisibilityIntegrationTest`, `SharedHostStartupTest` |

## Quick Reference

| Question | Answer |
| --- | --- |
| What does the toggle control? | Whether Topic `kmp` is visible. [The Model](#the-model-topic-is-the-visibility-boundary) |
| Why is `topicId == "kmp"` sufficient? | Every item has one home Topic, and the content-boundary tests guarantee that core content does not depend on it. [Guarantees](#authored-content-guarantees) |
| Where is visibility applied? | Two repository decorators, the history projection, destination guards, back-stack pruning. [Graph](#runtime-visibility-graph) |
| Why are ACTIVE reads filtered but identity reads not? | Identity reads resolve history, and filtering them would make hiding a deletion. [Reads](#eligibility-reads-and-identity-reads) |
| How is historical assessment projected? | Hidden answers dropped, scores recomputed from persisted correctness, unresolved IDs kept. [Projection](#historical-assessment-projection) |
| Why does hiding not delete anything? | Only the preference changes, and everything else is a projection. [Hide is not delete](#hide-is-not-delete) |
| How do progress, mistakes, recommendations and selection inherit visibility? | Through already-narrowed inputs. [Derived learner state](#derived-learner-state) |
| How do Saved Questions differ from history? | They are filtered at content resolution, and the holder stays raw. [Saved Questions](#saved-questions) |
| What happens to an in-progress assessment? | `ContentUnavailable`, never partial, and it resumes when shown. [Attempts](#in-progress-and-completed-attempts) |
| How are completed Mixed results shown? | Projected, with a hidden-question notice (D-2). [Attempts](#in-progress-and-completed-attempts) |
| Why are route guards needed as well as decorators? | Routes open content through identity reads. [Routes](#identity-addressed-routes) |
| What happens to retained or restored stacks? | `KnownHidden` routes are pruned; `Unknown` routes are kept. [Pruning](#back-stack-pruning) |
| How does Continue Learning treat KMP Units? | It skips them while hidden and reaches them after core Units when shown. [Learning content](#learning-content-and-continue-learning) |
| Where is the preference stored? | `content.include_kmp` in `AppPreferenceStorage`, not Room. [The Preference](#the-preference) |
| Which layer knows the `"kmp"` ID? | Only `CurriculumVisibility`'s companion. [The Model](#the-model-topic-is-the-visibility-boundary) |
| What prevents KMP leaking into core content? | The bundled-content invariant and leak tests. [Guarantees](#authored-content-guarantees) |
