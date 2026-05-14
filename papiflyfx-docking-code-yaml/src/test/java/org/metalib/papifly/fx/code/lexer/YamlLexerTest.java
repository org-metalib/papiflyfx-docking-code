package org.metalib.papifly.fx.code.lexer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlLexerTest {

    private final YamlLexer lexer = new YamlLexer();

    @Test
    void blockMappingLineUsesYamlKeyPunctuationAndPlainValue() {
        LexResult result = lexer.lexLine("name: Ada", LexState.DEFAULT);

        assertTypes(result, TokenType.IDENTIFIER, TokenType.PUNCTUATION, TokenType.PLAIN);
        assertScopeCount(result, YamlLexer.SCOPE_YAML_KEY, 1);
        assertEquals(LexState.DEFAULT, result.exitState());
    }

    @Test
    void quotedMappingValueUsesStringToken() {
        LexResult result = lexer.lexLine("name: \"Ada\"", LexState.DEFAULT);

        assertTypes(result, TokenType.IDENTIFIER, TokenType.PUNCTUATION, TokenType.STRING);
        assertEquals(LexState.DEFAULT, result.exitState());
    }

    @Test
    void inlineCommentEndsLineAsComment() {
        LexResult result = lexer.lexLine("key: value # note", LexState.DEFAULT);

        assertTypes(result, TokenType.IDENTIFIER, TokenType.PUNCTUATION, TokenType.PLAIN, TokenType.COMMENT);
    }

    @Test
    void booleanVariantsOnlyHighlightInValuePosition() {
        assertTypes(lexer.lexLine("enabled: true", LexState.DEFAULT),
            TokenType.IDENTIFIER, TokenType.PUNCTUATION, TokenType.BOOLEAN);
        assertTypes(lexer.lexLine("answer: yes", LexState.DEFAULT),
            TokenType.IDENTIFIER, TokenType.PUNCTUATION, TokenType.BOOLEAN);
        assertTypes(lexer.lexLine("on: value", LexState.DEFAULT),
            TokenType.IDENTIFIER, TokenType.PUNCTUATION, TokenType.PLAIN);
    }

    @Test
    void nullVariantsOnlyHighlightInValuePosition() {
        assertTypes(lexer.lexLine("missing: null", LexState.DEFAULT),
            TokenType.IDENTIFIER, TokenType.PUNCTUATION, TokenType.NULL_LITERAL);
        assertTypes(lexer.lexLine("empty: ~", LexState.DEFAULT),
            TokenType.IDENTIFIER, TokenType.PUNCTUATION, TokenType.NULL_LITERAL);
        assertTypes(lexer.lexLine("null: value", LexState.DEFAULT),
            TokenType.IDENTIFIER, TokenType.PUNCTUATION, TokenType.PLAIN);
    }

    @Test
    void numberFormsUseNumberTokens() {
        List<String> values = List.of("42", "-3.14", "1e5", "0x1F", "0o77", ".inf", "-.inf", "+.inf", ".nan");

        for (String value : values) {
            LexResult result = lexer.lexLine("value: " + value, LexState.DEFAULT);
            assertTrue(result.tokens().stream().anyMatch(token -> token.type() == TokenType.NUMBER), value);
        }
    }

    @Test
    void documentMarkersArePunctuation() {
        assertTypes(lexer.lexLine("---", LexState.DEFAULT), TokenType.PUNCTUATION);
        assertTypes(lexer.lexLine("...", LexState.DEFAULT), TokenType.PUNCTUATION);
    }

    @Test
    void sequenceIndicatorIsPunctuation() {
        assertTypes(lexer.lexLine("- item", LexState.DEFAULT), TokenType.PUNCTUATION, TokenType.PLAIN);
    }

    @Test
    void anchorsAliasesTagsAndMergeKeyUseDedicatedYamlTokens() {
        assertTypes(lexer.lexLine("base: &id001 value", LexState.DEFAULT),
            TokenType.IDENTIFIER, TokenType.PUNCTUATION, TokenType.IDENTIFIER, TokenType.PLAIN);
        assertTypes(lexer.lexLine("copy: *id001", LexState.DEFAULT),
            TokenType.IDENTIFIER, TokenType.PUNCTUATION, TokenType.IDENTIFIER);
        assertTypes(lexer.lexLine("type: !!str foo", LexState.DEFAULT),
            TokenType.IDENTIFIER, TokenType.PUNCTUATION, TokenType.IDENTIFIER, TokenType.PLAIN);
        assertTypes(lexer.lexLine("<<: *base", LexState.DEFAULT),
            TokenType.OPERATOR, TokenType.PUNCTUATION, TokenType.IDENTIFIER);
        assertScopeCount(lexer.lexLine("base: &id001", LexState.DEFAULT), YamlLexer.SCOPE_YAML_ANCHOR, 1);
        assertScopeCount(lexer.lexLine("copy: *id001", LexState.DEFAULT), YamlLexer.SCOPE_YAML_ALIAS, 1);
        assertScopeCount(lexer.lexLine("type: !!str", LexState.DEFAULT), YamlLexer.SCOPE_YAML_TAG, 1);
    }

    @Test
    void doubleQuotedStringStatePropagatesAcrossLines() {
        LexResult first = lexer.lexLine("name: \"Ada", LexState.DEFAULT);
        assertEquals(LexState.of(1), first.exitState());

        LexResult second = lexer.lexLine("Lovelace\"", first.exitState());
        assertEquals(LexState.DEFAULT, second.exitState());
        assertTrue(second.tokens().stream().anyMatch(token -> token.type() == TokenType.STRING));
    }

    @Test
    void singleQuotedStringStatePropagatesAcrossLinesAndHonorsEscapedQuote() {
        LexResult first = lexer.lexLine("name: 'Ada''s", LexState.DEFAULT);
        assertEquals(LexState.of(2), first.exitState());

        LexResult second = lexer.lexLine("notes'", first.exitState());
        assertEquals(LexState.DEFAULT, second.exitState());
        assertTrue(second.tokens().stream().anyMatch(token -> token.type() == TokenType.STRING));
    }

    @Test
    void blockScalarIndicatorPropagatesBlockScalarState() {
        LexResult header = lexer.lexLine("script: |-", LexState.DEFAULT);
        assertTypes(header, TokenType.IDENTIFIER, TokenType.PUNCTUATION, TokenType.PUNCTUATION);
        assertBlockState(header.exitState(), 0, -1, '|', '-');

        LexResult body = lexer.lexLine("  echo hello", header.exitState());
        assertTypes(body, TokenType.PLAIN);
        assertBlockState(body.exitState(), 0, 2, '|', '-');

        LexResult next = lexer.lexLine("done: true", body.exitState());
        assertTypes(next, TokenType.IDENTIFIER, TokenType.PUNCTUATION, TokenType.BOOLEAN);
        assertEquals(LexState.DEFAULT, next.exitState());
    }

    @Test
    void blockScalarStateTracksExplicitIndentStyleAndChomping() {
        LexResult header = lexer.lexLine("script: >2+", LexState.DEFAULT);
        assertBlockState(header.exitState(), 0, 2, '>', '+');

        LexResult body = lexer.lexLine("  folded line", header.exitState());
        assertTypes(body, TokenType.PLAIN);
        assertBlockState(body.exitState(), 0, 2, '>', '+');

        LexResult blank = lexer.lexLine("", body.exitState());
        assertEquals(0, blank.tokens().size());
        assertBlockState(blank.exitState(), 0, 2, '>', '+');

        LexResult next = lexer.lexLine("next: false", blank.exitState());
        assertTypes(next, TokenType.IDENTIFIER, TokenType.PUNCTUATION, TokenType.BOOLEAN);
        assertEquals(LexState.DEFAULT, next.exitState());
    }

    @Test
    void flowMappingKeysAreRecognized() {
        LexResult result = lexer.lexLine("{ name: Ada, enabled: true }", LexState.DEFAULT);

        assertEquals(2, result.tokens().stream()
            .filter(token -> YamlLexer.SCOPE_YAML_KEY.equals(token.styleScope()))
            .count());
        assertTrue(result.tokens().stream().anyMatch(token -> token.type() == TokenType.BOOLEAN));
    }

    @Test
    void incrementalRelexPropagatesBlockScalarStateChangesToFollowingLines() {
        CountingLexer countingLexer = new CountingLexer(new YamlLexer());
        TokenMap baseline = IncrementalLexerEngine.relex(
            TokenMap.empty(),
            "script: |\n  echo hello\nnext: true",
            0,
            countingLexer
        );
        assertEquals(3, baseline.lineCount());
        assertEquals(3, baseline.lineAt(1).entryState().code());
        assertEquals(3, baseline.lineAt(2).entryState().code());

        countingLexer.reset();
        TokenMap updated = IncrementalLexerEngine.relex(
            baseline,
            "script: plain\n  echo hello\nnext: true",
            0,
            countingLexer
        );

        assertEquals(3, countingLexer.invocations());
        assertEquals(LexState.DEFAULT, updated.lineAt(1).entryState());
        assertEquals(LexState.DEFAULT, updated.lineAt(2).entryState());
        assertEquals(
            List.of(TokenType.IDENTIFIER, TokenType.PUNCTUATION, TokenType.BOOLEAN),
            updated.lineAt(2).tokens().stream().map(Token::type).toList()
        );
    }

    private static void assertTypes(LexResult result, TokenType... expected) {
        assertEquals(List.of(expected), result.tokens().stream().map(Token::type).toList());
    }

    private static void assertScopeCount(LexResult result, String scope, long expected) {
        assertEquals(expected, result.tokens().stream()
            .filter(token -> scope.equals(token.styleScope()))
            .count());
    }

    private static void assertBlockState(
        LexState state,
        int headerIndent,
        int contentIndent,
        char style,
        char chomping
    ) {
        assertEquals(3, state.code());
        assertEquals(headerIndent, state.blockScalarHeaderIndent());
        assertEquals(contentIndent, state.blockScalarContentIndent());
        assertEquals(style, state.blockScalarStyle());
        assertEquals(chomping, state.blockScalarChomping());
    }

    private static final class CountingLexer implements Lexer {
        private final Lexer delegate;
        private int invocations;

        private CountingLexer(Lexer delegate) {
            this.delegate = delegate;
        }

        @Override
        public String languageId() {
            return delegate.languageId();
        }

        @Override
        public LexState initialState() {
            return delegate.initialState();
        }

        @Override
        public LexResult lexLine(String lineText, LexState entryState) {
            invocations++;
            return delegate.lexLine(lineText, entryState);
        }

        private int invocations() {
            return invocations;
        }

        private void reset() {
            invocations = 0;
        }
    }
}
