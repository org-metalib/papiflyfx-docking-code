package org.metalib.papifly.fx.code.settings;

import org.metalib.papifly.fx.code.language.LanguageEditorDefaults;
import org.metalib.papifly.fx.code.language.LanguageSupportRegistry;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingsStorage;

import java.util.Locale;
import java.util.Optional;

/**
 * Resolves per-language editor settings from {@link SettingsStorage}.
 * <p>
 * Resolution order is:
 * {@code editor.language.<id>.*}, then {@code editor.language.default.*},
 * then language-provider defaults, then core fallback defaults.
 */
public final class LanguageEditorSettingsResolver {

    public static final String DEFAULT_PREFIX = "editor.language.default.";
    public static final String LANGUAGE_PREFIX = "editor.language.";
    public static final String INDENT_WIDTH = "indentWidth";
    public static final String INSERT_SPACES = "insertSpaces";
    public static final String ENSURE_TRAILING_NEWLINE = "ensureTrailingNewline";
    public static final String TRIM_TRAILING_WHITESPACE = "trimTrailingWhitespace";

    private final SettingsStorage storage;
    private final LanguageSupportRegistry registry;

    /**
     * Creates a resolver backed by settings storage and a language registry.
     *
     * @param storage settings storage, or {@code null} for defaults only
     * @param registry language registry, or {@code null} for the default registry
     */
    public LanguageEditorSettingsResolver(SettingsStorage storage, LanguageSupportRegistry registry) {
        this.storage = storage;
        this.registry = registry == null ? LanguageSupportRegistry.defaultRegistry() : registry;
    }

    /**
     * Resolves editor settings for a language.
     *
     * @param languageId requested language id
     * @return resolved language editor settings
     */
    public LanguageEditorSettings resolve(String languageId) {
        String id = registry.normalizeLanguageId(languageId);
        LanguageEditorDefaults defaults = registry.findLanguageSupport(id)
            .map(support -> support.editorDefaults())
            .orElse(LanguageEditorDefaults.standard());

        int indentWidth = resolveInt(id, INDENT_WIDTH, defaults.indentWidth());
        boolean insertSpaces = resolveBoolean(id, INSERT_SPACES, defaults.insertSpaces());
        boolean ensureTrailingNewline = resolveBoolean(
            id,
            ENSURE_TRAILING_NEWLINE,
            defaults.ensureTrailingNewline()
        );
        boolean trimTrailingWhitespace = resolveBoolean(
            id,
            TRIM_TRAILING_WHITESPACE,
            defaults.trimTrailingWhitespace()
        );

        return new LanguageEditorSettings(
            id,
            indentWidth,
            insertSpaces,
            ensureTrailingNewline,
            trimTrailingWhitespace
        );
    }

    private int resolveInt(String languageId, String settingName, int defaultValue) {
        Optional<String> raw = firstRaw(languageKey(languageId, settingName));
        if (raw.isEmpty()) {
            raw = firstRaw(defaultKey(settingName));
        }
        if (raw.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.get());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private boolean resolveBoolean(String languageId, String settingName, boolean defaultValue) {
        Optional<String> raw = firstRaw(languageKey(languageId, settingName));
        if (raw.isEmpty()) {
            raw = firstRaw(defaultKey(settingName));
        }
        return raw.map(Boolean::parseBoolean).orElse(defaultValue);
    }

    private Optional<String> firstRaw(String key) {
        if (storage == null) {
            return Optional.empty();
        }
        for (SettingScope scope : SettingScope.resolutionOrder()) {
            Optional<String> value = storage.getRaw(scope, key);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    private static String languageKey(String languageId, String settingName) {
        return LANGUAGE_PREFIX + sanitizeLanguageId(languageId) + "." + settingName;
    }

    private static String defaultKey(String settingName) {
        return DEFAULT_PREFIX + settingName;
    }

    private static String sanitizeLanguageId(String languageId) {
        return languageId == null || languageId.isBlank()
            ? "plain-text"
            : languageId.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
    }
}
