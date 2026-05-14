# papiflyfx-docking-code quality review (2026-02-22)

## Prompt

For `papiflyfx-docking-code` module:
- Score this code from 1 to 10 based on clarity, maintainability, testability, performance, and architecture.
  Explain the score in bullet points and show key fixes that would raise it.
- Break down the biggest sources of technical debt in this code snippet. Rank them from highest to lowest impact and
  propose refactoring steps for each.
- Check for unnecessary complexity, large functions, repeated logic, overly large classes, or mixed responsibilities.
  Recommend how to simplify structure and improve cohesion.

Add the findings to spec/papiflyfx-docking-code-quality

## Scope and baseline
- Module reviewed: `papiflyfx-docking-code`
- Test baseline: `mvn -pl papiflyfx-docking-code -Dtestfx.headless=true test`
- Result: 326 tests passed, 0 failed, 0 errors
- Code size snapshot:
  - Main: ~8,254 LOC
  - Tests: ~6,022 LOC
  - Largest files: `CodeEditor.java` (1,766 LOC), `Viewport.java` (905 LOC), `SearchController.java` (578 LOC), `SearchModel.java` (487 LOC)

## Score (1-10)
- Overall: **6/10**
- Clarity: **6/10**
- Maintainability: **5/10**
- Testability: **7/10**
- Performance: **6/10**
- Architecture: **5/10**

### Why this score
- Strong automated test base and broad feature coverage improve confidence.
- Public APIs and class names are generally understandable.
- Two central classes (`CodeEditor`, `Viewport`) are monolithic and mix too many concerns.
- Rendering and search paths contain avoidable per-frame/per-keystroke allocations.
- A correctness defect exists in document undo/redo when CRLF normalization changes replacement length.

## Findings (ranked by impact)

1. **Critical: undo/redo correctness breaks on CRLF-normalized edits**
- Evidence:
  - `Document.insert` builds command with unnormalized text: `new InsertEdit(offset, text)` (`papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/Document.java:156`)
  - `InsertEdit.undo` deletes `text.length()` characters (`papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/InsertEdit.java:31`)
  - `ReplaceEdit.undo` uses `replacement.length()` (`papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/ReplaceEdit.java:38`)
- Reproduced locally:
  - `insert("\r\n")` + undo removed too much text (`"ab" -> "a"`)
  - `replace(..., "\r\n")` + undo threw `IndexOutOfBoundsException`
- Impact:
  - Data corruption / failed undo in real editing flows when Windows line endings are pasted/loaded.
- Refactor/fix steps:
  - Normalize text before creating edit commands, and store normalized replacement length explicitly.
  - Make `InsertEdit`/`ReplaceEdit` operate on canonical normalized text only.
  - Add focused tests for `undo/redo` after `\r\n` insert/replace in `DocumentTest`.

2. **High: `CodeEditor` is a God class with mixed responsibilities**
- Evidence:
  - 1,766 LOC single class (`papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`)
  - Handles UI composition, event wiring, key routing, edit commands, search integration, state persistence, theme wiring, and disposal in one type (`CodeEditor.java:118-204`, `CodeEditor.java:461-547`, `CodeEditor.java:1554-1748`)
- Impact:
  - Slower feature delivery, higher regression risk, difficult targeted unit tests.
- Refactor steps:
  - Extract `EditorInputController` (mouse/keyboard), `EditorCommandExecutor` (text ops), `EditorStateCoordinator` (capture/apply state).
  - Keep `CodeEditor` as composition root and thin facade.
  - Move line/block operations into dedicated service (see finding 4).

3. **High: `Viewport` rendering path is too complex and duplicates logic**
- Evidence:
  - 905 LOC in one renderer class (`papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java`)
  - Duplicate selection-drawing logic (`drawSelectionRange` vs `drawSelectionRangeForLine`) (`Viewport.java:600-624`, `Viewport.java:743-766`)
  - Duplicate full/incremental paint paths with repeated branches (`Viewport.java:466-528`)
- Impact:
  - Hard to optimize safely, hard to reason about repaint bugs.
- Refactor steps:
  - Introduce render passes (`BackgroundPass`, `SearchPass`, `SelectionPass`, `TextPass`, `CaretPass`) shared by full/incremental modes.
  - Consolidate selection-rect calculation into one helper returning line segments.
  - Keep dirty-line orchestration separate from drawing primitives.

4. **Medium-High: repeated line-block editing logic in `CodeEditor`**
- Evidence:
  - Same “resolve selected line block” logic copied in multiple handlers (`handleMoveLineUp`, `handleMoveLineDown`, `handleDuplicateLineUp`, `handleDuplicateLineDown`) (`CodeEditor.java:899-1034`)
  - Dead/unused variable indicates drift (`String insertion` unused in `handleDuplicateLineDown`) (`CodeEditor.java:1020`)
- Impact:
  - Behavior drift between commands and harder bug-fixing for edge cases.
- Refactor steps:
  - Extract `LineBlock` helper (`startLine`, `endLine`, `startOffset`, `endOffset`, `text`).
  - Implement `LineEditService` with `moveUp/moveDown/duplicateUp/duplicateDown/delete/join` on shared primitives.
  - Add parameterized tests for line operations over first/last line and no-final-newline files.

