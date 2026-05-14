# Additions: Review of `spec.md`

## Validation Summary
- `spec.md` is directionally correct and consistent with the docking architecture goals.
- Integration model is mostly valid (`DockManager.themeProperty`, `ContentFactory`, and `ContentStateAdapter` are real extension points).
- A few parts need correction to match the current Java API and avoid ambiguity.

## Validation Corrections
1. `SyntaxTheme extends Theme` is not valid as written.
`Theme` is a Java `record` (`org.metalib.papifly.fx.docks.theme.Theme`), so it is final and cannot be extended. Use one of:
- `CodeEditorTheme` as a separate record referenced by editor code.
- Composition: keep base `Theme` and store editor palette in a dedicated object/service.

2. Content persistence contract is underspecified.
Current docking persistence uses `LeafContentData(typeKey, contentId, version, state)`. The spec should explicitly define:
- stable `typeKey` (for adapter lookup),
- `version` (for schema evolution),
- `contentId` (stable identity across saves),
- `state` payload map.

3. Restore order should be documented.
Current restore flow supports adapter-based restoration first, then `ContentFactory` fallback. This should be explicit in the spec to avoid incompatible implementations.

## Conciseness Improvements
1. Merge sections `3.2 Viewport` and `3.4 Rendering Pipeline` into one section (`Rendering & Virtualization`) to remove repetition.
2. Split features into `MVP` vs `Post-MVP` instead of listing all advanced features upfront.
3. Move language list and roadmap details to one compact `Implementation Phases` table.
4. Replace aspirational performance wording ("sub-millisecond latency") with measurable targets and test conditions.

## Missing Sections to Add
1. Scope boundaries (`Non-goals`).
For example: mini-map, multi-caret, and language islands are out of MVP.

2. Acceptance criteria.
Define measurable goals:
- startup with a 100k-line file under defined hardware profile,
- max frame time budget during scroll/type,
- memory ceiling and GC pause tolerance.

3. Threading model.
Specify:
- lexer/tokenization runs off JavaFX UI thread,
- scene graph mutations occur only on JavaFX Application Thread,
- cancellation/debounce behavior for rapid edits.

4. Failure and fallback behavior.
Specify behavior when:
- file is missing/unreadable,
- lexer fails,
- `ContentStateAdapter` or `ContentFactory` is unavailable.

5. Persistence versioning and migration.
Define how `EditorStateData` evolves across versions and how older versions are migrated.

6. Lifecycle/disposal contract.
Define cleanup when a leaf closes (listeners, background workers, caches) to prevent leaks.

7. Test strategy.
Add required test layers:
- unit tests for document and lexer,
- JavaFX integration tests for rendering/caret/selection,
- persistence round-trip tests,
- optional performance benchmark suite.

## Suggested Compact Structure for `spec.md`
1. Vision (short)
2. MVP Scope / Non-goals
3. Architecture (Document, Renderer, Lexer, Docking Integration)
4. Contracts (Content factory/state schema, threading, lifecycle)
5. Acceptance Criteria (performance + correctness)
6. Roadmap (Post-MVP features)
