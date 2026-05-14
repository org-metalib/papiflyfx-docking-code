# papiflyfx-docking-code Quality Remediation Implementation Plan

Date: 2026-02-22  
Input review: `spec/papiflyfx-docking-code-quality1/README.md`  
Target module: `papiflyfx-docking-code`

Execution progress is tracked in:
`spec/papiflyfx-docking-code-quality1/progress.md`

## 1. Objective

Resolve all issues identified in the quality review and raise module quality from `7/10` to at least `8/10` by improving:

1. Clarity and cohesion (reduce monolithic classes and mixed responsibilities).
2. Maintainability (better separation of concerns and lower change risk).
3. Testability (narrower units, stronger behavior/performance assertions).
4. Performance (remove redundant full-document work and excessive invalidation).
5. Architecture (clear orchestration boundaries).

## 2. Scope

In scope:

1. Refactor editor orchestration and command wiring around `CodeEditor`.
2. Redesign search orchestration to eliminate duplicate scans.
3. Improve `Viewport` invalidation strategy for large documents.
4. Reduce index rebuild and avoid unnecessary text slicing in line operations.
5. Remove repeated command/caret logic and consolidate behavior.
6. Add/adjust unit, integration, and benchmark tests to guard regressions.

Out of scope:

1. New end-user features unrelated to quality items.
2. Visual redesign.
3. Public API breaking changes without compatibility wrappers.

## 3. Issue-to-Work Mapping

Issue 1 (highest impact): Monolithic `CodeEditor`  
Workstreams: WS1, WS5

Issue 2: Search duplicate full-document work  
Workstreams: WS2

Issue 3: Over-broad viewport dirty range  
Workstreams: WS3

Issue 4: Full index rebuilds and full-text line slicing  
Workstreams: WS4

Issue 5: Repeated command/caret logic  
Workstreams: WS1, WS5

## 4. Target Architecture After Remediation

`CodeEditor` remains public composition root, but delegates behavior to focused collaborators:

1. `EditorCommandRegistry` (command -> handler registration only).
2. `OccurrenceSelectionService` (select next/all occurrence logic).
3. `EditorSearchCoordinator` (single search execution flow and event wiring).
4. `EditorLifecycleService` (listener/handler install-uninstall, dispose graph).
5. `CaretNavigationService` (word and vertical movement helpers).
6. `ViewportInvalidationPlanner` (dirty-line planning and change impact bounds).
7. Optional: `LineTextSlice` utility to avoid full-document intermediate strings.

## 5. Workstreams and Detailed Steps

## WS1: Decompose `CodeEditor` orchestration

Primary files:

1. `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
2. New files under `.../api/` (see deliverables below)

Detailed steps:

1. Extract command registration:
   1. Create `EditorCommandRegistry` with one method `register(EditorCommandExecutor executor)`.
   2. Move all `executor.register(...)` calls out of `CodeEditor.createCommandExecutor()`.
   3. Keep existing command handlers in `CodeEditor` initially; only move registration first.
2. Extract occurrence logic:
   1. Create `OccurrenceSelectionService` with methods:
      - `selectNextOccurrence(...)`
      - `selectAllOccurrences(...)`
      - helper for “word under caret”.
   2. Move logic from:
      - `handleSelectNextOccurrence()`
      - `handleSelectAllOccurrences()`
   3. Inject dependencies explicitly (`Document`, `SelectionModel`, `MultiCaretModel`, repaint callback).
3. Extract lifecycle wiring:
   1. Create `EditorLifecycleService` with:
      - `bindInputHandlers(...)`
      - `unbindInputHandlers(...)`
      - `bindListeners(...)`
      - `unbindListeners(...)`
   2. Move install/uninstall code from constructor and `dispose()`.
4. Preserve behavior:
   1. Keep existing public API unchanged.
   2. Keep package-private test constructor signature intact, then gradually simplify constructor wiring.

Acceptance criteria:

1. `CodeEditor` reduced below ~900 LOC in phase completion.
2. No functional regressions in `CodeEditorIntegrationTest`.
3. Disposal behavior remains identical (especially callback clearing and listener detachment).

## WS2: Remove duplicate search scans and centralize search flow

Primary files:

1. `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java`
2. `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchModel.java`
3. `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
4. New `EditorSearchCoordinator` under `.../api/` (or `.../search/`)

