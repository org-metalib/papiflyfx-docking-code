package org.metalib.papifly.fx.code.language;

public interface LanguageRegistryListener {
    void onLanguageRegistered(String languageId);
    void onLanguageReplaced(String languageId);
    void onLanguageUnregistered(String languageId);
    void onDiagnostic(RegistryDiagnostic diagnostic);
}
