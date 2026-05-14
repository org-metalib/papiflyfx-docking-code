package org.metalib.papifly.fx.code.lexer;

/**
 * Supported token categories for syntax rendering.
 */
public enum TokenType {
    /**
     * Plain text token.
     */
    PLAIN,
    /**
     * Keyword token.
     */
    KEYWORD,
    /**
     * String literal token.
     */
    STRING,
    /**
     * JSON object key token.
     */
    JSON_KEY,
    /**
     * YAML mapping key token.
     */
    YAML_KEY,
    /**
     * YAML anchor token, such as {@code &base}.
     */
    YAML_ANCHOR,
    /**
     * YAML alias token, such as {@code *base}.
     */
    YAML_ALIAS,
    /**
     * YAML tag token, such as {@code !!str} or {@code !Custom}.
     */
    YAML_TAG,
    /**
     * Comment token.
     */
    COMMENT,
    /**
     * Numeric literal token.
     */
    NUMBER,
    /**
     * Boolean literal token.
     */
    BOOLEAN,
    /**
     * Null literal token.
     */
    NULL_LITERAL,
    /**
     * Operator token.
     */
    OPERATOR,
    /**
     * Punctuation token.
     */
    PUNCTUATION,
    /**
     * Identifier token.
     */
    IDENTIFIER,
    
    // Markdown elements
    /**
     * Markdown headline token.
     */
    HEADLINE,
    /**
     * Markdown list item marker token.
     */
    LIST_ITEM,
    /**
     * Markdown fenced code block token.
     */
    CODE_BLOCK,
    /**
     * Generic markdown text token.
     */
    TEXT
}
