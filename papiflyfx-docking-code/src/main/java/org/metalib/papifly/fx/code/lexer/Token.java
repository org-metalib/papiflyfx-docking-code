package org.metalib.papifly.fx.code.lexer;

import java.util.Objects;

/**
 * A token slice in a single line.
 *
 * @param startColumn token start column (inclusive)
 * @param length      token length in characters
 * @param type        token category
 * @param styleScope  optional semantic syntax style scope
 */
public record Token(int startColumn, int length, TokenType type, String styleScope) {

    /**
     * Creates a token without a semantic style scope.
     *
     * @param startColumn token start column (inclusive)
     * @param length      token length in characters
     * @param type        token category
     */
    public Token(int startColumn, int length, TokenType type) {
        this(startColumn, length, type, null);
    }

    /**
     * Creates a token with validated bounds and type.
     */
    public Token {
        if (startColumn < 0) {
            throw new IllegalArgumentException("startColumn must be >= 0");
        }
        if (length <= 0) {
            throw new IllegalArgumentException("length must be > 0");
        }
        type = Objects.requireNonNull(type, "type");
        styleScope = styleScope == null || styleScope.isBlank() ? null : styleScope.trim();
    }

    /**
     * Returns the end column (exclusive).
     *
     * @return end column (exclusive)
     */
    public int endColumn() {
        return startColumn + length;
    }
}
