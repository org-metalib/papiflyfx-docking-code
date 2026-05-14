# Plan — JSON / YAML / Markdown Module Split

Date: 2026-05-09
Lead: `@spec-steward`
Implementation owner: `@feature-dev`
Required reviewers: `@core-architect`, `@ops-engineer`, `@ui-ux-designer`, `@qa-engineer`
Linked design: [`design.md`](design.md)
Linked prompt: [`prompt.md`](prompt.md)

## Problem Statement

JSON, YAML, and Markdown lexers, fold providers, and tests currently live
inside `papiflyfx-docking-code` next to the editor core. The pluggable
language SPI introduced earlier already discovers
`LanguageSupportProvider`s through `ServiceLoader`, but no language is
shipped that way. The result is a single module that mixes the editor
runtime with multiple file-format implementations, which makes review,
ownership, and future feature work harder than it needs to be.

This plan extracts JSON, YAML, and Markdown into three sibling modules,
keeping the cross-cutting types (`TokenType`, `FoldKind`,
`CodeEditorTheme`, render dispatch) inside `papiflyfx-docking-code` while
moving each language's lexer, fold provider, and tests into the matching
new module. See [`design.md`](design.md) for the full architectural
rationale and decisions.

## Phasing Overview

| Phase | Name                                            | Lead              | Reviewers                                       |
| ----- | ----------------------------------------------- | ----------------- | ----------------------------------------------- |
| 0     | Pre-flight inventory                            | `@spec-steward`   | `@feature-dev`, `@ops-engineer`                 |
| 1     | Scaffold `papiflyfx-docking-code-json`          | `@feature-dev`    | `@ops-engineer`, `@core-architect`              |
| 2     | Scaffold `papiflyfx-docking-code-yaml`          | `@feature-dev`    | `@ops-engineer`, `@core-architect`              |
| 3     | Scaffold `papiflyfx-docking-code-markdown`      | `@feature-dev`    | `@ops-engineer`, `@core-architect`              |
| 4     | Wire samples and BOM                            | `@ops-engineer`   | `@feature-dev`, `@qa-engineer`                  |
| 5     | Documentation sweep                             | `@spec-steward`   | `@feature-dev`, `@ui-ux-designer`               |
| 6     | Validation and acceptance gate                  | `@qa-engineer`    | `@core-architect`, `@feature-dev`, `@ops-engineer`, `@ui-ux-designer` |

The phases are sequential by default. Phases 1, 2, and 3 follow the same
template; doing JSON first lets the team validate the template before
applying it to YAML and Markdown.

## Phase 0 — Pre-flight Inventory

Goal: confirm assumptions in [`design.md`](design.md) match the live
codebase before any move starts.

- [ ] 0.1 Walk `papiflyfx-docking-code/src/main/java` and confirm the file
      list in `design.md §4.4` is exhaustive (no helper class for JSON,
      YAML, or Markdown lives outside `lexer/` or `folding/`).
- [ ] 0.2 `grep -R --include='*.java' 'JsonLexer\|YamlLexer\|MarkdownLexer\|JsonFoldProvider\|YamlFoldProvider\|MarkdownFoldProvider' .`
      across the repository. Record every consumer that is not the
      `BuiltInLanguageSupportProvider` or one of the moving tests.
- [ ] 0.3 Confirm `LanguageSupportRegistry.bootstrap` loads
      `ServiceLoader<LanguageSupportProvider>` by default and that the
      conflict policy is `REJECT_ON_CONFLICT`. If anything has drifted,
      update the design before continuing.
- [ ] 0.4 Confirm `papiflyfx-docking-code` Surefire `argLine` block is
      the right template to copy into the new modules; confirm the
      simpler argLine in §6.2 is sufficient for plain JUnit tests.
- [ ] 0.5 Snapshot current test counts for each affected test class so
      Phase 6 can verify counts after the move.

Exit criteria: a checked-in note in `progress.md` listing the file
inventory, the consumer grep result, and the test snapshot counts.

## Phase 1 — `papiflyfx-docking-code-json`

Goal: ship the first language pack module end to end. This phase is the
template for Phases 2 and 3.

### 1.1 Module skeleton

