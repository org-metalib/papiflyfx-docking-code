# Code Review: papiflyfx-docking-code Module

**Date:** 2026-02-15
**Scope:** Full review of implemented code (Phase 0 + Phase 1) against spec documents
**Status:** Phase 0 (bootstrap) and Phase 1 (document core) are complete

---

## 1. Spec-to-Code Alignment Summary

| Spec Requirement | Status | Notes |
|---|---|---|
| Module bootstrap (Phase 0) | Done | pom.xml, dependency wiring, package skeleton |
| `CodeEditorFactory` (`factoryId = "code-editor"`) | Done | Correct `ContentFactory` impl |
| `CodeEditorStateAdapter` (version 1) | Done | Correct `ContentStateAdapter` impl |
| `EditorStateData` record | Done | Fields match spec Section 4.3 exactly |
| State serialization round-trip | Done | `EditorStateCodec` with `toMap`/`fromMap` |
| `TextSource` (StringBuilder backing) | Done | Clean offset-based API |
| `LineIndex` (offset/line mapping) | Done | Binary search for O(log n) lookups |
| `Document` with undo/redo | Done | Command pattern with `InsertEdit`, `DeleteEdit`, `ReplaceEdit` |
| `TokenMap` in Document | Not started | Phase 3 (Lexer) |
| `MarkerModel` in Document | Not started | Phase 4 (Gutter) |
| Canvas `Viewport` rendering | Not started | Phase 2 |
| Incremental lexer pipeline | Not started | Phase 3 |
| Gutter / line numbers | Not started | Phase 4 |
| Find/replace, go-to-line | Not started | Phase 4 |
| Theme composition (`CodeEditorTheme`) | Not started | Phase 5 |
| Disposal/lifecycle cleanup | Not started | Phase 7 |
| State version migration | Not started | Phase 6 (hook exists, no migration code) |

**Verdict:** The implemented code faithfully follows the spec and implementation plan. Phase 0 and Phase 1 deliverables are complete and correct.

---

## 2. Architecture Assessment

### What's good

- **Clean layered structure.** `api`, `document`, `state` packages have clear responsibilities. Placeholder packages (`gutter`, `lexer`, `render`, `search`, `theme`) are reserved with `package-info.java` for future work.
- **Command pattern for undo/redo.** `EditCommand` interface with `InsertEdit`, `DeleteEdit`, `ReplaceEdit` is textbook and correct. Redo is implemented by re-applying the same command.
- **Defensive normalization everywhere.** `EditorStateData` compact constructor normalizes nulls and negatives. `CodeEditor` property setters clamp values. `EditorStateCodec.fromMap()` uses type-safe extraction with fallbacks.
- **Separation of concerns.** `EditorStateCodec` handles serialization separately from `CodeEditorStateAdapter`, which handles the docking contract. `TextSource` handles raw text, `LineIndex` handles line mapping, `Document` composes both.
- **Immutability where it matters.** `EditorStateData` uses `List.copyOf()` for `foldedLines`. Edit command fields are final.

### What needs attention

- **`Document.rebuildIndex()` is O(n) on every edit.** This full-text scan after every insert/delete/replace will be a bottleneck for the 100k-line performance target (spec Section 8: typing latency p95 <= 16ms). An incremental index update strategy should be planned for Phase 2+.
- **No change notification from `Document`.** The rendering layer will need to observe document mutations. Currently there is no event/listener/observable mechanism. This should be addressed before Phase 2 (Viewport) starts.
- **`LineIndex.rebuild()` only recognizes `'\n'`.** Windows `'\r\n'` and old Mac `'\r'` line endings will leave stray `'\r'` characters in line text. This could cause rendering artifacts or incorrect column calculations. Recommend normalizing line endings in `TextSource.setText()` or handling `'\r'` in `LineIndex`.
- **`foldedLines` is not a JavaFX property.** The UI cannot react to folded line changes without additional wiring. Acceptable for now (folding is post-MVP) but worth noting.

---

## 3. Per-File Review

### api/CodeEditor.java
- Extends `StackPane` with five JavaFX properties matching `EditorStateData` fields.
- `captureState()` / `applyState()` provide the state bridge to the adapter.
- Currently renders only a placeholder `Label` -- intentional for Phase 0.
- Defensive clamping on all setters is good.
- **Gap:** No `Document` instance held. The text model is not connected to the editor node yet. This is Phase 2 work.

### api/CodeEditorFactory.java
- Minimal, correct `ContentFactory` implementation.
- Returns `null` for unrecognized factory IDs (correct contract for fallback chaining).
- Thread-safe (no mutable state).

### api/CodeEditorStateAdapter.java
- Links to factory via shared `FACTORY_ID` constant.
- Uses pattern matching (`instanceof CodeEditor editor`) -- idiomatic Java 16+.
- Delegates serialization to `EditorStateCodec` (good SRP).
- **Gap:** `getVersion()` returns `1` but no migration logic exists for handling older versions. Spec Section 7 says "restore must handle known older versions." Acceptable now (only version 1 exists), but migration hooks should be added before version 2.
- **Minor:** `saveState` ignores the `contentId` parameter. May need revisiting when editor identity becomes important.

