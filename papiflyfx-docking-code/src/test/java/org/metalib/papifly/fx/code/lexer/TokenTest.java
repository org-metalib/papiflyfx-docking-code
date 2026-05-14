package org.metalib.papifly.fx.code.lexer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenTest {

    @Test
    void threeArgumentConstructorLeavesStyleScopeAbsent() {
        Token token = new Token(1, 2, TokenType.STRING);

        assertEquals(1, token.startColumn());
        assertEquals(3, token.endColumn());
        assertEquals(TokenType.STRING, token.type());
        assertNull(token.styleScope());
    }

    @Test
    void styleScopeIsTrimmedAndBlankBecomesNull() {
        assertEquals("json.key", new Token(0, 1, TokenType.STRING, " json.key ").styleScope());
        assertNull(new Token(0, 1, TokenType.STRING, " ").styleScope());
    }

    @Test
    void rejectsInvalidBoundsAndMissingType() {
        assertThrows(IllegalArgumentException.class, () -> new Token(-1, 1, TokenType.STRING));
        assertThrows(IllegalArgumentException.class, () -> new Token(0, 0, TokenType.STRING));
        assertThrows(NullPointerException.class, () -> new Token(0, 1, null));
    }
}