- [ ] 1.1.1 Create `papiflyfx-docking-code-json/` at the repo root.
- [ ] 1.1.2 Add the directory tree from `design.md §4`:
      `pom.xml`, `README.md`, existing `lexer/` and `folding/`
      packages, `src/main/resources/META-INF/services/`, and
      `src/test/java/...`.
- [ ] 1.1.3 Copy the template `pom.xml` from `design.md §6.2`. Set
      `<artifactId>papiflyfx-docking-code-json</artifactId>` and adjust
      the description.
- [ ] 1.1.4 Add the module to root `pom.xml` `<modules>` between
      `papiflyfx-docking-code` and `papiflyfx-docking-tree`.
- [ ] 1.1.5 Run `./mvnw -pl papiflyfx-docking-code-json -am clean package`.
      Expect success on an empty module.

### 1.2 Move sources

- [ ] 1.2.1 Move `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/JsonLexer.java`
      into `papiflyfx-docking-code-json/src/main/java/org/metalib/papifly/fx/code/lexer/JsonLexer.java`.
- [ ] 1.2.2 Keep its package declaration as
      `org.metalib.papifly.fx.code.lexer`.
- [ ] 1.2.3 Move `JsonFoldProvider.java` from
      `papiflyfx-docking-code/.../folding/` into the new module's
      existing `org.metalib.papifly.fx.code.folding` package.
- [ ] 1.2.4 Preserve existing package paths so direct lexer imports only
      need the new Maven dependency.
- [ ] 1.2.5 Add `JsonLanguageSupportProvider.java` per the example in
      `design.md §4.2`.
- [ ] 1.2.6 Add the `META-INF/services/org.metalib.papifly.fx.code.language.LanguageSupportProvider`
      descriptor with the FQN of `JsonLanguageSupportProvider`.
- [ ] 1.2.7 Update `BuiltInLanguageSupportProvider` in
      `papiflyfx-docking-code` to remove the `json` entry and the now
      unused `JsonLexer` / `JsonFoldProvider` imports.
- [ ] 1.2.8 Run `./mvnw -pl papiflyfx-docking-code -am compile` to confirm
      the core module still compiles.

### 1.3 Move tests

- [ ] 1.3.1 Move `JsonLexerTest` and `JsonFoldProviderTest` into
      `papiflyfx-docking-code-json/src/test/java/org/metalib/papifly/fx/code/json/`.
- [ ] 1.3.2 Update package declarations and imports.
- [ ] 1.3.3 Add `JsonLanguageSupportProviderTest` and a discovery smoke
      test as described in `design.md §9.2`.
- [ ] 1.3.4 Run `./mvnw -pl papiflyfx-docking-code-json -am test`. Expect
      the moved tests plus the new ones to pass.

### 1.4 Module README and validation

- [ ] 1.4.1 Write `papiflyfx-docking-code-json/README.md` covering: scope,
      Maven coordinates, the language id `json`, file extensions, the
      `ServiceLoader` discovery mechanism, and a note that `TokenType`
      and theme keys live in `papiflyfx-docking-code`.
- [ ] 1.4.2 Run `./mvnw -pl papiflyfx-docking-samples -am -Dtestfx.headless=true test`
      to confirm the JSON sample still highlights and folds.
- [ ] 1.4.3 Tick all Phase 1 boxes in `progress.md` and record any
      deviations from the design.

Exit criteria: `./mvnw -pl papiflyfx-docking-code-json,papiflyfx-docking-code,papiflyfx-docking-samples -am clean package`
succeeds; the JSON editor sample renders identically to a pre-split
build (manual or screenshot check).

## Phase 2 — `papiflyfx-docking-code-yaml`

Goal: apply the Phase 1 template to YAML.

- [ ] 2.1 Repeat steps 1.1.1–1.1.5 with `papiflyfx-docking-code-yaml`.
- [ ] 2.2 Move `YamlLexer` (with `LANGUAGE_ID = "yaml"`) and
      `YamlFoldProvider` into the new module while preserving their
      existing `code.lexer` and `code.folding` packages.
