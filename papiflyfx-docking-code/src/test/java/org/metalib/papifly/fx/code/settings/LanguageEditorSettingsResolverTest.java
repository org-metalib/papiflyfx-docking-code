package org.metalib.papifly.fx.code.settings;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.folding.FoldMap;
import org.metalib.papifly.fx.code.folding.FoldProvider;
import org.metalib.papifly.fx.code.language.ConflictPolicy;
import org.metalib.papifly.fx.code.language.LanguageEditorDefaults;
import org.metalib.papifly.fx.code.language.LanguageSupport;
import org.metalib.papifly.fx.code.language.LanguageSupportRegistry;
import org.metalib.papifly.fx.code.lexer.PlainTextLexer;
import org.metalib.papifly.fx.code.lexer.TokenMap;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingsStorage;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageEditorSettingsResolverTest {

    @Test
    void usesLanguageDefaultsWhenStorageHasNoOverrides() {
        LanguageEditorSettings settings = new LanguageEditorSettingsResolver(null, registryWithTwoSpaceLanguage())
            .resolve("two-space");

        assertEquals("two-space", settings.languageId());
        assertEquals(2, settings.indentWidth());
        assertTrue(settings.insertSpaces());
        assertTrue(settings.ensureTrailingNewline());
        assertTrue(settings.trimTrailingWhitespace());
    }

    @Test
    void defaultSettingsOverrideProviderDefaults() {
        StubStorage storage = new StubStorage();
        storage.putInt(SettingScope.APPLICATION, "editor.language.default.indentWidth", 3);
        storage.putBoolean(SettingScope.APPLICATION, "editor.language.default.insertSpaces", false);

        LanguageEditorSettings settings = new LanguageEditorSettingsResolver(storage, registryWithTwoSpaceLanguage())
            .resolve("two-space");

        assertEquals(3, settings.indentWidth());
        assertFalse(settings.insertSpaces());
        assertTrue(settings.ensureTrailingNewline());
    }

    @Test
    void languageSpecificSettingsOverrideDefaultSettingsAcrossScopes() {
        StubStorage storage = new StubStorage();
        storage.putInt(SettingScope.APPLICATION, "editor.language.default.indentWidth", 3);
        storage.putInt(SettingScope.WORKSPACE, "editor.language.two-space.indentWidth", 6);
        storage.putBoolean(SettingScope.SESSION, "editor.language.two-space.trimTrailingWhitespace", false);

        LanguageEditorSettings settings = new LanguageEditorSettingsResolver(storage, registryWithTwoSpaceLanguage())
            .resolve("two-space");

        assertEquals(6, settings.indentWidth());
        assertFalse(settings.trimTrailingWhitespace());
    }

    @Test
    void invalidIndentWidthFallsBackToProviderDefault() {
        StubStorage storage = new StubStorage();
        storage.putString(SettingScope.APPLICATION, "editor.language.two-space.indentWidth", "wide");

        LanguageEditorSettings settings = new LanguageEditorSettingsResolver(storage, registryWithTwoSpaceLanguage())
            .resolve("two-space");

        assertEquals(2, settings.indentWidth());
    }

    private static LanguageSupportRegistry registryWithTwoSpaceLanguage() {
        LanguageSupportRegistry registry = new LanguageSupportRegistry();
        registry.register(new LanguageSupport(
            "two-space",
            "Two Space",
            java.util.Set.of(),
            java.util.Set.of("two"),
            java.util.Set.of(),
            PlainTextLexer::new,
            StubFoldProvider::new,
            LanguageEditorDefaults.spaces(2)
        ), ConflictPolicy.REPLACE_EXISTING);
        return registry;
    }

    private static final class StubFoldProvider implements FoldProvider {
        @Override
        public String languageId() {
            return "two-space";
        }

        @Override
        public FoldMap recompute(
            java.util.List<String> lines,
            TokenMap tokenMap,
            FoldMap baseline,
            int dirtyStartLine,
            java.util.function.BooleanSupplier cancelled
        ) {
            return FoldMap.empty();
        }
    }

    private static final class StubStorage implements SettingsStorage {
        private final Map<SettingScope, Map<String, Object>> values = new EnumMap<>(SettingScope.class);

        private StubStorage() {
            for (SettingScope scope : SettingScope.values()) {
                values.put(scope, new LinkedHashMap<>());
            }
        }

        @Override
        public String getString(SettingScope scope, String key, String defaultValue) {
            return getRaw(scope, key).orElse(defaultValue);
        }

        @Override
        public boolean getBoolean(SettingScope scope, String key, boolean defaultValue) {
            return getRaw(scope, key).map(Boolean::parseBoolean).orElse(defaultValue);
        }

        @Override
        public int getInt(SettingScope scope, String key, int defaultValue) {
            try {
                return getRaw(scope, key).map(Integer::parseInt).orElse(defaultValue);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }

        @Override
        public double getDouble(SettingScope scope, String key, double defaultValue) {
            return getRaw(scope, key).map(Double::parseDouble).orElse(defaultValue);
        }

        @Override
        public Optional<String> getRaw(SettingScope scope, String key) {
            return Optional.ofNullable(values.get(scope).get(key)).map(String::valueOf);
        }

        @Override
        public void putString(SettingScope scope, String key, String value) {
            values.get(scope).put(key, value);
        }

        @Override
        public void putBoolean(SettingScope scope, String key, boolean value) {
            values.get(scope).put(key, value);
        }

        @Override
        public void putInt(SettingScope scope, String key, int value) {
            values.get(scope).put(key, value);
        }

        @Override
        public void putDouble(SettingScope scope, String key, double value) {
            values.get(scope).put(key, value);
        }

        @Override
        public Map<String, Object> getMap(SettingScope scope, String key) {
            return Map.of();
        }

        @Override
        public void putMap(SettingScope scope, String key, Map<String, Object> value) {
            values.get(scope).put(key, value);
        }

        @Override
        public void save() {
        }

        @Override
        public void reload() {
        }
    }
}
