package org.metalib.papifly.fx.code.lexer;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.language.TestLanguageSupportProvider;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncrementalLexerPipelineTest {

    @Test
    void appliesJavaTokensAsynchronously() {
        Document document = new Document("plugin Demo {}");
        AtomicReference<TokenMap> applied = new AtomicReference<>(TokenMap.empty());
        IncrementalLexerPipeline pipeline = new IncrementalLexerPipeline(document, applied::set, Runnable::run, 5);
        try {
            pipeline.setLanguageId(TestLanguageSupportProvider.TEST_LANGUAGE_ID);
            assertTrue(waitFor(
                () -> hasTokenType(applied.get(), 0, TokenType.KEYWORD),
                Duration.ofSeconds(5)
            ));
        } finally {
            pipeline.dispose();
        }
    }

    @Test
    void keepsLatestRevisionResultAfterRapidUpdates() {
        Document document = new Document("plugin A {}");
        AtomicReference<TokenMap> applied = new AtomicReference<>(TokenMap.empty());
        IncrementalLexerPipeline pipeline = new IncrementalLexerPipeline(document, applied::set, Runnable::run, 5);
        try {
            pipeline.setLanguageId(TestLanguageSupportProvider.TEST_LANGUAGE_ID);
            document.setText("plugin B {}");
            document.setText("second value = 1;");
            pipeline.setLanguageId(TestLanguageSupportProvider.TEST_LANGUAGE_ID);

            assertTrue(waitFor(() -> {
                LineTokens line = applied.get().lineAt(0);
                return line != null
                    && "second value = 1;".equals(line.text())
                    && hasTokenType(applied.get(), 0, TokenType.KEYWORD);
            }, Duration.ofSeconds(10)));
        } finally {
            pipeline.dispose();
        }
    }

    @Test
    void unknownLanguageFallsBackToPlainText() {
        Document document = new Document("class Demo {}");
        AtomicReference<TokenMap> applied = new AtomicReference<>(TokenMap.empty());
        IncrementalLexerPipeline pipeline = new IncrementalLexerPipeline(document, applied::set, Runnable::run, 5);
        try {
            pipeline.setLanguageId("unknown-language");
            assertTrue(waitFor(() -> {
                LineTokens line = applied.get().lineAt(0);
                return line != null && line.tokens().isEmpty();
            }, Duration.ofSeconds(5)));

            LineTokens line = applied.get().lineAt(0);
            assertNotNull(line);
            assertEquals("class Demo {}", line.text());
        } finally {
            pipeline.dispose();
        }
    }

    @Test
    void preservesEarliestDirtyLineAcrossRapidPendingRevisions() {
        Document document = new Document("plugin A {}\nplugin B {}");
        AtomicReference<TokenMap> applied = new AtomicReference<>(TokenMap.empty());
        IncrementalLexerPipeline pipeline = new IncrementalLexerPipeline(document, applied::set, Runnable::run, 40);
        try {
            pipeline.setLanguageId(TestLanguageSupportProvider.TEST_LANGUAGE_ID);
            assertTrue(waitFor(() -> hasTokenType(applied.get(), 0, TokenType.KEYWORD), Duration.ofSeconds(5)));

            document.replace(0, 6, "plain");
            int lineOneStart = document.toOffset(1, 0);
            document.insert(lineOneStart, "prefix ");

            assertTrue(waitFor(
                () -> !hasTokenType(applied.get(), 0, TokenType.KEYWORD),
                Duration.ofSeconds(5)
            ));
        } finally {
            pipeline.dispose();
        }
    }

    @Test
    void fallsBackToPlainTextForCurrentSnapshotWhenLexerThrowsAndRecovers() {
        Document document = new Document("plugin Demo {}");
        AtomicReference<TokenMap> applied = new AtomicReference<>(TokenMap.empty());
        AtomicBoolean failLexing = new AtomicBoolean(false);
        AtomicInteger throwLexerCalls = new AtomicInteger(0);
        Lexer throwingLexer = new ThrowingLexer(failLexing, throwLexerCalls);
        Function<String, Lexer> resolver = languageId -> "throwing".equals(languageId)
            ? throwingLexer
            : org.metalib.papifly.fx.code.language.LanguageSupportRegistry.defaultRegistry().resolveLexer(languageId);

        IncrementalLexerPipeline pipeline = new IncrementalLexerPipeline(
            document,
            applied::set,
            Runnable::run,
            5,
            resolver
        );
        try {
            pipeline.setLanguageId(TestLanguageSupportProvider.TEST_LANGUAGE_ID);
            assertTrue(waitFor(
                () -> hasTokenType(applied.get(), 0, TokenType.KEYWORD),
                Duration.ofSeconds(5)
            ));

            document.setText("plugin Broken {}");
            failLexing.set(true);
            pipeline.setLanguageId("throwing");
            assertTrue(waitFor(() -> {
                if (throwLexerCalls.get() <= 0) {
                    return false;
                }
                LineTokens line = applied.get().lineAt(0);
                return line != null
                    && "plugin Broken {}".equals(line.text())
                    && line.tokens().isEmpty();
            }, Duration.ofSeconds(5)));

            failLexing.set(false);
            document.setText("plugin recovered = 1;");
            pipeline.setLanguageId(TestLanguageSupportProvider.TEST_LANGUAGE_ID);
            assertTrue(waitFor(() -> {
                LineTokens line = applied.get().lineAt(0);
                return line != null
                    && "plugin recovered = 1;".equals(line.text())
                    && hasTokenType(applied.get(), 0, TokenType.KEYWORD);
            }, Duration.ofSeconds(5)));
        } finally {
            pipeline.dispose();
        }
    }

    @Test
    void disposeStopsApplyingTokenUpdates() {
        Document document = new Document("plugin Demo {}");
        AtomicReference<TokenMap> applied = new AtomicReference<>(TokenMap.empty());
        AtomicInteger applyCount = new AtomicInteger(0);
        IncrementalLexerPipeline pipeline = new IncrementalLexerPipeline(
            document,
            tokenMap -> {
                applied.set(tokenMap);
                applyCount.incrementAndGet();
            },
            Runnable::run,
            5
        );
        try {
            pipeline.setLanguageId(TestLanguageSupportProvider.TEST_LANGUAGE_ID);
            assertTrue(waitFor(
                () -> hasTokenType(applied.get(), 0, TokenType.KEYWORD),
                Duration.ofSeconds(5)
            ));

            int countBeforeDispose = applyCount.get();
            pipeline.dispose();
            document.setText("let value = 1;");

            // No document listener should remain after dispose.
            boolean changedAfterDispose = waitFor(
                () -> applyCount.get() > countBeforeDispose,
                Duration.ofMillis(200)
            );
            assertFalse(changedAfterDispose);
            assertEquals(countBeforeDispose, applyCount.get());
        } finally {
            pipeline.dispose();
        }
    }

    private static boolean hasTokenType(TokenMap tokenMap, int line, TokenType type) {
        if (tokenMap == null) {
            return false;
        }
        return tokenMap.tokensForLine(line).stream().anyMatch(token -> token.type() == type);
    }

    private static boolean waitFor(BooleanSupplier condition, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return condition.getAsBoolean();
    }

    private static final class ThrowingLexer implements Lexer {
        private final AtomicBoolean failLexing;
        private final AtomicInteger calls;
        private final PlainTextLexer delegate = new PlainTextLexer();

        private ThrowingLexer(AtomicBoolean failLexing, AtomicInteger calls) {
            this.failLexing = failLexing;
            this.calls = calls;
        }

        @Override
        public String languageId() {
            return "throwing";
        }

        @Override
        public LexState initialState() {
            return delegate.initialState();
        }

        @Override
        public LexResult lexLine(String lineText, LexState entryState) {
            calls.incrementAndGet();
            if (failLexing.get()) {
                throw new IllegalStateException("simulated lexer failure");
            }
            return delegate.lexLine(lineText, entryState);
        }
    }
}
