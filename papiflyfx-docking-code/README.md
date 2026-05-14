# papiflyfx-docking-code

A dockable JavaFX code editor content type for the PapiflyFX docking framework. The editor is built with programmatic JavaFX and uses scoped overlay CSS layered on the shared UI standards in `papiflyfx-docking-api`.

## Features

- Canvas-based virtualized text rendering for large files (100k+ lines)
- Single-caret editing with undo/redo, copy/paste, and selection
- Incremental syntax highlighting for Java, JavaScript, and plain text, with JSON, Markdown, and YAML available as optional language packs
- Line number gutter with marker lane (errors, warnings, breakpoints, bookmarks)
- Find/replace overlay with regex support and go-to-line navigation
- Shared popup/chip styling for search and go-to-line overlays
- Full docking integration via `ContentFactory` and `ContentStateAdapter`
- Runtime theme switching through composition with docking `Theme`

## Maven Dependency

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-code</artifactId>
    <version>${papiflyfx.version}</version>
</dependency>
```

Add optional language packs when those languages should be highlighted and folded:

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-code-json</artifactId>
    <version>${papiflyfx.version}</version>
</dependency>
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-code-yaml</artifactId>
    <version>${papiflyfx.version}</version>
</dependency>
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-code-markdown</artifactId>
    <version>${papiflyfx.version}</version>
</dependency>
```

## Quick Start

### 1. Register the factory and adapter

```java
DockManager dockManager = new DockManager();

// Register content state adapter for session persistence
ContentStateRegistry registry = new ContentStateRegistry();
registry.register(new CodeEditorStateAdapter());
dockManager.setContentStateRegistry(registry);

// Register content factory for leaf creation
dockManager.setContentFactory(new CodeEditorFactory());
```

Alternatively, use ServiceLoader auto-discovery (the module ships a `META-INF/services` descriptor):

```java
ContentStateRegistry registry = ContentStateRegistry.fromServiceLoader();
dockManager.setContentStateRegistry(registry);
```

### 2. Create an editor leaf

```java
CodeEditor editor = new CodeEditor();
editor.setFilePath("/path/to/file.java");
editor.setText(Files.readString(Path.of("/path/to/file.java")));
editor.setLanguageId("java");

DockLeaf leaf = dockManager.createLeaf("file.java", editor);
leaf.setContentFactoryId(CodeEditorFactory.FACTORY_ID);
```

### 3. Bind to docking theme

```java
editor.bindThemeProperty(dockManager.themeProperty());
```

Theme changes propagate automatically to the viewport, gutter, and search overlay.

## UI Standards

The code module follows the shared UI standardization model introduced in `papiflyfx-docking-api`.

- `Theme` remains the only runtime theme source.
- Search and go-to-line overlays load `ui-common.css` through `UiStyleSupport`.
- Shared metrics come from `UiMetrics`, including the 4px grid and compact control heights.
- Search toggles use the shared `UiChipToggle` base styling.
- Overlay surfaces consume the shared `-pf-ui-*` CSS variable set instead of a separate module-only token vocabulary.

## Session Persistence Flow

1. **Save**: `DockManager.captureSession()` calls `CodeEditorStateAdapter.saveState()` on each editor leaf, producing a `LeafContentData` with cursor, scroll, language, and file path.
2. **Restore**: `DockManager.restoreSession()` calls `CodeEditorStateAdapter.restore()`, which creates a new `CodeEditor`, rehydrates document text from `filePath` (falling back to empty document if unreadable), and applies saved state.
3. **Fallback chain**: adapter restore → factory create → placeholder content. Session structure is always preserved.

## Editor API Highlights

| Method | Description |
|--------|-------------|
| `setText(String)` | Sets document text content |
| `getText()` | Returns document text |
| `setLanguageId(String)` | Sets syntax language (`plain-text`; language packs add `java`, `javascript`, `json`, `markdown`, `yaml`) |
| `bindThemeProperty(ObjectProperty<Theme>)` | Binds to docking theme for live updates |
| `setEditorTheme(CodeEditorTheme)` | Sets editor palette directly |
| `captureState()` | Captures current state as `EditorStateData` |
| `applyState(EditorStateData)` | Applies saved state (cursor, scroll, language) |
| `getMarkerModel()` | Access marker model for line annotations |
| `openSearch()` | Opens the search/replace overlay |
| `goToLine(int)` | Navigates to a 1-based line number |
| `dispose()` | Releases listeners, stops workers, cleans up resources |

