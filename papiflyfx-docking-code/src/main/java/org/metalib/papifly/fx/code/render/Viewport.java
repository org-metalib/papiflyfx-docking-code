package org.metalib.papifly.fx.code.render;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.util.Duration;
import org.metalib.papifly.fx.code.command.CaretRange;
import org.metalib.papifly.fx.code.command.MultiCaretModel;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.document.DocumentChangeEvent;
import org.metalib.papifly.fx.code.document.DocumentChangeListener;
import org.metalib.papifly.fx.code.folding.FoldMap;
import org.metalib.papifly.fx.code.folding.VisibleLineMap;
import org.metalib.papifly.fx.code.lexer.TokenMap;
import org.metalib.papifly.fx.code.search.SearchMatch;
import org.metalib.papifly.fx.code.theme.CodeEditorTheme;
import org.metalib.papifly.fx.ui.UiMetrics;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Canvas-based virtualized text renderer.
 * <p>
 * Draws visible text rows from a {@link Document} onto a {@link Canvas},
 * including selection, caret, search highlights, and custom scrollbars.
 */
public class Viewport extends Region {

    private static final int PREFETCH_LINES = 2;
    private static final Duration DEFAULT_CARET_BLINK_DELAY = Duration.millis(500);
    private static final Duration DEFAULT_CARET_BLINK_PERIOD = Duration.millis(500);
    private static final double METRIC_EPSILON = 0.01;

    /**
     * Scrollbar track thickness in pixels.
     */
    public static final double SCROLLBAR_WIDTH = UiMetrics.SPACE_3;
    /**
     * Thumb inset from track edge in pixels.
     */
    public static final double SCROLLBAR_THUMB_PAD = UiMetrics.SPACE_1 * 0.5;
    /**
     * Minimum scrollbar thumb size in pixels.
     */
    public static final double MIN_THUMB_SIZE = UiMetrics.SPACE_6;
    /**
     * Scrollbar corner radius in pixels.
     */
    public static final double SCROLLBAR_RADIUS = UiMetrics.RADIUS_MD;

    private final Canvas canvas;
    private final GlyphCache glyphCache;
    private final SelectionModel selectionModel;
    private final ChangeListener<Number> caretLineListener =
        (obs, oldValue, newValue) -> onCaretLineChanged(oldValue.intValue(), newValue.intValue());
    private final ChangeListener<Number> caretColumnListener = (obs, oldValue, newValue) -> onCaretColumnChanged();
    private final ChangeListener<Number> anchorLineListener = (obs, oldValue, newValue) -> onSelectionAnchorChanged();
    private final ChangeListener<Number> anchorColumnListener = (obs, oldValue, newValue) -> onSelectionAnchorChanged();
    private final PauseTransition caretBlinkDelay = new PauseTransition(DEFAULT_CARET_BLINK_DELAY);
    private final Timeline caretBlinkTimeline = new Timeline();
    private final ViewportInvalidationPlanner invalidationPlanner = new ViewportInvalidationPlanner();
    private final WrapMap wrapMap = new WrapMap();

    private CodeEditorTheme theme = CodeEditorTheme.dark();
    private Document document;
    private double scrollOffset;
    private double horizontalScrollOffset;
    private boolean wordWrap;

    private boolean dirty = true;
    private boolean fullRedrawRequired = true;
    private final BitSet dirtyLines = new BitSet();
    private boolean disposed;
    private TokenMap tokenMap = TokenMap.empty();
    private FoldMap foldMap = FoldMap.empty();
    private final VisibleLineMap visibleLineMap = new VisibleLineMap();
    private List<SearchMatch> searchMatches = List.of();
    private Map<Integer, List<Integer>> searchMatchIndexesByLine = Map.of();
    private int currentSearchMatchIndex = -1;
    private MultiCaretModel multiCaretModel;

    private int firstVisibleLine;
    private int visibleLineCount;
    private int firstVisibleVisualRow;
    private int visibleVisualRowCount;
    private int previousCaretLine = -1;
    private boolean previousSelectionActive;
    private int previousSelectionStartLine = -1;
    private int previousSelectionEndLine = -1;
    private int longestLineLength;

    private double effectiveTextWidth;
    private double effectiveTextHeight;
    private boolean verticalScrollbarVisible;
    private boolean horizontalScrollbarVisible;
    private ScrollbarGeometry verticalScrollbarGeometry;
    private ScrollbarGeometry horizontalScrollbarGeometry;
    private ScrollbarPart scrollbarHoverPart = ScrollbarPart.NONE;
    private ScrollbarPart scrollbarActivePart = ScrollbarPart.NONE;
    private Runnable scrollbarVisibilityListener;

    private boolean wrapMapDirty = true;
    private double lastWrapViewportWidth = -1;
    private double lastWrapCharWidth = -1;

    private final List<RenderLine> renderLines = new ArrayList<>();
    private final List<RenderPass> renderPasses = List.of(
        new BackgroundPass(),
        new SearchPass(),
        new SelectionPass(),
        new TextPass(),
        new CaretPass(),
        new ScrollbarPass()
    );
    private boolean caretBlinkActive;
    private boolean caretVisible = true;

    private final DocumentChangeListener changeListener = this::onDocumentChanged;

    /**
     * Creates a viewport with the given selection model.
     *
     * @param selectionModel selection model bound to caret/anchor changes
     */
    public Viewport(SelectionModel selectionModel) {
        this.selectionModel = selectionModel;
        this.glyphCache = new GlyphCache();
        this.canvas = new Canvas();

        getChildren().add(canvas);

        selectionModel.caretLineProperty().addListener(caretLineListener);
        selectionModel.caretColumnProperty().addListener(caretColumnListener);
        selectionModel.anchorLineProperty().addListener(anchorLineListener);
        selectionModel.anchorColumnProperty().addListener(anchorColumnListener);

        configureCaretBlink(DEFAULT_CARET_BLINK_DELAY, DEFAULT_CARET_BLINK_PERIOD);
        caretBlinkDelay.setOnFinished(event -> {
            if (caretBlinkActive && !disposed) {
                caretBlinkTimeline.playFromStart();
            }
        });
    }

