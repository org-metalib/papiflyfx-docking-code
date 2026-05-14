# YAML Code Format Implementation Plan

Date: 2026-05-06  
Lead: `@spec-steward`  
Implementation owner: `@feature-dev`  
Required reviewers: `@ui-ux-designer`, `@qa-engineer`, `@core-architect`

## Problem Statement

`papiflyfx-docking-code` does not recognize YAML. Files with `.yml` or
`.yaml` extensions open through `PlainTextLexer`, which means no comment
coloring, no key/value differentiation, no string/literal coloring, and
no folding. Configuration files — Hugo front matter, Kubernetes
manifests, CI workflows, application configs — are common dockable
content in this project, so the gap is visible to every realistic user.

## Scope

### In Scope

1. New `YamlLexer` covering YAML scalars, mapping keys, comments,
   booleans, null, numbers, anchors, aliases, tags, and document
   markers, with per-line state tracking for multi-line strings and
   block scalars.
2. New `YAML_KEY` token category and routing through `TextPass` to the
   existing `jsonKeyColor`.
3. New `YamlFoldProvider` that builds indentation-based fold regions
   for block mappings, block sequences, and block scalars.
4. Two new `FoldKind` values: `YAML_MAPPING`, `YAML_BLOCK_SCALAR`.
5. Registration of the `yaml` language in
   `BuiltInLanguageSupportProvider` with extensions `yaml`, `yml` and
   alias ids `yml`, `yaml`.
6. Lexer regression coverage and folding regression coverage in line
   with existing JSON, Markdown, and Java test patterns.
7. A representative YAML smoke fixture exercised through the
   incremental lexer and folding pipeline.

### Out of Scope

1. YAML 1.2 schema parsing, type inference, or AST construction.
2. Flow-style folding for `{...}` and `[...]`.
3. Anchor/alias resolution or merge-key (`<<`) expansion at the editor
   level.
4. YAML directives (`%YAML`, `%TAG`).
5. Schema-aware diagnostics or linting.
6. New theme palette colors. The first cut reuses existing colors
   (`jsonKeyColor`, `stringColor`, `numberColor`, `commentColor`,
   `booleanColor`, `nullLiteralColor`, plus `OPERATOR`/`PUNCTUATION`).
7. Brace matching or rainbow-bracket rendering.
8. Settings UI for YAML-specific preferences (tab width, indent size).

## Design

### Token Model

Extend `TokenType` with:

```java
YAML_KEY
```

`YAML_KEY` represents the key portion of a YAML mapping entry before the
mapping colon. The colon is emitted separately as `PUNCTUATION` for both
block and flow mappings, so `YAML_KEY` covers only the identifier-like
span before the colon.

The remaining YAML categories reuse existing token types:

| YAML construct           | Token type      |
| ------------------------ | --------------- |
| `# comment`              | `COMMENT`       |
| `'single quoted'`        | `STRING`        |
| `"double quoted"`        | `STRING`        |
| `plain scalar`           | `PLAIN`         |
| Numbers / floats / hex   | `NUMBER`        |
| `true`/`false`/`yes`/... | `BOOLEAN`       |
| `null`/`Null`/`NULL`/`~` | `NULL_LITERAL`  |
| `&anchor` / `*alias`     | `OPERATOR`      |
| `!!tag` / `!CustomTag`   | `OPERATOR`      |
| `<<` (merge key)         | `OPERATOR`      |
| `---` / `...`            | `PUNCTUATION`   |
| `|` / `>` indicators     | `PUNCTUATION`   |
| `:` / `-` / `,` / `{}[]` | `PUNCTUATION`   |

Plain scalars stay rendered through `editorForeground` (the default
fallback in `TextPass`), matching how unstyled tokens currently render.

### Lexer Rules

`YamlLexer implements Lexer` with `LANGUAGE_ID = "yaml"`. State codes:

```text
STATE_DEFAULT          = 0
STATE_DOUBLE_QUOTED    = 1   // unterminated "..." continues on next line
STATE_SINGLE_QUOTED    = 2   // unterminated '...' continues on next line
STATE_BLOCK_SCALAR     = 3   // body of |, |+, |-, >, >+, >- block
```

Line-by-line behavior in `lexLine(text, entryState)`:

