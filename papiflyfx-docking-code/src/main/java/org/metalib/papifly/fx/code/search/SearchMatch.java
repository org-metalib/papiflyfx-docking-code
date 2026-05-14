package org.metalib.papifly.fx.code.search;

/**
 * A single search match within the document.
 *
 * @param startOffset document offset of match start
 * @param endOffset   document offset of match end (exclusive)
 * @param line        zero-based line containing the match start
 * @param startColumn zero-based column of match start within the line
 * @param endColumn   zero-based column of match end within the line (may wrap)
 */
public record SearchMatch(int startOffset, int endOffset, int line, int startColumn, int endColumn) {

    /**
     * Returns the length of the match.
     *
     * @return match length in characters
     */
    public int length() {
        return endOffset - startOffset;
    }
}
