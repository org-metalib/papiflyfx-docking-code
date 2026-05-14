package org.metalib.papifly.fx.code.folding;

import org.metalib.papifly.fx.code.language.LanguageEditorDefaults;
import org.metalib.papifly.fx.code.language.LanguageSupport;
import org.metalib.papifly.fx.code.language.LanguageSupportProvider;
import org.metalib.papifly.fx.code.lexer.JavaScriptLexer;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class JavaScriptLanguageSupportProvider implements LanguageSupportProvider {

    @Override
    public Collection<LanguageSupport> getLanguageSupports() {
        return List.of(new LanguageSupport(
            JavaScriptLexer.LANGUAGE_ID, "JavaScript",
            Set.of("js"), Set.of("js", "mjs", "cjs"),
            Set.of(),
            JavaScriptLexer::new, JavaScriptFoldProvider::new,
            LanguageEditorDefaults.spaces(4)
        ));
    }
}
