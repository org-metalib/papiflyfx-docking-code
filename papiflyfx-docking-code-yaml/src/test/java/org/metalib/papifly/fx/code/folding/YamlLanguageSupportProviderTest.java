package org.metalib.papifly.fx.code.folding;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.language.LanguageSupport;
import org.metalib.papifly.fx.code.language.LanguageSupportProvider;
import org.metalib.papifly.fx.code.language.LanguageSupportRegistry;
import org.metalib.papifly.fx.code.lexer.YamlLexer;
import org.metalib.papifly.fx.code.theme.SyntaxStyleProvider;
import org.metalib.papifly.fx.code.theme.SyntaxStyleRegistry;

import java.util.ServiceLoader;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlLanguageSupportProviderTest {

    @Test
    void providerContributesYamlSupport() {
        LanguageSupport support = new YamlLanguageSupportProvider().getLanguageSupports().iterator().next();

        assertEquals("yaml", support.id());
        assertEquals("YAML", support.displayName());
        assertEquals(Set.of("yml"), support.aliases());
        assertEquals(Set.of("yaml", "yml"), support.fileExtensions());
        assertEquals(Set.of(
            YamlLexer.SCOPE_YAML_KEY,
            YamlLexer.SCOPE_YAML_ANCHOR,
            YamlLexer.SCOPE_YAML_ALIAS,
            YamlLexer.SCOPE_YAML_TAG
        ), support.customTokenScopes());
        assertEquals(2, support.editorDefaults().indentWidth());
        assertInstanceOf(YamlLexer.class, support.lexerFactory().get());
        assertInstanceOf(YamlFoldProvider.class, support.foldProviderFactory().get());
    }

    @Test
    void serviceLoaderDiscoversYamlSupport() {
        Set<String> ids = ServiceLoader.load(LanguageSupportProvider.class).stream()
            .flatMap(provider -> provider.get().getLanguageSupports().stream())
            .map(LanguageSupport::id)
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(ids.contains("yaml"));
        assertInstanceOf(YamlLexer.class, LanguageSupportRegistry.defaultRegistry().resolveLexer("yml"));
        assertEquals("yaml", LanguageSupportRegistry.defaultRegistry().resolveFoldProvider("yaml").languageId());
    }

    @Test
    void serviceLoaderDiscoversYamlSyntaxStyleScopes() {
        Set<String> scopeIds = ServiceLoader.load(SyntaxStyleProvider.class).stream()
            .flatMap(provider -> provider.get().getSyntaxStyleScopes().stream())
            .map(scope -> scope.id())
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(scopeIds.contains(YamlLexer.SCOPE_YAML_KEY));
        assertTrue(scopeIds.contains(YamlLexer.SCOPE_YAML_ANCHOR));
        assertTrue(scopeIds.contains(YamlLexer.SCOPE_YAML_ALIAS));
        assertTrue(scopeIds.contains(YamlLexer.SCOPE_YAML_TAG));
        assertTrue(SyntaxStyleRegistry.defaultRegistry().scope(YamlLexer.SCOPE_YAML_KEY).isPresent());
    }
}
