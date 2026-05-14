# Plan - Language Pack Follow-ups

Date: 2026-05-09
Lead: `@spec-steward`
Implementation owner: `@feature-dev`
Required reviewers: `@core-architect`, `@ops-engineer`, `@ui-ux-designer`, `@qa-engineer`
Linked prompt: [`prompt.md`](prompt.md)
Linked design: [`design.md`](design.md)

## Problem Statement

The JSON / YAML / Markdown language-pack split improved module boundaries
but left three deliberate gaps: syntax colors are still hardwired in core,
Java and JavaScript still live in the core editor module, and editor settings
are global rather than language-aware. This plan addresses those gaps while
preserving source compatibility where public APIs already exist.

## Phasing Overview

| Phase | Name | Lead | Reviewers |
| ----- | ---- | ---- | --------- |
| 0 | Pre-flight inventory | `@spec-steward` | `@feature-dev`, `@core-architect`, `@ops-engineer` |
| 1 | Syntax style SPI in core | `@core-architect` | `@feature-dev`, `@ui-ux-designer`, `@qa-engineer` |
| 2 | Migrate JSON / YAML / Markdown style scopes | `@feature-dev` | `@core-architect`, `@ui-ux-designer`, `@qa-engineer` |
| 3 | Split Java language pack | `@feature-dev` | `@core-architect`, `@ops-engineer`, `@qa-engineer` |
| 4 | Split JavaScript language pack | `@feature-dev` | `@core-architect`, `@ops-engineer`, `@qa-engineer` |
| 5 | Per-language settings | `@feature-dev` | `@ops-engineer`, `@ui-ux-designer`, `@qa-engineer` |
| 6 | Documentation and build topology sweep | `@spec-steward` | `@feature-dev`, `@ops-engineer`, `@ui-ux-designer` |
| 7 | Validation and acceptance gate | `@qa-engineer` | `@core-architect`, `@feature-dev`, `@ops-engineer`, `@ui-ux-designer` |

Phases are sequential by default. Do not move Java / JavaScript before Phase 1
lands, because migrated lexers should target the new style-scope token shape.

## Implementation Status

As of 2026-05-09, Phases 0-7 are implemented and validated. Detailed command
results, decisions, and reviewer handoff notes are recorded in
[`progress.md`](progress.md).

| Phase | Status | Notes |
| ----- | ------ | ----- |
| 0 | completed | Inventory informed the final SPI and module topology. |
| 1 | completed | Syntax style SPI and compatibility shims are in core. |
| 2 | completed | JSON, YAML, and Markdown emit module-owned style scopes. |
| 3 | completed | Java lives in `papiflyfx-docking-code-java`. |
| 4 | completed | JavaScript lives in `papiflyfx-docking-code-javascript`. |
| 5 | completed | Per-language settings resolve through settings API storage. |
| 6 | completed | Module, BOM, samples, README, and spec docs are updated. |
| 7 | completed | Targeted, samples, full headless, clean package, and grep audits passed. |

## Phase 0 - Pre-flight Inventory

Goal: confirm this design matches the current codebase before public API work.

- [ ] 0.1 Inspect `Token`, `TokenType`, `CodeEditorTheme`,
      `CodeEditorThemeMapper`, and `TextPass`; record current constructors,
      fields, and test coverage in `progress.md`.
- [ ] 0.2 Inspect `LanguageSupport.customTokenScopes()` and registry
      behavior; decide whether scope validation ties into
      `LanguageSupportRegistry` or a separate `SyntaxStyleRegistry`.
- [ ] 0.3 Inventory Java and JavaScript source/test files and all direct
      references outside `BuiltInLanguageSupportProvider`.
- [ ] 0.4 Inventory settings code in `EditorCategory`,
      `EditorSettingsSupport`, settings API records, and settings UI control
      support.
- [ ] 0.5 Snapshot current tests for Java, JavaScript, JSON, YAML, Markdown,
      theme mapper, language registry, and samples discovery.

Exit criteria: `progress.md` lists the inventories, direct consumers, and
test counts.

## Phase 1 - Syntax Style SPI in Core

Goal: introduce a compatibility-preserving semantic style path.

- [ ] 1.1 Add `SyntaxStyleScope`, `SyntaxStyleProvider`, and
      `SyntaxStyleRegistry` in `papiflyfx-docking-code`.
