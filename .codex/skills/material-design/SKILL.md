---
name: material-design
description: Apply this repository's Material 3 rules to Compose UI work. Use when adding or changing a screen, list row, spacing, padding, component choice, or theme value; not for non-UI Kotlin, build, or content changes.
---

# Material Design

## Use When

A task adds or changes Compose UI: a new screen or page, a list row, spacing or padding,
a choice of component, an empty or error state, or a value in the theme scales.

## Do Not Use When

The change is domain, data, build, curriculum content, or documentation only, or touches
Compose solely to rename a symbol or adjust a test tag.

## Workflow

1. Read [Material Design 3](../../../docs/development/material-design.md). It is the
   single source of truth for these rules — do not restate or reinterpret it here.
2. Find the comparable surface that already exists and follow it. Card lists and flat
   divider lists are both correct here, for different jobs; pick the one the neighbouring
   screen uses rather than introducing a third shape.
3. Take spacing, radii, type, motion, and colour from the scales in `ui/theme/`. If the
   value you need is not on a scale, change the scale rather than writing a literal.
4. For a row that is its own interactive surface, put the horizontal margin **inside** the
   row and leave the list full-bleed, so the state layer spans the pane. This is the rule
   most often got wrong; the doc explains why.
5. Justify any number that is not a scale step by naming the Material token it comes from,
   using the `unzip` command in the doc. Do not cite `m3.material.io`.
6. If the change must depart from the spec, add it to the deviations table in the doc with
   its reason, in the same change.
7. Verify visually. Assertions do not see a state layer or a margin — capture and measure
   the pixels, as the doc describes.

## Project References

- [Material Design 3](../../../docs/development/material-design.md) — the rules, the token
  values, and how to re-derive them.
- [Kotlin style](../../../docs/development/kotlin.md) — visibility, comments that explain
  why, no premature abstraction.
- [Validation](../../../docs/development/validation.md) — the commands that exist here.

## Output

Report which existing surface the change followed, any Material token quoted and its
value, any deviation recorded, and how the result was visually verified.
