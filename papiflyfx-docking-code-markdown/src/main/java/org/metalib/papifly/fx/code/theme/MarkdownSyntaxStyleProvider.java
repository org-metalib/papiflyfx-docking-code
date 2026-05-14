package org.metalib.papifly.fx.code.theme;

import javafx.scene.paint.Color;
import org.metalib.papifly.fx.code.lexer.MarkdownLexer;
import org.metalib.papifly.fx.code.lexer.TokenType;

import java.util.Collection;
import java.util.List;

public final class MarkdownSyntaxStyleProvider implements SyntaxStyleProvider {

    @Override
    public Collection<SyntaxStyleScope> getSyntaxStyleScopes() {
        return List.of(
            new SyntaxStyleScope(
                MarkdownLexer.SCOPE_MARKDOWN_HEADLINE,
                "Markdown Headline",
                TokenType.TEXT,
                Color.web("#569cd6"),
                Color.web("#0000ff")
            ),
            new SyntaxStyleScope(
                MarkdownLexer.SCOPE_MARKDOWN_LIST_ITEM,
                "Markdown List Item",
                TokenType.TEXT,
                Color.web("#9cdcfe"),
                Color.web("#001080")
            ),
            new SyntaxStyleScope(
                MarkdownLexer.SCOPE_MARKDOWN_CODE_BLOCK,
                "Markdown Code Block",
                TokenType.TEXT,
                Color.web("#d7ba7d"),
                Color.web("#795e26")
            )
        );
    }
}
