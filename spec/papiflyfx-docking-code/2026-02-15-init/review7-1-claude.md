# Review 7 (Claude-0): Phase 7 Implementation Review

Date: 2026-02-17
Reviewer: Claude Opus 4.6
Target: `review7-codex-0.md` — Phase 7: Failure Handling and Lifecycle Cleanup

## 1. Overall Assessment

**Verdict: PASS with minor observations**

All four workstreams (A–D) from the review plan are implemented correctly. The code changes are well-structured, tests cover the specified scenarios, and the PROGRESS.md documentation is accurate.

## 2. Workstream A: Safe Lexer Failure Fallback

**Status: Correctly implemented**

### What was required
- Replace "keep previous tokens" failure branch with deterministic plain-text fallback for the current text snapshot.
- Include languageId, revision, and dirtyStartLine in warning logs.
- Keep cancellation as a non-error path.

### What was implemented (`IncrementalLexerPipeline.java`)

1. **Plain-text fallback on exception** (lines 250–275): When the primary lexer throws, a `PLAIN_TEXT_FALLBACK_LEXER` (static singleton at line 26) re-lexes the same `lines` list for the same request. The fallback uses `baseline` (which could be non-empty if `forceFullRelex` was false), but this is acceptable because the plain-text lexer always produces a complete token map anyway.

2. **Nested fallback guard** (lines 266–275): If even the plain-text fallback throws (extremely unlikely but defensive), it logs and returns without applying stale tokens — previous `tokenMap` field is left unchanged, but no `tokenMapConsumer` callback fires, which is the correct choice.

3. **Log context** (lines 251–257): Warning log includes `languageId`, `revision`, and `dirtyStartLine` — matches spec exactly.

4. **Cancellation path** (lines 247–249): `CancellationException` is caught separately and triggers `scheduleNextIfNeeded()` without logging or applying tokens — unchanged from prior behavior.

### Observations

- **MINOR — fallback uses `baseline` not `TokenMap.empty()`**: The fallback re-lex passes `baseline` (line 260), which is the non-empty prior token map when `forceFullRelex=false`. For `PlainTextLexer` this is harmless because plain-text lexing produces trivial tokens regardless of baseline state. However, the spec says "Re-lex with `PlainTextLexer` for the same revision request" which is ambiguous about baseline. Current behavior is acceptable and potentially faster for incremental cases.

- **No issue**: The `PLAIN_TEXT_FALLBACK_LEXER` is a static final singleton — thread-safe and correct since `PlainTextLexer` is stateless.

## 3. Workstream B: Lifecycle Cleanup Hardening

**Status: Correctly implemented**

### What was required
- In `CodeEditor.dispose()`, clear search callbacks (`onNavigate`, `onClose`, `onSearchChanged`), detach document reference, close overlay.
- Keep disposal idempotent.
- No post-dispose UI mutation callbacks.

### What was implemented (`CodeEditor.java`, lines 748–772)

1. **Idempotency guard** (lines 750–753): `disposed` flag checked first, set immediately — correct.

2. **Search controller cleanup** (lines 754–758): All three callbacks nulled (`setOnNavigate(null)`, `setOnClose(null)`, `setOnSearchChanged(null)`), document detached (`setDocument(null)`), overlay closed (`searchController.close()`). The ordering is correct — callbacks are nulled *before* `close()` so the close action doesn't fire the user-facing `onClose` callback.

3. **Theme unbinding** (line 759): `unbindThemeProperty()` called — prevents any further theme change callbacks.

4. **Input handler removal** (lines 760–764): All key/mouse/scroll handlers nulled.

5. **Listener removal** (lines 765–769): Caret, column, gutter-width, scroll-offset, and language listeners all removed.

6. **Resource disposal** (lines 770–771): `lexerPipeline.dispose()` and `viewport.dispose()` called last — correct order since callbacks are already disconnected.

### Observations

- **No issue with ordering**: The dispose sequence correctly clears callbacks before disposing resources, preventing any late-firing callbacks from reaching disposed components.

