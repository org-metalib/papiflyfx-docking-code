package org.metalib.papifly.fx.code.folding;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.language.LanguageSupport;
import org.metalib.papifly.fx.code.language.LanguageSupportProvider;
import org.metalib.papifly.fx.code.language.LanguageSupportRegistry;
import org.metalib.papifly.fx.code.lexer.MarkdownLexer;
import org.metalib.papifly.fx.code.theme.SyntaxStyleProvider;
import org.metalib.papifly.fx.code.theme.SyntaxStyleRegistry;

import java.util.ServiceLoader;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownLanguageSupportProviderTest {

    @Test
    void providerContributesMarkdownSupport() {
        LanguageSupport support = new MarkdownLanguageSupportProvider().getLanguageSupports().iterator().next();

        assertEquals("markdown", support.id());
        assertEquals("Markdown", support.displayName());
        assertEquals(Set.of("md"), support.aliases());
        assertEquals(Set.of("md", "markdown"), support.fileExtensions());
        assertEquals(Set.of(
            MarkdownLexer.SCOPE_MARKDOWN_HEADLINE,
            MarkdownLexer.SCOPE_MARKDOWN_LIST_ITEM,
            MarkdownLexer.SCOPE_MARKDOWN_CODE_BLOCK
        ), support.customTokenScopes());
        assertEquals(2, support.editorDefaults().indentWidth());
        assertInstanceOf(MarkdownLexer.class, support.lexerFactory().get());
        assertInstanceOf(MarkdownFoldProvider.class, support.foldProviderFactory().get());
    }

    @Test
    void serviceLoaderDiscoversMarkdownSupport() {
        Set<String> ids = ServiceLoader.load(LanguageSupportProvider.class).stream()
            .flatMap(provider -> provider.get().getLanguageSupports().stream())
            .map(LanguageSupport::id)
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(ids.contains("markdown"));
        assertInstanceOf(MarkdownLexer.class, LanguageSupportRegistry.defaultRegistry().resolveLexer("md"));
        assertEquals("markdown", LanguageSupportRegistry.defaultRegistry().resolveFoldProvider("markdown").languageId());
    }

    @Test
    void serviceLoaderDiscoversMarkdownSyntaxStyleScopes() {
        Set<String> scopeIds = ServiceLoader.load(SyntaxStyleProvider.class).stream()
            .flatMap(provider -> provider.get().getSyntaxStyleScopes().stream())
            .map(scope -> scope.id())
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(scopeIds.contains(MarkdownLexer.SCOPE_MARKDOWN_HEADLINE));
        assertTrue(scopeIds.contains(MarkdownLexer.SCOPE_MARKDOWN_LIST_ITEM));
        assertTrue(scopeIds.contains(MarkdownLexer.SCOPE_MARKDOWN_CODE_BLOCK));
        assertTrue(SyntaxStyleRegistry.defaultRegistry().scope(MarkdownLexer.SCOPE_MARKDOWN_HEADLINE).isPresent());
    }
}