Detailed steps:

1. Define a single source of truth for “when search runs”.
2. Introduce `EditorSearchCoordinator` responsibilities:
   1. Refresh selection scope.
   2. Execute search once.
   3. Select nearest/current match.
   4. Push results to viewport and notify controller labels.
3. Remove duplicate scan in replace flows:
   Option A (recommended):
   1. Keep `SearchModel.replaceCurrent()` and `replaceAll()` rescanning.
   2. In `SearchController.replaceCurrent()` / `replaceAll()`, replace `executeSearch()` with:
      - `refreshMatchDisplay()`
      - notify `onSearchChanged`
      - navigate to current match if present.
   Option B:
   1. Remove rescan from model methods.
   2. Require coordinator to run exactly one explicit scan after replace.
4. Keep contract deterministic:
   1. Invalid regex never throws to UI.
   2. Search scope toggles do not trigger duplicate compute.

Acceptance criteria:

1. Exactly one scan per replace action.
2. Match count/current match remain correct after replace and replace-all.
3. No UX regression in enter/shift-enter navigation and close behavior.

## WS3: Viewport invalidation planning optimization

Primary files:

1. `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java`
2. `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/DocumentChangeEvent.java`
3. Optional new: `.../render/ViewportInvalidationPlanner.java`

Detailed steps:

1. Replace “dirty from changed line to doc end” with bounded dirty window:
   1. Derive affected range from event type + old/new lengths.
   2. Convert offsets to start/end affected lines.
   3. Expand by small safety margin (e.g., +2 lines) for wrap/highlight stability.
2. Apply visible-range intersection:
   1. If affected range is fully outside visible lines, keep minimal bookkeeping and skip per-line dirty fill.
   2. Continue forcing full redraw only for known global invalidators (multi-caret merge, font/theme change, token-map reset).
3. Keep selection/caret dirtiness behavior unchanged.
4. Add fallback:
   1. If planner cannot infer safe bounds, fall back to previous broad behavior to guarantee correctness.

Acceptance criteria:

1. Editing near start/middle of very large document avoids line-to-end invalidation.
2. No visual artifacts for selection, caret blink, and search highlights.
3. Benchmarks show improved or equal p95 render latency in 100k-line scenarios.

## WS4: Improve document/line operation performance characteristics

Primary files:

1. `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/Document.java`
2. `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/EditCommand.java`
3. `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/command/LineBlock.java`
4. `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/command/LineEditService.java`

Detailed steps:

1. Incremental undo/redo index updates:
   1. Extend `EditCommand` with optional metadata methods for index delta application.
   2. For `InsertEdit`, `DeleteEdit`, `ReplaceEdit`, apply inverse index updates during undo without full `rebuildIndex()`.
   3. Keep `rebuildIndex()` as safe fallback for unsupported commands.
2. Reduce line-block text allocation:
   1. Replace eager `LineBlock.text` full substring with:
      - offset range only, and
      - lazy retrieval via utility when needed.
   2. Adjust `LineEditService` to avoid repeated `document.getText()` checks where possible (e.g., add `Document.endsWithNewline()` helper).
3. Keep semantics identical for move/duplicate/join edge cases.

Acceptance criteria:

1. Undo/redo no longer always triggers full index rebuild for standard edit commands.
2. Line operations avoid unnecessary full-document intermediate strings.
3. Existing line operation tests and undo tests remain green.

## WS5: Consolidate repeated navigation and occurrence helpers

Primary files:

