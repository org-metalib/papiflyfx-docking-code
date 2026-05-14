# PapiflyFX Code Implementation Plan

Scope: implement the **MVP** described in `spec/papiflyfx-docking-code/spec.md` in a **new standalone module** `papiflyfx-docking-code`, integrated with `papiflyfx-docking-docks` through public extension APIs.

## 1. Delivery Goals
- Create and wire new Maven module `papiflyfx-docking-code`.
- Deliver a usable code editor content type (`factoryId = "code-editor"`) for docking leaves.
- Keep implementation pure programmatic JavaFX (no FXML/CSS).
- Meet MVP behavior: editing, syntax highlighting, gutter, search/go-to-line, state save/restore.
- Establish a stable internal architecture that can later support post-MVP features.

## 2. Assumptions and Constraints
- Implementation lives in module `papiflyfx-docking-code`.
- `papiflyfx-docking-docks` must not gain a compile dependency on `papiflyfx-docking-code`.
- Base APIs already available and must be used:
  - `ContentFactory`
  - `ContentStateAdapter`
  - `LeafContentData(typeKey, contentId, version, state)`
  - `DockManager.themeProperty()`
- Java 25 and current project Maven setup remain unchanged.
- Performance targets are validated on baseline hardware defined in `spec.md`.

## 3. Proposed Package Layout
Under `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code`:

- `api/`
  - `CodeEditor` (public Node/control entry point)
  - `CodeEditorFactory` (`ContentFactory` implementation)
  - `CodeEditorStateAdapter` (`ContentStateAdapter` implementation)
- `document/`
  - `Document`
  - `TextSource`
  - `LineIndex`
  - edit command types (`InsertEdit`, `DeleteEdit`, etc.)
- `lexer/`
  - `Lexer`
  - `LexState`
  - `Token`
  - `TokenRule`
  - language defs (`JavaLanguage`, `JsonLanguage`, `JavaScriptLanguage`)
- `render/`
  - `Viewport`
  - `RenderLine`
  - `GlyphCache`
- `gutter/`
  - `GutterView`
  - `MarkerModel`
- `search/`
  - `SearchModel`
  - `SearchController`
- `theme/`
  - `CodeEditorTheme`
  - `CodeEditorThemeMapper`
- `state/`
  - `EditorStateData`
  - `EditorStateCodec` (map <-> DTO helpers)

Tests under `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code`.

## 4. Phase Plan

## Phase 0: Bootstrap and Integration Skeleton
Goal: create the new module and a minimal editor content skeleton.

Tasks:
- Root aggregator setup:
  - add `<module>papiflyfx-docking-code</module>` to root `pom.xml`.
- Create `papiflyfx-docking-code/pom.xml`:
  - inherit root parent,
  - set `artifactId` to `papiflyfx-docking-code`,
  - depend on `papiflyfx-docking-docks`,
  - include test dependencies aligned with parent dependencyManagement.
- Add package skeleton and placeholder `CodeEditor` node.
- Implement `CodeEditorFactory` (`factoryId = "code-editor"`).
- Implement empty `CodeEditorStateAdapter` with version `1`.
- Register adapter path (manual registration first; ServiceLoader optional).

Exit criteria:
- `mvn compile -pl papiflyfx-docking-code -am` succeeds.
- A leaf restored with `contentFactoryId = "code-editor"` creates a visible editor placeholder.
- Save/restore path passes through adapter without errors.

## Phase 1: Document Core and Editing
Goal: deterministic text model and editing primitives.

Tasks:
- Implement `TextSource` backed by `StringBuilder`.
- Implement `Document` API:
  - read line/offset ranges,
  - insert/delete/replace,
  - cursor movement helpers.
- Implement `LineIndex` for line/offset mapping.
- Implement undo/redo stack with command objects.

Exit criteria:
- Unit tests cover inserts/deletes across line boundaries.
- Undo/redo is deterministic for mixed operations.

## Phase 2: Viewport and Rendering
Goal: virtualized canvas rendering for large files.

Tasks:
- Implement `Viewport` as single-`Canvas` renderer.
- Render only visible lines + buffer.
- Implement draw layers: background, text, overlay.
- Add dirty-line tracking and partial redraws.
- Add basic caret rendering and selection highlight.

Exit criteria:
- Scrolling does not instantiate per-line JavaFX nodes.
- Integration tests verify visible-line correctness on scroll and resize.

## Phase 3: Incremental Lexer Pipeline
Goal: async incremental tokenization with revision safety.

Tasks:
- Implement token model and per-line token storage.
- Implement stateful incremental lexing by line exit state.
- Add background lex executor with cancellation/debounce.
- Apply tokens on FX thread only when revision matches.
- Implement Java, JSON, JavaScript language definitions.

Exit criteria:
- Unit tests validate re-lex range propagation rules.
- Typing updates syntax colors without UI thread blocking.

