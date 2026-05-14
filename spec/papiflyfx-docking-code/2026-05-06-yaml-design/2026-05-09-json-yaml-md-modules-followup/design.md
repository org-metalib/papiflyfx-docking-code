# Design - Language Pack Follow-ups

Date: 2026-05-09
Lead: `@spec-steward`
Implementation owner: `@feature-dev`
Required reviewers: `@core-architect`, `@ops-engineer`, `@ui-ux-designer`, `@qa-engineer`
Linked prompt: [`prompt.md`](prompt.md)
Source spec: [`../2026-05-09-json-yaml-md-modules/design.md`](../2026-05-09-json-yaml-md-modules/design.md)

## 1. Summary

The previous module split made JSON, YAML, and Markdown optional
language-pack modules but kept three concerns in the core editor:
language-specific syntax colors, Java / JavaScript implementations, and
global-only editor settings. This follow-up completes the language-pack
direction without breaking existing users.

The design adds:

1. A semantic syntax style scope SPI in `papiflyfx-docking-code`.
2. New `papiflyfx-docking-code-java` and
   `papiflyfx-docking-code-javascript` language-pack modules.
3. A settings resolver for per-language editor preferences backed by
   `papiflyfx-docking-settings-api`.

Compatibility is explicit. Existing `TokenType` constants and
`CodeEditorTheme` fields can remain as deprecated aliases while language
modules migrate to semantic style scopes. The core no longer needs new enum
values or record fields for every language-specific color.

## 2. Current State

`papiflyfx-docking-code` currently owns:

- `Token`, whose public record shape is `Token(int startColumn, int length,
  TokenType type)`.
- `TokenType`, including generic categories plus language-specific constants
  such as `JSON_KEY`, `YAML_KEY`, `YAML_ANCHOR`, `YAML_ALIAS`, `YAML_TAG`,
  `HEADLINE`, `LIST_ITEM`, and `CODE_BLOCK`.
- `CodeEditorTheme`, a fixed Java record with fields such as
  `jsonKeyColor`, `yamlKeyColor`, `yamlAnchorColor`, `yamlTagColor`,
  `headlineColor`, `listItemColor`, and `codeBlockColor`.
- `TextPass#tokenColor(...)`, which switches on `TokenType` and reads fixed
  fields from `CodeEditorTheme`.
- `LanguageSupport`, which already exposes `customTokenScopes` metadata, but
  no renderer consumes those scopes and no default color contribution model
  exists.
- Java and JavaScript lexers, fold providers, tests, and registrations in
  `BuiltInLanguageSupportProvider`.
- `EditorCategory` and `EditorSettingsSupport`, which read global settings
  such as `editor.wordWrap`, `editor.tabSize`, `editor.fontSize`, and
  `editor.autoDetectLanguage`.

JSON, YAML, and Markdown now live in language-pack modules. They still emit
language-specific `TokenType` values whose colors are hardwired in core.

## 3. Goals and Non-goals

### Goals

1. Let language modules declare syntax style scopes and default dark/light
   colors without modifying `CodeEditorTheme` or `TextPass`.
2. Let lexers emit an optional semantic style scope while preserving the
   existing token construction API.
3. Move Java and JavaScript into dedicated language-pack modules using the
   JSON / YAML / Markdown template.
4. Resolve per-language settings from settings storage with fallback from
   language-specific keys to global editor defaults.
5. Keep samples, BOM, docs, and ServiceLoader tests aligned with the new
   topology.

### Non-goals

1. Dynamic fold-kind contribution. `FoldKind` remains a core enum.
2. Removing public enum constants or theme accessors in this iteration.
3. Adding formatters, linters, schema validation, or AST services.
4. Changing settings persistence implementations.

## 4. Target Module Topology

```text
papiflyfx-docking-code                 # core editor, SPI, plain text fallback
papiflyfx-docking-code-java            # Java language pack
papiflyfx-docking-code-javascript      # JavaScript language pack
papiflyfx-docking-code-json            # JSON language pack
papiflyfx-docking-code-markdown        # Markdown language pack
papiflyfx-docking-code-yaml            # YAML language pack
```

`papiflyfx-docking-code` keeps the public language SPI and editor runtime.
Only plain text stays built in. All feature languages register through
`META-INF/services/org.metalib.papifly.fx.code.language.LanguageSupportProvider`.

## 5. Syntax Style Contribution SPI

### 5.1 Scope identifiers

Language modules use stable, lowercase, dotted scope ids:

| Language | Scope id examples |
| -------- | ----------------- |
| JSON | `json.key` |
| YAML | `yaml.key`, `yaml.anchor`, `yaml.alias`, `yaml.tag` |
| Markdown | `markdown.headline`, `markdown.list-item`, `markdown.code-block` |
| Java | optional future scopes such as `java.annotation` |
| JavaScript | optional future scopes such as `javascript.template` |

The first migration only needs scopes for colors that are currently
language-specific in `CodeEditorTheme`.

