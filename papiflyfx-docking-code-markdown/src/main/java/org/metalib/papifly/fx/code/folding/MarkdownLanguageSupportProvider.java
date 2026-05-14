package org.metalib.papifly.fx.code.folding;

import org.metalib.papifly.fx.code.language.LanguageEditorDefaults;
import org.metalib.papifly.fx.code.language.LanguageSupport;
import org.metalib.papifly.fx.code.language.LanguageSupportProvider;
import org.metalib.papifly.fx.code.lexer.MarkdownLexer;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class MarkdownLanguageSupportProvider implements LanguageSupportProvider {

    @Override
    public Collection<LanguageSupport> getLanguageSupports() {
        return List.of(new LanguageSupport(
            MarkdownLexer.LANGUAGE_ID, "Markdown",
            Set.of("md"), Set.of("md", "markdown"),
            Set.of(
                MarkdownLexer.SCOPE_MARKDOWN_HEADLINE,
                MarkdownLexer.SCOPE_MARKDOWN_LIST_ITEM,
                MarkdownLexer.SCOPE_MARKDOWN_CODE_BLOCK
            ),
            MarkdownLexer::new, MarkdownFoldProvider::new,
            LanguageEditorDefaults.spaces(2)
        ));
    }
}
