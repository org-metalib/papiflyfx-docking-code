package org.metalib.papifly.fx.code.lexer;

import java.util.List;

/**
 * Fallback lexer that returns plain text without syntax tokens.
 */
public final class PlainTextLexer implements Lexer {

    /**
     * Stable id for plain-text language.
     */
    public static final String LANGUAGE_ID = "plain-text";

    /**
     * Creates a plain-text lexer instance.
     */
    public PlainTextLexer() {
    }

    @Override
    public String languageId() {
        return LANGUAGE_ID;
    }

    @Override
    public LexResult lexLine(String lineText, LexState entryState) {
        return new LexResult(List.of(), LexState.DEFAULT);
    }
}
