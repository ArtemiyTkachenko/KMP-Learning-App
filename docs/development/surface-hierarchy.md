# Surface Hierarchy

Which container a piece of content belongs in, and which component draws it.

[Material Design 3](material-design.md) states the *tone* ramp — three neutral levels and what
separates them in each theme. This page states the *container* vocabulary built on top of it: four
ranks, five components, and the rule for choosing between them. The two are the same system read
from different ends, and a UI change usually needs this page first and the tone ramp only if it is
introducing a colour.

Read this before adding a screen section, a list, or a summary surface. It exists because the
vocabulary was previously spread across the per-feature architecture documents and the components'
own KDoc, so every session re-derived it — and re-derived it slightly differently.

## Four ranks, three tones

The palette has three neutral levels. A screen has four ranks, because the fourth is stated by
**how much container there is** rather than by a fourth tone the ramp does not have room for.

| Rank | What draws it | What it means |
| --- | --- | --- |
| **Hero** | the brand gradient | The single most important thing on this page. |
| **Card** | `surfaceContainerLow`, one edge per item | An item that is its own subject. |
| **Group** | `surfaceContainerLow`, one edge for several rows | A set of related rows read together. |
| **Bare row** | no container at all, on `background` | The quietest tier: a record, a sequence, a list. |

Group and Card share a tone on purpose. Gathering is not promotion — what changes between them is
the number of edges, and the number of edges is what the eye reads as the number of things.

The Progress dashboard uses all four in one scroll, which is the clearest worked example: the
standing hero, the recent-performance card, the per-Topic table as a group, and session history as
bare rows. None of them changes colour to say where it sits.

## The components

| Component | Where it lives | Call sites today |
| --- | --- | --- |
| `ProgressHero`, `AssessmentCompletionHero` | their own features | 2 — the only gradient surfaces |
| `AccuracyHeroCard` | `ui/ContentHierarchy.kt` | 2 — Topic drill-down, Topic practice page |
| `SecondarySummaryCard` | `ui/ContentHierarchy.kt` | 3 — Progress recent performance, Topic practice, Practice Builder |
| `ContentGroup` + `AccuracyRow` | `ui/ContentHierarchy.kt`, `ui/PerformanceCard.kt` | 4 groups, 3 rows |
| `PerformanceCard` | `ui/PerformanceCard.kt` | weak areas, Subtopic rows, Topic drill-down |
| bare rows | written per feature | Units, Subtopics, Lessons, session history |

### The gradient is not a rank a screen may climb to

`AppSemanticColors.heroGradientStart`/`End` has exactly two call sites and a third needs the same
argument the second one made: that nothing else on the page competes, and that the surface is
genuinely the answer the learner came for — not that a card should look important.

The two stay distinguishable by **motion**, not by palette. `AssessmentCompletionHero` is an
*arrival* and counts its score out over the app's one celebratory duration
(`AppMotion.ScoreRevealDurationMillis`). `ProgressHero` is a *standing answer* and settles over the
ordinary `ContentRevealDurationMillis`, because a learner opening Progress to check on themselves is
not being congratulated.

The gradient carries no on-colour of its own. `onPrimaryContainer` is its documented contract, and
anything else put on it — an `accuracyColor` band, a reduced-alpha supporting line — is outside that
contract and checks its own contrast. `AppColorSchemeTest`, `AssessmentCompletionHeroTest`, and
`ProgressHeroThemeTest` hold those three answers.

## Choosing between a card and a group

Three questions, in order. A "no" to any of them means cards.

**1. Is this a set that *supports* the screen, or the thing the screen is about?**

A group is for the supporting set. The Topics catalogue stays a column of cards even though its rows
are as uniform as any table's, because browsing it *is* what that screen is for — and its rows carry
a colour marker, a variable badge, and an accuracy block that make them genuinely different heights.
The Progress dashboard's per-Topic table is the mirror case: the same kind of content, but supporting
detail under a hero, and two plain lines per row. One is content, one is reference.

**2. Is the length bounded and small?**

A group composes its rows eagerly. Two continuation shortcuts, the Topics a learner has answered
anything in, at most two interview records — all fine. Session history is not: its length is
unbounded, and a group that composed four hundred attempts to draw one border would be paying for
the border with the scroll. That list stays a lazy list of bare rows.

**3. Does any row need to be singled out visually?**

