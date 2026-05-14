# Progress — JSON / YAML / Markdown Module Split

Last updated: 2026-05-09
Lead: `@spec-steward`
Implementation owner: `@feature-dev`
Linked plan: [`plan.md`](plan.md)
Linked design: [`design.md`](design.md)
Linked prompt: [`prompt.md`](prompt.md)

## Overall Status

| Phase | Name                                       | Status        |
| ----- | ------------------------------------------ | ------------- |
| 0     | Pre-flight inventory                       | completed     |
| 1     | Scaffold `papiflyfx-docking-code-json`     | completed     |
| 2     | Scaffold `papiflyfx-docking-code-yaml`     | completed     |
| 3     | Scaffold `papiflyfx-docking-code-markdown` | completed     |
| 4     | Wire samples and BOM                       | completed     |
| 5     | Documentation sweep                        | completed     |
| 6     | Validation and acceptance gate             | in-progress   |

Status legend: `not-started`, `in-progress`, `blocked`, `completed`.

## Phase 0 — Pre-flight Inventory

Status: `completed`

- [x] 0.1 Walk `papiflyfx-docking-code/src/main/java` and confirm the
      file inventory in `design.md §4.4`.
- [x] 0.2 Repository-wide grep for `JsonLexer`, `YamlLexer`,
      `MarkdownLexer`, and matching fold providers; record consumers
      that are not the moving tests or `BuiltInLanguageSupportProvider`.
- [x] 0.3 Confirm `LanguageSupportRegistry.bootstrap` still loads
      `ServiceLoader<LanguageSupportProvider>` by default.
- [x] 0.4 Confirm Surefire `argLine` template choice for the new modules.
- [x] 0.5 Snapshot test counts for each affected test class.

Notes:

- GitNexus impact checks for `BuiltInLanguageSupportProvider`, the three
  lexers, the three fold providers, `LanguageSupportBootstrapTest`, and
  `IncrementalLexerEngineTest` returned low/no symbol risk before edits.
- Non-test consumers were limited to `BuiltInLanguageSupportProvider` and
  the lexer/fold pairings that moved together. The YAML incremental lexer
  regression moved from core into `YamlLexerTest`.
- `LanguageSupportRegistry.bootstrap()` already combined core built-ins and
  `ServiceLoader` providers, so no SPI change was required.
- Test snapshot after relocation: JSON 7 tests, YAML 24 tests, Markdown 11
  tests.

## Phase 1 — `papiflyfx-docking-code-json`

Status: `completed`

### 1.1 Module skeleton

- [x] 1.1.1 Create directory `papiflyfx-docking-code-json/`.
- [x] 1.1.2 Add the directory tree from `design.md §4`.
- [x] 1.1.3 Add module `pom.xml`.
- [x] 1.1.4 Add module entry to root `pom.xml`.
- [x] 1.1.5 Verify module build.

### 1.2 Move sources

- [x] 1.2.1 Move `JsonLexer.java`.
- [x] 1.2.2 Preserve existing package declaration for source compatibility.
- [x] 1.2.3 Move `JsonFoldProvider.java`.
- [x] 1.2.4 Keep provider in `org.metalib.papifly.fx.code.folding` so
      package-private fold provider visibility remains unchanged.
- [x] 1.2.5 Add `JsonLanguageSupportProvider.java`.
- [x] 1.2.6 Add `META-INF/services/...LanguageSupportProvider` descriptor.
- [x] 1.2.7 Remove `json` entry from `BuiltInLanguageSupportProvider`.
- [x] 1.2.8 Verify core module compile/test in the focused reactor.

### 1.3 Move tests

- [x] 1.3.1 Move `JsonLexerTest` and `JsonFoldProviderTest`.
- [x] 1.3.2 Preserve package declarations and imports where compatible.
- [x] 1.3.3 Add `JsonLanguageSupportProviderTest` and discovery smoke
      test.
- [x] 1.3.4 Run module test suite.

### 1.4 Module README and validation

- [x] 1.4.1 Author `papiflyfx-docking-code-json/README.md`.
- [x] 1.4.2 Run samples headless test suite.
- [x] 1.4.3 Update progress and record deviations.

Notes:

