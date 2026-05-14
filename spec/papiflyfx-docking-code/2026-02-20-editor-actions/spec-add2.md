# Addendum 2: Caret Blinking

Status (2026-02-20): implemented in `papiflyfx-docking-code`.

## 1. Objective

- Add visible caret blinking in the editor viewport.
- Reset blink state on caret/document interactions so the caret is immediately visible after edits or navigation.
- Tie blink activity to editor focus to avoid background repaint churn when the editor is inactive.

## 2. Behavior Matrix

| Behavior | Expected Result | Status (2026-02-20) |
| --- | --- | --- |
| Focused editor | Caret blinks on active caret line(s) | Implemented |
| Blur/inactive editor | Caret hidden and blink timer stopped | Implemented |
| Caret move/select/edit | Caret is shown immediately and blink cycle restarts | Implemented |
| Multi-caret mode | All active carets blink in sync | Implemented |
| Disposed viewport/editor | Blink timers stop; no further caret blink repaint | Implemented |

## 3. Behavioral Rules

- Caret rendering is gated by blink state (`active && visible`).
- Blink toggles repaint only caret-bearing lines (single or multi-caret) rather than forcing full redraw.
- Caret/document/selection changes reset blink and restore visible caret state.
- Blink timers are lifecycle-safe and are stopped during disposal.

## 4. Implementation Notes

- `render/Viewport.java`
  - Added caret blink scheduler (`PauseTransition` + `Timeline`).
  - Added caret blink activation/reset API and focus-safe lifecycle handling.
  - Updated full/incremental render paths to draw carets only when blink-visible.
  - Added incremental dirtying for caret lines on blink toggles.
- `api/CodeEditor.java`
  - Bound viewport caret blink activation to editor focus state.
  - Reset blink on key and mouse interaction entry points.
  - Detached focus listener and deactivated blinking during dispose.
- `render/ViewportTest.java`
  - Added blink behavior tests for toggle, reset-on-caret-move, and inactive-hidden behavior.

## 5. Validation

- Focused validation:
  - `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true -Dtest=ViewportTest,CodeEditorIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Result: `51` tests, `0` failures.
- Full module validation:
  - `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true test`
  - Result: `310` tests, `0` failures.