- [ ] 1.2 Add a ServiceLoader descriptor path and tests with a test provider.
- [ ] 1.3 Extend `Token` with optional `styleScope` and keep the existing
      three-argument constructor.
- [ ] 1.4 Add a dynamic scope color map to `CodeEditorTheme` with
      compatibility constructors or factories.
- [ ] 1.5 Update `CodeEditorThemeMapper` to merge `SyntaxStyleRegistry`
      defaults into the dynamic scope map.
- [ ] 1.6 Update `TextPass` so `styleScope` colors win before `TokenType`
      fallback.
- [ ] 1.7 Add tests for token construction, registry discovery, theme mapping,
      and render color resolution.
- [ ] 1.8 Run
      `./mvnw -pl papiflyfx-docking-code -am -Dtest=TokenTest,SyntaxStyleRegistryTest,CodeEditorThemeMapperTest test`
      with the actual test names used by implementation.

Exit criteria: core tests pass and existing token-color behavior is unchanged
when no style scopes are present.

## Phase 2 - JSON / YAML / Markdown Style Scope Migration

Goal: move existing language-specific syntax colors to module-owned style
scopes.

- [ ] 2.1 Add `JsonSyntaxStyleProvider`, `YamlSyntaxStyleProvider`, and
      `MarkdownSyntaxStyleProvider` with ServiceLoader descriptors.
- [ ] 2.2 Update each provider's `LanguageSupport.customTokenScopes()` to
      list the scopes owned by the module.
- [ ] 2.3 Update JSON lexer output for object keys to emit `styleScope =
      "json.key"` with `TokenType.STRING` fallback.
- [ ] 2.4 Update YAML lexer output for keys, anchors, aliases, and tags to
      emit `yaml.*` scopes with generic fallbacks.
- [ ] 2.5 Update Markdown lexer output for headlines, list items, and fenced
      code blocks to emit `markdown.*` scopes with generic fallbacks.
- [ ] 2.6 Keep old language-specific `TokenType` constants available. Mark
      them deprecated only after reviewer approval.
- [ ] 2.7 Add per-module style provider tests and update lexer assertions.
- [ ] 2.8 Run targeted tests for the three language modules and core theme
      mapper.

Exit criteria: JSON, YAML, and Markdown render with the same dark/light
colors, but their language-specific colors are contributed by their modules.

## Phase 3 - Java Language Pack

Goal: move Java implementation out of core using the established template.

- [ ] 3.1 Create `papiflyfx-docking-code-java/` with `pom.xml`, `README.md`,
      source/resource/test layout, and ServiceLoader descriptor.
- [ ] 3.2 Move `JavaLexer`, `JavaFoldProvider`, and their tests while
      preserving package paths.
- [ ] 3.3 Add `JavaLanguageSupportProvider` with id `java`, extension
      `java`, and existing factories.
- [ ] 3.4 Remove Java registration and imports from
      `BuiltInLanguageSupportProvider`.
- [ ] 3.5 Add provider and discovery tests.
- [ ] 3.6 Add root reactor module entry and BOM entry if applicable.
- [ ] 3.7 Run
      `./mvnw -pl papiflyfx-docking-code-java,papiflyfx-docking-code -am test`.

Exit criteria: Java language support is discovered only when the Java module
is on the classpath; direct imports work after adding the new dependency.

## Phase 4 - JavaScript Language Pack

Goal: move JavaScript implementation out of core using the Java phase as the
template.

- [ ] 4.1 Create `papiflyfx-docking-code-javascript/` with matching layout.
- [ ] 4.2 Move `JavaScriptLexer`, `JavaScriptFoldProvider`, and tests while
      preserving package paths.
- [ ] 4.3 Add `JavaScriptLanguageSupportProvider` with id `javascript`,
      alias `js`, and extensions `js`, `mjs`, `cjs`.
- [ ] 4.4 Remove JavaScript registration and imports from
      `BuiltInLanguageSupportProvider`.
- [ ] 4.5 Add provider and discovery tests.
- [ ] 4.6 Add root reactor module entry and BOM entry if applicable.
- [ ] 4.7 Run
      `./mvnw -pl papiflyfx-docking-code-javascript,papiflyfx-docking-code-java,papiflyfx-docking-code -am test`.

Exit criteria: JavaScript support is module-discovered and all moved tests
retain their current coverage.

