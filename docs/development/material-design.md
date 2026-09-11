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
| `InteractiveComponentSize` minimum touch target | 48dp |
| Window margins: compact / medium and up | 16dp / 24dp, breakpoint 600dp |

## Do not re-declare the scales

Every scale already exists. Change the scale, never the call site.

| Concern | Where |
| --- | --- |
| Spacing steps | `ui/theme/AppSpacing.kt` |
| Window margin, breakpoint, max content width | `ui/theme/AppLayout.kt` |
| Corner radii | `ui/theme/AppShapes.kt` |
| Type scale | `ui/theme/AppTypography.kt` |
| Durations and easing | `ui/theme/AppMotion.kt` |
| Colour scheme and semantic colours | `ui/theme/AppColorScheme.kt`, `AppSemanticColors.kt` |

No literal `.dp` spacing in a screen when a scale step says the same thing. A pill is the
exception `AppShapes` documents: `RoundedCornerShape(percent = 50)` locally, because a pill
is a function of the element's own height.

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

## Components

- **One filled `Button` per screen.** It is the primary action. Never two of equal weight.
- **`TextButton` is the lower-emphasis action** everywhere in this app. `OutlinedButton`
  is used once, for a toggle that reflects state.
- **No chips.** `AssistChip`, `SuggestionChip`, `ElevatedButton`, and `FilledTonalButton`
  have zero usages here. `FilterChip` is used only for selection inside the Practice
  Builder form. Do not introduce a new component family for an action a `TextButton`
  already expresses.
- **Empty states**: `ScreenAction` when there is a way forward, `ScreenMessage` when there
  is not — a failure is not an invitation. Both are in `ui/ScreenStatus.kt`.
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
- **Semantic colour marks, it does not flood.** A weak row carries an accent border and a
  tinted figure over the ordinary neutral container rather than filling the whole card
  with `partiallyCorrectContainer`: one saturated card reads as emphasis, six in a column
  read as an alarm wall in which nothing stands out. Never use the error palette for an
  ordinary navigation or continuation action.
- **A toggle looks like a state, not a command.** The saved-Question control is a bookmark
  whose fill, shape, and word all change together, and publishes `stateDescription` plus
  `toggleableState` beside its action label, so assistive technology hears both what
  pressing it does and what is true now.

## Deliberate deviations

Recorded so they are not "fixed" by the next reader, and so new ones are argued rather
than drifted into.

| Deviation | Why |
| --- | --- |
| Flat list rows use 16dp vertical padding, not the 10dp `ItemTopSpace`/`ItemBottomSpace` | The token describes a dense one-line item. These rows are three-line content blocks already past the 88dp three-line container height, where 10dp reads as cramped. |
| `AppShapes` departs from the Material baseline corner scale | Argued in `AppShapes.kt`: `medium` at 12dp made every content surface in the product the most generic shape Material can produce. |
| `AppMotion` states spring constants literally rather than reading `MotionScheme` | `MotionScheme` is `@Composable`-scoped and several call sites are not. The numbers are Material's own. |

## Verifying a layout change

Assertions do not see a state layer, a margin, or an overlap. When a change is about how
something *looks*, capture it: a throwaway `runSkikoComposeUiTest` that renders the screen
under `AppTheme`, drives `performMouseInput { moveTo(center) }` where hover matters, and
writes a PNG. Measure the PNG rather than eyeballing it — the alpha channel is what carries
a state layer over a transparent background, so compare alpha, not RGB. Delete the harness
afterwards; these are not tests.
