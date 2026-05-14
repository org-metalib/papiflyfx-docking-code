# Folding implementation summary

## Scope completed

Implemented full folding support for `papiflyfx-docking-code` across model, computation pipeline, rendering, interaction, persistence, commands, and tests.

## What was implemented

### 1) Folding core model and incremental computation

Added a new `org.metalib.papifly.fx.code.folding` package with:

- `FoldKind`
- `FoldRegion`
- `FoldMap`
- `VisibleLineMap`
- `FoldProvider`
- `FoldProviderRegistry`
- `IncrementalFoldingPipeline`

Added concrete providers:

- `PlainTextFoldProvider`
- `JavaFoldProvider` (brace/comment + Java text block `"""`)
- `JavaScriptFoldProvider` (brace/comment + template and nested `${...}` expressions)
- `JsonFoldProvider` (object/array folds outside strings)
- `MarkdownFoldProvider` (fence + heading section folds)

### 2) Viewport and gutter integration

Updated rendering/navigation path to respect collapsed content:

- `Viewport` now tracks `FoldMap` and `VisibleLineMap`.
- Logical-to-visible mapping is used for non-wrap rendering.
- Wrap rebuild excludes hidden logical lines.
- Hit testing and caret visibility calculations use visible-line mapping.

Updated gutter behavior:

- `GutterView` renders fold glyphs on fold headers.
- Gutter click hit-testing toggles fold state.
- Gutter line numbering respects folded visibility.

### 3) Editor orchestration and UX behavior

Updated `CodeEditor` to orchestrate lexing + folding:

- Added `IncrementalFoldingPipeline` lifecycle and language wiring.
- Added folding API:
  - `toggleFoldAtLine`
  - `foldRegion`
  - `unfoldRegion`
  - `foldAll`
  - `unfoldAll`
  - `foldRecursivelyAtLine`
  - `unfoldRecursivelyAtLine`
- Added reveal behavior so search/go-to-line/caret movement can target folded areas safely.

Updated navigation/controllers:

- `EditorNavigationController` now moves across visible lines.
- `EditorCaretCoordinator` clamps caret moves to visible logical lines.
- `EditorPointerController` now ignores already-consumed pointer events (gutter fold click coexistence).

### 4) Persistence migration (v4)

Extended state schema to persist fold regions robustly:

- Added `FoldRegionRef`.
- Extended `EditorStateData` with `foldedRegions`.
- Extended `EditorStateCodec` with `foldedRegions` serialization/deserialization.
- Upgraded `CodeEditorStateAdapter.VERSION` from `3` to `4`.
- Kept backward compatibility with legacy `foldedLines`; restore path can still consume v0/v1/v2/v3 payloads.

### 5) Commands and keybindings

Extended command system:

- Added commands:
  - `TOGGLE_FOLD`
  - `FOLD_ALL`
  - `UNFOLD_ALL`
  - `FOLD_RECURSIVE`
  - `UNFOLD_RECURSIVE`
- Added corresponding bindings in `KeymapTable`.
- Registered handlers in `EditorCommandRegistry` and wired execution in `CodeEditor`.

### 6) Test coverage added/updated

Added new folding tests:

- `FoldMapTest`
- `VisibleLineMapTest`
- `JavaFoldProviderTest`
- `JavaScriptFoldProviderTest`
- `JsonFoldProviderTest`
- `MarkdownFoldProviderTest`
- `IncrementalFoldingPipelineTest`

Updated existing tests for state/keymap integration:

- `EditorStateCodecTest`
- `CodeEditorIntegrationTest`
- `KeymapTableTest`

## Validation run

Executed:

- `./mvnw -q -pl papiflyfx-docking-code -am -DskipTests compile`
- `./mvnw -q -pl papiflyfx-docking-code -am -Dtestfx.headless=true test`

Both completed successfully.

## Plan status

All phases in `plan.md` are marked completed, including detailed backlog checkboxes.
