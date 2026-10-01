# Text Selection And Copy Confirmation

Why every screen's text is selectable, and why a copy is confirmed but never performed by this app.

## What the learner sees

`App.kt` wraps `NavDisplay` in `SelectableContent`, so text on every screen can be selected. The
wrapping sits around `NavDisplay` rather than around the whole shell so the four area-navigation
labels stay out of any selection — they are controls, not content.

Selecting text does nothing on its own. Copying is offered by the platform, through whichever menu
that platform already shows: Android's floating toolbar, the desktop right-click menu, Ctrl/Cmd-C
everywhere. The one thing this app adds is the confirmation — when a copy runs, a snackbar reads
"Copied to clipboard". With the Compose versions currently resolved that confirmation appears on
desktop and iOS only; see [the current reach](#current-reach) below.

An earlier version of this feature drew a menu of its own next to the selection, with its own Copy.
On desktop that put two menus offering the same word on screen at once, which is what a
platform-shaped affordance costs when it is reimplemented rather than used. The app now only
observes.

## Where the copy is observed

`ReportSelectionCopies` (`ui/selection/SelectionCopyReports.kt`) wraps the platform's copy action so
the clipboard still receives exactly what the platform's Copy would have written. It is an `expect`
because the menu that offers a copy differs by input device, and neither seam covers the other:

| | Android, iOS, web | desktop (JVM) |
| --- | --- | --- |
| the menu | the floating toolbar | the right-click context menu |
| the seam | `LocalTextToolbar` | `LocalTextContextMenu` |
| the wrapper | `CopyReportingTextToolbar` forwards every call untouched and wraps `onCopyRequested` | `CopyReportingTextManager` proxies the `TextManager` and wraps its `copy` action |

The split is not a matter of taste. `SelectionManager` shows its toolbar only when `isInTouchMode`,
which is `!event.isMouseOrTouchPad()`, so on desktop the toolbar seam is never called at all and a
mouse selection would go unnoticed. `LocalTextContextMenu` is the desktop counterpart, and it ships
only in the desktop artifact, so there is nothing to use in its place on the web targets — a browser
selection made with a mouse copies normally and simply passes unannounced.

The same holds for a keyboard copy on any platform: `SelectionContainer` handles Ctrl/Cmd-C through
its own clipboard handler rather than through either menu, so that copy is silent too. This is an
accepted limitation rather than an oversight: the snackbar confirms a menu action the learner
cannot otherwise see complete, and intercepting key events to announce a shortcut the learner chose
deliberately is not worth a global input hook.

## Current reach

Which seam a platform's selection menu actually calls is decided by the Compose foundation build
it resolves, not by this app, and the two have diverged:

| | menu Copy reaches the seam? | why |
| --- | --- | --- |
| Desktop | yes | Compose Multiplatform's desktop menu goes through `LocalTextContextMenu`; `SelectionCopyDesktopTest` proves it end to end. |
| iOS | yes, by source | Compose Multiplatform 1.11 leaves `ComposeFoundationFlags.isNewContextMenuEnabled` off and iOS is always in touch mode, so `SelectionManager` calls `LocalTextToolbar.showMenu`. Not run on a device. |
| Android | **no** | AndroidX foundation 1.11 turns `isNewContextMenuEnabled` on. The selection toolbar is then an `ActionMode` built by the new context-menu provider, whose Copy calls `SelectionManager.copy()` directly; `LocalTextToolbar` is never asked. Confirmed on an emulator: no snackbar by default, and the snackbar returns when the flag is forced off. |
| Web | **no** | `isInTouchMode` is the constant `false` on the web, so the toolbar is never shown, and the right-click menu is foundation's own `ContextMenuArea`, which also calls `SelectionManager.copy()` directly. |

Every copy, from any menu or the keyboard, still ends in `SelectionContainer`'s `onCopyHandler`
writing to `LocalClipboard`, so the copy itself is correct everywhere — only the confirmation is
missing. Restoring it on Android and the web is a design decision, not a one-line fix, and is
recorded as `CQ-KMP-003` in the [code-quality audit](../quality/code-quality-audit.md).

## Validation this needs

An earlier attempt hooked only the toolbar seam. Every target compiled, the unit tests passed, and
the feature did nothing whatsoever in the desktop app, because a mouse never reaches that seam.
`SelectionCopyDesktopTest` in `shared/src/jvmTest` now drives a real mouse drag and right-click with
`performMouseInput`, clicks the platform menu's own Copy item, and asserts against a recording
`Clipboard` and the shell's snackbar. Anything that claims to react to selection or copying has to
be proven against the input device the learner actually uses.
