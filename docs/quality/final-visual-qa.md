# Final Visual QA

## Purpose

The visual-polish pass and the
[visual consolidation audit](visual-consolidation-audit.md) both asked structural questions:
which surfaces adopted which primitive, which number is declared twice, which screen still
cuts hard between states. This pass asks the one question neither could: **does the
accumulated work still hold up when it is actually rendered.**

It is not another redesign, and it is deliberately not a screenshot-testing framework. It
renders a representative matrix of screens under `AppTheme` at stated windows, themes and
font scales, writes PNGs, and looks at them together. Findings are only filed for something
observable in a render or in a measured node; "could be prettier" is not a finding.

## Baseline

| Field | Value |
| --- | --- |
| QA date | 2026-09-27 |
| Baseline commit | `c9c15db` ("Task/design improvements v16", on `task/redesign-audit`) |
| Working tree at QA start | Clean |
| Scope | Every screen and page composable in `:shared`, rendered |
| Out of scope | Domain, data, curriculum content, build configuration, the platform shells |

## Method

Three throwaway `runSkikoComposeUiTest` harnesses rendered each screen inside the real
`AppNavigationScaffold` — so the window margin, the window size class and the compact
navigation overlay were the production values rather than the composition-local defaults —
and wrote a PNG per configuration. Two probes measured semantics-node bounds where a PNG
could only produce a suspicion. The harnesses were deleted afterwards; they are not tests,
and this repository does not keep golden images.

Everything below was verified from a render or a measurement. Nothing was inferred from
reading the source alone.

### Visual matrix inspected

| # | Screen | Configurations rendered |
| --- | --- | --- |
| 1 | Topic Browser (populated, guidance + catalogue) | light/dark compact, light/dark expanded, light compact at 2× |
| 2 | Topic Detail — Study, Practice, Subtopics tabs | light compact each tab, dark Study, expanded Study, 2× Study and Subtopics |
| 3 | Learning Unit | light/dark compact, light compact at 2× |
| 3 | Learning Lesson (paragraph, bullets, code, comparison, three callout kinds, three depths) | light/dark compact, light expanded (with outline), light compact at 2× |
| 4 | Practice Builder | light/dark compact, unavailable-source configuration, light expanded, light compact at 2× |
| 5 | Assessment Taking | unanswered, selected-unsubmitted, correct evaluated, incorrect evaluated with the correct answer revealed; dark and expanded for the incorrect state; 2× |
| 6 | Completion / Result | strong (18/20), mixed (12/20), weak (6/20); dark; expanded; 2× |
| 6 | Focused result and Mixed interview result, with transcript | light/dark compact, expanded, scrolled into the review list |
| 7 | Progress (hero, trend chart, coverage, weak areas, topic performance, history) | light/dark compact, light/dark expanded, light compact at 2×, scrolled to the list tail |
| 8 | Mixed Interview | no-history and populated, light/dark compact, expanded, 2×, scrolled to the record |
| 9 | Mistake Review | populated queue with expanded Questions, light/dark compact, expanded, 2× |
| 10 | Saved Questions | three collapsed entries, light/dark compact, 2×, node bounds measured at 1× and 2× |
| 11 | Settings | light, dark, expanded |

Compact is 380×820dp; expanded is 1280×900dp; large type is `Density(density = 1f,
fontScale = 2f)` so a failure is unambiguously about type size rather than a smaller window
in disguise.

## Finding Ledger

| ID | Severity | Confidence | Screen / component | Configuration | Status |
| -- | -------- | ---------- | ------------------ | ------------- | ------ |
| `VQA-001` | High | High | `MetricRow`, `ProgressHero.HeroCoverage` | compact, 2× | Fixed |
| `VQA-002` | High | High | `InterviewStartScreen.InterviewRecord` header | compact, 2× | Fixed |
| `VQA-003` | High | High | `TopicDetailScreen.TopicDetailTab` | compact, 2× | Fixed |
| `VQA-004` | Medium | High | `SavedQuestionsScreen` bookmark | compact, 2× | Fixed |
| `VQA-005` | Observation | High | Compact navigation labels | compact, 2× | Left unchanged |
| `VQA-006` | Observation | High | Weak-area card colouring | expanded, both themes | Left unchanged |
| `VQA-007` | Observation | Medium | Mistake Review semantic density | compact and expanded | Left unchanged |
| `VQA-008` | Observation | High | Page-level tab focus band | compact, light | Left unchanged |
| `VQA-009` | Low | High | `TopicDetailScreen` tab labels | compact, 2× | Open — the one follow-up |

