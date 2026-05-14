# YAML Code Format Progress

Last updated: 2026-05-06  
Lead: `@spec-steward`

## Overall Status

- Phase 0: `completed`
- Phase 1: `completed`
- Phase 2: `completed`
- Phase 3: `completed`
- Phase 4: `completed`
- Phase 5: `completed`
- Phase 6: `automated-validation-completed`
- Phase 7: `completed`
- Phase 8: `roadmap` (schema validation, captured from `1st-design-gemini.md`, not approved)

## Phase 0 - Analysis and Planning

- [x] Surveyed existing built-in languages (`plain-text`, `java`,
      `javascript`, `json`, `markdown`) and confirmed YAML is missing.
- [x] Confirmed `TokenType` exposes generic categories (`STRING`,
      `JSON_KEY`, `BOOLEAN`, `NULL_LITERAL`, `NUMBER`, `COMMENT`,
      `OPERATOR`, `PUNCTUATION`) reusable for YAML scalars.
- [x] Confirmed `CodeEditorTheme` already exposes the colors needed
      for the first cut (`jsonKeyColor`, `stringColor`, `numberColor`,
      `commentColor`, `booleanColor`, `nullLiteralColor`).
- [x] Confirmed `BuiltInLanguageSupportProvider` is the registration
      surface for built-in languages and that no `META-INF/services`
      additions are required.
- [x] Confirmed `LexState` carries a single `int code`, which limits
      the fidelity of multi-line block-scalar tokenization in the MVP.
- [x] Drafted `README.md` and `plan.md` covering tokens, folding,
      theme, registration, and tests.

## Planned Implementation Progress

### Phase 1 - Token Model

- [x] Add `YAML_KEY` to `TokenType`.
- [x] Add `YAML_MAPPING` and `YAML_BLOCK_SCALAR` to `FoldKind`.

### Phase 2 - Lexer

- [x] Implement `YamlLexer` (`LANGUAGE_ID = "yaml"`) with default,
      double-quoted, single-quoted, and block-scalar state codes.
- [x] Cover comments, mapping keys, quoted/plain scalars, booleans,
      null variants, numeric forms, anchors, aliases, tags, the merge
      key, document markers, and block-scalar indicators.
- [x] Add `YamlLexerTest` matching the cases in `plan.md`.

### Phase 3 - Folding

- [x] Implement `YamlFoldProvider` driven by per-line indent and
      header classification (mapping header, sequence header, block
      scalar header).
- [x] Add `YamlFoldProviderTest` covering mapping, sequence, block
      scalar, and comment-tolerant regions.

### Phase 4 - Theme and Rendering

- [x] Route `YAML_KEY` to `CodeEditorTheme#jsonKeyColor` in
      `TextPass#tokenColor(...)`.
- [x] Add a `CodeEditorThemeMapperTest` regression that protects the
      visual distinction between mapping-key color and string/plain
      colors.

### Phase 5 - Language Registration

- [x] Register `yaml` in `BuiltInLanguageSupportProvider` with
      extensions `yaml`, `yml` and alias `yml`.
- [x] Add a registration smoke test (or extend an existing one) that
      resolves YAML by id, alias, and extension.

### Phase 6 - Validation

- [x] Focused test command:
      `./mvnw -pl papiflyfx-docking-code -am -Dtest=YamlLexerTest,YamlFoldProviderTest,JsonLexerTest,JsonFoldProviderTest,CodeEditorThemeMapperTest,LanguageSupportBootstrapTest -Dsurefire.failIfNoSpecifiedTests=false test`
- [x] Full headless suite:
      `./mvnw -pl papiflyfx-docking-code -am -Dtestfx.headless=true test`
- [x] Manual spot check inside the samples app on a realistic YAML
      document.
- [x] Record validation results below.

### Phase 7 - Optional Follow-ups

- [x] Decide on dedicated `yamlAnchorColor` / `yamlTagColor` after
      UI/UX review.
- [x] Decide on widening `LexState` for indent-aware block-scalar
      tokenization.
- [x] Decide on flow-style folding (`{...}`, `[...]`).

