# PapiflyFX Code Editor Actions Specification

This document defines the keyboard and mouse action requirements for the `papiflyfx-docking-code` module, normalized from `spec/papiflyfx-docking-code-editor-actions/README.md`.

## 1. Objective

- Define a clear, testable command set for text editing interactions.
- Align behavior with common modern editor conventions (VS Code / Sublime style).
- Keep platform parity between Windows and macOS.
- Include page-level navigation and selection semantics for large-file workflows.
- Support macOS full-keyboard aliases for document boundaries (`Cmd+Home/End`).
- Provide deterministic caret blinking behavior tied to editor activity/focus.
- Preserve preferred caret column during repeated vertical movement across short lines.

## 2. Scope

### 2.1 In Scope

- Caret navigation and selection commands.
- Editing shortcuts (undo/redo, line actions, word actions).
- Multi-cursor and rectangular selection actions.
- Mouse click/drag gestures that affect editor state.

### 2.2 Out of Scope

- IDE semantic navigation (`go to definition`, symbol index).
- Custom keybinding UI.
- Language-server behavior and refactor actions.
- Workspace tab cycling shortcuts (`Ctrl/Cmd+PageUp/PageDown`).

## 3. Command Model

Platform modifiers:

| Logical Modifier | Windows | macOS |
| --- | --- | --- |
| `Primary` | `Ctrl` | `Cmd` |
| `Word` | `Ctrl` | `Option` |
| `MultiCursor` | `Alt` | `Option` |

The editor should bind shortcuts to command IDs, then execute behavior by command ID (not by hardcoded key-branches only).

## 4. Keyboard Actions

### 4.1 Core Actions (Profile A)

| Command | Windows | macOS | Status (2026-02-20) |
| --- | --- | --- | --- |
| Undo | `Ctrl+Z` | `Cmd+Z` | Implemented |
| Redo | `Ctrl+Y`, `Ctrl+Shift+Z` | `Shift+Cmd+Z` | Implemented |
| Copy | `Ctrl+C` | `Cmd+C` | Implemented |
| Cut | `Ctrl+X` | `Cmd+X` | Implemented |
| Paste | `Ctrl+V` | `Cmd+V` | Implemented |
| Select all | `Ctrl+A` | `Cmd+A` | Implemented |
| Move by char | `Left/Right` | `Left/Right` | Implemented |
| Move by line | `Up/Down` | `Up/Down` | Implemented |
| Extend selection | `Shift+Arrows` | `Shift+Arrows` | Implemented |
| Line start/end | `Home/End` | `Cmd+Left/Right` | Implemented |
| Find | `Ctrl+F` | `Cmd+F` | Implemented |
| Go to line | `Ctrl+G` | `Cmd+G` | Implemented |
| Close find | `Escape` | `Escape` | Implemented |

### 4.2 Required Actions (Profile B)

| Command | Windows | macOS | Status (2026-02-20) |
| --- | --- | --- | --- |
| Move by word | `Ctrl+Left/Right` | `Option+Left/Right` | Implemented |
| Select by word | `Ctrl+Shift+Left/Right` | `Option+Shift+Left/Right` | Implemented |
| Delete word left/right | `Ctrl+Backspace/Delete` | `Option+Delete` / `Option+Fn+Delete` | Implemented |
| Document start/end | `Ctrl+Home/End` | `Cmd+Up/Down`, `Cmd+Home/End` | Implemented |
| Select to doc bounds | `Ctrl+Shift+Home/End` | `Shift+Cmd+Up/Down`, `Shift+Cmd+Home/End` | Implemented |
| Delete current line | `Ctrl+Shift+K` | `Cmd+Shift+K` | Implemented |
| Move line up/down | `Alt+Up/Down` | `Option+Up/Down` | Implemented |
| Duplicate line up/down | `Shift+Alt+Up/Down` | `Shift+Option+Up/Down` | Implemented |
| Join lines | `Ctrl+J` | `Cmd+J` | Implemented |
| Select next occurrence | `Ctrl+D` | `Cmd+D` | Implemented |
| Select all occurrences | `Ctrl+Shift+L` | `Cmd+Shift+L` | Implemented |
| Add cursor up/down | `Ctrl+Alt+Up/Down` | `Cmd+Option+Up/Down` | Implemented |
| Undo last occurrence | `Ctrl+U` | `Cmd+U` | Implemented |

### 4.3 Optional Actions (Profile C)

| Command | Windows | macOS |
| --- | --- | --- |
| Skip occurrence in sequence | `Ctrl+K Ctrl+D` | `Cmd+K Cmd+D` |
| Expand/shrink semantic selection | `Shift+Alt+Right/Left` | `Ctrl+Shift+Cmd+Right/Left` |
| Zoom with wheel | `Ctrl+Wheel` | `Cmd+Wheel` |

### 4.4 Addendum 0: Page Navigation and Selection

| Command | Windows | macOS | Status (2026-02-20) |
| --- | --- | --- | --- |
| Move page up | `Page Up` | `Page Up` | Implemented |
| Move page down | `Page Down` | `Page Down` | Implemented |
| Select page up | `Shift+Page Up` | `Shift+Page Up` | Implemented |
| Select page down | `Shift+Page Down` | `Shift+Page Down` | Implemented |
| Scroll page up without caret move | `Alt+Page Up` | `Alt+Page Up`, `Cmd+Page Up` | Implemented |
| Scroll page down without caret move | `Alt+Page Down` | `Alt+Page Down`, `Cmd+Page Down` | Implemented |

