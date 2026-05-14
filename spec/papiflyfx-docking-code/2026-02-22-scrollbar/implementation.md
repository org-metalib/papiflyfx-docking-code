# PapiflyFX Code Scrollbar + Wrap Implementation

Date: 2026-02-22  
Reference: `spec/papiflyfx-docking-code-scrollbar/design.md`  
Module: `papiflyfx-docking-code`

## 1. Delivery status

- Phase 0: `completed`
- Phase 1: `completed`
- Phase 2: `completed`
- Phase 3: `completed`
- Phase 4: `completed`
- Phase 5: `completed`
- Phase 6: `completed`
- Phase 7: `completed`
- Phase 8: `partial`

## 2. Implemented objectives

- [x] Add canvas-rendered vertical and horizontal scrollbars.
- [x] Add `wordWrap` soft-wrap mode with wrap-aware rendering/hit-testing.
- [x] Keep logical model coordinates unchanged (wrapping is visual only).
- [x] Enforce wrap gates:
  - [x] `wordWrap=true` hides horizontal scrollbar.
  - [x] `wordWrap=true` forces `horizontalScrollOffset=0.0`.
  - [x] `wordWrap=true` disables box-selection gestures.
- [x] Preserve existing editor behavior (search, go-to-line, multi-caret, theme updates, persistence compatibility).

## 3. Phase completion checklist

## Phase 0: Safety Baseline -- COMPLETE

- [x] Established compile/test baseline for `papiflyfx-docking-code`.
- [x] Added phase-by-phase tracking in implementation/progress docs.
- [x] Deferred benchmark delta capture to hardening backlog (Phase 8).

## Phase 1: Horizontal Scroll State + X-Offset Rendering -- COMPLETE

- [x] Added `CodeEditor.horizontalScrollOffsetProperty()`.
- [x] Added `Viewport.horizontalScrollOffset` with clamp-to-max behavior.
- [x] Extended `RenderContext` with horizontal offset.
- [x] Applied x-offset in text/selection/search/caret render paths.
- [x] Updated hit testing for horizontal offset in unwrapped mode.
- [x] Added wheel/trackpad handling (`deltaX`, `Shift+wheel` horizontal map when wrap is off).
- [x] Added horizontal caret visibility support (`ensureCaretVisibleHorizontally`).
- [x] Updated tests (`ViewportTest`, `CodeEditorIntegrationTest`, caret visibility coverage).

## Phase 2: Canvas Scrollbar Rendering + Effective Text Area -- COMPLETE

- [x] Added scrollbar geometry constants in `Viewport`:
  - [x] `SCROLLBAR_WIDTH`
  - [x] `SCROLLBAR_THUMB_PAD`
  - [x] `MIN_THUMB_SIZE`
  - [x] `SCROLLBAR_RADIUS`
- [x] Added effective text area calculations (`effectiveTextWidth`, `effectiveTextHeight`).
- [x] Rebases visible range/max-offset calculations to effective dimensions.
- [x] Added `ScrollbarPass` as final render pass.
- [x] Extended `CodeEditorTheme` with scrollbar track/thumb/hover/active colors.
- [x] Mapped new tokens in `CodeEditorThemeMapper`.
- [x] Added scrollbar visibility/geometry getters on `Viewport`.
- [x] Updated top-right overlay margin when vertical scrollbar is visible.
- [x] Added tests (`ScrollbarPassTest`, `CodeEditorThemeMapperTest`, integration assertions).

## Phase 3: Scrollbar Mouse Interaction + Event Consumption -- COMPLETE

- [x] Added scrollbar interaction state (`Viewport.ScrollbarPart`).
- [x] Added scrollbar hit-region checks before text selection logic.
- [x] Consumed pointer events during scrollbar interaction.
- [x] Implemented track click jump and thumb drag scrolling.
- [x] Implemented hover/active visual state propagation.
- [x] Preserved wheel behavior while pointer is on scrollbar regions.
- [x] Added regression coverage so scrollbar interaction does not move caret.

## Phase 4: `wordWrap` Flag + Behavior Gates -- COMPLETE

- [x] Added `CodeEditor.wordWrapProperty()`.
- [x] Wired wrap mode into `Viewport` and `GutterView` state.
- [x] Enforced wrap-mode rules:
  - [x] horizontal scrollbar hidden.
  - [x] horizontal offset normalized to `0.0`.
  - [x] horizontal setter normalized/no-op behavior in wrap mode.
