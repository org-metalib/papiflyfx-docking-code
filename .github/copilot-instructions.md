# Copilot instructions for this repository

## Purpose

Give Copilot short, practical context about this split repository so completions stay accurate, module-scoped, and compatible with the PapiflyFX Docking extraction.

## Repository at a glance

- This repository was extracted from the PapiflyFX Docking monorepo. Keep changes scoped to this repository's modules.
- Multi-module Maven project with the parent POM at the repository root.
- Root artifact: `papiflyfx-docking-code-parent`.
- Project docs, plans, and design notes live under `spec/`.

## Lead roles

- `@feature-dev` owns dockable content modules and feature integrations, including code, tree, media, Hugo, and GitHub content.
- `@ui-ux-designer` owns theme primitives, CSS, shared UI polish, accessibility-sensitive interaction states, and layout ergonomics.
- `@qa-engineer` owns test strategy, headless profiles, regression coverage, and deterministic validation.

## Modules

- `papiflyfx-docking-code` - JavaFX code editor core, docking integration, editor API, document model, rendering, search, gutter markers, state persistence, language SPI, settings, and theme mapping.
- `papiflyfx-docking-code-java` - Java lexer, folding support, defaults, and `LanguageSupportProvider` for language id `java`.
- `papiflyfx-docking-code-javascript` - JavaScript lexer, folding support, defaults, and `LanguageSupportProvider` for language id `javascript` with alias `js`.
- `papiflyfx-docking-code-json` - JSON lexer, folding support, syntax style provider, and language id `json`.
- `papiflyfx-docking-code-yaml` - YAML lexer, folding support, syntax style provider, and language id `yaml` with alias `yml`.
- `papiflyfx-docking-code-markdown` - Markdown lexer, folding support, syntax style provider, and language id `markdown` with alias `md`.

## Local rules

- Do not change Maven `groupId`, module `artifactId`, or Java package names.
- Do not change Java source, public APIs, ServiceLoader descriptors, persistence formats, or theme assets as part of repository split maintenance.
- Preserve artifact coordinates and package names from the monorepo extraction.
- Same-repository PapiflyFX dependencies may use `${project.version}`.
- Cross-repository PapiflyFX dependencies must use `${papiflyfx.version}` or BOM management.
- Do not push split repositories until remotes are created explicitly by the project owner.

## Key packages and contracts

- Main editor API: `org.metalib.papifly.fx.code.api`.
- Document model: `org.metalib.papifly.fx.code.document`.
- Rendering pipeline: `org.metalib.papifly.fx.code.render`.
- Lexer SPI and token model: `org.metalib.papifly.fx.code.lexer`.
- Folding model and providers: `org.metalib.papifly.fx.code.folding`.
- Language registry and bootstrap: `org.metalib.papifly.fx.code.language`.
- Theme and syntax style SPI: `org.metalib.papifly.fx.code.theme`.
- State serialization: `org.metalib.papifly.fx.code.state`.
- Search UI/model: `org.metalib.papifly.fx.code.search`.
- Gutter markers: `org.metalib.papifly.fx.code.gutter`.
- Editor settings integration: `org.metalib.papifly.fx.code.settings`.

Keep these public lexer and fold-provider package paths stable:

- `org.metalib.papifly.fx.code.lexer.JavaLexer`
- `org.metalib.papifly.fx.code.lexer.JavaScriptLexer`
- `org.metalib.papifly.fx.code.lexer.JsonLexer`
- `org.metalib.papifly.fx.code.lexer.YamlLexer`
- `org.metalib.papifly.fx.code.lexer.MarkdownLexer`
- `org.metalib.papifly.fx.code.folding.JavaFoldProvider`
- `org.metalib.papifly.fx.code.folding.JavaScriptFoldProvider`
- `org.metalib.papifly.fx.code.folding.JsonFoldProvider`
- `org.metalib.papifly.fx.code.folding.YamlFoldProvider`
- `org.metalib.papifly.fx.code.folding.MarkdownFoldProvider`

## ServiceLoader descriptors

Language packs are discovered through:

- `META-INF/services/org.metalib.papifly.fx.code.language.LanguageSupportProvider`

Language packs with semantic colors also register:

- `META-INF/services/org.metalib.papifly.fx.code.theme.SyntaxStyleProvider`

The core editor module also exposes docking/settings integration through:

- `META-INF/services/org.metalib.papifly.fx.docking.api.ContentStateAdapter`
- `META-INF/services/org.metalib.papifly.fx.settings.api.SettingsCategory`

When adding or changing providers, keep the descriptor, provider tests, and `ServiceLoader.load(...)` coverage in sync.

## Development guidance