    /**
     * Sets the document to render.
     *
     * @param document document model to render
     */
    public void setDocument(Document document) {
        if (this.document != null) {
            this.document.removeChangeListener(changeListener);
        }
        this.document = document;
        if (this.document != null) {
            this.document.addChangeListener(changeListener);
        }
        visibleLineMap.rebuild(this.document == null ? 0 : this.document.getLineCount(), foldMap);
        recomputeLongestLineLength();
        wrapMapDirty = true;
        markDirty();
    }

    /**
     * Returns the current document.
     *
     * @return current document model
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Returns the glyph cache for font metrics.
     *
     * @return glyph metric cache
     */
    public GlyphCache getGlyphCache() {
        return glyphCache;
    }

    /**
     * Returns the wrap map used in wrap mode.
     *
     * @return wrap map used by this viewport
     */
    public WrapMap getWrapMap() {
        return wrapMap;
    }

    public void setFoldMap(FoldMap foldMap) {
        this.foldMap = foldMap == null ? FoldMap.empty() : foldMap;
        visibleLineMap.rebuild(document == null ? 0 : document.getLineCount(), this.foldMap);
        wrapMapDirty = true;
        recomputeLongestLineLength();
        markDirty();
    }

    public FoldMap getFoldMap() {
        return foldMap;
    }

    public VisibleLineMap getVisibleLineMap() {
        return visibleLineMap;
    }

    public boolean isLogicalLineHidden(int line) {
        return visibleLineMap.isHiddenLogicalLine(line);
    }

    public int nearestVisibleLogicalLine(int line) {
        return visibleLineMap.nearestVisibleLogicalLine(line);
    }

    public int previousVisibleLogicalLine(int line) {
        return visibleLineMap.previousVisibleLogicalLine(line);
    }

    public int nextVisibleLogicalLine(int line) {
        return visibleLineMap.nextVisibleLogicalLine(line);
    }

    public int totalVisibleLogicalLines() {
        return visibleLineMap.visibleCount();
    }

    public int logicalToVisibleLine(int line) {
        return visibleLineMap.logicalToVisible(line);
    }

