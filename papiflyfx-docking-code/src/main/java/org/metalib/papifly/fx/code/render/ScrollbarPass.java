package org.metalib.papifly.fx.code.render;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;

/**
 * Paints canvas-rendered viewport scrollbars.
 */
final class ScrollbarPass implements RenderPass {

    @Override
    public void renderFull(RenderContext context) {
        GraphicsContext gc = context.graphics();
        if (context.verticalScrollbarVisible() && context.verticalScrollbarGeometry() != null) {
            paintScrollbar(gc, context, context.verticalScrollbarGeometry(), Viewport.ScrollbarPart.VERTICAL_THUMB);
        }
        if (context.horizontalScrollbarVisible() && context.horizontalScrollbarGeometry() != null) {
            paintScrollbar(gc, context, context.horizontalScrollbarGeometry(), Viewport.ScrollbarPart.HORIZONTAL_THUMB);
        }
    }

    private void paintScrollbar(
        GraphicsContext gc,
        RenderContext context,
        Viewport.ScrollbarGeometry geometry,
        Viewport.ScrollbarPart part
    ) {
        gc.setFill(context.theme().scrollbarTrackColor());
        gc.fillRoundRect(
            geometry.trackX(),
            geometry.trackY(),
            geometry.trackWidth(),
            geometry.trackHeight(),
            Viewport.SCROLLBAR_RADIUS,
            Viewport.SCROLLBAR_RADIUS
        );

        Paint thumbFill = context.theme().scrollbarThumbColor();
        if (context.scrollbarActivePart() == part) {
            thumbFill = context.theme().scrollbarThumbActiveColor();
        } else if (context.scrollbarHoverPart() == part) {
            thumbFill = context.theme().scrollbarThumbHoverColor();
        }
        gc.setFill(thumbFill);
        gc.fillRoundRect(
            geometry.thumbX(),
            geometry.thumbY(),
            geometry.thumbWidth(),
            geometry.thumbHeight(),
            Viewport.SCROLLBAR_RADIUS,
            Viewport.SCROLLBAR_RADIUS
        );
    }
}
