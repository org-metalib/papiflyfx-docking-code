package org.metalib.papifly.fx.code.gutter;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.folding.FoldMap;
import org.metalib.papifly.fx.code.folding.VisibleLineMap;
import org.metalib.papifly.fx.code.render.GlyphCache;
import org.metalib.papifly.fx.code.render.WrapMap;
import org.metalib.papifly.fx.code.theme.CodeEditorTheme;
import org.metalib.papifly.fx.ui.UiMetrics;

import java.util.function.IntConsumer;

/**
 * Canvas-based gutter rendering line numbers and a marker lane.
 * <p>
 * Draws alongside the {@link org.metalib.papifly.fx.code.render.Viewport}
 * and synchronizes scroll position with it.
 */
public class GutterView extends Region {

    private static final double MARKER_LANE_WIDTH = UiMetrics.SPACE_3;
    private static final double FOLD_LANE_WIDTH = UiMetrics.SPACE_3;
    private static final double LINE_NUMBER_RIGHT_PADDING = UiMetrics.SPACE_2;
    private static final double GLYPH_HALF_STEP = UiMetrics.SPACE_1 * 0.5;

    private final Canvas canvas;
    private final GlyphCache glyphCache;

    private CodeEditorTheme theme = CodeEditorTheme.dark();
    private Document document;
    private MarkerModel markerModel;
    private double scrollOffset;
    private int activeLineIndex = -1;
    private boolean dirty = true;
    private double computedWidth;
    private boolean wordWrap;
    private WrapMap wrapMap;
    private FoldMap foldMap = FoldMap.empty();
    private VisibleLineMap visibleLineMap = new VisibleLineMap();
    private IntConsumer foldToggleHandler;
    private boolean mouseOverGutter;

    /**
     * Creates a gutter view backed by the provided glyph cache.
     *
     * @param glyphCache glyph metrics cache shared with viewport rendering
     */
    public GutterView(GlyphCache glyphCache) {
        this.glyphCache = glyphCache;
        this.canvas = new Canvas();
        getChildren().add(canvas);
        setOnMousePressed(this::handleMousePressed);
        setOnMouseEntered(e -> {
            mouseOverGutter = true;
            markDirty();
        });
        setOnMouseExited(e -> {
            mouseOverGutter = false;
            markDirty();
        });
    }

    /**
     * Sets the document to display line numbers for.
     *
     * @param document document model to render in the gutter
     */
    public void setDocument(Document document) {
        this.document = document;
        visibleLineMap.rebuild(document == null ? 0 : document.getLineCount(), foldMap);
        recomputeWidth();
        markDirty();
    }

    /**
     * Sets the editor theme palette and triggers a redraw.
     *
     * @param theme theme palette to apply
     */
    public void setTheme(CodeEditorTheme theme) {
        this.theme = theme == null ? CodeEditorTheme.dark() : theme;
        markDirty();
    }

    /**
     * Returns the current editor theme.
     *
     * @return active gutter theme
     */
    public CodeEditorTheme getTheme() {
        return theme;
    }

    /**
     * Sets the marker model for the marker lane.
     *
     * @param markerModel marker model used to render line markers
     */
    public void setMarkerModel(MarkerModel markerModel) {
        this.markerModel = markerModel;
        markDirty();
    }

    /**
     * Returns the current marker model.
     *
     * @return marker model used by this gutter, may be {@code null}
     */
    public MarkerModel getMarkerModel() {
        return markerModel;
    }

    /**
     * Synchronizes scroll offset with the viewport.
     *
     * @param offset vertical scroll offset in pixels
     */
    public void setScrollOffset(double offset) {
        this.scrollOffset = offset;
        markDirty();
    }

    /**
     * Enables/disables wrap-aware gutter layout.
     *
     * @param wordWrap {@code true} to render wrap-aware line numbers
     */
    public void setWordWrap(boolean wordWrap) {
        if (this.wordWrap == wordWrap) {
            return;
        }
        this.wordWrap = wordWrap;
        markDirty();
    }

    /**
     * Sets wrap metadata shared by the viewport in wrap mode.
     *
     * @param wrapMap wrap metadata for visual row mapping
     */
    public void setWrapMap(WrapMap wrapMap) {
        this.wrapMap = wrapMap;
        markDirty();
    }

    public void setFoldMap(FoldMap foldMap) {
        this.foldMap = foldMap == null ? FoldMap.empty() : foldMap;
        visibleLineMap.rebuild(document == null ? 0 : document.getLineCount(), this.foldMap);
        markDirty();
    }

