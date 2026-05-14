# papiflyfx-docking-code quality remediation implementation plan

Scope: address all concerns documented in `spec/papiflyfx-docking-code-quality/README.md` with a safe, test-driven sequence that improves reliability, maintainability, testability, performance, and architecture without feature regression.

## 1. Objectives
- Fix the CRLF undo/redo correctness defect first.
- Decompose oversized classes (`CodeEditor`, `Viewport`) into cohesive collaborators.
- Remove repeated line-operation logic and centralize text-block operations.
- Reduce avoidable allocations and nested render/search loops.
- Improve isolated testability via dependency injection seams.
- Preserve current behavior and keep docking/session persistence compatible.

## 2. Constraints
- Module: `papiflyfx-docking-code`
- JavaFX behavior must remain functionally equivalent for existing tests.
- Keep persistence contract backward compatible (`CodeEditorStateAdapter` v2).
- No cross-module architectural coupling regressions.

## 3. Execution order
The order is mandatory to reduce risk:
1. Reliability fix (CRLF undo/redo)
2. Line-operation extraction
3. `CodeEditor` decomposition
4. `Viewport` decomposition
5. Performance optimizations
6. Testability/DI seams
7. Hardening + benchmark and docs refresh

## 4. Phase plan

## Phase 0: Safety baseline
Goal: establish guardrails before invasive refactors.

Tasks:
- Freeze baseline with full suite run:
  - `mvn -pl papiflyfx-docking-code -Dtestfx.headless=true test`
- Add characterization tests for current keyboard/mouse behavior at API level where coverage is weak.
- Add targeted benchmark snapshot run:
  - `CodeEditorBenchmarkTest`

Deliverables:
- Baseline test report committed in PR notes.
- Baseline benchmark numbers (p50/p95 for typing/scroll/search).

Exit criteria:
- Existing suite stays green.
- Benchmark baseline available for post-refactor comparison.

## Phase 1: Critical reliability fix (CRLF undo/redo)
Goal: eliminate data-loss/exception behavior in undo/redo after normalized line-ending edits.

Primary files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/Document.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/InsertEdit.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/ReplaceEdit.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/document/DocumentTest.java`

Implementation steps:
- Normalize text before creating edit commands in `Document.insert` and `Document.replace`.
- Ensure command objects store canonical text used in apply/undo.
- Validate undo/redo lengths against normalized text, never caller raw input length.
- Add regression tests for:
  - insert `"\r\n"` + undo/redo
  - replace with `"\r\n"` + undo/redo
  - compound edits containing normalized inserts/replaces

Exit criteria:
- New regression tests pass.
- No existing document tests regress.
- Repro case from quality review no longer fails.

## Phase 2: Consolidate repeated line-edit logic
Goal: remove duplicate line-block extraction/manipulation logic in `CodeEditor`.

Primary files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- new package/classes under `org.metalib.papifly.fx.code.command` (or `api.internal`):
  - `LineBlock`
  - `LineEditService`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/command/LineOperationsTest.java`

Implementation steps:
- Introduce `LineBlock` resolver that computes selected/current block once.
- Move `moveUp/moveDown/duplicateUp/duplicateDown/delete/join` to `LineEditService`.
- Remove dead code paths (e.g., unused temporary variables).
- Keep command-to-handler API stable in `CodeEditor` (delegation only).
- Expand tests for edge cases:
  - first/last line
  - no trailing newline
  - multi-line selection with mixed newline boundaries

Exit criteria:
- Line operations use single shared service path.
- No behavior change in existing action tests.
- Code duplication removed from `CodeEditor` line-operation handlers.

## Phase 3: Decompose `CodeEditor` responsibilities
Goal: break `CodeEditor` into cohesive collaborators while preserving external API.

Primary files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- new classes:
  - `EditorInputController` (mouse/key/scroll orchestration)
  - `EditorCommandExecutor` (command dispatch + edit actions)
  - `EditorStateCoordinator` (capture/apply caret/scroll/multi-caret)

Implementation steps:
- Keep `CodeEditor` as composition root and public facade.
- Move input event methods (`handleKey*`, `handleMouse*`, `handleScroll`) into `EditorInputController`.
- Move command switch + command handlers into `EditorCommandExecutor`.
- Move state capture/apply methods into `EditorStateCoordinator`.
- Preserve lifecycle/dispose orchestration in `CodeEditor`, but delegate component-specific cleanup.

Testing steps:
- Add focused unit tests for executor/controller classes without full JavaFX scene setup where possible.
- Keep existing integration tests unchanged; use them as regression net.

