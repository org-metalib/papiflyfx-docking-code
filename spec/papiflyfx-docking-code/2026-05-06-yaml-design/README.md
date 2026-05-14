# YAML Code Format Design

Date: 2026-05-06  
Lead: `@spec-steward`  
Implementation owner: `@feature-dev`  
Required reviewers: `@ui-ux-designer`, `@qa-engineer`, `@core-architect`

## Summary

`papiflyfx-docking-code` ships built-in language support for plain text, Java,
JavaScript, JSON, and Markdown. YAML is a recurring file format across the
project (Maven POMs are XML, but settings, CI definitions, Hugo front matter,
Kubernetes manifests, and similar configuration documents are typically YAML)
and is not yet recognized: `.yml`/`.yaml` files open as plain text without
syntax highlighting, structural folding, or key/value differentiation.

This design adds a first-class `yaml` language to the code editor. It follows
the same shape as the existing language modules — a `Lexer`, a `FoldProvider`,
a registration in `BuiltInLanguageSupportProvider`, and `CodeEditorTheme`
routing through `TextPass`. The goal is a focused, lexer-friendly highlighter
that covers the YAML constructs developers actually read in configuration
files, without pulling in a YAML parser.

## Current Implementation

- Built-in language discovery lives in
  `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/folding/BuiltInLanguageSupportProvider.java`
  and registers `plain-text`, `java`, `javascript`, `json`, and `markdown`.
  YAML is not present.
- `TokenType` exposes `STRING`, `JSON_KEY`, `KEYWORD`, `BOOLEAN`,
  `NULL_LITERAL`, `NUMBER`, `COMMENT`, `PUNCTUATION`, `OPERATOR`,
  `IDENTIFIER`, plus markdown-specific roles. There is no token for YAML
  mapping keys, anchors/aliases, tags, or document markers.
- `JsonFoldProvider` handles brace/bracket folding for JSON. There is no
  indentation-aware folding for YAML.
- `CodeEditorTheme` already exposes `jsonKeyColor`, `stringColor`,
  `commentColor`, `numberColor`, `booleanColor`, and `nullLiteralColor`.
  These are reusable for YAML scalars; a YAML-specific key color is not
  required for the first cut.
- `TextPass#tokenColor(...)` maps the existing token categories above. A
  YAML-only token (e.g. `YAML_KEY`) needs an explicit case here.
- No `papiflyfx-docking-code/src/main/resources/META-INF/services` provider
  changes are needed — built-in languages are wired through
  `BuiltInLanguageSupportProvider`.

## Recommended Visual Improvement

Add a `yaml` language with the following distinguishing colors:

1. **Mapping keys** rendered through the existing `jsonKeyColor` so that
   block keys (`name:`) and flow keys (`{ name: Ada }`) are visually distinct
   from string and scalar values without growing the theme palette in the
   first cut.
2. **Comments** (`# ...` to end of line) rendered through `commentColor`.
3. **Booleans** (`true`/`false` and the YAML 1.1 forms `yes`/`no`/`on`/`off`
   when in a value position) rendered through `booleanColor`.
4. **Null literals** (`null`, `Null`, `NULL`, `~`) rendered through
   `nullLiteralColor`.
5. **Numbers** — integers, floats, scientific notation, hex (`0x...`), octal
   (`0o...`), and the YAML special floats (`.inf`, `-.inf`, `+.inf`, `.nan`)
   — rendered through `numberColor`.
6. **Quoted strings** — both single-quoted (`'...'`) and double-quoted
   (`"..."`) — rendered through `stringColor`.
7. **Document markers** (`---`, `...` at column 0) and block-scalar
   indicators (`|`, `|+`, `|-`, `>`, `>+`, `>-`) emitted as `PUNCTUATION`
   to avoid claiming a new theme color.

Anchors (`&name`), aliases (`*name`), the merge key (`<<`), and tags
(`!!str`, `!CustomTag`) should be tokenized as `OPERATOR` for the first
cut so they are visible without introducing dedicated theme colors.

## Folding

Add `YamlFoldProvider` that builds fold regions from indentation:

1. A line whose first non-whitespace character is `-` (block sequence
   item) or whose trimmed text matches `<key>:` (block mapping header)
   opens a fold whose body is the contiguous run of following lines whose
   leading indent is strictly greater than the header's indent.
2. Block scalars introduced by `|` or `>` fold their indented body the
   same way.
3. Flow style `{...}` and `[...]` are out of scope for the first cut.
   They are uncommon in YAML configuration documents and would otherwise
   require parser-level matching of nested flow.

Two new `FoldKind` values:

- `YAML_MAPPING` — block mapping or sequence indented under a header.
- `YAML_BLOCK_SCALAR` — `|`/`>` indented text body.

## Non-Goals

- No YAML 1.2 schema validation, type inference, or AST in the MVP.
  The lexer is line-oriented and best-effort. Schema validation is
  captured separately under "Future Direction: Schema Validation
  Pipeline" below.
- No new editor render passes for diagnostic squiggles, no Problems
  panel, no SnakeYAML/Jackson/`networknt/json-schema-validator`
  dependencies. All deferred to the same follow-up.
- No flow-style mapping/sequence folding (`{...}`, `[...]`) in the first
  cut. Defer until a future iteration if developers need it.
- No anchor/alias resolution or merge-key expansion.
- No directive parsing (`%YAML 1.2`, `%TAG ...`).
- No new theme colors. `yamlKeyColor`, `yamlAnchorColor`, and similar
  additions stay deferred until a UI/UX review confirms the shared
  palette is insufficient.