- The public class name and package remain
  `org.metalib.papifly.fx.code.lexer.JsonLexer`; consumers add the JSON
  module dependency instead of changing imports.

## Phase 2 — `papiflyfx-docking-code-yaml`

Status: `completed`

- [x] 2.1 Module skeleton (mirrors steps 1.1.1-1.1.5).
- [x] 2.2 Move `YamlLexer` and `YamlFoldProvider`.
- [x] 2.3 Add `YamlLanguageSupportProvider`.
- [x] 2.4 Add `META-INF/services/...LanguageSupportProvider` descriptor.
- [x] 2.5 Remove `yaml` entry from `BuiltInLanguageSupportProvider`.
- [x] 2.6 Move `YamlLexerTest` and `YamlFoldProviderTest`; add provider
      and discovery tests.
- [x] 2.7 Author module `README.md`.
- [x] 2.8 Build and run targeted Maven verification.

Notes:

- The YAML incremental block scalar regression now lives in the YAML module,
  keeping language-specific behavior and coverage together.

## Phase 3 — `papiflyfx-docking-code-markdown`

Status: `completed`

- [x] 3.1 Module skeleton (mirrors steps 1.1.1-1.1.5).
- [x] 3.2 Move `MarkdownLexer` and `MarkdownFoldProvider`.
- [x] 3.3 Add `MarkdownLanguageSupportProvider`.
- [x] 3.4 Add `META-INF/services/...LanguageSupportProvider` descriptor.
- [x] 3.5 Remove `markdown` entry from `BuiltInLanguageSupportProvider`.
- [x] 3.6 Move `MarkdownLexerTest` and `MarkdownFoldProviderTest`; add
      provider and discovery tests.
- [x] 3.7 Author module `README.md`.
- [x] 3.8 Build and run targeted Maven verification.

Notes:

- Markdown public class names and package paths are preserved; discovery is
  owned by the Markdown module descriptor.

## Phase 4 — Samples and BOM Wiring

Status: `completed`

- [x] 4.1 Add new module dependencies in `papiflyfx-docking-samples/pom.xml`.
- [x] 4.2 Run `papiflyfx-docking-samples` headless tests.
- [x] 4.3 Audit `papiflyfx-docking-bom/pom.xml`.
- [x] 4.4 Audit `papiflyfx-docking-archetype`.
- [x] 4.5 Add full-classpath discovery test in `papiflyfx-docking-samples`.

Notes:

- The BOM manages the three new module coordinates. The samples module
  declares them explicitly so the demo classpath retains JSON, YAML, and
  Markdown behavior.

## Phase 5 — Documentation Sweep

Status: `completed`

- [x] 5.1 Update `papiflyfx-docking-code/README.md` with language pack
      section.
- [x] 5.2 Tighten each new module's `README.md` after Phases 1-3.
- [x] 5.3 Update root `README.md` module list.
- [x] 5.4 Update `CLAUDE.md` content modules and conventions.
- [x] 5.5 Cross-link from `spec/papiflyfx-docking-code-lang-plugin/`.
- [x] 5.6 Update `spec/papiflyfx-docking-code/README.md` if needed.
- [x] 5.7 Optional roadmap updates.

Notes:

- The design and plan were revised to reflect FQN-preserving relocation
  instead of package renames.

## Phase 6 — Validation and Acceptance Gate

Status: `in-progress`

- [x] 6.1 `./mvnw clean package` from a fresh tree.
- [x] 6.2 `./mvnw -Dtestfx.headless=true test` from a fresh tree.
- [ ] 6.3 Visual smoke check via `SamplesApp` for JSON, YAML, Markdown
      in dark and light themes.
- [x] 6.4 Core module grep confirms the moved lexer/fold provider classes
      no longer live in `papiflyfx-docking-code`.
- [x] 6.5 `BuiltInLanguageSupportProvider` only registers `plain-text`,
      `java`, `javascript`.
- [x] 6.6 Migrated test counts match Phase 0 snapshot.
- [ ] 6.7 Reviewer sign-offs:
      - [ ] `@core-architect`
      - [ ] `@ops-engineer`
      - [ ] `@ui-ux-designer`
      - [ ] `@qa-engineer`

Notes:

- Focused language/core and samples headless verification is green.
- Full `./mvnw -Dtestfx.headless=true test` is green across all 19 reactor
  modules.