A weak-area row carries an accent border, and a border is a property of a container. Inside a group
there is no container to put it on, and the group's own edge would have to change colour because one
member did. Weak areas stay cards — and the contrast between those bordered cards and the quiet
grouped table beneath them is now what separates "these need attention" from "here is everything".

## `PerformanceCard` and `AccuracyRow`

Both draw the same reading: what it is on the left, how the learner is doing on the right. They are
two components rather than one with a flag, because the card carries three things a grouped row
cannot.

| | `PerformanceCard` | `AccuracyRow` |
| --- | --- | --- |
| Container | its own Card | none — the group's |
| Accent border (`isWeak`) | yes | no: a border is a container property |
| Comparison meter | yes | no: a meter belongs *under* a row |
| Action line | yes | no: it would bleed into the divider below |
| Navigable | `onClick` → chevron + `Role.Button` | same |
| Merges descendants | only when navigable | always |

A `PerformanceCard(inGroup = true)` flag would have to answer for all three of those, and the answer
is the same every time: they are card features. That is also the general rule — prefer a second
component with an explicit responsibility over a mode flag inside one that does everything.

`AccuracyRow` merges its descendants whether or not it navigates. A `clickable` merges on its own, so
a navigable row was always announced as one thing; an inert one was three separate nodes, and inside
a group there is no card edge left to imply that the name, the score, and the rate belong together. A
Mixed interview's breakdown would have been read as nine unrelated fragments.

## Row mechanics: the state layer

A bare or grouped row is not a Material component, so it gets no container, no minimum touch target,
and no clipped state layer for free. Whichever way the margin arrives, **the row's own bounds must be
what the state layer fills** — on a pointer host hover is the resting state of whatever the cursor is
over, so a band whose edges land on the text is permanent, not a flash.

There are two arrangements in the app and the difference is where the horizontal margin comes from.

**Full-bleed list** — the list supplies vertical padding only and the row carries the margin inside
its own `clickable`, so the state layer reaches the pane edges. This is the Material
`ItemLeadingSpace` arrangement and the one to prefer for a list of rows.

```kotlin
LazyColumn(contentPadding = appListContentPadding()) {       // vertical only
    items(rows, key = { it.id }) { row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { … }
                .padding(horizontal = margin, vertical = …),  // inside the clickable
        ) { … }
        HorizontalDivider(modifier = Modifier.padding(horizontal = margin))
    }
}
```

**Inset list** — the list already supplies the horizontal margin (`appScreenContentPadding`), usually
because the same list also holds cards. The row is inset with everything else, so it clips itself to
a shape *before* the interaction modifier and the layer follows those corners instead of drawing a
rectangle. Progress's session history does this with `clickable`; the Lesson reader's outline entries
do it with `selectable`, which needs the same treatment for the same reason.

```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.small)                     // before the click
        .clickable(role = Role.Button) { … }
        .padding(horizontal = AppSpacing.Comfortable, vertical = AppSpacing.Grouped),
) { … }
```

Inside a `ContentGroup` neither applies: the group clips, and the row takes `GroupRowPadding` so its
inset matches the dividers the group draws between rows. A divider aligned to a different inset from
the text above it is the one detail that makes a grouped list look assembled by hand.

## Spacing between ranks

Use the relational names in `AppSpacing`; never a `.dp` literal. The two that carry hierarchy:

- `AppSpacing.Grouped` (12dp) separates siblings in a list.
- `AppSpacing.Section` (24dp) separates one section from the next, and is deliberately twice
  `Grouped` so a section break is unambiguous. `SectionHeading` applies it as a *top* margin,
  which is the point of that component — a heading sharing the list's `spacedBy` flow sits the
  same distance from the section above as two sibling cards, and proximity then says nothing.

## When the vocabulary is not enough

Adding a rank is a bigger decision than it looks. The Material ramp has more container roles than
the product draws, and the restraint is the point: three tones the eye can actually tell apart —
`AppColorSchemeTest` holds a floor on the step between consecutive levels — say more than six that
blur together, which is the defect the current ramp was widened to fix.

So if a screen seems to need a new rank, the answer is usually that something already on it should be
*quieter* rather than that something needs to be louder. Moving the top up is cheaper than pushing
everything else down, and it is what the Progress hero did: the surfaces beneath it did not move.

Record the reasoning for a genuinely new surface in the architecture document for that feature, and
add it to the table at the top of this page.
