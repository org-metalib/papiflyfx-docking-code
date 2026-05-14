# PapiflyFX Code Editor — Text Wrap & Scrollbar Design

Date: 2026-02-22
Module: `papiflyfx-docking-code`
Status: Draft

---

## 1. Overview

The code editor renders text on a `Canvas` inside a `Viewport` region. Today it supports only vertical scrolling via mouse wheel and keyboard (Page Up/Down). There are no scrollbar widgets, no horizontal scrolling, and long lines clip at the canvas edge.

This document designs two interrelated features:

1. **Word wrap** (`wordWrap` property) — soft-wraps long lines at the viewport boundary, eliminating the need for horizontal scrolling.
2. **Scrollbars** — custom canvas-rendered scrollbar tracks painted directly on the editor canvas, integrated with the existing theme system.

The features are coupled: when `wordWrap=true`, the horizontal scrollbar is hidden and box selection is disabled. When `wordWrap=false`, the horizontal scrollbar enables panning across long lines.

---

## 2. Current Architecture Snapshot

### 2.1 Layout

```
CodeEditor (StackPane)
├── editorArea (BorderPane)
│   ├── left:   GutterView (Region → Canvas)
│   └── center: Viewport   (Region → Canvas)
├── SearchController       (overlay, TOP_RIGHT)
└── GoToLineController     (overlay, TOP_RIGHT)
```

### 2.2 Scroll pipeline

```
mouse wheel → EditorPointerController.handleScroll()
            → setVerticalScrollOffset(newOffset)           [DoubleConsumer]
            → CodeEditor.verticalScrollOffset property
            → listener → caretCoordinator.applyScrollOffset()
                        → viewport.setScrollOffset(clamped)
                        → gutterView.setScrollOffset(clamped)
```

Only `deltaY` is consumed. `deltaX` is ignored. There is no `horizontalScrollOffset`.

### 2.3 Coordinate model

All rendering uses a monospace grid: `x = column * charWidth`, `y = lineIndex * lineHeight - scrollOffset`. The `GlyphCache` measures a single `'M'` for `charWidth` and `"Hg"` for `lineHeight`. No per-character width variation exists.

### 2.4 Key limits

| Aspect | Current behavior |
|--------|-----------------|
| Vertical scroll | Mouse wheel + Page Up/Down; no scrollbar widget |
| Horizontal scroll | None — long lines clip at canvas edge |
| Max vertical offset | `lineCount * lineHeight - viewportHeight` |
| Box selection | Always available (Shift+Alt+Drag, middle-mouse) |
| State persistence | `EditorStateData` v2 — no `horizontalScrollOffset`, no `wordWrap` |

---

## 3. Feature 1: Word Wrap

### 3.1 Definition

Word wrap is a **soft wrap** — purely visual, no document mutation. When enabled, a logical line that exceeds the viewport width is broken into multiple **visual rows**. The document model, line indices, column indices, and all persistence remain in logical (unwrapped) coordinates.

### 3.2 Public API

Add to `CodeEditor`:

```java
// Property
BooleanProperty wordWrapProperty();   // default: false
boolean isWordWrap();
void setWordWrap(boolean value);
```

### 3.3 Behavioral contract

| Condition | `wordWrap = false` | `wordWrap = true` |
|-----------|-------------------|-------------------|
| Visual rows per line | 1 | 1..N (depending on line length vs viewport width) |
| Horizontal scrollbar | Visible when content wider than viewport | Hidden; offset forced to `0.0` |
| Horizontal scroll offset | Mutable, persisted | Always `0.0`, mutations ignored |
| Vertical content height | `lineCount * lineHeight` | `totalVisualRows * lineHeight` |
| Box selection (Shift+Alt+Drag, middle-mouse) | Enabled | Disabled — falls back to normal range selection |

### 3.4 WrapMap — the soft-wrap layout model

A new internal class `WrapMap` maps logical lines to visual rows.

```java
final class WrapMap {
    // Rebuild the entire map. Called on document change, viewport width change,
    // charWidth change, or wordWrap toggle.
    void rebuild(Document doc, double viewportWidth, double charWidth);

    // Incremental update: only recompute the affected logical line range.
    // Used after single-line edits for performance.
    void update(Document doc, int startLine, int endLine,
                double viewportWidth, double charWidth);

    // Total visual rows across all logical lines.
    int totalVisualRows();

    // Map a visual row index to its logical line + column slice.
    VisualRow visualRow(int visualRowIndex);

    // First visual row for a given logical line.
    int lineToFirstVisualRow(int lineIndex);

    // Number of visual rows for a given logical line.
    int lineVisualRowCount(int lineIndex);

    // Logical line index for a given visual row.
    int visualRowToLine(int visualRowIndex);
}

record VisualRow(int lineIndex, int startColumn, int endColumn) {}
```

