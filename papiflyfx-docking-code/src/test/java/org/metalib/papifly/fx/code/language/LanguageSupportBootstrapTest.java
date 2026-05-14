package org.metalib.papifly.fx.code.language;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.lexer.Lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class LanguageSupportBootstrapTest {

    @Test
    void defaultBootstrapIncludesBuiltIns() {
        LanguageSupportRegistry registry = new LanguageSupportRegistry();
        registry.bootstrap(BootstrapOptions.defaults());

        assertEquals("plain-text", registry.resolveLexer("plain-text").languageId());
        assertEquals(TestLanguageSupportProvider.TEST_LANGUAGE_ID,
            registry.resolveLexer(TestLanguageSupportProvider.TEST_LANGUAGE_ID).languageId());
        assertEquals("plain-text", registry.resolveLexer("java").languageId());
        assertEquals("plain-text", registry.resolveLexer("javascript").languageId());
        assertEquals("plain-text", registry.resolveLexer("json").languageId());
        assertEquals("plain-text", registry.resolveLexer("markdown").languageId());
        assertEquals("plain-text", registry.resolveLexer("yaml").languageId());
    }

    @Test
    void builtInsOmittedWhenDisabled() {
        LanguageSupportRegistry registry = new LanguageSupportRegistry();
        registry.bootstrap(new BootstrapOptions(false, false, ConflictPolicy.REPLACE_EXISTING));

        Lexer lexer = registry.resolveLexer("java");
        assertEquals("plain-text", lexer.languageId());
    }

    @Test
    void aliasResolutionWithBuiltIns() {
        LanguageSupportRegistry registry = new LanguageSupportRegistry();
        registry.bootstrap(BootstrapOptions.defaults());

        assertEquals("plain-text", registry.resolveLexer("txt").languageId());
        assertEquals("plain-text", registry.resolveLexer("plain").languageId());
        assertEquals("plain-text", registry.resolveLexer("plaintext").languageId());
        assertEquals("plain-text", registry.resolveLexer("text").languageId());
        assertEquals(TestLanguageSupportProvider.TEST_LANGUAGE_ID, registry.resolveLexer("tpl").languageId());
        assertEquals("plain-text", registry.resolveLexer("js").languageId());
        assertEquals("plain-text", registry.resolveLexer("md").languageId());
        assertEquals("plain-text", registry.resolveLexer("yml").languageId());
    }

    @Test
    void extensionDetectionWithBuiltIns() {
        LanguageSupportRegistry registry = new LanguageSupportRegistry();
        registry.bootstrap(BootstrapOptions.defaults());

        assertEquals("plain-text", registry.detectLanguageId("notes.txt").orElse(""));
        assertEquals(TestLanguageSupportProvider.TEST_LANGUAGE_ID, registry.detectLanguageId("fixture.tpl").orElse(""));
        assertEquals("", registry.detectLanguageId("Main.java").orElse(""));
        assertEquals("", registry.detectLanguageId("app.js").orElse(""));
    }

    @Test
    void conflictReplacementWorks() {
        LanguageSupportRegistry registry = new LanguageSupportRegistry();
        registry.bootstrap(BootstrapOptions.defaults());

        LanguageSupport custom = new LanguageSupport(
            "java", "Custom Java",
            java.util.Set.of(), java.util.Set.of("java"),
            java.util.Set.of(),
            org.metalib.papifly.fx.code.lexer.PlainTextLexer::new,
            () -> new org.metalib.papifly.fx.code.folding.FoldProvider() {
                @Override public String languageId() { return "custom-java"; }
                @Override public org.metalib.papifly.fx.code.folding.FoldMap recompute(
                    java.util.List<String> lines,
                    org.metalib.papifly.fx.code.lexer.TokenMap tokenMap,
                    org.metalib.papifly.fx.code.folding.FoldMap baseline,
                    int dirtyStartLine,
                    java.util.function.BooleanSupplier cancelled
                ) { return org.metalib.papifly.fx.code.folding.FoldMap.empty(); }
            }
        );
        registry.register(custom, ConflictPolicy.REPLACE_EXISTING);
        assertEquals("plain-text", registry.resolveLexer("java").languageId());
    }

    @Test
    void defaultRegistryIsPreBootstrapped() {
        LanguageSupportRegistry defaultRegistry = LanguageSupportRegistry.defaultRegistry();
        assertEquals("plain-text", defaultRegistry.resolveLexer("java").languageId());
        assertEquals("plain-text", defaultRegistry.resolveLexer("js").languageId());
        assertEquals("plain-text", defaultRegistry.resolveLexer("yml").languageId());
    }
}
