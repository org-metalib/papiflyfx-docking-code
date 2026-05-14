# PapiflyFX Code - Progress Report

**Date:** 2026-02-17
**Status:** Phase 8 complete — all MVP phases delivered

## Summary
- Specification and implementation plan were updated to target a separate module: `papiflyfx-docking-code`.
- New Maven module scaffold was created and wired into the root aggregator.
- Phase 0 starter classes were implemented:
  - `CodeEditor` (placeholder editor node with state capture/apply)
  - `CodeEditorFactory` (`ContentFactory` for `factoryId = "code-editor"`)
  - `CodeEditorStateAdapter` (`ContentStateAdapter`, version `1`)
  - `EditorStateData` and `EditorStateCodec`
- Phase 1 document core was implemented:
  - `TextSource`, `LineIndex`, `Document`
  - edit command primitives (`InsertEdit`, `DeleteEdit`, `ReplaceEdit`)
  - undo/redo and line/column mapping behavior
- Unit tests now cover state codec and document core logic.
- Follow-up hardening fixes were implemented for review findings:
  - restored caret state now applies to runtime `SelectionModel`,
  - undo/redo no longer resets caret to `0:0`,
  - persisted scroll offset is synchronized with actual viewport clamp,
  - version-aware state restore path added to `CodeEditorStateAdapter`,
  - disposal hooks added to `CodeEditor` and `Viewport`.
- Phase 3 incremental lexer pipeline was implemented:
  - token model (`Token`, `TokenType`, `LexState`, `TokenMap`) and per-line cache,
  - language lexers for Java, JSON, JavaScript, and plain-text fallback,
  - incremental re-lex engine with line-entry/exit state propagation,
  - debounced async lexer pipeline with revision-safe FX-thread token apply,
  - viewport token-aware rendering and editor language wiring.
- Phase 4 gutter, markers, and navigation was implemented:
  - line number gutter (`GutterView`) with dynamic width and active-line highlighting,
  - marker lane with `MarkerModel` integration (error, warning, info, breakpoint, bookmark),
  - search/replace model (`SearchModel`) supporting plain text and regex modes,
  - search/replace UI overlay (`SearchController`) with navigation and mode toggles,
  - go-to-line action via `Ctrl/Cmd+G` dialog and programmatic `goToLine(int)`,
  - search highlight rendering in `Viewport` with current-match distinction,
  - gutter scroll synchronization with viewport,
  - keyboard shortcuts: `Ctrl/Cmd+F` (search), `Ctrl/Cmd+G` (go-to-line), `Escape` (close search).
- Phase 7 failure handling and lifecycle cleanup was completed:
  - lexer exception path now falls back to plain-text tokenization of the current snapshot (not stale previous tokens),
  - lexer warning logs now include languageId/revision/dirtyStartLine context,
  - `CodeEditor.dispose()` now clears search callbacks/document reference and closes search overlay safely,
  - added tests for lexer failure fallback, post-dispose lexer quiescence, dispose callback cleanup, and docking session restore/capture under adapter restore/save failures.

## Update Log
- **2026-02-17:** Completed Phase 8 — Hardening, Benchmarks, and Docs (Review 8 Claude-0):
  - **Workstream A** — Performance benchmark harness: added `CodeEditorBenchmarkTest` with 4 tests measuring spec §8 acceptance criteria on 100k-line synthetic Java file. All thresholds met: open+render 218ms (≤2000ms), typing p95 3.27ms (≤16ms), scroll p95 0.18ms (≤16ms), memory 63MB (≤350MB). Benchmarks tagged `@Tag("benchmark")` and excluded from default test runs via `surefire.excludedGroups` property in pom.xml.
  - **Workstream B** — End-to-end docking integration tests: added `CodeEditorDockingIntegrationTest` with 4 scenarios: full state round-trip through DockManager (filePath/language/cursor/scroll preserved via temp file), missing adapter falls back to factory, missing factory falls back to placeholder, multi-leaf session preserves independent state.
  - **Workstream C** — Documentation: rewrote module `README.md` with Maven dependency, quickstart guide, factory/adapter registration examples, session persistence flow, API highlights table, supported languages, and acceptance metrics table. Added `META-INF/services/org.metalib.papifly.fx.docks.layout.ContentStateAdapter` ServiceLoader descriptor for auto-discovery.
  - **Workstream D** — Updated PROGRESS.md with Phase 8 completion, benchmark results, and final test counts.
  - Validation:
    - `mvn -pl papiflyfx-docking-code,papiflyfx-docking-docks -am -Dtestfx.headless=true test` -> ✅ pass (208 tests, 0 failures, 0 errors)
    - `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true -Dgroups=benchmark -Dsurefire.excludedGroups= test` -> ✅ pass (4 benchmarks, all within spec thresholds)
