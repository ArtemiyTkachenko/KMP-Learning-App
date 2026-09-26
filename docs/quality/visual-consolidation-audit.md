# Visual Consolidation Audit

## Purpose

The visual-polish pass that ran across this repository redesigned surfaces one at a time: the
theme system, assessment taking, the completion hero, Progress, navigation motion, Topic tabs, the
Practice Builder, the Mixed Interview hero, Mistake Review, the shared Question review components,
and finally Saved Questions. Each pass produced a shared primitive, and each shared primitive was
adopted by the surface that produced it.

This audit asks the question a per-screen pass cannot: **which surfaces did not adopt the primitive
that was built for them.** It is deliberately not another screen redesign. The defects it looks for
are a single concept stated in more than one place and drifting — which is how the Saved Questions
task found a bookmark affordance that said "Saved" on three screens and "Remove" on the fourth.

This document is the durable record so a later session reads it instead of re-deriving it. It makes
findings; it does not implement them.

## Baseline

| Field | Value |
| --- | --- |
| Audit date | 2026-09-26 |
| Baseline commit | `44357cf` ("Design improvements", on `task/design-improvements-v16`) |
| Working tree at audit start | Clean |
| Scope | `shared/src/commonMain/kotlin/.../ui/`, `.../ui/theme/`, and every screen and page composable in `:shared` |
| Out of scope | Domain, data, curriculum content, build configuration, the platform shells |

## Method

Static sweeps over the shared UI layer, then measurement where a static sweep can only produce a
suspicion:

1. Which screens switch state through `ScreenStateTransition` and which cut hard.
2. Which weighted-text-beside-trailing-figure rows go through `TrailingFigureRow`.
3. Every `dp` literal and named size token outside `ui/theme/`, grouped by value.
4. Every `Color` literal and `sp` literal outside `ui/theme/`.
5. Every animation spec not built from `AppMotion`.
6. Composable names declared more than once, to find near-duplicate private components.
7. `ScreenAction` versus `ScreenMessage` at each empty state, against the rule in
   [Material Design 3](../development/material-design.md).
8. Container-colour usage, against [surface hierarchy](../development/surface-hierarchy.md).

Findings that depend on how something *looks* were verified by capture, as that document requires:
a throwaway `runSkikoComposeUiTest` rendering the component under `AppTheme` at a stated density,
writing a PNG that was then inspected. The harnesses are not tests and were deleted.

Severity, confidence, and status use the same vocabulary as the
[code quality audit](code-quality-audit.md). Finding IDs are `VC-###`, stable and never reused.

## Finding Ledger

| ID | Severity | Confidence | Area | Finding | Status |
| -- | -------- | ---------- | ---- | ------- | ------ |
| `VC-001` | Medium | High | Shared rows | `PerformanceCard` and `AccuracyRow` break their title inside a word at a doubled type size. | Fixed |
| `VC-002` | Medium | High | Size tokens | One number, twelve declarations: there is no icon-size or stroke-width scale in `ui/theme/`. | Fixed |
| `VC-003` | Low | High | Screen state | Nine of eleven stateful screens still replace their state branches in one frame. | Fixed |
| `VC-004` | Low | Medium | Screen state | The Topic Browser cuts hard between three bodies *inside* its `Content` state, which a class-keyed transition would not cover. | Open |
| `VC-005` | Low | High | Motion | The app's enter/exit state-change pair is written out longhand in two places rather than named on `AppMotion`. | Fixed |
| `VC-006` | Observation | Medium | Empty states | Four of six `ScreenAction` call sites pass an icon; the split looks deliberate but is written down nowhere. | Open |
| `VC-007` | Observation | High | Documentation | Architecture prose describing UI drifts silently when a component is redesigned. | Open |
| `VC-008` | Low | High | Size tokens | `HeroElevation` and the busy-control stroke are each declared in several files; both were left out of `VC-002` because elevation needs a Material question answered first. | Open |

---

## VC-001 — The two shared performance rows break their title mid-word at large type

**Severity** Medium · **Confidence** High · **Status** Fixed

**Files** `ui/PerformanceCard.kt:95` (`PerformanceCard`), `ui/PerformanceCard.kt:238`
(`AccuracyRow`)

