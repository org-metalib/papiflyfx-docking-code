# papiflyfx-docking-code quality review (2026-02-22)

## Overall score: 7 / 10

- Clarity: `6.5/10` because behavior is mostly understandable, but key flows are spread across very large classes (`CodeEditor`, `Viewport`, `SearchController`, `SearchModel`).
- Maintainability: `6/10` because responsibilities are still mixed at top-level orchestration points, especially `CodeEditor` (`papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:58`).
- Testability: `8/10` because the module has broad unit/integration coverage, but several orchestration/performance paths are not directly asserted.
- Performance: `7/10` because rendering and lexing are optimized in many paths, but search and invalidation still trigger avoidable full-document work.
- Architecture: `7.5/10` because package boundaries are good (`api`, `document`, `render`, `lexer`, `search`, `command`), but cohesion is reduced by a few "god" classes.

Key fixes that would raise score to `8+`:
- Split `CodeEditor` into smaller collaborators for command registration, search orchestration, and lifecycle wiring.
- Remove duplicate full-document searches in replace/search flows.
- Narrow viewport dirty-region propagation instead of marking from change line to end of document.
- Reduce full index rebuilds in `Document.undo()` / `Document.redo()` paths.

## Ranked technical debt (highest impact first)

1. **Monolithic editor facade and mixed responsibilities**
   Evidence: `CodeEditor` is 1286 LOC and owns UI composition, input dispatch, command registry, caret movement, search orchestration, state mapping, and disposal (`papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:58`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:122`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:419`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:1233`).
   Impact: high long-term maintenance risk, harder onboarding, harder isolated testing for behavior changes.
   Refactor steps:
   1. Extract `EditorCommandRegistry` from `createCommandExecutor()` and command registration block.
   2. Extract `OccurrenceSelectionService` for `handleSelectNextOccurrence()` / `handleSelectAllOccurrences()`.
   3. Extract `EditorLifecycle` (listener binding/unbinding + dispose graph) from constructor/dispose.
   4. Keep `CodeEditor` as thin composition root that wires services and exposes public API.

2. **Search pipeline performs repeated full-document work**
   Evidence: `SearchController.executeSearch()` scans on each search update (`papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java:378`), `SearchModel.replaceCurrent()` and `replaceAll()` already rescan (`papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchModel.java:268`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchModel.java:287`), and controller calls `executeSearch()` again after replace (`papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java:413`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java:419`).
   Impact: avoidable extra scans on large files and added cognitive complexity in search lifecycle.
   Refactor steps:
   1. Change `SearchModel.replaceCurrent()` / `replaceAll()` contract to return an updated snapshot without auto-rescanning, or keep auto-rescan and remove controller rescan.
   2. Introduce a single `SearchSession` coordinator that owns "state changed -> run search -> publish current match" flow.
   3. Add targeted tests for "exactly one scan per replace action" using a counting `Document` test double.

3. **Viewport invalidation strategy is too broad for many edits**
   Evidence: on every document change, dirty range is set from changed line to document end (`papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java:362`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java:370`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java:371`).
   Impact: unnecessary work for large documents, especially frequent edits near top/middle.
   Refactor steps:
   1. Use `DocumentChangeEvent` shape (insert/delete/replace with lengths) to estimate bounded affected line window.
   2. Mark only visible intersection plus small safety tail; rely on token map updates for syntax churn.
   3. Add regression benchmarks for top-of-file typing and mid-file edits at 100k lines.

4. **Undo/redo path rebuilds full line index and some line operations allocate full-text slices**
   Evidence: `Document.undo()` / `redo()` call full `rebuildIndex()` (`papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/Document.java:214`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/Document.java:231`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/Document.java:292`); `LineBlock.fromLines()` extracts substring from whole document (`papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/command/LineBlock.java:44`).
   Impact: medium performance drag for large files and repeated editing operations.
   Refactor steps:
   1. Extend `EditCommand` to provide incremental line-index deltas for undo/redo.
   2. Replace full substring `LineBlock.text` capture with lazy range view or targeted line slice assembly.
   3. Add microbenchmarks for undo/redo throughput on large docs.

5. **Repeated command/caret logic reduces cohesion**
   Evidence: mirrored word movement/select methods (`papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:676`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:701`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:688`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:712`), repeated direct document scans for occurrence selection (`papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:855`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:905`).
   Impact: medium-low on runtime, medium on readability and change safety.
   Refactor steps:
   1. Replace per-command duplicated handlers with parameterized helpers (`wordMove(direction, extendSelection)` etc.).
   2. Reuse a single occurrence-finding utility across next/all-occurrence paths.
   3. Add focused unit tests around helper behavior so command handlers stay trivial.

## Unnecessary complexity and cohesion issues

- Overly large class: `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java` (1286 LOC).
- Overly large class: `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java` (658 LOC).
- Overly large class: `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java` (578 LOC).
- Overly large class: `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchModel.java` (538 LOC).
- Mixed responsibilities in `SearchController`: UI construction, search orchestration, model mutation, and theme style serialization are coupled (`papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java:69`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java:378`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java:520`).
- Mixed responsibilities in `Viewport`: invalidation policy, render graph assembly, blink lifecycle, and input projection helpers are coupled (`papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java:362`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java:449`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java:614`).
- Repeated logic: word navigation/select duplication in `CodeEditor` (`papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:676`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:701`).
- Repeated logic: occurrence selection scanning duplicated across two methods (`papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:820`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:888`).

## Simplification plan to improve cohesion

1. Create `EditorComposition` (or keep `CodeEditor`) only for wiring, and move behavior into domain services such as `CaretNavigationService`, `OccurrenceSelectionService`, and `EditorLifecycleService`.
2. Split search layer into `SearchView` (JavaFX nodes only), `SearchPresenter` (event flow), and `SearchEngine` (matching/replacement logic, mostly current `SearchModel`).
3. Split viewport internals into `ViewportInvalidationPlanner`, `ViewportRenderer`, and `CaretBlinkController`.
4. Keep existing tests, then add focused unit tests for each new service while shrinking integration-test surface.