- **MINOR — `markerModel` change listener not removed**: The constructor adds `this.markerModel.addChangeListener(gutterView::markDirty)` (line 109), but `dispose()` does not remove this listener. Since both `markerModel` and `gutterView` are owned by the editor and have no external references, this is a retention-neutral leak (same ownership graph), but for completeness it could be cleaned up. Not a functional issue.

## 4. Workstream C: Failure Path and Worker-Stop Tests

**Status: All required scenarios covered**

### Required scenario 1: Lexer throws -> plain-text fallback for current text
**Test**: `IncrementalLexerPipelineTest.fallsBackToPlainTextForCurrentSnapshotWhenLexerThrowsAndRecovers()` (lines 101–149)
- Uses a custom `ThrowingLexer` with an `AtomicBoolean` toggle to control failure.
- Verifies that after the lexer throws, the applied token map contains the current text `"class Broken {}"` with empty tokens (plain-text semantics).
- Then verifies recovery: turning off failure and switching to JavaScript produces keyword tokens again.
- **Correctly covers the spec requirement.**

### Required scenario 2: After dispose, no token-map callbacks
**Test**: `IncrementalLexerPipelineTest.disposeStopsApplyingTokenUpdates()` (lines 153–187)
- Captures `applyCount` before dispose, then modifies the document after dispose.
- Waits 200ms and asserts no additional apply calls occurred.
- **Correctly covers post-dispose quiescence.**

### Required scenario 3: Session restore with malformed/failed content
**Test**: `DockManagerSessionFxTest.restoreSession_adapterRestoreFailureFallsBackAndCaptureStillWorks()` (lines 343–417)
- Registers a `ContentStateAdapter` whose `restore()` and `saveState()` both throw.
- Builds a `DockSessionData` with a leaf using this adapter.
- Verifies the restore still produces a valid layout (root non-null, tab group with 1 tab, content non-null).
- Verifies `captureSession()` still succeeds and preserves the leaf's type key.
- **Correctly covers end-to-end failure containment at docking level.**

### Required scenario 4: Missing/unreadable file restore (regression guard)
**Test**: `CodeEditorIntegrationTest.stateAdapterHandlesMissingFile()` (lines 387–405)
- Restores with a non-existent file path.
- Verifies empty text, but metadata preserved (filePath, languageId).
- **Correctly guards the regression.**

### Additional test: Dispose clears search callbacks
**Test**: `CodeEditorIntegrationTest.disposeClearsSearchControllerCallbacksBeforeClose()` (lines 218–235)
- Sets custom `onClose` and `onSearchChanged` callbacks, opens search, then disposes.
- Verifies neither callback was fired during dispose, and search overlay is closed.
- **Good additional coverage for Workstream B.**

## 5. Workstream D: Progress and Contract Notes

**Status: Correctly updated**

- `PROGRESS.md` records Phase 7 as complete with detailed workstream descriptions.
- Fallback semantics documented: "lexer exception path now falls back to plain-text tokenization of the current snapshot (not stale previous tokens)".
- Validation results listed with test counts.
- Phase status table updated.

## 6. Summary of Findings

| # | Severity | Area | Finding |
|---|----------|------|---------|
| 1 | MINOR | Workstream A | Fallback re-lex passes `baseline` instead of `TokenMap.empty()`. Functionally neutral for `PlainTextLexer` but semantically imprecise vs. spec wording. No fix required. |
| 2 | MINOR | Workstream B | `markerModel` change listener (`gutterView::markDirty`) added in constructor is not removed in `dispose()`. No functional impact since both objects share the same lifecycle. Consider adding removal for completeness. |

## 7. Test Verification

All test scenarios from the review plan (section "Required scenarios" 1–4) have corresponding automated tests. No gaps found.

## 8. Conclusion

Phase 7 implementation faithfully follows the `review7-codex-0.md` plan. The lexer failure fallback correctly replaces the old "keep stale tokens" behavior with deterministic plain-text re-lexing. Lifecycle disposal is thorough with proper ordering. The test matrix covers all exit criteria. The two minor observations are non-blocking and do not affect correctness or safety.
