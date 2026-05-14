package org.metalib.papifly.fx.code.folding;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.language.LanguageSupport;
import org.metalib.papifly.fx.code.language.LanguageSupportProvider;
import org.metalib.papifly.fx.code.language.LanguageSupportRegistry;
import org.metalib.papifly.fx.code.lexer.JavaScriptLexer;

import java.util.ServiceLoader;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaScriptLanguageSupportProviderTest {

    @Test
    void providerContributesJavaScriptSupport() {
        LanguageSupport support = new JavaScriptLanguageSupportProvider().getLanguageSupports().iterator().next();

        assertEquals("javascript", support.id());
        assertEquals("JavaScript", support.displayName());
        assertEquals(Set.of("js"), support.aliases());
        assertEquals(Set.of("js", "mjs", "cjs"), support.fileExtensions());
        assertEquals(4, support.editorDefaults().indentWidth());
        assertInstanceOf(JavaScriptLexer.class, support.lexerFactory().get());
        assertInstanceOf(JavaScriptFoldProvider.class, support.foldProviderFactory().get());
    }

    @Test
    void serviceLoaderDiscoversJavaScriptSupport() {
        Set<String> ids = ServiceLoader.load(LanguageSupportProvider.class).stream()
            .flatMap(provider -> provider.get().getLanguageSupports().stream())
            .map(LanguageSupport::id)
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(ids.contains("javascript"));
        assertInstanceOf(JavaScriptLexer.class, LanguageSupportRegistry.defaultRegistry().resolveLexer("js"));
        assertEquals("javascript", LanguageSupportRegistry.defaultRegistry().resolveFoldProvider("javascript").languageId());
    }
}