**Storage:** An `int[]` prefix-sum array where `prefixSum[i]` = total visual rows for lines `0..i-1`. This allows O(1) `lineToFirstVisualRow` and O(log n) `visualRowToLine` via binary search.

**Wrap strategy (phase 1):** Character wrap — break at exactly `wrapColumns = floor(viewportWidth / charWidth)` characters. No word-boundary awareness in phase 1.

**Wrap strategy (phase 2, future):** Word wrap — prefer breaking at whitespace or punctuation boundaries. Falls back to character wrap when a single token exceeds `wrapColumns`.

**Rebuild triggers:**
- Document change (text inserted/deleted/replaced)
- Viewport width change (window resize, gutter width change)
- `charWidth` change (font change)
- `wordWrap` toggled `false → true`

When `wordWrap = false`, the `WrapMap` is not built and not consulted. All visual-row queries short-circuit to identity (visual row = logical line).

### 3.5 Render pipeline changes for wrap mode

When `wordWrap = true`, the render pipeline must produce visual rows instead of logical lines.

#### 3.5.1 RenderContext additions

```java
record RenderContext(
    // ... existing fields ...
    boolean wordWrap,                    // NEW
    double  horizontalScrollOffset,      // NEW (always 0.0 in wrap mode)
    WrapMap wrapMap                       // NEW (null when wordWrap=false)
)
```

#### 3.5.2 RenderLine changes

Currently `RenderLine` is one-to-one with logical lines:

```java
record RenderLine(int lineIndex, String text, double y, List<Token> tokens)
```

In wrap mode, each `RenderLine` represents a **visual row slice** of a logical line:

```java
record RenderLine(
    int lineIndex,        // logical line
    int startColumn,      // 0 for first row of a line, >0 for continuation rows
    int endColumn,        // exclusive
    String text,          // substring [startColumn, endColumn)
    double y,             // pixel y on canvas
    List<Token> tokens    // tokens clipped to [startColumn, endColumn)
)
```

In unwrapped mode, `startColumn = 0` and `endColumn = fullLineLength`, preserving backward compatibility.

#### 3.5.3 Viewport.buildRenderLines (wrap-aware)

```
if wordWrap:
    firstVisualRow = max(0, floor(scrollOffset / lineHeight) - PREFETCH_LINES)
    lastVisualRow  = min(totalVisualRows-1, ceil((scrollOffset + height) / lineHeight) + PREFETCH_LINES)
    for vr in [firstVisualRow .. lastVisualRow]:
        row = wrapMap.visualRow(vr)
        y   = vr * lineHeight - scrollOffset
        text = doc.getLineText(row.lineIndex).substring(row.startColumn, row.endColumn)
        tokens = clipTokens(allTokens[row.lineIndex], row.startColumn, row.endColumn)
        renderLines.add(RenderLine(row.lineIndex, row.startColumn, row.endColumn, text, y, tokens))
else:
    // existing logic, with startColumn=0, endColumn=lineLength
```

#### 3.5.4 Per-pass rendering adjustments

**BackgroundPass:**
- Current-line highlight: paint on **all visual rows** of the caret's logical line, not just one row.

**TextPass:**
- Text is drawn from `x = 0` for each visual row (since `startColumn` is already accounted for in the substring).
- Token coloring: tokens must be offset by `-startColumn` when computing `x` positions.

**SelectionPass:**
- `SelectionGeometry.spanForLine` must become `spanForVisualRow(selectionModel, renderLine)`:
  - Intersect the selection range `[selStartLine:selStartCol .. selEndLine:selEndCol]` with the visual row's `[lineIndex, startColumn..endColumn)`.
  - Compute `x` and `width` relative to the visual row's `startColumn`.

**SearchPass:**
- Search matches that span across visual row boundaries of the same logical line must be split into per-row highlight rectangles.
- Match intersection with `[startColumn, endColumn)` determines the highlighted portion in each visual row.

**CaretPass:**
- Determine which visual row the caret falls in: find the visual row where `startColumn <= caretColumn < endColumn` (or `caretColumn == endColumn` for end-of-row positioning).
- Paint at `x = (caretColumn - row.startColumn) * charWidth`.

#### 3.5.5 GutterView changes

