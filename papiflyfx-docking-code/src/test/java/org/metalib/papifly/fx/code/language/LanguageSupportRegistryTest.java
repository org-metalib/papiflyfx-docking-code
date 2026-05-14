package org.metalib.papifly.fx.code.language;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.folding.FoldMap;
import org.metalib.papifly.fx.code.folding.FoldProvider;
import org.metalib.papifly.fx.code.lexer.Lexer;
import org.metalib.papifly.fx.code.lexer.LexResult;
import org.metalib.papifly.fx.code.lexer.LexState;
import org.metalib.papifly.fx.code.lexer.PlainTextLexer;
import org.metalib.papifly.fx.code.lexer.TokenMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageSupportRegistryTest {

    private LanguageSupportRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new LanguageSupportRegistry();
    }

    @Test
    void normalizeNullReturnsPlainText() {
        assertEquals("plain-text", registry.normalizeLanguageId(null));
    }

    @Test
    void normalizeBlankReturnsPlainText() {
        assertEquals("plain-text", registry.normalizeLanguageId("  "));
    }

    @Test
    void normalizeTrimsAndLowerCases() {
        assertEquals("java", registry.normalizeLanguageId("  Java  "));
    }

    @Test
    void registerAndResolveLexer() {
        LanguageSupport support = testSupport("test-lang", Set.of(), Set.of());
        registry.register(support, ConflictPolicy.REJECT_ON_CONFLICT);
        Lexer lexer = registry.resolveLexer("test-lang");
        assertEquals("test-lang", lexer.languageId());
    }

    @Test
    void registerAndResolveFoldProvider() {
        LanguageSupport support = testSupport("test-lang", Set.of(), Set.of());
        registry.register(support, ConflictPolicy.REJECT_ON_CONFLICT);
        FoldProvider provider = registry.resolveFoldProvider("test-lang");
        assertEquals("test-lang", provider.languageId());
    }

    @Test
    void resolveViaAlias() {
        LanguageSupport support = testSupport("test-lang", Set.of("tl", "testlang"), Set.of());
        registry.register(support, ConflictPolicy.REJECT_ON_CONFLICT);
        Lexer lexer = registry.resolveLexer("tl");
        assertEquals("test-lang", lexer.languageId());
    }

    @Test
    void unknownLanguageFallsBackToPlainTextLexer() {
        Lexer lexer = registry.resolveLexer("nonexistent");
        assertEquals(PlainTextLexer.LANGUAGE_ID, lexer.languageId());
    }

    @Test
    void unknownLanguageFallsBackToPlainTextFoldProvider() {
        FoldProvider provider = registry.resolveFoldProvider("nonexistent");
        assertEquals(PlainTextLexer.LANGUAGE_ID, provider.languageId());
    }

    @Test
    void rejectOnConflictThrowsForDuplicateId() {
        LanguageSupport support1 = testSupport("conflict-lang", Set.of(), Set.of());
        LanguageSupport support2 = testSupport("conflict-lang", Set.of(), Set.of());
        registry.register(support1, ConflictPolicy.REJECT_ON_CONFLICT);
        assertThrows(IllegalStateException.class,
            () -> registry.register(support2, ConflictPolicy.REJECT_ON_CONFLICT));
    }

    @Test
    void replaceExistingReplacesRegistration() {
        LanguageSupport support1 = testSupport("replace-lang", Set.of(), Set.of());
        LanguageSupport support2 = new LanguageSupport(
            "replace-lang", "Replaced",
            Set.of(), Set.of(), Set.of(),
            () -> new StubLexer("replace-lang-v2"),
            () -> new StubFoldProvider("replace-lang-v2")
        );
        registry.register(support1, ConflictPolicy.REPLACE_EXISTING);
        registry.register(support2, ConflictPolicy.REPLACE_EXISTING);
        Lexer lexer = registry.resolveLexer("replace-lang");
        assertEquals("replace-lang-v2", lexer.languageId());
    }

    @Test
    void aliasConflictRejectsOnStrict() {
        LanguageSupport support1 = testSupport("lang-a", Set.of("shared-alias"), Set.of());
        LanguageSupport support2 = testSupport("lang-b", Set.of("shared-alias"), Set.of());
        registry.register(support1, ConflictPolicy.REJECT_ON_CONFLICT);
        assertThrows(IllegalStateException.class,
            () -> registry.register(support2, ConflictPolicy.REJECT_ON_CONFLICT));
    }

    @Test
    void extensionDetection() {
        LanguageSupport support = testSupport("test-lang", Set.of(), Set.of("tst", "tstx"));
        registry.register(support, ConflictPolicy.REPLACE_EXISTING);
        assertEquals(Optional.of("test-lang"), registry.detectLanguageId("file.tst"));
        assertEquals(Optional.of("test-lang"), registry.detectLanguageId("/path/to/file.tstx"));
        assertEquals(Optional.empty(), registry.detectLanguageId("file.unknown"));
    }

    @Test
    void extensionDetectionCaseInsensitive() {
        LanguageSupport support = testSupport("test-lang", Set.of(), Set.of("tst"));
        registry.register(support, ConflictPolicy.REPLACE_EXISTING);
        assertEquals(Optional.of("test-lang"), registry.detectLanguageId("FILE.TST"));
    }

    @Test
    void userFileAssociationOverridesExtension() {
        LanguageSupport support = testSupport("test-lang", Set.of(), Set.of("tst"));
        registry.register(support, ConflictPolicy.REPLACE_EXISTING);
        registry.setUserFileAssociationMapping(path -> {
            if (path.endsWith(".tst")) {
                return Optional.of("overridden-lang");
            }
            return Optional.empty();
        });
        assertEquals(Optional.of("overridden-lang"), registry.detectLanguageId("file.tst"));
    }

    @Test
    void unregisterRemovesLanguage() {
        LanguageSupport support = testSupport("removable", Set.of("rm"), Set.of("rmv"));
        registry.register(support, ConflictPolicy.REPLACE_EXISTING);
        assertNotNull(registry.resolveLexer("removable"));
        registry.unregister("removable");
        assertEquals(PlainTextLexer.LANGUAGE_ID, registry.resolveLexer("removable").languageId());
        assertEquals(Optional.empty(), registry.detectLanguageId("file.rmv"));
    }

    @Test
    void listenerCalledOnRegisterReplaceUnregister() {
        List<String> events = new ArrayList<>();
        registry.addListener(new LanguageRegistryListener() {
            @Override
            public void onLanguageRegistered(String languageId) {
                events.add("registered:" + languageId);
            }

            @Override
            public void onLanguageReplaced(String languageId) {
                events.add("replaced:" + languageId);
            }

            @Override
            public void onLanguageUnregistered(String languageId) {
                events.add("unregistered:" + languageId);
            }

            @Override
            public void onDiagnostic(RegistryDiagnostic diagnostic) {
                events.add("diagnostic:" + diagnostic.message());
            }
        });

        LanguageSupport support = testSupport("listener-lang", Set.of(), Set.of());
        registry.register(support, ConflictPolicy.REPLACE_EXISTING);
        assertEquals(List.of("registered:listener-lang"), events);

        registry.register(support, ConflictPolicy.REPLACE_EXISTING);
        assertEquals(List.of("registered:listener-lang", "replaced:listener-lang"), events);

        registry.unregister("listener-lang");
        assertEquals(List.of("registered:listener-lang", "replaced:listener-lang", "unregistered:listener-lang"), events);
    }

    @Test
    void diagnosticCapturedOnFactoryFailure() {
        LanguageSupport broken = new LanguageSupport(
            "broken", "Broken",
            Set.of(), Set.of(), Set.of(),
            () -> { throw new RuntimeException("lexer boom"); },
            () -> { throw new RuntimeException("fold boom"); }
        );
        registry.register(broken, ConflictPolicy.REPLACE_EXISTING);
        Lexer lexer = registry.resolveLexer("broken");
        assertEquals(PlainTextLexer.LANGUAGE_ID, lexer.languageId());
        assertFalse(registry.diagnosticsSnapshot().isEmpty());
        assertEquals("Lexer factory failed", registry.diagnosticsSnapshot().getFirst().message());
    }

    @Test
    void diagnosticCapturedOnFoldProviderFactoryFailure() {
        LanguageSupport broken = new LanguageSupport(
            "broken-fold", "Broken Fold",
            Set.of(), Set.of(), Set.of(),
            PlainTextLexer::new,
            () -> { throw new RuntimeException("fold boom"); }
        );
        registry.register(broken, ConflictPolicy.REPLACE_EXISTING);
        FoldProvider provider = registry.resolveFoldProvider("broken-fold");
        assertEquals(PlainTextLexer.LANGUAGE_ID, provider.languageId());
        assertFalse(registry.diagnosticsSnapshot().isEmpty());
    }

    @Test
    void detectLanguageIdReturnsEmptyForNullAndBlank() {
        assertEquals(Optional.empty(), registry.detectLanguageId(null));
        assertEquals(Optional.empty(), registry.detectLanguageId(""));
        assertEquals(Optional.empty(), registry.detectLanguageId("   "));
    }

    @Test
    void detectLanguageIdReturnsEmptyForNoExtension() {
        LanguageSupport support = testSupport("test-lang", Set.of(), Set.of("tst"));
        registry.register(support, ConflictPolicy.REPLACE_EXISTING);
        assertEquals(Optional.empty(), registry.detectLanguageId("Makefile"));
    }

    @Test
    void removeListenerStopsNotifications() {
        List<String> events = new ArrayList<>();
        LanguageRegistryListener listener = new LanguageRegistryListener() {
            @Override
            public void onLanguageRegistered(String languageId) {
                events.add("registered");
            }
            @Override
            public void onLanguageReplaced(String languageId) {}
            @Override
            public void onLanguageUnregistered(String languageId) {}
            @Override
            public void onDiagnostic(RegistryDiagnostic diagnostic) {}
        };
        registry.addListener(listener);
        registry.register(testSupport("a", Set.of(), Set.of()), ConflictPolicy.REPLACE_EXISTING);
        assertEquals(1, events.size());
        registry.removeListener(listener);
        registry.register(testSupport("b", Set.of(), Set.of()), ConflictPolicy.REPLACE_EXISTING);
        assertEquals(1, events.size());
    }

    private static LanguageSupport testSupport(String id, Set<String> aliases, Set<String> extensions) {
        return new LanguageSupport(
            id, id,
            aliases, extensions, Set.of(),
            () -> new StubLexer(id),
            () -> new StubFoldProvider(id)
        );
    }

    private static final class StubLexer implements Lexer {
        private final String id;
        StubLexer(String id) { this.id = id; }
        @Override public String languageId() { return id; }
        @Override public LexResult lexLine(String lineText, LexState entryState) {
            return new LexResult(List.of(), LexState.DEFAULT);
        }
    }

    private static final class StubFoldProvider implements FoldProvider {
        private final String id;
        StubFoldProvider(String id) { this.id = id; }
        @Override public String languageId() { return id; }
        @Override public FoldMap recompute(List<String> lines, TokenMap tokenMap, FoldMap baseline,
                                           int dirtyStartLine, BooleanSupplier cancelled) {
            return FoldMap.empty();
        }
    }
}
