# Review 6 (Codex-0): Phase 6 Implementation Plan

Date: 2026-02-17  
Target: `Phase 6: Persistence and Restore Contract` from `spec/papiflyfx-docking-code/implementation.md`

## 1. Phase 6 Goal

Deliver a stable and explicit persistence contract for code-editor leaves so docking session capture/restore is deterministic across versions:

- `EditorStateData` v1 is the canonical payload.
- `LeafContentData.state` map round-trips without ambiguity.
- `CodeEditorStateAdapter` owns version-aware save/restore.
- Restore flow remains compatible with docking fallback order: `adapter -> factory -> placeholder`.

## 2. Current Baseline (Already Implemented)

- State DTO exists: `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/state/EditorStateData.java`.
- Map codec exists: `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/state/EditorStateCodec.java`.
- Adapter v1 exists with save/restore + v0 hook: `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorStateAdapter.java`.
- Rehydrate-from-file behavior exists for readable paths and missing-file fallback.
- Layout restore precedence already fixed in `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/LayoutFactory.java`.
- Baseline tests exist for codec and adapter behavior:
  - `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/state/EditorStateCodecTest.java`
  - `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorIntegrationTest.java`
  - `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/layout/LayoutFactoryFxTest.java`

## 3. Remaining Gaps To Close Phase 6

1. Migration is only a minimal branch (`v0` same-codec path); explicit migration scaffolding for future schema evolution is not yet structured.
2. Restore/save error containment is incomplete:
- `LayoutFactory.buildLeaf(...)` does not guard `adapter.restore(...)` exceptions.
- `DockManager.refreshContentState(...)` does not guard `adapter.saveState(...)` exceptions.
3. No docking-level end-to-end test currently proves session round-trip for `cursorLine`, `cursorColumn`, `verticalScrollOffset`, `languageId` via real `DockManager` capture/restore.
4. File-path restore should also handle invalid path syntax defensively (for malformed serialized payloads), not only unreadable/missing files.

## 4. Detailed Implementation Plan

## Workstream A: Formalize v1 State Contract

Files:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/state/EditorStateData.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/state/EditorStateCodec.java`

Tasks:
1. Freeze and document v1 invariants in Javadoc:
- `filePath`: nullable input normalized to `""`.
- `cursorLine`, `cursorColumn`: `>= 0`.
- `verticalScrollOffset`: `>= 0.0`.
- `languageId`: blank/null normalized to `"plain-text"`.
- `foldedLines`: non-null immutable list (MVP may be empty).
2. Keep codec key set explicit and unchanged for v1 compatibility:
- `filePath`, `cursorLine`, `cursorColumn`, `verticalScrollOffset`, `languageId`, `foldedLines`.
3. Add dedicated codec tests for forward/backward tolerance:
- unknown keys ignored,
- missing keys defaulted,
- malformed field types safely fallback.

Deliverable:
- Contract is explicit and stable for serialized sessions.

## Workstream B: Adapter Versioning and Migration Hooks

File:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorStateAdapter.java`

Tasks:
1. Refactor `restoreState(...)` into version-gated decoding helpers:
- `decodeV1(Map<String, Object>)`
- `migrateV0ToV1(Map<String, Object>)`
- `fallbackEmptyState(...)` for unknown versions.
2. Keep current `VERSION = 1`, but make migration structure additive for `v2+` introduction without branching chaos.
3. Add defensive path parsing around `Path.of(filePath)`:
- catch malformed-path runtime failures and fallback to empty document with metadata.
4. Preserve apply order for correctness:
- decode state,
- rehydrate text,
- apply editor metadata/caret/scroll.

Deliverable:
- Migration-ready adapter contract with deterministic fallback behavior.

## Workstream C: Restore-Order Compatibility Hardening

Files:
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/LayoutFactory.java`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java`

Tasks:
1. Guard `adapter.restore(...)` in `LayoutFactory.buildLeaf(...)`:
- if adapter throws, continue to factory fallback,
- if factory unavailable/fails, create placeholder.
2. Guard `adapter.saveState(...)` in `DockManager.refreshContentState(...)`:
- if adapter save fails, do not abort full session capture,
- keep previous `contentData` or write minimal safe payload.
3. Add focused logging for both paths to aid diagnosis without breaking restore.

Deliverable:
- Corrupt state or adapter failure cannot break whole docking session capture/restore.

## Workstream D: Test Matrix Expansion (Phase 6 Exit Evidence)

Test files to extend/add:
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/state/EditorStateCodecTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorIntegrationTest.java`
- `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/layout/LayoutFactoryFxTest.java`
- `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/DockManagerSessionFxTest.java` (or a dedicated new persistence FX test)

Required scenarios:
1. Codec round-trip keeps all v1 fields.
2. Adapter restore supports `version=1`, migrates `version=0`, and safely falls back on unknown versions.
3. Invalid file path and missing/unreadable file both restore empty text while preserving metadata fields.
4. Restore precedence:
- adapter success wins,
- adapter null/exception falls to factory,
- missing adapter/factory falls to placeholder.
5. DockManager end-to-end session round-trip preserves:
- `cursorLine`,
- `cursorColumn`,
- `verticalScrollOffset`,
- `languageId`.

Deliverable:
- Phase 6 acceptance criteria validated by tests, not assumptions.

## Workstream E: Documentation and Progress Updates

Files:
- `spec/papiflyfx-docking-code/PROGRESS.md`
- Optional API docs in adapter/codec classes.

Tasks:
1. Mark Phase 6 as complete only when test evidence exists for round-trip + migration hook behavior.
2. Record final state schema contract and fallback semantics for future contributors.

## 5. Execution Sequence

1. Lock v1 schema docs and codec behavior.
2. Refactor adapter migration flow and malformed-path handling.
3. Add save/restore exception containment in docking integration points.
4. Add/expand tests in code module and docks module.
5. Run full validation and update progress docs.

## 6. Exit Criteria Mapping

| Phase 6 Exit Criterion | Concrete Evidence |
| --- | --- |
| Round-trip preserves cursor/scroll/language | DockManager integration FX test with save->restore->assert state |
| Adapter versioning + migration hooks in place | Dedicated adapter tests for `v1`, `v0`, unknown version fallback |
| Restore order compatibility enforced | LayoutFactory tests for adapter/factory/placeholder precedence including adapter failure |
| Stable map contract via `LeafContentData.state` | Codec tests covering full key set and malformed payload tolerance |

## 7. Validation Commands

- `mvn -pl papiflyfx-docking-code -am test`
- `mvn -pl papiflyfx-docking-docks -am test`
- `mvn -pl papiflyfx-docking-code,papiflyfx-docking-docks -am -Dtestfx.headless=true test`

## 8. Risks and Mitigations

1. Risk: adapter exceptions break session restore.
- Mitigation: guarded adapter calls + fallback chain + tests.
2. Risk: malformed persisted paths crash restore.
- Mitigation: safe path parsing and empty-document fallback.
3. Risk: future schema changes silently break old sessions.
- Mitigation: explicit migration methods per version and regression tests per version boundary.