5. **Medium: avoidable rendering/search allocations affect scaling**
- Evidence:
  - Per-token `substring` allocations during painting (`Viewport.java:679`)
  - Nested loops for search paint (`matches x renderLines`) (`Viewport.java:772-783`)
  - Case-insensitive search duplicates full document string per search (`SearchModel.java:342-343`)
- Impact:
  - Jank risk on large files and heavy search activity.
- Refactor steps:
  - Prefer `fillText(text, x, y, maxWidth)` span strategy or cached glyph runs to avoid substring churn.
  - Pre-index matches by line for paint (`Map<Integer, List<SearchMatch>>`).
  - Cache compiled pattern and lowercase query/text snapshots by revision when possible.

6. **Medium: construction-time tight coupling limits isolated testing**
- Evidence:
  - `CodeEditor` internally creates `SearchController`, `GoToLineController`, `IncrementalLexerPipeline`, `MarkerModel` (`CodeEditor.java:127-150`, `CodeEditor.java:189`)
- Impact:
  - Hard to mock behavior in small tests; integration tests become the default.
- Refactor steps:
  - Add package-private constructor for dependency injection (controllers, pipeline factory, command executor).
  - Keep public constructor delegating to defaults.

## Unnecessary complexity and cohesion issues
- Overly large class: `CodeEditor` currently acts as view + controller + command bus + state adapter + lifecycle manager.
- Large class: `Viewport` combines scheduling, dirty region tracking, render data prep, and low-level painting.
- Repeated logic:
  - line block extraction in multiple command handlers (`CodeEditor.java:899-1034`)
  - selection and search paint duplication (`Viewport.java:600-624`, `Viewport.java:743-766`, `Viewport.java:701-714`, `Viewport.java:768-783`)
- Mixed responsibilities:
  - Search navigation side effects are split between `SearchController` and `CodeEditor` callbacks, increasing implicit coupling (`CodeEditor.java:1389-1412`, `SearchController.java:326-341`).

## Key fixes that would raise score fastest
1. Fix CRLF undo/redo invariants and add regression tests.
2. Split `CodeEditor` into input/command/state collaborators.
3. Refactor `Viewport` into composable render passes and remove duplicate draw code.
4. Centralize line/block edit operations in a single service.
5. Add small performance optimizations (search-line index map, reduced substring churn).

## Expected score after fixes
- With fixes 1-3: **7.5/10** (major reliability and maintainability jump)
- With fixes 4-5: **8/10** (better cohesion and large-file responsiveness)

## Resolution status (2026-02-22)
- Finding 1 (CRLF undo/redo): `resolved`
  - Fixed in `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/Document.java` with normalized edit payloads.
  - Covered by CRLF regression tests in `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/document/DocumentTest.java`.
- Finding 2 (CodeEditor mixed responsibilities): `resolved via extraction`
  - Extracted collaborators: `EditorInputController`, `EditorCommandExecutor`, `EditorEditController`, `EditorPointerController`, `EditorStateCoordinator`.
  - `CodeEditor.java` reduced from 1,766 LOC to 1,286 LOC.
- Finding 3 (Viewport complexity/duplication): `resolved via render pipeline`
  - Added `RenderContext`, `SelectionGeometry`, and pass classes (`BackgroundPass`, `SearchPass`, `SelectionPass`, `TextPass`, `CaretPass`).
  - `Viewport.java` reduced from 905 LOC to 658 LOC and now uses shared full/incremental pass flow.
- Finding 4 (duplicated line operations): `resolved`
  - Added `LineBlock` + `LineEditService` and replaced duplicated line-edit handlers in `CodeEditor`.
  - Covered by `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/command/LineOperationsTest.java`.
- Finding 5 (render/search allocations): `resolved`
  - Search render now uses line-indexed matches in `Viewport`.
  - `SearchModel` now uses regex pattern caching and bounded case-insensitive matching without full-text lowercase duplication.
  - `TextPass` now paints base text once and overlays only styled runs to reduce per-line substring churn.
- Finding 6 (construction-time coupling): `resolved`
  - Added package-private injection constructor in `CodeEditor` for model/controller/factory seams.
  - Covered by targeted integration and collaborator tests.

### Validation snapshot
- Full headless module suite: `mvn -pl papiflyfx-docking-code -Dtestfx.headless=true test`
  - Result: 344 tests passed, 0 failed, 0 errors
- Benchmark suite: `mvn -pl papiflyfx-docking-code -Dtest=CodeEditorBenchmarkTest -Dsurefire.excludedGroups= -Dtestfx.headless=true test`
  - Large file open+render: `217ms` (baseline `406ms`)
  - Typing p95: `3.06ms` (baseline `3.38ms`)
  - Multi-caret typing p95: `10.59ms` (baseline `10.99ms`)
  - Scroll p95: `0.20ms` (baseline `0.18ms`)
  - Multi-caret scroll p95: `0.50ms` (baseline `0.55ms`)
  - Memory overhead: `55MB` (baseline `59MB`)