`docs/development/material-design.md` records a rule and a component for exactly one shape: a
weighted text column beside a non-weighted trailing figure. A `Row` measures the non-weighted child
first, so the text column can be left with less width than its own longest word, at which point
Compose stops wrapping and starts breaking *inside* the word. `ui/MetricComponents.kt`'s
`TrailingFigureRow` measures `minIntrinsicWidth` and drops the figure below the text when that would
happen.

Three call sites use it: `TopicSubtopicsPage.kt:168`, `TopicBrowserScreen.kt:997`, and — since the
Saved Questions pass — `SavedQuestionsScreen.kt:237`. The two **shared components that carry this
exact shape across the rest of the product** do not. Both are a weighted `Column` of title, detail
and caption, then a `titleLarge` accuracy figure, then an optional chevron, in a plain `Row`.

Verified by capture at 360dp with `Density(density = 1f, fontScale = 2f)`:

| Component | Title given | Rendered as |
| --- | --- | --- |
| `PerformanceCard` | "Structured concurrency" | "Structured concurren / cy" |
| `AccuracyRow` (navigable) | "Structured concurrency" | "Structur / ed concu / rrency" |
| `AccuracyRow` (navigable) | "Kotlin language fundamentals" | "Kotlin language fundame / ntals" |

At `fontScale = 1f` all three render correctly on one or two clean lines, so the defect is scoped
precisely to large type. It reaches every surface that renders these two components: the Progress
dashboard's Topic table and weak areas (via `ProgressPerformanceCard`), the per-Topic Progress
screen, and the Mixed Interview result's Topic breakdown.

`ui/LargeFontScaleTest.kt`'s `theDashboardKeepsItsFiguresInsideTheWindow` does not catch this, and
correctly so: it asserts that nothing leaves the window, and nothing does. The text is shredded
*inside* the window. The sibling assertion that would catch it is the one
`theSubtopicRowMovesItsFigureBelowTheNameAtADoubledTypeSize` already makes — that the figure is
below the text rather than beside it.

**Recommendation** Route both components' header rows through `TrailingFigureRow`, passing the
gap each already uses so nothing moves at an ordinary type scale, and cover each with a large-font
assertion in the existing harness. The chevron needs a decision of its own: it is a fixed 20dp glyph
that does not scale with type, so it can stay beside the text while the figure drops, or travel with
the figure. Decide it once and state it, because both components have one.

**Resolution** Both header rows now go through `TrailingFigureRow` at `ui/PerformanceCard.kt`,
following the shape the Subtopic and Topic rows already use: the `TrailingFigureRow` takes
`Modifier.weight(1f)` inside the existing `Row`, and the chevron stays outside it as a sibling. That
answers the chevron question in the direction the two existing callers had already chosen — a fixed
glyph costs the same width at every type scale and never causes the squeeze, and a navigation
affordance that dropped below the title would stop marking the row as one that travels.

The figure the two components duplicated is now one private `AccuracyFigure`, end-aligned in its own
column. The alignment is invisible until the row reflows and is the point once it does: measured
beside the title the column is exactly its content width, measured below it the column spans the
row, and the alignment keeps the figure in the right-hand column instead of letting it jump to the
leading edge and read as one more line of the text above it. This was caught by capture during
implementation — the first version dropped the figure to the leading edge — and is the convention
`TopicAccuracy` and the Subtopic row already followed.

Protected by `theSharedPerformanceRowsMoveTheirFigureBelowTheTitleAtADoubledTypeSize` in
`ui/LargeFontScaleTest.kt`, which asserts the decision rather than the appearance. It was confirmed
to fail against the previous implementation and pass against the current one.

---

## VC-002 — One number, twelve declarations: there is no size scale

**Severity** Medium · **Confidence** High · **Status** Fixed

**Files** `ui/theme/AppSpacing.kt` and fourteen feature files

`AppSpacing` is the spacing scale and AGENTS.md states the rule plainly: *"If the value you need is
not on a scale, change the scale rather than writing a literal."* There is a scale for spacing, for
shape, for type, for colour, and for motion. **There is none for icon sizes, stroke widths, or
minimum touch targets**, so every file that needs one invents a private name for it.