---

## VQA-001 — The Progress hero shreds its figures one digit per line at a doubled type size

**Severity** High · **Confidence** High · **Status** Fixed

**Files** `ui/MetricComponents.kt:415` (`MetricRow`), `progress/ProgressHero.kt:262`
(`HeroCoverage`)

**Configuration** 380dp window, light, `fontScale = 2f`. Reproduces in dark and at any
window narrow enough that the label's own longest line fills it.

**Evidence** `07-progress-light-380-x2.0.png`. The hero renders:

```
Completed sessions   9
Questions answered   1
                     4
                     0
Correct answers     98
Curriculum coverage  2
                     9
                     .
```

`140` is drawn as three stacked digits and `29.2%` as a vertical column. The figures are
the reason a learner opens Progress, and at a supported accessibility setting they are not
readable.

**Cause** Both rows are a plain `Row(horizontalArrangement = Arrangement.SpaceBetween)`
holding two unweighted `Text`s. A `Row` measures its children in order with the remaining
width, so the label — which has no weight and no maximum — takes the whole row, and the
value is measured against what is left, which is close to zero. Compose then stops wrapping
and breaks inside the string.

This is the defect `VC-001` closed for `PerformanceCard` and `AccuracyRow`, one component
along. `ui/MetricComponents.kt` is the file that *holds* `TrailingFigureRow`, and `MetricRow`
sits in it without using it.

`ui/LargeFontScaleTest.kt`'s `theDashboardKeepsItsFiguresInsideTheWindow` does not catch it,
correctly: it asserts that nothing leaves the window, and nothing does — the digits are
shredded inside it.

**Proposed fix** Route both through `TrailingFigureRow`, which is the repository's stated
rule for exactly this shape, with the value in an end-aligned `Column` so it keeps its
column when it drops. Nothing moves at an ordinary type scale, because the component only
reflows when the text would otherwise be left with less width than its longest word.

**Resolution** Both now go through `TrailingFigureRow`. `MetricRow` keeps its
`semantics(mergeDescendants = true)` on the row, so the label and value are still announced
as one reading. `HeroCoverage`'s title row passes the whole trailing `Text` as the figure
and composes nothing when the percentage is absent, which is the `figure` slot's documented
"there is nothing to place beside" case and leaves the unavailable-coverage state unchanged.

What the re-render shows is worth stating, because it is not what the finding predicted.
`TrailingFigureRow` does not drop these figures below their labels: it measures the label's
own longest word, finds that it fits beside the figure, and keeps them side by side —
handing the label the remaining width to *wrap* in. So "Questions answered" becomes two
lines with "140" intact beside it, which is better than either the old render or a stacked
one. The component only stacks when even the longest word will not fit.

At an ordinary type scale nothing moved at all: the Progress dashboard's light and dark
compact and expanded renders are byte-identical before and after.

Protected by `theProgressHeroKeepsItsFiguresOnOneLineAtADoubledTypeSize` in
`ui/LargeFontScaleTest.kt`. It asserts the decision rather than the appearance, and the
property it asserts is a shape rather than a measurement against a font: a figure broken one
character per line is far taller than it is wide, and an intact one is not. Confirmed
against the previous implementation, where it fails with *"the hero's coverage percentage is
3.0x240.0px, taller than it is wide"*.

That test was corrected once after it was first written, and the correction is worth
recording because it is a trap this harness invites. It originally rendered in the file's
360x640dp window, where at a doubled type size the coverage percentage sits *just* below the
fold — so `assertIsDisplayed` inside the assertion depended on the host's font metrics, and
the test passed on macOS and failed on CI with *"'29.2%' is not displayed"*. The defect is
horizontal, so the narrow 360dp measure is the part that matters and is unchanged; the
height was never part of the subject. It now renders in a window tall enough to hold the
whole hero, which also makes its failure against the defect the honest one — the full
3x240px stacked column rather than a visibility accident.

---

## VQA-002 — The interview record's "4 completed" renders as a column of single letters