- Line numbers should appear only on the **first visual row** of each logical line. Continuation rows show no line number (or a subtle wrap indicator like `↪`).
- The gutter must use `wrapMap.lineToFirstVisualRow(lineIndex)` to compute y positions.
- Vertical scroll offset remains pixel-based and shared with the viewport.

#### 3.5.6 Hit-testing in wrap mode

Replace the current two-step hit-test (`getLineAtY` + `getColumnAtX`) with a unified method:

```java
HitPosition getHitPosition(double localX, double localY) {
    if (wordWrap) {
        int visualRow = clamp(floor((localY + scrollOffset) / lineHeight), 0, totalVisualRows - 1);
        VisualRow vr = wrapMap.visualRow(visualRow);
        int col = clamp(round(localX / charWidth) + vr.startColumn, vr.startColumn, vr.endColumn);
        return new HitPosition(vr.lineIndex, col);
    } else {
        int line = clamp(floor((localY + scrollOffset) / lineHeight), 0, lineCount - 1);
        int col  = clamp(round((localX + horizontalScrollOffset) / charWidth), 0, lineLength);
        return new HitPosition(line, col);
    }
}

record HitPosition(int line, int column) {}
```

#### 3.5.7 Caret navigation in wrap mode

- **Up/Down arrows:** In phase 1, navigate by logical line (same as today). In a future phase, navigate by visual row for a more natural experience.
- **Home/End:** Navigate to start/end of the **logical line**, not the visual row (phase 1). Visual-row Home/End can be added later.
- **Page Up/Down:** Page delta becomes `floor(viewportHeight / lineHeight)` visual rows, then mapped back to logical line/column via `WrapMap`.
- **ensureCaretVisible:** Must compute caret y from its visual row index: `y = wrapMap.lineToFirstVisualRow(caretLine) * lineHeight` plus an offset for which visual row within the line the caret column falls in.

### 3.6 Disabling box selection in wrap mode

In `EditorPointerController`:

```java
// Inject via constructor or setter
private BooleanSupplier wordWrapSupplier;

void handleMousePressed(MouseEvent event) {
    // ...
    if (isBoxSelectionTrigger(event) && !wordWrapSupplier.getAsBoolean()) {
        startBoxSelection(line, col);
    } else {
        // Normal selection
    }
}

void handleMouseDragged(MouseEvent event) {
    if (boxSelectionActive && wordWrapSupplier.getAsBoolean()) {
        // Should not happen, but guard anyway
        cancelBoxSelection();
    }
    // ...
}
```

Middle-mouse drag is similarly guarded. When `wordWrap=true`, Shift+Alt+Drag and middle-drag behave as normal range selection.

---

## 4. Feature 2: Scrollbars

### 4.1 Design choice: fully custom scrollbar built from scratch

The scrollbars are **implemented entirely from scratch** — no `javafx.scene.control.ScrollBar`, no `ScrollPane`, no standard JavaFX controls of any kind. They are painted directly on the `Canvas` via `GraphicsContext` draw calls and handle mouse interaction through raw `MouseEvent` processing.

**Why not standard JavaFX ScrollBar?**

1. **Theming:** JavaFX `ScrollBar` is styled via CSS (`.scroll-bar`, `.thumb`, `.track`). This project follows a strict no-CSS, all-programmatic philosophy. Using a standard `ScrollBar` would require either CSS stylesheets (violating the project convention) or per-node `setStyle()` hacks that are fragile, incomplete, and cannot match the canvas-rendered editor aesthetic.
2. **Visual consistency:** The editor renders everything — text, carets, selections, search highlights, gutter — on a `Canvas`. A standard `ScrollBar` is a scene graph node that lives outside the canvas, creating a visual and architectural mismatch. A canvas-rendered scrollbar is visually seamless with the editor content.
3. **Pixel-perfect control:** Custom rendering gives full control over thumb shape, padding, rounded corners, opacity, animation (e.g. auto-fade), and hover/active color transitions — without fighting CSS pseudo-classes or JavaFX skin internals.
4. **Layout simplicity:** No need to restructure the existing `BorderPane` layout into a `GridPane` to accommodate `ScrollBar` nodes. The scrollbars are simply painted on top of the existing viewport canvas in the `ScrollbarPass`.
5. **User experience:** Custom scrollbars enable modern behaviors (overlay style, auto-fade, smooth opacity transitions) that would require significant CSS/skin customization with standard controls.

