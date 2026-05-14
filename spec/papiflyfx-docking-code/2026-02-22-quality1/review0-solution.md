# Review 0 Solution Plan

Date: 2026-02-22
Scope: close the unmet criteria identified in `review0.md`
Module: `papiflyfx-docking-code`

## Unmet Criteria to Close

1. WS1 strict size target not met:
- `CodeEditor` is `1160` LOC, target is `< ~900` (from `spec/papiflyfx-docking-code-quality1/implementation.md:103`).

2. DoD evidence/documentation gap:
- Benchmark validation is completed in reality, but `spec/papiflyfx-docking-code-quality1/progress.md:125-126` still marks it as pending.
- DoD closure in progress docs is not finalized.

3. DoD cohesion item only partially met:
- `CodeEditor`, `SearchController`, `Viewport` improved, but still large (`1160`, `594`, `669` LOC respectively).

## Proposed Solution

## A. Bring `CodeEditor` below 900 LOC (mandatory closure)

### A1. Extract caret + scroll coordination
Create `EditorCaretCoordinator` and move these methods out of `CodeEditor`:
- `moveCaret`, `moveCaretVertically`, `moveCaretInternal`, `moveCaretToOffset`
- `clampLine`, `clampColumn`, `clearPreferredVerticalColumn`
- `applyScrollOffset`, `syncVerticalScrollOffsetFromViewport`, `syncGutterScroll`
- `computePageLineDelta`, `computePagePixelDelta`

Current source region to move:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:902-1007`

Expected reduction in `CodeEditor`: ~120 LOC.

### A2. Extract navigation and line-command handlers
Create `EditorNavigationController` and move these methods out of `CodeEditor`:
- Character navigation (`handleLeft/right/up/down`, page/home/end, doc start/end)
- Word navigation and deletion (`moveWord`, `deleteWord`, `findWordBoundary`)
- Line commands (`handleDeleteLine`, `handleMoveLineUp/Down`, `handleDuplicateLineUp/Down`, `handleJoinLines`)
- Multi-caret add/remove helpers (`handleAddCursorUp/Down`, `handleUndoLastOccurrence`)

Current source region to move:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:596-876`

Expected reduction in `CodeEditor`: ~280 LOC.

### A3. Move command handler map construction out of `CodeEditor`
Extend `EditorCommandRegistry` (or add `EditorCommandBindings`) so `CodeEditor` no longer owns the large `buildCommandHandlers()` block.

Current source region to move:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:457-528`

Expected reduction in `CodeEditor`: ~70 LOC.

### A4. Keep `CodeEditor` as composition root only
After extraction, `CodeEditor` should only:
- Construct collaborators.
- Wire dependencies.
- Expose public API methods.
- Delegate command execution and lifecycle.

Target outcome:
- `CodeEditor.java` <= `890` LOC.

## B. Strengthen closure evidence for disposal parity (WS1 criterion 3)

Add explicit disposal-focused tests to remove ambiguity:
- New test class: `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/EditorLifecycleServiceTest.java`
- Add/extend in `CodeEditorIntegrationTest`:
  - Assert key handlers/listeners are detached after `dispose()`.
  - Assert overlays/controllers are closed and callbacks nulled.
  - Assert no follow-up document/listener side-effects post-dispose.

Acceptance proof:
- Green tests in headless suite, including new disposal assertions.

## C. Close DoD documentation gap (mandatory closure)

Update `spec/papiflyfx-docking-code-quality1/progress.md`:
1. Replace the "Remaining Plan Item" benchmark note (`progress.md:125-126`) with actual executed benchmark results:
- `./mvnw -pl papiflyfx-docking-code -am -Dtestfx.headless=true -Dgroups=benchmark -Dsurefire.excludedGroups= test`
- Include reported metrics (open/render, typing p95, scroll p95, memory).

2. Add final DoD status table with explicit pass/fail:
- Debt items addressed.
- Full headless suite pass.
- Benchmark evidence captured.
- Cohesion improved.
- Docs updated.

3. Add completion timestamp and command logs summary.

## D. Optional but recommended cohesion follow-up

To fully remove "partially met" on DoD cohesion for `SearchController`/`Viewport`:

1. `SearchController` split:
- `SearchView` (node creation/styling only)
- `SearchActionCoordinator` (execute/replace/navigate/publish state)
- Keep `SearchController` as facade.

2. `Viewport` split:
- Extract caret blink lifecycle to `CaretBlinkController`.
- Keep planner + render passes as-is.

These are not required to satisfy the strict unmet items, but they improve maintainability and reduce future regressions.

## Execution Plan (PR slices)

1. PR-1: `CodeEditor` LOC reduction to <900
- Add `EditorCaretCoordinator`
- Add `EditorNavigationController`
- Move command bindings out of `CodeEditor`
- Update unit/integration tests

2. PR-2: Disposal evidence + documentation closure
- Add disposal-focused tests
- Update `progress.md` benchmark section and final DoD table
- Link this solution in progress notes

3. PR-3 (optional): Search/viewport cohesion split
- `SearchController` internal decomposition
- `Viewport` blink-lifecycle extraction

## Acceptance Criteria for This Solution

1. `CodeEditor` LOC < 900
- Validate with:
`wc -l papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`

2. No regressions in functional behavior
- Validate with:
`./mvnw -pl papiflyfx-docking-code -am -Dtestfx.headless=true test`

3. Benchmark evidence present in progress docs
- Validate by checking `spec/papiflyfx-docking-code-quality1/progress.md` contains benchmark command + metric outputs.

4. Final DoD explicitly marked complete in progress docs
- Validate by a checklist/status table in `progress.md`.

## Risk Controls

1. Keep backward compatibility at API surface (`CodeEditor`, `CodeEditorFactory`, `CodeEditorStateAdapter`).
2. Extract in small commits with green tests after each extraction.
3. Keep existing fallback behaviors (caret/scroll sync and command routing) unchanged while moving logic.
4. Run targeted suites after each slice:
- `DocumentTest`, `LineOperationsTest`, `ViewportTest`, `CodeEditorIntegrationTest`, `CodeEditorSearchFlowTest`.

## Expected Result

After PR-1 and PR-2:
- All unmet criteria from `review0.md` are closed.
- WS1 strict target is met.
- DoD/documentation status is consistent with actual benchmark and test evidence.
- Review conclusion can be upgraded from "not fully done" to "done".
