package org.metalib.papifly.fx.code.lexer;

import java.util.Set;

/**
 * Java language lexer (MVP subset).
 */
public final class JavaLexer extends AbstractCStyleLexer {

    private static final Set<String> KEYWORDS = Set.of(
        "abstract", "assert", "break", "case", "catch", "class", "const", "continue", "default", "do",
        "else", "enum", "extends", "final", "finally", "for", "goto", "if", "implements", "import",
        "instanceof", "interface", "native", "new", "package", "private", "protected", "public", "return",
        "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient",
        "try", "volatile", "while", "var", "record", "sealed", "permits", "non-sealed", "yield",
        "module", "requires", "exports", "opens", "to", "uses", "provides", "with",
        "byte", "short", "int", "long", "float", "double", "char", "boolean", "void"
    );

    /**
     * Stable id for Java language.
     */
    public static final String LANGUAGE_ID = "java";

    /**
     * Creates a Java lexer instance.
     */
    public JavaLexer() {
        super(LANGUAGE_ID, KEYWORDS, false);
    }
}
