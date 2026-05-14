# Review 5 (Codex) - papiflyfx-docking-code

Date: 2026-02-17  
Scope: `spec/papiflyfx-docking-code/spec.md` + `implementation.md` versus current implementation in `papiflyfx-docking-code` and integration points in `papiflyfx-docking-docks`.

Validation run:
- `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true test`
- Result: PASS (`182` tests), but the issues below are still present and mostly untested.

## Findings

### 1. HIGH - Typed character input does not advance caret (can reverse typed text)
Spec linkage:
- `spec.md` 2.1 (single-caret editing correctness)

Evidence:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:323`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:579`

What is wrong:
- `handleKeyTyped()` inserts text, then calls `moveCaretRight(ch.length(), false)`.
- `moveCaretRight(...)` ignores `chars` and re-derives line/column from the unchanged caret position.
- Result: caret may stay before inserted text, so repeated typing can insert at the same offset.

Potential fix:
- Replace `moveCaretRight(...)` with offset-based movement:
  - `int newOffset = selectionModel.getCaretOffset(document) + chars;`
  - `moveCaretToOffset(newOffset);`
- Add an integration test that types `abc` and asserts document text is `abc` (not reversed) and caret column is `3`.

### 2. HIGH - Editor disposal contract is not enforced on leaf close
Spec linkage:
- `spec.md` 5.2 (must stop workers/unbind/release on leaf close)

Evidence:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:755`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockLeaf.java:149`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:346`

What is wrong:
- `CodeEditor` has explicit `dispose()` (stops lexer worker, unbinds listeners).
- `DockManager` closes leaves via `DockLeaf.dispose()`, but `DockLeaf.dispose()` only nulls content and never calls content-specific disposal.
- `CodeEditor.dispose()` is therefore not guaranteed during normal docking lifecycle.

Potential fix:
- In `DockLeaf.dispose()`, dispose the content node if it supports lifecycle (for example `AutoCloseable` or a dedicated docking `DisposableContent` interface).
- Make `CodeEditor` implement that interface (or `AutoCloseable`) and delegate to `dispose()`.
- Add a docking integration test that closes a leaf with `CodeEditor` content and verifies lexer worker is shut down.

### 3. HIGH - Restore fallback order does not match spec (adapter missing blocks factory fallback)
Spec linkage:
- `spec.md` 4.2 (restore order must be Adapter -> Factory -> Placeholder)

Evidence:
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/LayoutFactory.java:93`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/LayoutFactory.java:98`
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/LayoutFactory.java:103`

What is wrong:
- If `contentData` exists but registry has no adapter for `typeKey`, code creates placeholder immediately.
- Because `content` is no longer `null`, `ContentFactory.create(...)` fallback is skipped.

Potential fix:
- Only create placeholder after both adapter restore and factory creation attempts fail.
- Also handle the case `contentData == null` and `contentFactoryId != null` by showing placeholder if factory is unavailable.

### 4. MEDIUM - Restored state does not rehydrate document content; cursor/scroll persistence is fragile
Spec linkage:
- `spec.md` 4.3, 6, 8 (state round-trip should preserve cursor/scroll/language/folded lines)
- `spec.md` 6 (missing/unreadable file fallback behavior)

Evidence:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorStateAdapter.java:38`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:673`
- No file-loading path exists in `papiflyfx-docking-code/src/main/java` (only `filePath` metadata is persisted).

What is wrong:
- Restore applies metadata/caret/scroll to a new empty editor, but does not load file content or persisted buffer.
- Caret and scroll can clamp to empty-document bounds during restore, so round-trip guarantees are not reliable unless host code separately rehydrates text.

Potential fix:
- Define one of these explicit restore contracts:
  - Adapter loads `filePath` content (background I/O), then applies caret/scroll after text load.
  - Or persist document text (or unsaved buffer snapshot) in state for full editor-only round-trip.
- Implement missing/unreadable file fallback to empty document while preserving metadata as spec states.

### 5. MEDIUM - Per-edit O(n) work risks violating large-file latency targets
Spec linkage:
- `spec.md` 3.1, 3.3, 8 (large-file typing/scroll performance targets)

Evidence:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/Document.java:239` (full `LineIndex` rebuild after every edit)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/IncrementalLexerPipeline.java:138` (full text snapshot on each change)

What is wrong:
- Every edit rebuilds line index from full text and snapshots full document text for lexing.
- This is correct functionally, but high risk for `p95 <= 16ms` typing target on 100k+ lines.

Potential fix:
- Move to incremental line-index updates keyed by `DocumentChangeEvent`.
- Keep lexing incremental but reduce full-text copy frequency (or benchmark and document measured limits).
- Add a perf guard benchmark for rapid single-char edits on large documents.

### 6. MEDIUM - Dirty-region incremental redraw is not implemented
Spec linkage:
- `spec.md` 3.2 (track dirty lines/regions and redraw incrementally)

Evidence:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java:189`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java:269`

What is wrong:
- Rendering uses a single boolean `dirty` and redraws full visible canvas/layers each time.
- There is no dirty-line/dirty-region tracking despite explicit spec requirement.

Potential fix:
- Track dirty line range from document/caret/search changes.
- Redraw only affected line rectangles when possible, keeping full redraw as fallback.

## Test Gaps To Close

1. No test currently verifies key-typed caret progression and typed-text ordering.
2. No docking test verifies that closing a leaf disposes content-specific resources (`CodeEditor.dispose()`).
3. No integration test validates exact restore precedence `adapter -> factory -> placeholder` when adapter is absent.

## Suggested Priority

1. Fix issue 1 (typing correctness).
2. Fix issue 2 and 3 (lifecycle and restore-contract correctness).
3. Address issue 4 (state rehydration contract clarity/implementation).
4. Add perf and redraw work from issues 5 and 6 with benchmark evidence.
