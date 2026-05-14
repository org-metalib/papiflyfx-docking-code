# PapiflyFX Code Editor Scrollbars and Wrapped Text Design (Codex)

Date: 2026-02-22
Module: `papiflyfx-docking-code`

## 1. Wrapped Text Concept (First-Class Feature)

### 1.1 Problem framing

The editor currently renders one logical document line as one visual row and supports only vertical pixel offset scrolling (`verticalScrollOffset` + `Viewport.scrollOffset`). Long lines are drawn past the viewport width and cannot be reached through a horizontal scrollbar.

To add robust scrollbars, the design starts with a wrapped-text model because wrapping changes the meaning of:

1. vertical content height,
2. hit-testing (x,y -> line,column),
3. caret/selection rendering,
4. whether horizontal scrolling is needed at all.

### 1.2 Soft wrap definition

`textWrapEnabled=true` means soft wrap only:

1. No document mutation.
2. No inserted newline characters.
3. A logical line may map to N visual rows based on available viewport width.
4. All editor APIs and persistence remain logical-line based (`line`, `column`, offsets).

### 1.3 New flag and hard requirements

Introduce a new editor flag:

`textWrapEnabled` (boolean, default `false`)

Required semantics:

1. If `textWrapEnabled=true`:
   1. horizontal scrollbar is hidden/disabled and horizontal offset forced to 0,
   2. rectangular (box/column) selection support is disabled.
2. If `textWrapEnabled=false`:
   1. horizontal scrollbar is available when needed,
   2. box selection remains supported.

### 1.4 Behavior matrix

| Behavior | `textWrapEnabled=false` | `textWrapEnabled=true` |
| --- | --- | --- |
| Line rendering | Single row per logical line | Soft-wrapped visual rows |
| Horizontal scrollbar | Auto-visible when longest line exceeds viewport width | Hidden and disabled |
| Horizontal scroll offset | Mutable, persisted | Always `0.0` |
| Vertical content height | `lineCount * lineHeight` | `visualRowCount * lineHeight` |
| Box selection gestures | Enabled (`Shift+Alt+Drag`, middle-drag) | Disabled |
| Box selection fallback | N/A | Falls back to normal range selection drag |

## 2. Current State Summary (Why this change is needed)

Key observed behavior in current code:

1. `CodeEditor` has `verticalScrollOffset`, but no dedicated scrollbar UI nodes in layout.
2. `Viewport` only tracks vertical offset (`scrollOffset`) and assumes `x=column * charWidth` with no horizontal translation.
3. `EditorPointerController.handleScroll(...)` uses only `deltaY` and drives vertical offset.
4. Box selection is always available via:
   1. `Shift+Alt+Drag`,
   2. `MouseButton.MIDDLE` drag.
5. Persistence (`EditorStateData`/`EditorStateCodec`) stores vertical offset only; no wrap flag and no horizontal offset.

Result: mouse wheel vertical scrolling exists, but UI scrollbars and horizontal navigation do not.

## 3. Design Goals and Non-Goals

### 3.1 Goals

1. Add visible, synchronized vertical and horizontal scrollbars.
2. Add soft wrapping controlled by `textWrapEnabled`.
3. Guarantee: wrap mode disables horizontal scrollbar and box selection.
4. Keep logical document model unchanged.
5. Keep performance profile compatible with large files.
6. Implement scrollbars fully from scratch (no `javafx.scene.control.ScrollBar`) for UX and theming control.

### 3.2 Non-goals (for this feature set)

1. Semantic wrap at word boundaries (phase-2 enhancement; phase-1 is character wrap).
2. New multi-caret UX modes.
3. Folding redesign.
4. Minimap or overview ruler.

## 4. Proposed Architecture

### 4.1 Public API additions (`CodeEditor`)

Add:

1. `BooleanProperty textWrapEnabledProperty()`
2. `boolean isTextWrapEnabled()`
3. `void setTextWrapEnabled(boolean)`
4. `DoubleProperty horizontalScrollOffsetProperty()`
5. `double getHorizontalScrollOffset()`
6. `void setHorizontalScrollOffset(double)`

Rules:

1. Default: `textWrapEnabled=false`, `horizontalScrollOffset=0.0`.
2. Transition to wrap mode (`false -> true`) forces `horizontalScrollOffset=0.0`.
3. `setHorizontalScrollOffset(...)` is a no-op in wrap mode except normalizing to `0.0`.

### 4.2 UI composition changes (`CodeEditor` layout)

Current content layout:

1. `BorderPane` with `gutter` left and `viewport` center.
2. Search and go-to-line overlays on root `StackPane`.

Proposed content layout:

1. Internal `GridPane`:
   1. cell (0,0): gutter,
   2. cell (1,0): viewport,
   3. cell (2,0): custom vertical scrollbar node,
   4. cell (1,1): custom horizontal scrollbar node,
   5. optional cell (2,1): corner filler region when both bars visible.
