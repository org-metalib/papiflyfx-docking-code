# PapiflyFX Code Editor — Keyboard & Mouse Reference

## Keyboard Shortcuts

### Editing

| Action | Windows / Linux | macOS |
|---|---|---|
| Undo | `Ctrl+Z` | `Cmd+Z` |
| Redo | `Ctrl+Y` or `Ctrl+Shift+Z` | `Cmd+Shift+Z` |
| Copy | `Ctrl+C` | `Cmd+C` |
| Cut | `Ctrl+X` | `Cmd+X` |
| Paste | `Ctrl+V` | `Cmd+V` |
| Select all | `Ctrl+A` | `Cmd+A` |
| Backspace | `Backspace` | `Delete` |
| Delete forward | `Delete` | `Fn+Delete` |
| New line | `Enter` | `Enter` |

### Caret Navigation

| Action | Windows / Linux | macOS |
|---|---|---|
| Move left / right | `Left` / `Right` | `Left` / `Right` |
| Move up / down | `Up` / `Down` | `Up` / `Down` |
| Move page up / down | `Page Up` / `Page Down` | `Page Up` / `Page Down` |
| Line start | `Home` | `Home` |
| Line end | `End` | `End` |
| Move by word left | `Ctrl+Left` | `Alt+Left` |
| Move by word right | `Ctrl+Right` | `Alt+Right` |
| Document start | `Ctrl+Home` | `Cmd+Up` or `Cmd+Home` |
| Document end | `Ctrl+End` | `Cmd+Down` or `Cmd+End` |

### Selection

| Action | Windows / Linux | macOS |
|---|---|---|
| Select left / right | `Shift+Left` / `Shift+Right` | `Shift+Left` / `Shift+Right` |
| Select up / down | `Shift+Up` / `Shift+Down` | `Shift+Up` / `Shift+Down` |
| Select page up / down | `Shift+Page Up` / `Shift+Page Down` | `Shift+Page Up` / `Shift+Page Down` |
| Select to line start | `Shift+Home` | `Shift+Home` |
| Select to line end | `Shift+End` | `Shift+End` |
| Select word left | `Ctrl+Shift+Left` | `Alt+Shift+Left` |
| Select word right | `Ctrl+Shift+Right` | `Alt+Shift+Right` |
| Select to document start | `Ctrl+Shift+Home` | `Cmd+Shift+Up` or `Cmd+Shift+Home` |
| Select to document end | `Ctrl+Shift+End` | `Cmd+Shift+Down` or `Cmd+Shift+End` |

### Viewport Navigation

| Action | Windows / Linux | macOS |
|---|---|---|
| Scroll page up (caret unchanged) | `Alt+Page Up` | `Alt+Page Up` or `Cmd+Page Up` |
| Scroll page down (caret unchanged) | `Alt+Page Down` | `Alt+Page Down` or `Cmd+Page Down` |

### Word Deletion

| Action | Windows / Linux | macOS |
|---|---|---|
| Delete word left | `Ctrl+Backspace` | `Alt+Delete` |
| Delete word right | `Ctrl+Delete` | `Alt+Fn+Delete` |

### Line Operations

| Action | Windows / Linux | macOS |
|---|---|---|
| Delete line | `Ctrl+Shift+K` | `Cmd+Shift+K` |
| Move line up | `Alt+Up` | `Alt+Up` |
| Move line down | `Alt+Down` | `Alt+Down` |
| Duplicate line up | `Alt+Shift+Up` | `Alt+Shift+Up` |
| Duplicate line down | `Alt+Shift+Down` | `Alt+Shift+Down` |
| Join lines | `Ctrl+J` | `Cmd+J` |

### Multi-Caret

| Action | Windows / Linux | macOS |
|---|---|---|
| Select next occurrence | `Ctrl+D` | `Cmd+D` |
| Select all occurrences | `Ctrl+Shift+L` | `Cmd+Shift+L` |
| Add cursor up | `Ctrl+Alt+Up` | `Cmd+Alt+Up` |
| Add cursor down | `Ctrl+Alt+Down` | `Cmd+Alt+Down` |
| Undo last occurrence | `Ctrl+U` | `Cmd+U` |

### Search & Navigation

| Action | Windows / Linux | macOS |
|---|---|---|
| Find | `Ctrl+F` | `Cmd+F` |
| Go to line | `Ctrl+G` | `Cmd+G` |
| Close search | `Escape` | `Escape` |

## Mouse Actions

| Action | Gesture |
|---|---|
| Place caret | Click |
| Range selection | Click and drag |
| Extend selection | `Shift+Click` |

## Notes

- **Word boundaries** follow the `[A-Za-z0-9_]` word-character class. Underscores are treated as part of a word (e.g. `foo_bar` is one word). Punctuation and whitespace act as boundaries.
- **Line operations with selection** — when a selection spans multiple lines, delete/move/duplicate line commands act on the entire range of selected lines.
- **Single-step undo** — each line operation (delete, move, duplicate, join) is recorded as a single undo step.
- **Cross-line word navigation** — moving word-left at column 0 jumps to the end of the previous line; moving word-right at end of line jumps to the start of the next line.
- **macOS full-keyboard document aliases** — `Cmd+Home/End` and `Cmd+Shift+Home/End` are supported as aliases for document boundary move/select commands.
- **Caret blinking** — the caret blinks while the editor is focused, resets to visible after caret/edit interactions, and hides when the editor is unfocused or disposed.
- **Vertical preferred column** — repeated `Up/Down` (and shift/page variants) preserve the intended horizontal offset across shorter lines; non-vertical moves reset the preferred column baseline.
- **Page move/select step** — page commands use viewport-derived step: `max(1, floor(viewportHeight / lineHeight))` lines.
- **Scroll-only page commands** — `Alt/Cmd+Page Up/Down` scroll the viewport without moving caret or selection anchor.
- **Workspace tab switching** — `Ctrl/Cmd+Page Up/Down` is intentionally not mapped by the editor and remains available to workspace-level tab navigation.
- **Multi-caret editing** — when multiple carets are active, typing, backspace, delete, enter, cut, and paste fan out to all caret positions. Edits are processed in reverse offset order to preserve correctness. The entire operation is a single undo step.
- **Multi-caret collapse** — any single-caret navigation command (arrows, Home/End, word nav, document boundaries) or mouse click collapses back to a single caret.
- **Select next occurrence** (`Cmd/Ctrl+D`) — first press selects the word under the caret; subsequent presses find and select the next match, adding a secondary caret.
- **Undo last occurrence** (`Cmd/Ctrl+U`) — removes the most recently added occurrence selection.
- **Session persistence (`v2`)** — save/restore now preserves primary selection anchor and all secondary carets/selections. Older `v1` session payloads restore with anchor=caret and no secondary carets.
- **Restore hardening** — folded-line state is sanitized (non-negative deduplicated list), secondary-carets are deduplicated and capped at `2048` entries during restore.
- **Lifecycle hardening** — disposed editors ignore further keyboard/mouse/scroll input callbacks and detach caret/scroll mirror listeners.
