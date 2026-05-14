package org.metalib.papifly.fx.code.api;

import javafx.beans.property.DoubleProperty;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.gutter.GutterView;
import org.metalib.papifly.fx.code.render.SelectionModel;
import org.metalib.papifly.fx.code.render.Viewport;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Coordinates caret movement and vertical scroll synchronization.
 */
final class EditorCaretCoordinator {

    private final Document document;
    private final SelectionModel selectionModel;
    private final Viewport viewport;
    private final GutterView gutterView;
    private final DoubleProperty verticalScrollOffset;
    private final DoubleProperty horizontalScrollOffset;
    private final BooleanSupplier disposedSupplier;

    private boolean syncingScrollOffset;
    private boolean syncingHorizontalScrollOffset;
    private int preferredVerticalColumn = -1;

    EditorCaretCoordinator(
        Document document,
        SelectionModel selectionModel,
        Viewport viewport,
        GutterView gutterView,
        DoubleProperty verticalScrollOffset,
        DoubleProperty horizontalScrollOffset,
        BooleanSupplier disposedSupplier
    ) {
        this.document = Objects.requireNonNull(document, "document");
        this.selectionModel = Objects.requireNonNull(selectionModel, "selectionModel");
        this.viewport = Objects.requireNonNull(viewport, "viewport");
        this.gutterView = Objects.requireNonNull(gutterView, "gutterView");
        this.verticalScrollOffset = Objects.requireNonNull(verticalScrollOffset, "verticalScrollOffset");
        this.horizontalScrollOffset = Objects.requireNonNull(horizontalScrollOffset, "horizontalScrollOffset");
        this.disposedSupplier = Objects.requireNonNull(disposedSupplier, "disposedSupplier");
    }

    void clearPreferredVerticalColumn() {
        preferredVerticalColumn = -1;
    }

    void moveCaret(int line, int col, boolean extendSelection) {
        clearPreferredVerticalColumn();
        moveCaretInternal(line, col, extendSelection);
    }

    void moveCaretVertically(int targetLine, boolean extendSelection) {
        int safeLine = clampVisibleLine(targetLine);
        int preferredColumn = preferredVerticalColumn >= 0
            ? preferredVerticalColumn
            : selectionModel.getCaretColumn();
        int targetColumn = Math.min(preferredColumn, document.getLineText(safeLine).length());
        moveCaretInternal(safeLine, targetColumn, extendSelection);
        preferredVerticalColumn = preferredColumn;
    }

    void moveCaretToOffset(int offset) {
        clearPreferredVerticalColumn();
        int safeOffset = Math.max(0, Math.min(offset, document.length()));
        int line = document.getLineForOffset(safeOffset);
        int visibleLine = clampVisibleLine(line);
        int col = visibleLine == line
            ? document.getColumnForOffset(safeOffset)
            : Math.min(document.getColumnForOffset(safeOffset), document.getLineText(visibleLine).length());
        selectionModel.moveCaret(visibleLine, col);
        viewport.ensureCaretVisible();
        viewport.ensureCaretVisibleHorizontally();
        syncVerticalScrollOffsetFromViewport();
        syncHorizontalScrollOffsetFromViewport();
        syncGutterScroll();
    }

    void applyScrollOffset(double requestedOffset) {
        if (disposedSupplier.getAsBoolean() || syncingScrollOffset) {
            return;
        }
        viewport.setScrollOffset(requestedOffset);
        syncVerticalScrollOffsetFromViewport();
        syncGutterScroll();
    }

    void applyHorizontalScrollOffset(double requestedOffset) {
        if (disposedSupplier.getAsBoolean() || syncingHorizontalScrollOffset) {
            return;
        }
        viewport.setHorizontalScrollOffset(requestedOffset);
        syncHorizontalScrollOffsetFromViewport();
    }

    void syncVerticalScrollOffsetFromViewport() {
        double actualOffset = viewport.getScrollOffset();
        if (Double.compare(verticalScrollOffset.get(), actualOffset) == 0) {
            return;
        }
        syncingScrollOffset = true;
        try {
            verticalScrollOffset.set(actualOffset);
        } finally {
            syncingScrollOffset = false;
        }
    }

    void syncHorizontalScrollOffsetFromViewport() {
        double actualOffset = viewport.getHorizontalScrollOffset();
        if (Double.compare(horizontalScrollOffset.get(), actualOffset) == 0) {
            return;
        }
        syncingHorizontalScrollOffset = true;
        try {
            horizontalScrollOffset.set(actualOffset);
        } finally {
            syncingHorizontalScrollOffset = false;
        }
    }

    int computePageLineDelta() {
        double lineHeight = viewport.getGlyphCache().getLineHeight();
        if (lineHeight <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.floor(computePagePixelDelta() / lineHeight));
    }

    double computePagePixelDelta() {
        double viewportHeight = viewport.getHeight();
        if (viewportHeight <= 0) {
            return Math.max(1.0, viewport.getGlyphCache().getLineHeight());
        }
        return viewportHeight;
    }

    private void moveCaretInternal(int line, int col, boolean extendSelection) {
        int safeLine = clampVisibleLine(line);
        int safeColumn = clampColumn(safeLine, col);
        if (extendSelection) {
            selectionModel.moveCaretWithSelection(safeLine, safeColumn);
        } else {
            selectionModel.moveCaret(safeLine, safeColumn);
        }
        viewport.ensureCaretVisible();
        viewport.ensureCaretVisibleHorizontally();
        syncVerticalScrollOffsetFromViewport();
        syncHorizontalScrollOffsetFromViewport();
        syncGutterScroll();
    }

    private void syncGutterScroll() {
        gutterView.setScrollOffset(viewport.getScrollOffset());
    }

    private int clampLine(int line) {
        int maxLine = Math.max(0, document.getLineCount() - 1);
        return Math.max(0, Math.min(line, maxLine));
    }

    private int clampVisibleLine(int line) {
        int clamped = clampLine(line);
        if (viewport.isLogicalLineHidden(clamped)) {
            return viewport.nearestVisibleLogicalLine(clamped);
        }
        return clamped;
    }

    private int clampColumn(int line, int column) {
        int maxColumn = document.getLineText(line).length();
        return Math.max(0, Math.min(column, maxColumn));
    }
}
