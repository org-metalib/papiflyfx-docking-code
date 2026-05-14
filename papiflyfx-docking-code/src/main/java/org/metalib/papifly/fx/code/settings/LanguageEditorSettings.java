package org.metalib.papifly.fx.code.settings;

/**
 * Resolved editor preferences for a language.
 *
 * @param languageId              normalized language id
 * @param indentWidth             indentation width in columns
 * @param insertSpaces            true when indentation inserts spaces
 * @param ensureTrailingNewline   true when saved content should end with a newline
 * @param trimTrailingWhitespace  true when saved content should trim trailing whitespace
 */
public record LanguageEditorSettings(
    String languageId,
    int indentWidth,
    boolean insertSpaces,
    boolean ensureTrailingNewline,
    boolean trimTrailingWhitespace
) {
    /**
     * Creates validated language editor settings.
     */
    public LanguageEditorSettings {
        languageId = languageId == null || languageId.isBlank() ? "plain-text" : languageId.trim();
        if (indentWidth < 1) {
            indentWidth = 1;
        } else if (indentWidth > 16) {
            indentWidth = 16;
        }
    }
}
