# papiflyfx-docking-code-javascript

JavaScript language pack for `papiflyfx-docking-code`.

## Scope

This module owns JavaScript syntax highlighting, JavaScript fold regions, their
tests, and the `LanguageSupportProvider` that registers language id
`javascript` with alias `js`.

It keeps the public lexer package path stable:

- `org.metalib.papifly.fx.code.lexer.JavaScriptLexer`
- `org.metalib.papifly.fx.code.folding.JavaScriptFoldProvider`

Consumers that imported `JavaScriptLexer` directly add this module as a
dependency; no import change is required.

## Maven Dependency

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-code-javascript</artifactId>
    <version>${papiflyfx.version}</version>
</dependency>
```

## Discovery

The module registers `org.metalib.papifly.fx.code.folding.JavaScriptLanguageSupportProvider`
through `META-INF/services/org.metalib.papifly.fx.code.language.LanguageSupportProvider`.

Default editor settings: indent width `4`, insert spaces, ensure trailing
newline, and trim trailing whitespace.
