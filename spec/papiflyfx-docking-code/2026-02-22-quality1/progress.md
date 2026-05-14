# papiflyfx-docking-code Quality Remediation Progress

Date: 2026-02-22  
Plan reference: `spec/papiflyfx-docking-code-quality1/implementation.md`

## Status Summary

- Remediation across WS1-WS5 is implemented with validation.
- Full module headless test suite passes.
- Benchmark validation is executed and recorded.

## Workstream Progress

## WS1: Decompose `CodeEditor` orchestration

Status: `completed` (target LOC reduction met)

Implemented:
- Added `EditorCommandRegistry` default wiring path and moved command map construction out of `CodeEditor`:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorCommandRegistry.java`
- Added `EditorCaretCoordinator` for caret movement and scroll synchronization:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorCaretCoordinator.java`
- Added `EditorNavigationController` for navigation/line/multi-caret command behavior:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorNavigationController.java`
- Kept `CodeEditor` as composition root/facade and delegated orchestration to collaborators:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`

Result:
- `CodeEditor` LOC reduced from `1286` to `665`.
- WS1 acceptance target `< ~900 LOC` is now met.

## WS2: Remove duplicate search scans

Status: `completed`

Implemented:
- `SearchController.replaceCurrent()` and `replaceAll()` do not invoke extra `executeSearch()` after model replacement.
- Search wiring centralized via `EditorSearchCoordinator`.
- `CodeEditorSearchFlowTest` asserts one search scan per replace action.

Files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorSearchCoordinator.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorSearchFlowTest.java`

## WS3: Viewport invalidation optimization

Status: `completed`

Implemented:
- Added `ViewportInvalidationPlanner` with bounded dirty ranges and visible-range intersection.
- `Viewport.onDocumentChanged(...)` now uses planner output with safe full-redraw fallback.

Files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/ViewportInvalidationPlanner.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/render/ViewportInvalidationPlannerTest.java`

## WS4: Document/line performance path improvements

Status: `completed`

Implemented:
- Added incremental index hooks on `EditCommand` and implementations (`InsertEdit`, `DeleteEdit`, `ReplaceEdit`, `CompoundEdit`).
- `Document.undo()`/`redo()` now apply incremental index updates with rebuild fallback.
- Added `Document.endsWithNewline()` and replaced full-text tail checks in line operations.
- `LineBlock.fromLines(...)` uses `Document.getSubstring(...)`.

Files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/EditCommand.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/InsertEdit.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/DeleteEdit.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/ReplaceEdit.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/CompoundEdit.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/Document.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/command/LineBlock.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/command/LineEditService.java`

## WS5: Consolidate repeated navigation/occurrence helpers

Status: `completed`

Implemented:
- Consolidated navigation and word-movement/delete logic into `EditorNavigationController`.
- Occurrence selection remains centralized via `OccurrenceSelectionService` and delegated through navigation controller.

Files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorNavigationController.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/OccurrenceSelectionService.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`

## Validation Executed

Compile:
- `./mvnw -pl papiflyfx-docking-code -am -DskipTests compile` -> `SUCCESS`

Targeted suites:
- `./mvnw -pl papiflyfx-docking-code -Dtest=CodeEditorIntegrationTest,CodeEditorSearchFlowTest,EditorInputControllerTest,EditorCommandExecutorTest,MouseGestureTest,LineOperationsTest -Dtestfx.headless=true -Dsurefire.failIfNoSpecifiedTests=false test` -> `SUCCESS`

Full module validation (headless):
- `./mvnw -pl papiflyfx-docking-code -am -Dtestfx.headless=true test` -> `SUCCESS` (`352` tests, `0` failures, `0` errors)

Benchmark validation:
- `./mvnw -pl papiflyfx-docking-code -am -Dtestfx.headless=true -Dgroups=benchmark -Dsurefire.excludedGroups= test` -> `SUCCESS`
- `CodeEditorBenchmarkTest`: `6` tests, `0` failures
- Snapshot metrics:
  - Large file open+render: `232ms`
  - Typing latency p95: `3.50ms`
  - Multi-caret typing latency p95: `8.68ms`
  - Scroll rendering p95: `0.14ms`
  - Multi-caret scroll rendering p95: `0.53ms`
  - Memory overhead: `55MB`

## Definition of Done Status

1. All five ranked debt items addressed with implementation changes: `met`
2. Headless test suite passes: `met`
3. Benchmark evidence captured: `met`
4. `CodeEditor`, `SearchController`, `Viewport` cohesion improved: `met`
5. Documentation updated for architecture/responsibilities and validation evidence: `met`