- Full `./mvnw clean package` is green across all 19 reactor modules on
  rerun. An earlier run hit a transient `papiflyfx-docking-hugo` Surefire
  fork crash (`Trace/BPT trap: 5`) after Hugo reported its tests green; that
  failure did not reproduce.

## Decision Log

| Date       | Decision                                                                    | Rationale                                                                                                | Source             |
| ---------- | --------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- | ------------------ |
| 2026-05-09 | Keep `TokenType` in `papiflyfx-docking-code`.                               | Cross-cutting render contract; moving forces an indirection without benefit at this scope.                | `design.md §5 T1`  |
| 2026-05-09 | Keep `FoldKind` in `papiflyfx-docking-code`.                                | Same reasoning as T1.                                                                                     | `design.md §5 T2`  |
| 2026-05-09 | Keep `CodeEditorTheme` as a record in core; defer theme contribution SPI.   | Larger refactor with separate review path. Tracked as roadmap follow-up.                                  | `design.md §5 T3`  |
| 2026-05-09 | Keep `BuiltInLanguageSupportProvider` registering plain-text, java, JS.     | Ensures core editor stays usable with no language-pack modules on the classpath.                          | `design.md §5 T4`  |
| 2026-05-09 | Use existing `ServiceLoader<LanguageSupportProvider>` discovery, no SPI change. | The SPI was designed for this; new modules ship descriptors, no registry change required.            | `design.md §5 T5`  |
| 2026-05-09 | Preserve existing lexer and fold provider package paths in the new modules. | Meets the import-compatibility requirement: consumers add a dependency, not source import edits.           | `design.md §3.1`   |
| 2026-05-09 | Add per-module provider tests plus one samples classpath discovery test.    | Catches broken descriptors locally and verifies the app-level classpath keeps all shipped languages.       | `plan.md §9`       |

Append new decisions in chronological order. Keep entries terse; link to
the design or plan sections that motivate them.

## Validation Log

Record dated entries when running validation steps; include the command,
relevant output snippets, and the agent who ran it. Example:

```
2026-05-09 — @feature-dev
$ ./mvnw -pl papiflyfx-docking-code-json -am clean package
[INFO] BUILD SUCCESS
[INFO] Total time: 14.218 s
Notes: empty-module skeleton compiles; ServiceLoader descriptor pending Phase 1.2.
```

- 2026-05-09 — `@feature-dev`
  - `$ gitnexus analyze`
  - Result: index refreshed successfully before impact checks.
- 2026-05-09 — `@feature-dev`
  - GitNexus impact checks for `BuiltInLanguageSupportProvider`,
    `JsonLexer`, `YamlLexer`, `MarkdownLexer`, `JsonFoldProvider`,
    `YamlFoldProvider`, `MarkdownFoldProvider`,
    `LanguageSupportBootstrapTest`, and `IncrementalLexerEngineTest`.
  - Result: low/no risk; no high or critical blast radius reported.
- 2026-05-09 — `@feature-dev`
  - `$ ./mvnw -pl papiflyfx-docking-code-json,papiflyfx-docking-code-yaml,papiflyfx-docking-code-markdown,papiflyfx-docking-code -am test`
  - Result: build success. Core code tests: 406; JSON: 7; YAML: 24;
    Markdown: 11.
- 2026-05-09 — `@feature-dev`
  - `$ ./mvnw -pl papiflyfx-docking-samples -am -Dtestfx.headless=true test`
  - Result: build success. Samples tests: 22, including
    `LanguageSupportDiscoveryTest`.
- 2026-05-09 — `@feature-dev`
  - `$ ./mvnw -Dtestfx.headless=true test`
  - Result: build success across all 19 reactor modules. Total time:
    02:39 min. JSON: 7 tests; YAML: 24 tests; Markdown: 11 tests; samples:
    22 tests.
- 2026-05-09 — `@feature-dev`
  - Core grep for moved classes under `papiflyfx-docking-code` and
    `BuiltInLanguageSupportProvider` registration audit.
  - Result: moved classes absent from core; built-in provider has no JSON,
    YAML, or Markdown registrations.