**Severity** High · **Confidence** High · **Status** Fixed

**File** `mixed_interview/InterviewStartScreen.kt:557` (`InterviewRecord`)

**Configuration** 380dp window, light, `fontScale = 2f`.

**Evidence** `P1-interview-record-light-380-x2.0.png`. "Your interview record" takes the
full width and "4 completed" is broken to one character per line — a column roughly 400px
tall — which pushes the record group it heads most of a screen further down.

**Cause** Identical to `VQA-001`: a `SpaceBetween` `Row` with two unweighted `Text`s. It is
worse here only because the trailing string is words rather than digits, so there are more
characters to stack.

**Proposed fix** The same primitive, for the same reason.

**Resolution** The header row goes through `TrailingFigureRow` with the count in an
end-aligned `Column`. At a doubled type scale the count stays whole on one line at the
trailing edge and the heading wraps beside it, so the record group sits immediately under
its heading again instead of most of a screen below it.

One rendered change at an ordinary type scale, measured rather than asserted to be safe: the
count moved down about two pixels, because the `SpaceBetween` row had no `verticalAlignment`
and therefore top-aligned a `bodyMedium` count against a `titleMedium` heading, while
`TrailingFigureRow` centres them. The diff is 600 pixels — the label's own 83x15px box, and
nothing else on the screen — at a maximum channel delta that is entirely glyph antialiasing.
Both themes and the expanded layout show the same one box and no other difference.

Protected by `theInterviewRecordHeadingKeepsItsCountWholeAtADoubledTypeSize` in
`ui/LargeFontScaleTest.kt`, asserting the same shape property as `VQA-001`. Against the
previous implementation it fails for a second reason worth recording: driven to the record,
the test cannot find "4 completed" *displayed at all*, because the stacked column had pushed
it out of the viewport.

---

## VQA-003 — The Topic detail tabs clip their labels in both axes at a doubled type size

**Severity** High · **Confidence** High · **Status** Fixed

**File** `topic_study/topic_detail/TopicDetailScreen.kt:203` (`TopicDetailTab`)

**Configuration** 380dp window, light, `fontScale = 2f`. Both themes; reproduces on any
window where three equal cells cannot hold the longest label at the current type size.

**Evidence** `02-topic-study-light-380-x2.0.png` and `02-topic-subtopics-light-380-x2.0.png`.
The row renders `Study` / `Practic` / `Subtopi`: two of the three labels are cut mid-word
with no ellipsis, and the selected label's descender is sliced flat by the bottom of the
cell — the `y` of "Study" is cut through by the indicator rule.

**Cause** One line: the tab takes `Modifier.height(TabHeight)`, a hard 48dp. 48dp is right
as a *minimum* — it is both `PrimaryNavigationTabTokens.ContainerHeight` and the Material
touch target — but as a fixed height it cannot hold a line of type that has grown, and it
cannot hold the second line the label would otherwise wrap onto. With no vertical room to
wrap into, the label is clipped horizontally instead; with no room for the line box, the
descender is clipped too.

**Proposed fix** `heightIn(min = TabHeight)`. `PrimaryTabRow` sizes itself from its tallest
tab, so the row grows with the type and the labels wrap rather than truncate. The 48dp
minimum, and therefore the touch target and the Material container height, are unchanged at
every ordinary type scale.

**Resolution** Exactly that, and nothing else. Verified by re-rendering: at `fontScale = 1f`
the row is still 48dp tall and every Topic detail capture is byte-identical; at
`fontScale = 2f` nothing is cut any more — all three labels are present in full and the
selected label's descender is whole.

**This closes the clipping and not the wrapping.** At 2× on a 380dp window the longest label
still wraps *inside the word*, as "Subtopi / cs". That is a separate, lower-severity
residual and is filed as `VQA-009`; it is not fixable by a constraint on this component.

Protected by `theTopicTabsGrowWithTheTypeSizeRatherThanClippingTheirLabels` in
`ui/LargeFontScaleTest.kt`, which asserts the decision in both directions: at an ordinary
type scale the cell is still exactly the Material tab height, and at a doubled one it is
taller. Asserting that a label is not clipped would mean asserting the host's font metrics —
and worse, a clipped `Text` reports the bounds it was clamped to rather than the bounds it
wanted, so the honest observable is the cell. Against the previous implementation it fails
with *"the tab is 48.0px tall at a doubled type size, which is still the fixed 48.0px
cell"*.

