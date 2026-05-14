package org.metalib.papifly.fx.code.render;

import org.metalib.papifly.fx.code.document.Document;

import java.util.function.IntPredicate;

/**
 * Soft-wrap index mapping logical lines to visual rows.
 * <p>
 * This map never mutates the document. It stores row counts per logical line
 * and prefix sums to support fast row/line translation in wrap mode.
 */
public final class WrapMap {

    private int[] prefixRows = new int[]{0};
    private int[] visualRowsPerLine = new int[0];
    private int[] lineLengths = new int[0];
    private int wrapColumns = Integer.MAX_VALUE;

    /**
     * Creates an empty wrap map.
     */
    public WrapMap() {
        // Default constructor.
    }

    /**
     * Rebuilds the entire wrap map from document + viewport metrics.
     *
     * @param document source document
     * @param viewportWidth viewport text width in pixels
     * @param charWidth average character width in pixels
     */
    public void rebuild(Document document, double viewportWidth, double charWidth) {
        rebuild(document, viewportWidth, charWidth, ignored -> true);
    }

    public void rebuild(Document document, double viewportWidth, double charWidth, IntPredicate lineVisiblePredicate) {
        if (document == null) {
            prefixRows = new int[]{0};
            visualRowsPerLine = new int[0];
            lineLengths = new int[0];
            wrapColumns = Integer.MAX_VALUE;
            return;
        }
        int lineCount = Math.max(0, document.getLineCount());
        wrapColumns = computeWrapColumns(viewportWidth, charWidth);
        visualRowsPerLine = new int[lineCount];
        lineLengths = new int[lineCount];
        prefixRows = new int[lineCount + 1];
        prefixRows[0] = 0;
        for (int line = 0; line < lineCount; line++) {
            int length = document.getLineText(line).length();
            lineLengths[line] = length;
            boolean lineVisible = lineVisiblePredicate == null || lineVisiblePredicate.test(line);
            int rows = lineVisible ? rowsForLength(length, wrapColumns) : 0;
            visualRowsPerLine[line] = rows;
            prefixRows[line + 1] = prefixRows[line] + rows;
        }
    }

    /**
     * Updates wrap metadata for the specified logical range.
     * <p>
     * Current implementation performs a full rebuild to keep behavior
     * deterministic; incremental line-range recomputation can be added later.
     *
     * @param document source document
     * @param startLine inclusive start line index
     * @param endLine inclusive end line index
     * @param viewportWidth viewport text width in pixels
     * @param charWidth average character width in pixels
     */
    public void update(Document document, int startLine, int endLine, double viewportWidth, double charWidth) {
        rebuild(document, viewportWidth, charWidth, ignored -> true);
    }

    /**
     * Returns total visual row count across the document.
     *
     * @return total number of visual rows
     */
    public int totalVisualRows() {
        return prefixRows.length == 0 ? 0 : prefixRows[prefixRows.length - 1];
    }

    /**
     * Returns wrap columns used for this map rebuild.
     *
     * @return wrap column count
     */
    public int wrapColumns() {
        return wrapColumns;
    }

    /**
     * Returns the first visual row index for a logical line.
     *
     * @param lineIndex zero-based logical line index
     * @return first visual row index for the line
     */
    public int lineToFirstVisualRow(int lineIndex) {
        if (prefixRows.length <= 1) {
            return 0;
        }
        int safeLine = clamp(lineIndex, 0, visualRowsPerLine.length - 1);
        return prefixRows[safeLine];
    }

    /**
     * Returns visual row count for a logical line.
     *
     * @param lineIndex zero-based logical line index
     * @return number of visual rows occupied by the line
     */
    public int lineVisualRowCount(int lineIndex) {
        if (visualRowsPerLine.length == 0) {
            return 0;
        }
        int safeLine = clamp(lineIndex, 0, visualRowsPerLine.length - 1);
        return visualRowsPerLine[safeLine];
    }

    /**
     * Returns logical line for a visual row index.
     *
     * @param visualRowIndex zero-based visual row index
     * @return corresponding logical line index
     */
    public int visualRowToLine(int visualRowIndex) {
        if (visualRowsPerLine.length == 0 || totalVisualRows() <= 0) {
            return 0;
        }
        int safeRow = clamp(visualRowIndex, 0, totalVisualRows() - 1);
        int low = 0;
        int high = visualRowsPerLine.length - 1;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (prefixRows[mid + 1] <= safeRow) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return clamp(low, 0, visualRowsPerLine.length - 1);
    }

    /**
     * Returns visual-row metadata for the given visual row index.
     *
     * @param visualRowIndex zero-based visual row index
     * @return visual-row metadata
     */
    public VisualRow visualRow(int visualRowIndex) {
        if (visualRowsPerLine.length == 0 || totalVisualRows() <= 0) {
            return new VisualRow(0, 0, 0);
        }
        int safeRow = clamp(visualRowIndex, 0, totalVisualRows() - 1);
        int line = visualRowToLine(safeRow);
        int firstRow = prefixRows[line];
        int rowInLine = safeRow - firstRow;
        int startColumn = wrapColumns == Integer.MAX_VALUE ? 0 : rowInLine * wrapColumns;
        int endColumn = Math.min(lineLengths[line], startColumn + wrapColumns);
        return new VisualRow(line, startColumn, endColumn);
    }

    /**
     * Returns visual row index containing the given logical line/column.
     *
     * @param lineIndex zero-based logical line index
     * @param column zero-based column within the logical line
     * @return visual row index containing the position
     */
    public int lineColumnToVisualRow(int lineIndex, int column) {
        if (visualRowsPerLine.length == 0) {
            return 0;
        }
        int safeLine = clamp(lineIndex, 0, visualRowsPerLine.length - 1);
        if (visualRowsPerLine[safeLine] <= 0) {
            int totalRows = totalVisualRows();
            if (totalRows <= 0) {
                return 0;
            }
            return clamp(prefixRows[safeLine], 0, totalRows - 1);
        }
        int lineLength = lineLengths[safeLine];
        int safeColumn = clamp(column, 0, lineLength);
        if (wrapColumns == Integer.MAX_VALUE || wrapColumns <= 0) {
            return prefixRows[safeLine];
        }
        int rowInLine = Math.min(visualRowsPerLine[safeLine] - 1, safeColumn / wrapColumns);
        return prefixRows[safeLine] + rowInLine;
    }

    /**
     * Returns true when map contains data for at least one line.
     *
     * @return {@code true} when wrap map has at least one line entry
     */
    public boolean hasData() {
        return visualRowsPerLine.length > 0;
    }

    private static int computeWrapColumns(double viewportWidth, double charWidth) {
        if (viewportWidth <= 0 || charWidth <= 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, (int) Math.floor(viewportWidth / charWidth));
    }

    private static int rowsForLength(int length, int wrapColumns) {
        int safeLength = Math.max(0, length);
        if (safeLength == 0 || wrapColumns == Integer.MAX_VALUE) {
            return 1;
        }
        return Math.max(1, (safeLength + wrapColumns - 1) / wrapColumns);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    /**
     * Visual row descriptor for wrapped rendering.
     *
     * @param lineIndex   logical line index
     * @param startColumn row start column (inclusive) in logical line
     * @param endColumn   row end column (exclusive) in logical line
     */
    public record VisualRow(int lineIndex, int startColumn, int endColumn) {
    }
}
