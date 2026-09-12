# Adaptive layout

How this app uses the width it is given. One definition of "how much room is there", three
content widths, and a small set of screens that compose two panes instead of one.

Everything here lives in `shared/src/commonMain/kotlin/.../ui/theme/AppLayout.kt` and
`ui/AdaptivePanes.kt`. Nothing in this file is platform-specific: the app runs on five
hosts and any of them can be any size, because a desktop or browser window can be dragged
narrow.

## The one measurement

`AppNavigationScaffold` is the only composable in the app that measures the window. It
publishes two things into composition and nothing else reads a `Dp` of window width:

| Composition local | What it carries |
| --- | --- |
| `LocalAppWindowSizeClass` | `Compact`, `Medium`, or `Expanded` |
| `LocalAppContentMargin` | 16dp compact, 24dp from medium upward |

**A screen must not measure the window.** It reads the class and composes accordingly.
Every screen measuring for itself is how a codebase ends up with four breakpoints that
disagree, and the app already had the beginning of that: the rail breakpoint was the only
window measurement anywhere, so any screen wanting desktop behaviour would have had to
invent its own.

### The classes

| Class | Width | What it means |
| --- | --- | --- |
| `Compact` | `< 600dp` | Phone-shaped. One column, navigation along the bottom edge. |
| `Medium` | `600dp –<1040dp` | Tablet, split-screen desktop, narrow browser. One column, navigation rail beside it. |
| `Expanded` | `>= 1040dp` | Desktop or full-width tablet. A screen whose content genuinely divides may compose two panes. |

600dp is Material's own compact/medium boundary and is where the navigation rail has
always appeared. **1040dp is not Material's 840dp**, and the difference is deliberate:
840dp is this app's *single*-pane cap, so a window exactly that wide has room for one
comfortable column and not for two. Treating it as expanded would hand every dashboard a
pair of 380dp columns — narrower than the phone layout they replaced. At 1040dp a split
still leaves each pane wider than a phone once the rail, both margins, and the gutter are
taken out.

**`Expanded` does not mean "must be two panes."** The Lesson reader is a single centred
column at every width, because prose does not become more readable by being split in half.

## The three content widths

The shell used to cap everything below it at one value, which is why a wide window showed
the same phone column whatever was in it — a twenty-paragraph Lesson, a dashboard of eight
figures, and a result transcript all stopped at 840dp. It also capped each screen's
`TopAppBar`, so on a desktop the bar stopped short of both window edges and floated over
the page, which is the clearest single symptom of a phone layout dropped into a desktop
window.

The cap now belongs to the screen. Each one wraps its content in `AppContentPane` — or
`ColumnScope.AppScreenPane`, which is the same thing shaped for the `TopAppBar`-then-`when`
structure almost every screen has — and states which of three widths its content wants:

| `AppContentWidth` | Cap | For |
| --- | --- | --- |
| `Reading` | `ReadingMeasure` + both margins (600dp + 2×margin) | Long-form lesson prose |
| `Standard` | `MaxContentWidth` (840dp) | Lists, forms, detail screens |
| `Paned` | `MaxPanedWidth` (1440dp) at `Expanded`, `MaxContentWidth` below it | Screens that compose two panes |

A `TopAppBar` stays *outside* the pane, spanning the window: it is chrome and belongs to
the window rather than to the content.

### Why the reading measure is narrower

At `bodyLarge` (16sp) an average character occupies roughly half the type size, so 840dp
carries something over a hundred characters a line — past the width at which the eye
reliably finds the start of the next one. 600dp is about 70 characters, inside the 65–75
band typographic convention settles on. It is a maximum: a phone never reaches it and
reads full width behind the ordinary margins.

Dashboards and lists are *scanned* and do not have this problem, which is why they keep
840dp.

## Which screens compose two panes

