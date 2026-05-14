package org.metalib.papifly.fx.code.settings;

import javafx.beans.value.ChangeListener;
import org.metalib.papifly.fx.code.api.CodeEditor;
import org.metalib.papifly.fx.settings.api.SettingScope;
import org.metalib.papifly.fx.settings.api.SettingsStorage;

import java.util.Map;
import java.util.WeakHashMap;

public final class EditorSettingsSupport {

    private static final Map<CodeEditor, ChangeListener<String>> LANGUAGE_LISTENERS = new WeakHashMap<>();

    private EditorSettingsSupport() {
    }

    public static void applyDefaults(CodeEditor editor) {
        SettingsStorage storage = locateStorage();
        if (storage == null) {
            return;
        }
        applyDefaults(editor, storage);
    }

    static void applyDefaults(CodeEditor editor, SettingsStorage storage) {
        if (editor == null || storage == null) {
            return;
        }
        editor.setWordWrap(storage.getBoolean(SettingScope.APPLICATION, "editor.wordWrap", false));
        editor.setAutoDetectLanguage(storage.getBoolean(SettingScope.APPLICATION, "editor.autoDetectLanguage", false));
        applyLanguageDefaults(editor, storage);
        synchronized (LANGUAGE_LISTENERS) {
            ChangeListener<String> existing = LANGUAGE_LISTENERS.remove(editor);
            if (existing != null) {
                editor.languageIdProperty().removeListener(existing);
            }
            ChangeListener<String> listener = (obs, oldValue, newValue) -> applyLanguageDefaults(editor, storage);
            LANGUAGE_LISTENERS.put(editor, listener);
            editor.languageIdProperty().addListener(listener);
        }
    }

    private static void applyLanguageDefaults(CodeEditor editor, SettingsStorage storage) {
        LanguageEditorSettings settings = new LanguageEditorSettingsResolver(storage, null)
            .resolve(editor.getLanguageId());
        editor.setIndentWidth(settings.indentWidth());
        editor.setInsertSpaces(settings.insertSpaces());
        editor.setEnsureTrailingNewline(settings.ensureTrailingNewline());
        editor.setTrimTrailingWhitespace(settings.trimTrailingWhitespace());
    }

    private static SettingsStorage locateStorage() {
        try {
            Class<?> runtimeClass = Class.forName("org.metalib.papifly.fx.settings.runtime.SettingsRuntime");
            Object value = runtimeClass.getMethod("defaultStorage").invoke(null);
            if (value instanceof SettingsStorage storage) {
                return storage;
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
        return null;
    }
}
