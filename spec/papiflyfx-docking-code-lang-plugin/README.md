#

## Research

Currently, code editor supports only few text language pattern. Research the possibility to abstract a language support
through a pluggable architecture. Read this papiflyfx-docking-code module in depth, understand how it works deeply.

Come up with a solution to extend the language support.

Write a detailed report of your learnings, findings and ideas in spec/papiflyfx-docking-code-lang-plugin/research.md

## Planning

I want to build a new feature "folding/language" support that extends the system and give a use easier document navigation.
Read this document `spec/papiflyfx-docking-code-lang-plugin/research.md` and write a very detailed
`spec/papiflyfx-docking-code-lang-plugin/plan.md` document outlining how to implement this.
Include code snippets.

## Implementation
implement everthing from spec/papiflyfx-docking-code-lang-plugin/plan.md. when you’re done with a task or phase, mark it
as completed in the spec/papiflyfx-docking-code-lang-plugin/plan.md document.
Add new spec/papiflyfx-docking-code-lang-plugin/progress.md file to track your progress.
Do not stop until all tasks and phases are completed. do not add unnecessary comments or javadocs, do not use any or unknown types.
continuously run typecheck to make sure you’re not introducing new issues.

## Follow-up Module Split

JSON, YAML, and Markdown now use this language SPI from dedicated modules. See
[`spec/papiflyfx-docking-code/2026-05-06-yaml-design/2026-05-09-json-yaml-md-modules/`](../papiflyfx-docking-code/2026-05-06-yaml-design/2026-05-09-json-yaml-md-modules/)
for the module split design, plan, and validation notes.

The next staged follow-up completes the language-pack model by adding
module-owned syntax style scopes, Java and JavaScript language packs, and
per-language editor settings. See
[`spec/papiflyfx-docking-code/2026-05-06-yaml-design/2026-05-09-json-yaml-md-modules-followup/`](../papiflyfx-docking-code/2026-05-06-yaml-design/2026-05-09-json-yaml-md-modules-followup/).