- **2026-02-17:** Applied Review 7-1 (Claude-0) minor fixes:
  - **Finding 1** (Workstream A) — Fallback re-lex in `IncrementalLexerPipeline` now uses `TokenMap.empty()` and `dirtyStartLine=0` instead of passing the previous `baseline`. Semantically precise: plain-text fallback always produces a complete fresh token map from line 0.
  - **Finding 2** (Workstream B) — `markerModel` change listener (`gutterView::markDirty`) is now stored as a named field and explicitly removed in `CodeEditor.dispose()`, closing the retention-neutral listener leak.
  - Validation: `mvn -pl papiflyfx-docking-code,papiflyfx-docking-docks -am -Dtestfx.headless=true test` -> ✅ pass (204 tests, 0 failures, 0 errors)
- **2026-02-17:** Completed Phase 7 — Failure Handling and Lifecycle Cleanup (Review 7 Codex-0 workstreams 1+2):
  - **Workstream A** — Lexer failure fallback hardened in `IncrementalLexerPipeline`: exception path now re-lexes with plain-text fallback for the same pending request and revision. Previous behavior ("keep old tokens") that could leave stale highlighting is removed.
  - **Workstream B** — Lifecycle cleanup hardened in `CodeEditor.dispose()`: search controller callbacks are nulled, document is detached, and overlay is closed before disposing rendering/lexer resources.
  - **Workstream C** — Test matrix expanded:
    - `IncrementalLexerPipelineTest`: fallback-on-throw now asserts plain-text tokens for current text snapshot; added post-dispose no-update test.
    - `CodeEditorIntegrationTest`: added dispose/search callback cleanup regression test.
    - `DockManagerSessionFxTest`: added restore failure path test where adapter restore/save both throw; verifies restore fallback + session capture still succeeds.
  - Validation:
    - `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true -Dtest=IncrementalLexerPipelineTest,CodeEditorIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` -> ✅ pass (33 tests, 0 failures)
    - `mvn -pl papiflyfx-docking-docks -am -Dtestfx.headless=true -Dtest=DockManagerSessionFxTest -Dsurefire.failIfNoSpecifiedTests=false test` -> ✅ pass (5 tests, 0 failures)
- **2026-02-17:** Completed Phase 6 — Persistence and Restore Contract (Review 6 Codex-0):
  - **Workstream A** — Formalized v1 state contract: added comprehensive Javadoc to `EditorStateData` documenting all field invariants, canonical key set, and forward/backward compatibility semantics. Added 3 codec tolerance tests (unknown keys ignored, full v1 key set verification, all-fields round-trip).
  - **Workstream B** — Refactored adapter versioning: extracted `decodeV1()`, `migrateV0ToV1()`, and `fallbackEmptyState()` version-gated helpers in `CodeEditorStateAdapter`. Added `InvalidPathException` catch for malformed path syntax with empty-document fallback. Migration structure is now additive for future v2+ introduction.
  - **Workstream C** — Hardened restore-order compatibility: guarded `adapter.restore()` in `LayoutFactory.buildLeaf()` with try-catch so adapter failures fall through to factory/placeholder chain. Guarded `adapter.saveState()` in `DockManager.refreshContentState()` so save failures do not abort session capture. Added focused logging for both error paths.
  - **Workstream D** — Expanded test matrix: added 3 codec tests, 2 adapter tests (invalid path, saveState field verification), 2 LayoutFactory tests (adapter-throws-falls-to-factory, adapter-throws-falls-to-placeholder). Test suite now 202 tests passing (up from 197).
  - **Workstream E** — Updated progress docs and phase status.
- **2026-02-17:** Applied Review 5 Codex-1 follow-up fixes (all remaining issues):
  - **HIGH** — Resolved per-edit O(n) work (previously deferred issue 5): `LineIndex` now has `applyInsert()`/`applyDelete()` for incremental line-start updates. `Document.insert/delete/replace` use incremental updates instead of full rebuild. `IncrementalLexerPipeline` defers `getText()` snapshot to worker thread via lazy enqueue, avoiding O(n) string copy on every keystroke. Performance guard test validates <1ms per-edit on 50k-line document.
  - **HIGH** — Implemented dirty-region incremental redraw (previously deferred issue 6): `Viewport` now tracks dirty lines via `BitSet` and `fullRedrawRequired` flag. Document changes dirty only affected lines from change offset onward. Caret moves dirty only old/new caret lines. Full redraw occurs only on scroll, resize, or theme change.
  - **MEDIUM** — Fixed placeholder fallback when `contentData == null` and factory unavailable (issue 3 edge case): `LayoutFactory.buildLeaf()` now always creates a placeholder as final fallback using leaf id/title.
  - Closed all test gaps: DockManager-level `DisposableContent.dispose()` test, placeholder-precedence tests (contentData-present and contentData-null), `LineIndex` incremental update tests (6), `Document` incremental correctness + perf guard tests (5).
  - Test suite now 197 tests passing (up from 186), 0 failures, 0 errors.
