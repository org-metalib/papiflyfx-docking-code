package org.metalib.papifly.fx.code.language;

/**
 * Default editor preferences contributed by a language support provider.
 *
 * @param indentWidth             preferred indentation width in columns
 * @param insertSpaces            true when indentation should insert spaces
 * @param ensureTrailingNewline   true when saved content should end with a newline
 * @param trimTrailingWhitespace  true when saved content should trim trailing whitespace
 */
public record LanguageEditorDefaults(
    int indentWidth,
    boolean insertSpaces,
    boolean ensureTrailingNewline,
    boolean trimTrailingWhitespace
) {
    private static final int DEFAULT_INDENT_WIDTH = 4;

    /**
     * Creates validated language editor defaults.
     */
    public LanguageEditorDefaults {
        indentWidth = clampIndentWidth(indentWidth);
    }

    /**
     * Core fallback policy used when a language does not supply defaults.
     *
     * @return standard code-editor defaults
     */
    public static LanguageEditorDefaults standard() {
        return new LanguageEditorDefaults(DEFAULT_INDENT_WIDTH, true, true, true);
    }

    /**
     * Convenience factory for space-indented languages.
     *
     * @param indentWidth preferred indentation width
     * @return space-indentation defaults
     */
    public static LanguageEditorDefaults spaces(int indentWidth) {
        return new LanguageEditorDefaults(indentWidth, true, true, true);
    }

    private static int clampIndentWidth(int value) {
        if (value < 1) {
            return 1;
        }
        if (value > 16) {
            return 16;
        }
        return value;
    }
}
