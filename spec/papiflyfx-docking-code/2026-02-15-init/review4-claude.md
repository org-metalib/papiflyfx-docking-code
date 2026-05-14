# Review 4 (Claude) — papiflyfx-docking-code

**Date:** 2026-02-16
**Scope:** Full implementation audit of all source and test files against `spec.md` and `implementation.md`.
**Test baseline:** 158 tests passing.

---

## Summary

Phases 0–4 are structurally complete and the test suite is healthy. The issues below fall into three categories: **spec violations** that should be fixed before MVP, **bugs** that affect correctness, and **hardening suggestions** for robustness. No architectural rewrites are needed.

## Implementation Update (2026-02-16)

### Completed Fixes

- ✅ **H1** SearchController CSS strings removed; styling now uses programmatic JavaFX APIs (`Background`, `Border`, `TextFill`, `Padding`).
- ✅ **H2** `IncrementalLexerPipeline` now catches non-cancellation exceptions, logs warning, preserves previous tokens, and continues scheduling.
- ✅ **H3** `CodeEditor` now listens to document changes and recomputes gutter width when line-count digit width changes.
- ✅ **H4** `MarkerModel.removeMarker()` now notifies listeners only when a marker was actually removed.
- ✅ **H5** `Document.fireChange()` is exception-safe per-listener; failures are logged and remaining listeners still execute.
- ✅ **M1** Plain text search no longer returns overlapping matches (`index += searchFor.length()`).
- ✅ **M2** Multi-line search matches are now explicitly skipped in `SearchModel` to keep `SearchMatch` line/column semantics valid.
- ✅ **M3** `SearchModel.replaceCurrent()` now refreshes matches immediately by re-running search.
- ✅ **M4** Added `Document.getSubstring(int, int)` and switched selection copy path to use it (avoids full-document copy).
- ✅ **M5** Added interruption checks in incremental lexer prefix and suffix-reuse scans.
- ✅ **M6** Replaced hardcoded `lineHeight * 0.8` baselines with measured font baseline from `GlyphCache`.
- ✅ **M7** `Viewport.getLineAtY()` now returns `-1` for coordinates above viewport/document range.
- ✅ **L1** Marker priority no longer depends on enum ordinal; `MarkerType` now has explicit `priority()`.
- ✅ **L5** Removed redundant branch in `CodeEditor.setVerticalScrollOffset()` that reapplied unchanged offsets.

### Additional Notes

- Added an extra hardening fix during implementation: language changes in `IncrementalLexerPipeline` now force a full re-lex baseline reset, preventing stale token reuse across lexer/language switches.
- Added/updated tests for: gutter width digit-boundary updates, overlapping search behavior, replace-current match refresh, regex multi-line exclusion behavior, lexer pipeline error recovery, document listener exception safety, non-notifying missing marker removal, viewport negative Y behavior, and incremental lexer cancellation in prefix scans.
- Remaining low-priority items **not implemented** by design: **L2**, **L3**, **L4** (still valid as future optimization/feature work).
- Verification run: `mvn test -pl papiflyfx-docking-code -Dtestfx.headless=true -Djava.awt.headless=true -Dprism.order=sw` (pass).

---

## HIGH — Must Fix

### H1. SearchController uses inline CSS strings (spec violation)

**File:** `search/SearchController.java:26-30`

The spec mandates *"pure programmatic JavaFX (no FXML/CSS)"*. `SearchController` uses `setStyle()` with CSS strings for all styling:

```java
private static final String BACKGROUND_STYLE = "-fx-background-color: #252526; ...";
```

**Fix:** Replace all `setStyle()` calls with programmatic JavaFX equivalents:
- `setBackground(new Background(new BackgroundFill(Color.web("#252526"), ...)))`
- `setBorder(new Border(new BorderStroke(...)))`
- `setTextFill(Color.web("#d4d4d4"))` for labels
- TextField/Button styling via `setBackground()`, `setTextFill()`, `setPadding()`

This is the only CSS spec violation in the codebase.

---