**Implementation summary:** The scrollbar is a pure rendering + input-handling construct:
- **Rendering:** A new `ScrollbarPass` (the last `RenderPass` in the pipeline) paints track and thumb rectangles using `GraphicsContext.fillRect` / `fillRoundRect`.
- **Input:** `EditorPointerController` performs hit-testing against scrollbar bounds and handles thumb drag, track click, and hover state changes through raw `MouseEvent` / `MouseDragEvent` processing.
- **State:** Thumb position is derived from `scrollOffset / maxScrollOffset` — there is no JavaFX `Property` binding to a `ScrollBar.value`. Synchronization uses the same guard-boolean pattern already established in `EditorCaretCoordinator`.
- **Theming:** Scrollbar colors are fields on the `CodeEditorTheme` record, applied in the `ScrollbarPass` render method. Theme changes take effect on the next redraw — no CSS reload, no node re-skinning.

### 4.2 Scrollbar visual specification

#### 4.2.1 Vertical scrollbar

- **Position:** Right edge of the `Viewport` canvas, full height
- **Track width:** 12px (configurable via constant `SCROLLBAR_WIDTH`)
- **Thumb:** Rounded rectangle, width 8px centered in the 12px track, with 2px padding on each side
- **Thumb height:** `max(MIN_THUMB_SIZE, viewportHeight * (viewportHeight / contentHeight))`
- **Thumb position:** `thumbY = scrollOffset / maxScrollOffset * (trackHeight - thumbHeight)`
- **Visibility:** Always visible when content exceeds viewport height (no auto-hide in phase 1; auto-fade can be added later)
- **Corner region:** When both scrollbars are visible, a `SCROLLBAR_WIDTH x SCROLLBAR_WIDTH` square at the bottom-right is filled with `scrollbarTrackColor`

#### 4.2.2 Horizontal scrollbar

- **Position:** Bottom edge of the `Viewport` canvas, spanning from gutter boundary to vertical scrollbar (or right edge if no vertical scrollbar)
- **Track height:** 12px (same `SCROLLBAR_WIDTH` constant)
- **Thumb:** Rounded rectangle, height 8px centered in the 12px track
- **Thumb width:** `max(MIN_THUMB_SIZE, viewportWidth * (viewportWidth / contentWidth))`
- **Thumb position:** `thumbX = horizontalScrollOffset / maxHorizontalOffset * (trackWidth - thumbWidth)`
- **Visibility:** Only when `wordWrap=false` AND content width exceeds viewport width
- **Content width:** `maxLineLengthChars * charWidth`

#### 4.2.3 Constants

```java
static final double SCROLLBAR_WIDTH     = 12.0;  // track width/height
static final double SCROLLBAR_THUMB_PAD = 2.0;   // padding inside track
static final double MIN_THUMB_SIZE      = 24.0;   // minimum thumb dimension in pixels
static final double SCROLLBAR_RADIUS    = 4.0;    // corner radius for thumb
```

### 4.3 Theme integration

Add to `CodeEditorTheme`:

```java
record CodeEditorTheme(
    // ... existing 34 fields ...

    // Scrollbar (NEW — 4 fields)
    Paint scrollbarTrackColor,          // track background
    Paint scrollbarThumbColor,          // thumb normal state
    Paint scrollbarThumbHoverColor,     // thumb when mouse hovers
    Paint scrollbarThumbActiveColor     // thumb when being dragged
)
```

Default values:

| Property | Dark theme | Light theme |
|----------|-----------|-------------|
| `scrollbarTrackColor` | `#1e1e1e` (same as `editorBackground`) | `#ffffff` |
| `scrollbarThumbColor` | `rgba(121, 121, 121, 0.4)` | `rgba(100, 100, 100, 0.4)` |
| `scrollbarThumbHoverColor` | `rgba(121, 121, 121, 0.7)` | `rgba(100, 100, 100, 0.6)` |
| `scrollbarThumbActiveColor` | `rgba(191, 191, 191, 0.8)` | `rgba(80, 80, 80, 0.7)` |

The semi-transparent thumb over the track (which matches the editor background) produces a subtle, modern scrollbar appearance similar to VS Code.

### 4.4 New render pass: ScrollbarPass

Add `ScrollbarPass` as the **last** render pass (after `CaretPass`) so scrollbars paint on top of all editor content.

```java
final class ScrollbarPass implements RenderPass {

    @Override
    public void renderFull(RenderContext ctx) {
        paintVerticalScrollbar(ctx);
        paintHorizontalScrollbar(ctx);
        paintCorner(ctx);
    }

    // No per-line rendering — scrollbars are viewport-level overlays.
    // renderLine is a no-op.
}
```