### 4.5 Addendum 1: macOS Cmd+Home/Cmd+End Document Jump Aliases

| Command | Windows | macOS | Status (2026-02-20) |
| --- | --- | --- | --- |
| Document start alias | N/A | `Cmd+Home` | Implemented |
| Document end alias | N/A | `Cmd+End` | Implemented |
| Select to document start alias | N/A | `Shift+Cmd+Home` | Implemented |
| Select to document end alias | N/A | `Shift+Cmd+End` | Implemented |

### 4.6 Addendum 2: Caret Blinking

| Behavior | Windows | macOS | Status (2026-02-20) |
| --- | --- | --- | --- |
| Active caret blink while editor is focused | Same | Same | Implemented |
| Caret hidden while editor is unfocused/disposed | Same | Same | Implemented |
| Blink reset on caret/doc interaction | Same | Same | Implemented |

### 4.7 Addendum 3: Vertical Caret Column Preservation

| Behavior | Windows | macOS | Status (2026-02-21) |
| --- | --- | --- | --- |
| Up/down restores preferred column after short-line clamp | Same | Same | Implemented |
| Shift+up/down preserves anchor + preferred column | Same | Same | Implemented |
| Non-vertical moves reset preferred vertical column | Same | Same | Implemented |

## 5. Mouse Actions

| Action | Windows | macOS | Status (2026-02-20) |
| --- | --- | --- | --- |
| Set caret | Click | Click | Implemented |
| Drag range selection | Drag | Drag | Implemented |
| Extend selection | `Shift+Click` | `Shift+Click` | Implemented |
| Select word | Double click | Double click | Implemented |
| Select line | Triple click | Triple click | Implemented |
| Add caret at point | `Alt+Click` | `Option+Click` | Implemented |
| Box selection drag | `Shift+Alt+Drag` | `Shift+Option+Drag` | Implemented |
| Box selection by middle mouse | Middle-drag | N/A | Implemented |

## 6. Behavioral Rules

- Shift-extended operations must preserve anchor stability.
- Multi-caret edits must be deterministic for overlapping carets/selections.
- Line operations act on complete line spans when selection exists.
- Word boundaries must be deterministic (`[A-Za-z0-9_]` word class baseline).
- Undo/redo should treat each line action as a single history step.
- Page move/select uses viewport-derived line deltas (`max(1, floor(viewportHeight / lineHeight))`).
- Scroll-only page commands must not move caret or selection anchors.
- On macOS, `Cmd+Home/End` and `Cmd+Up/Down` must execute the same document-boundary commands.
- Caret blinking resets to visible on caret/document interactions and only paints while editor focus is active.
- Vertical movement (`Up/Down`, `Shift+Up/Down`, page move/select) must preserve preferred horizontal offset until a non-vertical move resets it.

## 7. Persistence Impact

Persistence `v2` is implemented (2026-02-20).  
`EditorStateData` now captures:

- primary caret position (`cursorLine`, `cursorColumn`),
- primary selection anchor (`anchorLine`, `anchorColumn`),
- secondary carets/selections (`secondaryCarets` list).

`v1` payload restore remains supported via migration defaults:
- anchor defaults to cursor,
- secondary carets default to empty.

Phase 6 hardening (2026-02-20) adds:
- bounded secondary-caret restore payloads (cap: 2048),
- folded-line payload sanitization (non-negative, deduplicated),
- deterministic duplicate filtering for restored secondary carets,
- disposal guards on input/scroll event handlers.

## 8. Acceptance Criteria

| ID | Criterion |
| --- | --- |
| AC-1 | Profile A behavior remains stable and fully tested in headless CI. |
| AC-2 | Profile B keyboard shortcuts behave correctly on Windows and macOS mappings. |
| AC-3 | Profile B mouse gestures for multi-caret and box selection are deterministic. |
| AC-4 | Undo/redo correctness is preserved after multi-caret and line operations. |
| AC-5 | Session save/restore remains backward-compatible (`v1`) and supports new action state (`v2`). |
| AC-6 | Hardening/performance regressions are covered: disposal/listener cleanup stays safe and benchmark thresholds remain green. |
| AC-7 | `Page Up`/`Page Down` move/select/scroll-only command semantics are deterministic and covered by keymap + integration tests. |
| AC-8 | macOS `Cmd+Home/End` aliases map to document-boundary commands with matching selection-extension behavior. |
| AC-9 | Caret blinking behavior is deterministic (toggle, reset-on-interaction, inactive hidden state) and covered by viewport tests. |
| AC-10 | Vertical caret movement preserves preferred column across short lines and resets correctly after non-vertical moves. |

## 9. Verification Strategy

- Unit tests for word boundaries, line action semantics, and multi-caret normalization.
- JavaFX/TestFX integration tests for key/mouse gesture matrices.
- JavaFX/TestFX integration tests for page move/select and scroll-only behavior.
- JavaFX/TestFX integration tests for vertical preferred-column behavior across short lines.
- Keymap unit tests for macOS `Cmd+Home/End` and `Shift+Cmd+Home/End` alias resolution.
- Viewport rendering tests for caret blink toggle/reset/inactive semantics.
- Docking integration tests for state round-trip and fallback order (adapter -> factory -> placeholder).
- Benchmark-tagged validation for large-file load, typing latency, scroll latency, multi-caret latency, and memory overhead.
