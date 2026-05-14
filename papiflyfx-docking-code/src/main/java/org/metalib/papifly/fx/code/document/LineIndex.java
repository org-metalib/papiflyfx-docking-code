package org.metalib.papifly.fx.code.document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Maps text offsets to line/column and line/column back to offsets.
 */
public class LineIndex {

    private final List<Integer> lineStarts = new ArrayList<>();

    /**
     * Creates an index for empty text.
     */
    public LineIndex() {
        this("");
    }

    /**
     * Creates an index initialized with given text.
     *
     * @param text initial text to index
     */
    public LineIndex(CharSequence text) {
        rebuild(text);
    }

    /**
     * Rebuilds the line index from full text.
     *
     * @param text full text to index
     */
    public void rebuild(CharSequence text) {
        lineStarts.clear();
        lineStarts.add(0);
        if (text == null) {
            return;
        }
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lineStarts.add(i + 1);
            }
        }
    }

    /**
     * Incrementally updates the line index after an insertion.
     *
     * @param offset     the offset where text was inserted
     * @param insertedText the inserted text
     */
    public void applyInsert(int offset, CharSequence insertedText) {
        if (insertedText == null || insertedText.length() == 0) {
            return;
        }
        // Find the line containing the offset
        int lineIndex = lineForOffset(offset);
        int insertionPoint = lineIndex + 1;

        // Collect new line starts within the inserted text
        List<Integer> newStarts = new ArrayList<>();
        for (int i = 0; i < insertedText.length(); i++) {
            if (insertedText.charAt(i) == '\n') {
                newStarts.add(offset + i + 1);
            }
        }

        // Shift all subsequent line starts by the inserted length
        int insertedLength = insertedText.length();
        for (int i = insertionPoint; i < lineStarts.size(); i++) {
            lineStarts.set(i, lineStarts.get(i) + insertedLength);
        }

        // Insert new line starts
        if (!newStarts.isEmpty()) {
            lineStarts.addAll(insertionPoint, newStarts);
        }
    }

    /**
     * Incrementally updates the line index after a deletion.
     *
     * @param startOffset start offset of the deleted range (inclusive)
     * @param endOffset   end offset of the deleted range (exclusive)
     */
    public void applyDelete(int startOffset, int endOffset) {
        if (startOffset >= endOffset) {
            return;
        }
        int deletedLength = endOffset - startOffset;

        // Find lines that start within the deleted range (they are removed)
        int startLine = lineForOffset(startOffset);
        int removeFrom = startLine + 1;
        int removeTo = removeFrom;
        while (removeTo < lineStarts.size() && lineStarts.get(removeTo) <= endOffset) {
            removeTo++;
        }

        // Remove collapsed lines
        if (removeTo > removeFrom) {
            lineStarts.subList(removeFrom, removeTo).clear();
        }

        // Shift all subsequent line starts by the deleted length
        for (int i = removeFrom; i < lineStarts.size(); i++) {
            lineStarts.set(i, lineStarts.get(i) - deletedLength);
        }
    }

    private int lineForOffset(int offset) {
        int position = Collections.binarySearch(lineStarts, offset);
        if (position >= 0) {
            return position;
        }
        return -position - 2;
    }

    /**
     * Returns number of lines in the text.
     *
     * @return number of indexed lines
     */
    public int getLineCount() {
        return lineStarts.size();
    }

    /**
     * Returns unmodifiable line start offsets.
     *
     * @return unmodifiable list of zero-based line start offsets
     */
    public List<Integer> getLineStarts() {
        return Collections.unmodifiableList(lineStarts);
    }

    /**
     * Returns start offset for a line.
     *
     * @param line zero-based line index
     * @return start offset for the given line
     */
    public int getLineStartOffset(int line) {
        requireLine(line);
        return lineStarts.get(line);
    }

    /**
     * Returns end offset (exclusive, without trailing newline) for a line.
     *
     * @param line zero-based line index
     * @param textLength total document text length
     * @return exclusive line end offset without trailing newline
     */
    public int getLineEndOffset(int line, int textLength) {
        requireLine(line);
        requireTextLength(textLength);

        if (line == lineStarts.size() - 1) {
            return textLength;
        }
        return lineStarts.get(line + 1) - 1;
    }

    /**
     * Returns line index for an offset.
     *
     * @param offset document offset
     * @param textLength total document text length
     * @return zero-based line index containing {@code offset}
     */
    public int getLineForOffset(int offset, int textLength) {
        requireOffset(offset, textLength);
        int position = Collections.binarySearch(lineStarts, offset);
        if (position >= 0) {
            return position;
        }
        int insertionPoint = -position - 1;
        return insertionPoint - 1;
    }

    /**
     * Returns column for offset.
     *
     * @param offset document offset
     * @param textLength total document text length
     * @return zero-based column index for {@code offset}
     */
    public int getColumnForOffset(int offset, int textLength) {
        int line = getLineForOffset(offset, textLength);
        return offset - getLineStartOffset(line);
    }

    /**
     * Returns offset for line and column, clamping column to line bounds.
     *
     * @param line zero-based line index
     * @param column zero-based column index
     * @param textLength total document text length
     * @return clamped document offset for the requested line/column
     */
    public int toOffset(int line, int column, int textLength) {
        requireLine(line);
        requireTextLength(textLength);

        int lineStart = getLineStartOffset(line);
        int lineEnd = getLineEndOffset(line, textLength);
        int lineLength = lineEnd - lineStart;
        int safeColumn = Math.max(0, Math.min(column, lineLength));
        return lineStart + safeColumn;
    }

    private void requireLine(int line) {
        if (line < 0 || line >= lineStarts.size()) {
            throw new IndexOutOfBoundsException(
                "Line out of range: " + line + ", lineCount: " + lineStarts.size()
            );
        }
    }

    private static void requireTextLength(int textLength) {
        if (textLength < 0) {
            throw new IllegalArgumentException("Text length cannot be negative: " + textLength);
        }
    }

    private static void requireOffset(int offset, int textLength) {
        if (offset < 0 || offset > textLength) {
            throw new IndexOutOfBoundsException(
                "Offset out of range: " + offset + ", textLength: " + textLength
            );
        }
    }
}