### document/TextSource.java
- Clean `StringBuilder` wrapper with offset-based API.
- `delete()` and `replace()` return the old text -- smart design that simplifies the command pattern.
- Null-safe constructor and mutators.
- Bounds checking via `requireOffset()` / `requireRange()`.
- **Note:** Not thread-safe. Acceptable since document access should be serialized by the caller (spec Section 5.1 says scene graph mutations are FX-thread-only).

### document/LineIndex.java
- Binary search via `Collections.binarySearch` for O(log n) offset-to-line lookup.
- `textLength` is a parameter, not stored -- avoids staleness, good design.
- **Issue:** Only `'\n'` is recognized. See item in Section 2 above.
- `toOffset()` clamps column to line bounds -- prevents out-of-range offsets.

### document/Document.java
- Composes `TextSource` + `LineIndex`. Maintains `undoStack` / `redoStack` as `ArrayDeque<EditCommand>`.
- `insert`, `delete`, `replace` each wrap in a command, apply, push to undo, clear redo.
- `setText()` clears history -- intentional (full replacement is non-undoable).
- **Issue:** `rebuildIndex()` after every edit is O(n). See item in Section 2 above.
- **Gap:** No `TokenMap`, `MarkerModel`, or change listeners yet.

### document/InsertEdit.java, DeleteEdit.java, ReplaceEdit.java
- `DeleteEdit` and `ReplaceEdit` use lazy capture: first `apply()` stores the deleted/replaced text, subsequent calls (redo) use stored lengths. This is correct because the undo/redo stack guarantees document state consistency.
- Null safety via `Objects.requireNonNullElse`.
- Package-private visibility is appropriate.

### state/EditorStateData.java
- Record with 6 fields matching spec Section 4.3 exactly.
- Compact constructor normalizes all fields defensively.
- `empty()` factory returns sensible defaults.

### state/EditorStateCodec.java
- `toMap()` / `fromMap()` with `LinkedHashMap` (preserves key order for readability).
- Type-safe extraction helpers with fallback values.
- `asIntList()` silently drops non-Number items -- robust but could mask corruption.
- String key constants reduce typo risk.

---

## 4. Test Coverage Assessment

| Test Class | What's Covered | Gaps |
|---|---|---|
| `EditorStateCodecTest` | Round-trip with non-default values | Missing: null input, empty map, type-mismatched values, missing keys |
| `TextSourceTest` | Insert/delete/replace happy path + invalid ranges | Missing: empty source, null text, boundary offsets, `setText()` |
| `LineIndexTest` | Line/column mapping, trailing newline, column clamping | Missing: empty text, single line, `'\r\n'` handling, error cases |
| `DocumentTest` | Multi-op undo/redo flow, redo clearing, line queries | Missing: `setText()` history clearing, empty document, boundary edits |

**Overall:** Happy paths are well covered. The tests validate core invariants (undo/redo determinism, round-trip serialization, line mapping correctness). Edge case coverage should be expanded, particularly for:
1. Empty/null inputs in codec
2. Windows line endings in LineIndex
3. Boundary conditions (insert at document end, delete last character)
4. `setText()` clearing undo history

---

## 5. Risks for Upcoming Phases

| Risk | Phase Affected | Mitigation |
|---|---|---|
| O(n) index rebuild per edit | Phase 2 (Viewport) | Switch to incremental line index updates before rendering integration |
| No document change events | Phase 2 (Viewport) | Add observable/listener pattern to `Document` before `Viewport` wires to it |
| `'\r\n'` line ending handling | Phase 2+ | Normalize on load in `TextSource.setText()` or handle in `LineIndex` |
| No undo stack size limit | Phase 7 (Hardening) | Could grow unbounded for long editing sessions; add configurable cap |
| State version migration absent | Phase 6 (Persistence) | Add migration handler before shipping version 2 of `EditorStateData` |

---

## 6. Recommendations

1. **Before Phase 2:** Add a change notification mechanism to `Document` (e.g., a listener list or JavaFX `InvalidationListener`) so that `Viewport` can observe mutations.
2. **Before Phase 2:** Plan incremental `LineIndex` updates (or at minimum, profile the O(n) rebuild to confirm it meets the 16ms latency target for typical edits on 100k-line files).
3. **Soon:** Normalize `'\r\n'` and `'\r'` line endings to `'\n'` in `TextSource.setText()` to avoid cross-platform issues.
4. **Soon:** Expand test coverage for edge cases listed in Section 4.
5. **Phase 6:** Implement version migration logic in `CodeEditorStateAdapter.restore()` before introducing `EditorStateData` v2.
6. **Phase 7:** Add a configurable undo stack depth limit to prevent unbounded memory growth during long sessions.

---

## 7. Conclusion

The implementation is solid and well-aligned with the spec. Code quality is high: defensive programming, clean separation of concerns, idiomatic Java patterns, and correct undo/redo semantics. The module is in good shape to proceed to Phase 2 (Viewport/rendering), provided the change notification and line index performance items are addressed first.
