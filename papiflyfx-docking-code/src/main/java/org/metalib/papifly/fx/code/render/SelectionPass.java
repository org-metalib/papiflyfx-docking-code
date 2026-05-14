package org.metalib.papifly.fx.code.render;

import javafx.scene.canvas.GraphicsContext;
import org.metalib.papifly.fx.code.command.CaretRange;

/**
 * Paints primary and multi-caret selection overlays.
 */
final class SelectionPass implements RenderPass {

    @Override
    public void renderFull(RenderContext context) {
        if (!hasAnySelection(context)) {
            return;
        }
        GraphicsContext gc = context.graphics();
        gc.setFill(context.theme().selectionColor());
        for (RenderLine renderLine : context.renderLines()) {
            renderLine(context, renderLine);
        }
    }

    @Override
    public void renderLine(RenderContext context, RenderLine renderLine) {
        if (!hasAnySelection(context)) {
            return;
        }
        GraphicsContext gc = context.graphics();
        gc.setFill(context.theme().selectionColor());
        if (context.hasMultiCarets()) {
            for (CaretRange caret : context.activeCarets()) {
                if (caret.hasSelection()) {
                    paintRangeForLine(context, renderLine,
                        caret.getStartLine(), caret.getStartColumn(), caret.getEndLine(), caret.getEndColumn());
                }
            }
            return;
        }
        if (!context.selectionModel().hasSelection()) {
            return;
        }
        paintRangeForLine(context, renderLine,
            context.selectionModel().getSelectionStartLine(),
            context.selectionModel().getSelectionStartColumn(),
            context.selectionModel().getSelectionEndLine(),
            context.selectionModel().getSelectionEndColumn());
    }

    private boolean hasAnySelection(RenderContext context) {
        if (!context.hasMultiCarets()) {
            return context.selectionModel().hasSelection();
        }
        for (CaretRange caret : context.activeCarets()) {
            if (caret.hasSelection()) {
                return true;
            }
        }
        return false;
    }

    private void paintRangeForLine(
        RenderContext context,
        RenderLine renderLine,
        int startLine,
        int startCol,
        int endLine,
        int endCol
    ) {
        if (!context.wordWrap()) {
            paintUnwrappedRange(context, renderLine, startLine, startCol, endLine, endCol);
            return;
        }
        SelectionGeometry.SelectionSpan span = SelectionGeometry.spanForVisualRow(
            renderLine,
            context.charWidth(),
            startLine,
            startCol,
            endLine,
            endCol
        );
        if (span == null) {
            return;
        }
        double x = context.textOriginX() + span.x();
        double left = Math.max(0.0, x);
        double right = Math.min(context.effectiveTextWidth(), x + span.width());
        if (right <= left) {
            return;
        }
        fillLineRect(context, renderLine, left, right - left);
    }

    private void paintUnwrappedRange(
        RenderContext context,
        RenderLine renderLine,
        int startLine,
        int startCol,
        int endLine,
        int endCol
    ) {
        int line = renderLine.lineIndex();
        if (line < startLine || line > endLine) {
            return;
        }
        double baseX = context.textOriginX();
        double left;
        double right;
        if (line == startLine && line == endLine) {
            left = baseX + (startCol * context.charWidth());
            right = baseX + (endCol * context.charWidth());
        } else if (line == startLine) {
            left = baseX + (startCol * context.charWidth());
            right = context.effectiveTextWidth();
        } else if (line == endLine) {
            left = 0.0;
            right = baseX + (endCol * context.charWidth());
        } else {
            left = 0.0;
            right = context.effectiveTextWidth();
        }
        left = Math.max(0.0, left);
        right = Math.min(context.effectiveTextWidth(), right);
        if (right <= left) {
            return;
        }
        fillLineRect(context, renderLine, left, right - left);
    }

    private void fillLineRect(RenderContext context, RenderLine renderLine, double x, double width) {
        double y = Math.max(0.0, Math.round(renderLine.y()));
        double bottom = Math.min(context.viewportHeight(), Math.round(renderLine.y() + context.lineHeight()));
        double height = Math.max(0.0, bottom - y);
        context.graphics().fillRect(x, y, width, height);
    }
}
