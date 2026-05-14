# Prompt — JSON / YAML / Markdown Module Split

Date: 2026-05-09
Lead: `@spec-steward`
Implementation owner: `@feature-dev`
Required reviewers: `@core-architect`, `@ops-engineer`, `@ui-ux-designer`, `@qa-engineer`

## Context

`papiflyfx-docking-code` ships built-in language support for plain text, Java,
JavaScript, JSON, Markdown, and YAML. JSON, Markdown, and YAML lexers,
fold providers, and tests live alongside the core editor in a single Maven
module. The pluggable language SPI introduced in
[`spec/papiflyfx-docking-code-lang-plugin/`](spec/papiflyfx-docking-code-lang-plugin/README.md)
already exposes `LanguageSupport`, `LanguageSupportProvider`, and a
`LanguageSupportRegistry` that discovers providers via `ServiceLoader`. JSON,
Markdown, and YAML are still registered through the in-tree
`BuiltInLanguageSupportProvider`.

The user wants the JSON, YAML, and Markdown editing features to live in their
own Maven modules so the codebase is easier to navigate, review, and extend
without touching the core editor.

## Goal

Split JSON, YAML, and Markdown editor support out of `papiflyfx-docking-code`
into three new Maven modules:

- `papiflyfx-docking-code-json`
- `papiflyfx-docking-code-yaml`
- `papiflyfx-docking-code-markdown`

Each new module owns its language-specific lexer, fold provider, JUnit tests,
and a `LanguageSupportProvider` registered through `META-INF/services` so the
core editor discovers it at runtime. The core module retains plain text,
Java, and JavaScript support, the language SPI, the token vocabulary
(`TokenType`), the fold vocabulary (`FoldKind`), and the `CodeEditorTheme`
palette. No user-visible behavior should change: JSON, YAML, and Markdown
files must continue to highlight, fold, and theme exactly as they do today.

## Non-goals

1. Changing JSON, YAML, or Markdown lexer or folding semantics.
2. Introducing a theme-contribution SPI (`CodeEditorTheme` stays a record;
   `TextPass` token-color routing stays in core).
3. Splitting `TokenType`, `FoldKind`, or the language SPI types.
4. Splitting Java or JavaScript out of core (out of scope for this iteration).
5. Adding new editor features (search, schema validation, formatters).
6. Renaming public classes — package paths can move, but public class names
   stay stable so user code that imports
   `org.metalib.papifly.fx.code.lexer.JsonLexer` only needs a new Maven
   dependency, not a code change.

## Required deliverables in this folder

- [`README.md`](spec/papiflyfx-docking-code/2026-05-06-yaml-design/2026-05-09-json-yaml-md-modules/README.md) — original user brief (already present).
- [`prompt.md`](spec/papiflyfx-docking-code/2026-05-06-yaml-design/2026-05-09-json-yaml-md-modules/prompt.md) — this document.
- [`design.md`](spec/papiflyfx-docking-code/2026-05-06-yaml-design/2026-05-09-json-yaml-md-modules/design.md) — architectural design, including module surface,
  build topology, ServiceLoader wiring, theme/token decisions, and migration
  rules.
- [`plan.md`](spec/papiflyfx-docking-code/2026-05-06-yaml-design/2026-05-09-json-yaml-md-modules/plan.md) — phased implementation plan with concrete tasks,
  ordered for safe roll-out under the agent operating model.
- [`progress.md`](spec/papiflyfx-docking-code/2026-05-06-yaml-design/2026-05-09-json-yaml-md-modules/progress.md) — status tracker per phase, decision log,
  validation log, and risks.

## Constraints from `AGENTS.md`

1. New modules and `pom.xml` topology changes require `@ops-engineer`
   review (see review gates in `AGENTS.md`).
2. Shared-contract changes (the language SPI lives under
   `papiflyfx-docking-code` and is consumed across modules) require
   `@core-architect` review.
3. Test infrastructure changes (Surefire configuration, TestFX argLine,
   headless profile) require `@qa-engineer` review.
4. Visual regressions in JSON, YAML, or Markdown rendering require
   `@ui-ux-designer` review before approval.
5. Spec, plan, and progress edits require `@spec-steward` review.
6. Cross-cutting work follows the handoff contract format documented in
   `spec/agents/README.md`. Each phase identifies its lead role and
   reviewers.

## Acceptance criteria

1. Three new Maven modules exist, each with a `pom.xml` consistent with the
   existing content modules and a `README.md` describing its scope.
2. `papiflyfx-docking-code` no longer contains `JsonLexer`, `YamlLexer`,
   `MarkdownLexer`, `JsonFoldProvider`, `YamlFoldProvider`, or
   `MarkdownFoldProvider`, and `BuiltInLanguageSupportProvider` no longer
   registers `json`, `yaml`, or `markdown`.
3. `papiflyfx-docking-samples` continues to compile, run, and pass smoke
   tests with JSON, YAML, and Markdown highlighting and folding intact.
4. `./mvnw clean package` succeeds end-to-end on a clean checkout.
5. `./mvnw -Dtestfx.headless=true test` is green across the new modules and
   the core `papiflyfx-docking-code` module, including the relocated tests.
6. The root `pom.xml` lists the three new modules. `papiflyfx-docking-bom`,
   if updated, is internally consistent.
7. The change is documented: each new module has a `README.md`; the core
   `papiflyfx-docking-code/README.md` describes how language modules are
   discovered and how to add a new one.

## Out-of-scope follow-ups (track as roadmap, not blockers)

1. Theme-contribution SPI that lets language modules declare their own
   color keys instead of editing `CodeEditorTheme` in core. This would
   allow `TokenType` `JSON_KEY`, `YAML_KEY`, `YAML_ANCHOR`, `YAML_ALIAS`,
   `YAML_TAG`, `HEADLINE`, `LIST_ITEM`, and `CODE_BLOCK` to move out of
   `papiflyfx-docking-code` together with their owning modules.
2. Splitting Java and JavaScript into dedicated `papiflyfx-docking-code-java`
   and `papiflyfx-docking-code-javascript` modules using the same template.
3. Per-language settings (indent width, trailing-newline policy) sourced
   from `papiflyfx-docking-settings-api`.
