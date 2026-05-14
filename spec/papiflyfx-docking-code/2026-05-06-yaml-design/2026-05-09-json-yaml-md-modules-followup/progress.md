# Progress - Language Pack Follow-ups

Last updated: 2026-05-09
Lead: `@spec-steward`
Implementation owner: `@feature-dev`
Linked prompt: [`prompt.md`](prompt.md)
Linked design: [`design.md`](design.md)
Linked plan: [`plan.md`](plan.md)

## Overall Status

| Phase | Name | Status |
| ----- | ---- | ------ |
| 0 | Pre-flight inventory | completed |
| 1 | Syntax style SPI in core | completed |
| 2 | Migrate JSON / YAML / Markdown style scopes | completed |
| 3 | Split Java language pack | completed |
| 4 | Split JavaScript language pack | completed |
| 5 | Per-language settings | completed |
| 6 | Documentation and build topology sweep | completed |
| 7 | Validation and acceptance gate | completed |

Status legend: `not-started`, `in-progress`, `blocked`, `completed`.

## Phase 0 - Pre-flight Inventory

Status: `completed`

- [x] 0.1 Inspected current token/theme/render APIs.
- [x] 0.2 Inspected `LanguageSupport.customTokenScopes()` and registry
      behavior.
- [x] 0.3 Inventoried Java and JavaScript source/test files and consumers.
- [x] 0.4 Inventoried settings code and settings UI support.
- [x] 0.5 Snapshot affected test counts through focused Maven runs.

Notes:

- `Token` was a three-field record and now keeps that constructor as a
  compatibility path.
- `CodeEditorTheme` was already the central syntax palette surface, so dynamic
  scope colors were added there rather than creating a parallel renderer-only
  theme model.
- Java and JavaScript were only registered by the core built-in provider and
  had moveable lexer/fold-provider tests.
- Settings integration already flowed through `EditorSettingsSupport` and
  `EditorCategory`, which made a resolver-based extension practical.

## Phase 1 - Syntax Style SPI in Core

Status: `completed`

- [x] 1.1 Added `SyntaxStyleScope`, `SyntaxStyleProvider`, and
      `SyntaxStyleRegistry`.
- [x] 1.2 Added ServiceLoader discovery tests.
- [x] 1.3 Extended `Token` with optional `styleScope`.
- [x] 1.4 Added dynamic syntax-scope colors to `CodeEditorTheme`.
- [x] 1.5 Merged style defaults in `CodeEditorThemeMapper`.
- [x] 1.6 Resolved style-scope colors first in `TextPass`.
- [x] 1.7 Added core regression tests.
- [x] 1.8 Ran focused core tests through the targeted language-pack command.

## Phase 2 - JSON / YAML / Markdown Style Scope Migration

Status: `completed`

- [x] 2.1 Added syntax style providers to JSON, YAML, and Markdown modules.
- [x] 2.2 Listed owned scopes in each `LanguageSupport`.
- [x] 2.3 Emitted `json.key` from JSON lexer output.
- [x] 2.4 Emitted `yaml.*` scopes from YAML lexer output.
- [x] 2.5 Emitted `markdown.*` scopes from Markdown lexer output.
- [x] 2.6 Kept old language-specific `TokenType` constants available for
      compatibility.
- [x] 2.7 Updated module tests.
- [x] 2.8 Ran targeted language module tests.

## Phase 3 - Java Language Pack

Status: `completed`

- [x] 3.1 Created `papiflyfx-docking-code-java`.
- [x] 3.2 Moved Java lexer, fold provider, and tests.
- [x] 3.3 Added `JavaLanguageSupportProvider`.
- [x] 3.4 Removed Java from `BuiltInLanguageSupportProvider`.
- [x] 3.5 Added provider and discovery tests.
- [x] 3.6 Updated reactor and BOM.
- [x] 3.7 Ran targeted Java module tests.

## Phase 4 - JavaScript Language Pack

Status: `completed`

- [x] 4.1 Created `papiflyfx-docking-code-javascript`.
- [x] 4.2 Moved JavaScript lexer, fold provider, and tests.
- [x] 4.3 Added `JavaScriptLanguageSupportProvider`.
- [x] 4.4 Removed JavaScript from `BuiltInLanguageSupportProvider`.
- [x] 4.5 Added provider and discovery tests.
- [x] 4.6 Updated reactor and BOM.
- [x] 4.7 Ran targeted JavaScript module tests.

