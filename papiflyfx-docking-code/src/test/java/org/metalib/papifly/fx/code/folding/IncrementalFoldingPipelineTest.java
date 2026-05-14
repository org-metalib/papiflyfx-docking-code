package org.metalib.papifly.fx.code.folding;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.language.TestLanguageSupportProvider;
import org.metalib.papifly.fx.code.lexer.TokenMap;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IncrementalFoldingPipelineTest {

    @Test
    void appliesLanguageSpecificFoldMapAndCollapsedHeaders() {
        Document document = new Document("plugin block\n  body");
        AtomicReference<FoldMap> applied = new AtomicReference<>(FoldMap.empty());
        IncrementalFoldingPipeline pipeline = new IncrementalFoldingPipeline(
            document,
            TokenMap::empty,
            applied::set,
            Runnable::run,
            languageId -> org.metalib.papifly.fx.code.language.LanguageSupportRegistry.defaultRegistry().resolveFoldProvider(languageId),
            5
        );
        try {
            pipeline.setLanguageId(TestLanguageSupportProvider.TEST_LANGUAGE_ID);
            assertTrue(waitFor(
                () -> applied.get().regions().stream().anyMatch(region -> region.kind() == FoldKind.BRACE_BLOCK),
                Duration.ofSeconds(2)
            ));
            pipeline.setCollapsedHeaders(Set.of(0));
            assertTrue(waitFor(() -> applied.get().isCollapsedHeader(0), Duration.ofSeconds(2)));
        } finally {
            pipeline.dispose();
        }
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
}
