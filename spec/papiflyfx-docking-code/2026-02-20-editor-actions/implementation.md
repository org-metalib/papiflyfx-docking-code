# PapiflyFX Code Editor Actions Implementation Plan

Scope: deliver keyboard/mouse action parity defined in `spec/papiflyfx-docking-code-editor-actions/spec.md` for module `papiflyfx-docking-code`.

## 1. Goals

- Preserve current stable editing behavior.
- Add missing word/line actions.
- Add multi-caret and rectangular selection support.
- Add page-level caret/selection/scroll navigation commands.
- Add lifecycle-safe caret blinking behavior for active editors.
- Preserve preferred caret column during repeated vertical movement.
- Keep rendering/performance and docking persistence contracts intact.

## 2. Target Areas

Primary classes/packages expected to change:

- `org.metalib.papifly.fx.code.api.CodeEditor`
- `org.metalib.papifly.fx.code.render.SelectionModel` (or multi-caret successor model)
- `org.metalib.papifly.fx.code.document.Document`
- `org.metalib.papifly.fx.code.state.EditorStateData`
- `org.metalib.papifly.fx.code.state.EditorStateCodec`
- `org.metalib.papifly.fx.code.api.CodeEditorStateAdapter`

## 3. Delivery Phases

## Phase 0: Command Abstraction Baseline -- COMPLETE (2026-02-20)

Tasks:
- [x] Introduce command IDs for all Profile A/B actions.
- [x] Add platform keymap tables (Windows, macOS).
- [x] Route existing input handling through command dispatch.

Deliverables:
- `command/EditorCommand.java` — 38 command IDs
- `command/KeyBinding.java` — key combination record
- `command/KeymapTable.java` — platform-aware keymap (macOS Alt vs Windows Ctrl for word nav)
- `command/WordBoundary.java` — word boundary utility
- Refactored `CodeEditor.handleKeyPressed` → `KeymapTable.resolve()` + `executeCommand()`
- `command/KeymapTableTest.java` — 13 tests

Exit criteria:
- [x] Existing behavior unchanged.
- [x] Tests pass with command-based dispatch enabled (263 total, 0 failures).

## Phase 1: Word + Document Navigation -- COMPLETE (2026-02-20)

Tasks:
- [x] Add word navigation and word selection expansion.
- [x] Add word deletion left/right.
- [x] Add document start/end and select-to-boundary shortcuts.

Deliverables:
- 8 new handler methods in `CodeEditor`: `handleMoveWordLeft/Right`, `handleSelectWordLeft/Right`, `handleDeleteWordLeft/Right`, `handleDocumentStart/End`
- Cross-line boundary support for word navigation
- `command/WordBoundaryTest.java` — 26 edge-case tests

Exit criteria:
- [x] All Profile B word/document commands are functional.
- [x] Unit tests cover token/whitespace/punctuation edge cases.

## Phase 2: Line Operations -- COMPLETE (2026-02-20)

Tasks:
- [x] Add delete line, move line up/down, duplicate line up/down, join lines.
- [x] Ensure operations work with and without selection.
- [x] Ensure each action is single-step undo/redo.

Deliverables:
- 6 new handler methods in `CodeEditor`: `handleDeleteLine`, `handleMoveLineUp/Down`, `handleDuplicateLineUp/Down`, `handleJoinLines`
- `command/LineOperationsTest.java` — 15 tests (delete, move, duplicate, join with undo coverage)

Exit criteria:
- [x] Line actions match expected behavior across single and multi-line selections.
- [x] Undo/redo tests pass for each action.

## Phase 3: Multi-Caret Core -- COMPLETE (2026-02-20)

Tasks:
- [x] Extend selection model to track multiple carets/selections.
- [x] Implement add-next-occurrence, select-all-occurrences, add-cursor-up/down, undo-last-occurrence.
- [x] Normalize overlapping carets/ranges deterministically.
- [x] Implement compound edit mechanism for single-step undo of multi-caret edits.
- [x] Fan-out editing (typing/backspace/delete/enter/cut/paste) at all carets.
- [x] Multi-caret collapse on single-caret navigation and mouse click.
- [x] Multi-caret rendering in Viewport (selections + caret bars).

Deliverables:
- `command/CaretRange.java` — immutable record with start/end/offset helpers
- `command/MultiCaretModel.java` — primary + secondary carets, occurrence stack, normalize/merge
- `document/CompoundEdit.java` — grouped edit for single-step undo
- `Document.beginCompoundEdit()` / `endCompoundEdit()` API
- 5 new command handlers in `CodeEditor` + `executeAtAllCarets()` fan-out helper
- Multi-caret rendering in `Viewport.drawSelection/drawCaret/drawSelectionForLine`
- `command/MultiCaretModelTest.java` — 8 tests
- `command/MultiCaretEditTest.java` — 5 tests
- `document/CompoundEditTest.java` — 6 tests
- `command/KeymapTableTest.java` — 1 new test (5 bindings)

