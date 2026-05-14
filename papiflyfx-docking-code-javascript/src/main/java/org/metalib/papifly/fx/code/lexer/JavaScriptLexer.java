package org.metalib.papifly.fx.code.lexer;

import java.util.Set;

/**
 * JavaScript language lexer (MVP subset).
 */
public final class JavaScriptLexer extends AbstractCStyleLexer {

    private static final Set<String> KEYWORDS = Set.of(
        "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do",
        "else", "export", "extends", "finally", "for", "function", "if", "import", "in", "instanceof",
        "let", "new", "return", "super", "switch", "this", "throw", "try", "typeof", "var", "void",
        "while", "with", "yield", "await"
    );

    /**
     * Stable id for JavaScript language.
     */
    public static final String LANGUAGE_ID = "javascript";

    /**
     * Creates a JavaScript lexer instance.
     */
    public JavaScriptLexer() {
        super(LANGUAGE_ID, KEYWORDS, true);
    }
}
