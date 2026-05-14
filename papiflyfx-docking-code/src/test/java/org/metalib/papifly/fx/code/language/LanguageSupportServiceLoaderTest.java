package org.metalib.papifly.fx.code.language;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.lexer.Lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguageSupportServiceLoaderTest {

    @Test
    void serviceLoaderDiscoversTestProvider() {
        LanguageSupportRegistry registry = new LanguageSupportRegistry();
        registry.bootstrap(new BootstrapOptions(true, true, ConflictPolicy.REPLACE_EXISTING));

        Lexer lexer = registry.resolveLexer(TestLanguageSupportProvider.TEST_LANGUAGE_ID);
        assertEquals(TestLanguageSupportProvider.TEST_LANGUAGE_ID, lexer.languageId());
    }

    @Test
    void extensionDetectionWorksForSpiProvider() {
        LanguageSupportRegistry registry = new LanguageSupportRegistry();
        registry.bootstrap(new BootstrapOptions(true, true, ConflictPolicy.REPLACE_EXISTING));

        assertEquals(
            TestLanguageSupportProvider.TEST_LANGUAGE_ID,
            registry.detectLanguageId("file.tpl").orElse("")
        );
    }

    @Test
    void aliasResolutionWorksForSpiProvider() {
        LanguageSupportRegistry registry = new LanguageSupportRegistry();
        registry.bootstrap(new BootstrapOptions(true, true, ConflictPolicy.REPLACE_EXISTING));

        Lexer lexer = registry.resolveLexer("tpl");
        assertEquals(TestLanguageSupportProvider.TEST_LANGUAGE_ID, lexer.languageId());
    }

    @Test
    void spiProvidersExcludedWithoutServiceLoaderFlag() {
        LanguageSupportRegistry registry = new LanguageSupportRegistry();
        registry.bootstrap(new BootstrapOptions(true, false, ConflictPolicy.REPLACE_EXISTING));

        Lexer lexer = registry.resolveLexer(TestLanguageSupportProvider.TEST_LANGUAGE_ID);
        assertEquals("plain-text", lexer.languageId());
    }

    @Test
    void runtimeRefreshAddsNewProviders() {
        LanguageSupportRegistry registry = new LanguageSupportRegistry();
        registry.bootstrap(new BootstrapOptions(true, false, ConflictPolicy.REPLACE_EXISTING));

        assertEquals("plain-text", registry.resolveLexer(TestLanguageSupportProvider.TEST_LANGUAGE_ID).languageId());

        registry.refreshServiceProviders(
            Thread.currentThread().getContextClassLoader(),
            ConflictPolicy.REPLACE_EXISTING
        );

        assertEquals(
            TestLanguageSupportProvider.TEST_LANGUAGE_ID,
            registry.resolveLexer(TestLanguageSupportProvider.TEST_LANGUAGE_ID).languageId()
        );
    }
}