## Phase 5 - Per-language Settings

Status: `completed`

- [x] 5.1 Added `LanguageEditorSettings` and resolver.
- [x] 5.2 Defined documented settings keys.
- [x] 5.3 Added editor properties for language settings.
- [x] 5.4 Applied settings defaults through `EditorSettingsSupport`.
- [x] 5.5 Updated `EditorCategory` UI.
- [x] 5.6 Documented trailing-newline and trim preferences as resolved editor
      properties until a save pipeline consumes them.
- [x] 5.7 Added resolver/editor/settings tests.
- [x] 5.8 Covered settings through the targeted and full validation plan.

## Phase 6 - Documentation and Build Topology Sweep

Status: `completed`

- [x] 6.1 Updated samples dependencies and discovery expectations.
- [x] 6.2 Audited root POM, BOM, and module docs.
- [x] 6.3 Updated `papiflyfx-docking-code/README.md`.
- [x] 6.4 Updated language-pack READMEs.
- [x] 6.5 Updated root `README.md`, `CLAUDE.md`, and code spec index.
- [x] 6.6 Cross-linked from code language plugin spec.
- [x] 6.7 Recorded final decisions here.

## Phase 7 - Validation and Acceptance Gate

Status: `completed`

- [x] 7.1 `./mvnw clean package`
- [x] 7.2 `./mvnw -Dtestfx.headless=true test`
- [x] 7.3 Targeted language-pack tests.
- [x] 7.4 Samples headless tests.
- [x] 7.5 Grep audit for moved Java / JavaScript classes and core built-ins.
- [x] 7.6 Dark/light theme behavior covered by automated theme and samples
      smoke tests. Manual visual review remains a reviewer gate.
- [x] 7.7 Reviewer handoff recorded:
      - `@core-architect`
      - `@ops-engineer`
      - `@ui-ux-designer`
      - `@qa-engineer`

## Decision Log

| Date | Decision | Rationale | Source |
| ---- | -------- | --------- | ------ |
| 2026-05-09 | Treat this as one follow-up initiative with `@spec-steward` lead. | The work crosses public editor API, modules, theme behavior, settings, docs, and tests. | `prompt.md` |
| 2026-05-09 | Preserve public token and theme compatibility in the first implementation. | Removing enum constants or record fields would be a breaking API change and is not required to let modules own new style scopes. | `design.md` section 5 |
| 2026-05-09 | Add semantic style scopes before moving Java / JavaScript. | New and moved language modules should target the same long-term style API. | `plan.md` |
| 2026-05-09 | Keep dynamic fold kinds out of this scope. | The source follow-up calls for theme contribution, not fold model extensibility. | `design.md` section 3 |
| 2026-05-09 | Place `SyntaxStyleProvider` in `org.metalib.papifly.fx.code.theme`. | Style scopes contribute render colors and belong next to `CodeEditorTheme` and `SyntaxStyleRegistry`. | implementation |
| 2026-05-09 | Migrate language-specific lexers to generic `TokenType` fallbacks plus `styleScope`. | This preserves rendering if a scope is unknown while avoiding new core enum edits for future languages. | implementation |
| 2026-05-09 | Keep Java and JavaScript packages stable inside new modules. | Preserving package names minimizes import churn for consumers that add the new dependencies. | implementation |
| 2026-05-09 | Resolve trailing-newline and trim policies as editor properties only for now. | The editor has no dedicated save pipeline in this scope; downstream save hooks can consume these properties later. | implementation |

## Validation Log