### 5.2 New model

Add the following public API in `papiflyfx-docking-code`, package
`org.metalib.papifly.fx.code.theme` unless implementation review chooses a
more precise package:

```java
public record SyntaxStyleScope(
    String id,
    String displayName,
    TokenType fallbackTokenType,
    Paint darkDefault,
    Paint lightDefault
) {}

public interface SyntaxStyleProvider {
    Collection<SyntaxStyleScope> getSyntaxStyleScopes();
}
```

Add `SyntaxStyleRegistry` in core:

- Bootstraps built-in generic scopes, if any.
- Loads `SyntaxStyleProvider` through `ServiceLoader`.
- Rejects duplicate scope ids by default and records diagnostics, mirroring
  `LanguageSupportRegistry` behavior.
- Exposes immutable scope snapshots and color lookup helpers.

Each language-pack module that owns custom style scopes registers a
`SyntaxStyleProvider` descriptor:

```text
META-INF/services/org.metalib.papifly.fx.code.theme.SyntaxStyleProvider
```

### 5.3 Token shape

Extend `Token` with an optional semantic style scope while preserving the
current constructor:

```java
public record Token(int startColumn, int length, TokenType type, String styleScope) {
    public Token(int startColumn, int length, TokenType type) {
        this(startColumn, length, type, null);
    }
}
```

Validation rules:

- `type` stays required.
- `styleScope` is normalized to blank/null when absent.
- A token can carry both a generic fallback `TokenType` and a style scope.
  The scope wins if the active theme resolves it.

### 5.4 Theme shape

Add a dynamic syntax-scope color map to `CodeEditorTheme`:

```java
Map<String, Paint> syntaxScopeColors
```

Because `CodeEditorTheme` is a public record, implementation must provide
compatibility constructors or factory methods so current callers do not need
to update immediately. Existing language-specific fields may remain as
deprecated compatibility fields. They should be populated from the same
defaults as the new scope map so old token output still looks identical.

`CodeEditorThemeMapper` resolves the active docking `Theme`, starts from the
existing dark/light editor palette, then merges `SyntaxStyleRegistry`
defaults for every loaded scope. This means a language module on the
classpath can provide default syntax colors without a core code change.

### 5.5 Rendering order

`TextPass` resolves color in this order:

1. If `token.styleScope()` is present and `CodeEditorTheme` has a color for
   that scope, use it.
2. Otherwise fall back to the existing `TokenType` switch.
3. Otherwise use `editorForeground`.

This order lets migrated language modules use scopes while old callers that
emit `TokenType.JSON_KEY` or `TokenType.HEADLINE` continue to render
correctly.

### 5.6 Migration of existing language colors

JSON, YAML, and Markdown modules add style providers and update their lexers
to emit style scopes with generic fallback token types:

| Old token type | New fallback type | Style scope |
| -------------- | ----------------- | ----------- |
| `JSON_KEY` | `STRING` | `json.key` |
| `YAML_KEY` | `IDENTIFIER` or `STRING` by context | `yaml.key` |
| `YAML_ANCHOR` | `IDENTIFIER` | `yaml.anchor` |
| `YAML_ALIAS` | `IDENTIFIER` | `yaml.alias` |
| `YAML_TAG` | `IDENTIFIER` | `yaml.tag` |
| `HEADLINE` | `TEXT` | `markdown.headline` |
| `LIST_ITEM` | `TEXT` | `markdown.list-item` |
| `CODE_BLOCK` | `TEXT` | `markdown.code-block` |

The old enum constants should be marked `@Deprecated(forRemoval = false)`
only after migrated tests prove no shipped lexer emits them.

## 6. Java and JavaScript Module Split

Create two new modules:

- `papiflyfx-docking-code-java`
- `papiflyfx-docking-code-javascript`

Each module follows the existing language-pack shape:

```text
papiflyfx-docking-code-<lang>/
+-- pom.xml
+-- README.md
+-- src/
    +-- main/java/org/metalib/papifly/fx/code/lexer/<Lang>Lexer.java
    +-- main/java/org/metalib/papifly/fx/code/folding/<Lang>FoldProvider.java
    +-- main/java/org/metalib/papifly/fx/code/folding/<Lang>LanguageSupportProvider.java
    +-- main/resources/META-INF/services/org.metalib.papifly.fx.code.language.LanguageSupportProvider
    +-- test/java/...
```

Package paths stay unchanged to preserve direct-import compatibility. After
the split, `BuiltInLanguageSupportProvider` registers only `plain-text`.
Samples, BOM, README files, and discovery tests gain explicit dependencies on
the Java and JavaScript modules.

## 7. Per-language Settings

### 7.1 Settings model

Add a small settings model in `papiflyfx-docking-code`, for example:

```java
public record LanguageEditorSettings(
    String languageId,
    int indentWidth,
    boolean insertSpaces,
    boolean ensureTrailingNewline,
    boolean trimTrailingWhitespace
) {}
```

