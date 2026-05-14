package org.metalib.papifly.fx.code.theme;

import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.code.language.ConflictPolicy;
import org.metalib.papifly.fx.code.language.RegistryDiagnostic;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry for semantic syntax style scopes contributed by language packs.
 * <p>
 * The default registry loads {@link SyntaxStyleProvider} implementations from
 * {@link ServiceLoader}. Duplicate scope ids are rejected by default and are
 * recorded as diagnostics so a broken language pack cannot prevent the editor
 * from starting.
 */
public final class SyntaxStyleRegistry {

    private static final SyntaxStyleRegistry DEFAULT = new SyntaxStyleRegistry();

    static {
        DEFAULT.bootstrap(true, ConflictPolicy.REJECT_ON_CONFLICT);
    }

    private final ConcurrentMap<String, SyntaxStyleScope> scopesById = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<RegistryDiagnostic> diagnostics = new CopyOnWriteArrayList<>();

    /**
     * Returns the process-wide syntax style registry.
     *
     * @return default registry
     */
    public static SyntaxStyleRegistry defaultRegistry() {
        return DEFAULT;
    }

    /**
     * Normalizes a syntax scope id.
     *
     * @param id candidate scope id
     * @return trimmed, lowercase id, or an empty string when absent
     */
    public static String normalizeScopeId(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Clears and reloads this registry.
     *
     * @param loadServiceProviders whether to read {@link ServiceLoader} providers
     * @param policy duplicate handling policy
     */
    public synchronized void bootstrap(boolean loadServiceProviders, ConflictPolicy policy) {
        scopesById.clear();
        diagnostics.clear();
        if (loadServiceProviders) {
            refreshServiceProviders(Thread.currentThread().getContextClassLoader(), policy);
        }
    }

    /**
     * Loads syntax style providers from the supplied class loader.
     *
     * @param loader class loader to inspect
     * @param policy duplicate handling policy
     */
    public synchronized void refreshServiceProviders(ClassLoader loader, ConflictPolicy policy) {
        ClassLoader resolvedLoader = loader == null
            ? Thread.currentThread().getContextClassLoader()
            : loader;
        ServiceLoader<SyntaxStyleProvider> serviceLoader =
            ServiceLoader.load(SyntaxStyleProvider.class, resolvedLoader);
        for (SyntaxStyleProvider provider : serviceLoader) {
            try {
                registerAll(provider.getSyntaxStyleScopes(), policy);
            } catch (RuntimeException ex) {
                publishDiagnostic(new RegistryDiagnostic(
                    null, provider.getClass().getName(), "Failed to load syntax style scopes", ex));
            }
        }
    }

    /**
     * Registers a syntax style scope.
     *
     * @param scope scope descriptor
     * @param policy duplicate handling policy
     */
    public synchronized void register(SyntaxStyleScope scope, ConflictPolicy policy) {
        String id = normalizeScopeId(scope.id());
        SyntaxStyleScope existing = scopesById.get(id);
        if (existing != null && policy == ConflictPolicy.REJECT_ON_CONFLICT) {
            throw new IllegalStateException(
                "Syntax style scope '" + id + "' is already registered. Use REPLACE_EXISTING to override.");
        }
        scopesById.put(id, scope);
    }

    /**
     * Registers all supplied syntax style scopes.
     *
     * @param scopes scopes to register
     * @param policy duplicate handling policy
     */
    public synchronized void registerAll(Collection<SyntaxStyleScope> scopes, ConflictPolicy policy) {
        if (scopes == null) {
            return;
        }
        for (SyntaxStyleScope scope : scopes) {
            if (scope != null) {
                register(scope, policy);
            }
        }
    }

    /**
     * Finds a registered scope.
     *
     * @param id scope id
     * @return scope descriptor when registered
     */
    public Optional<SyntaxStyleScope> scope(String id) {
        return Optional.ofNullable(scopesById.get(normalizeScopeId(id)));
    }

    /**
     * Returns an immutable snapshot of registered scopes.
     *
     * @return registered scopes
     */
    public Collection<SyntaxStyleScope> scopesSnapshot() {
        return List.copyOf(scopesById.values());
    }

    /**
     * Returns the default color for a scope in the requested palette.
     *
     * @param id scope id
     * @param darkPalette true for dark palette defaults
     * @return default color when registered
     */
    public Optional<Paint> defaultColor(String id, boolean darkPalette) {
        return scope(id).map(scope -> darkPalette ? scope.darkDefault() : scope.lightDefault());
    }

    /**
     * Builds a scope-id to default-color map for a palette.
     *
     * @param darkPalette true for dark palette defaults
     * @return immutable default color map
     */
    public Map<String, Paint> defaultColors(boolean darkPalette) {
        Map<String, Paint> colors = new LinkedHashMap<>();
        for (SyntaxStyleScope scope : scopesSnapshot()) {
            colors.put(scope.id(), darkPalette ? scope.darkDefault() : scope.lightDefault());
        }
        return Map.copyOf(colors);
    }

    /**
     * Returns diagnostics captured during ServiceLoader discovery.
     *
     * @return immutable diagnostics snapshot
     */
    public List<RegistryDiagnostic> diagnosticsSnapshot() {
        return List.copyOf(diagnostics);
    }

    private void publishDiagnostic(RegistryDiagnostic diagnostic) {
        diagnostics.add(diagnostic);
    }
}