## Phase 5 - Per-language Settings

Goal: make editor preferences language-aware while using the existing settings
API.

- [ ] 5.1 Add `LanguageEditorSettings` and
      `LanguageEditorSettingsResolver` in `papiflyfx-docking-code`.
- [ ] 5.2 Define and document keys under `editor.language.default.*` and
      `editor.language.<id>.*`.
- [ ] 5.3 Add editor properties for `indentWidth`, `insertSpaces`,
      `ensureTrailingNewline`, and `trimTrailingWhitespace`.
- [ ] 5.4 Update `EditorSettingsSupport.applyDefaults(...)` to resolve
      settings for the editor's current language and re-apply when language
      changes.
- [ ] 5.5 Update `EditorCategory` UI with global defaults and an installed
      language section generated from `LanguageSupportRegistry`.
- [ ] 5.6 If behavior is implemented, wire Tab/newline indentation to the
      resolved indentation settings. If save behavior is not available,
      document trailing-newline and trimming as resolved preferences only.
- [ ] 5.7 Add resolver tests, editor property tests, and settings UI tests
      where practical.
- [ ] 5.8 Run
      `./mvnw -pl papiflyfx-docking-code,papiflyfx-docking-settings,papiflyfx-docking-samples -am -Dtestfx.headless=true test`.

Exit criteria: language-specific settings override defaults in storage and
apply to editor properties deterministically.

## Phase 6 - Documentation and Build Topology Sweep

Goal: make the completed language-pack model discoverable.

- [ ] 6.1 Add Java and JavaScript dependencies to `papiflyfx-docking-samples`
      and update samples discovery tests.
- [ ] 6.2 Audit `papiflyfx-docking-bom`, root `pom.xml`, and archetype docs
      for the new module list.
- [ ] 6.3 Update `papiflyfx-docking-code/README.md` with syntax scopes,
      language-pack modules, and per-language settings keys.
- [ ] 6.4 Update every language-pack README with style scopes and settings
      defaults.
- [ ] 6.5 Update root `README.md`, `CLAUDE.md`, and
      `spec/papiflyfx-docking-code/README.md`.
- [ ] 6.6 Cross-link this folder from
      `spec/papiflyfx-docking-code-lang-plugin/README.md`.
- [ ] 6.7 Record final implementation decisions in `progress.md`.

Exit criteria: docs point authors to the new complete language-pack template.

## Phase 7 - Validation and Acceptance Gate

Goal: prove the full reactor and user-visible behavior remain stable.

- [ ] 7.1 `./mvnw clean package`
- [ ] 7.2 `./mvnw -Dtestfx.headless=true test`
- [ ] 7.3 Targeted language-pack run:
      `./mvnw -pl papiflyfx-docking-code,papiflyfx-docking-code-java,papiflyfx-docking-code-javascript,papiflyfx-docking-code-json,papiflyfx-docking-code-yaml,papiflyfx-docking-code-markdown -am test`
- [ ] 7.4 Samples run:
      `./mvnw -pl papiflyfx-docking-samples -am -Dtestfx.headless=true test`
- [ ] 7.5 Grep audit confirms Java / JavaScript lexers and fold providers no
      longer live in core, and core built-ins register only `plain-text`.
- [ ] 7.6 Visual smoke check in `SamplesApp` for Java, JavaScript, JSON,
      YAML, and Markdown in dark and light themes.
- [ ] 7.7 Reviewer sign-offs:
      - [ ] `@core-architect`
      - [ ] `@ops-engineer`
      - [ ] `@ui-ux-designer`
      - [ ] `@qa-engineer`

Exit criteria: all validation is recorded in `progress.md`, all reviewers
sign off, and the initiative status is marked `completed`.

## Rollback Plan

1. Phase 1 is the only public API-heavy step. Keep it isolated and green
   before moving language modules.
2. Phases 3 and 4 are file-level moves into new modules; each can be reverted
   independently if a classpath or packaging issue appears.
3. Phase 5 settings behavior must be guarded by defaults. If settings storage
   is unavailable, editor behavior falls back to current defaults.
4. Documentation changes are reversible and should be updated whenever an
   implementation decision diverges.

## Done Definition

This plan is done when every phase exit criterion is satisfied,
[`progress.md`](progress.md) is current, automated validation is green, visual
smoke is recorded, and required reviewer sign-offs are captured.