- [x] Disabled box-selection gestures in wrap mode.
- [x] Added guard to clear/ignore box selection when wrap mode becomes active.
- [x] Added tests in `MouseGestureTest` + integration tests for wrap gating.

## Phase 5: Soft Wrap Core (`WrapMap`) + Wrap-Aware Rendering -- COMPLETE

- [x] Added `WrapMap` with prefix-sum visual-row indexing.
- [x] Added `RenderLine` visual-slice fields (`startColumn`, `endColumn`).
- [x] Reworked `Viewport.buildRenderLines()` to emit visual rows in wrap mode.
- [x] Added wrap-aware hit testing via `Viewport.getHitPosition(double x, double y)`.
- [x] Updated render passes for wrap slices:
  - [x] `BackgroundPass`
  - [x] `TextPass`
  - [x] `SelectionPass`
  - [x] `SearchPass`
  - [x] `CaretPass`
- [x] Updated `SelectionGeometry` for wrapped-row splitting.
- [x] Updated `GutterView` for first-row-only line numbers in wrap mode.
- [x] Added/updated tests (`WrapMapTest`, `RenderLineTest`, `ViewportTest`, `GutterViewTest`).

## Phase 6: Navigation + Caret Visibility in Wrap Mode -- COMPLETE

- [x] Made caret vertical visibility wrap-aware.
- [x] Updated page navigation to operate on visual rows in wrap mode.
- [x] Preserved existing logical-line semantics for `Up/Down/Home/End`.
- [x] Skipped horizontal caret-visibility alignment in wrap mode.
- [x] Added wrap-mode page navigation and visibility regression coverage.

## Phase 7: Persistence v3 + Migration -- COMPLETE

- [x] Extended persisted state with `horizontalScrollOffset` and `wordWrap`.
- [x] Updated codec keys/defaults in `EditorStateCodec`.
- [x] Bumped `CodeEditorStateAdapter.VERSION` from `2` to `3`.
- [x] Added migration defaults for v2/v1/v0 -> v3.
- [x] Applied restore order: set wrap first, then restore offsets.
- [x] Added persistence test coverage in codec/integration suites.

## Phase 8: Hardening + Performance Follow-ups -- PARTIAL

- [ ] Implement bounded incremental `WrapMap.update(...)` (currently full rebuild fallback).
- [ ] Implement incremental longest-line tracking for horizontal max offset.
- [ ] Run and document wrap-on/wrap-off benchmark deltas in `CodeEditorBenchmarkTest`.
- [x] Added regression coverage for wrap mode gates and listener/disposal safety.

## 4. Final file matrix

## New files

- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/WrapMap.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/ScrollbarPass.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/render/WrapMapTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/render/ScrollbarPassTest.java`

## Modified files (core)

- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorPointerController.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorCaretCoordinator.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorNavigationController.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/EditorStateCoordinator.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorStateAdapter.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/gutter/GutterView.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/RenderContext.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/RenderLine.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/BackgroundPass.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/TextPass.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/SelectionPass.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/SearchPass.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/CaretPass.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/SelectionGeometry.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/state/EditorStateData.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/state/EditorStateCodec.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorTheme.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeMapper.java`

## Modified files (tests)

- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/render/ViewportTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/render/RenderLineTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/gutter/GutterViewTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/state/EditorStateCodecTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeMapperTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorIntegrationTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/MouseGestureTest.java`

## 5. Validation snapshot (2026-02-22)

- Compile:
  - `mvn -pl papiflyfx-docking-code -DskipTests compile`
  - Result: success
- Targeted regression suite:
  - `mvn -pl papiflyfx-docking-code -Dtest=RenderLineTest,ViewportTest,EditorStateCodecTest,WrapMapTest,ScrollbarPassTest,CodeEditorIntegrationTest,MouseGestureTest,GutterViewTest,CodeEditorThemeMapperTest -Dtestfx.headless=true test`
  - Result: success (`117` tests, `0` failures, `0` errors)
- Full module headless suite:
  - `mvn -pl papiflyfx-docking-code -Dtestfx.headless=true test`
  - Result: success (`371` tests, `0` failures, `0` errors)
