package org.metalib.papifly.fx.code.theme;

import java.util.Collection;

/**
 * ServiceLoader SPI for language packs that contribute syntax style scopes.
 * <p>
 * Implementations should be registered in
 * {@code META-INF/services/org.metalib.papifly.fx.code.theme.SyntaxStyleProvider}.
 * Each returned scope id must be stable because user settings and themes may
 * refer to it directly.
 */
public interface SyntaxStyleProvider {

    /**
     * Returns the syntax style scopes owned by this provider.
     *
     * @return contributed scopes; never mutate after returning
     */
    Collection<SyntaxStyleScope> getSyntaxStyleScopes();
}
