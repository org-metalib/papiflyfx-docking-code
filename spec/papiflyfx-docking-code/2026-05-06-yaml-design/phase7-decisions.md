# YAML Phase 7 Decisions

Date: 2026-05-07
Decision owner: the owner
Reviewers: @feature-dev, @ui-ux-designer, @qa-engineer
User impact: Phase 7 improves YAML readability and structural navigation for
users who work with Kubernetes manifests, CI files, Hugo front matter, and
other YAML-heavy configuration documents. The decisions favor richer YAML
specific behavior over keeping YAML visually identical to JSON.
Implementation impact: Phase 7 extends the existing lexer, fold provider,
theme palette, theme mapper, and render tests, but it remains inside
`papiflyfx-docking-code` unless later Phase 8 parser/schema work is approved.
Validation required: Focused lexer, folding, theme mapper, and viewport tests;
full headless `papiflyfx-docking-code` test run; manual SamplesApp spot check
using realistic block, flow, anchor, alias, and tag YAML examples.

## Decision 1: Dedicated YAML Colors

Status: accepted

Decision: Yaml should have dedicated colors.
Rationale: although yaml and json are similar for better isolation yaml should have dedicated colors.
User impact: YAML anchors, aliases, tags, and mapping keys become easier to
scan independently from JSON keys and plain scalar values. This helps users
spot reuse (`&anchor` / `*alias`), type hints (`!!str`), custom tags, and key
structure in larger configuration files.
Implementation impact: Add YAML-specific color entries to `CodeEditorTheme`
and defaults, map them through `CodeEditorThemeMapper`, extend `TextPass`
token-color routing, and ensure the YAML lexer emits distinct token types for
anchors, aliases, tags, and mapping keys instead of overloading generic
operator or JSON key colors.
Validation required: Unit tests for YAML token classification, theme default
and mapper tests proving the new YAML colors are populated and distinct where
intended, and a visual/manual SamplesApp check in dark and light themes.

## Decision 2: Block Scalar LexState

Status: accepted

Decision: Widen LexState: track block-scalar mode, expected indent, scalar style, and chomping.
Rationale: as preliminary work to go to toward parser-backed YAML: probably Phase 8 territory, not Phase 7.
User impact: Multiline YAML block scalars render more predictably. Users
should see literal (`|`) and folded (`>`) content treated consistently until
the scalar ends, without later indented mappings or comments being colored as
part of the scalar by accident.
Implementation impact: Extend `LexState` with block-scalar metadata such as
style, chomping mode, and expected indentation. Update `YamlLexer` state
transitions, incremental relex behavior, and any tests that assume the current
line-local approximation.
Validation required: Lexer tests for literal and folded block scalars with
explicit indentation indicators, chomping indicators (`|+`, `|-`, `>+`, `>-`),
blank lines, comments, and the first following line that exits the scalar.
Include incremental lexer tests to verify edits before a block scalar correctly
invalidate and reclassify affected downstream lines.

## Decision 3: Flow-Style Folding

Status: accepted

Decision: limited {} / [] folding, likely brittle.
Rationale: as preliminary work to go to toward parser-backed YAML: probably Phase 8 territory, not Phase 7.
User impact: Users can collapse compact flow-style mappings and sequences in
YAML documents, which helps with inline metadata, labels, and small nested
objects that are otherwise visually dense.
Implementation impact: Extend `YamlFoldProvider` to recognize balanced flow
mapping and sequence delimiters while respecting quoted strings, comments, and
nested delimiters. Because this is still lexer-level rather than parser-backed,
the implementation must clearly define fallback behavior for malformed or
multi-line flow constructs.
Validation required: Folding tests for single-line and multi-line `{...}` and
`[...]` constructs, nested flow values, quoted braces/brackets, comments after
flow values, malformed unbalanced delimiters, and interaction with existing
indentation-based folds.