1. **String continuation entry states** restart inside the open
   `STRING` and either close (`"` or `'` honoring `\` escape for
   double-quoted, `''` escape for single-quoted) and switch back to
   `STATE_DEFAULT`, or emit the rest of the line as `STRING` and
   propagate the same state.
2. **Block scalar entry state** emits the entire line as `PLAIN`
   (rendered through editor foreground) and exits when the line is
   blank or its leading-space count drops to zero. The MVP does not
   track the introducing indent in `LexState`, which is a known
   approximation called out in `README.md` Q2.
3. **Default state**:
   - Skip leading whitespace.
   - If the trimmed line is exactly `---` or `...` at column 0, emit
     `PUNCTUATION` for the marker.
   - If the next character is `#`, emit the rest of the line as
     `COMMENT`.
   - If the next character is `'` or `"`, scan a quoted string. If the
     closing quote is missing, switch to `STATE_SINGLE_QUOTED` or
     `STATE_DOUBLE_QUOTED` and emit the partial string.
   - If the next character is `&` or `*` followed by a name character,
     emit `OPERATOR`.
   - If the next characters are `!!` or `!` followed by a tag name,
     emit `OPERATOR`.
   - If the next characters are `<<` followed by `:`, emit the `<<`
     as `OPERATOR` and the `:` as `PUNCTUATION`.
   - If the next character is `-` followed by space, emit
     `PUNCTUATION` for the sequence indicator.
   - If the cursor is at the start of an unquoted token that is
     followed by optional whitespace and `:` (still inside the same
     line and the colon is followed by whitespace, end-of-line, or
     `#`), emit the token as `YAML_KEY`. Keys may contain letters,
     digits, `_`, `-`, `.`, and `/`. The trailing `:` is emitted as
     `PUNCTUATION`.
   - If the next characters are `|` or `>` optionally followed by
     `+`/`-` and end-of-line or whitespace, emit `PUNCTUATION` and
     switch to `STATE_BLOCK_SCALAR`.
   - If the next token is a number literal (integer, float, exponent,
     `0x`/`0o`, or `.inf`/`-.inf`/`+.inf`/`.nan`), emit `NUMBER`.
   - If the next token is `true`/`false`/`True`/`False`/`TRUE`/`FALSE`
     or `yes`/`no`/`on`/`off` and is in value position (i.e. not
     followed by `:`), emit `BOOLEAN`.
   - If the next token is `null`/`Null`/`NULL` or a lone `~` and is
     in value position, emit `NULL_LITERAL`.
   - Otherwise emit a plain scalar run — characters up to the next
     comment marker, comma, colon-followed-by-space, or end of line —
     as `PLAIN`.

### Folding Rules

`YamlFoldProvider implements FoldProvider` with
`languageId() = YamlLexer.LANGUAGE_ID`.

Algorithm:

1. Pre-compute each line's `indent` (count of leading spaces; tabs
   normalized to a fixed width, default 1 column) and a `kind`:
   - `BLANK` if the trimmed line is empty.
   - `COMMENT` if the trimmed line starts with `#`.
   - `MAPPING_HEADER` if the line matches `^\s*[A-Za-z0-9_./-]+:\s*(#.*)?$`
     (a key with no inline value).
   - `SEQUENCE_HEADER` if the trimmed line equals `-` or starts with
     `-\s*$` or `-\s*#` (a sequence item that opens a child mapping).
   - `BLOCK_SCALAR_HEADER` if the line ends in `|`, `|+`, `|-`, `>`,
     `>+`, or `>-` (after stripping inline comments).
   - `OTHER` otherwise.
2. For each `MAPPING_HEADER` or `SEQUENCE_HEADER` line at indent `i`,
   the fold region ends at the last following non-blank, non-comment
   line whose indent is strictly greater than `i`. Emit
   `FoldKind.YAML_MAPPING` if the run contains at least one line.
3. For each `BLOCK_SCALAR_HEADER` line at indent `i`, the fold region
   ends at the last following line whose indent is greater than `i` or
   that is blank inside the body. Emit
   `FoldKind.YAML_BLOCK_SCALAR`.
4. Preserve the baseline `collapsedHeaderLines()` set across recompute
   cycles, mirroring `JsonFoldProvider` and `MarkdownFoldProvider`.

Add to `FoldKind`:

```java
YAML_MAPPING,
YAML_BLOCK_SCALAR
```

### Theme and Rendering Rules

No `CodeEditorTheme` field changes. `TextPass#tokenColor(...)` adds:

```java
case YAML_KEY -> context.theme().jsonKeyColor();
```

`OPERATOR` already falls through to `editorForeground` today. If
`@ui-ux-designer` requests differentiation for anchors/aliases or
tags, that becomes a follow-up phase that introduces a new theme
color rather than a token type change.

### Language Registration

`BuiltInLanguageSupportProvider` adds:

```java
new LanguageSupport(
    "yaml", "YAML",
    Set.of("yml"), Set.of("yaml", "yml"),
    Set.of(),
    YamlLexer::new, YamlFoldProvider::new
)
```

### Module Boundaries

- All changes stay inside `papiflyfx-docking-code`.
- No `papiflyfx-docking-api`, settings-api, or login changes.
- No `META-INF/services` additions; built-in registration is enough.

