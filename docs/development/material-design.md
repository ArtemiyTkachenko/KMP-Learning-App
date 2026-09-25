# Material Design 3

This app is a Material 3 product. These are the rules a change to Compose UI has to hold
to, and where the numbers behind them come from.

## Where the spec lives

Not `m3.material.io`. Those pages are client-rendered and cannot be quoted or diffed, and
the guidance on them drifts independently of the library.

The authority for this repository is the **design tokens compiled into the Material 3
version pinned in `gradle/libs.versions.toml`**. They are on disk, they match the
components the app actually renders, and they change only when the version does:

```sh
SRC=$(find ~/.gradle/caches/modules-2/files-2.1/org.jetbrains.compose.material3 \
  -name "material3-desktop-*-sources.jar" | head -1)
unzip -l "$SRC" | grep tokens                       # what is available
unzip -p "$SRC" '*/tokens/ListTokens.kt'            # one token file
```

Quote a token by name when a value needs justifying, so a reviewer can re-derive it.

## Values this app relies on

| Token | Value |
| --- | --- |
| `ListTokens.ItemLeadingSpace` / `ItemTrailingSpace` | 16dp |
| `ListTokens.ItemTopSpace` / `ItemBottomSpace` | 10dp |
| `ListTokens.ItemBetweenSpace` | 12dp |
| `ListTokens.ItemOneLine` / `TwoLine` / `ThreeLineContainerHeight` | 56 / 72 / 88dp |
| `ListTokens.DividerLeadingSpace` / `DividerTrailingSpace` | 16dp |
| `PrimaryNavigationTabTokens.ContainerHeight` | 48dp |
| `PrimaryNavigationTabTokens.ActiveIndicatorHeight` | 3dp |
| Compact floating navigation height / `ElevationTokens.Level2` | 68dp / 3dp |
| `InteractiveComponentSize` minimum touch target | 48dp |
| Window margins: compact / medium and up | 16dp / 24dp, breakpoint 600dp |
| Window class breakpoints: compact / medium / expanded | 600dp / 1040dp (see below) |

## Do not re-declare the scales

Every scale already exists. Change the scale, never the call site.

| Concern | Where |
| --- | --- |
| Spacing steps | `ui/theme/AppSpacing.kt` |
| Window margin, window classes, content widths | `ui/theme/AppLayout.kt` |
| Corner radii | `ui/theme/AppShapes.kt` |
| Type scale | `ui/theme/AppTypography.kt` |
| Durations and easing | `ui/theme/AppMotion.kt` |
| Colour scheme and semantic colours | `ui/theme/AppColorScheme.kt`, `AppSemanticColors.kt` |

No literal `.dp` spacing in a screen when a scale step says the same thing. A pill is the
exception `AppShapes` documents: `RoundedCornerShape(percent = 50)` locally, because a pill
is a function of the element's own height.

## Surfaces are three levels, not a spectrum

The palette has a full Material container ramp, but the product only draws three levels, and a
change picks one of them rather than a tone it likes the look of.

| Level | Role | What belongs here |
| --- | --- | --- |
| 0 — page | `background` | The screen itself. Only `AppNavigationScaffold` paints it. |
| 1 — ordinary content | `surfaceContainerLow` | Most Cards: a list row, a review question, `SecondarySummaryCard`. |
| 2 — raised / interactive | `surfaceContainer` and above | The one surface on a screen that outranks the rest: `AccuracyHeroCard` (at `surfaceContainerHigh`), a weak `PerformanceCard`, a code block, a menu or sheet. |

Level 2 is a claim about importance, so a screen that puts everything on it has said nothing. If two
sibling cards both want it, neither should have it.

Three tones, but four *ranks*: the fourth — a set of rows under one shared edge — is stated by how
much container there is rather than by a tone the ramp has no room for, and the brand gradient sits
outside the ramp entirely. Which container a section belongs in, and which component draws it, is
[surface hierarchy](surface-hierarchy.md); this section stays the answer for the tones themselves.

