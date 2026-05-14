package org.metalib.papifly.fx.code.folding;

import org.metalib.papifly.fx.code.language.LanguageEditorDefaults;
import org.metalib.papifly.fx.code.language.LanguageSupport;
import org.metalib.papifly.fx.code.language.LanguageSupportProvider;
import org.metalib.papifly.fx.code.lexer.JsonLexer;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class JsonLanguageSupportProvider implements LanguageSupportProvider {

    @Override
    public Collection<LanguageSupport> getLanguageSupports() {
        return List.of(new LanguageSupport(
            JsonLexer.LANGUAGE_ID, "JSON",
            Set.of(), Set.of("json"),
            Set.of(JsonLexer.SCOPE_JSON_KEY),
            JsonLexer::new, JsonFoldProvider::new,
            LanguageEditorDefaults.spaces(2)
        ));
    }
}