### H2. IncrementalLexerPipeline swallows non-cancellation exceptions

**File:** `lexer/IncrementalLexerPipeline.java:168-181`

**Spec requirement (§6):** *"Lexer failure: keep previous stable tokens for unaffected lines and log warning."*

`processPending()` only catches `CancellationException`. Any `RuntimeException` from `lexer.lexLine()` propagates to the scheduled executor, silently kills the worker thread, and permanently stops syntax highlighting with no log output.

```java
try {
    // ... relex ...
} catch (CancellationException cancellationException) {
    scheduleNextIfNeeded();
    return;
}
```

**Fix:** Add a catch for `Exception`, log a warning (e.g. `System.Logger` or `java.util.logging`), keep `baseline` as the token map, and call `scheduleNextIfNeeded()`:

```java
} catch (CancellationException e) {
    scheduleNextIfNeeded();
    return;
} catch (Exception e) {
    LOGGER.log(Level.WARNING, "Lexer failure, keeping previous tokens", e);
    scheduleNextIfNeeded();
    return;
}
```

---

### H3. GutterView width not updated on document edits that change line count

**File:** `gutter/GutterView.java`, `api/CodeEditor.java`

`gutterView.recomputeWidth()` is only called from `CodeEditor.setText()` and `GutterView.setDocument()`. When the user types and line count goes from 99 to 100 (or 999 to 1000), the gutter stays at the old digit width. The width only updates on the next `setText()` or `setDocument()` call.

**Fix:** In `CodeEditor` constructor, add a document change listener that calls `gutterView.recomputeWidth()` when line count changes:

```java
document.addChangeListener(event -> {
    int lineCount = document.getLineCount();
    int digits = Math.max(2, String.valueOf(lineCount).length());
    // Only recompute if digit count changed
    if (digits != currentGutterDigits) {
        currentGutterDigits = digits;
        gutterView.recomputeWidth();
    }
});
```

---

### H4. MarkerModel.removeMarker() fires change event unconditionally

**File:** `gutter/MarkerModel.java:52-60`

`fireChanged()` is always called even if the marker was not in the list. This triggers unnecessary gutter redraws and listener notifications.

```java
public void removeMarker(Marker marker) {
    List<Marker> lineMarkers = markersByLine.get(marker.line());
    if (lineMarkers != null) {
        lineMarkers.remove(marker);
        if (lineMarkers.isEmpty()) {
            markersByLine.remove(marker.line());
        }
    }
    fireChanged(); // <-- fires even if marker wasn't present
}
```

**Fix:** Only fire when removal actually occurred:

```java
public void removeMarker(Marker marker) {
    List<Marker> lineMarkers = markersByLine.get(marker.line());
    if (lineMarkers != null && lineMarkers.remove(marker)) {
        if (lineMarkers.isEmpty()) {
            markersByLine.remove(marker.line());
        }
        fireChanged();
    }
}
```

---

### H5. Document.fireChange() not exception-safe

**File:** `document/Document.java` (fireChange method)

If any `DocumentChangeListener` throws a `RuntimeException`, remaining listeners in the list are never notified. This can silently break the lexer pipeline, gutter sync, or viewport dirty-flagging.

**Fix:** Wrap each listener callback in try-catch:

```java
private void fireChange(DocumentChangeEvent event) {
    for (DocumentChangeListener listener : listeners) {
        try {
            listener.documentChanged(event);
        } catch (RuntimeException e) {
            // Log and continue to remaining listeners
        }
    }
}
```

---

## MEDIUM — Should Fix

### M1. SearchModel plain text search finds overlapping matches

**File:** `search/SearchModel.java:238`

Increment after a match is `index++`, which finds overlapping matches (e.g. "aa" in "aaa" yields matches at offsets 0 and 1). Standard editor behavior is to advance by query length.

**Fix:** Change `index++` to `index += searchFor.length()`.

---

### M2. SearchModel regex endColumn calculation incorrect for multi-line matches

**File:** `search/SearchModel.java:259`

```java
int endCol = end - lineStart;
```

