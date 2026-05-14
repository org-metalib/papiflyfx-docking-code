package org.metalib.papifly.fx.code.lexer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaLexerTest {

    @Test
    void javaLineIncludesKeywordStringCommentAndNumberTokens() {
        JavaLexer lexer = new JavaLexer();
        String line = "public class Demo { int n = 42; String s = \"ok\"; // note";

        LexResult result = lexer.lexLine(line, LexState.DEFAULT);
        List<TokenType> types = result.tokens().stream().map(Token::type).toList();

        assertTrue(types.contains(TokenType.KEYWORD));
        assertTrue(types.contains(TokenType.STRING));
        assertTrue(types.contains(TokenType.COMMENT));
        assertTrue(types.contains(TokenType.NUMBER));
        assertEquals(LexState.DEFAULT, result.exitState());
    }

    @Test
    void javaBlockCommentStatePropagatesAcrossLines() {
        JavaLexer lexer = new JavaLexer();

        LexResult first = lexer.lexLine("/* open block", LexState.DEFAULT);
        assertEquals(LexState.of(AbstractCStyleLexer.STATE_BLOCK_COMMENT), first.exitState());

        LexResult second = lexer.lexLine("close */ int x = 1;", first.exitState());
        assertEquals(LexState.DEFAULT, second.exitState());
        assertTrue(second.tokens().stream().anyMatch(token -> token.type() == TokenType.COMMENT));
        assertTrue(second.tokens().stream().anyMatch(token -> token.type() == TokenType.KEYWORD));
    }
}
