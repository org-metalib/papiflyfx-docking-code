# Addendum 0: Page Navigation and Selection

Status (2026-02-20): implemented in `papiflyfx-docking-code`.

## 1. Objective

- Add editor-native `Page Up` / `Page Down` behavior for caret movement, range selection, and viewport scrolling.
- Keep behavior deterministic across Windows/Linux and macOS mappings.
- Preserve existing docking/workspace shortcut ownership (tab switching remains outside this module).

## 2. Command Matrix

| Command | Windows / Linux | macOS | Status (2026-02-20) |
| --- | --- | --- | --- |
| Move page up | `Page Up` | `Page Up` | Implemented |
| Move page down | `Page Down` | `Page Down` | Implemented |
| Select page up | `Shift+Page Up` | `Shift+Page Up` | Implemented |
| Select page down | `Shift+Page Down` | `Shift+Page Down` | Implemented |
| Scroll page up (caret unchanged) | `Alt+Page Up` | `Alt+Page Up`, `Cmd+Page Up` | Implemented |
| Scroll page down (caret unchanged) | `Alt+Page Down` | `Alt+Page Down`, `Cmd+Page Down` | Implemented |

## 3. Behavioral Rules

- Page movement/select step is derived from current viewport height:
  - `max(1, floor(viewportHeight / lineHeight))` lines.
- Page movement preserves target column preference and clamps to the line length.
- `Shift+Page Up/Down` extends selection while preserving anchor stability.
- Scroll-only page commands change vertical scroll offset only:
  - caret and selection remain unchanged.
  - multi-caret state remains unchanged.
- `Ctrl+Page Up/Down` tab/workspace switching is out of scope for this module.

## 4. Implementation Notes

- Added command IDs:
  - `MOVE_PAGE_UP`, `MOVE_PAGE_DOWN`
  - `SELECT_PAGE_UP`, `SELECT_PAGE_DOWN`
  - `SCROLL_PAGE_UP`, `SCROLL_PAGE_DOWN`
- Added keymap entries in `KeymapTable`.
- Added `CodeEditor` handlers for page move/select and scroll-only commands.
- Added keymap + integration coverage.

## 5. Validation

- Focused validation:
  - `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true -Dtest=KeymapTableTest,CodeEditorIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Result: `52` tests, `0` failures.
- Full module validation:
  - `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true test`
  - Result: `307` tests, `0` failures.