## Phase 4: Gutter, Markers, and Navigation
Goal: core editor productivity features in MVP scope.

Tasks:
- Implement line number gutter with dynamic width.
- Add marker lane and `MarkerModel` integration.
- Implement search/replace model and UI overlay.
- Implement go-to-line action.

Exit criteria:
- Integration tests verify line number alignment and search navigation.
- Search supports plain text and regex modes.

## Phase 5: Theme Composition and Mapping
Goal: map docking `Theme` to editor palette through composition.

Tasks:
- Implement `CodeEditorTheme` and mapper from base `Theme`.
- Listen to `DockManager.themeProperty()` changes and refresh palette.
- Ensure rendering, gutter, and overlay all use mapped theme values.

Exit criteria:
- Theme switches update editor visuals at runtime.
- No inheritance from docking `Theme` record.

## Phase 6: Persistence and Restore Contract
Goal: stable save/restore of editor content state through docking session data.

Tasks:
- Define `EditorStateData` v1 fields:
  - `filePath`,
  - `cursorLine`, `cursorColumn`,
  - `verticalScrollOffset`,
  - `languageId`,
  - `foldedLines` (empty in MVP if folding UI is deferred).
- Implement map codec for `LeafContentData.state`.
- Implement `CodeEditorStateAdapter.saveState(...)` and `restore(...)`.
- Enforce restore order behavior (adapter -> factory -> placeholder compatibility).

Exit criteria:
- Persistence round-trip tests preserve cursor/scroll/language.
- Adapter versioning and migration hooks are in place for future versions.

## Phase 7: Failure Handling and Lifecycle Cleanup
Goal: robust behavior under partial failure and clean disposal.

Tasks:
- Add fallback for unknown language to plain text lexer.
- Handle lexer exceptions with safe token fallback and logging.
- Handle unreadable/missing files by creating empty document + metadata.
- Implement `dispose()` lifecycle on editor components:
  - stop workers,
  - unbind listeners,
  - release caches.

Exit criteria:
- Closing leaves does not leave active background tasks.
- Failure paths do not break docking session restore.

## Phase 8: Hardening, Benchmarks, and Docs
Goal: verify acceptance criteria and document operation.

Tasks:
- Add performance benchmark harness for large file open/type/scroll.
- Add FX tests for end-to-end docking integration with editor state.
- Document usage examples for:
  - `CodeEditorFactory`,
  - `ContentStateRegistry.register(...)`,
  - session persistence flow.
- Add module-level README and quickstart for host apps to integrate `papiflyfx-docking-code`.

Exit criteria:
- Acceptance metrics in `spec.md` are measured and reported.
- Documentation is sufficient for embedding the editor in demo apps.

## 5. Test Plan by Layer

Unit tests:
- `document`: line index mapping, edit operations, undo/redo.
- `lexer`: tokenization correctness, incremental invalidation.
- `state`: DTO/map round-trip, version migration behavior.

Integration tests (JavaFX/TestFX):
- viewport rendering and scroll behavior,
- caret/selection movement,
- gutter synchronization,
- search/go-to-line,
- theme switching.

Docking integration tests:
- `CodeEditorFactory` creation from `contentFactoryId`,
- `CodeEditorStateAdapter` save/restore with `LeafContentData`,
- full `DockManager` session capture/restore round-trip from an app using both modules.

## 6. Milestone Checklist
- M1: Factory + placeholder + adapter skeleton.
- M2: Editable document with undo/redo.
- M3: Virtualized rendering + caret/selection.
- M4: Incremental lexer for 3 languages.
- M5: Gutter + search/go-to-line.
- M6: State persistence v1 complete.
- M7: Failure handling + disposal complete.
- M8: Performance validation + docs complete.

## 7. Risks and Mitigations
- Risk: UI stutter during lexing.
  - Mitigation: strict background tokenization + revision checks + debounce.
- Risk: memory growth for large token maps.
  - Mitigation: compact token storage and cache limits with explicit release.
- Risk: restore incompatibility across state versions.
  - Mitigation: adapter version gate + migration handlers + safe fallback state.
- Risk: flaky FX tests.
  - Mitigation: deterministic test fixtures, headless profile, explicit FX synchronization helpers.

## 8. Validation Commands
- Compile:
  - `mvn compile -pl papiflyfx-docking-code -am`
- Module tests:
  - `mvn test -pl papiflyfx-docking-code`
- Headless UI tests:
  - `mvn -Dtestfx.headless=true test -pl papiflyfx-docking-code`

## 9. Definition of Done (MVP)
- All MVP features from `spec.md` are implemented.
- Persistence works via `ContentStateAdapter` and `LeafContentData`.
- Runtime theming updates work through composition (`CodeEditorTheme` mapping).
- Required tests pass locally and in headless mode.
- Measured performance is within defined acceptance thresholds or documented with gaps and follow-up actions.