- [ ] 2.3 Add `YamlLanguageSupportProvider` registering id `yaml`,
      aliases `{"yml"}`, extensions `{"yaml", "yml"}`, lexer factory
      `YamlLexer::new`, fold-provider factory `YamlFoldProvider::new`.
- [ ] 2.4 Add `META-INF/services/...LanguageSupportProvider` with the
      FQN of `YamlLanguageSupportProvider`.
- [ ] 2.5 Remove the `yaml` entry and YAML imports from
      `BuiltInLanguageSupportProvider`.
- [ ] 2.6 Move `YamlLexerTest` and `YamlFoldProviderTest`. Add
      `YamlLanguageSupportProviderTest` and a discovery smoke test.
- [ ] 2.7 Add `papiflyfx-docking-code-yaml/README.md`.
- [ ] 2.8 Build and test:
      `./mvnw -pl papiflyfx-docking-code-yaml,papiflyfx-docking-code-json,papiflyfx-docking-code,papiflyfx-docking-samples -am clean package`
      and the headless sample test.

Exit criteria: YAML editor sample renders identically to pre-split;
all targeted Maven runs are green.

## Phase 3 — `papiflyfx-docking-code-markdown`

Goal: apply the Phase 1 template to Markdown.

- [ ] 3.1 Repeat steps 1.1.1–1.1.5 with `papiflyfx-docking-code-markdown`.
- [ ] 3.2 Move `MarkdownLexer` and `MarkdownFoldProvider` into the new
      module while preserving their existing `code.lexer` and
      `code.folding` packages.
- [ ] 3.3 Add `MarkdownLanguageSupportProvider` registering id
      `markdown`, aliases `{"md"}`, extensions `{"md", "markdown"}`,
      lexer factory `MarkdownLexer::new`, fold-provider factory
      `MarkdownFoldProvider::new`.
- [ ] 3.4 Add `META-INF/services/...LanguageSupportProvider` with the
      FQN of `MarkdownLanguageSupportProvider`.
- [ ] 3.5 Remove the `markdown` entry and Markdown imports from
      `BuiltInLanguageSupportProvider`.
- [ ] 3.6 Move `MarkdownLexerTest` and `MarkdownFoldProviderTest`. Add
      `MarkdownLanguageSupportProviderTest` and a discovery smoke test.
- [ ] 3.7 Add `papiflyfx-docking-code-markdown/README.md`.
- [ ] 3.8 Build and test:
      `./mvnw -pl papiflyfx-docking-code-markdown,papiflyfx-docking-code-yaml,papiflyfx-docking-code-json,papiflyfx-docking-code,papiflyfx-docking-samples -am clean package`.

Exit criteria: Markdown editor sample renders identically to pre-split;
all targeted Maven runs are green.

## Phase 4 — Samples and BOM Wiring

Goal: ensure the samples module declares the new dependencies explicitly
and downstream-facing artifacts (BOM, archetype) reflect reality.

- [ ] 4.1 In `papiflyfx-docking-samples/pom.xml`, add three `<dependency>`
      entries for `papiflyfx-docking-code-json`, `-yaml`, and `-markdown`.
- [ ] 4.2 Run `./mvnw -pl papiflyfx-docking-samples -am -Dtestfx.headless=true test`
      and the smoke test in `SamplesSmokeTest`. Confirm green.
- [ ] 4.3 Audit `papiflyfx-docking-bom/pom.xml`. If it enumerates feature
      modules, add the three new modules with `${project.version}`. If
      it manages versions only via `<dependencyManagement>`, add the
      same entries. Decision is delegated to `@ops-engineer`.
- [ ] 4.4 Audit `papiflyfx-docking-archetype` for hardcoded module lists
      or sample code referencing the old packages. Update or note as
      out-of-scope per `@ops-engineer`.
- [ ] 4.5 Add a `papiflyfx-docking-samples` discovery test (per
      `design.md §9.4`) that asserts every shipped language id resolves
      to a real lexer when the full classpath is loaded.

Exit criteria: samples module compiles, runs, and tests pass; BOM /
archetype align with the new module list.

## Phase 5 — Documentation Sweep

Goal: leave the codebase navigable for future work.