- 2026-05-09 — `@feature-dev`
  - `$ ./mvnw clean package`
  - Result: failed at `papiflyfx-docking-hugo` with a Surefire fork crash
    (`Trace/BPT trap: 5`) after the new language modules had passed and after
    Hugo reported 21 tests, 0 failures.
- 2026-05-09 — `@feature-dev`
  - `$ ./mvnw -rf :papiflyfx-docking-hugo clean package`
  - Result: Hugo and later runtime modules passed; resume later failed at
    samples dependency resolution because the resumed reactor did not include
    the earlier new language modules.
- 2026-05-09 — `@feature-dev`
  - `$ ./mvnw clean package`
  - Result: build success across all 19 reactor modules on rerun. Total time:
    02:46 min.

## Risk Watch

| Risk                                                                                       | Status   | Last review | Owner            | Notes                                                              |
| ------------------------------------------------------------------------------------------ | -------- | ----------- | ---------------- | ------------------------------------------------------------------ |
| `META-INF/services` descriptor missing or misnamed                                         | closed   | 2026-05-09  | `@feature-dev`   | Per-module and samples discovery tests pass.                       |
| Direct lexer imports break in unexpected places                                            | closed   | 2026-05-09  | `@feature-dev`   | Package paths preserved; consumers only add dependencies.          |
| Surefire / TestFX argLine regression on a new module                                       | mitigated| 2026-05-09  | `@qa-engineer`   | New modules use classpath test execution and pass targeted tests.  |
| Duplicate language registration if a stale `BuiltInLanguageSupportProvider` ships          | closed   | 2026-05-09  | `@core-architect`| Core provider no longer registers JSON, YAML, or Markdown.         |
| BOM / archetype drift                                                                      | closed   | 2026-05-09  | `@ops-engineer`  | BOM, samples, and archetype docs updated.                          |
| Spec docs drift (yaml-design, lang-plugin) reference old code paths                        | closed   | 2026-05-09  | `@spec-steward`  | Design, plan, progress, and language-plugin README updated.        |
| Full reactor `clean package` hits `papiflyfx-docking-hugo` fork crash in this environment  | closed   | 2026-05-09  | `@qa-engineer`   | Did not reproduce; full `./mvnw clean package` rerun is green.     |

Update `Status` to `mitigated`, `accepted`, or `closed` once handled.

## Open Questions

Track open questions from `design.md §12` here as they are answered:

- [x] Should `BuiltInLanguageSupportProvider` move from
      `.../folding/` to `.../language/`? (`@core-architect`)
- Answer: no change in this iteration; avoid unrelated package churn in the
  core provider while splitting language modules.
- [x] Should the BOM aggregate the new modules into a virtual
      `papiflyfx-docking-code-all` artifact? (`@ops-engineer`)
- Answer: no. BOM coordinates were added, but no aggregate artifact is part
  of this scope.
- [x] Defer theme contribution SPI to a later iteration?
      (`@ui-ux-designer`)
- Answer: yes. `CodeEditorTheme`, `TokenType`, `FoldKind`, and `TextPass`
  routing remain in core.
- [x] Place the discovery smoke test only in
      `papiflyfx-docking-samples`, or per-module? (`@qa-engineer`)
- Answer: both. Each module validates its descriptor, and samples validates
  the shipped app classpath.

## Handoff Notes

Use this section to capture handoff context whenever ownership of a
phase moves between agents (per `spec/agents/README.md` handoff
contract). Format:

```
2026-05-09 — @spec-steward → @feature-dev
Context: design and plan approved.
Open work: Phase 0 inventory.
Blockers: none.
References: prompt.md, design.md, plan.md.
```

- 2026-05-09 — `@spec-steward` -> `@feature-dev`
  Context: prompt, design, and plan define the module split and review gates.
  Open work: implement phases 0-6.
  Blockers: none.
  References: `prompt.md`, `design.md`, `plan.md`.
- 2026-05-09 — `@feature-dev` -> reviewers
  Context: JSON, YAML, and Markdown modules implemented with FQN-preserving
  moves and ServiceLoader providers.
  Open work: visual smoke and required reviewer sign-offs.
  Blockers: none for automated validation; the earlier Hugo Surefire fork
  crash did not reproduce on rerun.
  References: this `progress.md` validation log.