- No changes to existing JSON, Java, JavaScript, Markdown, or plain-text
  token behavior.
- No new file-association UI surface — built-in registration in
  `BuiltInLanguageSupportProvider` is enough; user overrides flow through
  the existing `UserFileAssociationMapping` plumbing.

## Future Direction: Schema Validation Pipeline

Captured from the alternative design in `1st-design-gemini.md` and
recorded here for traceability. **This is roadmap material, not MVP
scope.** The MVP described above ships syntax highlighting and
folding only.

A follow-up feature could layer JSON-Schema-driven validation on top
of the YAML lexer to surface structural errors (squiggles, hover
diagnostics, a Problems panel). Recommended architecture if that
work is approved:

1. **Unidirectional pipeline.** State holds the raw YAML text, the
   active schema, and the current diagnostic set. Editing produces an
   action; a background reducer parses + validates; the UI thread
   applies diagnostic decorations and updates a Problems panel.
2. **Coordinate map (Pass 1).** Parse the buffer with **SnakeYAML**
   to obtain an AST whose nodes carry `Mark` line/column info, then
   build a `JsonPointer → (line, column, length)` map.
3. **Validation (Pass 2).** Convert the parsed YAML to a `JsonNode`
   (Jackson) and run **`networknt/json-schema-validator`**. Each
   reported error carries a JSON Pointer; join through the coordinate
   map to obtain editor coordinates.
4. **Reactive execution.** Debounce keystrokes (~300 ms), run AST
   mapping and validation off the JavaFX Application Thread (the
   existing `IncrementalLexerPipeline` already runs incrementally on
   a worker), and marshal the diagnostic set back through
   `Platform.runLater`.
5. **Editor integration.** `papiflyfx-docking-code` is **canvas-based**,
   not CSS-styled, so squiggles must be drawn through a new
   `RenderPass` (alongside `TextPass`, `SearchPass`, `SelectionPass`)
   that consumes the diagnostic set. This is a notable correction to
   the Gemini doc's CSS-class assumption.
6. **Problems panel.** A new dockable content backed by
   `papiflyfx-docking-tree` (or a purpose-built leaf), wired through
   the standard `ContentFactory` + `ContentStateAdapter` pattern,
   navigates to the offending line on click. This module currently
   has no Problems panel; it would be a new SPI surface.
7. **Dirty/saved indicator.** Tab dirty markers are a docking-shell
   concern, not a YAML concern. Treat as a separate enhancement to
   `DockManager`/`DockTabGroup` if it is not already covered.
8. **Cross-cutting agents.** Per `AGENTS.md`, schema validation
   touches `papiflyfx-docking-code`, `papiflyfx-docking-tree`, and
   shared SPI. It is a cross-cutting change requiring a named lead
   (`@core-architect`) and reviews from `@feature-dev`,
   `@ui-ux-designer`, and `@qa-engineer`.

Tradeoffs the follow-up will need to settle:

- **Dependencies.** SnakeYAML, Jackson, and
  `networknt/json-schema-validator` would be the first heavy parser
  dependencies inside `papiflyfx-docking-code`. The MVP's lexer
  approach intentionally avoids them; the follow-up must justify
  their footprint or pick lighter-weight alternatives (e.g.
  snakeyaml-engine, `jakarta.json` + a lightweight schema impl).
- **Lexer reuse.** The validation pipeline reads the buffer through
  SnakeYAML, not through `YamlLexer`. The lexer continues to drive
  highlighting, while the validator drives diagnostics. Sharing
  results (e.g. lexer-discovered ranges feeding diagnostic anchors)
  is an optimization, not a requirement.
- **Schema discovery.** See Q5 below.

## Open Questions for Reviewers

- **Q1 (`@ui-ux-designer`)**: Reuse `jsonKeyColor` for YAML mapping keys,
  or add a dedicated `yamlKeyColor`? Reuse keeps the palette small and
  ties "structural key" to one stable color across formats; a dedicated
  color would let the two languages diverge later.
- **Q2 (`@core-architect`)**: Is the line-oriented lexer with
  per-line state code sufficient, or should `LexState` be widened to
  carry indent context for proper multi-line block-scalar tokenization?
  The MVP encodes "in block scalar" as a state code but does not carry
  the introducing indent, so block-scalar end detection is heuristic.
- **Q3 (`@feature-dev`)**: Should boolean variants `yes`/`no`/`on`/`off`
  be highlighted by default (YAML 1.1 spec) or only `true`/`false`
  (YAML 1.2 spec)? Default proposed: highlight both, since project
  configuration files commonly use either set.
- **Q4 (`@qa-engineer`)**: What sample YAML documents should be used as
  golden lexer fixtures — the `application.yml` style, Kubernetes
  manifest style, GitHub Actions workflow style, Hugo front matter, or
  a synthetic super-set? Default proposed: a synthetic super-set plus
  one realistic Kubernetes-shaped fixture.
- **Q5 (`@core-architect`, schema follow-up only)**: If the schema
  validation follow-up is approved, should the schema be discovered
  dynamically (e.g. from a leading `# yaml-language-server: $schema=...`
  modeline, file-name patterns, or an explicit project setting) or
  bound statically per content factory? Dynamic discovery is the
  industry convention but requires a schema-resolution SPI.

See `plan.md` for the phased implementation breakdown and
`progress.md` for the live progress tracker.