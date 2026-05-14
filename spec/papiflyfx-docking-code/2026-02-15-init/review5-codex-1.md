# Review 5 Follow-up (Codex-1)

Date: 2026-02-17
Source reviewed: `spec/papiflyfx-docking-code/review5-codex.md`

## Verification Summary

- Full reactor validation run: `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true test`
- Result: PASS (`197` tests, `0` failures, `0` errors)

## Issue Status

| # | Original Issue | Status | Evidence | Notes |
| --- | --- | --- | --- | --- |
| 1 | Typed character input does not advance caret | ADDRESSED | `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:326` | `handleKeyTyped()` now advances using `moveCaretToOffset(offset + ch.length())`. Regression test exists at `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorIntegrationTest.java:298`. |
| 2 | Editor disposal contract not enforced on leaf close | ADDRESSED | `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:49`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/core/DockLeaf.java:150`, `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/DockManager.java:346` | `CodeEditor` implements `DisposableContent`; `DockLeaf.dispose()` calls `DisposableContent.dispose()`, and `DockManager` close path calls `leaf.dispose()`. DockManager-level disposal test added at `DockManagerCloseLeafFxTest.java:closingLeafCallsDisposableContentDispose`. |
| 3 | Restore fallback order violates Adapter -> Factory -> Placeholder | ADDRESSED | `papiflyfx-docking-docks/src/main/java/org/metalib/papifly/fx/docks/layout/LayoutFactory.java:107` | Placeholder is now always created as final fallback, even when `contentData == null` (uses leaf id/title). Tests at `LayoutFactoryFxTest.java:build_leaf_createsPlaceholderWhenContentDataPresentButNoAdapterOrFactory` and `build_leaf_createsPlaceholderWhenNullContentDataAndNoFactory`. |
| 4 | Restore does not rehydrate document content; cursor/scroll fragile | ADDRESSED | `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorStateAdapter.java:51`, `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorStateAdapter.java:77` | Restore now loads file content first and then applies state (`rehydrateDocument` + `applyState`). Tests for readable and missing files: `CodeEditorIntegrationTest.java:338`, `CodeEditorIntegrationTest.java:366`. |
| 5 | Per-edit O(n) work risks large-file latency targets | ADDRESSED | `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/LineIndex.java:applyInsert`, `LineIndex.java:applyDelete`, `Document.java:insert/delete/replace`, `IncrementalLexerPipeline.java:enqueueLazy` | LineIndex now supports incremental `applyInsert`/`applyDelete` methods. Document insert/delete/replace use incremental updates instead of full rebuild. IncrementalLexerPipeline defers `getText()` snapshot to worker thread via lazy enqueue, avoiding O(n) on every keystroke. Performance guard test validates <1ms per-edit on 50k-line document at `DocumentTest.java:perEditPerformanceGuard_largeDocument`. |
| 6 | Dirty-region incremental redraw not implemented | ADDRESSED | `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java` | Viewport now tracks dirty lines via `BitSet dirtyLines` and a `fullRedrawRequired` flag. Document changes dirty only affected lines from change offset onward. Caret moves dirty only old/new caret lines. Full redraw occurs only on scroll position change, resize, theme change, etc. Incremental path clears and repaints only dirty line regions. |

## Test Gap Status From Review 5

1. Typed-key caret progression test: CLOSED (`CodeEditorIntegrationTest.java:298`).
2. Leaf close disposal integration with editor content: CLOSED. DockManager-level test verifies `DisposableContent.dispose()` is called (`DockManagerCloseLeafFxTest.java:closingLeafCallsDisposableContentDispose`).
3. Restore precedence adapter -> factory -> placeholder: CLOSED. Explicit placeholder-precedence tests for both contentData-present and contentData-null cases (`LayoutFactoryFxTest.java`).

## Conclusion

All items from `review5-codex.md` are fully addressed.
Total test count: 197 (up from 186), 0 failures, 0 errors.
