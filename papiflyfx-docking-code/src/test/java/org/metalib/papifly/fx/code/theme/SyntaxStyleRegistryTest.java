package org.metalib.papifly.fx.code.theme;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.language.ConflictPolicy;
import org.metalib.papifly.fx.code.lexer.TokenType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyntaxStyleRegistryTest {

    @Test
    void registerAndResolveScopeDefaults() {
        SyntaxStyleRegistry registry = new SyntaxStyleRegistry();
        SyntaxStyleScope scope = new SyntaxStyleScope(
            "Example.Scope",
            "Example Scope",
            TokenType.STRING,
            Color.RED,
            Color.BLUE
        );

        registry.register(scope, ConflictPolicy.REJECT_ON_CONFLICT);

        assertTrue(registry.scope("example.scope").isPresent());
        assertEquals(Color.RED, registry.defaultColor("example.scope", true).orElseThrow());
        assertEquals(Color.BLUE, registry.defaultColor("example.scope", false).orElseThrow());
        assertEquals(Color.RED, registry.defaultColors(true).get("example.scope"));
    }

    @Test
    void rejectsDuplicateScopeByDefault() {
        SyntaxStyleRegistry registry = new SyntaxStyleRegistry();
        SyntaxStyleScope scope = new SyntaxStyleScope(
            "dup.scope",
            "Duplicate",
            TokenType.STRING,
            Color.RED,
            Color.BLUE
        );

        registry.register(scope, ConflictPolicy.REJECT_ON_CONFLICT);

        assertThrows(IllegalStateException.class,
            () -> registry.register(scope, ConflictPolicy.REJECT_ON_CONFLICT));
    }

    @Test
    void serviceLoaderDiscoversTestProvider() {
        SyntaxStyleRegistry registry = new SyntaxStyleRegistry();
        registry.bootstrap(true, ConflictPolicy.REPLACE_EXISTING);

        assertEquals(
            Color.web("#123456"),
            registry.defaultColor(TestSyntaxStyleProvider.TEST_SCOPE, true).orElseThrow()
        );
        assertFalse(registry.diagnosticsSnapshot().stream()
            .anyMatch(diagnostic -> diagnostic.sourceProvider().contains("TestSyntaxStyleProvider")));
    }

    @Test
    void snapshotIsImmutable() {
        SyntaxStyleRegistry registry = new SyntaxStyleRegistry();
        registry.register(new SyntaxStyleScope(
            "immutable.scope",
            "Immutable",
            TokenType.TEXT,
            Color.RED,
            Color.BLUE
        ), ConflictPolicy.REPLACE_EXISTING);

        assertThrows(UnsupportedOperationException.class,
            () -> ((List<SyntaxStyleScope>) registry.scopesSnapshot()).add(registry.scope("immutable.scope").orElseThrow()));
    }
}
