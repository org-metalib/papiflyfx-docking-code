package org.metalib.papifly.fx.code.lexer;

import java.util.List;
import java.util.Objects;

/**
 * Per-line token cache entry.
 *
 * @param text       line text snapshot
 * @param tokens     line tokens
 * @param entryState lexer entry state used for this line
 * @param exitState  lexer exit state produced by this line
 */
public record LineTokens(
    String text,
    List<Token> tokens,
    LexState entryState,
    LexState exitState
) {

    /**
     * Creates a line token snapshot with normalized immutable values.
     */
    public LineTokens {
        text = text == null ? "" : text;
        tokens = tokens == null ? List.of() : List.copyOf(tokens);
        entryState = Objects.requireNonNullElse(entryState, LexState.DEFAULT);
        exitState = Objects.requireNonNullElse(exitState, LexState.DEFAULT);
    }
}
