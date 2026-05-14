# JSON Theme Highlighting Analysis

Date: 2026-04-30  
Lead: `@spec-steward`  
Implementation owner: `@feature-dev`  
Required reviewers: `@ui-ux-designer`, `@qa-engineer`

## Summary

The current JSON syntax highlighting in `papiflyfx-docking-code` is functional but coarse-grained. `JsonLexer` emits tokens for strings, numbers, booleans, null literals, and punctuation, but it does not distinguish object keys from string values. As a result, JSON like `"name": "Ada"` renders both `"name"` and `"Ada"` with the same string color.

Rendering is centralized in `TextPass`, which maps token categories to `CodeEditorTheme` colors. The renderer currently maps common syntax categories such as `STRING`, `NUMBER`, `BOOLEAN`, and `NULL_LITERAL`, but JSON punctuation falls back to the editor foreground because `PUNCTUATION` has no dedicated color mapping. The theme record also does not expose JSON-specific colors such as `jsonKeyColor` or a general `punctuationColor`.

## Current Implementation

- `JsonLexer` lives in `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/JsonLexer.java`.
- Built-in language discovery registers JSON through `BuiltInLanguageSupportProvider`.
- `JsonFoldProvider` already scans JSON object and array structure outside strings for folding.
- `TextPass` draws the full line in the default foreground first, then overlays token-colored text segments.
- `CodeEditorTheme` provides dark and light palettes for general syntax categories, but not JSON-specific semantic roles.

## Recommended Visual Improvement

Add a dedicated JSON object-key token and theme color.

Implementation direction:

1. Add `JSON_KEY` to `TokenType`.
2. Add `jsonKeyColor` to `CodeEditorTheme`.
3. Update `JsonLexer` so a closed string followed by optional whitespace and `:` is emitted as `JSON_KEY` instead of `STRING`.
4. Update `TextPass` so `JSON_KEY` maps to `CodeEditorTheme#jsonKeyColor`.
5. Add focused lexer and theme tests proving keys and values render through separate token categories.

Suggested initial colors:

- Dark theme JSON keys: `#9cdcfe`
- Light theme JSON keys: `#0451a5`
- Keep string values on existing string colors: dark `#ce9178`, light `#a31515`

## Secondary Improvements

- Add a subtle punctuation color so `{`, `}`, `[`, `]`, `:`, and `,` are visible without dominating content.
- Separate `null` from booleans. Today `booleanColor` and `nullLiteralColor` are identical in both default palettes.
- Consider brace matching as a later enhancement. Folding already tracks object and array structure, but caret-adjacent brace highlighting should be handled separately from this JSON-key token work.

## Non-Goals

- Do not introduce an AST parser for this change.
- Do not alter folding behavior as part of key highlighting.
- Do not reuse `KEYWORD` or `IDENTIFIER` for JSON keys. A dedicated `JSON_KEY` token keeps the rendering model explicit and avoids weakening meaning for other languages.