If a regex match spans multiple lines, `end` is on a different line than `start`, so `endCol = end - lineStart` produces a value larger than the line length (or the wrong value entirely). The `SearchMatch.endColumn` would be nonsensical.

**Fix:** Compute `endCol` relative to the line where the match ends:

```java
int endLine = document.getLineForOffset(end);
int endLineStart = document.getLineStartOffset(endLine);
int endCol = end - endLineStart;
```

Note: `SearchMatch` record would also need an `endLine` field, or multi-line matches should be split per-line.

---

### M3. SearchModel.replaceCurrent() leaves stale match list

**File:** `search/SearchModel.java:187-193`

After `replaceCurrent()`, the match list still contains the old offsets. All matches after the replaced one now have stale offsets. Callers must remember to call `search()` again.

**Fix:** After replacing, call `search(document)` to refresh matches, or at minimum document this contract.

---

### M4. SelectionModel.getSelectedText() copies entire document text

**File:** `render/SelectionModel.java:108`

```java
return document.getText().substring(startOffset, endOffset);
```

For a 100k-line document, this allocates the full text string just to extract a substring. Called on every Ctrl+C.

**Fix:** Add a `Document.getSubstring(int start, int end)` method that delegates to `TextSource.substring()`, and use it here.

---

### M5. IncrementalLexerEngine prefix scan loop has no cancellation check

**File:** `lexer/IncrementalLexerEngine.java:58-64`

The prefix-reuse loop iterates without calling `ensureNotInterrupted()`. On a 100k-line document where the prefix is long, cancellation cannot be detected until the loop completes.

**Fix:** Add `ensureNotInterrupted()` inside the prefix loop (e.g. every 1000 lines):

```java
for (int i = 0; i < maxPrefix; i++) {
    if (i % 1000 == 0) ensureNotInterrupted();
    // ...
}
```

---

### M6. Viewport and GutterView hardcode baseline offset at 0.8

**Files:** `render/Viewport.java:370`, `gutter/GutterView.java:142`

```java
double baseline = lineHeight * 0.8;
```

The 0.8 factor assumes a specific ascent/descent ratio. Different monospace fonts (JetBrains Mono, Fira Code, Cascadia) have different metrics. Text will appear misaligned vertically with non-default fonts.

**Fix:** Derive baseline from actual font metrics:

```java
double baseline = glyphCache.getFont().getSize(); // or measure via Text node ascent
```

Or add a `getBaseline()` method to `GlyphCache` that measures the actual ascent.

---

### M7. Viewport.getLineAtY() returns 0 when it should return -1

**File:** `render/Viewport.java:228-229`

When `y + scrollOffset` is negative (mouse above viewport), the method returns `0` instead of `-1`. This makes it impossible for callers to distinguish "clicked on line 0" from "clicked outside the document area".

The method doc says *"returns -1 if outside"* but only returns -1 when `document == null`.

**Fix:** Return -1 when computed line is negative:

```java
if (line < 0) {
    return -1;
}
```

---

## LOW — Consider Fixing

### L1. MarkerType priority depends on enum ordinal order

**File:** `gutter/MarkerModel.java:119`

```java
if (best == null || m.type().ordinal() < best.ordinal()) {
```

If `MarkerType` enum constants are reordered, priority logic silently breaks. Consider adding an explicit `priority()` method to `MarkerType` or documenting the ordinal contract.

---

### L2. Viewport draws full canvas every frame

**File:** `render/Viewport.java:284-285`

The entire canvas is cleared and redrawn on every `layoutChildren()` call, even if only the caret blinked. For large viewports this is wasteful. The `dirty` flag is binary; a region-based dirty tracking could optimize redraws.

This is acceptable for MVP but should be addressed if profiling shows rendering latency exceeding the 16ms p95 target.

---

### L3. AbstractCStyleLexer skips identifier tokens

**File:** `lexer/AbstractCStyleLexer.java:102`

```java
if (type != TokenType.IDENTIFIER && type != TokenType.PLAIN) {
    addToken(tokens, index, end, type);
}
```