---

## VQA-004 — A stacked saved-question bookmark becomes a full-width control

**Severity** Medium · **Confidence** High · **Status** Fixed

**File** `saved_questions/SavedQuestionsScreen.kt:237`

**Configuration** 380dp window, `fontScale = 2f`.

**Evidence** Measured node bounds, `savedBookmarkBoundsWhenStacked` and
`savedBookmarkBoundsAtOrdinaryType`:

| Type scale | `savedQuestionRemoveTag("q1")` bounds | Width |
| --- | --- | --- |
| 1× | `(260, 204) – (348, 244)` | 88px, against the trailing edge |
| 2× | `(32, 548) – (348, 604)` | 316px, the whole card |

and `10-saved-light-380-x2.0.png`, where the "Saved" label reads centred rather than at the
trailing edge it occupies at ordinary type.

**Cause** `TrailingFigureRow`'s stacked branch measures the figure at the row's full width
on purpose, so that a figure which aligns itself to the trailing edge stays in the right-hand
column instead of jumping to the leading edge. This call site passes the `TextButton` itself
as the figure rather than an end-aligned `Column` around it, so the button — not a column
containing it — is what takes the full width. Its content then centres inside it and its hit
area spans the card.

The consequence is a control whose target is four times the affordance that draws it: the
whole line under the question becomes "remove from saved", with a small centred label as the
only indication of where it is. The alignment is the visible half of the same bug.

[Material Design 3](../development/material-design.md) states the rule — *"the figure aligns
itself to the trailing edge, in its own `Column(horizontalAlignment = Alignment.End)`"* — and
the other three callers follow it. This one predates the rule being written down.

**Proposed fix** Wrap the bookmark in `Column(horizontalAlignment = Alignment.End)`, which
is the convention and one line.

**Resolution** Exactly that. The column takes the full width in the stacked branch, the
button keeps its content width inside it, and the control returns to the trailing edge at
every type scale. Re-measured:

| Type scale | Bounds | Width |
| --- | --- | --- |
| 1× | `(260, 204) – (348, 244)` | 88px — unchanged |
| 2× | `(218, 548) – (348, 604)` | 130px, against the trailing edge at 348 |

130px at 2× against 88px at 1× is the label at twice the type size, which is correct. The 1×
render is byte-identical.

Protected by an extension to the existing
`theSavedQuestionBookmarkMovesBelowALongQuestionAtADoubledTypeSize` in
`ui/LargeFontScaleTest.kt`: the bookmark stays below the text *and* stays narrower than the
row it sits in. Against the previous implementation the second assertion fails with *"the
bookmark is 296.0px wide inside a 296.0px row"*.

---

## VQA-005 — Compact navigation labels ellipsize at a doubled type size

**Severity** Observation · **Confidence** High · **Status** Left unchanged

**Evidence** `01-topics-light-380-x2.0.png` and every other 2× compact capture: the four
destinations read "Learn", "Inter…", "Prog…", "Mist…".

This was reviewed and deliberately not changed. The label is ellipsized, not clipped — the
word is marked as truncated rather than cut mid-letter — the icon above it still names the
destination, the accessibility label is the full string, and `AppNavigationBarTest` holds
the 48dp target and the equal centred slots, none of which move. Four destinations across a
380dp bar have about 90dp each; a full 2× label does not fit in that and no arrangement of
the existing bar makes it fit. The alternatives — dropping the labels, scrolling the bar, or
stacking it — are all larger changes than the defect, and the defect is that a label the
icon already carries is shortened.

Recorded so the next session does not re-derive it.

---

## VQA-006 — Weak-area cards pair an amber edge with a red figure

**Severity** Observation · **Confidence** High · **Status** Left unchanged

**Evidence** `07-progress-light-1280.png`, `07-progress-dark-1280.png`. A weak-area card
carries `partiallyCorrect` on its border and its section icon, while the accuracy figure
inside it resolves through `accuracyColor` to `incorrect`.

