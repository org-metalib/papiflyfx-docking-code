package org.metalib.papifly.fx.code.lexer;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IncrementalLexerEngineTest {

    @Test
    void splitLinesKeepsTrailingEmptyLine() {
        List<String> lines = IncrementalLexerEngine.splitLines("a\nb\n");
        assertEquals(List.of("a", "b", ""), lines);
    }

    @Test
    void relexReusesUnchangedSuffixWhenEntryStateMatches() {
        CountingLexer lexer = new CountingLexer(new PlainTextLexer());
        TokenMap baseline = IncrementalLexerEngine.relex(TokenMap.empty(), "a\nb\nc", 0, lexer);
        assertEquals(3, lexer.invocations());

        lexer.reset();
        TokenMap updated = IncrementalLexerEngine.relex(baseline, "a\nbx\nc", 1, lexer);

        assertEquals(1, lexer.invocations());
        assertSame(baseline.lineAt(0), updated.lineAt(0));
        assertSame(baseline.lineAt(2), updated.lineAt(2));
    }

    @Test
    void relexPropagatesToFollowingLinesWhenExitStateChanges() {
        CountingLexer lexer = new CountingLexer(new CommentStateLexer());
        TokenMap baseline = IncrementalLexerEngine.relex(
            TokenMap.empty(),
            "start /*\ninside\n*/ done\ntail",
            0,
            lexer
        );
        assertEquals(4, baseline.lineCount());

        lexer.reset();
        TokenMap updated = IncrementalLexerEngine.relex(
            baseline,
            "start\ninside\n*/ done\ntail",
            0,
            lexer
        );

        assertEquals(3, lexer.invocations());
        assertSame(baseline.lineAt(3), updated.lineAt(3));
    }

    @Test
    void relexHandlesNonContiguousChangesWithSameLineCount() {
        // Regression: non-contiguous edits with unchanged line count must not produce stale tokens.
        // Baseline: ["a0","a1","a2","a3"]
        // Updated:  ["a0x","a1","a2x","a3"], dirtyStartLine=0
        PlainTextLexer plain = new PlainTextLexer();
        TokenMap baseline = IncrementalLexerEngine.relex(
            TokenMap.empty(),
            List.of("a0", "a1", "a2", "a3"),
            0,
            plain
        );
        assertEquals(4, baseline.lineCount());
        assertEquals("a0", baseline.lineAt(0).text());
        assertEquals("a2", baseline.lineAt(2).text());

        TokenMap updated = IncrementalLexerEngine.relex(
            baseline,
            List.of("a0x", "a1", "a2x", "a3"),
            0,
            plain
        );
        assertEquals(4, updated.lineCount());
        assertEquals("a0x", updated.lineAt(0).text());
        assertEquals("a1", updated.lineAt(1).text());
        assertEquals("a2x", updated.lineAt(2).text());
        assertEquals("a3", updated.lineAt(3).text());
        // Line 2 must NOT be stale
        assertNotEquals("a2", updated.lineAt(2).text());
    }

    @Test
    void relexChecksCancellationDuringPrefixReuse() {
        List<String> lines = java.util.stream.IntStream.range(0, 5000)
            .mapToObj(i -> "line-" + i)
            .toList();
        PlainTextLexer lexer = new PlainTextLexer();
        TokenMap baseline = IncrementalLexerEngine.relex(TokenMap.empty(), lines, 0, lexer);

        Thread.currentThread().interrupt();
        try {
            assertThrows(
                CancellationException.class,
                () -> IncrementalLexerEngine.relex(baseline, lines, lines.size(), lexer)
            );
        } finally {
            Thread.interrupted();
        }
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

    private static final class CommentStateLexer implements Lexer {
        private static final int IN_COMMENT = 1;

        @Override
        public String languageId() {
            return "comment-state";
        }

        @Override
        public LexResult lexLine(String lineText, LexState entryState) {
            String line = lineText == null ? "" : lineText;
            boolean insideComment = entryState.code() == IN_COMMENT;
            List<Token> commentToken = line.isEmpty()
                ? List.of()
                : List.of(new Token(0, line.length(), TokenType.COMMENT));

            if (insideComment) {
                if (line.contains("*/")) {
                    return new LexResult(commentToken, LexState.DEFAULT);
                }
                return new LexResult(commentToken, LexState.of(IN_COMMENT));
            }

            if (line.contains("/*") && !line.contains("*/")) {
                return new LexResult(commentToken, LexState.of(IN_COMMENT));
            }

            return new LexResult(List.of(), LexState.DEFAULT);
        }
    }
}