Decision: accepted in `phase7-decisions.md` and implemented. YAML now
has dedicated key, anchor/alias, and tag colors; `LexState` carries
block-scalar metadata; and `YamlFoldProvider` adds limited multi-line
flow-style folding for balanced `{...}` and `[...]` constructs.

### Phase 8 - Schema Validation Pipeline (Roadmap)

Captured from `1st-design-gemini.md`. Not approved; requires a
separate cross-cutting design review per `AGENTS.md` (lead
`@core-architect`).

- [ ] Pick YAML AST parser and JSON Schema validator dependencies
      (SnakeYAML vs. snakeyaml-engine; `networknt/json-schema-validator`
      vs. lighter alternative).
- [ ] Build `YamlPositionIndex` (JSON Pointer → coordinate map).
- [ ] Build background validation runner with debounce + cancellation.
- [ ] Add canvas-based `DiagnosticPass` and theme colors for
      error/warning/info squiggles. (Correction vs. Gemini doc:
      this module is canvas-painted, not CSS-styled, so squiggles
      are a render pass, not a CSS class.)
- [ ] Add Problems panel content factory backed by
      `papiflyfx-docking-tree`.
- [ ] Define schema discovery SPI (modeline, file-name pattern,
      project setting, explicit binding).
- [ ] Coordinate cross-module wiring with
      `papiflyfx-docking-tree` and `papiflyfx-docking-settings-api`.
- [ ] Author the dedicated test plan covering coordinate accuracy,
      debounce, cancellation, and Problems-panel navigation.

Decision: roadmap-only. Q5 in `README.md` (dynamic schema discovery)
must be answered before Phase 8 enters planning.

## Validation

- Focused tests:
  - Command: `./mvnw -pl papiflyfx-docking-code -am -Dtest=YamlLexerTest,YamlFoldProviderTest,CodeEditorThemeMapperTest,IncrementalLexerEngineTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Result: passed, 37 tests, 0 failures, 0 errors, 0 skipped
- Full headless code-module suite:
  - Command: `./mvnw -pl papiflyfx-docking-code -am -Dtestfx.headless=true test`
  - Result: passed, 442 tests, 0 failures, 0 errors, 0 skipped
- Samples app smoke:
  - Command: `./mvnw -pl papiflyfx-docking-samples -am -Dtest=SamplesSmokeTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test`
  - Result: passed, 15 tests, 0 failures, 0 errors, 0 skipped
- Change hygiene:
  - Command: `git diff --check`
  - Result: passed
  - Command: `gitnexus detect_changes --scope all`
  - Result: low risk, 14 changed files, no affected execution processes
- Manual spot check:
  - Document: realistic YAML demo document in SamplesApp
  - Observed: previously checked during the SamplesApp demo pass; no
    new interactive GUI run was performed for Phase 7.

## Outstanding Questions

Tracked in `README.md` "Open Questions for Reviewers" (Q1–Q5). Each
should be resolved before the corresponding phase closes:

- Q1 (key color reuse) resolved for the MVP by reusing
  `CodeEditorTheme#jsonKeyColor`; dedicated YAML colors remain a
  Phase 7 review item.
- Q2 (block scalar state fidelity) influences Phase 2 implementation
  detail but does not block the MVP shape. The implementation keeps
  the documented `LexState` approximation.
- Q3 (boolean variant coverage) resolved for the MVP by highlighting
  both YAML 1.2 `true`/`false` and YAML 1.1 `yes`/`no`/`on`/`off` in
  value position.
- Q4 (golden lexer fixtures) resolved for this pass with a synthetic
  super-set lexer fixture and Kubernetes-shaped folding fixture.
- Q5 (schema discovery model) blocks Phase 8 entering planning.

## Notes

Phase 7 accepted and implemented the richer YAML-only color palette,
the widened `LexState` envelope for block-scalar metadata, and limited
flow-style folding for balanced multi-line `{...}` and `[...]`
constructs. Parser-backed schema validation remains separate Phase 8
scope.

Phase 8 captures the schema-validation pipeline proposed in
`1st-design-gemini.md`. It is not part of the MVP; it is recorded so
the direction is preserved for a future cross-cutting review. The
Gemini doc's CSS-class assumption was corrected — squiggles must be
a canvas render pass since `papiflyfx-docking-code` does not style
token spans through CSS.
