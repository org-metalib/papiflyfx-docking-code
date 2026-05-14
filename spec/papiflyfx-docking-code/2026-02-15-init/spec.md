# PapiflyFX Code Specification

PapiflyFX Code is a dockable JavaFX code editor content type for the PapiflyFX docking framework. It must stay pure programmatic (no FXML/CSS), fast on large files, and compatible with docking session persistence.

## 1. Vision
- Pure JavaFX implementation aligned with project architecture.
- Practical performance for large documents (100k+ lines) through virtualization and incremental lexing.
- Stable extension points for syntax, markers, and persistence.
- Native integration with docking theme/state lifecycle.
- Implemented as a separate Maven module: `papiflyfx-docking-code`.

## 2. Scope

### 2.1 MVP Scope
- Single-caret editing with undo/redo, copy/paste, and selection.
- Virtualized canvas-based text rendering.
- Incremental syntax highlighting for Java, JSON, and JavaScript.
- Gutter with line numbers and marker lane.
- Find/replace and go-to-line.
- Full docking integration via `ContentFactory` and `ContentStateAdapter`.

### 2.2 Non-Goals (Post-MVP)
- Multi-caret editing.
- Block/column selection.
- Language islands (embedded lexers in one document).
- Mini-map.
- Advanced code folding UX.

### 2.3 Module Boundary
- New module: `papiflyfx-docking-code` (new Maven child under root aggregator).
- Existing module `papiflyfx-docking-docks` remains independent and reusable.
- Dependency direction: `papiflyfx-docking-code` depends on `papiflyfx-docking-docks`, not the reverse.
- Integration is done through public contracts only:
  - `ContentFactory`
  - `ContentStateAdapter`
  - `LeafContentData`
  - `DockManager.themeProperty()`

## 3. Architecture

### 3.1 Document Model (`Document`)
`Document` owns text and editor metadata.
- `TextSource`: backing store (`StringBuilder` first, piece-table/rope later if needed).
- `TokenMap`: per-line token slices from lexer.
- `MarkerModel`: line markers (errors, breakpoints, warnings, VCS state).
- `LineIndex`: offset-to-line and line-to-offset mapping for fast navigation.

### 3.2 Rendering and Virtualization (`Viewport`)
`Viewport` is a custom `Region` using one `Canvas`.
- Render only visible lines plus a small prefetch buffer.
- Draw order:
1. background (selection/current-line),
2. tokenized text,
3. overlays (caret/search hits).
- Cache glyph measurements and commonly used draw data.
- Track dirty lines/regions and redraw incrementally.

### 3.3 Syntax Pipeline (`Lexer`)
- Stateful incremental lexing with line exit states.
- On edit, re-lex changed line(s) and continue while exit state changes.
- Tokenization runs off the JavaFX Application Thread.
- UI applies token updates only for current document revision.

## 4. Docking Integration Contracts

### 4.1 Theming
Editor observes `DockManager.themeProperty()` and maps base theme values into editor palette values.

`Theme` in docking is a Java `record` and cannot be extended. Editor theming must use composition, for example:
```java
public record CodeEditorTheme(
    Paint editorBackground,
    Paint editorForeground,
    Paint keywordColor,
    Paint stringColor,
    Paint commentColor,
    Paint numberColor,
    Paint caretColor,
    Paint selectionColor,
    Paint lineNumberColor,
    Paint lineNumberActiveColor
) {}
```

### 4.2 Content Creation and Restore
- `CodeEditorFactory` implements `ContentFactory` with `factoryId = "code-editor"`.
- `CodeEditorStateAdapter` implements `ContentStateAdapter` with `typeKey = "code-editor"`.
- These integration classes are provided by module `papiflyfx-docking-code` and registered by the host application.
- Restore order must be:
1. adapter-based restore (`ContentStateAdapter.restore`),
2. `ContentFactory.create(factoryId)` fallback,
3. placeholder if neither is available.

### 4.3 Persistence Payload
Docking session persistence uses `LeafContentData(typeKey, contentId, version, state)`. For the code editor:
- `typeKey`: `"code-editor"` (stable lookup key).
- `contentId`: stable editor identity across sessions.
- `version`: state schema version from adapter.
- `state`: serialized `EditorStateData`.

Recommended editor state:
```java
public record EditorStateData(
    String filePath,
    int cursorLine,
    int cursorColumn,
    double verticalScrollOffset,
    String languageId,
    List<Integer> foldedLines
) {}
```

## 5. Threading and Lifecycle

### 5.1 Threading Model
- JavaFX scene graph mutations only on JavaFX Application Thread.
- Lexing and file I/O on background workers.
- Edit-triggered lex jobs must support cancellation/debounce.
- Token apply step uses revision checks to discard stale results.

### 5.2 Disposal Contract
When editor leaf is closed:
- stop background workers,
- unbind all listeners/property bindings,
- release caches and large token buffers,
- clear references to avoid memory leaks.

## 6. Failure and Fallback Behavior
- Unknown language: fall back to plain-text lexer.
- Lexer failure: keep previous stable tokens for unaffected lines and log warning.
- Missing/unreadable file on restore: create empty document and retain metadata.
- Missing adapter/factory: show placeholder content; do not fail whole session restore.

## 7. Versioning and Migration
- `ContentStateAdapter.getVersion()` defines current schema version.
- `restore(LeafContentData)` must handle known older versions.
- If migration is not possible, fallback to minimal editor state (empty document + cursor at 0:0).

## 8. Acceptance Criteria
Measured on a baseline machine (4+ cores, 16GB RAM, SSD):
- Open and first render of 100k-line text file in <= 2.0s.
- Typing latency p95 <= 16ms for single-character edits in active viewport.
- Scroll rendering p95 <= 16ms while continuously scrolling large files.
- Editor memory overhead for 100k-line file <= 350MB after warmup.
- Save/restore round-trip preserves cursor, scroll, language, and folded lines.

## 9. Test Strategy
- Unit tests: document edits, line index, lexer incremental invalidation, state migrations.
- JavaFX integration tests: caret/selection behavior, gutter rendering, search navigation.
- Persistence tests: `EditorStateData` and full `LeafContentData` round-trip.
- Performance benchmarks (optional but recommended) for large-file typing/scrolling.

## 10. Implementation Phases
| Phase | Focus | Deliverable |
| --- | --- | --- |
| 0 | Module bootstrap | Create `papiflyfx-docking-code` module and dependency wiring |
| 1 | Foundation | `Document`, `Viewport`, line numbers, basic editing |
| 2 | Syntax | Incremental lexer for Java/JSON/JS, token styles |
| 3 | Docking integration | `ContentFactory`, `ContentStateAdapter`, restore fallback behavior |
| 4 | Hardening | metrics, migration tests, lifecycle cleanup, performance tuning |
| 5 | Post-MVP | multi-caret, language islands, mini-map, advanced folding |