**Vertical scrollbar rendering:**
```
trackX = viewportWidth - SCROLLBAR_WIDTH
trackY = 0
trackH = viewportHeight - (horizontalBarVisible ? SCROLLBAR_WIDTH : 0)

thumbH = max(MIN_THUMB_SIZE, trackH * viewportHeight / contentHeight)
thumbY = scrollOffset / maxScrollOffset * (trackH - thumbH)
thumbX = trackX + SCROLLBAR_THUMB_PAD
thumbW = SCROLLBAR_WIDTH - 2 * SCROLLBAR_THUMB_PAD

gc.setFill(trackColor);
gc.fillRect(trackX, trackY, SCROLLBAR_WIDTH, trackH);
gc.setFill(thumbColor);    // varies by hover/active state
gc.fillRoundRect(thumbX, thumbY, thumbW, thumbH, SCROLLBAR_RADIUS, SCROLLBAR_RADIUS);
```

**Horizontal scrollbar rendering** (only when `wordWrap=false` and content wider than viewport):
```
trackX = 0
trackY = viewportHeight - SCROLLBAR_WIDTH
trackW = viewportWidth - (verticalBarVisible ? SCROLLBAR_WIDTH : 0)

thumbW = max(MIN_THUMB_SIZE, trackW * viewportWidth / contentWidth)
thumbX = horizontalScrollOffset / maxHorizontalOffset * (trackW - thumbW)
thumbY = trackY + SCROLLBAR_THUMB_PAD
thumbH = SCROLLBAR_WIDTH - 2 * SCROLLBAR_THUMB_PAD

gc.setFill(trackColor);
gc.fillRect(trackX, trackY, trackW, SCROLLBAR_WIDTH);
gc.setFill(thumbColor);
gc.fillRoundRect(thumbX, thumbY, thumbW, thumbH, SCROLLBAR_RADIUS, SCROLLBAR_RADIUS);
```

### 4.5 Scrollbar interaction handling (raw mouse events)

Since there is no JavaFX `ScrollBar` node, all scrollbar interaction is implemented through raw mouse event handling in `EditorPointerController` (or a dedicated `ScrollbarController`). There are no JavaFX skin behaviors, no CSS pseudo-class state transitions — just coordinate math and scroll offset updates.

#### 4.5.1 Hit regions

Before processing normal editor mouse events, check if the pointer is within scrollbar bounds:

```java
boolean isInVerticalScrollbar(double x, double y) {
    return x >= viewportWidth - SCROLLBAR_WIDTH && verticalBarVisible;
}

boolean isInHorizontalScrollbar(double x, double y) {
    return y >= viewportHeight - SCROLLBAR_WIDTH && horizontalBarVisible;
}
```

#### 4.5.2 Mouse interactions

**Click on track (outside thumb):** Jump scroll — set scroll offset so the thumb centers on the click position.

**Click on thumb + drag:** Proportional drag.
```
// Vertical drag example:
onDragStart: dragAnchorY = mouseY, dragAnchorOffset = scrollOffset
onDrag:      deltaY = mouseY - dragAnchorY
             scrollDelta = deltaY / (trackH - thumbH) * maxScrollOffset
             setVerticalScrollOffset(dragAnchorOffset + scrollDelta)
```

**Mouse hover:** Track the hover state (`NONE`, `VERTICAL_THUMB`, `HORIZONTAL_THUMB`, `VERTICAL_TRACK`, `HORIZONTAL_TRACK`) to switch thumb color between `thumbColor` and `thumbHoverColor`.

**Scroll wheel over scrollbar:** Same as normal scroll wheel (vertical delta changes vertical offset).

#### 4.5.3 Hover state tracking and visual feedback

The scrollbar maintains an internal state enum — no CSS pseudo-classes:

```java
enum ScrollbarHoverState { NONE, VERTICAL_TRACK, VERTICAL_THUMB, HORIZONTAL_TRACK, HORIZONTAL_THUMB }
```

State transitions on `MOUSE_MOVED` and `MOUSE_EXITED` events trigger a viewport repaint (to re-render the thumb with the appropriate color from `CodeEditorTheme`). This is lightweight — only the `ScrollbarPass` needs to repaint, and the editor already supports incremental dirty marking.

**Cursor:** When hovering over a scrollbar region, the cursor should remain as `Cursor.DEFAULT` (pointer). When dragging a thumb, it stays `DEFAULT` until release.

#### 4.5.4 Event consumption

When a mouse press lands on a scrollbar, the event must be consumed so it does not trigger text selection or caret movement in the editor.

### 4.6 Viewport effective area

With scrollbars painted on the canvas, the **effective text area** is reduced:

