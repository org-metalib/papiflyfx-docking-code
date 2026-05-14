# JSON / YAML / Markdown Module Follow-ups

This folder turns the `Out-of-scope follow-ups` from
[`../2026-05-09-json-yaml-md-modules/prompt.md`](../2026-05-09-json-yaml-md-modules/prompt.md)
into a concrete follow-up initiative.

The source module split moved JSON, YAML, and Markdown into language-pack
modules while leaving three larger roadmap items behind:

1. A theme-contribution SPI so language modules can declare syntax color
   keys without editing `CodeEditorTheme` or `TextPass` for each language.
2. Java and JavaScript language-pack modules using the same ServiceLoader
   template as JSON, YAML, and Markdown.
3. Per-language editor settings, such as indent width and trailing-newline
   policy, sourced from `papiflyfx-docking-settings-api`.

## Documents

- [`prompt.md`](prompt.md) - implementation prompt for the follow-up work.
- [`design.md`](design.md) - architecture, API shape, module topology, and
  migration rules.
- [`plan.md`](plan.md) - phased work plan with ownership and validation.
- [`progress.md`](progress.md) - status, decisions, validation log, risks,
  and handoff notes.

Lead agent: `@spec-steward`
Implementation owner: `@feature-dev`
Required reviewers: `@core-architect`, `@ops-engineer`, `@ui-ux-designer`,
`@qa-engineer`
