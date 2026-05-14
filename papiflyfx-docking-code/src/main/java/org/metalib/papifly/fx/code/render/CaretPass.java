package org.metalib.papifly.fx.code.render;

import javafx.scene.canvas.GraphicsContext;
import org.metalib.papifly.fx.code.command.CaretRange;
import org.metalib.papifly.fx.ui.UiMetrics;

/**
 * Paints primary and multi-caret insertion carets.
 */
final class CaretPass implements RenderPass {

    private static final double CARET_WIDTH = UiMetrics.SPACE_1 * 0.5;

    @Override
    public void renderFull(RenderContext context) {
        if (!context.paintCaret()) {
            return;
        }
        GraphicsContext gc = context.graphics();
        gc.setFill(context.theme().caretColor());
        if (context.hasMultiCarets()) {
            for (CaretRange caret : context.activeCarets()) {
                paintCaret(context, gc, caret.caretLine(), caret.caretColumn());
            }
            return;
        }
        paintCaret(
            context,
            gc,
            context.selectionModel().getCaretLine(),
            context.selectionModel().getCaretColumn()
        );
    }

    @Override
    public void renderLine(RenderContext context, RenderLine renderLine) {
        if (!context.paintCaret()) {
            return;
        }
        GraphicsContext gc = context.graphics();
        gc.setFill(context.theme().caretColor());
        if (context.hasMultiCarets()) {
            for (CaretRange caret : context.activeCarets()) {
                paintCaretIfOnRenderLine(context, renderLine, gc, caret.caretLine(), caret.caretColumn());
            }
            return;
        }
        paintCaretIfOnRenderLine(
            context,
            renderLine,
            gc,
            context.selectionModel().getCaretLine(),
            context.selectionModel().getCaretColumn()
        );
    }

    private void paintCaret(RenderContext context, GraphicsContext gc, int line, int column) {
        RenderLine target = resolveRenderLine(context, line, column);
        if (target == null) {
            return;
        }
        double x = context.textOriginX() + ((column - target.startColumn()) * context.charWidth());
        if (x + CARET_WIDTH < 0 || x > context.effectiveTextWidth()) {
            return;
        }
        gc.fillRect(x, target.y(), CARET_WIDTH, context.lineHeight());
    }

    private void paintCaretIfOnRenderLine(
        RenderContext context,
        RenderLine renderLine,
        GraphicsContext gc,
        int line,
        int column
    ) {
        RenderLine resolved = resolveRenderLine(context, line, column);
        if (resolved == null || resolved.lineIndex() != renderLine.lineIndex()
            || resolved.startColumn() != renderLine.startColumn()) {
            return;
        }
        double x = context.textOriginX() + ((column - renderLine.startColumn()) * context.charWidth());
        if (x + CARET_WIDTH < 0 || x > context.effectiveTextWidth()) {
            return;
        }
        gc.fillRect(x, renderLine.y(), CARET_WIDTH, context.lineHeight());
    }

    private RenderLine resolveRenderLine(RenderContext context, int line, int column) {
        if (context.renderLines().isEmpty()) {
            return null;
        }
        if (!context.wordWrap() || context.wrapMap() == null) {
            for (RenderLine renderLine : context.renderLines()) {
                if (renderLine.lineIndex() == line) {
                    return renderLine;
                }
            }
            return null;
        }
        int visualRow = context.wrapMap().lineColumnToVisualRow(line, column);
        WrapMap.VisualRow targetRow = context.wrapMap().visualRow(visualRow);
        for (RenderLine renderLine : context.renderLines()) {
            if (renderLine.lineIndex() == targetRow.lineIndex()
                && renderLine.startColumn() == targetRow.startColumn()
                && renderLine.endColumn() == targetRow.endColumn()) {
                return renderLine;
            }
        }
        return null;
    }
}
