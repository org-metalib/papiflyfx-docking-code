package org.metalib.papifly.fx.code.lexer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonLexerTest {

    @Test
    void jsonLineIncludesStringBooleanNullNumberAndPunctuationTokens() {
        JsonLexer lexer = new JsonLexer();
        String line = "{\"ok\": true, \"value\": 12.5, \"none\": null, \"name\": \"Ada\"}";

        LexResult result = lexer.lexLine(line, LexState.DEFAULT);
        List<TokenType> types = result.tokens().stream().map(Token::type).toList();

        assertTrue(types.contains(TokenType.STRING));
        assertTrue(result.tokens().stream().anyMatch(token -> JsonLexer.SCOPE_JSON_KEY.equals(token.styleScope())));
        assertTrue(types.contains(TokenType.BOOLEAN));
        assertTrue(types.contains(TokenType.NULL_LITERAL));
        assertTrue(types.contains(TokenType.NUMBER));
        assertTrue(types.contains(TokenType.PUNCTUATION));
        assertEquals(LexState.DEFAULT, result.exitState());
    }

    @Test
    void jsonStringStatePropagatesAcrossLines() {
        JsonLexer lexer = new JsonLexer();

        LexResult first = lexer.lexLine("\"open string", LexState.DEFAULT);
        assertEquals(LexState.of(1), first.exitState());

        LexResult second = lexer.lexLine("close\"", first.exitState());
        assertEquals(LexState.DEFAULT, second.exitState());
        assertTrue(second.tokens().stream().anyMatch(token -> token.type() == TokenType.STRING));
    }

    @Test
    void jsonObjectKeysUseSemanticStyleScope() {
        JsonLexer lexer = new JsonLexer();
        String line = "{\"name\": \"Ada\", \"url\" : \"https://example.test:443\"}";

        LexResult result = lexer.lexLine(line, LexState.DEFAULT);
        List<TokenType> types = result.tokens().stream().map(Token::type).toList();

        assertEquals(2, result.tokens().stream()
            .filter(token -> JsonLexer.SCOPE_JSON_KEY.equals(token.styleScope()))
            .count());
        assertEquals(4, types.stream().filter(type -> type == TokenType.STRING).count());
        assertEquals(LexState.DEFAULT, result.exitState());
    }

    @Test
    void jsonEscapedQuoteInsideKeyDoesNotEndKeyTokenEarly() {
        JsonLexer lexer = new JsonLexer();
        String line = "{\"a\\\"b\": \"value\"}";

        LexResult result = lexer.lexLine(line, LexState.DEFAULT);
        Token key = result.tokens().stream()
            .filter(token -> JsonLexer.SCOPE_JSON_KEY.equals(token.styleScope()))
            .findFirst()
            .orElseThrow();

        assertEquals(1, result.tokens().stream()
            .filter(token -> JsonLexer.SCOPE_JSON_KEY.equals(token.styleScope()))
            .count());
        assertEquals("\"a\\\"b\"", line.substring(key.startColumn(), key.endColumn()));
        assertEquals(LexState.DEFAULT, result.exitState());
    }
}