2. Overlays remain in root `StackPane` above this grid.
3. Horizontal scrollbar width equals viewport width only (not gutter width).

### 4.2.1 Custom scrollbar implementation (from scratch)

The scrollbar element is a project-local component, not JavaFX `ScrollBar`.

New component set (recommended package: `org.metalib.papifly.fx.code.scroll`):

1. `EditorScrollbar` (`Region`)
2. `EditorScrollbarOrientation` (`VERTICAL`, `HORIZONTAL`)
3. `EditorScrollbarModel` (min/max/value/visibleAmount/step increments)
4. Optional helper: `ScrollbarGeometry` for thumb math and hit zones

Implementation principles:

1. Paint track/thumb directly (Canvas or `GraphicsContext`) for pixel-level control.
2. Handle interactions locally:
   1. thumb drag,
   2. track click page jump,
   3. hover/pressed states,
   4. optional press-and-hold auto-repeat.
3. Keep `value` clamped and normalized with stable math (avoid thumb jitter).
4. Expose lightweight API to editor layer:
   1. `setRange(min,max,visibleAmount)`,
   2. `setValue(value)`,
   3. `valueProperty()`.
5. Avoid style coupling to JavaFX control skin/CSS internals.
6. Do not subclass `javafx.scene.control.ScrollBar` or any JavaFX control skin.

Theming contract:

1. Extend `CodeEditorTheme` with scrollbar tokens:
   1. track background/border,
   2. thumb normal/hover/pressed,
   3. optional corner filler background.
2. Apply theme live via existing `CodeEditor.setEditorTheme(...)` fan-out path.

### 4.3 Scroll synchronization model

Introduce a small internal coordinator (new class recommended: `EditorScrollCoordinator`) to avoid property feedback loops.

Responsibilities:

1. Keep `CodeEditor` scroll properties synchronized with `Viewport`.
2. Update custom scrollbar model `min/max/value/visibleAmount`.
3. Normalize/clamp offsets when:
   1. viewport size changes,
   2. document changes,
   3. wrap flag changes,
   4. font metrics change.
4. Keep gutter vertical offset synchronized with viewport vertical offset.

Loop safety:

1. Keep guard booleans similar to existing `syncingScrollOffset` pattern in `EditorCaretCoordinator`.

### 4.4 Viewport model extensions

`Viewport` gains:

1. `textWrapEnabled` flag.
2. `horizontalScrollOffset`.
3. wrapped layout cache (`SoftWrapLayoutModel`, new helper class).

New/updated methods:

1. `setTextWrapEnabled(boolean)`
2. `isTextWrapEnabled()`
3. `setHorizontalScrollOffset(double)`
4. `getHorizontalScrollOffset()`
5. `double computeMaxHorizontalScrollOffset()`
6. `double computeMaxVerticalScrollOffset()` (updated for wrap)
7. `HitPosition getHitPosition(double x, double y)` (preferred over current split APIs)

`HitPosition` record:

1. `int line`
2. `int column`

This removes ambiguity in wrap mode where multiple visual rows map to one logical line.

### 4.5 Wrapped layout cache (`SoftWrapLayoutModel`, new)

Purpose:

1. Map logical lines to wrapped visual rows.
2. Provide fast row/line conversions for rendering and hit-testing.

Suggested API:

1. `void rebuild(Document doc, int wrapColumns)`
2. `int totalVisualRows()`
3. `VisualRow rowAt(int visualRowIndex)` where `VisualRow(lineIndex, startColumn, endColumn)`
4. `int lineToFirstVisualRow(int lineIndex)`
5. `int visualRowToLine(int visualRowIndex)`

Rebuild triggers:

1. document change,
2. viewport width change (changes wrap columns),
3. font metric change (`charWidth`),
4. wrap toggle on.

Note:

1. Phase-1 implementation may do full rebuild for correctness.
2. Incremental rebuild optimization can be added after correctness baseline.

## 5. Rendering Design

### 5.1 Unwrapped mode (`textWrapEnabled=false`)

Rendering rules:

1. Keep one render row per logical line.
2. Apply x translation by horizontal offset:
   1. `xScreen = xLogical - horizontalScrollOffset`.
3. All passes (`TextPass`, `SelectionPass`, `SearchPass`, `CaretPass`) must use this translation consistently.

Practical implementation options:

1. Add `horizontalScrollOffset` to `RenderContext` and subtract in each pass.
2. Or apply `GraphicsContext.translate(-horizontalScrollOffset, 0)` around text-layer passes.

Preferred:

1. explicit context field for clarity and easier testing.

### 5.2 Wrapped mode (`textWrapEnabled=true`)

Rendering rules:

