package org.metalib.papifly.fx.code.theme;

import javafx.scene.paint.Color;
import org.metalib.papifly.fx.code.lexer.TokenType;

import java.util.Collection;
import java.util.List;

public final class TestSyntaxStyleProvider implements SyntaxStyleProvider {

    static final String TEST_SCOPE = "test.scope";

    @Override
    public Collection<SyntaxStyleScope> getSyntaxStyleScopes() {
        return List.of(new SyntaxStyleScope(
            TEST_SCOPE,
            "Test Scope",
            TokenType.STRING,
            Color.web("#123456"),
            Color.web("#654321")
        ));
    }
}
