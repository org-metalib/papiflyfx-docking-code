package org.metalib.papifly.fx.code.language;

import org.metalib.papifly.fx.code.folding.BuiltInLanguageSupportProvider;
import org.metalib.papifly.fx.code.folding.FoldMap;
import org.metalib.papifly.fx.code.folding.FoldProvider;
import org.metalib.papifly.fx.code.lexer.Lexer;
import org.metalib.papifly.fx.code.lexer.PlainTextLexer;
import org.metalib.papifly.fx.code.lexer.TokenMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

public final class LanguageSupportRegistry {

    private static final LanguageSupportRegistry DEFAULT = new LanguageSupportRegistry();

    static {
        DEFAULT.bootstrap(BootstrapOptions.defaults());
    }

    private final ConcurrentMap<String, LanguageSupport> supportsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> aliasToId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> extensionToId = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<LanguageRegistryListener> listeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<RegistryDiagnostic> diagnostics = new CopyOnWriteArrayList<>();
    private volatile UserFileAssociationMapping userFileAssociationMapping;

    public static LanguageSupportRegistry defaultRegistry() {
        return DEFAULT;
    }

    public synchronized void bootstrap(BootstrapOptions options) {
        clear();
        if (options.includeBuiltIns()) {
            registerAll(new BuiltInLanguageSupportProvider().getLanguageSupports(), options.conflictPolicy());
        }
        if (options.loadServiceProviders()) {
            ServiceLoader<LanguageSupportProvider> loader = ServiceLoader.load(LanguageSupportProvider.class);
            for (LanguageSupportProvider provider : loader) {
                try {
                    registerAll(provider.getLanguageSupports(), options.conflictPolicy());
                } catch (RuntimeException ex) {
                    publishDiagnostic(new RegistryDiagnostic(
                        null, provider.getClass().getName(), "Failed to load language supports", ex));
                }
            }
        }
    }

    public synchronized void refreshServiceProviders(ClassLoader loader, ConflictPolicy policy) {
        ServiceLoader<LanguageSupportProvider> serviceLoader =
            ServiceLoader.load(LanguageSupportProvider.class, loader);
        for (LanguageSupportProvider provider : serviceLoader) {
            try {
                registerAll(provider.getLanguageSupports(), policy);
            } catch (RuntimeException ex) {
                publishDiagnostic(new RegistryDiagnostic(
                    null, provider.getClass().getName(), "Failed to load language supports", ex));
            }
        }
    }

    public synchronized void register(LanguageSupport support, ConflictPolicy policy) {
        String id = normalizeLanguageId(support.id());
        LanguageSupport existing = supportsById.get(id);
        if (existing != null) {
            if (policy == ConflictPolicy.REJECT_ON_CONFLICT) {
                throw new IllegalStateException(
                    "Language ID '" + id + "' is already registered. Use REPLACE_EXISTING to override.");
            }
            unregisterMappings(existing);
            supportsById.put(id, support);
            registerMappings(support, id, policy);
            notifyReplaced(id);
            return;
        }
        supportsById.put(id, support);
        registerMappings(support, id, policy);
        notifyRegistered(id);
    }

    public synchronized void registerAll(Collection<LanguageSupport> supports, ConflictPolicy policy) {
        for (LanguageSupport support : supports) {
            register(support, policy);
        }
    }

    public synchronized void unregister(String id) {
        String normalizedId = normalizeLanguageId(id);
        LanguageSupport removed = supportsById.remove(normalizedId);
        if (removed == null) {
            return;
        }
        unregisterMappings(removed);
        notifyUnregistered(normalizedId);
    }