Two semantic colours on one card looks at first like the aggressive use this pass was asked
to look for. It is not: they say different things. The border and the heading icon mark the
card as *flagged by the weakness rule* — a warning — and the figure states *what the rate
is*, which at 41.7% is genuinely in the bottom band. Recolouring either to match the other
would make the card state one fact twice and lose the other.

Dark was checked specifically for neon and does not have it: the same two roles render as a
desaturated gold and a muted salmon, and the containers stay near-black.

---

## VQA-007 — Mistake Review repeats an incorrect container once per card

**Severity** Observation · **Confidence** Medium · **Status** Left unchanged

**Evidence** `09-mistakes-light-380.png`, `09-mistakes-light-1280.png`,
`09-mistakes-dark-380.png`.

Every card on this screen is a mistake, so every card draws one `incorrect` container. A
screen that is mostly pale red is the shape of a punitive surface, which is the risk the
brief names.

Reviewed and left alone, because the rendering does not actually read that way and the
reason is the ordering. Each card puts the *correct* answer first, in a white container with
a green edge, and the chosen wrong answer second; the explanation follows immediately. The
red is the second thing on each card rather than the first, and it is a pale tint carrying a
1dp edge rather than a saturated block — which is the restraint
[Material Design 3](../development/material-design.md) asks for and which the palette is
already delivering. The screen's own heading is a neutral level-2 surface with a Practice
button, not a red banner.

Confidence is Medium rather than High because this is a judgement about tone, and the right
evidence for it is a learner rather than a capture. Nothing measurable is wrong.

---

## VQA-008 — The page-level tab keeps a focus band after a click

**Severity** Observation · **Confidence** High · **Status** Left unchanged

**Evidence** Measured from `02-topic-practice-light-380.png` and
`02-topic-subtopics-light-380.png`: the clicked tab's cell renders `#E7E9F5` against the
`#F6F7FC` page, while the two tabs that were not clicked stay on the page colour.
`02-topic-study-light-380.png`, captured without driving any input, shows no band on any tab.

This is Material's own focus state layer on the cell that was just activated, and
`TopicDetailTab` records keeping it as a deliberate decision — with no pill of its own,
the tab keeps Material's full-cell hover, focus and press so that the whole cell is visibly
the target on a pointer host. It is correct behaviour and it is what a Desktop learner will
actually see after clicking a tab.

Filed only because it is an artefact a later capture session will see again and may mistake
for a stray container.

---

## VQA-009 — The longest tab label still wraps inside the word at a doubled type size

**Severity** Low · **Confidence** High · **Status** Open — the one follow-up this pass leaves

**File** `topic_study/topic_detail/TopicDetailScreen.kt`

**Configuration** 380dp window, `fontScale = 2f`. Both themes.

**Evidence** The re-render after `VQA-003`: the row reads "Study" / "Practice" /
"Subtopi / cs". Nothing is clipped and no content is lost — the word is whole across two
lines — but it is broken at no word boundary, which the brief names as a large-font failure
in its own right.

**Cause** `PrimaryTabRow` divides the row into three equal cells, so on a 380dp window each
tab has about 126dp less its label padding. At 2× the word "Subtopics" is wider than that,
and a single word wider than its line has nowhere to break except inside itself. `VQA-003`
gave the cell the vertical room to wrap into, which is why the label is now present rather
than cut; it cannot give it horizontal room, because the width is a third of the window.

**Why it was not fixed here** Three things were measured or considered and rejected:

- **Tightening the label padding** from `AppSpacing.Related` to `AppSpacing.Tight` was
  tried and re-rendered. It buys 8dp, which is enough for "Practice" and not for
  "Subtopics" — so it fixes one of the two labels. It is also exactly the font-scale
  threshold-hunting that [Material Design 3](../development/material-design.md) warns
  against in the `TrailingFigureRow` section: a value chosen because it happens to clear one
  English string at one scale on one window. It was reverted.
- **Reducing the type** is not available: a large-font defect is not solved by making the
  type smaller.
- **A scrollable tab row** is the answer that actually works, and it is not a small change.
  `TopicTabIndicator` positions itself from `constraints.maxWidth` as the cell width and
  `position * tabWidth` as the offset, which is only correct while the cells are equal. A
  scrollable row sizes each tab to its content, so the pager-linked indicator — the whole
  point of a previous pass — would have to be rewritten to read per-tab geometry. That is a
  task, not a fix, and doing it inside a QA pass would be the redesign this one was told not
  to start.

