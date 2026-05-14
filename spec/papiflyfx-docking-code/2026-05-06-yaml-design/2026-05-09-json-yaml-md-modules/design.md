# Design — JSON / YAML / Markdown Module Split

Date: 2026-05-09
Lead: `@spec-steward`
Implementation owner: `@feature-dev`
Required reviewers: `@core-architect`, `@ops-engineer`, `@ui-ux-designer`, `@qa-engineer`

## 1. Summary

Today JSON, YAML, and Markdown live inside `papiflyfx-docking-code` next to
the editor renderer, lexer engine, fold pipeline, theme, and language SPI.
This design extracts them into three sibling modules:

```text
papiflyfx-docking-code            (core editor + language SPI + plain/java/js)
├── papiflyfx-docking-code-json
├── papiflyfx-docking-code-yaml
└── papiflyfx-docking-code-markdown
```

Each new module is a thin "language pack" that depends on the core editor
and contributes a `LanguageSupportProvider` discovered through
`META-INF/services`. The core module keeps the cross-cutting vocabulary
(`TokenType`, `FoldKind`, `CodeEditorTheme`, render dispatch) so token and
fold rendering stays consistent. Sample applications add explicit Maven
dependencies on the new modules so JSON, YAML, and Markdown content keeps
working without behavioral changes.

This is a structural refactor. It changes module boundaries and Maven
topology, not editor semantics.

## 2. Current State

### 2.1 Where the JSON / YAML / Markdown code lives

```
papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/
├── lexer/
│   ├── JsonLexer.java
│   ├── MarkdownLexer.java
│   ├── YamlLexer.java
│   └── ...                # PlainTextLexer, JavaLexer, JavaScriptLexer, engine
├── folding/
│   ├── BuiltInLanguageSupportProvider.java   # registers all 6 built-ins
│   ├── JsonFoldProvider.java
│   ├── MarkdownFoldProvider.java
│   ├── YamlFoldProvider.java
│   └── ...                # PlainTextFoldProvider, JavaFoldProvider, JS, pipeline
├── language/              # LanguageSupport, LanguageSupportProvider,
│                          # LanguageSupportRegistry, BootstrapOptions, etc.
├── lexer/TokenType.java   # JSON_KEY, YAML_KEY, YAML_ANCHOR, YAML_ALIAS,
│                          # YAML_TAG, HEADLINE, LIST_ITEM, CODE_BLOCK, ...
├── folding/FoldKind.java  # JSON_OBJECT, JSON_ARRAY, MARKDOWN_FENCE,
│                          # MARKDOWN_SECTION, YAML_MAPPING, YAML_BLOCK_SCALAR,
│                          # YAML_FLOW
├── theme/CodeEditorTheme.java     # jsonKeyColor, yamlKeyColor, yamlAnchorColor,
│                                  # yamlTagColor, headlineColor, listItemColor,
│                                  # codeBlockColor
├── theme/CodeEditorThemeMapper.java
└── render/TextPass.java   # token-color dispatch (TokenType -> Paint)
```

Tests in `src/test/java/.../lexer/{Json,Yaml,Markdown}LexerTest.java` and
`src/test/java/.../folding/{Json,Yaml,Markdown}FoldProviderTest.java`
exercise the lexers and fold providers directly.

### 2.2 Discovery and registration

`LanguageSupportRegistry.bootstrap(BootstrapOptions)` runs once for the
default registry. With default options, it:

1. Registers built-ins from `new BuiltInLanguageSupportProvider()`.
2. Loads `ServiceLoader<LanguageSupportProvider>` and registers each
   discovered provider with the configured `ConflictPolicy`.

Today every built-in is in step 1; the `ServiceLoader` path is unused for
shipped languages. The split moves JSON, YAML, and Markdown from step 1 to
step 2 without changing the registry itself.

### 2.3 Theme palette and token vocabulary

`CodeEditorTheme` is a Java `record` whose fields include
language-specific colors (`jsonKeyColor`, `yamlKeyColor`, `yamlAnchorColor`,
`yamlTagColor`, `headlineColor`, `listItemColor`, `codeBlockColor`).
`CodeEditorThemeMapper` maps these onto the shared docking `Theme`, and
`TextPass#tokenColor(...)` dispatches `TokenType` values to the right color
field. `TokenType` and `FoldKind` carry concrete language-specific names.

