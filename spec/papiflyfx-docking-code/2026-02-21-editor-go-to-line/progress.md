# PapiflyFX Code Editor Go-To-Line Progress

**Date:** 2026-02-21  
**Status:** Implemented

## Summary

Implemented a new integrated go-to-line dialog as a themed overlay, replacing the generic JavaFX `TextInputDialog` flow.

## Completed

- Added `GoToLineController` overlay:
  - compact floating panel
  - line range hint (`1..N`)
  - numeric-only input
  - Enter to confirm
  - Escape / close / cancel to dismiss
  - validation state for invalid input
- Added dedicated stylesheet:
  - `papiflyfx-docking-code/src/main/resources/org/metalib/papifly/fx/code/api/go-to-line-overlay.css`
- Wired into `CodeEditor`:
  - `Ctrl/Cmd+G` now opens the overlay
  - open search/replace closes go-to-line overlay
  - open go-to-line closes search overlay
  - theme propagation via `setEditorTheme(...)`
  - key handling suppresses editor typing when overlay is focused
  - Escape closes either open overlay
  - disposal clears callbacks and closes overlay safely
- Added/updated tests:
  - `CodeEditorIntegrationTest`:
    - go-to-line shortcut opens overlay
    - Escape closes overlay
  - `CodeEditorThemeIntegrationTest`:
    - go-to-line controller receives direct and mapped themes

## Validation

```bash
mvn -pl papiflyfx-docking-code -Dtest=CodeEditorIntegrationTest,CodeEditorThemeIntegrationTest -Dtestfx.headless=true test
# Tests run: 49, Failures: 0, Errors: 0, Skipped: 0
```

## Files Changed

- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/GoToLineController.java`
- `papiflyfx-docking-code/src/main/resources/org/metalib/papifly/fx/code/api/go-to-line-overlay.css`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorIntegrationTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeIntegrationTest.java`
