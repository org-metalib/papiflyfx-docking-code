package org.metalib.papifly.fx.code.theme;

import javafx.scene.paint.Color;
import org.metalib.papifly.fx.code.lexer.JsonLexer;
import org.metalib.papifly.fx.code.lexer.TokenType;

import java.util.Collection;
import java.util.List;

public final class JsonSyntaxStyleProvider implements SyntaxStyleProvider {

    @Override
    public Collection<SyntaxStyleScope> getSyntaxStyleScopes() {
        return List.of(new SyntaxStyleScope(
            JsonLexer.SCOPE_JSON_KEY,
            "JSON Key",
            TokenType.STRING,
            Color.web("#9cdcfe"),
            Color.web("#0451a5")
        ));
    }
}