## Language Packs

The core editor owns the language SPI:

- `LanguageSupport`
- `LanguageSupportProvider`
- `LanguageSupportRegistry`
- `BootstrapOptions`
- `ConflictPolicy`
- `SyntaxStyleProvider`
- `SyntaxStyleRegistry`
- `LanguageEditorDefaults`

`LanguageSupportRegistry.bootstrap(BootstrapOptions.defaults())` first registers the core plain-text fallback and then loads every `LanguageSupportProvider` visible through `ServiceLoader`.

To add a language pack:

1. Depend on `papiflyfx-docking-code`.
2. Implement a lexer, a fold provider, language defaults, and a `LanguageSupportProvider`.
3. Register the provider in `META-INF/services/org.metalib.papifly.fx.code.language.LanguageSupportProvider`.
4. If the language needs semantic colors, implement `SyntaxStyleProvider` and register it in `META-INF/services/org.metalib.papifly.fx.code.theme.SyntaxStyleProvider`.
5. Add tests that call both the provider directly and `ServiceLoader.load(...)`.

`Token` can carry an optional semantic style scope. `TextPass` resolves colors in this order: contributed style scope, `TokenType` fallback, then editor foreground. `TokenType`, `FoldKind`, and compatibility `CodeEditorTheme` accessors remain in core; language-specific colors should be contributed through `SyntaxStyleProvider`.

Per-language editor settings resolve through `papiflyfx-docking-settings-api` with these keys:

- `editor.language.default.indentWidth`
- `editor.language.default.insertSpaces`
- `editor.language.default.ensureTrailingNewline`
- `editor.language.default.trimTrailingWhitespace`
- `editor.language.<id>.indentWidth`
- `editor.language.<id>.insertSpaces`
- `editor.language.<id>.ensureTrailingNewline`
- `editor.language.<id>.trimTrailingWhitespace`

## Supported Languages

| Language | ID | Module | Highlights |
|----------|-----|--------|------------|
| Plain Text | `plain-text` | `papiflyfx-docking-code` | No highlighting (default fallback) |
| Java | `java` | `papiflyfx-docking-code-java` | Keywords, strings, comments, numbers, annotations |
| JavaScript | `javascript` | `papiflyfx-docking-code-javascript` | Keywords, strings, template literals, comments, numbers |
| JSON | `json` | `papiflyfx-docking-code-json` | Keys via `json.key`, strings, numbers, booleans, null |
| Markdown | `markdown` | `papiflyfx-docking-code-markdown` | `markdown.headline`, `markdown.list-item`, `markdown.code-block` |
| YAML | `yaml` | `papiflyfx-docking-code-yaml` | `yaml.key`, anchors, aliases, tags, scalar tokens, block folding |

## Acceptance Metrics

Measured on macOS (Apple Silicon), headless mode, 100k-line synthetic Java file:

| Metric | Threshold | Measured | Status |
|--------|-----------|----------|--------|
| Large file open + first render | ≤ 2000ms | 218ms | PASS |
| Typing latency (p95, single char) | ≤ 16ms | 3.27ms | PASS |
| Scroll rendering (p95) | ≤ 16ms | 0.18ms | PASS |
| Memory overhead (100k lines) | ≤ 350MB | 63MB | PASS |

## Running Tests

```bash
# Regular tests (excludes benchmarks)
./mvnw -pl papiflyfx-docking-code,papiflyfx-docking-code-java,papiflyfx-docking-code-javascript,papiflyfx-docking-code-json,papiflyfx-docking-code-yaml,papiflyfx-docking-code-markdown,papiflyfx-docking-docks -am -Dtestfx.headless=true test

# Benchmarks only
./mvnw -pl papiflyfx-docking-code -am -Dtestfx.headless=true -Dgroups=benchmark -Dsurefire.excludedGroups= test
```

## Further Reading

- [UI Standards Research](../spec/ui-standards/research.md)
- [UI Standards Plan](../spec/ui-standards/plan.md)
