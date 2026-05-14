package org.metalib.papifly.fx.code.theme;

import javafx.scene.paint.Color;
import org.metalib.papifly.fx.code.lexer.TokenType;
import org.metalib.papifly.fx.code.lexer.YamlLexer;

import java.util.Collection;
import java.util.List;

public final class YamlSyntaxStyleProvider implements SyntaxStyleProvider {

    @Override
    public Collection<SyntaxStyleScope> getSyntaxStyleScopes() {
        return List.of(
            new SyntaxStyleScope(
                YamlLexer.SCOPE_YAML_KEY,
                "YAML Key",
                TokenType.IDENTIFIER,
                Color.web("#4ec9b0"),
                Color.web("#267f99")
            ),
            new SyntaxStyleScope(
                YamlLexer.SCOPE_YAML_ANCHOR,
                "YAML Anchor",
                TokenType.IDENTIFIER,
                Color.web("#d7ba7d"),
                Color.web("#795e26")
            ),
            new SyntaxStyleScope(
                YamlLexer.SCOPE_YAML_ALIAS,
                "YAML Alias",
                TokenType.IDENTIFIER,
                Color.web("#d7ba7d"),
                Color.web("#795e26")
            ),
            new SyntaxStyleScope(
                YamlLexer.SCOPE_YAML_TAG,
                "YAML Tag",
                TokenType.IDENTIFIER,
                Color.web("#c586c0"),
                Color.web("#af00db")
            )
        );
    }
}