`AppTwoPaneRow` is a thin wrapper over `Row` and exists for the two things it states: the
gutter is `AppLayout.PaneGutter` everywhere, and **`primary` is composed first**. That
second point is the accessibility rule for every expanded layout here — a screen reader, a
keyboard tab order, and a linear traversal all follow composition order, so the pane a
learner would read first on a phone is the pane declared first.

The groups inside each screen are declared once, as `LazyListScope` extension functions,
and both layouts call them in the same order. That is the mechanism that keeps the two
arrangements honest: a section added to a screen reaches the phone and the desktop at the
same position, and neither arrangement can quietly acquire something the other lacks.

| Screen | Primary pane | Secondary pane |
| --- | --- | --- |
| Learn | Guidance: the one recommendation, continue studying, continue reading, saved Questions | The Topic catalogue |
| Progress | Standing: all-time accuracy, coverage, recent window | Actionable: mistake queue, weak areas, then Topic performance and history |
| Interview landing | The invitation and what an Interview is | The learner's record |
| Practice Builder | The four configuration decisions | Availability, its retry, and Start |
| Mistakes | The count, what unresolved means, and practise-all | The queue |
| Interview result / Practice result | Score, notices, repeat, per-Topic breakdown | The question transcript |

Everything else keeps one column at every width.

### Results are not master/detail

A master/detail transcript was considered and rejected. It would mean introducing selection
state for a document the learner is meant to read through, and it would hide nineteen of
twenty reviews behind a click each — on the screen whose whole purpose is going back over
what happened. Splitting the *summary* away from the transcript solves the actual
complaint, which is that the score and the next actions scroll away and have to be hunted
back up to, and it leaves every P0 review rule untouched because the transcript pane holds
the same `ReviewQuestionCard` list in the same order.

### Mistakes is not a mail client

Same reasoning. There is no selection, no detail pane, no filtering and no sorting: the
queue is already in the domain's order, every entry is already a full review card, and the
per-entry scoped practice shortcut is on the entry it belongs to. The second pane holds
what the top of the single column holds, and nothing that did not exist before.

## The Lesson outline

On `Expanded`, the Lesson reader spends its spare width on orientation rather than on
measure: a section outline sits beside the prose, and the reading column keeps exactly the
width it would have had alone.

The outline is derived from the Lesson's own structure, not from rendered text. A Section
earns an entry when it starts a depth run — which is exactly when `LearningSectionContent`
draws the depth heading — or when it carries an authored title, and its label is that title
where there is one and the depth otherwise. So every entry names something the reader can
actually see on the page, and the outline cannot drift from it. A Section with no title in
the middle of a depth run has no heading and gets no entry.

Anchors are measured with `onGloballyPositioned` rather than estimated: block heights
depend on the font scale, the window width, and how a paragraph happened to wrap, none of
which is knowable from the authored model. A Lesson producing fewer than two entries gets
no outline, because a contents list of one item is a decoration.

This is why no content-model migration was needed. `LearningSection` already carries
`depth` and an optional `title`, which is enough; had it not been, the right answer would
have been to keep the narrow centred column and say so here.

## Verifying a change

`shared/src/jvmTest/.../ui/AdaptiveLayoutTest.kt` holds the rules above as assertions: the
class boundaries, that the shell publishes the class it measured, that Progress is one
scroller when compact and two panes when expanded, that prose stays at the reading measure,
and that the outline appears only where it is earned.

Two things are worth knowing before adding to it:

- `runSkikoComposeUiTest` defaults to a **1024×768** virtual display, which is a *medium*
  window under these rules. A test that only sizes a `Box` to 1400dp inside it is still
  measuring a 1024px window. Pass `size = Size(1600f, 900f)`.
- The default density is 1, so a bound in pixels is the same number as the `Dp` that
  produced it. Derive expectations from the tokens rather than writing the number out.

`LargeFontScaleTest.kt` runs the densest layouts at `fontScale = 2f` on a 360dp window and
asserts the two things that actually break — content running off the side, and a primary
action becoming unreachable.