- [ ] 5.1 Update `papiflyfx-docking-code/README.md`:
      - Document the language SPI.
      - Add a "Language packs" section listing the three new modules
        and explaining how to add a new one (file extension descriptor,
        `LanguageSupportProvider` shape, `META-INF/services` descriptor).
- [ ] 5.2 Update each new module's `README.md` for accuracy after Phase
      1–3 work has settled.
- [ ] 5.3 Update root `README.md` module list.
- [ ] 5.4 Update `CLAUDE.md`:
      - "Shared contracts and SPI" or "Content modules" section reflects
        the new modules.
      - "Working Conventions" section notes that language packs are
        ServiceLoader-discovered.
- [ ] 5.5 Cross-link from
      `spec/papiflyfx-docking-code-lang-plugin/README.md` to this folder.
- [ ] 5.6 Update `spec/papiflyfx-docking-code/README.md` if it lists
      sub-folders.
- [ ] 5.7 Optional: update `spec/papiflyfx-docking-roadmap` to record
      the follow-up work captured in `prompt.md` "Out-of-scope
      follow-ups".

Exit criteria: each new module is reachable from the docs; new authors
can find the language SPI and follow the same template.

## Phase 6 — Validation and Acceptance Gate

Goal: a single, repeatable green-light run.

- [ ] 6.1 `./mvnw clean package` from a fresh tree.
- [ ] 6.2 `./mvnw -Dtestfx.headless=true test` from a fresh tree.
- [ ] 6.3 Run the JSON, YAML, and Markdown editor samples in
      `SamplesApp`; capture before/after screenshots in dark and light
      themes (`@ui-ux-designer` review).
- [ ] 6.4 `rg 'JsonLexer|YamlLexer|MarkdownLexer|JsonFoldProvider|YamlFoldProvider|MarkdownFoldProvider' papiflyfx-docking-code`
      confirms those classes no longer live in the core module while the
      FQNs remain available from the new language modules.
- [ ] 6.5 `grep -R 'BuiltInLanguageSupportProvider' .` confirms the
      provider only registers `plain-text`, `java`, `javascript`.
- [ ] 6.6 Test counts per migrated test class match the Phase 0 snapshot.
- [ ] 6.7 `@core-architect`, `@ops-engineer`, `@ui-ux-designer`, and
      `@qa-engineer` sign off in `progress.md`.

Exit criteria: every checkbox in this phase is ticked, all reviewer
sign-offs are recorded, and `progress.md` is marked `completed`.

## Sequencing and Parallelism

Sequential by default. The team can parallelize Phases 1–3 if reviewers
agree, but this risks merge conflicts in `BuiltInLanguageSupportProvider`
and `pom.xml` `<modules>`. Recommended order: 0 → 1 → 2 → 3 → 4 → 5 → 6.

## Rollback Plan

Each phase is structured so the change set is reversible by `git revert`
without breaking the build:

1. Phases 1–3 each leave the build green at the end. Reverting one
   phase brings the corresponding language back into
   `papiflyfx-docking-code` cleanly because all moves are file-level.
2. Phase 4 changes to BOM and archetype are isolated POM edits.
3. Phase 5 doc edits are reversible.

If discovery breaks in production after release, the immediate workaround
is to add the affected language module dependency to the consumer's
classpath. Rolling back the split is a last resort.

## Estimated Effort

| Phase | Estimate                                        |
| ----- | ----------------------------------------------- |
| 0     | 0.5 day                                         |
| 1     | 1 day                                           |
| 2     | 0.5 day (template established in Phase 1)       |
| 3     | 0.5 day                                         |
| 4     | 0.5 day                                         |
| 5     | 0.5 day                                         |
| 6     | 0.5 day                                         |
| Total | ~3.5–4 days of focused work                     |

## Dependencies

1. The `LanguageSupport`/`LanguageSupportProvider`/`LanguageSupportRegistry`
   SPI must remain stable for the duration of this work. Any in-flight
   change there should land first.
2. `papiflyfx-docking-code` must remain on Java 25 / JavaFX 23.0.1
   matching the parent POM. The new modules inherit the same toolchain.

## Done Definition

The plan is "done" when every phase's exit criteria is satisfied,
[`progress.md`](progress.md) shows all phases `completed`, and the
acceptance gate in Phase 6 has been signed off.