`20.dp` — the trailing glyph size — is declared **twelve times**:

| Name | Location |
| --- | --- |
| `NavigationChevronSize` | `ui/PerformanceCard.kt:298` |
| `NavigationChevronSize` | `progress/ProgressScreen.kt:766` |
| `NavigationChevronSize` | `topic_study/learning_unit/LearningUnitScreen.kt:392` |
| `NavigationChevronSize` | `topic_study/topics/TopicBrowserScreen.kt:1291` |
| `ChevronSize` | `topic_study/learning_lesson/LearningLessonScreen.kt:1076` |
| `RowIconSize` | `topic_study/topic_detail/TopicStudyPage.kt:383` |
| `SectionAccentSize` | `progress/ProgressComponents.kt:115` |
| `CompletionIconSize` | `assessment_review/AssessmentCompletionHero.kt:311` |
| `StudiedMarkSize` | `topic_study/learning_unit/LearningUnitScreen.kt:391` |
| `ScopeIconSize` | `topic_study/practice_builder/PracticeBuilderScreen.kt:1103` |
| `VerdictIconSize` | `topic_study/practice_builder/PracticeBuilderScreen.kt:1106` |
| `BulletMarkerWidth` | `topic_study/learning_lesson/LearningLessonBlocks.kt:502` |

Five further call sites write `Modifier.size(20.dp)` inline with no name at all:
`progress/ProgressScreen.kt:579` and `:596`,
`topic_study/topic_detail/TopicSubtopicsPage.kt:217`, `topic_study/topics/TopicBrowserScreen.kt:899`
and `:1225`. (A sixth, in `ui/PerformanceCard.kt`, was replaced by that file's own
`NavigationChevronSize` while fixing `VC-001`; the other five stand.) Note that
`NavigationChevronSize` is the *same name for the same value in four different files* — the
strongest possible signal that it wants to be one declaration.

The same pattern repeats at three other values:

- `1.dp` hairline — `HeroBorderWidth` three times (`ui/ContentHierarchy.kt:138`,
  `assessment_review/AssessmentCompletionHero.kt:308`, `progress/ProgressHero.kt:324`),
  plus `WeakBorderWidth`, `GuideWidth`, `LatestDropWidth`, `UnselectedBorderWidth` twice,
  `SummaryBorderWidth`, and one inline `BorderStroke(1.dp, …)`.
- `48.dp` Material minimum touch target — `MinimumTouchTargetSize` three times, plus
  `AnswerRowMinHeight`, `TabHeight`, `OutlineEntryMinHeight`, `BuilderChoiceMinHeight`.
- `18.dp` text-button leading icon — `QuestionActionIconSize`, `ProgressIndicatorSize`,
  `FinishProgressSize`, `StudiedIconSize`.

This is the failure mode `ui/ScreenStatus.kt`'s own doc comment describes one level up, where seven
private centred-state wrappers "had drifted into three different spacings, so the gap between an
error message and its Retry button depended on which screen you were looking at". Nothing has
drifted yet at this level. Twelve copies of a number is how it starts.

