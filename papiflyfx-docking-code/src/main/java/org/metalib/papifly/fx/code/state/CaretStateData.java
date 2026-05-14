package org.metalib.papifly.fx.code.state;

/**
 * Serializable caret snapshot with optional selection.
 * <p>
 * All line/column values are zero-based and normalized to non-negative values.
 *
 * @param anchorLine zero-based line of selection anchor
 * @param anchorColumn zero-based column of selection anchor
 * @param caretLine zero-based line of caret position
 * @param caretColumn zero-based column of caret position
 */
public record CaretStateData(
    int anchorLine,
    int anchorColumn,
    int caretLine,
    int caretColumn
) {
    /**
     * Creates a caret snapshot and normalizes all coordinates to non-negative values.
     */
    public CaretStateData {
        anchorLine = Math.max(0, anchorLine);
        anchorColumn = Math.max(0, anchorColumn);
        caretLine = Math.max(0, caretLine);
        caretColumn = Math.max(0, caretColumn);
    }
}
