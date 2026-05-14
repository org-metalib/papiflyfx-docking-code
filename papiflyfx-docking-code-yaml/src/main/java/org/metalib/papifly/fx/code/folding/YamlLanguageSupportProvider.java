package org.metalib.papifly.fx.code.folding;

import org.metalib.papifly.fx.code.language.LanguageEditorDefaults;
import org.metalib.papifly.fx.code.language.LanguageSupport;
import org.metalib.papifly.fx.code.language.LanguageSupportProvider;
import org.metalib.papifly.fx.code.lexer.YamlLexer;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class YamlLanguageSupportProvider implements LanguageSupportProvider {

    @Override
    public Collection<LanguageSupport> getLanguageSupports() {
        return List.of(new LanguageSupport(
            YamlLexer.LANGUAGE_ID, "YAML",
            Set.of("yml"), Set.of("yaml", "yml"),
            Set.of(
                YamlLexer.SCOPE_YAML_KEY,
                YamlLexer.SCOPE_YAML_ANCHOR,
                YamlLexer.SCOPE_YAML_ALIAS,
                YamlLexer.SCOPE_YAML_TAG
            ),
            YamlLexer::new, YamlFoldProvider::new,
            LanguageEditorDefaults.spaces(2)
        ));
    }
}