1. `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
2. New helper/service classes in `.../api/` or `.../command/`

Detailed steps:

1. Consolidate word move/select behavior:
   1. Add one helper method parameterized by direction and selection-extension mode.
   2. Route four handlers (`move/select word left/right`) through helper.
2. Consolidate delete-word behavior:
   1. Extract common “selection delete or boundary delete” flow.
3. Move word-under-caret selection logic into shared utility used by both occurrence handlers.
4. Keep public command behavior unchanged.

Acceptance criteria:

1. Duplicate code in word/occurrence handlers reduced substantially.
2. Behavior parity retained for edge cases (line start/end, empty line, punctuation boundaries).

## 6. Testing Strategy

## Unit tests

Add or update:

1. `search/SearchModelTest`:
   1. Single-scan assertions around replace/replace-all.
   2. Preserve-case and regex behavior unchanged.
2. New coordinator tests:
   1. `api/EditorSearchCoordinatorTest` (or package equivalent).
3. Document/index tests:
   1. `document/DocumentTest` incremental undo/redo index consistency.
4. Command/navigation tests:
   1. Add focused tests for new shared helper behavior.

## Integration tests (JavaFX/TestFX)

Add or extend:

1. `api/CodeEditorIntegrationTest`:
   1. Search replace actions do not double-trigger expensive scans.
   2. Disposal still detaches callbacks/listeners safely.
2. `render/ViewportTest`:
   1. Invalidation planner bounds update correctly.
   2. No redraw artifacts for caret/search/selection.

## Benchmark tests

Update `benchmark/CodeEditorBenchmarkTest` with:

1. Typing near file top with large doc.
2. Mid-file replace-all with search overlay open.
3. Undo/redo throughput over large edit history.

## 7. Implementation Phases and Milestones

## Phase A: Safety and baseline

1. Freeze current behavior with additional characterization tests where gaps exist.
2. Capture benchmark baseline numbers for comparison.

Exit:

1. Baseline tests and benchmark records committed in spec progress notes.

## Phase B: Search flow remediation (WS2)

1. Introduce coordinator.
2. Remove duplicate rescan path.
3. Update tests.

Exit:

1. Single-scan assertions pass.

## Phase C: CodeEditor decomposition (WS1 + WS5)

1. Extract command registry and occurrence service.
2. Extract lifecycle service.
3. Consolidate word/occurrence helpers.

Exit:

1. `CodeEditor` reduced in size and complexity, tests green.

## Phase D: Viewport invalidation optimization (WS3)

1. Add planner and bounded dirty ranges.
2. Verify no render regressions.

Exit:

1. Render benchmark non-regression or improvement confirmed.

## Phase E: Document/line performance work (WS4)

1. Incremental undo/redo index updates.
2. Line block allocation improvements.
3. Validate with unit + benchmark tests.

Exit:

1. Throughput and memory behavior improved or neutral with simpler hot paths.

## Phase F: Hardening and docs

1. Final pass on flaky scenarios.
2. Update README/spec progress notes with results.

Exit:

1. All tests pass headless, benchmark deltas documented, cleanup complete.

## 8. Backward Compatibility and Migration

1. Preserve public API signatures for:
   1. `CodeEditor`
   2. `CodeEditorFactory`
   3. `CodeEditorStateAdapter`
2. Keep persistence/state behavior unchanged unless explicitly versioned.
3. If any internal refactor requires behavior adjustment, add compatibility shims and tests before removal.

## 9. Risks and Mitigations

Risk: behavior drift during large decomposition  
Mitigation: extract in thin slices with characterization tests before each move.

Risk: rendering artifacts from tighter dirty bounds  
Mitigation: retain fallback full-dirty path and add visual regression checks.

Risk: undo/redo index bugs  
Mitigation: keep rebuild fallback and validate line/offset invariants in exhaustive tests.

Risk: benchmark noise obscures impact  
Mitigation: run repeated benchmark iterations and compare medians/p95.

## 10. Definition of Done

1. All five ranked debt items from `README.md` are addressed with implemented code changes.
2. Test suite passes:
   1. `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true test`
3. Benchmark evidence is captured and attached to spec progress:
   1. No regressions in large-file open/type/scroll.
   2. Search replace and undo/redo paths show measurable efficiency gain or justified parity.
4. `CodeEditor`, `SearchController`, and `Viewport` each have improved cohesion with clearly separated concerns.
5. Documentation updated to reflect new architecture and responsibilities.

## 11. Validation Commands

```bash
# compile
mvn compile -pl papiflyfx-docking-code -am

# full module tests (headless)
mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true test

# benchmark group
mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true -Dgroups=benchmark -Dsurefire.excludedGroups= test
```
