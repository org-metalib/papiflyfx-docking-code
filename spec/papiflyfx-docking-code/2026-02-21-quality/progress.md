# papiflyfx-docking-code quality remediation progress

Date: 2026-02-22
Scope reference: `spec/papiflyfx-docking-code-quality/implementation.md`

## Overall status
- Phase 0: `completed`
- Phase 1: `completed`
- Phase 2: `completed`
- Phase 3: `completed`
- Phase 4: `completed`
- Phase 5: `completed`
- Phase 6: `completed`
- Phase 7: `completed`

## Completed work

### Phase 1: CRLF undo/redo correctness (`completed`)
- Fixed canonical text usage in edit command creation:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/Document.java`
  - `insert(...)` now creates `InsertEdit` with normalized text.
  - `replace(...)` now creates `ReplaceEdit` with normalized replacement.
- Added regression coverage for CRLF normalization + undo/redo:
  - `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/document/DocumentTest.java`
  - New tests:
    - `insertCrLfUndoRedoUsesNormalizedLength`
    - `replaceCrLfUndoRedoUsesNormalizedLength`
    - `compoundEditsWithCrLfUndoRedoRemainConsistent`

### Phase 2: line-edit logic consolidation (`completed`)
- Added reusable line block model:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/command/LineBlock.java`
- Added centralized line edit service:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/command/LineEditService.java`
  - Covers delete/move/duplicate/join line operations.
- Replaced duplicated line-operation logic in editor handlers with delegation:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- Reworked line operation tests to validate shared service behavior:
  - `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/command/LineOperationsTest.java`

### Phase 6: testability seams (`completed`)
- Added package-private constructor with injected collaborators/factories:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
  - Supports injected `SearchModel`, `SearchController`, `GoToLineController`, lexer pipeline factory, and `LineEditService`.
- Added integration test validating injected factory path:
  - `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorIntegrationTest.java`
  - Test: `packagePrivateConstructorUsesInjectedFactories`
- Added non-UI unit tests for extracted command/input collaborators:
  - `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/EditorCommandExecutorTest.java`
  - `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/EditorInputControllerTest.java`

### Phase 3: `CodeEditor` decomposition (`completed`)
- Added dedicated pointer orchestration collaborator:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorPointerController.java`
  - Owns mouse press/drag/release handling, box-selection lifecycle, and wheel scroll behavior.
- Added dedicated edit orchestration collaborator:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorEditController.java`
  - Owns typed insertion, backspace/delete/enter, copy/cut/paste, and multi-caret compound edit execution.
- `CodeEditor` now delegates mouse/scroll/edit handling to collaborators and remains composition-root/facade focused:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- Added isolated unit tests for extracted edit orchestration:
  - `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/EditorEditControllerTest.java`

### Phase 4: viewport decomposition (`completed`)
- Added explicit render frame context and geometry helpers:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/RenderContext.java`
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/SelectionGeometry.java`
- Added render pass contract and concrete passes:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/RenderPass.java`
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/BackgroundPass.java`
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/SearchPass.java`
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/SelectionPass.java`
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/TextPass.java`
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/CaretPass.java`
- Refactored `Viewport.redraw()` to execute a shared pass pipeline for both full redraw and incremental dirty-line redraw:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java`
- Removed duplicate in-class paint implementations (selection/search/caret/text draw paths) from `Viewport`.

### Phase 5: performance cleanup (`completed`)
- Added search highlight line index in viewport to remove nested `matches x lines` scanning:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java`
- Optimized caret rendering lookups in full redraw path (removed nested caret/line scan).
- Added regex pattern cache in search model and reduced case-insensitive plain-text allocations:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchModel.java`
- Reworked text paint path to draw base line once and overlay only styled token runs (reduces per-line segment churn):
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/TextPass.java`
- Removed per-render token-list copy from `RenderLine`:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/RenderLine.java`
- Added cache invalidation regression coverage:
  - `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/search/SearchModelTest.java`
  - New tests:
    - `regexSearchCacheInvalidatesWhenCaseSensitivityChanges`
    - `regexSearchCacheInvalidatesWhenWholeWordChanges`
    - `regexSearchCacheRecoversAfterInvalidPattern`

### Phase 7: hardening and docs refresh (`completed`)
- Re-ran full headless suite and benchmark suite after Phase 5 changes.
- Updated quality review with explicit resolution status and before/after benchmark snapshot:
  - `spec/papiflyfx-docking-code-quality/README.md`
- Finalized implementation status tracking:
  - `spec/papiflyfx-docking-code-quality/progress.md`

### Phase 0: safety baseline (`completed`)
- Full module headless suite re-run after refactors.
- Benchmark baseline capture executed from `CodeEditorBenchmarkTest` with benchmark tag exclusion overridden.
- Baseline snapshot:
  - Large file open+render: `406ms`
  - Typing latency p95: `3.38ms`
  - Multi-caret typing latency p95: `10.99ms`
  - Scroll rendering p95: `0.18ms`
  - Multi-caret scroll rendering p95: `0.55ms`
  - Memory overhead: `59MB`

## Validation
- Targeted regression run:
  - `mvn -pl papiflyfx-docking-code -Dtest=SearchModelTest,ViewportTest,CodeEditorIntegrationTest -Dtestfx.headless=true test`
  - Result: success
- Full module headless suite:
  - `mvn -pl papiflyfx-docking-code -Dtestfx.headless=true test`
  - Result: success
  - Tests run: `344`, failures: `0`, errors: `0`
- Benchmark run (with benchmark exclusion overridden):
  - `mvn -pl papiflyfx-docking-code -Dtest=CodeEditorBenchmarkTest -Dsurefire.excludedGroups= -Dtestfx.headless=true test`
  - Result: success
  - Post-phase-5 snapshot:
    - Large file open+render: `217ms`
    - Typing latency p95: `3.06ms`
    - Multi-caret typing latency p95: `10.59ms`
    - Scroll rendering p95: `0.20ms`
    - Multi-caret scroll rendering p95: `0.50ms`
    - Memory overhead: `55MB`