**Proposed fix** One task: move the Topic detail tabs to a content-sized tab row and
re-derive the pager-linked indicator's position from the tabs' own geometry rather than from
an assumed equal cell width, keeping the drag coupling and the at-rest alignment that
`TopicDetailScreenTest` already holds.

---

## What was inspected and found clean

Recorded so a later session does not re-derive it.

- **Brand consistency.** The indigo/periwinkle family carries every screen in both themes.
  Light is a tonal ramp on a cool off-white; dark is a luminance ramp on near-black. The
  three gradient heroes are the only gradient surfaces and each reads as its own thing:
  Completion is an arrival (`06-result-*`), Progress a standing (`07-progress-*`), the
  Interview an invitation holding its own Start button (`08-interview-*`).
- **Surface hierarchy.** No nested same-level cards were found in any render. The Progress
  dashboard draws all four ranks in one scroll — gradient hero, recent-performance card,
  bordered weak-area cards, grouped Topic table, bare history rows — and each is
  distinguishable at a glance in both themes. The Practice Builder's summary and the
  Mistakes remediation block are each the only level-2 surface on their screen.
- **Selection language.** A chosen option is `primaryContainer` behind a 2dp `primary`
  border on every surface that has one — the builder's count tiles, its level checkboxes,
  its source rows, and an answer in a run — so a learner meets one selection language
  throughout. An unavailable source keeps its container and drops to the disabled opacities
  rather than disappearing (`04-builder-unavailable-light-380.png`).
- **Compact versus expanded.** Topic Browser, Progress, Mixed Interview, Mistake Review, the
  Practice Builder and both result screens each compose two panes at 1280dp with the same
  information hierarchy as their compact form and no filler added to fill the room. The
  Lesson reader stays a single centred reading column and gains an outline rail, which is
  the documented decision.
- **Navigation clearance.** Scrolled to the end of the Progress dashboard
  (`07-progress-tail-light-380.png`), the final session-history row sits clear above the
  floating compact navigation. `AppLayout.CompactNavigationClearance` is doing its job.
- **Motion vocabulary.** Every `tween` in `commonMain` is built from `AppMotion` durations
  and easings; the three outside `AppMotion.kt` — the navigation cross-fade and the two hero
  reveals — each name an `AppMotion` constant. No screen composes two competing entrance
  sequences: the Progress hero staggers its coverage behind its figure, the Interview hero
  staggers its rules and then its button, and both reveal the bulk at zero delay. Nothing in
  any screen's interaction is gated on an animation finishing, which
  `practiceIsClickableWithoutWaitingForTheEntrance` already holds for the one surface where
  it would matter most.
- **Design-system integrity.** No `Color` literal, no `.sp` literal and no ad-hoc animation
  spec outside `ui/theme/`. The size literals that remain outside the scales each state at
  their declaration why they are local to their surface — the interview hero's two glyph
  sizes, the trend chart's proportions, the answer row's line height, the lesson bullet's
  prose column. No new token, primitive or component was introduced by this pass.
- **Settings.** Rendered in both themes and expanded. Spacing is consistent between its two
  sections, the switch states read clearly in both themes, and nothing is misaligned. Left
  exactly as it is; its simplicity is the point.

## Conclusion

Every finding this pass produced is a reflow failure under a doubled type size, and none of
them is a colour, a surface, a motion or a composition problem. That is the useful result:
the cumulative visual-polish work does hold together as one product when rendered — one
brand family, one selection language, four legible surface ranks, three distinguishable
heroes, restrained semantic colour in both themes, and compact and expanded layouts that
keep the same hierarchy — and what it had not been measured against was type at 200%.

Three of the four are literally the same defect in three files: a trailing figure measured
before the text beside it, in a plain `SpaceBetween` row. That is `VC-001` one component
further along, and it is fixed with the primitive `VC-001` produced. The fourth is a fixed
height where a minimum was meant.

One residual remains and is named rather than smoothed over: the longest Topic detail tab
label still wraps inside the word at 2× on a phone, which needs a tab row that sizes to its
content and therefore a rewrite of the pager-linked indicator. It is `VQA-009` and it is the
single follow-up.

The rest of the matrix rendered cleanly and was deliberately left alone.