1. Build render rows from `SoftWrapLayoutModel`.
2. `horizontalScrollOffset` always `0`.
3. Each `RenderLine` becomes a visual-row slice:
   1. `lineIndex`,
   2. `startColumn`,
   3. `endColumn`,
   4. `y`.
4. Text, selection, search, and caret rendering clip/scope to row slice boundaries.

### 5.3 Selection and caret in wrap mode

Selection:

1. For a selection crossing a wrapped line, paint only the covered segment in each affected visual row.
2. Replace line-wide selection span logic with row-aware span calculation.

Caret:

1. Determine caret visual row from `(line,column)` using wrap model.
2. Paint caret at:
   1. `x = (column - row.startColumn) * charWidth`,
   2. `y = visualRowIndex * lineHeight - verticalScrollOffset`.

Search highlights:

1. Split highlight rectangles across wrapped row boundaries when a match spans rows of the same logical line.

## 6. Input, Navigation, and Gesture Behavior

### 6.1 Pointer hit-testing

Replace pointer coordinate mapping flow:

Current:

1. `line = viewport.getLineAtY(y)`
2. `col = viewport.getColumnAtX(x)`

Proposed:

1. `hit = viewport.getHitPosition(x, y)`
2. use `hit.line`, `hit.column`

This is required for correct wrap-mode caret placement.

### 6.2 Scroll wheel / trackpad

Behavior:

1. Vertical scroll:
   1. always applied from `deltaY`.
2. Horizontal scroll:
   1. applied from `deltaX` when `textWrapEnabled=false`,
   2. ignored when `textWrapEnabled=true`.
3. Shift+wheel:
   1. treated as horizontal intent only when `textWrapEnabled=false`.

### 6.3 Box selection disablement in wrap mode

`EditorPointerController` change:

1. inject `BooleanSupplier textWrapEnabledSupplier`.
2. if wrap is enabled:
   1. do not enter `boxSelectionActive`,
   2. ignore middle-mouse box mode,
   3. treat `Shift+Alt+Drag` as standard range-selection drag.

This satisfies the requirement that box selection support is turned off in wrap mode.

### 6.4 Keyboard navigation semantics

Phase-1:

1. Keep existing logical-line navigation commands (`UP`, `DOWN`, page commands).
2. Maintain preferred column behavior already implemented in `EditorCaretCoordinator`.

Future enhancement (optional):

1. Add visual-row navigation mode for wrapped lines.

## 7. Scrollbar Visibility and Range Rules

### 7.1 Vertical scrollbar

Visible when:

1. `maxVerticalOffset > 0`.

Range:

1. `min=0`
2. `max=maxVerticalOffset`
3. `value=verticalScrollOffset`
4. `visibleAmount=viewportHeight` (or proportional thumb policy)
5. thumb size enforces a minimum pixel size for usability (for example >= 18px).

### 7.2 Horizontal scrollbar

Visible when all are true:

1. `textWrapEnabled=false`
2. `maxHorizontalOffset > 0`

Range:

1. `min=0`
2. `max=maxHorizontalOffset`
3. `value=horizontalScrollOffset`
4. `visibleAmount=viewportWidth`
5. thumb size enforces a minimum pixel size for usability (for example >= 18px).

Forced states:

1. if `textWrapEnabled=true`, horizontal bar is hidden/disabled and value reset to `0`.

### 7.3 Longest-line width computation

Need `contentPixelWidth` for horizontal max:

1. `maxLineLengthChars * charWidth`

Phase-1 approach:

1. full recompute on document changes for correctness.

Optimization path:

1. introduce incremental line-length metrics cache once baseline is stable.

## 8. Persistence and Versioning

### 8.1 State schema update

Update `EditorStateData` with:

1. `double horizontalScrollOffset`
2. `boolean textWrapEnabled`

State version:

1. bump `CodeEditorStateAdapter.VERSION` from `2` to `3`.

### 8.2 Codec keys

Add `EditorStateCodec` keys:

1. `horizontalScrollOffset`
2. `textWrapEnabled`

### 8.3 Migration behavior

v2 -> v3 defaults:

1. `horizontalScrollOffset=0.0`
2. `textWrapEnabled=false`

Compatibility guarantees:

1. existing v2 states restore exactly as before in unwrapped mode.
2. unknown/future versions still fallback safely to empty/default state.

## 9. File-Level Change Plan

### 9.1 Core API and layout

1. `api/CodeEditor.java`
2. `api/EditorLifecycleService.java`
3. New `api/EditorScrollCoordinator.java` (recommended)
4. New `scroll/EditorScrollbar.java`
5. New `scroll/EditorScrollbarModel.java`
6. New `scroll/EditorScrollbarOrientation.java`
7. Optional new `scroll/ScrollbarGeometry.java`

### 9.2 Rendering

