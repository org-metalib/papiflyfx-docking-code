package org.metalib.papifly.fx.code.render;

import javafx.scene.canvas.GraphicsContext;

/**
 * Paints editor background and current-line highlight.
 */
final class BackgroundPass implements RenderPass {

    @Override
    public void renderFull(RenderContext context) {
        GraphicsContext gc = context.graphics();
        gc.setFill(context.theme().editorBackground());
        gc.fillRect(0, 0, context.viewportWidth(), context.viewportHeight());
        paintCurrentLineHighlight(context);
    }

    @Override
    public void renderLine(RenderContext context, RenderLine renderLine) {
        GraphicsContext gc = context.graphics();
        gc.setFill(context.theme().editorBackground());
        double y = Math.max(0.0, renderLine.y() - 1.0);
        double height = Math.min(context.viewportHeight() - y, context.lineHeight() + 2.0);
        gc.fillRect(0, y, context.effectiveTextWidth(), height);
        paintCurrentLineHighlight(context, renderLine);
    }

    private void paintCurrentLineHighlight(RenderContext context) {
        if (context.selectionModel().hasSelection()) {
            return;
        }
        int caretLine = context.selectionModel().getCaretLine();
        GraphicsContext gc = context.graphics();
        gc.setFill(context.theme().currentLineColor());
        for (RenderLine renderLine : context.renderLines()) {
            if (renderLine.lineIndex() == caretLine) {
                gc.fillRect(0, renderLine.y(), context.effectiveTextWidth(), context.lineHeight());
            }
        }
    }

    private void paintCurrentLineHighlight(RenderContext context, RenderLine renderLine) {
        if (context.selectionModel().hasSelection()) {
            return;
        }
        if (renderLine.lineIndex() != context.selectionModel().getCaretLine()) {
            return;
        }
        GraphicsContext gc = context.graphics();
        gc.setFill(context.theme().currentLineColor());
        gc.fillRect(0, renderLine.y(), context.effectiveTextWidth(), context.lineHeight());
    }
}