The two themes reach the separation differently, and neither is the other inverted. **Light is
tonal**: the page is the brightest surface — a cool off-white, never white — and each level above it
is a deeper, bluer tint, so layering never becomes white on white and never needs a shadow to be
legible. **Dark is luminance**: the page is near black and each level is lifted toward light, so
separation comes from brightness rather than from borders, which is what keeps dark cards from
flattening into grey rectangles.

`AppColorSchemeTest` holds a floor on the step between consecutive levels. The ramp it replaced
stepped as little as 4/255, which is below what a display in a lit room resolves — a card on a page
and a card on a card read as one tone, and no call site was at fault.

## Semantic colour is a mark, not a fill

`AppSemanticColors` gives each of correct, partially-correct, and incorrect three values: a
**container** for a block, an **on-container** for text in it, and a bare **accent** for a border, an
icon, or a tag beside neutral text. The containers are the quietest member on purpose — pale tints
in light, deep and desaturated in dark — because saturation belongs on the 1dp border and the line of
tag text, not on the 200dp block. Six review cards should read as six results, not six alarms.

`colorScheme.error` and `AppSemanticColors.incorrect` are deliberately the same colour in each
theme, and a test asserts it. A product whose failed answer and failed operation are different reds
has two error languages and teaches neither.

An answer option has exactly three states and they are drawn in that order of strength: at rest it
is a level-1 surface with a hairline `outlineVariant` border; chosen, it takes `secondaryContainer`
with a 2dp `primary` border; marked, it takes its `AnswerOutcome` colours. Practice and review share
that vocabulary — `AnswerOutcome`, `QuestionOutcome`, and their colours live in
`assessment_review/QuestionContentComponents.kt` and neither screen keeps a colour rule of its own.
A marked option keeps the answer text in `onSurface` rather than an outcome colour, because the text
is the authored question and the mark is the label and border around it.

`AppSemanticColors.heroGradientStart`/`heroGradientEnd` has exactly one call site,
`AssessmentCompletionHero`, and is reserved for a surface that is the single most important thing on
its screen. It carries no on-colour: both endpoints sit within the scheme's `primaryContainer` tone,
so `onPrimaryContainer` is the text colour across the sweep. A hero also takes a hairline border in
its own on-colour at low alpha plus a small `shadowElevation`, because a gradient alone is a colour
change rather than a lifted object — most noticeably in light, where the sweep is a pale tint over an
off-white page.

A screen may have a hero without the gradient, and three do. `AccuracyHeroCard` is
`surfaceContainerHigh` with a hairline `outlineVariant` edge and a small shadow, and it is what the
Progress dashboard, the Topic drill-down, and the Topic practice page lead with. The edge is what
carries the rank in dark, where one container step is nearly invisible and a shadow almost is; the
shadow is what carries it in light, where the two tones are close in value. Neither alone works in
both schemes. Reach for the gradient only where the surface is an *arrival*.

Every hero states its figure beside `AccuracyRing` rather than over a `ProgressMeter`. A bar is the
picture of "how far through something you are" — which is what a coverage meter on the same card
shows — and a ring is the picture of a rate. Drawing both with one control made a Topic's accuracy
and its coverage look like two readings of one quantity. One ring size exists on purpose: a ring
that changed size between surfaces would read as several controls rather than one product idea.

`PerformanceCard(comparesWithSiblings = true)` adds a meter under a row, and is off everywhere
except a Topic's Subtopics. Those rows are parts of one whole on one scale, and "which of these is
worst" is the question the screen was opened to answer — four percentages down the right-hand edge
answer it only by being read and remembered one at a time. A session-history list is the
counter-example: consecutive attempts of different lengths on different scopes are not rows to
compare, and a bar on each would invite exactly that. Both the ring and these meters clear their
semantics, because the percentage is written in full immediately beside them.

A completed assessment is the one surface where a performance colour must **not** come from
`accuracyColor`. That scale bottoms out at `AppSemanticColors.incorrect`, and the error role is for a
wrong answer or a failed operation, not for a verdict on the learner; the hero's own
`ResultEmphasis` band resolves a weak run to `partiallyCorrect` and never to a red.