```
effectiveTextWidth  = viewportWidth  - (verticalBarVisible   ? SCROLLBAR_WIDTH : 0)
effectiveTextHeight = viewportHeight - (horizontalBarVisible ? SCROLLBAR_WIDTH : 0)
```

This affects:
- `computeMaxScrollOffset()` (use `effectiveTextHeight` instead of `viewportHeight`)
- `computeVisibleRange()` (visible lines calculation)
- Wrap column calculation: `wrapColumns = floor(effectiveTextWidth / charWidth)`
- Hit-testing: clicks in the scrollbar area must not map to text positions
- `ensureCaretVisible()`: ensure the caret is not hidden behind the scrollbar

### 4.7 Horizontal scroll offset

Add to `CodeEditor`:

```java
DoubleProperty horizontalScrollOffsetProperty();  // default: 0.0
double getHorizontalScrollOffset();
void setHorizontalScrollOffset(double value);
```

Clamping: `max(0.0, min(value, computeMaxHorizontalScrollOffset()))`.

```
maxHorizontalScrollOffset = max(0.0, maxLineLengthChars * charWidth - effectiveTextWidth)
```

When `wordWrap = true`:
- `setHorizontalScrollOffset()` forces value to `0.0`
- The horizontal scrollbar is hidden

When `wordWrap` transitions `true → false`:
- Horizontal offset remains `0.0` (user can scroll manually)

When `wordWrap` transitions `false → true`:
- Horizontal offset is reset to `0.0`

### 4.8 Horizontal scroll in render passes

In unwrapped mode, all x-coordinate computations must subtract `horizontalScrollOffset`:

```
xScreen = column * charWidth - horizontalScrollOffset
```

This applies to:
- `TextPass`: `gc.fillText(text, xScreen, y + baseline)`
- `SelectionPass`: selection rectangle x position
- `SearchPass`: search highlight x position
- `CaretPass`: caret x position
- `BackgroundPass`: current-line highlight spans full viewport width (no change needed)

**Implementation approach:** Add `horizontalScrollOffset` to `RenderContext`. Each pass reads it and subtracts from x-coordinates. This is preferred over `GraphicsContext.translate()` because it keeps coordinates explicit and testable.

### 4.9 Scroll coordinator

Introduce `EditorScrollCoordinator` to centralize scroll state synchronization:

```java
final class EditorScrollCoordinator {
    // Sync guards (prevent property feedback loops)
    private boolean syncingVertical;
    private boolean syncingHorizontal;

    // Called when CodeEditor.verticalScrollOffset changes
    void onVerticalOffsetChanged(double newValue);

    // Called when CodeEditor.horizontalScrollOffset changes
    void onHorizontalOffsetChanged(double newValue);

    // Called when viewport size, document, font metrics, or wordWrap change
    void recomputeScrollBounds();

    // Called from Viewport after redraw to update scrollbar thumb positions
    void updateScrollbarState();
}
```

This replaces the current ad-hoc sync in `EditorCaretCoordinator.applyScrollOffset()` for vertical scrolling and adds the same pattern for horizontal scrolling.

### 4.10 Scroll wheel / trackpad enhancements

Update `EditorPointerController.handleScroll`:

```java
void handleScroll(ScrollEvent event) {
    double deltaY = -event.getDeltaY() * scrollLineFactor;
    double newVertical = viewport.getScrollOffset() + deltaY;
    setVerticalScrollOffset.accept(newVertical);

    if (!wordWrapSupplier.getAsBoolean()) {
        double deltaX = -event.getDeltaX() * scrollLineFactor;
        if (deltaX != 0) {
            double newHorizontal = viewport.getHorizontalScrollOffset() + deltaX;
            setHorizontalScrollOffset.accept(newHorizontal);
        }
        // Shift+wheel vertical → treat as horizontal
        if (event.isShiftDown() && event.getDeltaY() != 0) {
            double hDelta = -event.getDeltaY() * scrollLineFactor;
            double newH = viewport.getHorizontalScrollOffset() + hDelta;
            setHorizontalScrollOffset.accept(newH);
            // Skip vertical scroll in this case
        }
    }

    event.consume();
}
```

### 4.11 Search/GoToLine overlay margin adjustment

The `SearchController` overlay is positioned at `TOP_RIGHT` with margin `(0, 16, 0, 0)`. With a vertical scrollbar, the right margin should increase to avoid overlapping:

```java
double rightMargin = 16.0 + (verticalBarVisible ? SCROLLBAR_WIDTH : 0);
StackPane.setMargin(searchController, new Insets(0, rightMargin, 0, 0));
```

Similarly for `GoToLineController`.

---

## 5. State Persistence

### 5.1 EditorStateData v3

