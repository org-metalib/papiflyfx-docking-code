# JSON Theme Highlighting Implementation Plan

Date: 2026-04-30  
Lead: `@spec-steward`  
Implementation owner: `@feature-dev`  
Required reviewers: `@ui-ux-designer`, `@qa-engineer`

## Problem Statement

JSON object keys currently render with the same color as JSON string values because `JsonLexer` emits all quoted JSON text as `TokenType.STRING`. This makes object-heavy JSON harder to scan and hides the structural distinction between field names and values.

## Scope

### In Scope

1. Add a JSON-specific token category for object keys.
2. Add theme support for JSON key color in dark and light editor palettes.
3. Route the new token through the existing canvas text renderer.
4. Add regression tests for JSON key tokenization and theme mapping.
5. Keep the change limited to `papiflyfx-docking-code`.

### Out of Scope

1. AST parsing or schema-aware JSON analysis.
2. Folding behavior changes.
3. Validation diagnostics for malformed JSON.
4. Full semantic highlighting framework across all languages.
5. Brace matching or rainbow bracket rendering.

## Design

### Token Model

Extend `TokenType` with:

```java
JSON_KEY
```

`JSON_KEY` represents a quoted string that appears in object-key position, including the surrounding quotes. A string is in object-key position when the closing quote is followed by optional whitespace and a colon on the same logical line.

This heuristic is intentionally line-local and lexer-friendly. It handles common formatted JSON and avoids adding a parser dependency.

### Lexer Rules

Update `JsonLexer` string handling:

1. Continue using the existing string continuation state for multiline recovery.
2. When a string closes in the current line, inspect characters after the closing quote.
3. If the next non-whitespace character is `:`, emit `JSON_KEY`.
4. Otherwise emit `STRING`.
5. Preserve existing handling for unterminated strings across line boundaries.

### Theme Rules

Extend `CodeEditorTheme` with:

```java
Paint jsonKeyColor
```

Recommended palette values:

- Dark: `#9cdcfe`
- Light: `#0451a5`

`CodeEditorThemeMapper` should preserve the base dark/light value, like current syntax colors do, rather than deriving JSON key color from the docking accent. Syntax colors should remain stable across application theme accents.

### Rendering Rules

Update `TextPass#tokenColor(...)`:

```java
case JSON_KEY -> context.theme().jsonKeyColor();
```

Punctuation color can be a follow-up unless implementation scope is expanded. If included in this same rollout, add a general `punctuationColor` to the theme and map `PUNCTUATION` explicitly.

## Acceptance Criteria

1. JSON object keys and string values produce different token types.
2. Dark and light default editor themes expose a non-null JSON key color.
3. `TextPass` maps `JSON_KEY` to the JSON key theme color.
4. Existing Java, JavaScript, Markdown, and plain-text token behavior is unchanged.
5. Existing JSON folding behavior is unchanged.

## Test Plan

### Unit Tests

1. Extend `JsonLexerTest`:
   - keys are emitted as `JSON_KEY`,
   - string values remain `STRING`,
   - escaped quotes inside keys do not terminate early,
   - unterminated strings retain existing multiline behavior.
2. Extend `CodeEditorThemeMapperTest`:
   - dark mapped theme has non-null `jsonKeyColor`,
   - light mapped theme has non-null `jsonKeyColor`.

### Focused Build

Run:

```bash
./mvnw -pl papiflyfx-docking-code -am -Dtest=JsonLexerTest,CodeEditorThemeMapperTest test
```

### Broader Regression

Run:

```bash
./mvnw -pl papiflyfx-docking-code -am -Dtestfx.headless=true test
```

## Review Gates

- `@feature-dev`: confirm lexer/token model fits content-module conventions.
- `@ui-ux-designer`: confirm key color choices and any punctuation follow-up.
- `@qa-engineer`: confirm test coverage and regression scope.

## Phased Tasks

### Phase 1 - Token Model

- [ ] Add `JSON_KEY` to `TokenType`.
- [ ] Update JSON lexer string classification.
- [ ] Add focused lexer tests.

### Phase 2 - Theme and Rendering

- [ ] Add `jsonKeyColor` to `CodeEditorTheme`.
- [ ] Update dark and light default palettes.
- [ ] Update `CodeEditorThemeMapper`.
- [ ] Update `TextPass` token-color mapping.
- [ ] Add theme mapper tests.

### Phase 3 - Optional Punctuation Polish

- [ ] Decide whether punctuation color belongs in this rollout.
- [ ] If included, add `punctuationColor` to theme and renderer.
- [ ] Validate punctuation remains subtle in dark and light themes.

### Phase 4 - Validation

- [ ] Run focused tests.
- [ ] Run the full `papiflyfx-docking-code` headless suite.
- [ ] Capture results in `progress.md`.