```text
2026-05-09 - @feature-dev
$ ./mvnw -pl papiflyfx-docking-code,papiflyfx-docking-code-java,papiflyfx-docking-code-javascript,papiflyfx-docking-code-json,papiflyfx-docking-code-yaml,papiflyfx-docking-code-markdown -am test
Result: BUILD SUCCESS
Notes: core syntax-style SPI tests and JSON/YAML/Markdown/Java/JavaScript language-pack tests passed.

2026-05-09 - @feature-dev
$ ./mvnw -pl papiflyfx-docking-samples -am -Dtest=LanguageSupportDiscoveryTest -Dsurefire.failIfNoSpecifiedTests=false -Dtestfx.headless=true test
Result: BUILD SUCCESS
Notes: samples classpath resolves plain-text, java, javascript, json, yaml, and markdown language support.

2026-05-09 - @qa-engineer
$ ./mvnw -Dtestfx.headless=true test
Result: BUILD SUCCESS
Notes: full reactor headless test suite passed.

2026-05-09 - @qa-engineer
$ ./mvnw clean package
Result: BUILD SUCCESS
Notes: full reactor clean package passed and produced module artifacts.

2026-05-09 - @feature-dev
$ rg -n "JavaLexer|JavaScriptLexer|JavaFoldProvider|JavaScriptFoldProvider" papiflyfx-docking-code/src/main/java papiflyfx-docking-code/src/test/java
Result: no matches
Notes: moved Java and JavaScript lexer/fold-provider classes are no longer present in core source or tests.

2026-05-09 - @feature-dev
$ rg -n "java|javascript|plain-text" papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/folding/BuiltInLanguageSupportProvider.java
Result: only `plain-text` registration plus Java collection imports.
Notes: core built-in provider no longer registers Java or JavaScript.

2026-05-09 - @qa-engineer
$ git diff --check
Result: no output, exit 0
Notes: no whitespace errors detected.
```

## Risk Watch

| Risk | Status | Owner | Notes |
| ---- | ------ | ----- | ----- |
| `CodeEditorTheme` record compatibility break | mitigated | `@core-architect` | Added compatibility constructor and preserved existing factories. |
| Style provider descriptor missing | mitigated | `@qa-engineer` | Added ServiceLoader descriptors and discovery tests in core and language modules. |
| Palette drift after dynamic scope migration | mitigated | `@ui-ux-designer` | Language style providers use the previous dark/light default colors; automated theme tests passed. Manual reviewer visual review remains external. |
| Java / JavaScript module classpath drift | mitigated | `@ops-engineer` | Added reactor, BOM, samples dependencies, provider descriptors, and module READMEs. |
| Settings preferences resolve but do not affect editor behavior | accepted | `@feature-dev` | Indent preferences are editor properties now; trailing-newline and trim policies are resolved properties for save-pipeline consumers. |

## Open Questions

- [x] Should `SyntaxStyleProvider` live under `...code.theme` or a new
      `...code.style` package? Decision: `...code.theme`.
- [x] Should old language-specific `TokenType` constants be deprecated
      immediately after migrated lexers stop emitting them? Decision: keep
      compatibility aliases without deprecating them in this iteration.
- [x] Should default style colors exactly preserve current JSON/YAML/Markdown
      palettes or receive a new visual review pass? Decision: preserve current
      defaults and leave visual review as a reviewer gate.
- [x] Should Java and JavaScript language packs remain independent only, or
      should a future aggregate artifact be considered? Decision: independent
      shipped modules only in this iteration.
- [x] Should trailing-newline policy be property-only until a save pipeline
      exists? Decision: yes.

## Handoff Notes

```text
Lead Agent:
Task Scope:
Impacted Modules:
Files Changed:
Key Invariants:
Validation Performed:
Open Risks / Follow-ups:
Required Reviewer:
```

- 2026-05-09 - `@spec-steward` -> `@feature-dev`
  Context: follow-up prompt, design, and plan were created for the source
  module split's out-of-scope roadmap items.
  Open work: implement Phases 0-7.
  Blockers: none after implementation decisions were recorded.
  References: `prompt.md`, `design.md`, `plan.md`.
- 2026-05-09 - `@feature-dev` -> required reviewers
  Context: syntax style SPI, Java/JavaScript language packs, JSON/YAML/Markdown
  scope migration, language settings, module topology, and documentation were
  implemented.
  Impacted modules: `papiflyfx-docking-code`,
  `papiflyfx-docking-code-java`, `papiflyfx-docking-code-javascript`,
  `papiflyfx-docking-code-json`, `papiflyfx-docking-code-yaml`,
  `papiflyfx-docking-code-markdown`, `papiflyfx-docking-samples`,
  `papiflyfx-docking-bom`, and repository/spec documentation.
  Key invariants: public token constructor compatibility is preserved; old
  `TokenType` constants remain; core built-ins register only `plain-text`;
  samples depend explicitly on shipped language packs.
  Validation performed: targeted language-pack Maven run, samples discovery,
  full headless reactor tests, full clean package, grep audits, and
  `git diff --check` passed.
  Open risks / follow-ups: external reviewer sign-offs and any manual
  dark/light visual review requested by `@ui-ux-designer`.
  Required reviewers: `@core-architect`, `@ops-engineer`,
  `@ui-ux-designer`, `@qa-engineer`.