**Recommendation** Add a size scale to `ui/theme/` beside `AppSpacing` — the trailing navigation
glyph, the text-button leading icon, the status icon, the hairline border, the minimum touch target
— each entry naming the Material token it comes from, as `AppSpacing` does. Then replace the private
copies. Values that are genuinely about one surface (`AccuracyRingSize`, `LessonOutlineWidth`,
`ComparisonColumnWidth`, the chart's marker geometry) should stay local: the scale is for values the
whole product shares, not for every number.

**Resolution** `ui/theme/AppSizing.kt` now holds three objects, named by where a value is used
rather than by its number, as `AppSpacing` is:

| Entry | Value | What it is |
| --- | --- | --- |
| `AppIconSize.Inline` | 16dp | A glyph set inside a line of body text — a source link's mark. |
| `AppIconSize.Action` | `ButtonDefaults.IconSize` (18dp) | A text button's leading icon, and anything sized to match one. |
| `AppIconSize.Row` | 20dp | A glyph beside a row's text: chevron, completion mark, leading accent. |
| `AppStroke.Hairline` | 1dp | A surface's own edge. |
| `AppStroke.Emphasis` | 2dp | The selected state of a choice. |
| `AppMinimumTouchTarget` | 48dp | For bare `Row`s that get no Material minimum of their own. |

`AppIconSize.Action` is Material's own `ButtonDefaults.IconSize`, quoted rather than restated so it
cannot drift from the buttons it sits in — verified against the pinned sources at
`material3-desktop-1.11.0-alpha07-sources.jar`, `Button.kt:1112`. The touch target stays a plain
`Dp` because Material's current API is the composition local
`LocalMinimumInteractiveComponentSize`, and these call sites apply the value as a `heightIn`
minimum while building a row rather than reading it from composition; the value is the same 48dp.

Fifteen files lost twenty-two private declarations. Five stayed local for the reason the
recommendation gives, and each is a value whose justification is a relationship to something on its
own surface rather than to the product: `TopicMarkerIconSize` is proportional to the 40dp marker
around it, `NavigationIconSize` is a navigation-bar token, `TabHeight` is
`PrimaryNavigationTabTokens.ContainerHeight` and says so — a different token that happens to equal
the touch target — `BulletMarkerWidth` is a prose column rather than an icon, and
`RecentTrendChart`'s `GuideWidth` and `LatestDropWidth` are part of a set of chart proportions
(`LineWidth` is 2.5dp precisely so the data line outweighs the 1dp guide), which folding half of
into a global hairline would have made illegible. `StudiedIconSize` also stayed: its 18dp is
justified by matching a badge on its own page, not by the button token.

One entry the recommendation named was deliberately not added: the empty-state status icon. It is
40dp at exactly one call site, inside `ScreenStatus.ScreenAction`, which every screen already reaches
through that one component — so it is already a single declaration, and the defect this finding
describes is many declarations of one value. A scale step with one caller is not a scale.

Verified as a pure refactor rather than argued to be one: the affected surfaces were captured
before and after, and the PNGs are byte-identical by SHA-256. Every replacement maps a private
`val` to a scale entry holding the same number, so nothing was expected to move, and nothing did.

---

## VC-003 — Nine of eleven stateful screens still cut hard between states

**Severity** Low · **Confidence** High · **Status** Fixed

`ui/ScreenStatus.kt`'s `ScreenStateTransition` exists because *"each branch simply replaced the
last, so content appeared the instant a read finished — a spinner one frame and a full list the
next. That hard cut is what made a fast load look like a glitch and a slow one look broken."*

Two screens use it:

| Screen | Line |
| --- | --- |
| `ProgressScreen` | `progress/ProgressScreen.kt:148` |
| `SavedQuestionsScreen` | `saved_questions/SavedQuestionsScreen.kt:101` |

Nine still switch with a bare `when`:

| Screen | Line | Shape |
| --- | --- | --- |
| `MistakeReviewScreen` | `:104` | `AppScreenPane` + `weight(1f)` branches |
| `ProgressTopicScreen` | `:59` | `AppScreenPane` + `weight(1f)` branches |
| `FocusedResultScreen` | `:70` | `AppScreenPane` + `weight(1f)` branches |
| `MixedInterviewResultScreen` | `:82` | `AppScreenPane` + `weight(1f)` branches |
| `LearningUnitScreen` | `:90` | `AppScreenPane` + `weight(1f)` branches |
| `TopicDetailScreen` | `:304` | `AppScreenPane` + `weight(1f)` branches |
| `LearningLessonScreen` | `:241` | bare `Column` + `weight(1f)` branches |
| `AssessmentTakingScreen` | `:142` | `AppScreenPane` + `weight(1f)` branches |
| `TopicBrowserScreen` | `:216` | `AppContentPane` + `Box`, nested `Content` branching |

The first six are mechanically identical to the two that already adopted it, and the Saved Questions
pass is the worked example: wrap the `when` in `ScreenStateTransition(state, Modifier.fillMaxSize())`
and change each branch's `Modifier.weight(1f)` to `Modifier.fillMaxSize()`. The default
`contentKey = { it::class }` is what makes this safe — a data change inside `Content` keeps the same
key and does not re-fade, which is the property the Mistakes queue and both result transcripts need
just as much as the saved collection did.

Three need judgement rather than the recipe:

- **`LearningLessonScreen`** composes a reading-progress meter above the `when`, but only under
  `Content`. The meter's appearance is itself a state change and would need to be inside whatever
  transitions, or deliberately outside it.
- **`AssessmentTakingScreen`** already owns careful per-question motion through
  `key(state.question.id)`, and has five states rather than four. Class-keying would leave
  question-to-question movement untouched — which is correct — but this is the one screen where the
  interaction is the product, so verify rather than assume.
- **`TopicBrowserScreen`** — see `VC-004`.

**Recommendation** Adopt it on the six mechanical screens in one change, with the existing
loading/empty/error/content assertions as the protection. Treat the Lesson and the taking screen as
separate decisions. Do not add custom transition machinery anywhere: if the shared primitive does
not fit a screen, leave that screen switching as it is and record why.

**Resolution** Eight screens adopted it, taking the count from two of eleven to ten. No custom
machinery was added anywhere and no screen grew a second way of switching.

The six mechanical ones went through unchanged in shape: the `when` moved inside
`ScreenStateTransition(state, Modifier.fillMaxSize())` and each branch's `Modifier.weight(1f)`
became `Modifier.fillMaxSize()`, since inside `AnimatedContent` the scope is a box rather than a
column. `TopicDetailScreen` was the one exception among them and keeps a `Column(Modifier
.fillMaxSize())` inside the transition, because `TopicDetailTabs` is a `ColumnScope` extension — its
pager takes the height the tab row leaves — and that scope had to be restored for the tabs to lay
out as before.

The two judgement cases were both adopted, and answering them produced a rule worth stating:
**the transition covers the screen's body, and chrome pinned under the top bar stays outside it.**
`LearningLessonScreen`'s reading meter and `AssessmentTakingScreen`'s progress meter are both
hairlines under the bar that exist only under `Content`, and both now sit outside the transition for
the same reason the top bar's own title does — the bar is not cross-faded when a screen's state
changes, and a meter pinned to it is part of that bar, not part of the body.

`AssessmentTakingScreen` is the screen the finding said to verify rather than assume, and the
verification is the keying: moving from one question to the next stays inside `Content`, keeps the
same key, and therefore runs no transition at all, so the per-question `key(question.id)` swap and
the answer reveal are untouched. What does now cross-fade is the screen becoming a different kind of
thing — questions arriving, the last answer giving way to the finish prompt, the finish handing over
to results.

`TopicBrowserScreen` was deliberately not converted; it is `VC-004` and still open.

**Protection** `ui/ScreenStateTransitionTest.kt` asserts the contract on the shared primitive rather
than through any one screen, which is the level that covers all ten callers: crossing state classes
leaves the outgoing branch composed one frame later (a transition is running), and a data change
within one class replaces the content outright (no transition). It asserts the decision, never a
duration or an alpha. Confirmed to be real protection by flipping the default `contentKey` from
`{ it::class }` to `{ it }`, which fails the second test and leaves the first passing — exactly the
regression the finding warns about.

---

## VC-004 — The Topic Browser cuts hard inside one state class

**Severity** Low · **Confidence** Medium · **Status** Open

**File** `topic_study/topics/TopicBrowserScreen.kt:221`

`TopicBrowserUiState.Content` branches internally into three visually unrelated bodies: the browse
list with its guided-learning cards, the no-results `ScreenAction`, and the search results list. A
learner typing into the search field crosses all three, and every crossing is a one-frame
replacement of the entire viewport — the most frequently seen hard cut in the app, and the one
`ScreenStateTransition` keyed on the state class would *not* fix, because all three are `Content`.

This is recorded as Medium confidence because the right answer is not obvious. Search results
arriving as the learner types may well be better instant: a body that fades on every keystroke is
worse than one that snaps. The specific transition worth having is probably only
browse ⇄ search — the one that changes what kind of thing the screen is showing — rather than
result-set to result-set.

**Recommendation** Decide it explicitly and write the decision down, rather than leaving it as an
omission. If a transition is wanted, it needs a key that distinguishes the three bodies
(`query.isBlank()`, empty matches, matches) and not the data inside them — the same principle as
`contentKey`, applied one level down.

---

## VC-005 — The enter/exit state-change pair is written longhand twice

**Severity** Low · **Confidence** High · **Status** Fixed

**Files** `ui/ScreenStatus.kt:61`, `AppNavigationBar.kt:460`

Every animation spec in the shared UI is built from `AppMotion` constants and easings — no raw
durations, no ad-hoc curves. That part is healthy and the sweep found no exceptions.

What is not named is the app's enter/exit *pairing*: `fadeIn(tween(StateChangeDurationMillis,
EmphasizedDecelerateEasing))` against `fadeOut(tween(StateChangeDurationMillis / 2,
EmphasizedAccelerateEasing))` — content settling in over twice the time it takes to accelerate
out. `AppMotion` offers `effectSpec`, `revealSpec`, `spatialSpec`, and `offsetSpec`, but nothing for
this, so `ScreenStateTransition` and the navigation bar's snackbar slide each spell it out. They
agree today. `VC-003` proposes adding callers to one of them.

**Recommendation** Name the pair on `AppMotion` when `VC-003` is implemented, so the screens
adopting the transition inherit one definition rather than a third copy. Not worth a change of its
own.

**Resolution** `AppMotion.arriveSpec()` and `AppMotion.departSpec()` now name the pair, and both
call sites use them. One correction to the finding's reasoning: adopting `ScreenStateTransition` on
eight more screens could never have produced a third copy, because those screens inherit the pair
*through* the component rather than restating it — the duplication was only ever the two sites, and
naming it was worth doing on its own terms rather than as a side effect.

`departSpec` takes an optional duration because the navigation bar's exit genuinely needs two: its
fade uses the default half-duration while its slide takes the full one, since a departure that moves
has its own travel to clear and reads as still moving if cut short. That was already true in the
code and is now stated rather than implied.

---

## VC-006 — The empty-state icon split is undocumented

**Severity** Observation · **Confidence** Medium · **Status** Open

`ScreenAction` takes an optional `icon`. Four call sites pass one — `ProgressScreen`
(`AppIcons.Insights`), `MistakeReviewScreen` (`CheckCircle`), `SavedQuestionsScreen` (`Bookmark`),
`TopicBrowserScreen`'s no-results state (`Search`). Two do not: `TopicStudyPage:81` and
`TopicSubtopicsPage:72`.

The split looks principled — the four with icons are whole-screen empties, and the two without sit
inside a Topic detail tab, where a 40dp glyph would outweigh the tab body around it. But that
reasoning exists nowhere; it is currently indistinguishable from an omission, and the next empty
state added has nothing to follow.

**Recommendation** Either state the rule in the empty-states section of
[Material Design 3](../development/material-design.md), or give the two in-tab states icons. State
the rule; do not add the icons by default.

---

## VC-007 — Architecture prose describing UI drifts silently

**Severity** Observation · **Confidence** High · **Status** Open

The Saved Questions pass found `docs/architecture/assessment.md` describing the review save control
as *"a text Save/Unsave control beside the question heading"* — two design passes after it stopped
being a text control and became a bookmark. Nothing failed; prose has no compiler. It was corrected
in that change, along with the component list beside it.

An automated sweep for documentation referencing symbols that no longer exist was attempted and
abandoned: at this repository's level of cross-referencing it produces overwhelmingly false
positives (Material and Kotlin API names, file names, enum values in content documents, classes in
the platform shells), and a signal that noisy is worse than none.

**Recommendation** No tooling. Make it a step of the work instead: when a visual pass changes what a
component *is* rather than how it is arranged, grep `docs/architecture/` for the component's name
before reporting completion. The documentation map in `AGENTS.md` makes that one search, and this
audit is the evidence that it is not automatic.

---

## VC-008 — Elevation and indicator stroke are still per-file

**Severity** Low · **Confidence** High · **Status** Open

**Files** `ui/ContentHierarchy.kt:138`, `progress/ProgressHero.kt:325`,
`assessment_review/AssessmentCompletionHero.kt:310`, `assessment_review/AssessmentRetakeAction.kt:209`,
`assessment_taking/AssessmentTakingScreen.kt:291`

Found while implementing `VC-002` and deliberately left out of it, because neither belongs to the
scale that finding described and one of them asks a question that change could not answer.

`HeroElevation = 2.dp` is declared three times, once in each of the app's three heroes, for one
concept — `ui/ContentHierarchy.kt` states it as *"the lift that makes a hero outrank the cards under
it"*. That is the same defect as `VC-002` in a different axis. It was not folded in because 2dp is
**not** a Material elevation level: M3 puts a resting card at Level1 (1dp) and a raised one at
Level2 (3dp), so this value is already a deviation from the token set, and consolidating three
copies of an undocumented deviation would settle it by accident. The question — is the hero lift a
deliberate deviation worth recording in the deviations table, or should it move onto a Material
level — has to be answered before the value gets a shared name.

`ProgressStrokeWidth` and `FinishProgressStroke`, both 2.dp, are the stroke of a busy control's
spinner, sized so that a ring rather than a disc appears inside an `AppIconSize.Action` circle. They
are two statements of one relationship, and now that the circle they relate to is a shared scale
entry the stroke can reasonably follow it. This is small enough to ride along with the elevation
decision rather than justify a change of its own.

**Recommendation** Answer the elevation question first, in the deviations table of
[Material Design 3](../development/material-design.md), then give both values a home.

---

## Areas With No Material Issue Found

Recorded so a later session does not re-derive them.

- **Colour literals.** No hardcoded colours anywhere outside `ui/theme/`. The only matches are
  `Color.Unspecified` as an "unset" sentinel in `ScreenStatus.ScreenAction`, `Color.Transparent` on
  two hero containers, and `SolidColor(Color.Black)` as the path fill inside `AppIcons`' vector
  builder. Semantic colour goes through `AppThemeExtras.semanticColors` throughout.
- **Type literals.** No `.sp` literals outside `ui/theme/AppTypography.kt`. Every style comes from
  `MaterialTheme.typography`.
- **Motion values.** Every `tween` is built from `AppMotion` durations and easings; every spring
  comes from `AppMotion.spatialSpec`/`offsetSpec`. No raw numbers, no ad-hoc curves. See `VC-005`
  for the one structural nit.
- **Near-duplicate components.** Exactly one composable name is declared twice —
  `TopicPerformanceRow`, in `progress/ProgressScreen.kt:666` and
  `mixed_interview/MixedInterviewResultScreen.kt:258`. Both are four-line adapters over the shared
  `AccuracyRow` that differ only in which model they read and whether the row navigates. That is
  the shared component being used correctly, not duplication.
- **Surface hierarchy.** Container colours are disciplined: `surfaceContainerLow` for cards
  (eight call sites) with a small number of deliberate, commented exceptions. No spectrum of tonal
  levels, no card-inside-a-card at the same level.
- **Empty versus dead-end states.** Every `ScreenMessage` call site is a genuine dead end — attempt
  not found, attempt not completed, Lesson or Unit not found, catalogue empty — and every state with
  somewhere to go uses `ScreenAction`. The rule in `material-design.md` is being followed. The one
  arguable case is `ProgressTopicUiState.Empty` at `ProgressTopicScreen.kt:65`, which states that a
  Topic has no recorded attempts without offering to practise it; whether a target exists there is a
  product question, not a visual one.

## Suggested Order Of Work

`VC-001` first and alone: it is the only finding where a learner using a supported accessibility
setting currently cannot read the screen, and it is contained to one file with an existing test
harness to prove it. **Done** — see its Resolution above.

`VC-002` next, as its own change, because it touches fourteen files and nothing else should be
moving while it does. **Done** — see its Resolution above; it touched fifteen.

`VC-003` and `VC-005` together, since the second exists to serve the first. **Done** — see their
Resolutions above.

`VC-004`, `VC-006` and `VC-008` are decisions to record more than code to write, and can ride along
with any change that touches their surface.

`VC-007` is a habit, not a task.

## What This Audit Concludes

The per-screen visual pass has reached its stopping point. The primitives are right, they are
documented, and where they are adopted they are adopted correctly — the sweep found no drifting
colour, no invented type, no ad-hoc motion, and no duplicated component. What remains is uniformly
the same shape of defect: a shared primitive that a surface predating it never adopted, and a number
that has no scale to live on. None of it needs another screen redesigned. All of it is adoption.
