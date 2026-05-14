package org.metalib.papifly.fx.code.lexer;

/**
 * Line-exit lexer state used for incremental re-lex propagation.
 *
 * @param code state code
 * @param blockScalarHeaderIndent YAML block-scalar header indent, or {@code -1} when unused
 * @param blockScalarContentIndent YAML block-scalar content indent, or {@code -1} when unknown/unused
 * @param blockScalarStyle YAML block-scalar style ({@code |} or {@code >}), or {@code 0} when unused
 * @param blockScalarChomping YAML block-scalar chomping indicator, or {@code 0} when unused
 */
public record LexState(
    int code,
    int blockScalarHeaderIndent,
    int blockScalarContentIndent,
    char blockScalarStyle,
    char blockScalarChomping
) {

    /**
     * Default lexer state.
     */
    public static final LexState DEFAULT = new LexState(0);

    /**
     * Creates a lexer state with no auxiliary metadata.
     *
     * @param code state code
     */
    public LexState(int code) {
        this(code, -1, -1, '\0', '\0');
    }

    /**
     * Creates a lexer state.
     */
    public LexState {
        if (code < 0) {
            throw new IllegalArgumentException("code must be >= 0");
        }
        if (blockScalarHeaderIndent < -1) {
            throw new IllegalArgumentException("blockScalarHeaderIndent must be >= -1");
        }
        if (blockScalarContentIndent < -1) {
            throw new IllegalArgumentException("blockScalarContentIndent must be >= -1");
        }
    }

    /**
     * Returns a state instance for a code.
     *
     * @param code state code
     * @return cached default state for zero, otherwise a new state with the provided code
     */
    public static LexState of(int code) {
        if (code == 0) {
            return DEFAULT;
        }
        return new LexState(code);
    }

    /**
     * Returns a block-scalar state with YAML-specific metadata.
     *
     * @param code state code
     * @param headerIndent header indentation
     * @param contentIndent expected content indentation, or {@code -1} when unknown
     * @param style block-scalar style ({@code |} or {@code >})
     * @param chomping chomping indicator, or {@code 0} when omitted
     * @return state carrying block-scalar metadata
     */
    public static LexState blockScalar(int code, int headerIndent, int contentIndent, char style, char chomping) {
        return new LexState(code, headerIndent, contentIndent, style, chomping);
    }
}