Exit criteria:
- [x] Multi-caret edit fan-out works for insert/delete/replace.
- [x] Deterministic order and collapse rules are covered by tests.
- [x] All 283 tests pass (0 failures).

## Phase 4: Mouse Multi-Caret + Box Selection -- COMPLETE (2026-02-20)

Tasks:
- [x] Add double-click word selection and triple-click line selection.
- [x] Add `Alt/Option+Click` secondary caret creation.
- [x] Add rectangular box selection via `Shift+Alt+Drag` / `Shift+Option+Drag`.
- [x] Add middle-mouse box selection (Windows/Linux).
- [x] Add `handleMouseReleased` handler for box selection state cleanup.

Deliverables:
- `MultiCaretModel.setSecondaryCarets(List<CaretRange>)` — bulk replacement helper for box selection drag rebuild
- 7 new handler methods in `CodeEditor`: `handleDoubleClick`, `handleTripleClick`, `handleAltClick`, `startBoxSelection`, `updateBoxSelection`, `handleMouseDragged` (box branch), `handleMouseReleased`
- 3 new fields in `CodeEditor`: `boxSelectionActive`, `boxAnchorLine`, `boxAnchorCol`
- Restructured `handleMousePressed` with priority dispatch: triple-click → double-click → Alt+Click → Shift+Alt+Click → Middle-button → normal click
- `api/MouseGestureTest.java` — 9 integration tests

Exit criteria:
- [x] Mouse gesture matrix passes headless TestFX tests.
- [x] Box selection creates per-line carets with column clamping.
- [x] All 292 tests pass (0 failures).

## Phase 5: Persistence v2 -- COMPLETE (2026-02-20)

Tasks:
- [x] Extend `EditorStateData` and codec for multi-caret state.
- [x] Add adapter migration from `v1` single-caret state.
- [x] Keep tolerant restore for missing/invalid fields.

Deliverables:
- `state/CaretStateData.java` — serialized caret record for secondary caret payloads.
- `state/EditorStateData.java` — v2 fields (`anchorLine`, `anchorColumn`, `secondaryCarets`) + backward-compatible 6-arg constructor.
- `state/EditorStateCodec.java` — v2 key encode/decode and tolerant secondary-caret parsing.
- `api/CodeEditor.java` — capture/apply persistence upgraded to include primary selection + secondary carets.
- `api/CodeEditorStateAdapter.java` — `VERSION = 2`, with `decodeV2()`, `migrateV1ToV2()`, `migrateV0ToV2()`.
- `state/EditorStateCodecTest.java` — expanded v2 round-trip/migration coverage.
- `api/CodeEditorIntegrationTest.java` — editor-level capture/apply + adapter migration assertions.

Exit criteria:
- [x] Round-trip tests for `v2` pass.
- [x] `v1` restore compatibility remains green.
- [x] Full module headless suite passes after migration (`298` tests, `0` failures).

## Phase 6: Hardening and Performance -- COMPLETE (2026-02-20)

Tasks:
- [x] Re-run typing/scroll benchmarks with advanced actions enabled.
- [x] Add regressions for input handler disposal and listener cleanup.
- [x] Ensure no docking restore regressions.

Deliverables:
- `api/CodeEditor.java` — disposal guards for input/scroll paths; bounded + deduplicated secondary-caret restore (`2048` cap) and primary-duplicate filtering.
- `state/EditorStateCodec.java` — payload sanitization: non-negative deduplicated folded lines, bounded + deduplicated secondary caret decoding/encoding (`2048` cap).
- `render/Viewport.java` — multi-caret render-path optimization: one active-caret snapshot per redraw reused across full/incremental paint paths.
- `api/CodeEditorIntegrationTest.java` — disposal listener-detach regressions and large-state secondary-caret cap regression.
- `api/CodeEditorDockingIntegrationTest.java` — docking session round-trip now asserts primary anchor + secondary carets.
- `state/EditorStateCodecTest.java` — hardening coverage for folded-line sanitization and secondary-caret cap/dedupe behavior.
- `benchmark/CodeEditorBenchmarkTest.java` — advanced benchmarks added: multi-caret typing latency p95 and multi-caret scroll rendering p95.

Exit criteria:
- [x] Latency and memory remain within existing module thresholds.
- [x] Full module test suite passes in headless mode.

## Addendum 0: Page Navigation and Selection -- COMPLETE (2026-02-20)

Tasks:
- [x] Add command IDs for page move/select and scroll-only behavior.
- [x] Bind `Page Up`/`Page Down` combinations in platform-aware keymap.
- [x] Implement caret move/select by viewport page size.
- [x] Implement scroll-only page commands that preserve caret/selection state.
- [x] Add keymap and editor integration tests.