1. `render/Viewport.java`
2. `render/RenderContext.java`
3. `render/RenderLine.java`
4. `render/TextPass.java`
5. `render/SelectionPass.java`
6. `render/SearchPass.java`
7. `render/CaretPass.java`
8. New `render/SoftWrapLayoutModel.java`

### 9.3 Input and interaction

1. `api/EditorPointerController.java`
2. Optional small updates in `api/EditorNavigationController.java` for wrap-aware page semantics.

### 9.4 Persistence

1. `state/EditorStateData.java`
2. `state/EditorStateCodec.java`
3. `api/CodeEditorStateAdapter.java`
4. `api/EditorStateCoordinator.java`

### 9.5 Theme model

1. `theme/CodeEditorTheme.java`
2. `theme/CodeEditorThemeMapper.java`
3. `test/theme/CodeEditorThemeMapperTest.java`

## 10. Testing Strategy

### 10.1 Unit tests

Add/update:

1. `scroll/EditorScrollbarTest`:
   1. thumb geometry math for edge values,
   2. drag/track click value updates,
   3. min thumb size behavior.
2. `render/SoftWrapLayoutModelTest`
3. `render/ViewportTest`:
   1. horizontal offset affects hit-testing and rendering positions,
   2. wrap mode row mapping correctness,
   3. vertical max changes with wrap.
4. `state/EditorStateCodecTest`:
   1. v3 encode/decode fields,
   2. v2 migration defaults.
5. `theme/CodeEditorThemeMapperTest`:
   1. scrollbar color tokens mapped for both light/dark themes.

### 10.2 Integration tests (TestFX)

Add/update:

1. `api/CodeEditorIntegrationTest`:
   1. scrollbar visibility in wrap vs non-wrap modes,
   2. horizontal offset persistence,
   3. verify the UI uses `EditorScrollbar` instances and no JavaFX `ScrollBar`.
2. `api/MouseGestureTest`:
   1. `Shift+Alt+Drag` creates box selection only when wrap is off,
   2. in wrap mode same gesture produces normal range selection,
   3. middle-drag box selection disabled in wrap mode.

### 10.3 Benchmark/regression checks

Run and compare:

1. scroll rendering p95 benchmark with wrap off,
2. add wrap-on scroll benchmark scenario for long lines.

## 11. Rollout Plan

### Phase 1: Wrap flag + behavior gates

1. Add `textWrapEnabled` and `horizontalScrollOffset` properties.
2. Disable horizontal offset + box selection when wrap is enabled.
3. Add persistence schema v3.

### Phase 2: Scrollbar UI wiring

1. Add vertical/horizontal `EditorScrollbar` nodes.
2. Wire property synchronization and clamp logic.
3. Keep render behavior unwrapped-only until phase 3.

### Phase 3: Horizontal rendering and hit-testing (unwrapped mode)

1. Apply horizontal translation through render passes.
2. Update pointer hit-testing for horizontal offset.

### Phase 4: Soft wrap rendering model

1. Implement `SoftWrapLayoutModel`.
2. Switch `Viewport` render pipeline when wrap is enabled.
3. Update selection/caret/search painting for wrapped rows.

### Phase 5: Hardening

1. Add performance optimizations (incremental wrap metrics).
2. Stabilize tests and benchmark thresholds.

## 12. Risks and Mitigations

1. Risk: regressions in selection and caret painting.
   1. Mitigation: row-aware geometry tests + screenshot-based integration assertions.
2. Risk: scroll property feedback loops.
   1. Mitigation: centralized coordinator + explicit sync guards.
3. Risk: full wrap/width recomputation cost on large files.
   1. Mitigation: ship correctness first; add incremental cache in hardening phase.
4. Risk: mixed multi-caret behavior with wrap toggle.
   1. Mitigation: deterministic wrap-mode rule set and explicit tests.
5. Risk: custom scrollbar implementation complexity vs off-the-shelf control behavior.
   1. Mitigation: constrain scope to essential interactions first, then iterate with dedicated tests.

## 13. Acceptance Criteria

1. Vertical scrollbar is present and synchronized with viewport offset.
2. Horizontal scrollbar appears only when:
   1. wrap is off,
   2. content width exceeds viewport.
3. Setting `textWrapEnabled=true`:
   1. hides/disables horizontal scrollbar,
   2. forces horizontal offset to `0`,
   3. disables box selection gestures.
4. Wrapped rendering keeps document text unchanged and edits logically correct.
5. State save/restore preserves wrap flag and horizontal/vertical offsets (v3), while v2 payloads remain backward-compatible.
6. Existing editor search, go-to-line, and theme behavior continue to work.
7. Scrollbar UI is provided by custom project code (`EditorScrollbar`) and does not depend on `javafx.scene.control.ScrollBar`.