- **2026-02-17:** Applied Review 5 (Codex) fixes:
  - **HIGH** — Fixed typing caret advancement: `handleKeyTyped` now computes new offset after insert and calls `moveCaretToOffset()` instead of broken `moveCaretRight()`. Removed unused `moveCaretRight` method.
  - **HIGH** — Fixed DockLeaf content disposal: added `DisposableContent` interface in `papiflyfx-docking-docks`, `DockLeaf.dispose()` now calls `DisposableContent.dispose()` on content nodes. `CodeEditor` implements `DisposableContent`.
  - **HIGH** — Fixed restore fallback order in `LayoutFactory.buildLeaf()`: when adapter is absent for a typeKey, factory creation is attempted before placeholder fallback (previously a placeholder was created immediately, blocking factory).
  - **MEDIUM** — Fixed state rehydration: `CodeEditorStateAdapter.restore()` now loads file content from `filePath` when readable; falls back to empty document with metadata preserved for missing/unreadable files (spec §6).
  - Issues 5 (per-edit O(n)) and 6 (dirty-region redraw) deferred to Phase 8 benchmarks.
  - Added 4 regression tests in `CodeEditorIntegrationTest` (typing caret, DockLeaf dispose, file rehydration, missing file fallback).
  - Added 3 tests in `DockLeafTest` (dispose with DisposableContent, non-disposable, null).
  - Added 1 test in `LayoutFactoryFxTest` (adapter-missing factory fallback order).
  - Test suite now 186 code-module + 41 docks-module tests passing.
- **2026-02-17:** Completed Phase 5 theme composition and mapping:
  - Added `CodeEditorTheme` record with 30 palette fields (spec core 10 + syntax/gutter/search/overlay colors).
  - Added `CodeEditorThemeMapper` that maps docking `Theme` to `CodeEditorTheme` via composition (dark/light detection from background brightness).
  - Refactored `Viewport` to render all colors from `CodeEditorTheme` instead of hardcoded constants.
  - Refactored `GutterView` to use `CodeEditorTheme` for background, line numbers, and markers.
  - Refactored `SearchController` to use `CodeEditorTheme` for overlay styling with runtime refresh.
  - Added `CodeEditor.bindThemeProperty(ObjectProperty<Theme>)` to observe `DockManager.themeProperty()` changes.
  - Added `CodeEditor.setEditorTheme(CodeEditorTheme)` for direct palette control.
  - Dispose unbinds theme listener; no inheritance from docking `Theme` record.
  - Added 9 mapper unit tests and 7 integration tests. Test suite now 182 passing.
- **2026-02-16:** Completed Phase 4 gutter, markers, and navigation:
  - Added `gutter/` package: `MarkerType`, `Marker`, `MarkerModel`, `GutterView`.
  - Added `search/` package: `SearchMatch`, `SearchModel`, `SearchController`.
  - Modified `Viewport` to render search match highlights with current-match distinction.
  - Modified `CodeEditor` to integrate gutter (BorderPane layout), marker model, search controller (StackPane overlay), go-to-line dialog, and keyboard shortcuts (Ctrl+F, Ctrl+G, Escape).
  - Added 36 new tests (15 MarkerModel, 16 SearchModel, 6 GutterView integration). Test suite now 158 passing.
- **2026-02-16:** Applied Review 3 (Codex) fixes:
  - **HIGH** — Fixed `IncrementalLexerEngine` stale-lines bug: early-stop optimization now validates all remaining baseline lines text-match before copying tail; non-contiguous edits with unchanged line count no longer produce stale tokens.
  - **MEDIUM** — Fixed `MarkdownLexer` ordered list detection for numbers >= 10: replaced single-digit `startsWith(". ", 1)` check with arbitrary digit-span parser; marker length is now computed dynamically.
  - **MEDIUM** — Moved line snapshot creation off UI/change thread in `IncrementalLexerPipeline`: caller now captures lightweight `document.getText()` string; `splitLines()` runs on worker thread.
  - Added 5 regression tests (1 engine non-contiguous edit, 4 Markdown ordered list including negative case). Test suite now 122 passing.