Deliverables:
- `command/EditorCommand.java` — 6 new command IDs (`MOVE_PAGE_*`, `SELECT_PAGE_*`, `SCROLL_PAGE_*`).
- `command/KeymapTable.java` — key bindings for page commands:
  - `PgUp/PgDn`, `Shift+PgUp/PgDn`
  - `Alt+PgUp/PgDn` (all platforms)
  - `Cmd+PgUp/PgDn` (macOS)
- `api/CodeEditor.java` — page command handlers and viewport-derived page-step computation.
- `command/KeymapTableTest.java` — page binding coverage.
- `api/CodeEditorIntegrationTest.java` — caret move/select and scroll-only behavior coverage.

Exit criteria:
- [x] Page commands execute through command dispatch and keymap resolution.
- [x] Caret/anchor semantics are deterministic for page move/select.
- [x] Scroll-only page commands do not move caret.
- [x] Full module headless suite remains green.

## Addendum 1: macOS Cmd+Home/Cmd+End Document Jump Aliases -- COMPLETE (2026-02-20)

Tasks:
- [x] Add `Cmd+Home` / `Cmd+End` bindings on macOS for document boundary navigation.
- [x] Add `Shift+Cmd+Home` / `Shift+Cmd+End` bindings on macOS for selection-to-document-boundary behavior.
- [x] Extend keymap tests to verify the new alias resolution.

Deliverables:
- `command/KeymapTable.java` — macOS aliases added for document start/end and selection-to-document start/end.
- `command/KeymapTableTest.java` — document-boundary assertions now cover `Home/End` aliases on macOS.

Exit criteria:
- [x] macOS `Cmd+Home/End` behavior matches existing `Cmd+Up/Down` document-boundary commands.
- [x] Shift-modified aliases preserve selection-anchor semantics via existing document selection commands.
- [x] Focused headless regression suite remains green.

## Addendum 2: Caret Blinking -- COMPLETE (2026-02-20)

Tasks:
- [x] Add viewport caret blink scheduling and visibility toggling.
- [x] Reset blink cycle on caret/document interactions.
- [x] Bind blink activity to editor focus and stop lifecycle timers on dispose.
- [x] Add deterministic viewport blink behavior tests.

Deliverables:
- `render/Viewport.java` — caret blink scheduler (`PauseTransition` + `Timeline`), active/visible gating, caret-line dirty repaint path.
- `api/CodeEditor.java` — focus listener wiring for blink activation, interaction-triggered blink resets, disposal cleanup.
- `render/ViewportTest.java` — blink toggle/reset/inactive assertions.
- `spec/papiflyfx-docking-code-editor-actions/spec-add2.md` — addendum spec/progress notes.

Exit criteria:
- [x] Caret blinks while editor focus is active.
- [x] Caret hides when editor is inactive/disposed.
- [x] Caret becomes immediately visible after caret movement/edit interactions.
- [x] Full module headless suite remains green.

## Addendum 3: Vertical Caret Column Preservation -- COMPLETE (2026-02-21)

Tasks:
- [x] Preserve preferred horizontal column for repeated vertical navigation (`Up/Down`, `Shift+Up/Down`, page move/select).
- [x] Reset preferred vertical column after non-vertical movement.
- [x] Add integration regressions for short-line clamp/restore behavior.

Deliverables:
- `api/CodeEditor.java` — preferred vertical column state + vertical movement helpers with non-vertical reset behavior.
- `api/CodeEditorIntegrationTest.java` — preferred-column restore/reset and shift-selection vertical regressions.
- `spec/papiflyfx-docking-code-editor-actions/spec-add3.md` — addendum spec/progress notes.

Exit criteria:
- [x] Moving down/up through short lines does not permanently lose preferred column.
- [x] Shift-extended vertical moves keep anchor stability while preserving preferred column.
- [x] Horizontal moves reset preferred column baseline.
- [x] Focused + full module headless suites remain green.

## 4. Risk Notes

- Multi-caret can increase complexity in text mutation ordering.
- Box selection can conflict with viewport coordinate conversion logic.
- Persistence expansion must not break existing saved sessions.

Mitigations:
- Normalize edit ranges before mutating document.
- Keep command tests independent of rendering tests.
- Maintain strict adapter version gates and fallback behavior.

## 5. Validation Commands

```bash
# Module tests
mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true test

# Focused keymap + viewport + integration coverage (includes Addenda 0/1/2/3)
mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true \
  -Dtest=KeymapTableTest,ViewportTest,CodeEditorIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

# Docks + code integration coverage
mvn -pl papiflyfx-docking-code,papiflyfx-docking-docks -am -Dtestfx.headless=true test

# Benchmark-tagged suite (includes Phase 6 multi-caret perf checks)
mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true \
  -Dsurefire.excludedGroups= -Dgroups=benchmark \
  -Dtest=CodeEditorBenchmarkTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```
