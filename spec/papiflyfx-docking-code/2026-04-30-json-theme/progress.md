# JSON Theme Highlighting Progress

Last updated: 2026-05-06  
Lead: `@spec-steward`

## Overall Status

- Phase 0: `completed`
- Phase 1: `completed`
- Phase 2: `completed`
- Phase 3: `deferred`
- Phase 4: `completed`
- UI/UX review follow-up: `completed`

## Phase 0 - Analysis and Planning

- [x] Reviewed current JSON highlighting implementation.
- [x] Identified that JSON keys and string values both use `TokenType.STRING`.
- [x] Confirmed `TextPass` does not map punctuation to a dedicated color.
- [x] Confirmed `CodeEditorTheme` does not expose JSON-key-specific color.
- [x] Created implementation plan and acceptance criteria.

## Planned Implementation Progress

### Phase 1 - Token Model

- [x] Add `JSON_KEY` to `TokenType`.
- [x] Update `JsonLexer` to classify object-key strings.
- [x] Add lexer regression coverage.

### Phase 2 - Theme and Rendering

- [x] Add `jsonKeyColor` to `CodeEditorTheme`.
- [x] Update default dark and light palettes.
- [x] Update `CodeEditorThemeMapper`.
- [x] Update `TextPass` token color routing.
- [x] Add theme mapper regression coverage.

### Phase 3 - Optional Punctuation Polish

- [x] Decide whether to add `punctuationColor` in this rollout.
- [ ] Implement punctuation rendering only if accepted by `@ui-ux-designer`.

Decision: deferred. This rollout keeps scope to JSON object keys and leaves punctuation color for visual review.

### Phase 4 - Validation

- [x] Run focused code-module tests.
- [x] Run full headless `papiflyfx-docking-code` tests.
- [x] Record validation results here.

### UI/UX Review Follow-up

- [x] Captured `@ui-ux-designer` review in `review-ui-ux-designer.md`.
- [x] Implemented F-01 test hardening from the review.
- [x] Added `CodeEditorThemeMapperTest#defaultJsonKeyColorsStayDistinctFromStringValues`.
- [x] Pinned default JSON key colors (`#9cdcfe` dark, `#0451a5` light).
- [x] Asserted JSON key colors remain distinct from string value colors.

## Validation

- Focused tests:
  - `./mvnw -pl papiflyfx-docking-code -am -Dtest=JsonLexerTest,CodeEditorThemeMapperTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Result: success (`13` tests, `0` failures, `0` errors)
- Full headless code-module suite:
  - `./mvnw -pl papiflyfx-docking-code -am -Dtestfx.headless=true test`
  - Result: success (`416` tests, `0` failures, `0` errors)
- UI/UX follow-up focused test:
  - `./mvnw -pl papiflyfx-docking-code -am -Dtest=CodeEditorThemeMapperTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Result: success (`10` tests, `0` failures, `0` errors)
- GitNexus change detection:
  - `gitnexus_detect_changes(scope=all)`
  - Result: low risk, no affected processes

## Notes

Recommended first implementation slice: `TokenType.JSON_KEY` plus `JsonLexer` classification and `TextPass` rendering. Punctuation color should remain optional until the visual review confirms it improves readability without adding noise.

`git diff --check` passes for the JSON-theme files touched by this work.
