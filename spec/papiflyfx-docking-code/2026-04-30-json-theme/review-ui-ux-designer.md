# JSON Theme Highlighting Review - UI/UX Designer

Date: 2026-05-06  
Lead reviewer: `@ui-ux-designer`  
Scope: design/progress/implementation review for `spec/papiflyfx-docking-code/2026-04-30-json-theme`

## Verdict

Approved with minor follow-ups.

The implementation satisfies the core visual goal: JSON object keys are now semantically separate from string values and route through their own stable theme color. The chosen colors fit the existing code-editor palette, preserve the minimalist editor surface, and maintain strong foreground/background contrast in both default themes.

## Reviewed Artifacts

- `spec/papiflyfx-docking-code/2026-04-30-json-theme/README.md`
- `spec/papiflyfx-docking-code/2026-04-30-json-theme/plan.md`
- `spec/papiflyfx-docking-code/2026-04-30-json-theme/progress.md`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/JsonLexer.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/TokenType.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/TextPass.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorTheme.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeMapper.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/lexer/JsonLexerTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeMapperTest.java`

## Design Assessment

The original design asked for object keys to be visually distinct from string values without introducing a parser or broader semantic-highlighting framework. The delivered `JSON_KEY` token keeps that distinction explicit and avoids overloading `KEYWORD` or `IDENTIFIER`, which is the right design choice for future syntax-theme growth.

The lexer heuristic is visually pragmatic: a closed quoted string followed by optional whitespace and `:` becomes a key. That matches common formatted JSON and keeps highlighting fast and line-oriented. The behavior is also understandable for users because the rendered distinction follows visible JSON structure.

The theme decision is sound. `CodeEditorThemeMapper` preserves syntax colors from the base dark/light editor palette instead of deriving `jsonKeyColor` from the docking accent, so syntax meaning remains stable when the application accent changes. That is preferable for code readability.

## Acceptance Check

| Requirement | Status | Evidence |
| --- | --- | --- |
| JSON keys and string values produce different token types | Pass | `JsonLexer` emits `TokenType.JSON_KEY` when a closed string is followed by `:`; `JsonLexerTest` covers key/value separation. |
| Dark and light themes expose JSON key color | Pass | `CodeEditorTheme` adds `jsonKeyColor`; dark uses `#9cdcfe`, light uses `#0451a5`. |
| Text rendering maps JSON keys to the key color | Pass | `TextPass#tokenColor(...)` maps `JSON_KEY` to `context.theme().jsonKeyColor()`. |
| Existing syntax categories remain stable | Pass | The change adds a new token path and leaves existing `STRING`, `NUMBER`, `BOOLEAN`, `NULL_LITERAL`, and markdown mappings intact. |
| Punctuation polish remains optional | Pass | Progress marks punctuation color as deferred for visual review. |

## Contrast Check

Calculated against the default editor backgrounds:

| Pair | Contrast |
| --- | ---: |
| Dark JSON key `#9cdcfe` on `#1e1e1e` | 11.18:1 |
| Dark string `#ce9178` on `#1e1e1e` | 6.31:1 |
| Light JSON key `#0451a5` on `#ffffff` | 7.71:1 |
| Light string `#a31515` on `#ffffff` | 7.85:1 |

All four combinations clear normal-text AA contrast expectations. The key colors are sufficiently distinct from string colors without making JSON visually busy.

## Findings

### F-01: Visual distinction is implemented but not directly guarded by theme tests

**Severity:** P2  
**Area:** Theme regression coverage  
**Evidence:** `CodeEditorThemeMapperTest` asserts `jsonKeyColor()` is non-null, but does not assert it differs from `stringColor()` or matches the intended default palette values.  
**Risk:** A future palette edit could accidentally make JSON keys and string values the same color while tests still pass. That would regress the primary visual goal.  
**Suggested follow-up:** Add assertions that default dark/light `jsonKeyColor` values differ from `stringColor`, and optionally assert the chosen defaults exactly.

### F-02: Punctuation color remains a valid but non-blocking follow-up

**Severity:** P3  
**Area:** Syntax polish  
**Evidence:** `JsonLexer` still emits `PUNCTUATION`, but `TextPass` lets punctuation fall back to the editor foreground. `progress.md` explicitly defers punctuation color.  
**Risk:** JSON structure is now more readable through keys, but braces, brackets, colons, and commas still do not get a subtle structural treatment. This is acceptable for the current rollout and avoids adding visual noise prematurely.  
**Suggested follow-up:** Prototype a low-emphasis punctuation color only after visual review in realistic JSON documents. Keep it neutral and less prominent than keys, literals, and values.

### F-03: `null` and boolean values still share color

**Severity:** P3  
**Area:** Syntax hierarchy  
**Evidence:** The default palettes still use the same color for `booleanColor` and `nullLiteralColor`.  
**Risk:** JSON value scanning would improve slightly if `null` had a softer or less active color than `true`/`false`, but this is secondary to the key/value distinction.  
**Suggested follow-up:** Consider a later palette pass that separates `null` from booleans across dark and light themes, with contrast checks.

## Progress Review

`progress.md` accurately reflects the implementation state:

- Phase 1 token model: completed.
- Phase 2 theme/rendering: completed.
- Phase 3 punctuation polish: deferred by design.
- Phase 4 validation: completed and recorded.

One documentation note: `plan.md` remains a pre-implementation checklist with unchecked tasks. That is acceptable if `progress.md` is the canonical completion tracker, but future readers may benefit from either marking the plan as historical or linking them explicitly.

## Validation Performed For This Review

- Source and spec inspection.
- Color contrast calculation for the selected key and string colors.
- Maven tests were not rerun during this review; validation results were taken from `progress.md`.

## Recommendation

Keep the current implementation. The visual design is restrained, accessible, and consistent with the existing code-editor palette. The only recommended near-term hardening is to add tests that protect the actual visual distinction between JSON keys and string values, not just the presence of `jsonKeyColor`.