## Acceptance Criteria

1. `.yaml` and `.yml` files open through `YamlLexer` and produce
   non-empty token streams for every category in the token model
   table above.
2. YAML mapping keys produce `YAML_KEY` tokens distinct from
   `STRING` and `PLAIN`, and render through `jsonKeyColor` via
   `TextPass`.
3. Comments (`# ...`) render through `commentColor`.
4. Multi-line double-quoted and single-quoted strings continue across
   line boundaries via lexer state.
5. Indentation-based folding produces `YAML_MAPPING` regions for
   block mappings/sequences and `YAML_BLOCK_SCALAR` regions for `|`
   and `>` block scalars.
6. Existing JSON, Java, JavaScript, Markdown, and plain-text token
   and folding behavior is unchanged.
7. The `papiflyfx-docking-code` headless test suite passes.

## Test Plan

### Unit Tests

1. `YamlLexerTest` (new):
   - Block mapping line `name: Ada` produces `YAML_KEY` + `PUNCTUATION`
     + `PLAIN`.
   - Block mapping with quoted value `name: "Ada"` produces `YAML_KEY`
     + `PUNCTUATION` + `STRING`.
   - Inline comment `key: value # note` produces `YAML_KEY` +
     `PUNCTUATION` + `PLAIN` + `COMMENT`.
   - Boolean value variants (`true`, `false`, `yes`, `on`) emit
     `BOOLEAN` only in value position.
   - Null variants (`null`, `~`) emit `NULL_LITERAL` only in value
     position.
   - Number forms (`42`, `-3.14`, `1e5`, `0x1F`, `.inf`, `.nan`) emit
     `NUMBER`.
   - Document markers `---` and `...` emit `PUNCTUATION`.
   - Sequence indicator `- item` emits `PUNCTUATION` + `PLAIN`.
   - Anchor/alias `&id001`/`*id001` emit `OPERATOR`.
   - Tag `!!str foo` emits `OPERATOR` + `PLAIN`.
   - Merge key `<<: *base` emits `OPERATOR` + `PUNCTUATION` +
     `OPERATOR`.
   - Unterminated double-quoted string switches to
     `STATE_DOUBLE_QUOTED`; the next line completes the string and
     returns to `STATE_DEFAULT`.
   - Unterminated single-quoted string behaves analogously, including
     `''` escape handling.
   - Block scalar header `description: |` switches to
     `STATE_BLOCK_SCALAR`; subsequent indented lines emit `PLAIN`;
     a dedented or blank line returns to `STATE_DEFAULT`.

2. `YamlFoldProviderTest` (new):
   - Mapping header with two indented children produces one
     `YAML_MAPPING` region spanning header to last child.
   - Block sequence with three indented sub-mappings produces a
     `YAML_MAPPING` region.
   - Block scalar `|` with three indented body lines produces a
     `YAML_BLOCK_SCALAR` region.
   - Mixed comment lines inside a region do not truncate the region.
   - Empty document yields no regions.

3. `BuiltInLanguageSupportProviderTest` (extend if present, else add a
   smoke test inside `YamlLexerTest`) — confirm `yaml` resolves by
   id, alias `yml`, and extensions `yaml`/`yml`.

4. `CodeEditorThemeMapperTest`: no functional change required, but
   add a guard that `jsonKeyColor` continues to render YAML keys (a
   defensive assertion that the token routes through the same paint
   helps catch accidental regressions if a future change splits the
   color).

### Focused Build

```bash
./mvnw -pl papiflyfx-docking-code -am -Dtest=YamlLexerTest,YamlFoldProviderTest,JsonLexerTest,JsonFoldProviderTest,CodeEditorThemeMapperTest test
```

### Broader Regression

```bash
./mvnw -pl papiflyfx-docking-code -am -Dtestfx.headless=true test
```

### Manual Spot Check

Open one realistic YAML file in the samples app (Kubernetes-style
manifest, Hugo front matter, or GitHub Actions workflow) and confirm:

- Keys are visually distinct from values.
- Comments are clearly subdued.
- Block scalars render without color noise.
- Folding gutters appear on mapping and sequence headers.

## Review Gates

- `@core-architect`: confirm the line-oriented lexer with
  per-line state code is acceptable for block scalars, or require
  widening `LexState` (and the downstream incremental engine) to
  carry indent context.
- `@feature-dev`: confirm token classification choices fit existing
  content-module conventions and the rendering path.
- `@ui-ux-designer`: confirm that reusing `jsonKeyColor` for YAML
  mapping keys reads correctly in dark and light themes, and decide
  on the anchor/alias/tag color follow-up.
- `@qa-engineer`: confirm the lexer/folding test fixtures cover the
  realistic YAML shapes used in the project.

