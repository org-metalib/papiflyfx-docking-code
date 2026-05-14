package org.metalib.papifly.fx.code.theme;

import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.code.lexer.TokenType;

import java.util.Objects;

/**
 * Describes a semantic syntax style scope contributed by a language pack.
 * <p>
 * Scope ids are stable lowercase dotted identifiers such as
 * {@code json.key} or {@code markdown.headline}. The fallback token type lets
 * renderers keep meaningful colors when a scope is unknown to the active
 * theme.
 *
 * @param id                stable scope id
 * @param displayName       human-readable scope name
 * @param fallbackTokenType token category used when the scope is unavailable
 * @param darkDefault       default color in dark editor palettes
 * @param lightDefault      default color in light editor palettes
 */
public record SyntaxStyleScope(
    String id,
    String displayName,
    TokenType fallbackTokenType,
    Paint darkDefault,
    Paint lightDefault
) {
    /**
     * Creates a validated style scope descriptor.
     */
    public SyntaxStyleScope {
        id = SyntaxStyleRegistry.normalizeScopeId(id);
        if (id.isEmpty()) {
            throw new IllegalArgumentException("id must not be null or blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be null or blank");
        }
        displayName = displayName.trim();
        fallbackTokenType = Objects.requireNonNull(fallbackTokenType, "fallbackTokenType");
        darkDefault = Objects.requireNonNull(darkDefault, "darkDefault");
        lightDefault = Objects.requireNonNull(lightDefault, "lightDefault");
    }
}
