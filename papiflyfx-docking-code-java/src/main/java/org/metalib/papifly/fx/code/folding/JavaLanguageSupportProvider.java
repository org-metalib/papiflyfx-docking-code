package org.metalib.papifly.fx.code.folding;

import org.metalib.papifly.fx.code.language.LanguageEditorDefaults;
import org.metalib.papifly.fx.code.language.LanguageSupport;
import org.metalib.papifly.fx.code.language.LanguageSupportProvider;
import org.metalib.papifly.fx.code.lexer.JavaLexer;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class JavaLanguageSupportProvider implements LanguageSupportProvider {

    @Override
    public Collection<LanguageSupport> getLanguageSupports() {
        return List.of(new LanguageSupport(
            JavaLexer.LANGUAGE_ID, "Java",
            Set.of(), Set.of("java"),
            Set.of(),
            JavaLexer::new, JavaFoldProvider::new,
            LanguageEditorDefaults.spaces(4)
        ));
    }
}
