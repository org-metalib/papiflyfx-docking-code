package org.metalib.papifly.fx.code.render;

/**
 * A composable drawing stage in the viewport render pipeline.
 */
interface RenderPass {

    default void renderFull(RenderContext context) {
    }

    default void renderLine(RenderContext context, RenderLine renderLine) {
    }
}
