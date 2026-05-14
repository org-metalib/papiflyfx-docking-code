# Code Review Recommendations - Completed

**Date:** 2026-02-15
**Source:** `spec/papiflyfx-docking-code/claude-review1.md` Recommendations 1-4

---

## Recommendation 1: Document Change Notification Mechanism

**Status:** Done

Added a listener-based change notification system to `Document` so that Phase 2 (Viewport) can observe mutations.

### New files
- `document/DocumentChangeListener.java` -- `@FunctionalInterface` with `documentChanged(DocumentChangeEvent)` callback.
- `document/DocumentChangeEvent.java` -- Record carrying `offset`, `oldLength`, `newLength`, and `ChangeType` enum (`INSERT`, `DELETE`, `REPLACE`, `UNDO`, `REDO`, `SET_TEXT`). Package-private factory methods for each type.

### Changes to `Document.java`
- Added `CopyOnWriteArrayList<DocumentChangeListener>` for thread-safe listener management.
- `addChangeListener(listener)` / `removeChangeListener(listener)` public API.
- All mutation paths fire events: `insert`, `delete`, `replace`, `undo`, `redo`, `setText`.
- Event is fired **after** the mutation and index rebuild, so listeners see consistent state.

---

## Recommendation 2: Not implemented (incremental LineIndex)

**Status:** Deferred to Phase 2

The review noted the O(n) `rebuildIndex()` call and recommended profiling or incremental updates. This is deferred -- the change notification mechanism added in Recommendation 1 provides the hook needed for Phase 2 to connect `Viewport`, at which point incremental index updates can be implemented and profiled together.

---

## Recommendation 3: Line Ending Normalization

**Status:** Done

### Changes to `TextSource.java`
- Added `static normalizeLineEndings(String)` method that converts `\r\n` and standalone `\r` to `\n`.
- Fast path: if no `\r` is present, returns input unchanged (no allocation).
- Applied in constructor, `setText()`, `insert()`, and `replace()` -- all text input paths.
- `delete()` does not need normalization (only removes existing content).

---

## Recommendation 4: Expanded Test Coverage

**Status:** Done

Test count increased from **8 tests** to **54 tests** (46 new tests).

### `EditorStateCodecTest` (1 -> 7 tests)
| Test | What it covers |
|---|---|
| `fromMapWithNullReturnsEmpty` | Null map input |
| `fromMapWithEmptyMapReturnsEmpty` | Empty map input |
| `fromMapWithMissingKeysReturnsFallbackValues` | Partial map with missing keys |
| `fromMapWithTypeMismatchedValuesReturnsFallbacks` | Wrong types for all fields |
| `fromMapFoldedLinesDropsNonNumbers` | Mixed list with non-Number and null items |
| `toMapWithNullStateProducesEmptyDefaults` | Null state to toMap() |

### `TextSourceTest` (2 -> 18 tests)
| Test | What it covers |
|---|---|
| `emptySourceOperations` | Empty constructor, length, isEmpty |
| `nullTextInConstructorGivesEmpty` | Null in constructor |
| `setTextReplacesContent` | setText() basic |
| `setTextWithNullClearsContent` | setText(null) |
| `insertAtEndWorks` | Boundary: insert at text end |
| `insertAtStartWorks` | Boundary: insert at offset 0 |
| `deleteLastCharacterWorks` | Boundary: delete last char |
| `deleteAllContentWorks` | Delete entire content |
| `insertNullTextIsNoOp` | Null insert text |
| `insertEmptyTextIsNoOp` | Empty insert text |
| `constructorNormalizesWindowsLineEndings` | `\r\n` -> `\n` in constructor |
| `constructorNormalizesOldMacLineEndings` | `\r` -> `\n` in constructor |
| `setTextNormalizesLineEndings` | `\r\n` and `\r` in setText() |
| `insertNormalizesLineEndings` | `\r\n` and `\r` in insert() |
| `replaceNormalizesLineEndings` | `\r\n` in replace() |
| `mixedLineEndingsNormalized` | Mixed `\r\n`, `\r`, `\n` |

### `LineIndexTest` (2 -> 10 tests)
| Test | What it covers |
|---|---|
| `emptyTextHasOneLine` | Empty string -> 1 line |
| `singleLineNoNewline` | Single line without trailing newline |
| `nullTextHasOneLine` | Null input -> 1 line |
| `invalidLineThrows` | Out-of-range line index |
| `invalidOffsetThrows` | Out-of-range offset |
| `offsetAtEndOfTextIsValid` | Offset == textLength (valid boundary) |
| `rebuildUpdatesIndex` | rebuild() with new text |
| `columnClampingOnToOffset` | Column clamping behavior |

### `DocumentTest` (3 -> 19 tests)
| Test | What it covers |
|---|---|
| `setTextClearsUndoHistory` | setText() clears undo/redo |
| `emptyDocumentBehavior` | Empty document: length, lineCount, undo/redo |
| `insertAtDocumentEnd` | Boundary: insert at end |
| `deleteLastCharacter` | Boundary: delete last char |
| `deleteAllContent` | Delete all content |
| `insertNullTextIsNoOp` | Null insert is no-op, no undo entry |
| `deleteEqualOffsetsIsNoOp` | Equal offsets is no-op |
| `replaceWithEmptyRangeIsInsert` | Replace with empty range delegates to insert |
| `changeListenerFiredOnInsert` | Listener receives INSERT event |
| `changeListenerFiredOnDelete` | Listener receives DELETE event |
| `changeListenerFiredOnReplace` | Listener receives REPLACE event |
| `changeListenerFiredOnSetText` | Listener receives SET_TEXT event |
| `changeListenerFiredOnUndoAndRedo` | Listener receives UNDO/REDO events |
| `removedListenerNotFired` | Removed listener is not called |
| `windowsLineEndingsNormalizedInDocument` | `\r\n` normalization through Document |
| `insertNormalizesLineEndingsInDocument` | Insert normalization through Document |

---

## Recommendations 5-6: Deferred

| # | Recommendation | Target Phase |
|---|---|---|
| 5 | State version migration logic | Phase 6 (Persistence) |
| 6 | Configurable undo stack depth limit | Phase 7 (Hardening) |

These are not applicable until their respective phases.

---

## Validation

```
mvn -pl papiflyfx-docking-code -am test
Tests run: 54, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Files Changed

### New files
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/DocumentChangeListener.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/DocumentChangeEvent.java`

### Modified files
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/Document.java` (change listener support)
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/TextSource.java` (line ending normalization)
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/state/EditorStateCodecTest.java` (6 new tests)
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/document/TextSourceTest.java` (16 new tests)
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/document/LineIndexTest.java` (8 new tests)
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/document/DocumentTest.java` (16 new tests)