These are cross-cutting types: every editor instance, regardless of which
language modules ship, references the same `TokenType` enum and the same
`CodeEditorTheme` palette. Splitting them out would require a parallel
theme-contribution SPI and a token-scope registry. This design keeps
them in the core module and treats them as part of the stable rendering
contract — see [§5 Theme and token decisions](#5-theme-and-token-decisions).

## 3. Goals and Non-goals

### 3.1 Goals

1. JSON, YAML, and Markdown editor support lives in dedicated, optional
   Maven modules. Consumers can drop a module if they do not need that
   language.
2. The core `papiflyfx-docking-code` module no longer references the
   `JsonLexer`, `YamlLexer`, `MarkdownLexer`, or their fold providers.
3. Discovery is automatic: any module on the classpath that ships a
   `META-INF/services/org.metalib.papifly.fx.code.language.LanguageSupportProvider`
   descriptor contributes its languages.
4. JSON, YAML, and Markdown rendering, folding, and theming behave
   identically to the pre-split build for end users.
5. Build, test, and sample workflows continue to work under the existing
   headless / TestFX setup.

### 3.2 Non-goals

1. Theme-contribution SPI. `CodeEditorTheme` stays a record; `TextPass`
   keeps its current dispatch table.
2. Per-language settings UI.
3. Schema validation, formatting, or AST-level features.
4. Java / JavaScript module split (intentional, kept as roadmap follow-up).

## 4. Module Surface

Each new module has the same shape:

```text
papiflyfx-docking-code-<lang>/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/org/metalib/papifly/fx/code/lexer/
    │   │   └── <Lang>Lexer.java
    │   ├── java/org/metalib/papifly/fx/code/folding/
    │   │   ├── <Lang>FoldProvider.java
    │   │   └── <Lang>LanguageSupportProvider.java
    │   └── resources/
    │       └── META-INF/services/
    │           └── org.metalib.papifly.fx.code.language.LanguageSupportProvider
    └── test/
        └── java/org/metalib/papifly/fx/code/
            ├── lexer/<Lang>LexerTest.java
            └── folding/<Lang>FoldProviderTest.java
```

### 4.1 Package layout

The new modules preserve the existing lexer and folding package paths:

| Class family | Package |
| ------------ | ------- |
| Lexers | `org.metalib.papifly.fx.code.lexer` |
| Fold providers and language providers | `org.metalib.papifly.fx.code.folding` |

This intentionally creates split packages across Maven artifacts in the
unnamed module path used by the build. The compatibility trade-off is
deliberate: user code that imports
`org.metalib.papifly.fx.code.lexer.JsonLexer` only needs the matching new
Maven dependency, not a source import change.

The fold providers remain package-private as they were before the split.
Each `<Lang>LanguageSupportProvider` therefore lives in
`org.metalib.papifly.fx.code.folding`, next to the fold provider it
instantiates.

`LanguageSupport.id()` values stay identical (`json`, `yaml`, `markdown`)
so user-visible identifiers in serialized session data and theme keys do
not change.

### 4.2 LanguageSupportProvider contribution

Each module ships exactly one `LanguageSupportProvider` that returns the
single `LanguageSupport` it owns. Example for JSON:

```java
package org.metalib.papifly.fx.code.folding;

import org.metalib.papifly.fx.code.language.LanguageSupport;
import org.metalib.papifly.fx.code.language.LanguageSupportProvider;
import org.metalib.papifly.fx.code.lexer.JsonLexer;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class JsonLanguageSupportProvider implements LanguageSupportProvider {
    @Override
    public Collection<LanguageSupport> getLanguageSupports() {
        return List.of(new LanguageSupport(
            "json", "JSON",
            Set.of(), Set.of("json"),
            Set.of(),
            JsonLexer::new, JsonFoldProvider::new));
    }
}
```

The descriptor lives at:

```text
src/main/resources/META-INF/services/org.metalib.papifly.fx.code.language.LanguageSupportProvider
```

with a single line:

```text
org.metalib.papifly.fx.code.folding.JsonLanguageSupportProvider
```

YAML and Markdown providers follow the same shape. No registry changes
are required: `LanguageSupportRegistry.bootstrap(...)` already loads
`ServiceLoader<LanguageSupportProvider>` providers when
`BootstrapOptions.loadServiceProviders()` is true (the default).

### 4.3 What stays in `papiflyfx-docking-code`

After the split, the core module keeps:

1. The language SPI: `LanguageSupport`, `LanguageSupportProvider`,
   `LanguageSupportRegistry`, `BootstrapOptions`, `ConflictPolicy`,
   `LanguageRegistryListener`, `RegistryDiagnostic`,
   `UserFileAssociationMapping`.
2. Plain text, Java, and JavaScript lexers and fold providers.
3. `BuiltInLanguageSupportProvider`, slimmed down to register only
   plain-text, java, and javascript.
4. `TokenType`, `FoldKind` (full enum, including JSON / YAML / Markdown
   values).
5. `CodeEditorTheme`, `CodeEditorThemeMapper`, `TextPass` (full token-color
   dispatch).
6. `IncrementalLexerEngine`, `IncrementalLexerPipeline`,
   `IncrementalFoldingPipeline`, `LexState`, `TokenMap`, `LineTokens`,
   the editor `api`, `command`, `document`, `gutter`, `render`, `search`,
   `state`, `theme` packages.

### 4.4 What moves to each new module

| Source path (current)                                                          | Destination module                | Destination package                          |
| ------------------------------------------------------------------------------ | --------------------------------- | -------------------------------------------- |
| `lexer/JsonLexer.java`                                                         | `papiflyfx-docking-code-json`     | `org.metalib.papifly.fx.code.lexer`          |
| `folding/JsonFoldProvider.java`                                                | `papiflyfx-docking-code-json`     | `org.metalib.papifly.fx.code.folding`        |
| `lexer/YamlLexer.java`                                                         | `papiflyfx-docking-code-yaml`     | `org.metalib.papifly.fx.code.lexer`          |
| `folding/YamlFoldProvider.java`                                                | `papiflyfx-docking-code-yaml`     | `org.metalib.papifly.fx.code.folding`        |
| `lexer/MarkdownLexer.java`                                                     | `papiflyfx-docking-code-markdown` | `org.metalib.papifly.fx.code.lexer`          |
| `folding/MarkdownFoldProvider.java`                                            | `papiflyfx-docking-code-markdown` | `org.metalib.papifly.fx.code.folding`        |
| `src/test/java/.../lexer/{Json,Yaml,Markdown}LexerTest.java`                   | matching new module               | `org.metalib.papifly.fx.code.lexer`          |
| `src/test/java/.../folding/{Json,Yaml,Markdown}FoldProviderTest.java`          | matching new module               | `org.metalib.papifly.fx.code.folding`        |

Each new module also adds:

1. A `<Lang>LanguageSupportProvider` class.
2. A `META-INF/services/org.metalib.papifly.fx.code.language.LanguageSupportProvider`
   descriptor.
3. A `README.md` describing what the module ships.

## 5. Theme and Token Decisions

### Decision T1 — `TokenType` stays in core

Status: accepted

Decision: `TokenType` keeps its current values (`JSON_KEY`, `YAML_KEY`,
`YAML_ANCHOR`, `YAML_ALIAS`, `YAML_TAG`, `HEADLINE`, `LIST_ITEM`,
`CODE_BLOCK`, etc.) inside `papiflyfx-docking-code`.

Rationale: `TokenType` is consumed by `TextPass`, by the lexer engine, and
by every theme. Moving language-specific values into language modules
forces an indirection (per-module token registries, dispatch tables) for
no concrete benefit in this iteration. The split is structural; the
rendering contract stays stable.

User impact: None.

Implementation impact: Language modules import `TokenType` from
`papiflyfx-docking-code` and emit the same values they emit today.

Validation required: Visual smoke check via `SamplesApp` that JSON, YAML,
and Markdown render identically pre- and post-split.

### Decision T2 — `FoldKind` stays in core

Status: accepted

Decision: `FoldKind` keeps `JSON_OBJECT`, `JSON_ARRAY`, `MARKDOWN_FENCE`,
`MARKDOWN_SECTION`, `YAML_MAPPING`, `YAML_BLOCK_SCALAR`, `YAML_FLOW`.

Rationale: same as T1. `FoldKind` is consumed by the folding pipeline and
gutter; modules emit values, they do not interpret them.

### Decision T3 — `CodeEditorTheme` stays a record in core

Status: accepted

Decision: `CodeEditorTheme` keeps its language-specific color fields
(`jsonKeyColor`, `yamlKeyColor`, `yamlAnchorColor`, `yamlTagColor`,
`headlineColor`, `listItemColor`, `codeBlockColor`).

Rationale: replacing the record with a `Map<String, Paint>` and a
contribution SPI is a meaningful refactor. It deserves its own design
document and `@ui-ux-designer` review. This split is intentionally
scoped to physical module decomposition; theme contribution is tracked
as a roadmap follow-up.

User impact: None for end users.

Implementation impact: Language modules do not own theme keys. If a
language module needs a new color, it must coordinate with
`@ui-ux-designer` and add the color to `CodeEditorTheme`.

### Decision T4 — `BuiltInLanguageSupportProvider` slimmed, not deleted

Status: accepted

Decision: keep `BuiltInLanguageSupportProvider` in
`papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/folding/`
but remove `json`, `yaml`, and `markdown` entries. Plain text, Java, and
JavaScript stay registered there.

Rationale: keeping `BuiltInLanguageSupportProvider` in core preserves the
rule that `papiflyfx-docking-code` always ships at least plain-text and
the two general-purpose languages, even if no language-pack module is on
the classpath. This keeps the core module standalone-usable and avoids
empty-registry edge cases.

### Decision T5 — Discovery via `ServiceLoader`, no registry change

Status: accepted

Decision: rely on the existing
`LanguageSupportRegistry.bootstrap(BootstrapOptions)` flow. Each new
module ships a `META-INF/services/...LanguageSupportProvider` descriptor.
No changes to `LanguageSupportRegistry`, `BootstrapOptions`, or
`ConflictPolicy` are required.

Rationale: the SPI was designed for exactly this. Adding a new layer
would be premature.

Implementation impact: only documentation work — extend
`papiflyfx-docking-code/README.md` to point new authors at the SPI.

## 6. Build Topology

### 6.1 Root POM module list

Add three modules to `pom.xml` `<modules>`, after `papiflyfx-docking-code`
and before `papiflyfx-docking-tree`, to keep alphabetical-by-feature
order intact:

```xml
<module>papiflyfx-docking-code</module>
<module>papiflyfx-docking-code-json</module>
<module>papiflyfx-docking-code-markdown</module>
<module>papiflyfx-docking-code-yaml</module>
<module>papiflyfx-docking-tree</module>
```

### 6.2 New module `pom.xml` shape

Each new module inherits from the root parent POM, depends on
`papiflyfx-docking-code`, and reuses the same Surefire `argLine` block as
`papiflyfx-docking-code` so headless TestFX runs work uniformly. Sketch
(`papiflyfx-docking-code-json/pom.xml`):

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.metalib.papifly.docking</groupId>
        <artifactId>papiflyfx-docking</artifactId>
        <version>0.0.24-SNAPSHOT</version>
    </parent>

    <artifactId>papiflyfx-docking-code-json</artifactId>
    <name>papiflyfx-docking-code-json</name>
    <description>JSON language pack for the PapiflyFX code editor.</description>

    <dependencies>
        <dependency>
            <groupId>org.metalib.papifly.docking</groupId>
            <artifactId>papiflyfx-docking-code</artifactId>
            <version>${project.version}</version>
        </dependency>

        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <scope>test</scope>
        </dependency>
        <!-- TestFX deps mirrored only when UI tests need them. -->
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <useModulePath>false</useModulePath>
                    <argLine>
                        --enable-native-access=javafx.graphics
                        --add-exports=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
                        --add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
                    </argLine>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

`papiflyfx-docking-code-yaml` and `papiflyfx-docking-code-markdown` follow
the identical template. The current JSON, YAML, and Markdown lexer and
folding tests are pure JUnit tests with no JavaFX threading dependency,
so the TestFX-specific dependencies and the heavier Surefire `argLine`
that `papiflyfx-docking-code` uses for editor UI tests are not required
in the new modules. If a future test needs a JavaFX scene graph it can
opt in by adding the TestFX/Monocle dependencies.

### 6.3 Sample wiring

`papiflyfx-docking-samples/pom.xml` adds three new dependencies (one per
new module). The existing `JsonEditorSample`, `MarkdownEditorSample`, and
`YamlEditorSample` classes already go through the registry — they do not
import the lexer or fold-provider classes — so no sample code changes
are required.

### 6.4 BOM update

If `papiflyfx-docking-bom` enumerates feature modules, add the three new
modules to keep downstream consumers consistent. Validate by inspecting
the BOM during Phase 4.

## 7. ServiceLoader Wiring and Discovery Order

`LanguageSupportRegistry.bootstrap(BootstrapOptions.defaults())` runs
once per registry. Order:

1. `BuiltInLanguageSupportProvider` registers `plain-text`, `java`,
   `javascript`.
2. `ServiceLoader<LanguageSupportProvider>` enumerates providers from the
   classpath. With the three new modules on the classpath, this yields
   `JsonLanguageSupportProvider`, `MarkdownLanguageSupportProvider`,
   `YamlLanguageSupportProvider`.

The default `BootstrapOptions` use `ConflictPolicy.REJECT_ON_CONFLICT`.
Because step 1 no longer registers `json`, `yaml`, or `markdown`, there
are no conflicts. If a downstream user installs an alternative provider
for the same language id, the existing diagnostic flow surfaces it
through `LanguageSupportRegistry.diagnosticsSnapshot()`.

Edge case: if a deployment ships `papiflyfx-docking-code` without any of
the new modules, `.json`, `.yaml`, and `.md` files fall back to
`PlainTextLexer` exactly the way unknown languages do today. This is the
intended degradation path.

## 8. Migration Rules for External Consumers

The repository is the only known consumer. This section documents what
external users would see if a third party depends on
`papiflyfx-docking-code` directly.

1. **Direct lexer imports** (e.g.
   `import org.metalib.papifly.fx.code.lexer.JsonLexer;`) continue to
   compile after adding the matching new module dependency. The class
   moves artifacts, not packages.
2. **Registry usage** (e.g. `LanguageSupportRegistry.defaultRegistry()
   .resolveLexer("json")`) continues to work as long as the relevant
   module is on the classpath.
3. **`LanguageSupport.id()` strings** stay the same (`json`, `yaml`,
   `markdown`). Persisted session data, theme keys, and
   `detectLanguageId(...)` results are unaffected.
4. **Maven coordinates**: a project that wants only the editor and JSON
   support now declares both `papiflyfx-docking-code` and
   `papiflyfx-docking-code-json`. Documented in each new module's
   `README.md`.

## 9. Testing Strategy

### 9.1 Test moves

`JsonLexerTest`, `JsonFoldProviderTest`, `YamlLexerTest`,
`YamlFoldProviderTest`, `MarkdownLexerTest`, and `MarkdownFoldProviderTest`
move into their respective new modules under
their existing `org.metalib.papifly.fx.code.lexer` and
`org.metalib.papifly.fx.code.folding` packages inside the matching new
module.

### 9.2 New tests per language module

Each new module adds:

1. A `<Lang>LanguageSupportProviderTest` that:
   - Constructs the provider directly and asserts the returned
     `LanguageSupport` has the expected id, aliases, file extensions,
     non-null lexer factory, and non-null fold-provider factory.
   - Optionally instantiates the lexer and the fold provider via the
     factories to ensure they are not stubs.
2. A discovery smoke test that:
   - Calls `ServiceLoader.load(LanguageSupportProvider.class)` against
     the current class loader,
   - Asserts the language id is in the loaded set,
   - Calls `LanguageSupportRegistry.defaultRegistry().resolveLexer("<id>")`
     and asserts the returned class is the expected lexer.

### 9.3 Core regression tests

`papiflyfx-docking-code` keeps any tests that exercise registry behavior
generically. Add a new test:

`LanguageSupportRegistryDiscoveryTest` that runs only with the language
modules on the classpath (this happens in `papiflyfx-docking-samples`
where all modules co-locate) and asserts every shipped language id
resolves to a real lexer and a real fold provider. This guards against
regressions where the descriptor file is missing or misnamed.

### 9.4 Sample smoke coverage

`papiflyfx-docking-samples` already exercises JSON, YAML, and Markdown
through `SamplesApp`. The existing smoke test (or a new one if missing)
exercises the editor for each language and asserts:

1. Highlighting tokens appear (no plain-text fallback).
2. Fold regions exist where the test text contains foldable structures.
3. Theme switching does not crash.

### 9.5 Build verification

Each phase runs:

```bash
./mvnw -pl papiflyfx-docking-code -am clean package
./mvnw -pl papiflyfx-docking-code-json -am clean package
./mvnw -pl papiflyfx-docking-code-yaml -am clean package
./mvnw -pl papiflyfx-docking-code-markdown -am clean package
./mvnw -pl papiflyfx-docking-samples -am -Dtestfx.headless=true test
./mvnw clean package
```

Final acceptance gate: `./mvnw clean package` and
`./mvnw -Dtestfx.headless=true test` from a clean tree.

## 10. Risks and Mitigations

| Risk                                                                                         | Likelihood | Impact | Mitigation                                                                                                                                       |
| -------------------------------------------------------------------------------------------- | ---------- | ------ | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| `META-INF/services` descriptor missing or misnamed; language silently falls back to plain.   | Medium     | High   | Discovery smoke test in §9.2; sample integration smoke test asserts non-plain rendering; reviewer checklist requires inspecting the descriptor.  |
| Direct imports of `JsonLexer` / `YamlLexer` / `MarkdownLexer` somewhere unexpected break.    | Low        | Medium | Repo-wide `grep` for the FQNs before merge; document migration in each new module's `README.md`.                                                 |
| Surefire / TestFX argLine regression on a new module.                                        | Low        | Medium | Copy-paste argLine from `papiflyfx-docking-code` for any module that adds UI tests; keep current set of tests JUnit-only where possible.         |
| ServiceLoader cycle or duplicate registration when both core and a module register `json`.   | Low        | High   | Step 1 of bootstrap is updated to drop json/yaml/markdown explicitly; conflict policy still defaults to `REJECT_ON_CONFLICT` to surface mistakes. |
| BOM / archetype drift after adding modules.                                                  | Medium     | Low    | Phase 4 explicitly validates `papiflyfx-docking-bom` and the archetype against the new modules.                                                  |
| Confusion about whether to add a new theme color or token type from a language module.       | Low        | Low    | `README.md` of each new module documents Decisions T1–T3 and points at `@ui-ux-designer` review for theme changes.                               |
| Spec docs drift (yaml-design, lang-plugin) reference `papiflyfx-docking-code` paths.         | High       | Low    | Phase 5 sweeps known specs and updates code-path references, leaving date-stamped notes in the affected progress files.                          |

## 11. Documentation Updates

1. `papiflyfx-docking-code/README.md` — add a "Language packs" section
   that documents the SPI, lists the three new modules, and explains how
   to add a new language pack.
2. `papiflyfx-docking-code-json/README.md`, `-yaml/README.md`,
   `-markdown/README.md` — short README per module describing scope,
   Maven coordinates, and how the module is discovered.
3. Root `README.md` — module list updated to include the three new
   modules.
4. `CLAUDE.md` — module-structure and content-modules sections updated
   to mention the new modules and describe how language packs are
   discovered.
5. `spec/papiflyfx-docking-code-lang-plugin/` — add a forward link from
   that spec's README into this folder so future authors see the split
   when extending the SPI.

## 12. Open Questions for Reviewers

1. `@core-architect`: do we want to keep `BuiltInLanguageSupportProvider`
   inside `papiflyfx-docking-code/.../folding/`, or is this the right
   moment to move it under `.../language/` to match its semantic purpose?
2. `@ops-engineer`: should the BOM aggregate the new modules into a
   `papiflyfx-docking-code-all` virtual artifact for downstream
   convenience, or do we keep a plain list of independent modules?
3. `@ui-ux-designer`: are we comfortable deferring the theme-contribution
   SPI to a later iteration, given that `CodeEditorTheme` color fields
   stay tied to language-specific naming?
4. `@qa-engineer`: do we want a single discovery smoke test to live in
   `papiflyfx-docking-samples` (sees the full classpath) or per-module
   discovery tests, accepting that they only see their own module?