- Prefer existing editor abstractions before adding new ones: `CodeEditor`, `Document`, `Viewport`, `LanguageSupportRegistry`, `SyntaxStyleRegistry`, `FoldMap`, `EditorStateData`, and related controller classes.
- Keep rendering changes localized to the render pipeline and add focused tests for invalidation, wrapping, selection, or viewport behavior.
- Keep language-pack changes inside the relevant language module unless the core SPI must change.
- If the core language SPI changes, check all language packs and their ServiceLoader tests.
- If state changes, inspect `EditorStateCodec`, `EditorStateData`, `CodeEditorStateAdapter`, and session persistence tests.
- If theme or syntax colors change, inspect `CodeEditorTheme`, `CodeEditorThemeMapper`, `SyntaxStyleProvider`, `SyntaxStyleRegistry`, and language-specific style providers.
- If settings change, inspect `LanguageEditorSettingsResolver`, `EditorSettingsSupport`, and the setting keys documented in `papiflyfx-docking-code/README.md`.
- Benchmark tests are excluded by default with `surefire.excludedGroups=benchmark`; do not accidentally include them in normal test commands.

## Frequently used commands

Validate the whole repository with the preferred split-local setup:

```bash
./mvnw -Dmaven.repo.local=$HOME/github/papiflyfx/.m2-split -Dtestfx.headless=true clean verify
```

Build the whole repository:

```bash
./mvnw -Dmaven.repo.local=$HOME/github/papiflyfx/.m2-split -Dtestfx.headless=true clean package
```

Run regular tests:

```bash
./mvnw -Dmaven.repo.local=$HOME/github/papiflyfx/.m2-split -Dtestfx.headless=true test
```

Run the core editor module and dependencies:

```bash
./mvnw -Dmaven.repo.local=$HOME/github/papiflyfx/.m2-split -pl papiflyfx-docking-code -am -Dtestfx.headless=true test
```

Run one language pack and dependencies:

```bash
./mvnw -Dmaven.repo.local=$HOME/github/papiflyfx/.m2-split -pl papiflyfx-docking-code-json -am -Dtestfx.headless=true test
```

Run a specific test class:

```bash
./mvnw -Dmaven.repo.local=$HOME/github/papiflyfx/.m2-split -pl papiflyfx-docking-code -am -Dtest=FullyQualifiedTestName -Dtestfx.headless=true test
```

Run benchmarks only when explicitly needed:

```bash
./mvnw -Dmaven.repo.local=$HOME/github/papiflyfx/.m2-split -pl papiflyfx-docking-code -am -Dtestfx.headless=true -Dgroups=benchmark -Dsurefire.excludedGroups= test
```

CI package command mirrors `.github/workflows/test.yaml`:

```bash
./mvnw -B -U --errors -Dsurefire.useFile=false -Djava.awt.headless=true -Dtestfx.headless=true package
```

## Java and JavaFX notes

- CI uses Zulu JDK FX `25.0.1`.
- JavaFX-heavy tests should run with `-Dtestfx.headless=true`.
- Confirm local toolchain details with `java -version` and `./mvnw -v` when diagnosing environment failures.
- Check `target/surefire-reports` after Maven test runs.

## Useful search keywords for Copilot

- Core editor: `CodeEditor`, `CodeEditorFactory`, `CodeEditorStateAdapter`, `EditorStateData`, `EditorStateCodec`, `Document`, `Viewport`, `RenderPass`, `TextPass`, `SelectionPass`, `SearchController`, `MarkerModel`.
- Language SPI: `LanguageSupport`, `LanguageSupportProvider`, `LanguageSupportRegistry`, `BootstrapOptions`, `ConflictPolicy`, `SyntaxStyleProvider`, `SyntaxStyleRegistry`, `LanguageEditorDefaults`.
- Lexing and folding: `Lexer`, `Token`, `TokenType`, `IncrementalLexerPipeline`, `FoldProvider`, `FoldMap`, `FoldRegion`, `IncrementalFoldingPipeline`, `VisibleLineMap`.
- Language modules: `JavaLexer`, `JavaScriptLexer`, `JsonLexer`, `YamlLexer`, `MarkdownLexer`, `JsonSyntaxStyleProvider`, `YamlSyntaxStyleProvider`, `MarkdownSyntaxStyleProvider`.

## Where to look first

- Root `pom.xml` for module structure.
- `README.md` for repository-level build guidance.
- `papiflyfx-docking-code/README.md` for editor behavior, language SPI, persistence, UI standards, and benchmark notes.
- Each `papiflyfx-docking-code-*` module README for language-pack scope and discovery details.
- `spec/papiflyfx-docking-code/` for design notes and implementation plans.

## Common task checklist

- For editor behavior, inspect the core module first and add focused tests in `papiflyfx-docking-code/src/test/java`.
- For a language-pack change, update the matching language module, provider tests, lexer tests, folding tests, and style-provider tests when applicable.
- For ServiceLoader changes, verify both the descriptor file and a test that loads providers through `ServiceLoader`.
- For UI/theme behavior, preserve shared docking UI standards and runtime theme mapping through `Theme` and `CodeEditorThemeMapper`.
- Before finalizing, run the narrowest relevant Maven command, then the full split-local verification when the blast radius justifies it.
