package org.metalib.papifly.fx.code.folding;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.language.LanguageSupport;
import org.metalib.papifly.fx.code.language.LanguageSupportProvider;
import org.metalib.papifly.fx.code.language.LanguageSupportRegistry;
import org.metalib.papifly.fx.code.lexer.JsonLexer;
import org.metalib.papifly.fx.code.theme.SyntaxStyleProvider;
import org.metalib.papifly.fx.code.theme.SyntaxStyleRegistry;

import java.util.ServiceLoader;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonLanguageSupportProviderTest {

    @Test
    void providerContributesJsonSupport() {
        LanguageSupport support = new JsonLanguageSupportProvider().getLanguageSupports().iterator().next();

        assertEquals("json", support.id());
        assertEquals("JSON", support.displayName());
        assertEquals(Set.of(), support.aliases());
        assertEquals(Set.of("json"), support.fileExtensions());
        assertEquals(Set.of(JsonLexer.SCOPE_JSON_KEY), support.customTokenScopes());
        assertEquals(2, support.editorDefaults().indentWidth());
        assertInstanceOf(JsonLexer.class, support.lexerFactory().get());
        assertInstanceOf(JsonFoldProvider.class, support.foldProviderFactory().get());
    }

    @Test
    void serviceLoaderDiscoversJsonSupport() {
        Set<String> ids = ServiceLoader.load(LanguageSupportProvider.class).stream()
            .flatMap(provider -> provider.get().getLanguageSupports().stream())
            .map(LanguageSupport::id)
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(ids.contains("json"));
        assertInstanceOf(JsonLexer.class, LanguageSupportRegistry.defaultRegistry().resolveLexer("json"));
        assertEquals("json", LanguageSupportRegistry.defaultRegistry().resolveFoldProvider("json").languageId());
    }

    @Test
    void serviceLoaderDiscoversJsonSyntaxStyleScope() {
        Set<String> scopeIds = ServiceLoader.load(SyntaxStyleProvider.class).stream()
            .flatMap(provider -> provider.get().getSyntaxStyleScopes().stream())
            .map(scope -> scope.id())
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(scopeIds.contains(JsonLexer.SCOPE_JSON_KEY));
        assertTrue(SyntaxStyleRegistry.defaultRegistry().scope(JsonLexer.SCOPE_JSON_KEY).isPresent());
    }
}