Add a resolver that depends only on `papiflyfx-docking-settings-api`:

```java
public final class LanguageEditorSettingsResolver {
    LanguageEditorSettings resolve(String languageId, SettingsStorage storage);
}
```

Resolution order:

1. `editor.language.<languageId>.<setting>`
2. `editor.language.default.<setting>`
3. hardcoded safe default

Initial keys:

| Key | Type | Default |
| --- | ---- | ------- |
| `editor.language.default.indentWidth` | integer | `4` |
| `editor.language.default.insertSpaces` | boolean | `true` |
| `editor.language.default.ensureTrailingNewline` | boolean | `true` |
| `editor.language.default.trimTrailingWhitespace` | boolean | `false` |
| `editor.language.<id>.indentWidth` | integer | default key value |
| `editor.language.<id>.insertSpaces` | boolean | default key value |
| `editor.language.<id>.ensureTrailingNewline` | boolean | default key value |
| `editor.language.<id>.trimTrailingWhitespace` | boolean | default key value |

### 7.2 Editor integration

Add observable editor properties only where behavior exists:

- `indentWidth`
- `insertSpaces`
- `ensureTrailingNewline`
- `trimTrailingWhitespace`

At minimum, settings application updates the properties and tests assert the
resolver chooses language-specific values over defaults. If edit behavior is
added in the same implementation, `Tab` insertion and newline indentation
should use `indentWidth` / `insertSpaces`; save hooks can enforce trailing
newline and trailing whitespace policy only where the editor owns the save
operation. If the editor still has no save pipeline, document save-time
policy as resolved but not automatically applied.

### 7.3 Settings UI

`EditorCategory` should expose global defaults first. A language-specific
section can be generated from `LanguageSupportRegistry.registeredLanguages()`
so only installed language packs appear. Use existing settings controls and
`-pf-ui-*` tokens; do not introduce a new visual style system.

## 8. Testing Strategy

1. Core token tests cover the new token constructor, scope validation, and
   fallback compatibility constructor.
2. `SyntaxStyleRegistryTest` covers ServiceLoader discovery, duplicate
   scope diagnostics, and default color lookup.
3. `CodeEditorThemeMapperTest` covers dynamic scope color population for
   dark and light themes.
4. `TextPass` or render-level tests prove scope color wins over fallback
   `TokenType`.
5. JSON, YAML, and Markdown lexer tests assert language-specific tokens emit
   expected style scopes.
6. Java and JavaScript moved tests retain their existing counts and add
   provider/discovery tests.
7. Settings resolver tests cover default fallback, language-specific override,
   invalid indent fallback, and missing storage behavior.
8. Samples discovery test asserts all shipped language ids resolve on the
   full application classpath.

## 9. Documentation Updates

Update:

- `papiflyfx-docking-code/README.md` with syntax style scopes, Java /
  JavaScript language packs, and per-language settings keys.
- Each language-pack `README.md` with its language id, file extensions,
  style scopes, settings defaults, and ServiceLoader descriptors.
- Root `README.md`, `CLAUDE.md`, and `spec/papiflyfx-docking-code/README.md`
  with the completed module list.
- `spec/papiflyfx-docking-code-lang-plugin/README.md` with guidance that new
  languages should provide both `LanguageSupportProvider` and, when custom
  colors are needed, `SyntaxStyleProvider`.

## 10. Risks and Mitigations

| Risk | Impact | Mitigation |
| ---- | ------ | ---------- |
| Public `CodeEditorTheme` record change breaks callers. | High | Provide compatibility constructors or factories; keep old fields until a major release. |
| ServiceLoader descriptor missing for style provider. | Medium | Per-module discovery tests and samples classpath test. |
| Dynamic scope colors diverge from current palettes. | Medium | Assert exact dark/light defaults in theme mapper tests and require UI review. |
| Java / JavaScript moves break direct imports. | Medium | Preserve package paths and document dependency migration. |
| Settings values resolve but do not affect behavior. | Medium | Define which properties are behavior-backed in this iteration and test property application. |
| Module split changes BOM/sample classpaths. | Medium | Ops review, BOM audit, samples smoke tests. |

## 11. Open Questions for Reviewers

1. `@core-architect`: should `SyntaxStyleProvider` live under
   `org.metalib.papifly.fx.code.theme` or a new `...code.style` package?
2. `@core-architect`: should old language-specific `TokenType` constants be
   deprecated immediately after migration or left undecorated for one release?
3. `@ui-ux-designer`: are the existing JSON/YAML/Markdown default colors the
   required baseline for contributed scopes, or should this be the moment for
   a palette review?
4. `@ops-engineer`: should Java and JavaScript modules be optional standalone
   artifacts only, or should a convenience aggregate be considered later?
5. `@qa-engineer`: should trailing-newline policy be tested only as a
   resolver property until a save pipeline exists, or should this task add a
   save-time hook?
