package org.metalib.papifly.fx.code.render;

/**
 * Selection geometry helpers used by selection render passes.
 */
final class SelectionGeometry {

    private SelectionGeometry() {
    }

    static SelectionSpan spanForVisualRow(
        RenderLine renderLine,
        double charWidth,
        int startLine,
        int startCol,
        int endLine,
        int endCol
    ) {
        int line = renderLine.lineIndex();
        if (line < startLine || line > endLine || renderLine.endColumn() < renderLine.startColumn()) {
            return null;
        }
        int rowStart = renderLine.startColumn();
        int rowEnd = renderLine.endColumn();
        int segmentStart;
        int segmentEnd;
        if (line == startLine && line == endLine) {
            segmentStart = Math.max(rowStart, startCol);
            segmentEnd = Math.min(rowEnd, endCol);
        } else if (line == startLine) {
            segmentStart = Math.max(rowStart, startCol);
            segmentEnd = rowEnd;
        } else if (line == endLine) {
            segmentStart = rowStart;
            segmentEnd = Math.min(rowEnd, endCol);
        } else {
            segmentStart = rowStart;
            segmentEnd = rowEnd;
        }
        if (segmentEnd <= segmentStart) {
            return null;
        }
        double x = (segmentStart - rowStart) * charWidth;
        double width = (segmentEnd - segmentStart) * charWidth;
        return new SelectionSpan(x, width);
    }

    record SelectionSpan(double x, double width) {
    }
}