Identifiers and plain text words are intentionally not emitted as tokens — the viewport renders gaps between tokens with `TEXT_COLOR`. This is correct for MVP but means future features (e.g. semantic highlighting, symbol navigation) would need to change this behavior.

---

### L4. AbstractCStyleLexer number scanning misses hex/binary literals

**File:** `lexer/AbstractCStyleLexer.java:207-213`

`isNumberStart()` and `scanNumber()` only handle decimal and scientific notation. Hex (`0xFF`), binary (`0b1010`), and octal (`0o17`) literals are tokenized as identifier `0` + identifier `xFF` instead of a single number token. Acceptable for MVP but visually wrong for code using these formats.

---

### L5. CodeEditor.setVerticalScrollOffset() has redundant branch

**File:** `api/CodeEditor.java:659-663`

```java
if (Double.compare(this.verticalScrollOffset.get(), safeOffset) == 0) {
    applyScrollOffset(safeOffset);  // still calls applyScrollOffset
    return;
}
```

When the offset hasn't changed, it still calls `applyScrollOffset()` which triggers gutter sync and viewport operations. This should be a no-op.

**Fix:**

```java
if (Double.compare(this.verticalScrollOffset.get(), safeOffset) == 0) {
    return;
}
```

---

## Test Coverage Gaps

### T1. No test for gutter width update on line count change (relates to H3)

Add a test that inserts enough lines to cross a digit boundary (e.g. 9 → 10, 99 → 100) and verifies `getComputedWidth()` increases.

### T2. SearchModel overlapping match behavior untested (relates to M1)

Add a test for `"aa"` in `"aaa"` to define expected behavior (currently finds overlapping matches).

### T3. No test for SearchModel.replaceCurrent() match staleness (relates to M3)

Add a test that calls `replaceCurrent()` then verifies remaining match offsets are still valid.

### T4. No integration test for lexer pipeline error recovery (relates to H2)

Add a test with a `Lexer` implementation that throws `RuntimeException` and verify that:
- tokens from previous successful run are preserved,
- subsequent edits still trigger re-lexing.

### T5. No test for search across line boundaries

Add regex search tests where the match spans multiple lines (if supported) or verify that multi-line matches are explicitly excluded.

### T6. Lexer tests lack edge cases

- Java/JavaScript: hex numbers (`0xFF`), escape sequences in strings (`\n`, `\uFFFF`), annotations (`@Override`)
- JSON: escaped characters (`\uXXXX`), nested structures
- Markdown: bold/italic inline, blockquotes, nested lists

---

## Spec Compliance Matrix

| Requirement | Status | Notes |
|---|---|---|
| Pure programmatic JavaFX (no FXML/CSS) | **FAIL** | H1: SearchController uses CSS strings |
| Single-caret editing with undo/redo | PASS | |
| Copy/paste and selection | PASS | |
| Virtualized canvas-based rendering | PASS | |
| Incremental syntax highlighting (Java/JSON/JS) | PASS | |
| Gutter with line numbers and marker lane | PASS | H3: width not auto-updated |
| Find/replace and go-to-line | PASS | M1-M3: edge case issues |
| Full docking integration (factory/adapter) | PASS | |
| Lexer failure keeps previous tokens + log | **FAIL** | H2: exceptions kill worker |
| Theme composition via `CodeEditorTheme` | N/A | Phase 5 not started |
| Disposal contract (stop workers, unbind) | PASS | Minor: SearchController not explicitly disposed |
| Tokenization off FX thread | PASS | |
| Revision-safe token apply | PASS | |

---

## Recommended Fix Order

1. **H1** — SearchController CSS removal (spec compliance, self-contained change)
2. **H2** — Pipeline error handling (spec compliance, prevents silent failures)
3. **H3** — Gutter width auto-update (visible UI bug)
4. **H4** — MarkerModel conditional fire (trivial fix)
5. **H5** — Exception-safe listener dispatch (robustness)
6. **M1–M3** — SearchModel correctness fixes
7. **M4** — SelectionModel performance
8. Remaining medium and low items as time permits
