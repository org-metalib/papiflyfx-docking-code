package org.metalib.papifly.fx.code.command;

/**
 * Utility for locating word boundaries in a line of text.
 * <p>
 * Word characters are {@code [A-Za-z0-9_]}. Everything else
 * (whitespace, punctuation) is a non-word character. The algorithm
 * groups consecutive characters of the same class and moves across
 * one full group, matching VS Code / IntelliJ behaviour.
 */
public final class WordBoundary {

    private WordBoundary() {}

    /**
     * Returns the column position of the word boundary to the left of {@code column}.
     * <p>
     * Scans leftward: first skips any non-word characters, then skips the
     * preceding word characters (or vice versa if starting inside a non-word run).
     *
     * @param lineText the full text of the line (no trailing newline)
     * @param column   current 0-based column (clamped to line length)
     * @return target column (always {@code >= 0})
     */
    public static int findWordLeft(String lineText, int column) {
        if (lineText == null || lineText.isEmpty()) {
            return 0;
        }
        int pos = Math.min(column, lineText.length());
        if (pos <= 0) {
            return 0;
        }

        // Skip whitespace to the left first
        while (pos > 0 && Character.isWhitespace(lineText.charAt(pos - 1))) {
            pos--;
        }
        if (pos == 0) {
            return 0;
        }

        // Now skip a group of same-class chars
        boolean startIsWord = isWordChar(lineText.charAt(pos - 1));
        while (pos > 0 && isWordChar(lineText.charAt(pos - 1)) == startIsWord
                && !Character.isWhitespace(lineText.charAt(pos - 1))) {
            pos--;
        }
        return pos;
    }

    /**
     * Returns the column position of the word boundary to the right of {@code column}.
     * <p>
     * Scans rightward: first skips any characters of the current class,
     * then skips trailing whitespace.
     *
     * @param lineText the full text of the line (no trailing newline)
     * @param column   current 0-based column (clamped to line length)
     * @return target column (always {@code <= lineText.length()})
     */
    public static int findWordRight(String lineText, int column) {
        if (lineText == null || lineText.isEmpty()) {
            return 0;
        }
        int len = lineText.length();
        int pos = Math.min(column, len);
        if (pos >= len) {
            return len;
        }

        // Skip current class of characters
        boolean startIsWord = isWordChar(lineText.charAt(pos));
        boolean startIsWhitespace = Character.isWhitespace(lineText.charAt(pos));
        if (startIsWhitespace) {
            // Skip whitespace, then stop at the next non-whitespace
            while (pos < len && Character.isWhitespace(lineText.charAt(pos))) {
                pos++;
            }
        } else {
            // Skip same-class (word or punctuation), then skip whitespace
            while (pos < len && isWordChar(lineText.charAt(pos)) == startIsWord
                    && !Character.isWhitespace(lineText.charAt(pos))) {
                pos++;
            }
            // Skip trailing whitespace
            while (pos < len && Character.isWhitespace(lineText.charAt(pos))) {
                pos++;
            }
        }
        return pos;
    }

    /**
     * Returns whether a character is treated as part of a word token.
     *
     * @param ch character to classify
     * @return {@code true} when character is alphanumeric or underscore
     */
    public static boolean isWordChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_';
    }
}