## Phased Tasks

### Phase 1 - Token Model

- [ ] Add `YAML_KEY` to `TokenType`.
- [ ] Add `YAML_MAPPING` and `YAML_BLOCK_SCALAR` to `FoldKind`.

### Phase 2 - Lexer

- [ ] Implement `YamlLexer` with `STATE_DEFAULT`,
      `STATE_DOUBLE_QUOTED`, `STATE_SINGLE_QUOTED`, and
      `STATE_BLOCK_SCALAR` codes.
- [ ] Add `YamlLexerTest` covering the cases listed in the test plan.

### Phase 3 - Folding

- [ ] Implement `YamlFoldProvider` driven by line indent and header
      classification.
- [ ] Add `YamlFoldProviderTest` covering mapping, sequence, block
      scalar, and comment-tolerant regions.

### Phase 4 - Theme and Rendering

- [ ] Update `TextPass#tokenColor(...)` to map `YAML_KEY` to
      `jsonKeyColor`.
- [ ] Add a `CodeEditorThemeMapperTest` regression guarding that
      `jsonKeyColor` stays distinct from `stringColor`, `editorForeground`,
      and the default plain-text color.

### Phase 5 - Language Registration

- [ ] Register `yaml` in `BuiltInLanguageSupportProvider` with
      extensions `yaml`, `yml` and alias `yml`.
- [ ] Add a registration smoke test (or extend an existing one).

### Phase 6 - Validation

- [ ] Run focused tests.
- [ ] Run the full headless `papiflyfx-docking-code` suite.
- [ ] Capture results in `progress.md`.
- [ ] Manual spot check of a realistic YAML document inside the
      samples app.

### Phase 7 - Optional Follow-ups

- [x] Decide whether to introduce `yamlAnchorColor` / `yamlTagColor`
      after UI/UX review.
- [x] Decide whether to widen `LexState` for proper block-scalar
      indent tracking.
- [x] Decide whether flow-style folding (`{...}`, `[...]`) is worth
      the parser-level work.

### Phase 8 - Schema Validation Pipeline (Roadmap, Not Approved)

This phase exists to record the schema-validation direction proposed
in `1st-design-gemini.md` so it survives the MVP rollout. Approval
requires a separate cross-cutting design review per `AGENTS.md`
(lead `@core-architect`; reviewers `@feature-dev`, `@ui-ux-designer`,
`@qa-engineer`). Entries here are intentionally coarse — each one
expands into its own plan once approved.

- [ ] **Dependency decision.** Pick the YAML AST parser
      (SnakeYAML vs. snakeyaml-engine) and the JSON Schema validator
      (`networknt/json-schema-validator` vs. lighter alternative).
      Document footprint, license, and module-path compatibility
      with Java 25 + JavaFX 23.
- [ ] **Coordinate map (`YamlPositionIndex`).** Build a service that
      consumes the AST and exposes
      `JsonPointer → (line, column, length)` lookups. Lives in
      `papiflyfx-docking-code` initially; promote to
      `papiflyfx-docking-api` only if a second module needs it.
- [ ] **Validation runner.** Background pipeline (debounce ~300 ms,
      cancellable, reuses `IncrementalLexerPipeline` threading
      conventions) that produces a `List<Diagnostic>` keyed by
      coordinate map results. Marshal into the UI via
      `Platform.runLater`.
- [ ] **Diagnostic render pass.** Add a new `DiagnosticPass` (sibling
      of `TextPass`, `SearchPass`, `SelectionPass`) that paints
      squiggles on the canvas. Theme adds
      `diagnosticErrorColor`, `diagnosticWarningColor`, and
      `diagnosticInfoColor`. **Note:** The Gemini doc proposed CSS
      classes; this module is canvas-rendered, so we paint instead.
- [ ] **Problems panel content factory.** New dockable content built
      on `papiflyfx-docking-tree`, exposed through
      `ContentFactory` + `ContentStateAdapter`. Clicking an entry
      navigates the bound `CodeEditor` to the diagnostic line via
      its existing go-to-line action.
- [ ] **Schema discovery SPI.** Resolve schemas from (in order):
      explicit content-factory binding, modeline
      `# yaml-language-server: $schema=<url-or-path>`, file-name
      pattern, project setting. Defines the resolution interface and
      a built-in resolver bundle.
- [ ] **Cross-module wiring.** Document handoffs to
      `papiflyfx-docking-tree` (Problems panel host) and
      `papiflyfx-docking-settings-api` (schema-binding settings).
      Cross-cutting per `AGENTS.md` review gates.
- [ ] **Test plan.** Add a synthetic YAML + schema fixture set;
      verify diagnostics, coordinate accuracy, debounce behavior,
      cancellation under rapid edits, and Problems-panel
      navigation.
