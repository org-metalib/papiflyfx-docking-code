# Prompt - Language Pack Follow-ups

Date: 2026-05-09
Lead: `@spec-steward`
Implementation owner: `@feature-dev`
Required reviewers: `@core-architect`, `@ops-engineer`, `@ui-ux-designer`, `@qa-engineer`
Source spec: [`../2026-05-09-json-yaml-md-modules/prompt.md`](../2026-05-09-json-yaml-md-modules/prompt.md)

## Context

The JSON / YAML / Markdown module split moved those languages out of
`papiflyfx-docking-code` into optional language-pack modules discovered
through `ServiceLoader`. That split intentionally kept `TokenType`,
`FoldKind`, `CodeEditorTheme`, `TextPass`, Java, JavaScript, and global
editor settings in the core editor module.

The source prompt recorded three out-of-scope follow-ups:

1. A theme-contribution SPI that lets language modules declare their own
   syntax color keys instead of editing `CodeEditorTheme` in core.
2. Dedicated `papiflyfx-docking-code-java` and
   `papiflyfx-docking-code-javascript` modules using the language-pack
   template.
3. Per-language editor settings, such as indent width and trailing-newline
   policy, sourced from `papiflyfx-docking-settings-api`.

This follow-up turns those roadmap items into one staged implementation plan.

## Goal

Make the code editor language-pack model complete enough that feature
languages own their lexers, fold providers, syntax style scopes, tests, and
language defaults without requiring a core editor edit for every language.

After this work:

- `papiflyfx-docking-code` owns the editor runtime, language SPI, plain-text
  fallback, generic token rendering, shared settings resolver, and common
  compatibility shims.
- JSON, YAML, Markdown, Java, and JavaScript each live in their own
  language-pack module with `LanguageSupportProvider` registration.
- Language modules can contribute syntax style scopes with dark/light default
  colors. `TextPass` resolves those dynamic scopes before falling back to
  generic `TokenType` colors.
- Per-language settings resolve from `papiflyfx-docking-settings-api` using
  stable keys and default values supplied by the language modules or core
  fallback policy.

## Non-goals

1. Rewriting lexers or fold providers beyond the changes needed to emit
   style scopes and move Java / JavaScript to modules.
2. Removing public `TokenType` enum constants or `CodeEditorTheme` accessors
   in a compatibility-breaking way in this iteration. Deprecated compatibility
   aliases can remain until a future major release.
3. Introducing schema validation, formatting engines, AST parsers, or LSP
   integration.
4. Adding a dynamic `FoldKind` registry. Fold kinds remain in core unless a
   later design proves a concrete need to make them extensible.
5. Changing settings storage backends, secret handling, login flows, or
   authentication behavior.

## Required Deliverables in This Folder

- [`README.md`](README.md) - original follow-up brief and document index.
- [`prompt.md`](prompt.md) - this implementation prompt.
- [`design.md`](design.md) - architectural design, API shape, module
  topology, syntax style scope model, settings key model, and migration rules.
- [`plan.md`](plan.md) - phased implementation plan with concrete tasks,
  ownership, reviewers, and validation strategy.
- [`progress.md`](progress.md) - status tracker, decision log, validation log,
  risk watch, and handoff notes.

## Constraints from `AGENTS.md`

1. Public editor API/SPI changes require `@core-architect` review.
2. Theme model, color defaults, visual rendering, and interaction polish
   require `@ui-ux-designer` review.
3. New Maven modules, BOM updates, sample dependencies, and archetype docs
   require `@ops-engineer` review.
4. Test strategy, migrated test coverage, headless profile behavior, and
   regression suites require `@qa-engineer` review.
5. Spec, plan, progress, and roadmap edits require `@spec-steward` review.
6. The work crosses multiple ownership domains, so exactly one lead remains
   `@spec-steward`; implementation phases hand off to owning specialists
   without splitting ownership of the same file set.

## Acceptance Criteria

1. A syntax style contribution SPI exists in `papiflyfx-docking-code`, is
   documented with Javadocs, and is discovered through `ServiceLoader`.
2. `Token` can carry an optional semantic style scope while preserving the
   existing `Token(int, int, TokenType)` construction path.
3. `TextPass` resolves token color in this order: contributed style scope,
   existing `TokenType` mapping, editor foreground fallback.
4. JSON, YAML, and Markdown modules declare their style scopes and emit them
   for their language-specific tokens. User-visible colors match the current
   default dark/light palettes.
5. `papiflyfx-docking-code-java` and `papiflyfx-docking-code-javascript`
   exist, own their lexers, fold providers, provider descriptors, tests, and
   README files, and are listed in the root reactor and BOM as appropriate.
6. `BuiltInLanguageSupportProvider` in core registers only plain text after
   Java and JavaScript move.
7. `papiflyfx-docking-samples` declares explicit dependencies on all shipped
   language-pack modules and still resolves `java`, `javascript`, `json`,
   `yaml`, `markdown`, and `plain-text`.
8. Per-language settings resolve through `papiflyfx-docking-settings-api`
   using documented keys and defaults. At minimum, `indentWidth`,
   `insertSpaces`, `ensureTrailingNewline`, and `trimTrailingWhitespace`
   have a resolver and tests.
9. Module README files, root `README.md`, `CLAUDE.md`, and relevant specs
   document the completed language-pack model.
10. Focused and full validation are green:
    `./mvnw -Dtestfx.headless=true test`,
    `./mvnw clean package`, and targeted language-pack tests.

## Roadmap Ordering

Implement the syntax style SPI before migrating token output, then split Java
and JavaScript modules, then add per-language settings. This keeps the highest
risk public API work visible before broad file moves.