## Adaptive layout

**Never measure the window in a screen.** `AppNavigationScaffold` is the only composable
that measures, and it publishes `LocalAppWindowSizeClass` (`Compact` / `Medium` /
`Expanded`) and `LocalAppContentMargin`. A screen reads the class and composes; it does not
compare `Dp` values of its own, and it does not introduce a second breakpoint.

**State the content width, do not inherit one.** Every top-level screen wraps its content
in `AppContentPane` — or `ColumnScope.AppScreenPane` under a `TopAppBar` — with one of
`AppContentWidth.Reading` (lesson prose, ~65–75 characters), `Standard` (lists, forms,
detail screens), or `Paned` (screens that compose two panes at `Expanded`). A `TopAppBar`
stays outside the pane and spans the window: it is chrome, not content.

**In a two-pane layout the first-declared pane is the one a phone shows first.**
`AppTwoPaneRow` composes `primary` before `secondary`, and composition order is what a
screen reader, a tab order, and a linear traversal follow. Declare the sections once as
`LazyListScope` extensions and call them in the same order in both arrangements, so the two
cannot drift.

The classes, which screens use two panes, and the decisions behind the 1040dp expanded
breakpoint are in [adaptive layout](../architecture/adaptive-layout.md).

## Navigation and page-level tabs are different levels

The app has two navigation systems and they are not peers. The bar or rail says which of
the four **areas** of the product the learner is in and persists across every screen in it;
a tab row says which of one screen's pages is showing and exists only there.

- **Area navigation keeps the filled pill** — Material's `secondaryContainer` over
  `onSecondaryContainer`.
- **Page-level tabs take Material's own tab affordance** — a `primary` rule of
  `ActiveIndicatorHeight` under the selected tab, with the label in `primary`.

Drawing both as the same filled pill, which this app did until P2, made a page control look
like a second copy of the app's navigation and made the Topic screen read as though it had
two rows of destinations.

## Lists

**A row that is its own interactive surface must be full-bleed, and carry the margin
itself.** The container of a list item spans its pane and holds the horizontal space
inside it — that is what `ItemLeadingSpace` / `ItemTrailingSpace` are.

Putting the margin on the list instead (`LazyColumn(contentPadding = …)`) insets the row,
and the row's state layer with it, so hover, focus, and press draw a band whose edges land
exactly on the text. That is nearly invisible on a touch screen, where a press flashes and
is gone, and permanent on a pointer host, where hover is the resting state of whatever the
cursor is over. This app runs on desktop and web, so it is a real defect there.

```kotlin
LazyColumn(contentPadding = appListContentPadding()) {      // vertical only
    items(rows, key = { it.id }) { row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { … }
                .padding(horizontal = margin, vertical = …), // inside the clickable
        ) { … }
        HorizontalDivider(modifier = Modifier.padding(horizontal = margin))
    }
}
```

where `margin` is `LocalAppContentMargin.current`. Dividers take the same inset, which is
what keeps them aligned with the content — `DividerLeadingSpace` is that alignment stated
at the compact margin.

**A list of Cards needs none of this.** A Card is its own container, supplies its own
internal padding, and clips its own state layer, so those lists use
`appScreenContentPadding()` as normal. Both shapes exist in the app on purpose: Cards for
short or heterogeneous lists, flat divider rows for the long ones.

A bare row inside a list that *does* supply the horizontal margin is the third case — it clips
itself before its `clickable` instead. That, and rows inside a `ContentGroup`, are in
[surface hierarchy](surface-hierarchy.md).

## Components

Grouped by the question each rule answers. The list grew flat and unordered while the screens were
being worked through one at a time; the rules did not change in the regrouping, but three of them
were checked against the code and found to be describing something the app no longer does. Those
are marked where they appear.

### Which component

