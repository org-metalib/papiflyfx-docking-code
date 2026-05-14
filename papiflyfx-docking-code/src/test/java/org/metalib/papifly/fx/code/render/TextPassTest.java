package org.metalib.papifly.fx.code.render;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.lexer.Token;
import org.metalib.papifly.fx.code.lexer.TokenType;
import org.metalib.papifly.fx.code.theme.CodeEditorTheme;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextPassTest {

    @Test
    void styleScopeColorWinsBeforeTokenTypeFallback() {
        CodeEditorTheme theme = CodeEditorTheme.dark()
            .withSyntaxScopeColors(Map.of("json.key", Color.RED));

        assertEquals(
            Color.RED,
            TextPass.resolveTokenColor(theme, new Token(0, 4, TokenType.STRING, "json.key"))
        );
    }

    @Test
    void unknownStyleScopeFallsBackToTokenTypeColor() {
        CodeEditorTheme theme = CodeEditorTheme.dark();

        assertEquals(
            theme.stringColor(),
            TextPass.resolveTokenColor(theme, new Token(0, 4, TokenType.STRING, "unknown.scope"))
        );
    }

    @Test
    void plainTextFallsBackToEditorForeground() {
        CodeEditorTheme theme = CodeEditorTheme.dark();

        assertEquals(
            theme.editorForeground(),
            TextPass.resolveTokenColor(theme, new Token(0, 4, TokenType.TEXT))
        );
    }
}