- **2026-02-16:** Added MarkdownLexer with support for headlines, list items, and code blocks. Updated TokenType and Viewport with Markdown-specific categories and colors. Added MarkdownLexerTest. Test suite now 117 passing.
- **2026-02-16:** Completed Phase 3 incremental lexer pipeline. Added lexer model/engine/pipeline, Java/JSON/JavaScript lexers, tokenized viewport rendering, and language-driven async syntax updates in `CodeEditor`. Added 14 Phase 3 tests (language lexers, incremental engine/pipeline, and editor integration). Test suite now 113 passing.
- **2026-02-16:** Applied Review 2 fixes: caret-state restore wiring, undo/redo caret behavior, scroll-state sync, adapter version-aware restore fallback, and disposal APIs (`CodeEditor.dispose`, `Viewport.dispose`). Added 7 new integration tests. Test suite now 99 passing.
- **2026-02-15:** Completed Phase 2 viewport and rendering — `GlyphCache`, `RenderLine`, `SelectionModel`, `Viewport` (canvas-based virtualized renderer), full keyboard/mouse input in `CodeEditor`, headless FX test infrastructure. 92 tests passing.
- **2026-02-14:** Completed Phase 1 core model implementation (`TextSource`, `LineIndex`, `Document`, edit commands) and added document-focused unit tests.
- **2026-02-14:** Completed Phase 0 module bootstrap, integration starter classes, and initial codec test.

## Phase Status
| Phase | Description | Status |
| --- | --- | --- |
| 0 | Module bootstrap + integration skeleton | ✅ Complete |
| 1 | Document core and editing | ✅ Complete |
| 2 | Viewport and rendering | ✅ Complete |
| 3 | Incremental lexer pipeline | ✅ Complete |
| 4 | Gutter, markers, navigation | ✅ Complete |
| 5 | Theme composition and mapping | ✅ Complete |
| 6 | Persistence hardening/migration | ✅ Complete |
| 7 | Failure handling and disposal | ✅ Complete |
| 8 | Benchmarks and documentation hardening | ✅ Complete |

## Implemented Files (Highlights)

### Module Wiring
- `pom.xml` (aggregator includes `papiflyfx-docking-code`)
- `papiflyfx-docking-code/pom.xml`
- `papiflyfx-docking-code/README.md`

### Phase 0 Source
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorFactory.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorStateAdapter.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/state/EditorStateData.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/state/EditorStateCodec.java`

### Phase 1 Source
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/TextSource.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/LineIndex.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/Document.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/InsertEdit.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/DeleteEdit.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/ReplaceEdit.java`

### Phase 2 Source
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/GlyphCache.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/RenderLine.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/SelectionModel.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java`

### Phase 2 Modified
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java` (replaced placeholder with Document + Viewport + input handling)
- `papiflyfx-docking-code/pom.xml` (added headless TestFX surefire config)

### Phase 3 Source
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/TokenType.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/Token.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/LexState.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/LexResult.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/Lexer.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/LineTokens.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/TokenMap.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/PlainTextLexer.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/JavaLexer.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/JsonLexer.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/JavaScriptLexer.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/MarkdownLexer.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/LexerRegistry.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/IncrementalLexerEngine.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/IncrementalLexerPipeline.java`

### Phase 3 Modified
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java` (async lexer pipeline integration + language listener wiring)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java` (token-map rendering support)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/RenderLine.java` (tokenized line payload)

### Phase 4 Source
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/gutter/MarkerType.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/gutter/Marker.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/gutter/MarkerModel.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/gutter/GutterView.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchMatch.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchModel.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java`

### Phase 4 Modified
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java` (gutter + search + go-to-line integration, BorderPane layout)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java` (search highlight rendering)

### Phase 5 Source
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorTheme.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeMapper.java`

### Phase 5 Modified
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java` (replaced hardcoded colors with `CodeEditorTheme` palette)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/gutter/GutterView.java` (replaced hardcoded colors with `CodeEditorTheme` palette)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java` (replaced hardcoded colors with `CodeEditorTheme` palette + runtime refresh)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java` (theme binding, `bindThemeProperty`, `setEditorTheme`, dispose unbind)

### Phase 6 Modified
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/state/EditorStateData.java` (v1 contract Javadoc)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorStateAdapter.java` (version-gated decode helpers, malformed-path guard)
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/LayoutFactory.java` (adapter.restore exception guard + logging)
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java` (adapter.saveState exception guard + logging)