    public void setVisibleLineMap(VisibleLineMap visibleLineMap) {
        this.visibleLineMap = visibleLineMap == null ? new VisibleLineMap() : visibleLineMap;
        markDirty();
    }

    public void setOnFoldToggle(IntConsumer foldToggleHandler) {
        this.foldToggleHandler = foldToggleHandler;
    }

    /**
     * Sets the active (caret) line index for highlighting.
     *
     * @param lineIndex zero-based active line index
     */
    public void setActiveLineIndex(int lineIndex) {
        if (this.activeLineIndex != lineIndex) {
            this.activeLineIndex = lineIndex;
            markDirty();
        }
    }

    /**
     * Returns the computed preferred width of the gutter.
     *
     * @return computed gutter width in pixels
     */
    public double getComputedWidth() {
        return computedWidth;
    }

    /**
     * Recomputes the gutter width based on total line count.
     */
    public void recomputeWidth() {
        if (document == null) {
            computedWidth = 0;
            return;
        }
        int lineCount = document.getLineCount();
        int digits = Math.max(2, String.valueOf(lineCount).length());
        double charWidth = glyphCache.getCharWidth();
        computedWidth = MARKER_LANE_WIDTH + (digits * charWidth) + LINE_NUMBER_RIGHT_PADDING + FOLD_LANE_WIDTH;
        setPrefWidth(computedWidth);
        setMinWidth(computedWidth);
        setMaxWidth(computedWidth);
    }

    /**
     * Marks gutter render cache dirty and requests layout.
     */
    public void markDirty() {
        dirty = true;
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        if (w != canvas.getWidth() || h != canvas.getHeight()) {
            canvas.setWidth(w);
            canvas.setHeight(h);
            dirty = true;
        }
        if (dirty) {
            dirty = false;
            redraw();
        }
    }

