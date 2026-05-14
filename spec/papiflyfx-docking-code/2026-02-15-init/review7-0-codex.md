# Review 7 (Codex-0): Phase 7 Implementation Plan

Date: 2026-02-17  
Target: `Phase 7: Failure Handling and Lifecycle Cleanup` from `spec/papiflyfx-docking-code/implementation.md`

## 1. Phase 7 Goal

Deliver resilient failure behavior and deterministic cleanup so editor content does not destabilize docking restore flows:

- Unknown language IDs must safely fall back to plain-text lexing.
- Lexer runtime failures must degrade to a safe token state with diagnostics.
- Missing/unreadable file paths must restore to empty content while preserving metadata.
- Editor lifecycle disposal must stop background work and detach listeners when leaves close.

## 2. Current Baseline (Already Implemented)

- Unknown language fallback already exists in lexer resolution:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/LexerRegistry.java`
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/IncrementalLexerPipeline.java`
- Baseline unknown-language test already exists:
  - `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/lexer/IncrementalLexerPipelineTest.java` (`unknownLanguageFallsBackToPlainText`)
- Missing/unreadable/invalid-path restore fallback is implemented:
  - `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorStateAdapter.java`
- Restore failure containment in docking flow is already present from Phase 6:
  - `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/LayoutFactory.java` (guard adapter restore)
  - `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java` (guard adapter saveState)
- Disposal chain is wired:
  - `CodeEditor` implements `DisposableContent`
  - `DockLeaf.dispose()` invokes `DisposableContent.dispose()`
  - `CodeEditor.dispose()` stops lexer pipeline and viewport resources

## 3. Remaining Gaps To Close Phase 7

1. Lexer exception fallback keeps prior token map instead of producing a safe current snapshot.
   - Current behavior logs warning and retains old tokens, which can leave stale highlighting when document text changed.
2. No explicit proof that closing a leaf leaves no active lexer processing.
   - Existing disposal tests verify handler cleanup, but do not assert background-worker quiescence after close.
3. Cleanup can be strengthened for full lifecycle isolation.
   - `CodeEditor.dispose()` should proactively clear search-controller callbacks/document references to reduce retention risk.
4. Failure-path end-to-end evidence is still thin at docking session level.
   - Need a restore-flow test that confirms malformed content state does not break overall session rebuild.

## 4. Detailed Implementation Plan

## Workstream A: Safe Lexer Failure Fallback

File:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/IncrementalLexerPipeline.java`

Tasks:
1. Replace "keep previous tokens" failure branch with deterministic fallback tokens for current text snapshot:
   - Re-lex with `PlainTextLexer` for the same revision request.
   - Apply fallback map only when revision is still current.
2. Improve log context:
   - Include language id, revision, and dirty start line in warning logs.
3. Keep cancellation behavior unchanged (cancellation remains non-error path).

Deliverable:
- Lexer exceptions never leave stale token maps for changed text; editor degrades to plain-text coloring safely.

## Workstream B: Lifecycle Cleanup Hardening

Files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java` (if helper API is needed)

Tasks:
1. In `CodeEditor.dispose()` clear search-related references/callbacks:
   - clear `onNavigate`, `onClose`, `onSearchChanged`,
   - detach controller document reference and close overlay state.
2. Keep disposal idempotent and side-effect safe for repeated calls.
3. Ensure no post-dispose UI mutation callbacks are triggered from disposed components.

Deliverable:
- Editor disposal leaves no active listeners/callback references outside owned object graph.

## Workstream C: Failure Path and Worker-Stop Tests

Tests to add/extend:
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/lexer/IncrementalLexerPipelineTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorIntegrationTest.java`
- `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/DockManagerSessionFxTest.java` (or dedicated new failure-flow test)

Required scenarios:
1. Lexer throws -> pipeline applies plain-text fallback for current text (not stale prior token map).
2. After dispose/leaf close, subsequent document edits do not produce token-map application callbacks.
3. Session restore with malformed/failed content restore still builds layout and remains capturable.
4. Missing/unreadable file restore remains empty-text + metadata-preserving (regression guard).

Deliverable:
- Phase 7 exit criteria are proven with automated tests.

## Workstream D: Progress and Contract Notes

Files:
- `spec/papiflyfx-docking-code/PROGRESS.md`

Tasks:
1. Record exact fallback semantics:
   - unknown language -> plain text,
   - lexer exception -> plain-text fallback tokenization of current snapshot.
2. Mark Phase 7 complete only after worker-stop and restore-failure tests pass.

## 5. Execution Sequence

1. Implement safe lexer-exception fallback in pipeline.
2. Strengthen editor dispose cleanup for search-controller references.
3. Add regression tests for fallback correctness and post-dispose quiescence.
4. Add docking-level failure-path restore test.
5. Run full validation and update progress documentation.

## 6. Exit Criteria Mapping

| Phase 7 Exit Criterion | Concrete Evidence |
| --- | --- |
| Closing leaves does not leave active background tasks | Dispose/close tests proving no token updates after close and no worker processing continues |
| Failure paths do not break docking session restore | Docking restore tests with malformed/failed content restore still producing valid captured/restored session |

## 7. Validation Commands

- `mvn -pl papiflyfx-docking-code -am test`
- `mvn -pl papiflyfx-docking-docks -am -Dtestfx.headless=true test`
- `mvn -pl papiflyfx-docking-code,papiflyfx-docking-docks -am -Dtestfx.headless=true test`

## 8. Risks and Mitigations

1. Risk: lexer fallback path introduces extra re-lex overhead during failure bursts.  
   Mitigation: only invoke fallback on exception path; keep debounce and revision checks unchanged.
2. Risk: disposal tests become timing-sensitive due to async worker behavior.  
   Mitigation: use deterministic latches/timeouts and assert no callback activity after dispose window.
3. Risk: restore-failure tests become brittle if placeholder text changes.  
   Mitigation: assert structural invariants (leaf exists, content non-null, session capture succeeds) rather than exact label strings.
