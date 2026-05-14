# PapiflyFX Code Scrollbar + Wrap Progress

Last updated: 2026-02-22 17:06 -06:00  
Reference: `spec/papiflyfx-docking-code-scrollbar/implementation.md`

## Overall status

- Phase 0: `completed`
- Phase 1: `completed`
- Phase 2: `completed`
- Phase 3: `completed`
- Phase 4: `completed`
- Phase 5: `completed`
- Phase 6: `completed`
- Phase 7: `completed`
- Phase 8: `partial`

## Delivered

- Horizontal scrolling model and rendering offsets are implemented.
- Vertical and horizontal canvas scrollbars are rendered, themed, and interactive.
- `wordWrap` mode is implemented with wrap-aware rendering and hit-testing.
- Wrap-mode behavior gates are enforced (horizontal offset reset/hide + box selection disabled).
- `WrapMap` and wrapped visual-row rendering are integrated into viewport, search, selection, caret, and gutter.
- Wrap-aware navigation and caret visibility logic are implemented.
- Persistence schema upgraded to v3 with migration paths from v2/v1/v0.

## Tests and verification

- Compile:
  - `mvn -pl papiflyfx-docking-code -DskipTests compile`
  - Result: success
- Targeted wrap/scrollbar regression suite:
  - `mvn -pl papiflyfx-docking-code -Dtest=RenderLineTest,ViewportTest,EditorStateCodecTest,WrapMapTest,ScrollbarPassTest,CodeEditorIntegrationTest,MouseGestureTest,GutterViewTest,CodeEditorThemeMapperTest -Dtestfx.headless=true test`
  - Result: success (`117` tests, `0` failures, `0` errors)
- Full module headless suite:
  - `mvn -pl papiflyfx-docking-code -Dtestfx.headless=true test`
  - Result: success (`371` tests, `0` failures, `0` errors)

## Remaining (Phase 8 backlog)

- Implement bounded incremental `WrapMap.update(...)` instead of full rebuild fallback.
- Implement incremental longest-line tracking for horizontal max-offset updates.
- Run and capture benchmark deltas for wrap-off vs wrap-on scrolling scenarios.