- **One filled `Button` per screen.** It is the primary action. Never two of equal weight.
- **`TextButton` is the lower-emphasis action** everywhere in this app.
- **`OutlinedButton` is the secondary *rank* of an action that also has a primary form.** Both call
  sites — `AssessmentRetakeAction` and the Lesson reader's practice action — switch between
  `Button` and `OutlinedButton` on which of two actions the screen has decided is the continuation,
  keeping the same label and the same callback. It is not a toggle: a control whose *state* the
  learner is reading is a `Switch` or the saved-Question bookmark, below.
- **No chips.** `AssistChip`, `SuggestionChip`, `ElevatedButton`, and `FilledTonalButton`
  have zero usages here. `FilterChip` is used only for selection inside the Practice
  Builder form. Do not introduce a new component family for an action a `TextButton`
  already expresses.
- **Empty states**: `ScreenAction` when there is a way forward, `ScreenMessage` when there
  is not — a failure is not an invitation. Both are in `ui/ScreenStatus.kt`.

### What it says

- **Status is stated in words**, with colour and icons as a second channel, never the only
  one. `StatusBadge` is the app's pill; Material's `Badge` is reserved for the navigation
  bar count.
- **A badge must add something the container does not already say.** Under a heading that
  reads "Weak areas", a "Weak area" badge on every card repeats the heading once per row;
  on a screen titled by its count of unresolved mistakes, an "Incorrect" badge on every
  card does the same. Both are suppressed there and kept where the surrounding list is
  mixed — `PerformanceCard(weakLabel = null)` and
  `ReviewQuestionCard(statesOutcome = false)` are how a caller says the context already
  states it. A badge that distinguishes a *different* state, such as "Partially correct"
  in the mistake queue, always stays.
- **A marker is not a heading.** Metadata about *where* content sits — a depth layer, a position,
  a band label — takes `labelLarge` in `primary` and publishes no `heading()`. `SectionHeading` is
  for the thing that introduces content, and giving it to a marker makes the metadata outrank the
  content and hands assistive technology two peer headings where the page drew one inside the other.
- **A screen's own subject line *is* a heading**, and publishes `heading()` even though it is not a
  `SectionHeading`. The bar says which screen this is; the subject line says what this instance of
  it holds — the Topic being configured, how many mistakes are outstanding, which Unit is open. A
  screen whose field labels are headings while the thing they describe is not leaves heading
  navigation landing in the middle of the page.
- **Name the colour role on every styled `Text`.** `LocalContentColor` resolves correctly inside
  the shell, because the `Scaffold` supplies `onBackground` — but a preview, a test, or any
  composition outside it gets Compose's default black. A screen should not depend on an ambient it
  does not control to be legible in dark.
- **Semantic colour marks, it does not flood.** A weak row carries an accent border and a
  tinted figure over the ordinary neutral container rather than filling the whole card
  with `partiallyCorrectContainer`: one saturated card reads as emphasis, six in a column
  read as an alarm wall in which nothing stands out. Never use the error palette for an
  ordinary navigation or continuation action.

### Where it sits

- **An action about one item belongs inside that item.** A per-entry control emitted as a sibling
  of the card it acts on lands on the page background between entries, where it reads as navigation
  for the screen rather than as what that entry offers. `ReviewQuestionCard(footer = …)` is the slot
  for those; the Mistakes queue's scoped-practice and study-lesson shortcuts are its only caller.
  Several such controls go in a `FlowRow`, because an authored Lesson title has no known length.
- **A primary action fills a measured column, never a weighted pane.** *(Corrected: this was
  written as "only when the container is the window", which several screens have always
  contradicted.)* The distinction is how the container got its width. A column capped at an
  `AppContentWidth` — a form, a reading measure — was sized for content, so a terminal action
  filling it stays a button; the taking screen, the Unit overview and the Lesson reader all do this
  and are right to. A pane taking `weight(1f)` of the window was sized by the window, so the same
  `fillMaxWidth` produces a five- or six-hundred-pixel bar, wider than the sentence explaining what
  it does. Those read `LocalAppWindowSizeClass` and size to their label at an expanded width: the
  Practice Builder's Start, and the Interview invitation's, which this sweep found and fixed.
