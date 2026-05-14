package org.metalib.papifly.fx.code.language;

import org.metalib.papifly.fx.code.folding.FoldProvider;
import org.metalib.papifly.fx.code.lexer.Lexer;

import java.util.Set;
import java.util.function.Supplier;

public record LanguageSupport(
    String id,
    String displayName,
    Set<String> aliases,
    Set<String> fileExtensions,
    Set<String> customTokenScopes,
    Supplier<Lexer> lexerFactory,
    Supplier<FoldProvider> foldProviderFactory,
    LanguageEditorDefaults editorDefaults
) {
    public LanguageSupport(
        String id,
        String displayName,
        Set<String> aliases,
        Set<String> fileExtensions,
        Set<String> customTokenScopes,
        Supplier<Lexer> lexerFactory,
        Supplier<FoldProvider> foldProviderFactory
    ) {
        this(
            id,
            displayName,
            aliases,
            fileExtensions,
            customTokenScopes,
            lexerFactory,
            foldProviderFactory,
            LanguageEditorDefaults.standard()
        );
    }

    public LanguageSupport {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be null or blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be null or blank");
        }
        if (lexerFactory == null) {
            throw new IllegalArgumentException("lexerFactory must not be null");
        }
        if (foldProviderFactory == null) {
            throw new IllegalArgumentException("foldProviderFactory must not be null");
        }
        aliases = aliases == null ? Set.of() : Set.copyOf(aliases);
        fileExtensions = fileExtensions == null ? Set.of() : Set.copyOf(fileExtensions);
        customTokenScopes = customTokenScopes == null ? Set.of() : Set.copyOf(customTokenScopes);
        editorDefaults = editorDefaults == null ? LanguageEditorDefaults.standard() : editorDefaults;
    }
}
