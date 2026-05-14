# Addendum 3: Vertical Caret Column Preservation

Status (2026-02-21): implemented in `papiflyfx-docking-code`.

## 1. Objective

- Preserve preferred horizontal caret offset during repeated vertical navigation (`Up`/`Down`, including shift-extended selection and page moves).
- Prevent irreversible column collapse when traversing through shorter lines.
- Reset preferred vertical offset when non-vertical caret moves occur.

## 2. Behavior Matrix

| Behavior | Expected Result | Status (2026-02-21) |
| --- | --- | --- |
| Move down across short line then down again | Caret returns to original preferred column when line length allows | Implemented |
| Move up across short line then up again | Caret returns to original preferred column when line length allows | Implemented |
| `Shift+Up/Down` across short lines | Selection anchor remains stable and caret reuses preferred column | Implemented |
| Horizontal move between vertical moves | Preferred vertical column resets to current caret column | Implemented |

## 3. Behavioral Rules

- Vertical movement computes target column from a preferred vertical column, clamped by target line length.
- Preferred vertical column is initialized from current caret column on first vertical move in a sequence.
- Preferred vertical column is cleared by non-vertical commands and mouse-driven caret repositioning.
- Page move/select commands reuse the same preferred vertical column logic.

## 4. Implementation Notes

- `api/CodeEditor.java`
  - Added preferred vertical column state.
  - Added vertical-move helper path that preserves preferred column across repeated vertical navigation.
  - Cleared preferred column on non-vertical command paths and mouse/set-state caret repositioning.
- `api/CodeEditorIntegrationTest.java`
  - Added regressions for:
    - down/down restoration after short-line clamp,
    - horizontal-move reset behavior,
    - shift-extended vertical selection with preserved preferred column.

## 5. Validation

- Focused validation:
  - `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true -Dtest=KeymapTableTest,ViewportTest,CodeEditorIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Result: `70` tests, `0` failures.
- Full module validation:
  - `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true test`
  - Result: `313` tests, `0` failures.
