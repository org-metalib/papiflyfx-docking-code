package org.metalib.papifly.fx.code.lexer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaScriptLexerTest {

    @Test
    void javascriptLineIncludesKeywordStringAndNumberTokens() {
        JavaScriptLexer lexer = new JavaScriptLexer();
        String line = "function demo() { return `v-${x}` + 42; }";

        LexResult result = lexer.lexLine(line, LexState.DEFAULT);
        List<TokenType> types = result.tokens().stream().map(Token::type).toList();

        assertTrue(types.contains(TokenType.KEYWORD));
        assertTrue(types.contains(TokenType.STRING));
        assertTrue(types.contains(TokenType.NUMBER));
        assertEquals(LexState.DEFAULT, result.exitState());
    }

    @Test
    void javascriptBlockCommentStatePropagatesAcrossLines() {
        JavaScriptLexer lexer = new JavaScriptLexer();

        LexResult first = lexer.lexLine("/* open", LexState.DEFAULT);
        assertEquals(LexState.of(AbstractCStyleLexer.STATE_BLOCK_COMMENT), first.exitState());

        LexResult second = lexer.lexLine("close */ const value = 1;", first.exitState());
        assertEquals(LexState.DEFAULT, second.exitState());
        assertTrue(second.tokens().stream().anyMatch(token -> token.type() == TokenType.COMMENT));
        assertTrue(second.tokens().stream().anyMatch(token -> token.type() == TokenType.KEYWORD));
    }
}
