package org.metalib.papifly.fx.code.lexer;

import java.util.List;
import java.util.Objects;

/**
 * Result of lexing one line.
 *
 * @param tokens    line tokens
 * @param exitState line exit state
 */
public record LexResult(List<Token> tokens, LexState exitState) {

    /**
     * Creates a lex result with normalized immutable values.
     */
    public LexResult {
        tokens = tokens == null ? List.of() : List.copyOf(tokens);
        exitState = Objects.requireNonNullElse(exitState, LexState.DEFAULT);
    }
}
