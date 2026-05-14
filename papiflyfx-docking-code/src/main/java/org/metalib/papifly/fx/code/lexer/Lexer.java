package org.metalib.papifly.fx.code.lexer;

/**
 * Stateful single-line lexer contract.
 */
public interface Lexer {

    /**
     * Returns stable language identifier.
     *
     * @return canonical language identifier
     */
    String languageId();

    /**
     * Returns the initial lexer state for line 0.
     *
     * @return initial lex state
     */
    default LexState initialState() {
        return LexState.DEFAULT;
    }

    /**
     * Lexes one line using entry state and returns tokens with exit state.
     *
     * @param lineText line text to lex
     * @param entryState entry lex state for the line
     * @return lexing result with tokens and resulting exit state
     */
    LexResult lexLine(String lineText, LexState entryState);
}
