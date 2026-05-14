package org.metalib.papifly.fx.code.render;

import javafx.scene.canvas.GraphicsContext;
import org.metalib.papifly.fx.code.search.SearchMatch;

import java.util.List;

/**
 * Paints search match highlight overlays.
 */
final class SearchPass implements RenderPass {

    @Override
    public void renderFull(RenderContext context) {
        if (context.searchMatches().isEmpty()) {
            return;
        }
        for (RenderLine renderLine : context.renderLines()) {
            renderLine(context, renderLine);
        }
    }

    @Override
    public void renderLine(RenderContext context, RenderLine renderLine) {
        if (context.searchMatches().isEmpty()) {
            return;
        }
        List<Integer> lineIndexes = context.searchMatchIndexesByLine().get(renderLine.lineIndex());
        if (lineIndexes == null || lineIndexes.isEmpty()) {
            return;
        }
        GraphicsContext gc = context.graphics();
        int rowStart = renderLine.startColumn();
        int rowEnd = renderLine.endColumn();
        for (int matchIndex : lineIndexes) {
            SearchMatch match = context.searchMatches().get(matchIndex);
            int startColumn = Math.max(rowStart, match.startColumn());
            int endColumn = Math.min(rowEnd, match.endColumn());
            if (endColumn <= startColumn) {
                continue;
            }
            double x = context.textOriginX() + ((startColumn - rowStart) * context.charWidth());
            double width = (endColumn - startColumn) * context.charWidth();
            if (width <= 0) {
                continue;
            }
            double left = Math.max(0.0, x);
            double right = Math.min(context.effectiveTextWidth(), x + width);
            if (right <= left) {
                continue;
            }
            gc.setFill(matchIndex == context.currentSearchMatchIndex()
                ? context.theme().searchCurrentColor()
                : context.theme().searchHighlightColor());
            gc.fillRect(left, renderLine.y(), right - left, context.lineHeight());
        }
    }
}