```java
public record EditorStateData(
    String filePath,
    int cursorLine,
    int cursorColumn,
    double verticalScrollOffset,
    String languageId,
    List<Integer> foldedLines,
    int anchorLine,
    int anchorColumn,
    List<CaretStateData> secondaryCarets,
    // v3 additions:
    double horizontalScrollOffset,       // NEW, default 0.0
    boolean wordWrap                     // NEW, default false
) {}
```

### 5.2 EditorStateCodec changes

Add keys:
- `"horizontalScrollOffset"` → `double`
- `"wordWrap"` → `boolean`

### 5.3 Version migration

Bump `CodeEditorStateAdapter.VERSION` from `2` to `3`.

v2 → v3 migration defaults:
- `horizontalScrollOffset = 0.0`
- `wordWrap = false`

Existing v2 state payloads restore identically — the editor opens in unwrapped mode with no horizontal offset, which matches current behavior.

### 5.4 State capture and restore

**Capture** (in `EditorStateCoordinator.captureState`):
- Read `horizontalScrollOffset` from `viewport.getHorizontalScrollOffset()`
- Read `wordWrap` from `CodeEditor.isWordWrap()`

**Restore** (in `EditorStateCoordinator.restoreState`):
- Apply `wordWrap` first (affects layout and scroll bounds)
- Apply `verticalScrollOffset` and `horizontalScrollOffset` after layout settles (use `Platform.runLater` if needed to ensure viewport dimensions are final)

---

## 6. File-Level Change Summary

### 6.1 Modified files

| File | Changes |
|------|---------|
| `api/CodeEditor.java` | Add `wordWrap` and `horizontalScrollOffset` properties; wire scroll coordinator |
| `api/EditorPointerController.java` | Horizontal scroll handling; box-selection guard; scrollbar hit-testing and drag |
| `api/EditorNavigationController.java` | Page Up/Down in wrap mode; horizontal scroll keyboard shortcuts |
| `api/EditorCaretCoordinator.java` | `ensureCaretVisible` wrap-aware; `ensureCaretVisibleHorizontally` (new) |
| `api/EditorStateCoordinator.java` | Capture/restore `horizontalScrollOffset` and `wordWrap` |
| `api/CodeEditorStateAdapter.java` | `VERSION = 3` |
| `render/Viewport.java` | `horizontalScrollOffset`, `wordWrap`, `WrapMap` integration; `getHitPosition`; effective area calculation |
| `render/RenderContext.java` | Add `wordWrap`, `horizontalScrollOffset`, `wrapMap` fields |
| `render/RenderLine.java` | Add `startColumn`, `endColumn` fields |
| `render/TextPass.java` | Subtract `horizontalScrollOffset` from x; handle visual row slices |
| `render/SelectionPass.java` | Row-aware selection geometry; horizontal offset |
| `render/SearchPass.java` | Split highlights across visual rows; horizontal offset |
| `render/CaretPass.java` | Visual-row caret positioning; horizontal offset |
| `render/BackgroundPass.java` | Multi-row current-line highlight in wrap mode |
| `gutter/GutterView.java` | Visual-row-aware line numbering; wrap continuation indicator |
| `theme/CodeEditorTheme.java` | Add 4 scrollbar color fields |
| `theme/CodeEditorThemeMapper.java` | Map scrollbar colors from `Theme` |
| `state/EditorStateData.java` | Add `horizontalScrollOffset`, `wordWrap` |
| `state/EditorStateCodec.java` | Encode/decode new fields; v2→v3 migration |

### 6.2 New files

| File | Purpose |
|------|---------|
| `render/WrapMap.java` | Soft-wrap layout model (logical line ↔ visual row mapping) |
| `render/ScrollbarPass.java` | Canvas-rendered scrollbar overlay |
| `api/EditorScrollCoordinator.java` | Scroll state synchronization (replaces ad-hoc sync) |

---

## 7. Implementation Phases

### Phase 1: Horizontal scroll offset + scrollbar rendering (no wrap)

**Goal:** Add scrollbars and horizontal scrolling in unwrapped mode.

1. Add `horizontalScrollOffset` property to `CodeEditor` and `Viewport`.
2. Add `horizontalScrollOffset` to `RenderContext`.
3. Update all render passes to subtract `horizontalScrollOffset` from x-coordinates.
4. Update `EditorPointerController.handleScroll` to consume `deltaX`.
5. Update hit-testing: `getColumnAtX` accounts for `horizontalScrollOffset`.
6. Add `ensureCaretVisibleHorizontally` to `EditorCaretCoordinator`.
7. Implement `ScrollbarPass` (vertical + horizontal).
8. Add scrollbar theme properties to `CodeEditorTheme`.
9. Add scrollbar mouse interaction (thumb drag, track click, hover states).
10. Adjust search/go-to-line overlay margins.
11. Update `EditorStateData` to v3 with `horizontalScrollOffset` and `wordWrap`.