    public void addListener(LanguageRegistryListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(LanguageRegistryListener listener) {
        listeners.remove(listener);
    }

    public void setUserFileAssociationMapping(UserFileAssociationMapping mapping) {
        this.userFileAssociationMapping = mapping;
    }

    public List<RegistryDiagnostic> diagnosticsSnapshot() {
        return List.copyOf(diagnostics);
    }

    public Lexer resolveLexer(String languageId) {
        String resolved = resolveId(languageId);
        LanguageSupport support = supportsById.get(resolved);
        if (support == null) {
            return new PlainTextLexer();
        }
        try {
            return support.lexerFactory().get();
        } catch (RuntimeException ex) {
            publishDiagnostic(new RegistryDiagnostic(
                resolved, support.getClass().getName(), "Lexer factory failed", ex));
            return new PlainTextLexer();
        }
    }

    public FoldProvider resolveFoldProvider(String languageId) {
        String resolved = resolveId(languageId);
        LanguageSupport support = supportsById.get(resolved);
        if (support == null) {
            return new PlainTextFallbackFoldProvider();
        }
        try {
            return support.foldProviderFactory().get();
        } catch (RuntimeException ex) {
            publishDiagnostic(new RegistryDiagnostic(
                resolved, support.getClass().getName(), "FoldProvider factory failed", ex));
            return new PlainTextFallbackFoldProvider();
        }
    }

    public Optional<String> detectLanguageId(String fileNameOrPath) {
        if (fileNameOrPath == null || fileNameOrPath.isBlank()) {
            return Optional.empty();
        }
        UserFileAssociationMapping mapping = this.userFileAssociationMapping;
        if (mapping != null) {
            Optional<String> userResult = mapping.resolveLanguageId(fileNameOrPath);
            if (userResult != null && userResult.isPresent()) {
                return userResult;
            }
        }
        String extension = extractExtension(fileNameOrPath);
        if (extension.isEmpty()) {
            return Optional.empty();
        }
        String id = extensionToId.get(extension);
        return Optional.ofNullable(id);
    }

    public String normalizeLanguageId(String id) {
        if (id == null || id.isBlank()) {
            return PlainTextLexer.LANGUAGE_ID;
        }
        return id.trim().toLowerCase(Locale.ROOT);
    }

    public Collection<LanguageSupport> registeredLanguages() {
        return List.copyOf(supportsById.values());
    }

    /**
     * Finds registered language metadata after applying id and alias resolution.
     *
     * @param languageId requested language id or alias
     * @return language support metadata when registered
     */
    public Optional<LanguageSupport> findLanguageSupport(String languageId) {
        return Optional.ofNullable(supportsById.get(resolveId(languageId)));
    }

    private String resolveId(String languageId) {
        String normalized = normalizeLanguageId(languageId);
        if (supportsById.containsKey(normalized)) {
            return normalized;
        }
        String aliasTarget = aliasToId.get(normalized);
        if (aliasTarget != null) {
            return aliasTarget;
        }
        return normalized;
    }

    private void registerMappings(LanguageSupport support, String id, ConflictPolicy policy) {
        for (String alias : support.aliases()) {
            String normalizedAlias = normalizeLanguageId(alias);
            if (normalizedAlias.equals(id)) {
                continue;
            }
            String existingId = aliasToId.get(normalizedAlias);
            if (existingId != null && !existingId.equals(id)) {
                if (policy == ConflictPolicy.REJECT_ON_CONFLICT) {
                    throw new IllegalStateException(
                        "Alias '" + normalizedAlias + "' conflicts: already maps to '" + existingId + "'");
                }
            }
            aliasToId.put(normalizedAlias, id);
        }
        for (String ext : support.fileExtensions()) {
            String normalizedExt = ext.trim().toLowerCase(Locale.ROOT);
            if (normalizedExt.isEmpty()) {
                continue;
            }
            String existingId = extensionToId.get(normalizedExt);
            if (existingId != null && !existingId.equals(id)) {
                if (policy == ConflictPolicy.REJECT_ON_CONFLICT) {
                    throw new IllegalStateException(
                        "Extension '" + normalizedExt + "' conflicts: already maps to '" + existingId + "'");
                }
            }
            extensionToId.put(normalizedExt, id);
        }
    }

    private void unregisterMappings(LanguageSupport support) {
        String id = normalizeLanguageId(support.id());
        aliasToId.entrySet().removeIf(entry -> entry.getValue().equals(id));
        extensionToId.entrySet().removeIf(entry -> entry.getValue().equals(id));
    }

    private synchronized void clear() {
        supportsById.clear();
        aliasToId.clear();
        extensionToId.clear();
        diagnostics.clear();
    }

    private void publishDiagnostic(RegistryDiagnostic diagnostic) {
        diagnostics.add(diagnostic);
        for (LanguageRegistryListener listener : listeners) {
            try {
                listener.onDiagnostic(diagnostic);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void notifyRegistered(String id) {
        for (LanguageRegistryListener listener : listeners) {
            try {
                listener.onLanguageRegistered(id);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void notifyReplaced(String id) {
        for (LanguageRegistryListener listener : listeners) {
            try {
                listener.onLanguageReplaced(id);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void notifyUnregistered(String id) {
        for (LanguageRegistryListener listener : listeners) {
            try {
                listener.onLanguageUnregistered(id);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static String extractExtension(String fileNameOrPath) {
        String name = fileNameOrPath;
        int lastSep = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (lastSep >= 0) {
            name = name.substring(lastSep + 1);
        }
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == name.length() - 1) {
            return "";
        }
        return name.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private static final class PlainTextFallbackFoldProvider implements FoldProvider {
        @Override
        public String languageId() {
            return PlainTextLexer.LANGUAGE_ID;
        }

        @Override
        public FoldMap recompute(
            java.util.List<String> lines,
            TokenMap tokenMap,
            FoldMap baseline,
            int dirtyStartLine,
            BooleanSupplier cancelled
        ) {
            return FoldMap.empty();
        }
    }
}
