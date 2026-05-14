package org.metalib.papifly.fx.code.folding;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.language.LanguageSupport;
import org.metalib.papifly.fx.code.language.LanguageSupportProvider;
import org.metalib.papifly.fx.code.language.LanguageSupportRegistry;
import org.metalib.papifly.fx.code.lexer.JavaLexer;

import java.util.ServiceLoader;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaLanguageSupportProviderTest {

    @Test
    void providerContributesJavaSupport() {
        LanguageSupport support = new JavaLanguageSupportProvider().getLanguageSupports().iterator().next();

        assertEquals("java", support.id());
        assertEquals("Java", support.displayName());
        assertEquals(Set.of("java"), support.fileExtensions());
        assertEquals(4, support.editorDefaults().indentWidth());
        assertInstanceOf(JavaLexer.class, support.lexerFactory().get());
        assertInstanceOf(JavaFoldProvider.class, support.foldProviderFactory().get());
    }

    @Test
    void serviceLoaderDiscoversJavaSupport() {
        Set<String> ids = ServiceLoader.load(LanguageSupportProvider.class).stream()
            .flatMap(provider -> provider.get().getLanguageSupports().stream())
            .map(LanguageSupport::id)
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(ids.contains("java"));
        assertInstanceOf(JavaLexer.class, LanguageSupportRegistry.defaultRegistry().resolveLexer("java"));
        assertEquals("java", LanguageSupportRegistry.defaultRegistry().resolveFoldProvider("java").languageId());
    }
}
