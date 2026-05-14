package org.metalib.papifly.fx.code.render;

import javafx.scene.canvas.GraphicsContext;
import org.metalib.papifly.fx.code.command.CaretRange;
import org.metalib.papifly.fx.code.search.SearchMatch;
import org.metalib.papifly.fx.code.theme.CodeEditorTheme;

import java.util.List;
import java.util.Map;

/**
 * Immutable render frame context shared by viewport render passes.
 */
record RenderContext(
    GraphicsContext graphics,
    CodeEditorTheme theme,
    GlyphCache glyphCache,
    SelectionModel selectionModel,
    List<RenderLine> renderLines,
    List<CaretRange> activeCarets,
    boolean hasMultiCarets,
    boolean paintCaret,
    List<SearchMatch> searchMatches,
    Map<Integer, List<Integer>> searchMatchIndexesByLine,
    int currentSearchMatchIndex,
    double viewportWidth,
    double viewportHeight,
    double effectiveTextWidth,
    double effectiveTextHeight,
    double lineHeight,
    double charWidth,
    double baseline,
    double scrollOffset,
    double horizontalScrollOffset,
    boolean wordWrap,
    WrapMap wrapMap,
    boolean verticalScrollbarVisible,
    boolean horizontalScrollbarVisible,
    Viewport.ScrollbarGeometry verticalScrollbarGeometry,
    Viewport.ScrollbarGeometry horizontalScrollbarGeometry,
    Viewport.ScrollbarPart scrollbarHoverPart,
    Viewport.ScrollbarPart scrollbarActivePart
) {

    boolean isLineVisible(int line) {
        for (RenderLine renderLine : renderLines) {
            if (renderLine.lineIndex() == line) {
                return true;
            }
        }
        return false;
    }

    double lineToY(int line) {
        if (!wordWrap || wrapMap == null) {
            return line * lineHeight - scrollOffset;
        }
        return wrapMap.lineToFirstVisualRow(line) * lineHeight - scrollOffset;
    }

    double textOriginX() {
        return wordWrap ? 0.0 : -horizontalScrollOffset;
    }
}