    public int visibleToLogicalLine(int line) {
        return visibleLineMap.visibleToLogical(line);
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
     * @return active editor theme
     */
    public CodeEditorTheme getTheme() {
        return theme;
    }

    /**
     * Returns the selection model.
     *
     * @return bound selection model
     */
    public SelectionModel getSelectionModel() {
        return selectionModel;
    }

    /**
     * Sets syntax token map used for rendering.
     *
     * @param tokenMap token map to use for syntax coloring
     */
    public void setTokenMap(TokenMap tokenMap) {
        this.tokenMap = tokenMap == null ? TokenMap.empty() : tokenMap;
        markDirty();
    }

    /**
     * Returns current token map.
     *
     * @return active syntax token map
     */
    public TokenMap getTokenMap() {
        return tokenMap;
    }

    /**
     * Sets search match highlights to render.
     *
     * @param matches current search matches
     * @param currentIndex index of current match in {@code matches}
     */
    public void setSearchMatches(List<SearchMatch> matches, int currentIndex) {
        this.searchMatches = matches == null ? List.of() : matches;
        this.currentSearchMatchIndex = currentIndex;
        this.searchMatchIndexesByLine = indexMatchesByLine(this.searchMatches);
        markDirty();
    }

    /**
     * Returns the current search matches.
     *
     * @return immutable list of current search matches
     */
    public List<SearchMatch> getSearchMatches() {
        return searchMatches;
    }

    /**
     * Sets the multi-caret model for rendering multiple carets and selections.
     *
     * @param multiCaretModel multi-caret model to render
     */
    public void setMultiCaretModel(MultiCaretModel multiCaretModel) {
        this.multiCaretModel = multiCaretModel;
    }

    /**
     * Enables/disables caret blinking and caret visibility.
     *
     * @param active {@code true} to enable caret blinking
     */
    public void setCaretBlinkActive(boolean active) {
        if (caretBlinkActive == active) {
            return;
        }
        caretBlinkActive = active;
        if (active) {
            showCaretAndRestartBlink();
        } else {
            stopCaretBlink();
            if (caretVisible) {
                caretVisible = false;
                markCaretLinesDirty();
            }
        }
    }

    /**
     * Returns true when caret blink animation is active.
     *
     * @return {@code true} when caret blink animation is active
     */
    public boolean isCaretBlinkActive() {
        return caretBlinkActive;
    }

    /**
     * Resets blink cycle and makes caret immediately visible.
     */
    public void resetCaretBlink() {
        if (disposed || !caretBlinkActive) {
            return;
        }
        if (!caretVisible) {
            caretVisible = true;
            markCaretLinesDirty();
        }
        restartCaretBlink();
    }

    /**
     * Sets the font for rendering.
     *
     * @param font font used for text metrics and rendering
     */
    public void setFont(Font font) {
        glyphCache.setFont(font);
        wrapMapDirty = true;
        markDirty();
    }

    /**
     * Sets the vertical scroll offset in pixels.
     *
     * @param offset requested vertical scroll offset
     */
    public void setScrollOffset(double offset) {
        double maxScroll = computeMaxScrollOffset(currentEffectiveTextHeight(), glyphCache.getLineHeight());
        double clamped = clampDouble(offset, 0.0, maxScroll);
        if (Double.compare(this.scrollOffset, clamped) == 0) {
            return;
        }
        this.scrollOffset = clamped;
        markDirty();
    }

    /**
     * Returns the current vertical scroll offset.
     *
     * @return current vertical scroll offset in pixels
     */
    public double getScrollOffset() {
        return scrollOffset;
    }

    /**
     * Returns the maximum vertical scroll offset for current metrics.
     *
     * @return maximum vertical scroll offset in pixels
     */
    public double getMaxScrollOffset() {
        return computeMaxScrollOffset(currentEffectiveTextHeight(), glyphCache.getLineHeight());
    }

    /**
     * Sets horizontal scroll offset in pixels.
     *
     * @param offset requested horizontal scroll offset
     */
    public void setHorizontalScrollOffset(double offset) {
        if (wordWrap) {
            if (Double.compare(horizontalScrollOffset, 0.0) == 0) {
                return;
            }
            horizontalScrollOffset = 0.0;
            markDirty();
            return;
        }
        double maxScroll = computeMaxHorizontalScrollOffset(currentEffectiveTextWidth(), glyphCache.getCharWidth());
        double clamped = clampDouble(offset, 0.0, maxScroll);
        if (Double.compare(horizontalScrollOffset, clamped) == 0) {
            return;
        }
        horizontalScrollOffset = clamped;
        markDirty();
    }

    /**
     * Returns current horizontal scroll offset.
     *
     * @return current horizontal scroll offset in pixels
     */
    public double getHorizontalScrollOffset() {
        return horizontalScrollOffset;
    }

    /**
     * Returns maximum horizontal scroll offset for current metrics.
     *
     * @return maximum horizontal scroll offset in pixels
     */
    public double getMaxHorizontalScrollOffset() {
        return computeMaxHorizontalScrollOffset(currentEffectiveTextWidth(), glyphCache.getCharWidth());
    }

    /**
     * Enables/disables soft wrap mode.
     *
     * @param wordWrap {@code true} to enable soft wrapping
     */
    public void setWordWrap(boolean wordWrap) {
        if (this.wordWrap == wordWrap) {
            return;
        }
        this.wordWrap = wordWrap;
        if (wordWrap) {
            horizontalScrollOffset = 0.0;
            if (scrollbarHoverPart == ScrollbarPart.HORIZONTAL_THUMB || scrollbarActivePart == ScrollbarPart.HORIZONTAL_THUMB) {
                scrollbarHoverPart = ScrollbarPart.NONE;
                scrollbarActivePart = ScrollbarPart.NONE;
            }
        }
        wrapMapDirty = true;
        markDirty();
    }

    /**
     * Returns whether wrap mode is active.
     *
     * @return {@code true} when soft wrap mode is active
     */
    public boolean isWordWrap() {
        return wordWrap;
    }

    /**
     * Returns true when vertical scrollbar is visible.
     *
     * @return {@code true} when vertical scrollbar is visible
     */
    public boolean isVerticalScrollbarVisible() {
        return verticalScrollbarVisible;
    }

    /**
     * Returns true when horizontal scrollbar is visible.
     *
     * @return {@code true} when horizontal scrollbar is visible
     */
    public boolean isHorizontalScrollbarVisible() {
        return horizontalScrollbarVisible;
    }

    /**
     * Returns effective drawable text width excluding scrollbar reservations.
     *
     * @return effective text width in pixels
     */
    public double getEffectiveTextWidth() {
        return currentEffectiveTextWidth();
    }

    /**
     * Returns effective drawable text height excluding scrollbar reservations.
     *
     * @return effective text height in pixels
     */
    public double getEffectiveTextHeight() {
        return currentEffectiveTextHeight();
    }

    /**
     * Returns current vertical scrollbar geometry.
     *
     * @return vertical scrollbar geometry, or {@code null} when hidden
     */
    public ScrollbarGeometry getVerticalScrollbarGeometry() {
        return verticalScrollbarGeometry;
    }

    /**
     * Returns current horizontal scrollbar geometry.
     *
     * @return horizontal scrollbar geometry, or {@code null} when hidden
     */
    public ScrollbarGeometry getHorizontalScrollbarGeometry() {
        return horizontalScrollbarGeometry;
    }

    /**
     * Returns active scrollbar hover part.
     *
     * @return hovered scrollbar part
     */
    public ScrollbarPart getScrollbarHoverPart() {
        return scrollbarHoverPart;
    }

    /**
     * Returns active scrollbar drag part.
     *
     * @return actively dragged scrollbar part
     */
    public ScrollbarPart getScrollbarActivePart() {
        return scrollbarActivePart;
    }

    /**
     * Sets hovered scrollbar part for visual state.
     *
     * @param scrollbarHoverPart hovered scrollbar part
     */
    public void setScrollbarHoverPart(ScrollbarPart scrollbarHoverPart) {
        ScrollbarPart next = scrollbarHoverPart == null ? ScrollbarPart.NONE : scrollbarHoverPart;
        if (this.scrollbarHoverPart == next) {
            return;
        }
        this.scrollbarHoverPart = next;
        markDirty();
    }

    /**
     * Sets active scrollbar part for drag visuals.
     *
     * @param scrollbarActivePart actively dragged scrollbar part
     */
    public void setScrollbarActivePart(ScrollbarPart scrollbarActivePart) {
        ScrollbarPart next = scrollbarActivePart == null ? ScrollbarPart.NONE : scrollbarActivePart;
        if (this.scrollbarActivePart == next) {
            return;
        }
        this.scrollbarActivePart = next;
        markDirty();
    }

    /**
     * Registers a listener invoked when scrollbar visibility changes.
     *
     * @param listener listener to invoke on scrollbar visibility changes
     */
    public void setOnScrollbarVisibilityChanged(Runnable listener) {
        this.scrollbarVisibilityListener = listener;
    }

    /**
     * Converts a vertical thumb top position to scroll offset.
     *
     * @param thumbTop thumb top y-coordinate
     * @return corresponding vertical scroll offset
     */
    public double verticalOffsetForThumbTop(double thumbTop) {
        if (!verticalScrollbarVisible || verticalScrollbarGeometry == null) {
            return scrollOffset;
        }
        double maxScroll = computeMaxScrollOffset(currentEffectiveTextHeight(), glyphCache.getLineHeight());
        double trackStart = verticalScrollbarGeometry.trackY() + SCROLLBAR_THUMB_PAD;
        double travel = Math.max(0.0,
            verticalScrollbarGeometry.trackHeight() - (2 * SCROLLBAR_THUMB_PAD) - verticalScrollbarGeometry.thumbHeight());
        if (maxScroll <= 0 || travel <= 0) {
            return 0.0;
        }
        double clampedTop = clampDouble(thumbTop, trackStart, trackStart + travel);
        double ratio = (clampedTop - trackStart) / travel;
        return ratio * maxScroll;
    }

    /**
     * Converts a horizontal thumb left position to scroll offset.
     *
     * @param thumbLeft thumb left x-coordinate
     * @return corresponding horizontal scroll offset
     */
    public double horizontalOffsetForThumbLeft(double thumbLeft) {
        if (!horizontalScrollbarVisible || horizontalScrollbarGeometry == null) {
            return horizontalScrollOffset;
        }
        double maxScroll = computeMaxHorizontalScrollOffset(currentEffectiveTextWidth(), glyphCache.getCharWidth());
        double trackStart = horizontalScrollbarGeometry.trackX() + SCROLLBAR_THUMB_PAD;
        double travel = Math.max(0.0,
            horizontalScrollbarGeometry.trackWidth() - (2 * SCROLLBAR_THUMB_PAD) - horizontalScrollbarGeometry.thumbWidth());
        if (maxScroll <= 0 || travel <= 0) {
            return 0.0;
        }
        double clampedLeft = clampDouble(thumbLeft, trackStart, trackStart + travel);
        double ratio = (clampedLeft - trackStart) / travel;
        return ratio * maxScroll;
    }

    /**
     * Computes scroll offset for a vertical track click using centered-thumb behavior.
     *
     * @param y click y-coordinate
     * @return target vertical scroll offset
     */
    public double verticalOffsetForTrackClick(double y) {
        if (!verticalScrollbarVisible || verticalScrollbarGeometry == null) {
            return scrollOffset;
        }
        return verticalOffsetForThumbTop(y - (verticalScrollbarGeometry.thumbHeight() * 0.5));
    }

    /**
     * Computes scroll offset for a horizontal track click using centered-thumb behavior.
     *
     * @param x click x-coordinate
     * @return target horizontal scroll offset
     */
    public double horizontalOffsetForTrackClick(double x) {
        if (!horizontalScrollbarVisible || horizontalScrollbarGeometry == null) {
            return horizontalScrollOffset;
        }
        return horizontalOffsetForThumbLeft(x - (horizontalScrollbarGeometry.thumbWidth() * 0.5));
    }

    /**
     * Returns the first visible logical line index.
     *
     * @return zero-based first visible logical line index
     */
    public int getFirstVisibleLine() {
        return firstVisibleLine;
    }

    /**
     * Returns the count of visible logical lines.
     *
     * @return number of visible logical lines
     */
    public int getVisibleLineCount() {
        return visibleLineCount;
    }

    /**
     * Marks the viewport as needing a full redraw and schedules one.
     */
    public void markDirty() {
        dirty = true;
        fullRedrawRequired = true;
        requestLayout();
    }

    /**
     * Marks specific logical lines as dirty for incremental redraw.
     *
     * @param startLine inclusive start line
     * @param endLine inclusive end line
     */
    public void markLinesDirty(int startLine, int endLine) {
        for (int i = startLine; i <= endLine; i++) {
            dirtyLines.set(i);
        }
        dirty = true;
        requestLayout();
    }

    /**
     * Returns true if the viewport needs redrawing.
     *
     * @return {@code true} when viewport has pending redraw work
     */
    public boolean isDirty() {
        return dirty;
    }

    void setCaretBlinkTimings(Duration delay, Duration period) {
        Duration safeDelay = sanitizeDuration(delay, DEFAULT_CARET_BLINK_DELAY);
        Duration safePeriod = sanitizeDuration(period, DEFAULT_CARET_BLINK_PERIOD);
        configureCaretBlink(safeDelay, safePeriod);
        if (caretBlinkActive) {
            restartCaretBlink();
        }
    }

    boolean isCaretVisible() {
        return caretVisible;
    }

    /**
     * Ensures the caret row is visible by adjusting vertical scroll offset.
     */
    public void ensureCaretVisible() {
        if (document == null) {
            return;
        }
        double lineHeight = glyphCache.getLineHeight();
        int caretLine = selectionModel.getCaretLine();
        int caretColumn = selectionModel.getCaretColumn();
        int resolvedCaretLine = isLogicalLineHidden(caretLine) ? nearestVisibleLogicalLine(caretLine) : caretLine;
        double caretY;
        if (wordWrap) {
            ensureWrapMap(currentEffectiveTextWidth(), glyphCache.getCharWidth());
            int caretRow = wrapMap.lineColumnToVisualRow(resolvedCaretLine, caretColumn);
            caretY = caretRow * lineHeight;
        } else {
            int visibleLine = logicalToVisibleLine(resolvedCaretLine);
            if (visibleLine < 0) {
                visibleLine = Math.max(0, visibleLineMap.nearestVisibleIndexForLogical(resolvedCaretLine));
            }
            caretY = visibleLine * lineHeight;
        }
        double viewportHeight = currentEffectiveTextHeight();

        if (caretY < scrollOffset) {
            setScrollOffset(caretY);
        } else if (caretY + lineHeight > scrollOffset + viewportHeight) {
            setScrollOffset(caretY + lineHeight - viewportHeight);
        }
    }

    /**
     * Ensures the caret column is visible by adjusting horizontal scroll offset.
     */
    public void ensureCaretVisibleHorizontally() {
        if (document == null || wordWrap) {
            return;
        }
        double charWidth = glyphCache.getCharWidth();
        double caretX = selectionModel.getCaretColumn() * charWidth;
        double viewportWidth = currentEffectiveTextWidth();

        if (caretX < horizontalScrollOffset) {
            setHorizontalScrollOffset(caretX);
        } else if (caretX + charWidth > horizontalScrollOffset + viewportWidth) {
            setHorizontalScrollOffset(caretX + charWidth - viewportWidth);
        }
    }

    /**
     * Returns logical line index at y coordinate, or -1 when above content.
     *
     * @param y y-coordinate in local viewport space
     * @return zero-based logical line index, or {@code -1}
     */
    public int getLineAtY(double y) {
        HitPosition hit = getHitPosition(0.0, y);
        return hit.line();
    }

    /**
     * Returns column index at x coordinate in current mode.
     *
     * @param x x-coordinate in local viewport space
     * @return zero-based column index
     */
    public int getColumnAtX(double x) {
        double charWidth = glyphCache.getCharWidth();
        if (wordWrap) {
            int column = (int) Math.round(x / charWidth);
            return Math.max(0, column);
        }
        int column = (int) Math.round((x + horizontalScrollOffset) / charWidth);
        return Math.max(0, column);
    }

    /**
     * Resolves a wrap-aware hit position for local viewport coordinates.
     *
     * @param localX local x-coordinate
     * @param localY local y-coordinate
     * @return resolved hit position
     */
    public HitPosition getHitPosition(double localX, double localY) {
        if (document == null || document.getLineCount() <= 0) {
            return new HitPosition(-1, 0);
        }
        double lineHeight = glyphCache.getLineHeight();
        double charWidth = glyphCache.getCharWidth();
        if (wordWrap) {
            ensureWrapMap(currentEffectiveTextWidth(), charWidth);
            int totalRows = Math.max(1, wrapMap.totalVisualRows());
            int visualRow = clamp((int) Math.floor((localY + scrollOffset) / lineHeight), 0, totalRows - 1);
            WrapMap.VisualRow row = wrapMap.visualRow(visualRow);
            int column = (int) Math.round(localX / charWidth) + row.startColumn();
            int clampedColumn = clamp(column, row.startColumn(), row.endColumn());
            return new HitPosition(row.lineIndex(), clampColumn(row.lineIndex(), clampedColumn));
        }
        int line = (int) Math.floor((localY + scrollOffset) / lineHeight);
        if (line < 0) {
            return new HitPosition(-1, 0);
        }
        if (visibleLineMap.visibleCount() <= 0) {
            return new HitPosition(-1, 0);
        }
        int safeVisibleLine = clamp(line, 0, visibleLineMap.visibleCount() - 1);
        int safeLine = visibleLineMap.visibleToLogical(safeVisibleLine);
        int column = (int) Math.round((localX + horizontalScrollOffset) / charWidth);
        return new HitPosition(safeLine, clampColumn(safeLine, Math.max(0, column)));
    }

    private void onDocumentChanged(DocumentChangeEvent event) {
        visibleLineMap.rebuild(document == null ? 0 : document.getLineCount(), foldMap);
        recomputeLongestLineLength();
        wrapMapDirty = true;
        if (document == null) {
            markDirty();
            return;
        }
        resetCaretBlink();
        if (wordWrap || foldMap.hasCollapsedRegions()) {
            markDirty();
            return;
        }
        ViewportInvalidationPlanner.InvalidationPlan plan = invalidationPlanner.plan(
            document,
            event,
            firstVisibleLine,
            visibleLineCount,
            PREFETCH_LINES
        );
        if (plan.fullRedraw()) {
            markDirty();
            return;
        }
        if (plan.hasLineRange()) {
            markLinesDirty(plan.startLine(), plan.endLine());
        }
    }

    private void onCaretLineChanged(int oldLine, int newLine) {
        dirtyLines.set(oldLine);
        dirtyLines.set(newLine);
        markSelectionRangeDirty();
        resetCaretBlink();
        dirty = true;
        requestLayout();
    }

    private void onCaretColumnChanged() {
        int caretLine = selectionModel.getCaretLine();
        dirtyLines.set(caretLine);
        markSelectionRangeDirty();
        resetCaretBlink();
        dirty = true;
        requestLayout();
    }

    private void onSelectionAnchorChanged() {
        markSelectionRangeDirty();
        resetCaretBlink();
        dirty = true;
        requestLayout();
    }

    private void markSelectionRangeDirty() {
        if (multiCaretModel != null && multiCaretModel.hasMultipleCarets()) {
            fullRedrawRequired = true;
            return;
        }
        boolean hasSelection = selectionModel.hasSelection();
        if (!previousSelectionActive && !hasSelection) {
            return;
        }
        if (previousSelectionActive) {
            dirtyLineRange(previousSelectionStartLine, previousSelectionEndLine);
        }
        if (hasSelection) {
            int startLine = selectionModel.getSelectionStartLine();
            int endLine = selectionModel.getSelectionEndLine();
            dirtyLineRange(startLine, endLine);
            previousSelectionStartLine = startLine;
            previousSelectionEndLine = endLine;
        } else {
            previousSelectionStartLine = -1;
            previousSelectionEndLine = -1;
        }
        previousSelectionActive = hasSelection;
    }

    private void dirtyLineRange(int startLine, int endLine) {
        if (startLine < 0 || endLine < 0 || startLine > endLine) {
            return;
        }
        dirtyLines.set(startLine, endLine + 1);
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        if (w != canvas.getWidth() || h != canvas.getHeight()) {
            canvas.setWidth(w);
            canvas.setHeight(h);
            fullRedrawRequired = true;
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
        double charWidth = glyphCache.getCharWidth();
        List<CaretRange> activeCarets = collectActiveCarets();
        boolean hasMultiCarets = !activeCarets.isEmpty();
        boolean paintCaret = shouldPaintCaret();

        int previousVisibleAnchor = firstVisibleVisualRow;

        resolveViewportMetrics(w, h, lineHeight, charWidth);
        computeVisibleRange(lineHeight);
        buildRenderLines(lineHeight);

        int currentVisibleAnchor = firstVisibleVisualRow;

        boolean doFullRedraw = fullRedrawRequired || previousVisibleAnchor != currentVisibleAnchor;
        fullRedrawRequired = false;

        RenderContext renderContext = new RenderContext(
            gc,
            theme,
            glyphCache,
            selectionModel,
            renderLines,
            activeCarets,
            hasMultiCarets,
            paintCaret,
            searchMatches,
            searchMatchIndexesByLine,
            currentSearchMatchIndex,
            w,
            h,
            effectiveTextWidth,
            effectiveTextHeight,
            lineHeight,
            charWidth,
            glyphCache.getBaselineOffset(),
            scrollOffset,
            horizontalScrollOffset,
            wordWrap,
            wrapMap,
            verticalScrollbarVisible,
            horizontalScrollbarVisible,
            verticalScrollbarGeometry,
            horizontalScrollbarGeometry,
            scrollbarHoverPart,
            scrollbarActivePart
        );

        if (doFullRedraw) {
            dirtyLines.clear();
            for (RenderPass renderPass : renderPasses) {
                renderPass.renderFull(renderContext);
            }
        } else {
            int caretLine = selectionModel.getCaretLine();
            dirtyLines.set(caretLine);
            if (previousCaretLine >= 0) {
                dirtyLines.set(previousCaretLine);
            }

            for (RenderPass renderPass : renderPasses) {
                for (RenderLine renderLine : renderLines) {
                    if (dirtyLines.get(renderLine.lineIndex())) {
                        renderPass.renderLine(renderContext, renderLine);
                    }
                }
            }
            dirtyLines.clear();
        }
        previousCaretLine = selectionModel.getCaretLine();
    }

    private void resolveViewportMetrics(double width, double height, double lineHeight, double charWidth) {
        boolean previousVerticalVisible = verticalScrollbarVisible;
        boolean previousHorizontalVisible = horizontalScrollbarVisible;

        boolean nextVerticalVisible = previousVerticalVisible;
        boolean nextHorizontalVisible = !wordWrap && previousHorizontalVisible;

        for (int i = 0; i < 4; i++) {
            double candidateWidth = Math.max(0.0, width - (nextVerticalVisible ? SCROLLBAR_WIDTH : 0.0));
            double candidateHeight = Math.max(0.0, height - (nextHorizontalVisible ? SCROLLBAR_WIDTH : 0.0));

            if (wordWrap) {
                ensureWrapMap(candidateWidth, charWidth);
            }

            double maxVertical = computeMaxScrollOffset(candidateHeight, lineHeight);
            boolean computedVerticalVisible = maxVertical > 0.0;
            double maxHorizontal = wordWrap ? 0.0 : computeMaxHorizontalScrollOffset(candidateWidth, charWidth);
            boolean computedHorizontalVisible = !wordWrap && maxHorizontal > 0.0;

            if (computedVerticalVisible == nextVerticalVisible
                && computedHorizontalVisible == nextHorizontalVisible) {
                break;
            }
            nextVerticalVisible = computedVerticalVisible;
            nextHorizontalVisible = computedHorizontalVisible;
        }

        verticalScrollbarVisible = nextVerticalVisible;
        horizontalScrollbarVisible = !wordWrap && nextHorizontalVisible;

        effectiveTextWidth = Math.max(0.0, width - (verticalScrollbarVisible ? SCROLLBAR_WIDTH : 0.0));
        effectiveTextHeight = Math.max(0.0, height - (horizontalScrollbarVisible ? SCROLLBAR_WIDTH : 0.0));

        if (wordWrap) {
            ensureWrapMap(effectiveTextWidth, charWidth);
            horizontalScrollOffset = 0.0;
        }

        double maxVerticalOffset = computeMaxScrollOffset(effectiveTextHeight, lineHeight);
        scrollOffset = clampDouble(scrollOffset, 0.0, maxVerticalOffset);

        double maxHorizontalOffset = computeMaxHorizontalScrollOffset(effectiveTextWidth, charWidth);
        horizontalScrollOffset = wordWrap
            ? 0.0
            : clampDouble(horizontalScrollOffset, 0.0, maxHorizontalOffset);

        double contentHeight = computeContentHeight(lineHeight);
        double contentWidth = computeContentWidth(charWidth);

        verticalScrollbarGeometry = verticalScrollbarVisible
            ? buildVerticalScrollbarGeometry(contentHeight, maxVerticalOffset)
            : null;
        horizontalScrollbarGeometry = horizontalScrollbarVisible
            ? buildHorizontalScrollbarGeometry(contentWidth, maxHorizontalOffset)
            : null;

        normalizeScrollbarInteractionState();

        if ((previousVerticalVisible != verticalScrollbarVisible
            || previousHorizontalVisible != horizontalScrollbarVisible)
            && scrollbarVisibilityListener != null) {
            scrollbarVisibilityListener.run();
        }
    }

    private void computeVisibleRange(double lineHeight) {
        if (document == null || visibleLineMap.visibleCount() <= 0) {
            firstVisibleLine = 0;
            visibleLineCount = 0;
            firstVisibleVisualRow = 0;
            visibleVisualRowCount = 0;
            return;
        }

        if (wordWrap) {
            int totalRows = Math.max(1, wrapMap.totalVisualRows());
            firstVisibleVisualRow = Math.max(0, (int) (scrollOffset / lineHeight) - PREFETCH_LINES);
            int lastVisibleRow = Math.min(
                totalRows - 1,
                (int) ((scrollOffset + effectiveTextHeight) / lineHeight) + PREFETCH_LINES
            );
            visibleVisualRowCount = Math.max(0, lastVisibleRow - firstVisibleVisualRow + 1);
            WrapMap.VisualRow firstRow = wrapMap.visualRow(firstVisibleVisualRow);
            firstVisibleLine = firstRow.lineIndex();
            return;
        }

        int totalVisibleLines = visibleLineMap.visibleCount();
        int firstVisibleLogicalIndex = Math.max(0, (int) (scrollOffset / lineHeight) - PREFETCH_LINES);
        int lastVisibleLogicalIndex = Math.min(
            totalVisibleLines - 1,
            (int) ((scrollOffset + effectiveTextHeight) / lineHeight) + PREFETCH_LINES
        );
        visibleLineCount = Math.max(0, lastVisibleLogicalIndex - firstVisibleLogicalIndex + 1);
        firstVisibleVisualRow = firstVisibleLogicalIndex;
        firstVisibleLine = visibleLineMap.visibleToLogical(firstVisibleLogicalIndex);
        visibleVisualRowCount = visibleLineCount;
    }

    private void buildRenderLines(double lineHeight) {
        renderLines.clear();
        if (document == null) {
            visibleLineCount = 0;
            return;
        }

        if (wordWrap) {
            int totalRows = Math.max(1, wrapMap.totalVisualRows());
            for (int i = 0; i < visibleVisualRowCount; i++) {
                int visualRow = firstVisibleVisualRow + i;
                if (visualRow >= totalRows) {
                    break;
                }
                WrapMap.VisualRow row = wrapMap.visualRow(visualRow);
                int lineIndex = row.lineIndex();
                String lineText = document.getLineText(lineIndex);
                int start = clamp(row.startColumn(), 0, lineText.length());
                int end = clamp(row.endColumn(), start, lineText.length());
                String text = lineText.substring(start, end);
                double y = visualRow * lineHeight - scrollOffset;
                renderLines.add(new RenderLine(
                    lineIndex,
                    start,
                    end,
                    text,
                    y,
                    tokenMap.tokensForLine(lineIndex)
                ));
            }
            int count = 0;
            int previousLine = -1;
            for (RenderLine renderLine : renderLines) {
                if (renderLine.lineIndex() != previousLine) {
                    count++;
                    previousLine = renderLine.lineIndex();
                }
            }
            visibleLineCount = count;
            return;
        }

        for (int i = 0; i < visibleLineCount; i++) {
            int visibleIndex = firstVisibleVisualRow + i;
            if (visibleIndex >= visibleLineMap.visibleCount()) {
                break;
            }
            int lineIndex = visibleLineMap.visibleToLogical(visibleIndex);
            String text = document.getLineText(lineIndex);
            double y = visibleIndex * lineHeight - scrollOffset;
            renderLines.add(new RenderLine(
                lineIndex,
                0,
                text.length(),
                text,
                y,
                tokenMap.tokensForLine(lineIndex)
            ));
        }
    }

    private void ensureWrapMap(double viewportWidth, double charWidth) {
        if (!wordWrap) {
            return;
        }
        boolean viewportChanged = Math.abs(viewportWidth - lastWrapViewportWidth) > METRIC_EPSILON;
        boolean charWidthChanged = Math.abs(charWidth - lastWrapCharWidth) > METRIC_EPSILON;
        if (!wrapMapDirty && !viewportChanged && !charWidthChanged) {
            return;
        }
        wrapMap.rebuild(document, viewportWidth, charWidth, line -> !foldMap.isHiddenLine(line));
        wrapMapDirty = false;
        lastWrapViewportWidth = viewportWidth;
        lastWrapCharWidth = charWidth;
    }

    private ScrollbarGeometry buildVerticalScrollbarGeometry(double contentHeight, double maxOffset) {
        double trackX = effectiveTextWidth;
        double trackY = 0.0;
        double trackWidth = SCROLLBAR_WIDTH;
        double trackHeight = effectiveTextHeight;
        double thumbWidth = Math.max(1.0, trackWidth - (SCROLLBAR_THUMB_PAD * 2));
        double usableTrack = Math.max(0.0, trackHeight - (SCROLLBAR_THUMB_PAD * 2));

        double ratio = contentHeight <= 0 ? 1.0 : effectiveTextHeight / contentHeight;
        double thumbHeight = clampDouble(Math.max(MIN_THUMB_SIZE, usableTrack * ratio), 0.0, usableTrack);
        double thumbTravel = Math.max(0.0, usableTrack - thumbHeight);
        double thumbY = trackY + SCROLLBAR_THUMB_PAD
            + (maxOffset <= 0 ? 0.0 : (scrollOffset / maxOffset) * thumbTravel);
        double thumbX = trackX + SCROLLBAR_THUMB_PAD;

        return new ScrollbarGeometry(
            trackX,
            trackY,
            trackWidth,
            trackHeight,
            thumbX,
            thumbY,
            thumbWidth,
            thumbHeight
        );
    }

    private ScrollbarGeometry buildHorizontalScrollbarGeometry(double contentWidth, double maxOffset) {
        double trackX = 0.0;
        double trackY = effectiveTextHeight;
        double trackWidth = effectiveTextWidth;
        double trackHeight = SCROLLBAR_WIDTH;
        double thumbHeight = Math.max(1.0, trackHeight - (SCROLLBAR_THUMB_PAD * 2));
        double usableTrack = Math.max(0.0, trackWidth - (SCROLLBAR_THUMB_PAD * 2));

        double ratio = contentWidth <= 0 ? 1.0 : effectiveTextWidth / contentWidth;
        double thumbWidth = clampDouble(Math.max(MIN_THUMB_SIZE, usableTrack * ratio), 0.0, usableTrack);
        double thumbTravel = Math.max(0.0, usableTrack - thumbWidth);
        double thumbX = trackX + SCROLLBAR_THUMB_PAD
            + (maxOffset <= 0 ? 0.0 : (horizontalScrollOffset / maxOffset) * thumbTravel);
        double thumbY = trackY + SCROLLBAR_THUMB_PAD;

        return new ScrollbarGeometry(
            trackX,
            trackY,
            trackWidth,
            trackHeight,
            thumbX,
            thumbY,
            thumbWidth,
            thumbHeight
        );
    }

    private void normalizeScrollbarInteractionState() {
        if (!verticalScrollbarVisible
            && (scrollbarHoverPart == ScrollbarPart.VERTICAL_THUMB || scrollbarActivePart == ScrollbarPart.VERTICAL_THUMB)) {
            scrollbarHoverPart = ScrollbarPart.NONE;
            scrollbarActivePart = ScrollbarPart.NONE;
        }
        if (!horizontalScrollbarVisible
            && (scrollbarHoverPart == ScrollbarPart.HORIZONTAL_THUMB
            || scrollbarActivePart == ScrollbarPart.HORIZONTAL_THUMB)) {
            scrollbarHoverPart = ScrollbarPart.NONE;
            scrollbarActivePart = ScrollbarPart.NONE;
        }
    }

    private void recomputeLongestLineLength() {
        if (document == null) {
            longestLineLength = 0;
            return;
        }
        int max = 0;
        int visibleCount = visibleLineMap.visibleCount();
        for (int index = 0; index < visibleCount; index++) {
            int line = visibleLineMap.visibleToLogical(index);
            max = Math.max(max, document.getLineText(line).length());
        }
        longestLineLength = max;
    }

    private double computeContentHeight(double lineHeight) {
        if (document == null) {
            return 0.0;
        }
        if (wordWrap) {
            int totalRows = Math.max(1, wrapMap.totalVisualRows());
            return totalRows * lineHeight;
        }
        return visibleLineMap.visibleCount() * lineHeight;
    }

    private double computeContentWidth(double charWidth) {
        if (document == null) {
            return 0.0;
        }
        return Math.max(0.0, longestLineLength * charWidth);
    }

    private double computeMaxScrollOffset(double viewportHeight, double lineHeight) {
        double contentHeight = computeContentHeight(lineHeight);
        return Math.max(0.0, contentHeight - Math.max(0.0, viewportHeight));
    }

    private double computeMaxHorizontalScrollOffset(double viewportWidth, double charWidth) {
        if (document == null || wordWrap) {
            return 0.0;
        }
        double contentWidth = computeContentWidth(charWidth);
        return Math.max(0.0, contentWidth - Math.max(0.0, viewportWidth));
    }

    private double currentEffectiveTextWidth() {
        if (effectiveTextWidth > 0) {
            return effectiveTextWidth;
        }
        return getWidth();
    }

    private double currentEffectiveTextHeight() {
        if (effectiveTextHeight > 0) {
            return effectiveTextHeight;
        }
        return getHeight();
    }

    private int clampColumn(int line, int column) {
        if (document == null || document.getLineCount() == 0) {
            return 0;
        }
        int safeLine = clamp(line, 0, document.getLineCount() - 1);
        int maxColumn = document.getLineText(safeLine).length();
        return clamp(column, 0, maxColumn);
    }

    private List<CaretRange> collectActiveCarets() {
        if (document == null || multiCaretModel == null || !multiCaretModel.hasMultipleCarets()) {
            return List.of();
        }
        return multiCaretModel.allCarets(document);
    }

    private Map<Integer, List<Integer>> indexMatchesByLine(List<SearchMatch> matches) {
        if (matches.isEmpty()) {
            return Map.of();
        }
        Map<Integer, List<Integer>> byLine = new HashMap<>();
        for (int i = 0; i < matches.size(); i++) {
            byLine.computeIfAbsent(matches.get(i).line(), key -> new ArrayList<>()).add(i);
        }
        return byLine;
    }

    private boolean shouldPaintCaret() {
        return caretBlinkActive && caretVisible;
    }

    private void showCaretAndRestartBlink() {
        if (!caretVisible) {
            caretVisible = true;
        }
        markCaretLinesDirty();
        restartCaretBlink();
    }

    private void restartCaretBlink() {
        if (disposed || !caretBlinkActive) {
            return;
        }
        stopCaretBlink();
        caretBlinkDelay.playFromStart();
    }

    private void stopCaretBlink() {
        caretBlinkDelay.stop();
        caretBlinkTimeline.stop();
    }

    private void toggleCaretVisibility() {
        if (disposed || !caretBlinkActive) {
            return;
        }
        caretVisible = !caretVisible;
        markCaretLinesDirty();
    }

    private void markCaretLinesDirty() {
        if (document == null) {
            return;
        }
        List<CaretRange> activeCarets = collectActiveCarets();
        if (!activeCarets.isEmpty()) {
            for (CaretRange caret : activeCarets) {
                dirtyLines.set(caret.caretLine());
            }
        } else {
            dirtyLines.set(selectionModel.getCaretLine());
            if (previousCaretLine >= 0) {
                dirtyLines.set(previousCaretLine);
            }
        }
        dirty = true;
        requestLayout();
    }

    private void configureCaretBlink(Duration delay, Duration period) {
        caretBlinkDelay.setDuration(delay);
        caretBlinkTimeline.getKeyFrames().setAll(new KeyFrame(period, event -> toggleCaretVisibility()));
        caretBlinkTimeline.setCycleCount(Animation.INDEFINITE);
    }

    private Duration sanitizeDuration(Duration value, Duration fallback) {
        if (value == null || value.isUnknown() || value.lessThanOrEqualTo(Duration.ZERO)) {
            return fallback;
        }
        return value;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    /**
     * Releases listeners and cached render data for this viewport.
     */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        stopCaretBlink();
        caretBlinkActive = false;
        caretVisible = false;
        setDocument(null);
        selectionModel.caretLineProperty().removeListener(caretLineListener);
        selectionModel.caretColumnProperty().removeListener(caretColumnListener);
        selectionModel.anchorLineProperty().removeListener(anchorLineListener);
        selectionModel.anchorColumnProperty().removeListener(anchorColumnListener);
        renderLines.clear();
        dirtyLines.clear();
        tokenMap = TokenMap.empty();
        searchMatches = List.of();
        searchMatchIndexesByLine = Map.of();
        currentSearchMatchIndex = -1;
        verticalScrollbarGeometry = null;
        horizontalScrollbarGeometry = null;
        scrollbarHoverPart = ScrollbarPart.NONE;
        scrollbarActivePart = ScrollbarPart.NONE;
    }

    /**
     * Logical hit-test result in editor coordinates.
     *
     * @param line zero-based line index in the document
     * @param column zero-based column index within {@code line}
     */
    public record HitPosition(int line, int column) {
    }

    /**
     * Scrollbar geometry snapshot for pointer hit-testing and rendering.
     *
     * @param trackX track left x-coordinate in local coordinates
     * @param trackY track top y-coordinate in local coordinates
     * @param trackWidth track width in pixels
     * @param trackHeight track height in pixels
     * @param thumbX thumb left x-coordinate in local coordinates
     * @param thumbY thumb top y-coordinate in local coordinates
     * @param thumbWidth thumb width in pixels
     * @param thumbHeight thumb height in pixels
     */
    public record ScrollbarGeometry(
        double trackX,
        double trackY,
        double trackWidth,
        double trackHeight,
        double thumbX,
        double thumbY,
        double thumbWidth,
        double thumbHeight
    ) {
        /**
         * Returns whether the provided point is inside the scrollbar track.
         *
         * @param x point x-coordinate in local coordinates
         * @param y point y-coordinate in local coordinates
         * @return {@code true} when the point is inside the track bounds
         */
        public boolean containsTrack(double x, double y) {
            return x >= trackX && x <= trackX + trackWidth && y >= trackY && y <= trackY + trackHeight;
        }

        /**
         * Returns whether the provided point is inside the scrollbar thumb.
         *
         * @param x point x-coordinate in local coordinates
         * @param y point y-coordinate in local coordinates
         * @return {@code true} when the point is inside the thumb bounds
         */
        public boolean containsThumb(double x, double y) {
            return x >= thumbX && x <= thumbX + thumbWidth && y >= thumbY && y <= thumbY + thumbHeight;
        }
    }

    /**
     * Scrollbar part state used for hover/active visuals.
     */
    public enum ScrollbarPart {
        /**
         * No hovered or active scrollbar part.
         */
        NONE,
        /**
         * Vertical scrollbar thumb.
         */
        VERTICAL_THUMB,
        /**
         * Horizontal scrollbar thumb.
         */
        HORIZONTAL_THUMB
    }
}
