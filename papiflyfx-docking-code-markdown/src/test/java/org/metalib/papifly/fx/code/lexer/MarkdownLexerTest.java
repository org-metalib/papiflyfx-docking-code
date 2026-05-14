package org.metalib.papifly.fx.code.lexer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownLexerTest {

    @Test
    void headlineDetection() {
        MarkdownLexer lexer = new MarkdownLexer();
        String line = "### Headline 3";

        LexResult result = lexer.lexLine(line, LexState.DEFAULT);
        List<TokenType> types = result.tokens().stream().map(Token::type).toList();

        assertEquals(1, types.size());
        assertEquals(TokenType.TEXT, types.get(0));
        assertEquals(MarkdownLexer.SCOPE_MARKDOWN_HEADLINE, result.tokens().getFirst().styleScope());
    }

    @Test
    void listItemDetection() {
        MarkdownLexer lexer = new MarkdownLexer();
        String line = "  - List Item";

        LexResult result = lexer.lexLine(line, LexState.DEFAULT);
        List<TokenType> types = result.tokens().stream().map(Token::type).toList();

        assertTrue(types.contains(TokenType.TEXT));
        assertTrue(result.tokens().stream()
            .anyMatch(token -> MarkdownLexer.SCOPE_MARKDOWN_LIST_ITEM.equals(token.styleScope())));
    }

    @Test
    void codeBlockStatePropagates() {
        MarkdownLexer lexer = new MarkdownLexer();

        LexResult first = lexer.lexLine("```java", LexState.DEFAULT);
        assertEquals(LexState.of(1), first.exitState());
        assertEquals(TokenType.TEXT, first.tokens().get(0).type());
        assertEquals(MarkdownLexer.SCOPE_MARKDOWN_CODE_BLOCK, first.tokens().get(0).styleScope());

        LexResult second = lexer.lexLine("int x = 1;", first.exitState());
        assertEquals(LexState.of(1), second.exitState());
        assertEquals(TokenType.TEXT, second.tokens().get(0).type());
        assertEquals(MarkdownLexer.SCOPE_MARKDOWN_CODE_BLOCK, second.tokens().get(0).styleScope());

        LexResult third = lexer.lexLine("```", second.exitState());
        assertEquals(LexState.DEFAULT, third.exitState());
        assertEquals(TokenType.TEXT, third.tokens().get(0).type());
        assertEquals(MarkdownLexer.SCOPE_MARKDOWN_CODE_BLOCK, third.tokens().get(0).styleScope());
    }

    @Test
    void orderedListTwoDigitNumber() {
        MarkdownLexer lexer = new MarkdownLexer();
        LexResult result = lexer.lexLine("10. item ten", LexState.DEFAULT);
        List<TokenType> types = result.tokens().stream().map(Token::type).toList();

        assertTrue(result.tokens().stream()
            .anyMatch(token -> MarkdownLexer.SCOPE_MARKDOWN_LIST_ITEM.equals(token.styleScope())));
        // Marker should be "10. " (4 chars)
        Token marker = result.tokens().get(0);
        assertEquals(0, marker.startColumn());
        assertEquals(4, marker.length());
    }

    @Test
    void orderedListThreeDigitNumber() {
        MarkdownLexer lexer = new MarkdownLexer();
        LexResult result = lexer.lexLine("123. item", LexState.DEFAULT);
        List<TokenType> types = result.tokens().stream().map(Token::type).toList();

        assertTrue(result.tokens().stream()
            .anyMatch(token -> MarkdownLexer.SCOPE_MARKDOWN_LIST_ITEM.equals(token.styleScope())));
        Token marker = result.tokens().get(0);
        assertEquals(5, marker.length()); // "123. "
    }

    @Test
    void orderedListSingleDigit() {
        MarkdownLexer lexer = new MarkdownLexer();
        LexResult result = lexer.lexLine("1. first item", LexState.DEFAULT);
        List<TokenType> types = result.tokens().stream().map(Token::type).toList();

        assertTrue(result.tokens().stream()
            .anyMatch(token -> MarkdownLexer.SCOPE_MARKDOWN_LIST_ITEM.equals(token.styleScope())));
        Token marker = result.tokens().get(0);
        assertEquals(3, marker.length()); // "1. "
    }

    @Test
    void numberWithoutSpaceAfterDotIsNotList() {
        MarkdownLexer lexer = new MarkdownLexer();
        LexResult result = lexer.lexLine("1.item", LexState.DEFAULT);
        List<TokenType> types = result.tokens().stream().map(Token::type).toList();

        assertFalse(result.tokens().stream()
            .anyMatch(token -> MarkdownLexer.SCOPE_MARKDOWN_LIST_ITEM.equals(token.styleScope())));
    }

    @Test
    void punctuationAndText() {
        MarkdownLexer lexer = new MarkdownLexer();
        String line = "Hello *world* [link]";

        LexResult result = lexer.lexLine(line, LexState.DEFAULT);
        List<TokenType> types = result.tokens().stream().map(Token::type).toList();

        assertTrue(types.contains(TokenType.TEXT));
        assertTrue(types.contains(TokenType.PUNCTUATION));
    }
}
