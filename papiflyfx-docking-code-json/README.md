# papiflyfx-docking-code-json

JSON language pack for `papiflyfx-docking-code`.

## Scope

This module owns JSON syntax highlighting, JSON fold regions, their tests, and the `LanguageSupportProvider` that registers language id `json`.

It intentionally keeps the public lexer package path stable:

- `org.metalib.papifly.fx.code.lexer.JsonLexer`
- `org.metalib.papifly.fx.code.folding.JsonFoldProvider`

Consumers that imported `JsonLexer` directly add this module as a dependency; no import change is required.

## Maven Dependency

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-code-json</artifactId>
    <version>${papiflyfx.version}</version>
</dependency>
```

`papiflyfx-docking-code-json` depends on `papiflyfx-docking-code`, so the core editor is pulled in transitively.

## Discovery

The module registers `org.metalib.papifly.fx.code.folding.JsonLanguageSupportProvider` through:

```text
META-INF/services/org.metalib.papifly.fx.code.language.LanguageSupportProvider
```

It also registers `org.metalib.papifly.fx.code.theme.JsonSyntaxStyleProvider`
through `META-INF/services/org.metalib.papifly.fx.code.theme.SyntaxStyleProvider`.
When this module is on the classpath, `LanguageSupportRegistry.defaultRegistry()`
and `SyntaxStyleRegistry.defaultRegistry()` discover JSON support through
`ServiceLoader`.

| Language | ID | Extensions |
|----------|----|------------|
| JSON | `json` | `json` |

Style scopes:

| Scope | Dark | Light |
|-------|------|-------|
| `json.key` | `#9cdcfe` | `#0451a5` |

Default editor settings: indent width `2`, insert spaces, ensure trailing
newline, and trim trailing whitespace.
