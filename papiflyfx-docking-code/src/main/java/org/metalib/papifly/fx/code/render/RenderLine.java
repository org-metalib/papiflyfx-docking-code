package org.metalib.papifly.fx.code.render;

import org.metalib.papifly.fx.code.lexer.Token;

import java.util.List;

/**
 * Per-line render data used by the viewport.
 *
 * @param lineIndex   zero-based logical line number in the document
 * @param startColumn start column (inclusive) represented by this visual row
 * @param endColumn   end column (exclusive) represented by this visual row
 * @param text        the visual row text content (without trailing newline)
 * @param y           y-coordinate in the canvas for this visual row
 * @param tokens      syntax tokens for the logical line (clipped by row in passes)
 */
public record RenderLine(int lineIndex, int startColumn, int endColumn, String text, double y, List<Token> tokens) {

    /**
     * Creates a render-line snapshot with normalized bounds and text.
     */
    public RenderLine {
        startColumn = Math.max(0, startColumn);
        endColumn = Math.max(startColumn, endColumn);
        text = text == null ? "" : text;
        tokens = tokens == null ? List.of() : tokens;
    }
}