### Review 5 Fixes (cross-module)
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/DisposableContent.java` (new interface)
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockLeaf.java` (dispose calls `DisposableContent.dispose()`)
- `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/LayoutFactory.java` (fixed restore fallback order + null-contentData placeholder fallback)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java` (implements `DisposableContent`, fixed typing caret, removed `moveCaretRight`)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorStateAdapter.java` (file rehydration + missing-file fallback)

### Review 5 Codex-1 Follow-up (performance + hardening)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/LineIndex.java` (added `applyInsert`/`applyDelete` incremental methods)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/Document.java` (insert/delete/replace use incremental line index updates)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/IncrementalLexerPipeline.java` (lazy text snapshot via `enqueueLazy`, deferred `getText()` to worker)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java` (dirty-region tracking via `BitSet`, incremental redraw path)

### Post-Phase 2 Hardening (2026-02-16)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
  - state application now drives caret model,
  - undo/redo caret behavior improved,
  - scroll offset synchronization with viewport clamp,
  - disposal lifecycle hook.
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorStateAdapter.java`
  - version-aware restore path with safe fallback.
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java`
  - disposal lifecycle hook and listener cleanup.

### Phase 8 Source
- `papiflyfx-docking-code/src/main/resources/META-INF/services/org.metalib.papifly.fx.docks.layout.ContentStateAdapter` (ServiceLoader descriptor)

### Phase 8 Modified
- `papiflyfx-docking-code/pom.xml` (added `surefire.excludedGroups` property for benchmark tag exclusion)
- `papiflyfx-docking-code/README.md` (full rewrite with integration guide, quickstart, and acceptance metrics)

### Tests
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/state/EditorStateCodecTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/document/TextSourceTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/document/LineIndexTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/document/DocumentTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/render/SelectionModelTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/render/RenderLineTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/render/ViewportTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorIntegrationTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/lexer/JavaLexerTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/lexer/JsonLexerTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/lexer/JavaScriptLexerTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/lexer/IncrementalLexerEngineTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/lexer/IncrementalLexerPipelineTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/lexer/MarkdownLexerTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/gutter/MarkerModelTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/gutter/GutterViewTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/search/SearchModelTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeMapperTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeIntegrationTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorDockingIntegrationTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/benchmark/CodeEditorBenchmarkTest.java`
- `papiflyfx-docking-docks/src/test/java/org/metalib/papifly/fx/docks/core/DockLeafTest.java`

## Validation Results
- `mvn -pl papiflyfx-docking-code -am compile` -> ✅ success
- `mvn -pl papiflyfx-docking-code,papiflyfx-docking-docks -am -Dtestfx.headless=true test` -> ✅ success (208 tests total, 0 failures, 0 errors)
- `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true -Dgroups=benchmark -Dsurefire.excludedGroups= test` -> ✅ success (4 benchmarks, 0 failures)

### Acceptance Metrics (spec §8, 100k-line Java file)
| Metric | Threshold | Measured | Status |
| --- | --- | --- | --- |
| Large file open + first render | ≤ 2000ms | 218ms | ✅ PASS |
| Typing latency (p95, single char) | ≤ 16ms | 3.27ms | ✅ PASS |
| Scroll rendering (p95) | ≤ 16ms | 0.18ms | ✅ PASS |
| Memory overhead (100k lines) | ≤ 350MB | 63MB | ✅ PASS |

## Notes / Known Issues
- Existing project warning remains in parent build config: duplicate `maven-release-plugin` declaration in root `pom.xml` pluginManagement.
- All Review 5 issues (1–6) and Codex-1 follow-up items are now fully addressed.
- Phase 6 persistence contract is complete: v1 schema documented, migration hooks structured, error containment in place.
- Phase 7 failure handling/lifecycle cleanup is complete.
- Phase 8 benchmarks and docs are complete. All spec §8 acceptance metrics pass with significant margin.
- ServiceLoader auto-discovery is available via `ContentStateRegistry.fromServiceLoader()`.

## MVP Completion Summary
All 8 phases are complete. The `papiflyfx-docking-code` module delivers:
- Canvas-based virtualized code editor with single-caret editing, undo/redo, copy/paste, selection.
- Incremental syntax highlighting for Java, JSON, JavaScript, Markdown, and plain text.
- Line number gutter with marker lane, find/replace overlay with regex, go-to-line navigation.
- Full docking integration via `ContentFactory`, `ContentStateAdapter`, and theme composition.
- Session persistence with version-gated migration and failure containment.
- 208 tests (unit + integration) + 4 benchmarks, all passing.
