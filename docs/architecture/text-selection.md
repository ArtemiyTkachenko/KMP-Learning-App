# Text Selection And Copy Confirmation

Why every screen's text is selectable, and why a copy is confirmed but never performed by this app.

## What the learner sees

`App.kt` wraps `NavDisplay` in `SelectableContent`, so text on every screen can be selected. The
wrapping sits around `NavDisplay` rather than around the whole shell so the four area-navigation
labels stay out of any selection — they are controls, not content.

Selecting text does nothing on its own. Copying is offered by the platform, through whatever it
already shows: Android's selection menu (with its own Select all and Read aloud), the iOS floating
toolbar, the desktop and browser right-click menus, the copy shortcut. The app neither draws nor
alters any of them.

The one thing this app adds is the confirmation — when a copy from that content succeeds, a
snackbar reads "Copied to clipboard" (`SnackbarDuration.Short`; a repeated copy replaces the
message rather than queueing behind it).

An earlier version of this feature drew a menu of its own next to the selection, with its own Copy.
On desktop that put two menus offering the same word on screen at once, which is what a
platform-shaped affordance costs when it is reimplemented rather than used. The app now only
observes.

## Where the copy is observed

At the clipboard, not at any menu. However a copy is asked for, `SelectionContainer`'s selection
manager ends it the same way: it takes the selected text and writes it through the `LocalClipboard`
the container was composed with. `SelectableContent` provides a `CopyReportingClipboard`
(`ui/selection/CopyReportingClipboard.kt`) around the platform's clipboard at exactly that point:

```text
Android menu ─┐
iOS toolbar  ─┤
desktop menu ─┼─ SelectionContainer copy → CopyReportingClipboard → platform Clipboard
web menu     ─┤
shortcut     ─┘   (except the browser's — see below)
```

The decorator forwards reads and `nativeClipboard` untouched, hands every write to the platform
clipboard unchanged, and reports only after a **non-null** write has **returned**. A write that
throws is not reported and still throws; `setClipEntry(null)` clears the clipboard rather than
copying, so it is not reported either. The app never reconstructs the selection or writes text of
its own.

The decorator is common code: there is no `expect`/`actual` and no per-platform hook, because the
seam it observes is the same on every target.

### Input modality

The confirmation is tied to a successful clipboard write from the selection, not to a menu. A
keyboard copy that the selection handles itself crosses the same clipboard and is confirmed too:
on desktop, Android and iOS, `SelectionManager` answers the copy shortcut by calling the same
`copy()` the menus call.

The browser is the exception. There Compose leaves Ctrl/Cmd-C to the browser and answers its `copy`
event by writing to the event's `clipboardData`, which never touches `LocalClipboard`. A keyboard
copy on the web therefore stays silent. That is accepted rather than worked around: there is no
global keyboard listener and no synthesized clipboard event, because announcing a shortcut is not
worth an input hook.

### Editable descendants

`SelectableContent` wraps every routed screen, and some of them hold text fields — the Topic search
field is one. A text field cuts, copies and pastes through `LocalClipboard` too, so a reporting
clipboard visible to it would call a Cut "Copied".

`SelectionContainer` reads `LocalClipboard` in its own body, before composing its children, so the
reporting clipboard is provided only around the container and the platform clipboard is provided
again inside it, around `content`. The selection writes through the reporting one; everything under
it, including text fields, sees the platform's own.

## Validation this needs

An earlier attempt hooked only the touch toolbar. Every target compiled, the unit tests passed, and
the feature did nothing in the desktop app, because a mouse never reaches that seam; a later one
hooked menus individually and missed Android's new context menu and the browser's. Anything that
claims to react to copying has to be proven through the real selection, not only in isolation:

- `CopyReportingClipboardTest` (`shared/src/jvmTest`) pins the decorator: exact entry delegated,
  write before report, no report on failure or on a clear, reads and native clipboard delegated.
- `SelectionCopyDesktopTest` drives a real mouse drag, then either the platform menu's own Copy or
  the copy shortcut, and asserts against a recording `Clipboard` and the shell's snackbar. It also
  cuts from a text field inside `SelectableContent` and asserts the cut is not confirmed.
- Android has no UI test stack for its native menu; the selection-menu Copy and a search-field Cut
  were checked by hand on an emulator when this seam was introduced (`CQ-KMP-003` in the
  [code-quality audit](../quality/code-quality-audit.md)).