**Deliverable:** Fully functional scrollbars with horizontal panning. No wrap support yet.

### Phase 2: Word wrap flag + behavior gates

**Goal:** Add the `wordWrap` property with correct behavioral side effects, but without actual visual wrapping.

1. Add `wordWrap` property to `CodeEditor`.
2. When `wordWrap=true`: hide horizontal scrollbar, force `horizontalScrollOffset=0`.
3. Inject `wordWrapSupplier` into `EditorPointerController`; gate box selection.
4. Persist `wordWrap` in state.

**Deliverable:** The `wordWrap` flag exists and controls scrollbar visibility and box selection. Long lines still clip in wrap mode (visual wrapping comes in phase 3).

### Phase 3: Soft-wrap rendering

**Goal:** Implement visual line wrapping.

1. Implement `WrapMap` with full rebuild.
2. Update `Viewport.buildRenderLines` for visual rows.
3. Update `RenderLine` with `startColumn`/`endColumn`.
4. Update all render passes for visual-row-aware rendering.
5. Update `GutterView` for wrap-aware line numbering.
6. Implement `getHitPosition` for wrap mode.
7. Update `ensureCaretVisible` for wrap mode.

**Deliverable:** Soft wrapping works correctly for rendering, selection, search highlights, and caret.

### Phase 4: Wrap-mode navigation polish

**Goal:** Natural visual-row navigation.

1. Up/Down arrows navigate by visual row (not logical line) in wrap mode.
2. Home/End navigate to visual-row boundaries (with Ctrl+Home/End for logical line boundaries).
3. Page Up/Down account for visual rows.

### Phase 5: Hardening

1. Incremental `WrapMap.update()` for single-line edits.
2. `maxLineLengthChars` incremental tracking (avoid full recompute on every edit).
3. Scrollbar auto-fade animation — implemented via a `PauseTransition` (similar to the existing caret blink pattern) that reduces thumb opacity after an idle period. On scroll or hover, opacity snaps to 1.0 and the idle timer resets. All animation is driven by `GraphicsContext.setGlobalAlpha()` in the `ScrollbarPass` — no JavaFX `FadeTransition` or CSS animation required.
4. Performance benchmarks for wrap mode with large files.

---

## 8. Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Selection/caret painting regressions | High | Row-aware geometry unit tests; visual regression tests via TestFX screenshots |
| Scroll property feedback loops | Medium | Centralized `EditorScrollCoordinator` with explicit sync guards |
| Full `WrapMap` rebuild cost on large files | Medium | Phase 1-3 ship with full rebuild for correctness; incremental update in phase 5 |
| Scrollbar interaction conflicts with text editing | Medium | Hit-test scrollbar regions first; consume events before editor processing |
| Effective text area reduction by scrollbar width | Low | Account for `SCROLLBAR_WIDTH` in all viewport calculations from the start |

---

## 9. Acceptance Criteria

1. **Vertical scrollbar** is visible and synchronized with the viewport scroll offset when content exceeds viewport height.
2. **Horizontal scrollbar** appears only when `wordWrap=false` AND content width exceeds viewport width.
3. Scrollbar **thumb dragging** updates the scroll offset proportionally.
4. Scrollbar **track clicking** jumps the scroll position.
5. Setting `wordWrap=true`:
   - Hides the horizontal scrollbar
   - Forces `horizontalScrollOffset` to `0.0`
   - Disables box selection (Shift+Alt+Drag and middle-mouse fall back to range selection)
6. **Wrapped rendering** displays logical lines as multiple visual rows without modifying the document.
7. **Gutter** shows line numbers only on the first visual row of each wrapped line.
8. **Hit-testing** correctly maps clicks to `(line, column)` in both wrapped and unwrapped modes.
9. **Caret and selection** render correctly across visual row boundaries in wrap mode.
10. **Search highlights** split correctly across visual row boundaries.
11. **State persistence** saves and restores `wordWrap`, `horizontalScrollOffset`, and `verticalScrollOffset` (v3 schema). v2 payloads remain backward-compatible.
12. **Theme** scrollbar colors are applied from `CodeEditorTheme` and respond to live theme changes.
13. **Existing behavior** (search, go-to-line, multi-caret, keyboard navigation) continues to work in both modes.
