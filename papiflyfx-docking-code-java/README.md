# papiflyfx-docking-code-java

Java language pack for `papiflyfx-docking-code`.

## Scope

This module owns Java syntax highlighting, Java fold regions, their tests, and
the `LanguageSupportProvider` that registers language id `java`.

It keeps the public lexer package path stable:

- `org.metalib.papifly.fx.code.lexer.JavaLexer`
- `org.metalib.papifly.fx.code.folding.JavaFoldProvider`

Consumers that imported `JavaLexer` directly add this module as a dependency;
no import change is required.

## Maven Dependency

```xml
<dependency>
    <groupId>org.metalib.papifly.docking</groupId>
    <artifactId>papiflyfx-docking-code-java</artifactId>
    <version>${papiflyfx.version}</version>
</dependency>
```

## Discovery

The module registers `org.metalib.papifly.fx.code.folding.JavaLanguageSupportProvider`
through `META-INF/services/org.metalib.papifly.fx.code.language.LanguageSupportProvider`.

Default editor settings: indent width `4`, insert spaces, ensure trailing
newline, and trim trailing whitespace.