- **Chrome pinned outside the content pane still takes the content pane's width.** A bar, meter or
  counter that describes the column below it has to be capped and centred the same way, or it lines
  up with the column only until the window passes `AppContentWidth`'s cap. Wrap it and apply the
  same `maxWidth()` and `LocalAppContentMargin`. The exception is chrome that belongs to the
  *toolbar* rather than to the column — see the deviations table.
- **One meter style.** `ProgressMeter` is the product's linear bar: 8dp, rounded, no track gap and
  no stop indicator, because these are measurements of how much has been covered rather than
  operations in flight. Do not configure a second `LinearProgressIndicator` by hand; the Lesson
  reading hairline is the single recorded exception.
- **A modal's states share a container.** A hand-rolled `Surface` inside a `Dialog` defaults to
  `surface` at whatever shape it is given, while `AlertDialog` takes `DialogTokens.ContainerColor`
  (`surfaceContainerHigh`) at `CornerExtraLarge` — so one dialog's busy state and its failure state
  arrived on different containers. Quote `AlertDialogDefaults.shape` and `.containerColor`.

### How it behaves

- **A settings row is one toggle, not a row containing one.** The appearance row in
  Settings takes `Modifier.toggleable(role = Role.Switch)` and gives its `Switch`
  `onCheckedChange = null`, so the whole two-line row is the target and the accessible node
  — one switch to announce, not a clickable row beside a separate switch.
- **A toggle looks like a state, not a command.** The saved-Question control is a bookmark
  whose fill, shape, and word all change together, and publishes `stateDescription` plus
  `toggleableState` beside its action label, so assistive technology hears both what
  pressing it does and what is true now.
- **A list that is browsed closes its items; a list that is read through opens them.** Both share
  `QuestionDisclosure`, and the default is the caller's, because it is a statement about the
  surface rather than about the control.
- **A row that can be pressed responds to being pressed.** An option the learner is asked
  to choose takes its own `MutableInteractionSource`, hands it to `selectable`/`toggleable`
  so Material still draws the ripple from it, and reads `collectIsPressedAsState` for a
  scale of about 0.98 in a `graphicsLayer`. Draw-layer only, so layout, hit testing, and
  when the click callback runs are all untouched — a press treatment must never be
  something the callback waits for. Never hand-roll a gesture detector for this.
- **One discrete state, one `Transition`.** Where several properties of a component are
  decided by the same fact — an answer option's container, border, border width, label
  colour, and control tints — name that fact as an enum and drive them from a single
  `updateTransition`, not from one `animate*AsState` per property. Independent animations
  on the same input drift apart under a fast state change and let a later edit teach one
  property a rule the others do not know.

### How it moves

- **Stagger a reveal in the animation spec, not in a coroutine.** When several pieces of
  one reveal should arrive in order, give one container the layout expansion and give the
  children delayed specs through `AppMotion.revealSpec(delayMillis)` and
  `Modifier.animateEnterExit`. The page then relayouts once, the order is still legible,
  and nothing in the interaction is gated on an animation finishing.
- **Give the piece that opens the space no delay of its own.** In a staggered reveal, the
  content that accounts for most of the new height arrives at zero delay and only the
  supporting pieces trail it. Holding the bulk back even 60ms reads as a container opening
  an empty space and then filling it.
- **Expand downwards from the top.** `expandVertically` and `shrinkVertically` default to
  `Alignment.Bottom`, which reveals the *end* of the content first: for the first hundred
  milliseconds the visible sliver of an opening block is its last paragraph. Content that
  opens below a control the learner just pressed takes `expandFrom = Alignment.Top`, so the
  first thing revealed is the first thing to read. Combined with a stagger, the default is
  worse than wrong — the only strip on screen is also the piece deliberately held back, so
  the block appears to open empty.