Exit criteria:
- `CodeEditor` reduced to orchestration/facade role.
- Existing public methods remain source-compatible.
- Full module suite remains green.

## Phase 4: Decompose `Viewport` render pipeline
Goal: reduce complexity and duplicate painting logic in `Viewport`.

Primary files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java`
- new classes under `render`:
  - `RenderContext`
  - `SelectionGeometry`
  - `RenderPass` implementations (`BackgroundPass`, `SearchPass`, `SelectionPass`, `TextPass`, `CaretPass`)

Implementation steps:
- Extract selection rectangle math into one reusable helper.
- Build unified paint pipeline reused by both full and incremental redraw.
- Keep dirty-line scheduling in `Viewport`; delegate drawing primitives to passes.
- Preserve blink and caret visibility behavior exactly.

Testing steps:
- Extend `ViewportTest` around full vs incremental redraw parity.
- Add tests for search/selection/caret overlay ordering.

Exit criteria:
- Duplicated methods removed (`drawSelectionRange*`, repeated search rendering paths).
- Render behavior remains pixel/logic equivalent for existing tests.
- No regressions in caret blink tests.

## Phase 5: Performance-focused cleanup
Goal: reduce avoidable allocations and expensive loops.

Primary files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchModel.java`

Implementation steps:
- Replace repeated per-token substring rendering with lower-allocation segment painting approach.
- Build line-indexed search match lookup for render (`line -> matches`).
- In `SearchModel`, reduce full-text duplication for case-insensitive search and cache compiled pattern per query/options snapshot.
- Ensure optimizations do not break regex/whole-word semantics.

Testing and validation:
- Extend search tests for correctness under caching.
- Run benchmark before/after and record delta.

Exit criteria:
- Benchmark p95 is not worse; expected improvement in search/render heavy scenarios.
- No regressions in `SearchModelTest`, `ViewportTest`, integration tests.

## Phase 6: Testability and constructor seams
Goal: enable isolated testing by reducing construction-time hard coupling.

Primary files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/*`

Implementation steps:
- Add package-private constructor that accepts collaborators/factories:
  - overlay controllers
  - lexer pipeline factory
  - command executor/input controller dependencies
- Keep current public constructor as default runtime path.
- Add tests that inject fakes for targeted behavior validation.

Exit criteria:
- New unit tests can exercise command/state logic without full UI assembly.
- Public API and production behavior unchanged.

## Phase 7: Hardening, cleanup, and documentation
Goal: finalize and make maintenance path explicit.

Tasks:
- Run full suite and benchmark regression checks.
- Remove obsolete code paths and comments from pre-refactor structure.
- Update docs with new class responsibilities and extension points.

Deliverables:
- Updated quality review references where issues are resolved.
- This plan marked complete with checklist status.

Exit criteria:
- All phases completed.
- All tests green in headless mode.
- Measured performance equal or better than Phase 0 baseline.

## 5. PR slicing plan
Recommended PR breakdown (keep each independently releasable):
1. PR-1: Phase 1 only (CRLF correctness + tests)
2. PR-2: Phase 2 only (line-edit service extraction)
3. PR-3: Phase 3 (`CodeEditor` decomposition)
4. PR-4: Phase 4 (`Viewport` decomposition)
5. PR-5: Phase 5 performance updates
6. PR-6: Phase 6 DI/testability seams
7. PR-7: Phase 7 cleanup/docs and final benchmark report

## 6. Validation commands
- Compile:
  - `mvn compile -pl papiflyfx-docking-code -am`
- Full module tests:
  - `mvn test -pl papiflyfx-docking-code`
- Headless UI tests:
  - `mvn -Dtestfx.headless=true test -pl papiflyfx-docking-code`
- Targeted suites during refactor:
  - `mvn -pl papiflyfx-docking-code -Dtest=DocumentTest test`
  - `mvn -pl papiflyfx-docking-code -Dtest=LineOperationsTest,CodeEditorIntegrationTest test`
  - `mvn -pl papiflyfx-docking-code -Dtest=ViewportTest,SearchModelTest test`

## 7. Definition of done
- Critical defect fixed and covered by regression tests.
- `CodeEditor` and `Viewport` responsibilities are decomposed and documented.
- Duplicate line-edit logic removed and centralized.
- Performance hotspots addressed with measured evidence.
- Dependency seams exist for isolated unit testing.
- Full module suite passes; behavior remains compatible with existing docking/persistence flows.
