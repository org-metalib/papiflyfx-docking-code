package org.metalib.papifly.fx.code.folding;

import org.metalib.papifly.fx.code.language.LanguageEditorDefaults;
import org.metalib.papifly.fx.code.language.LanguageSupport;
import org.metalib.papifly.fx.code.language.LanguageSupportProvider;
import org.metalib.papifly.fx.code.lexer.PlainTextLexer;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class BuiltInLanguageSupportProvider implements LanguageSupportProvider {

    @Override
    public Collection<LanguageSupport> getLanguageSupports() {
        return List.of(
            new LanguageSupport(
                "plain-text", "Plain Text",
                Set.of("plain", "plaintext", "text", "txt"), Set.of("txt"),
                Set.of(),
                PlainTextLexer::new, PlainTextFoldProvider::new,
                new LanguageEditorDefaults(4, false, false, false))
        );
    }
}