- **A busy control states its own condition, in place.** When an action is working, the
  button keeps its place, its size and its emphasis, and its content crosses over to a
  word plus a spinner sized like a leading icon (18dp, 2dp stroke — not Material's
  standalone 40dp indicator, which grows the button it sits in). Never put the
  explanation for a button's busy state in a separate line beside it: that is a second
  piece of layout appearing and disappearing, and it leaves the button saying nothing
  about why it stopped working. A standalone indicator that is *not* inside a control —
  a loading screen, a dialog — keeps Material's own size.
- **Disabled-because-working is not disabled-because-unavailable.** Material fades a
  disabled button's content to 38% of `onSurface`, which is right for an action the
  learner cannot take and wrong for one that is reporting progress — it dims the spinner
  and its label at the moment they are the only things saying anything. Such a control
  keeps `enabled = false` for its semantics and overrides `disabledContentColor` so the
  state stays readable.

## Deliberate deviations

Recorded so they are not "fixed" by the next reader, and so new ones are argued rather
than drifted into.

| Deviation | Why |
| --- | --- |
| Flat list rows use 16dp vertical padding, not the 10dp `ItemTopSpace`/`ItemBottomSpace` | The token describes a dense one-line item. These rows are three-line content blocks already past the 88dp three-line container height, where 10dp reads as cramped. |
| `AppShapes` departs from the Material baseline corner scale | Argued in `AppShapes.kt`: `medium` at 12dp made every content surface in the product the most generic shape Material can produce. |
| `AppMotion` states spring constants literally rather than reading `MotionScheme` | `MotionScheme` is `@Composable`-scoped and several call sites are not. The numbers are Material's own. |
| The Lesson reading hairline is a hand-configured `LinearProgressIndicator`, not `ProgressMeter` | It belongs to the toolbar rather than to the reading column: 3dp, square caps, no gap, full-bleed, and unanimated because it tracks a finger rather than jumping between figures. `ProgressMeter`'s rounded, inset, animated treatment would read as a loose component that had drifted under the bar. |
| Compact area navigation uses a translucent floating container, 24dp icons, and a whole-destination selected pill | The standard full-width container reserved viewport space, while `NavigationBarItem` imposed an icon-only indicator and excess internal layout. The custom 68dp surface keeps Material selection semantics and 48dp targets while centring each icon-label pair in one fixed 60dp-high destination. |

## Empty and early states

An empty surface is a state to design, not an edge case. A good one answers three
questions: what this area is for, why it is empty, and what to do next.

- **Do not state something the app cannot support.** An empty weak-area list means either
  "the rule ran and singled nothing out" or "nothing was derived at all", and the state
  carries no flag saying which — so Progress infers it from whether Topic performance was
  derived, and shows no section rather than a sentence that might be false. Never classify
  something prematurely to avoid a blank space.
- **Do not draw an empty chart, a 0% gauge, or a placeholder row.** A `0 of 20` is a result
  the learner never got. Say what will appear here and why it is worth coming back to.
- **Match the surface to the message.** A "nothing to report" line under a heading is two
  lines of type, not a card — a card there is one more surface of exactly the kind it is
  denying.
- **A no-match search keeps the query and offers to clear it**, quoting back what was
  searched for so a typo can be seen. It does not recommend anything.
- **Only offer an action the product state supports**, and only where one exists.
  `ScreenAction` when there is a way forward, `ScreenMessage` when there is not.

## Verifying a layout change

Width, wrapping, and reachability *are* assertable, and there are two harnesses for them:
`ui/AdaptiveLayoutTest.kt` for the window classes and pane composition, and
`ui/LargeFontScaleTest.kt` for a doubled type size on a 360dp window. Note that
`runSkikoComposeUiTest` defaults to a 1024x768 display — a *medium* window here — so an
expanded-layout test must pass `size` explicitly.

Assertions do not see a state layer, a margin, or an overlap. When a change is about how
something *looks*, capture it: a throwaway `runSkikoComposeUiTest` that renders the screen
under `AppTheme`, drives `performMouseInput { moveTo(center) }` where hover matters, and
writes a PNG. Measure the PNG rather than eyeballing it — the alpha channel is what carries
a state layer over a transparent background, so compare alpha, not RGB. Delete the harness
afterwards; these are not tests.
