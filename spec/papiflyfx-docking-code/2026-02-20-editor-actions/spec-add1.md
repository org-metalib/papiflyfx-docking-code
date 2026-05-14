# Addendum 1: macOS Cmd+Home/Cmd+End Document Jump Aliases

Status (2026-02-20): implemented in `papiflyfx-docking-code`.

## 1. Objective

- Add macOS full-keyboard aliases for document-boundary navigation.
- Keep behavior identical to existing `Cmd+Up/Down` document-boundary commands.
- Preserve selection-anchor semantics for shift-extended variants.

## 2. Command Matrix

| Command | Windows / Linux | macOS | Status (2026-02-20) |
| --- | --- | --- | --- |
| Document start | `Ctrl+Home` | `Cmd+Up`, `Cmd+Home` | Implemented |
| Document end | `Ctrl+End` | `Cmd+Down`, `Cmd+End` | Implemented |
| Select to document start | `Ctrl+Shift+Home` | `Shift+Cmd+Up`, `Shift+Cmd+Home` | Implemented |
| Select to document end | `Ctrl+Shift+End` | `Shift+Cmd+Down`, `Shift+Cmd+End` | Implemented |

## 3. Behavioral Rules

- Alias bindings resolve to the same command IDs as `Cmd+Up/Down` on macOS:
  - `Cmd+Home` -> `DOCUMENT_START`
  - `Cmd+End` -> `DOCUMENT_END`
  - `Shift+Cmd+Home` -> `SELECT_TO_DOCUMENT_START`
  - `Shift+Cmd+End` -> `SELECT_TO_DOCUMENT_END`
- Selection-extending aliases must preserve anchor stability.
- Existing non-mac mappings remain unchanged.

## 4. Implementation Notes

- Updated `KeymapTable` macOS document-boundary section to include `Home/End` aliases.
- Extended `KeymapTableTest.documentBoundaries()` macOS branch with alias assertions.
- No changes were required in command dispatch or editor handlers because document-boundary commands already existed.

## 5. Validation

- Focused validation:
  - `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true -Dtest=KeymapTableTest,CodeEditorIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Result: `52` tests, `0` failures.