    private void redraw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0 || document == null) {
            return;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double lineHeight = glyphCache.getLineHeight();
        double baseline = glyphCache.getBaselineOffset();
        // Clear
        gc.setFill(theme.gutterBackground());
        gc.fillRect(0, 0, w, h);

        gc.setFont(glyphCache.getFont());

        if (wordWrap && wrapMap != null && wrapMap.hasData() && wrapMap.totalVisualRows() > 0) {
            int totalRows = wrapMap.totalVisualRows();
            int firstRow = Math.max(0, (int) (scrollOffset / lineHeight));
            int lastRow = Math.min(totalRows - 1, (int) ((scrollOffset + h) / lineHeight) + 1);
            for (int row = firstRow; row <= lastRow; row++) {
                WrapMap.VisualRow visualRow = wrapMap.visualRow(row);
                if (visualRow.startColumn() != 0) {
                    continue;
                }
                int line = visualRow.lineIndex();
                double y = row * lineHeight - scrollOffset;
                paintGutterLine(gc, w, baseline, lineHeight, line, y);
            }
            return;
        }

        int totalVisibleLines = Math.max(0, visibleLineMap.visibleCount());
        if (totalVisibleLines == 0) {
            return;
        }
        int firstVisibleLine = Math.max(0, (int) (scrollOffset / lineHeight));
        int lastVisibleLine = Math.min(totalVisibleLines - 1, (int) ((scrollOffset + h) / lineHeight) + 1);
        for (int visibleLine = firstVisibleLine; visibleLine <= lastVisibleLine; visibleLine++) {
            int line = visibleLineMap.visibleToLogical(visibleLine);
            double y = visibleLine * lineHeight - scrollOffset;
            paintGutterLine(gc, w, baseline, lineHeight, line, y);
        }
    }

    private void paintGutterLine(GraphicsContext gc, double width, double baseline, double lineHeight, int line, double y) {
        if (markerModel != null && markerModel.hasMarkers(line)) {
            MarkerType type = markerModel.getHighestPriorityType(line);
            if (type != null) {
                gc.setFill(markerColor(type));
                double markerSize = Math.min(lineHeight - UiMetrics.SPACE_1, MARKER_LANE_WIDTH - UiMetrics.SPACE_1);
                double markerX = (MARKER_LANE_WIDTH - markerSize) / 2;
                double markerY = y + (lineHeight - markerSize) / 2;
                gc.fillOval(markerX, markerY, markerSize, markerSize);
            }
        }

        String lineNum = String.valueOf(line + 1);
        double lineNumberWidth = lineNum.length() * glyphCache.getCharWidth();
        double textX = MARKER_LANE_WIDTH + lineNumberWidth; // Right edge of number
        // We want to align numbers, so we should actually calculate based on max digits
        int maxDigits = Math.max(2, String.valueOf(document.getLineCount()).length());
        double maxNumberWidth = maxDigits * glyphCache.getCharWidth();
        
        double actualTextX = MARKER_LANE_WIDTH + maxNumberWidth - lineNumberWidth;
        gc.setFill(line == activeLineIndex ? theme.lineNumberActiveColor() : theme.lineNumberColor());
        gc.fillText(lineNum, actualTextX, y + baseline);

        double foldX = MARKER_LANE_WIDTH + maxNumberWidth + (LINE_NUMBER_RIGHT_PADDING / 2.0);
        if (mouseOverGutter) {
            paintFoldGlyph(gc, lineHeight, line, y, foldX);
        }
    }

    private void paintFoldGlyph(GraphicsContext gc, double lineHeight, int line, double y, double x) {
        if (!foldMap.hasRegionStartingAt(line)) {
            return;
        }
        double centerY = y + (lineHeight * 0.5);
        gc.setStroke(line == activeLineIndex ? theme.lineNumberActiveColor() : theme.lineNumberColor());
        gc.setLineWidth(1.5);
        if (foldMap.isCollapsedHeader(line)) {
            // Chevron Right
            gc.strokePolyline(
                new double[]{x + GLYPH_HALF_STEP, x + UiMetrics.SPACE_1 + GLYPH_HALF_STEP, x + GLYPH_HALF_STEP},
                new double[]{centerY - UiMetrics.SPACE_1, centerY, centerY + UiMetrics.SPACE_1},
                3
            );
            return;
        }
        // Chevron Down
        gc.strokePolyline(
            new double[]{x, x + UiMetrics.SPACE_1, x + UiMetrics.SPACE_2},
            new double[]{centerY - GLYPH_HALF_STEP, centerY + GLYPH_HALF_STEP, centerY - GLYPH_HALF_STEP},
            3
        );
    }

    private void handleMousePressed(MouseEvent event) {
        if (event == null || event.getButton() != MouseButton.PRIMARY || foldToggleHandler == null) {
            return;
        }
        int maxDigits = Math.max(2, String.valueOf(document.getLineCount()).length());
        double maxNumberWidth = maxDigits * glyphCache.getCharWidth();
        double foldXStart = MARKER_LANE_WIDTH + maxNumberWidth;
        double foldXEnd = foldXStart + LINE_NUMBER_RIGHT_PADDING + FOLD_LANE_WIDTH;

        if (event.getX() < foldXStart || event.getX() > foldXEnd) {
            return;
        }
        int line = resolveLineAtY(event.getY());
        if (line < 0 || !foldMap.hasRegionStartingAt(line)) {
            return;
        }
        foldToggleHandler.accept(line);
        event.consume();
    }

    private int resolveLineAtY(double localY) {
        if (document == null || document.getLineCount() <= 0) {
            return -1;
        }
        double lineHeight = glyphCache.getLineHeight();
        if (lineHeight <= 0) {
            return -1;
        }
        if (wordWrap && wrapMap != null && wrapMap.hasData() && wrapMap.totalVisualRows() > 0) {
            int visualRow = Math.max(0, Math.min(
                (int) Math.floor((localY + scrollOffset) / lineHeight),
                wrapMap.totalVisualRows() - 1
            ));
            return wrapMap.visualRow(visualRow).lineIndex();
        }
        int visibleLine = (int) Math.floor((localY + scrollOffset) / lineHeight);
        if (visibleLineMap.visibleCount() <= 0) {
            int safeLine = Math.max(0, Math.min(visibleLine, document.getLineCount() - 1));
            return safeLine;
        }
        int safeVisibleLine = Math.max(0, Math.min(visibleLine, visibleLineMap.visibleCount() - 1));
        return visibleLineMap.visibleToLogical(safeVisibleLine);
    }

    private Paint markerColor(MarkerType type) {
        return switch (type) {
            case ERROR -> theme.markerErrorColor();
            case WARNING -> theme.markerWarningColor();
            case INFO -> theme.markerInfoColor();
            case BREAKPOINT -> theme.markerBreakpointColor();
            case BOOKMARK -> theme.markerBookmarkColor();
        };
    }
}
