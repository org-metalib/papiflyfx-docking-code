# papiflyfx-docking-code folding/language plugin implementation plan

## 1) Problem statement

`papiflyfx-docking-code` already has strong incremental lexing and folding pipelines, but language support is still closed over hardcoded registries:

- `lexer/LexerRegistry` (immutable map)
- `folding/FoldProviderRegistry` (immutable map)

This limits document navigation quality because only built-in languages can provide language-aware folds.  
Goal: implement a plugin-ready language/folding architecture so hosts and external modules can add or replace language definitions (lexer + fold provider) at runtime, including boot-time profiles that can omit built-ins entirely.

---

## 2) Desired outcomes

1. Add a unified language definition model (`LanguageSupport`) that binds:
   - language ID,
   - aliases/extensions,
   - lexer factory,
   - fold provider factory.
2. Replace static registry lookups in pipelines with a single registry API.
3. Support boot-time policies:
   - include built-ins,
   - exclude built-ins,
   - load providers via `ServiceLoader`,
   - resolve conflicts via explicit policy.
4. Keep runtime safety:
   - unknown language -> plain-text lexer + empty folding fallback.
5. Improve navigation UX by enabling richer folding for additional languages without touching core editor code.
6. Intentionally relax compatibility constraints where legacy APIs duplicate behavior.
7. Support dynamic plugin lifecycle events (runtime register/refresh/unregister) so active editors can react without restart.
8. Support optional user-defined file associations that override static extension mappings.
9. Provide host-visible diagnostics for provider load failures and runtime factory failures.
10. Publish naming guidance for third-party language IDs (reverse-DNS recommendation) to reduce accidental collisions.
11. Keep future theming evolution unblocked by carrying optional token-scope metadata in `LanguageSupport` now.

---

## 3) Scope and non-goals

## In scope

- New language plugin infrastructure in `papiflyfx-docking-code`.
- Pipeline integration (`IncrementalLexerPipeline`, `IncrementalFoldingPipeline`).
- Optional `CodeEditor` conveniences for extension-based language detection.
- Built-in language definitions moved out of hardcoded static maps.
- Tests and docs for plugin registration and boot profiles.
- Registry listener and diagnostics surfaces for runtime plugin lifecycle.
- Optional user file-association override service used by language detection.
- Documentation for third-party language ID naming convention.
- Forward-compatible token-scope metadata (non-rendered in this phase).

## Out of scope

- Replacing `TokenType`/rendering model with semantic token scopes in this phase (metadata only now, renderer changes later).
- AST-level parser plugins.
- Reworking existing folding algorithms for built-in languages (except packaging/integration changes).

---

## 4) Current state summary (implementation anchors)

The implementation work should be anchored to these existing classes:

- `api/CodeEditor.java`
  - wires `IncrementalLexerPipeline` + `IncrementalFoldingPipeline`,
  - propagates `languageId` changes to both pipelines.
- `lexer/IncrementalLexerPipeline.java`
  - currently defaults to `LexerRegistry::resolve`,
  - normalizes via `LexerRegistry.normalizeLanguageId(...)`.
- `folding/IncrementalFoldingPipeline.java`
  - currently defaults to `FoldProviderRegistry::resolve`,
  - normalizes via `FoldProviderRegistry.normalizeLanguageId(...)`.
- `lexer/LexerRegistry.java`, `folding/FoldProviderRegistry.java`
  - static immutable maps and aliases,
  - no runtime registration.
- `state/EditorStateData.java`, `state/EditorStateCodec.java`
  - persist language ID as string (already useful for plugin IDs).

---

## 5) Target architecture

## 5.1 New package: `org.metalib.papifly.fx.code.language`

Add the following core types.

### `LanguageSupport`

```java
package org.metalib.papifly.fx.code.language;

import org.metalib.papifly.fx.code.folding.FoldProvider;
import org.metalib.papifly.fx.code.lexer.Lexer;

import java.util.Set;
import java.util.function.Supplier;

public record LanguageSupport(
    String id,
    String displayName,
    Set<String> aliases,
    Set<String> fileExtensions,
    Set<String> customTokenScopes,
    Supplier<Lexer> lexerFactory,
    Supplier<FoldProvider> foldProviderFactory
) {}
```

ID guidance:
- Built-ins keep short IDs (`java`, `json`, ...).
- Third-party providers should prefer reverse-DNS IDs (`com.example.python`) and expose short names only as aliases.

### `LanguageSupportProvider` (SPI)

```java
package org.metalib.papifly.fx.code.language;

import java.util.Collection;

public interface LanguageSupportProvider {
    Collection<LanguageSupport> getLanguageSupports();
}
```

### `UserFileAssociationMapping` (optional host override)

```java
package org.metalib.papifly.fx.code.language;

import java.util.Optional;

public interface UserFileAssociationMapping {
    Optional<String> resolveLanguageId(String fileNameOrPath);
}
```

### `LanguageRegistryListener` and diagnostics events

```java
package org.metalib.papifly.fx.code.language;

public interface LanguageRegistryListener {
    void onLanguageRegistered(String languageId);
    void onLanguageReplaced(String languageId);
    void onLanguageUnregistered(String languageId);
    void onDiagnostic(RegistryDiagnostic diagnostic);
}
```

```java
package org.metalib.papifly.fx.code.language;

public record RegistryDiagnostic(
    String languageId,
    String sourceProvider,
    String message,
    Throwable cause
) {}
```

### `ConflictPolicy` and `BootstrapOptions`

```java
package org.metalib.papifly.fx.code.language;

public enum ConflictPolicy {
    REJECT_ON_CONFLICT,
    REPLACE_EXISTING
}
```

```java
package org.metalib.papifly.fx.code.language;

public record BootstrapOptions(
    boolean includeBuiltIns,
    boolean loadServiceProviders,
    ConflictPolicy conflictPolicy
) {
    public static BootstrapOptions defaults() {
        return new BootstrapOptions(true, true, ConflictPolicy.REPLACE_EXISTING);
    }
}
```

### `LanguageSupportRegistry`

```java
package org.metalib.papifly.fx.code.language;

import org.metalib.papifly.fx.code.folding.FoldMap;
import org.metalib.papifly.fx.code.folding.FoldProvider;
import org.metalib.papifly.fx.code.lexer.Lexer;
import org.metalib.papifly.fx.code.lexer.PlainTextLexer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class LanguageSupportRegistry {
    private static final LanguageSupportRegistry DEFAULT = new LanguageSupportRegistry();

    private static final Lexer PLAIN_FALLBACK_LEXER = new PlainTextLexer();
    private static final FoldProvider PLAIN_FALLBACK_FOLD = new FoldProvider() {
        @Override public String languageId() { return PlainTextLexer.LANGUAGE_ID; }
        @Override public FoldMap recompute(
            List<String> lines, org.metalib.papifly.fx.code.lexer.TokenMap tokenMap,
            FoldMap baseline, int dirtyStartLine, java.util.function.BooleanSupplier cancelled
        ) { return FoldMap.empty(); }
    };

    private final ConcurrentMap<String, LanguageSupport> supportsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> aliasToId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> extensionToId = new ConcurrentHashMap<>();

    public static LanguageSupportRegistry defaultRegistry() {
        return DEFAULT;
    }

    public synchronized void bootstrap(BootstrapOptions options) { /* see phases */ }
    public synchronized void refreshServiceProviders(ClassLoader loader, ConflictPolicy policy) { /* runtime refresh */ }
    public synchronized void register(LanguageSupport support, ConflictPolicy policy) { /* ... */ }
    public synchronized void registerAll(Collection<LanguageSupport> supports, ConflictPolicy policy) { /* ... */ }
    public synchronized void unregister(String id) { /* ... */ }
    public void addListener(LanguageRegistryListener listener) { /* ... */ }
    public void removeListener(LanguageRegistryListener listener) { /* ... */ }
    public void setUserFileAssociationMapping(UserFileAssociationMapping mapping) { /* optional */ }
    public List<RegistryDiagnostic> diagnosticsSnapshot() { /* immutable snapshot */ }

    public Lexer resolveLexer(String languageId) { /* fallback to plain */ }
    public FoldProvider resolveFoldProvider(String languageId) { /* fallback to plain */ }
    public Optional<String> detectLanguageId(String fileNameOrPath) { /* user mapping first, then extension lookup */ }
    public String normalizeLanguageId(String id) { /* trim + lower + default plain-text */ }
}
```

Implementation notes:
- `resolve*` must never throw for unknown IDs and must always return functional fallback instances.
- Listener notifications should be emitted for register/replace/unregister so host services can re-evaluate active editors.
- Provider/factory failures should emit `RegistryDiagnostic` entries and continue with fallback behavior.

---

## 5.2 Built-ins become provider-based

Instead of static maps in registries, create a built-in provider that contributes existing definitions.

Recommended file: `folding/BuiltInLanguageSupportProvider.java` (public), so it can instantiate package-private fold providers.

```java
package org.metalib.papifly.fx.code.folding;

import org.metalib.papifly.fx.code.language.LanguageSupport;
import org.metalib.papifly.fx.code.language.LanguageSupportProvider;
import org.metalib.papifly.fx.code.lexer.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class BuiltInLanguageSupportProvider implements LanguageSupportProvider {
    @Override
    public Collection<LanguageSupport> getLanguageSupports() {
        return List.of(
            new LanguageSupport("plain-text", "Plain Text", Set.of("plain", "plaintext", "text", "txt"), Set.of("txt"),
                PlainTextLexer::new, PlainTextFoldProvider::new),
            new LanguageSupport("java", "Java", Set.of(), Set.of("java"),
                JavaLexer::new, JavaFoldProvider::new),
            new LanguageSupport("javascript", "JavaScript", Set.of("js"), Set.of("js", "mjs", "cjs"),
                JavaScriptLexer::new, JavaScriptFoldProvider::new),
            new LanguageSupport("json", "JSON", Set.of(), Set.of("json"),
                JsonLexer::new, JsonFoldProvider::new),
            new LanguageSupport("markdown", "Markdown", Set.of("md"), Set.of("md", "markdown"),
                MarkdownLexer::new, MarkdownFoldProvider::new)
        );
    }
}
```

---

## 5.3 Pipeline integration (single resolver surface)

### `IncrementalLexerPipeline`

Replace default resolver wiring:

```java
public IncrementalLexerPipeline(Document document, Consumer<TokenMap> tokenMapConsumer) {
    this(
        document,
        tokenMapConsumer,
        IncrementalLexerPipeline::dispatchOnFxThread,
        DEFAULT_DEBOUNCE_MILLIS,
        languageId -> LanguageSupportRegistry.defaultRegistry().resolveLexer(languageId)
    );
}
```

And normalize through the new registry:

```java
public void setLanguageId(String languageId) {
    String normalized = LanguageSupportRegistry.defaultRegistry().normalizeLanguageId(languageId);
    this.languageId = normalized;
    long nextRevision = revision.incrementAndGet();
    enqueue(document.getText(), 0, nextRevision, normalized, true, debounceMillis);
}
```

### `IncrementalFoldingPipeline`

Mirror the same approach:

```java
public IncrementalFoldingPipeline(Document document, Supplier<TokenMap> tokenMapSupplier, Consumer<FoldMap> foldMapConsumer) {
    this(
        document,
        tokenMapSupplier,
        foldMapConsumer,
        IncrementalFoldingPipeline::dispatchOnFxThread,
        languageId -> LanguageSupportRegistry.defaultRegistry().resolveFoldProvider(languageId),
        DEFAULT_DEBOUNCE_MILLIS
    );
}
```

Result: lexing and folding cannot drift because both resolve through one registry.

---

## 5.4 `CodeEditor` navigation-oriented convenience APIs

Add optional extension detection without changing existing manual workflows.

```java
private final BooleanProperty autoDetectLanguage = new SimpleBooleanProperty(this, "autoDetectLanguage", false);

public boolean detectLanguageFromFilePath() {
    String path = getFilePath();
    if (path == null || path.isBlank()) {
        return false;
    }
    return LanguageSupportRegistry.defaultRegistry()
        .detectLanguageId(path)
        .map(id -> {
            setLanguageId(id);
            return true;
        })
        .orElse(false);
}

public void setFilePath(String filePath) {
    this.filePath.set(filePath == null ? "" : filePath);
    if (isAutoDetectLanguage()) {
        detectLanguageFromFilePath();
    }
}
```

This improves navigation for newly opened files because folding behavior aligns with file type immediately when enabled.

---

## 5.5 SPI discovery, runtime refresh, and plugin packaging

Registry bootstrap should support both built-ins and service discovery:

```java
public synchronized void bootstrap(BootstrapOptions options) {
    clear();
    if (options.includeBuiltIns()) {
        registerAll(new BuiltInLanguageSupportProvider().getLanguageSupports(), options.conflictPolicy());
    }
    if (options.loadServiceProviders()) {
        ServiceLoader<LanguageSupportProvider> loader = ServiceLoader.load(LanguageSupportProvider.class);
        for (LanguageSupportProvider provider : loader) {
            registerAll(provider.getLanguageSupports(), options.conflictPolicy());
        }
    }
}
```

Runtime host refresh path:

```java
public synchronized void refreshServiceProviders(ClassLoader loader, ConflictPolicy policy) {
    ServiceLoader<LanguageSupportProvider> serviceLoader =
        ServiceLoader.load(LanguageSupportProvider.class, loader);
    for (LanguageSupportProvider provider : serviceLoader) {
        try {
            registerAll(provider.getLanguageSupports(), policy);
        } catch (RuntimeException ex) {
            publishDiagnostic(new RegistryDiagnostic(null, provider.getClass().getName(),
                "Failed to load language supports", ex));
        }
    }
}
```

For plugin unload/disable flows, host code should call `unregister(id)` and react via listeners by downgrading affected editors to plain text.

External plugin JAR adds:

`META-INF/services/org.metalib.papifly.fx.code.language.LanguageSupportProvider`

```text
com.example.papiflyfx.code.python.PythonLanguageSupportProvider
```

---

## 6) Detailed phased implementation

## Phase 0 - Baseline and branch safety ✅ COMPLETED

1. Confirm baseline tests for code module:
   - `./mvnw -pl papiflyfx-docking-code -am test -Dtestfx.headless=true`
2. Capture baseline failures (if any) and do not expand scope.
3. Implement feature in small, mergeable slices.

Acceptance:
- Baseline understood before edits. (380 tests, 0 failures)

## Phase 1 - Add core language package ✅ COMPLETED

1. Create `language` package and add:
    - `LanguageSupport`,
    - `LanguageSupportProvider`,
    - `ConflictPolicy`,
    - `BootstrapOptions`,
    - `LanguageSupportRegistry`,
    - `UserFileAssociationMapping`,
    - `LanguageRegistryListener`,
    - `RegistryDiagnostic`.
2. Implement normalization and fallback behavior.
3. Implement conflict handling semantics:
    - `REJECT_ON_CONFLICT`: throw clear `IllegalStateException`.
    - `REPLACE_EXISTING`: replace previous binding atomically.
4. Implement extension detection.
5. Add reverse-DNS naming convention checks/documented validation for third-party IDs.
6. Emit listener and diagnostic events on register/replace/unregister and load/runtime failures.

Acceptance:
- Unit tests prove registration, replacement, alias handling, extension lookup, and fallback.
- Unit tests prove event emission and diagnostic capture.

## Phase 2 - Move built-ins into provider model ✅ COMPLETED

1. Add `folding/BuiltInLanguageSupportProvider`.
2. Remove hardcoded maps from `LexerRegistry` / `FoldProviderRegistry`.
3. Preferred compatibility-relaxed path: delete both registry classes after pipelines move.
4. Ensure `LanguageSupportRegistry.bootstrap(BootstrapOptions.defaults())` reproduces current built-in behavior.

Acceptance:
- Built-ins still available with default bootstrap.
- Built-ins omitted when `includeBuiltIns=false`.

## Phase 3 - Wire pipelines to `LanguageSupportRegistry` ✅ COMPLETED

1. Update `IncrementalLexerPipeline` default resolver and normalization.
2. Update `IncrementalFoldingPipeline` default resolver and normalization.
3. Keep constructor seams that accept resolver functions for deterministic tests.
4. Ensure exception fallback paths remain unchanged in behavior.

Acceptance:
- Existing pipeline tests pass with new resolver source.
- Unknown IDs still resolve to plain-text/empty fold map.

## Phase 4 - `CodeEditor` integration and UX polish ✅ COMPLETED

1. Add optional auto-detect property and API methods.
2. Keep `setLanguageId(...)` public API but normalize through registry for consistency.
3. Wire detection to consult optional `UserFileAssociationMapping` before extension lookup.
4. Ensure no regressions in state capture/apply (`languageId` persisted string remains unchanged in format policy).
5. Ensure `applyState(...)` behavior remains stable if plugin language is unavailable (fallback path).

Acceptance:
- New integration tests for:
  - `detectLanguageFromFilePath()`,
  - user association override precedence,
  - auto-detect on `setFilePath(...)`,
  - plugin-missing restore safety.

## Phase 5 - SPI and host boot profiles ✅ COMPLETED

1. Add startup helper docs/examples:
    - default profile (built-ins + SPI),
    - plugin-only profile (no built-ins),
    - conflict strict/permissive modes.
2. Add service-loading tests with test-only providers.
3. Add runtime refresh examples (`refreshServiceProviders(...)`) for hot-load hosts.
4. Add diagnostics integration examples for host UIs/status bars.
5. Add third-party naming convention guidance (reverse-DNS IDs + optional aliases).
6. Add a sample plugin provider in tests or samples module.

Acceptance:
- `ServiceLoader` providers are discovered and usable by `CodeEditor#setLanguageId(...)`.
- Runtime refresh/unregister behavior is observable through listener callbacks.

## Phase 6 - Compatibility-relaxed cleanup ✅ COMPLETED

1. Remove deprecated/duplicated language resolution APIs once migration is complete.
2. Update all in-repo call sites to new registry.
3. Document explicit breaking changes in module README/release notes.

Acceptance:
- No internal references remain to old registries.
- Public docs match final API.

---

## 7) File-by-file change plan

## New files (planned)

- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/language/LanguageSupport.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/language/LanguageSupportProvider.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/language/ConflictPolicy.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/language/BootstrapOptions.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/language/LanguageSupportRegistry.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/language/UserFileAssociationMapping.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/language/LanguageRegistryListener.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/language/RegistryDiagnostic.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/folding/BuiltInLanguageSupportProvider.java`

## Modified files (planned)

- `.../lexer/IncrementalLexerPipeline.java`
- `.../folding/IncrementalFoldingPipeline.java`
- `.../api/CodeEditor.java`
- `.../README.md` (module docs, plugin instructions)
- optionally `.../src/main/resources/META-INF/services/...` if an internal provider is published through SPI

## Removed files (compatibility-relaxed target)

- `.../lexer/LexerRegistry.java`
- `.../folding/FoldProviderRegistry.java`

---

## 8) Test strategy (detailed)

## 8.1 New unit tests

1. `language/LanguageSupportRegistryTest`
    - normalize null/blank IDs,
    - register + resolve lexer/fold provider,
    - alias collisions under both policies,
    - extension detection rules,
    - unknown language fallback,
    - listener callbacks for register/replace/unregister,
    - diagnostic event capture on provider/factory failures.
2. `language/LanguageSupportBootstrapTest`
    - built-ins included by default,
    - built-ins omitted with `includeBuiltIns=false`,
    - conflict replacement behavior,
    - reverse-DNS guidance validation for third-party IDs (if strict mode enabled).

## 8.2 ServiceLoader tests

1. `language/LanguageSupportServiceLoaderTest`
    - test-only provider discovery,
    - multiple providers loaded,
    - deterministic conflict handling,
    - runtime refresh path behavior.

Implementation approach:
- Add test resources under `src/test/resources/META-INF/services/...LanguageSupportProvider`.

## 8.3 Pipeline regression tests

1. Update `IncrementalLexerPipelineTest`
   - preserve async/revision behavior,
   - preserve fallback when plugin lexer throws.
2. Update `IncrementalFoldingPipelineTest`
   - preserve fold recompute behavior,
   - preserve collapse header synchronization.

## 8.4 Integration tests

1. Extend `CodeEditorIntegrationTest`:
    - detects language from file extension when enabled,
    - respects user association override before extension defaults,
    - keeps manual language assignment precedence when auto-detect disabled,
    - plugin-missing state restore falls back safely.

## 8.5 Full-module verification

Run:

```bash
./mvnw -pl papiflyfx-docking-code -am test -Dtestfx.headless=true
```

---

## 9) Breaking changes and migration guidance

This plan intentionally permits backward-incompatible cleanup.

Expected breaking changes:

1. Removal of `LexerRegistry` and `FoldProviderRegistry`.
2. Language resolution API centralized in `LanguageSupportRegistry`.
3. Potential normalization change if `CodeEditor#setLanguageId(...)` now stores canonical IDs.
4. Optional strict validation for third-party language IDs may reject non-conforming plugin IDs.

Migration guidance for host apps:

- Before constructing editors, bootstrap registry explicitly:

```java
LanguageSupportRegistry.defaultRegistry().bootstrap(
    new BootstrapOptions(true, true, ConflictPolicy.REPLACE_EXISTING)
);
```

- Plugin-only boot:

```java
LanguageSupportRegistry.defaultRegistry().bootstrap(
    new BootstrapOptions(false, true, ConflictPolicy.REJECT_ON_CONFLICT)
);
```

- If previously relying on internal registries, migrate to `LanguageSupportRegistry` registration APIs.
- For third-party plugins, prefer reverse-DNS `LanguageSupport.id` values and expose short aliases for ergonomics.
- Hosts that support hot plugin install/uninstall should use `refreshServiceProviders(...)`, `unregister(...)`, and registry listeners.
- Hosts that surface plugin health should consume `diagnosticsSnapshot()` or listener diagnostic events.

---

## 10) Operational checklist for implementation execution

1. Implement Phase 1 and add registry tests first.
2. Implement Phase 2 and ensure built-ins still work through new registry.
3. Implement Phase 3 (pipeline migration) and rerun pipeline tests.
4. Implement Phase 4 (`CodeEditor` detection APIs) and add integration tests.
5. Implement Phase 5 SPI tests and documentation.
6. Remove legacy registries and dead code paths (Phase 6).
7. Run full module tests headless and fix only regressions introduced by this feature.
8. Validate dynamic refresh/unregister + diagnostics flows with focused tests.
9. Update spec docs with final API examples after code lands.

---

## 11) Definition of done

Feature is complete when all are true:

1. New language/folding definitions can be registered at runtime without core code edits.
2. Built-ins can be excluded at boot and replaced by plugins/host definitions.
3. `CodeEditor` can optionally detect language from file path and fold accordingly.
4. Unknown/missing plugin languages degrade safely to plain text behavior.
5. Runtime register/refresh/unregister workflows are observable and safe via registry listener callbacks.
6. User-defined file associations can override extension defaults when configured.
7. Plugin load/runtime failures are exposed through diagnostics surfaces (not only logs).
8. All relevant tests pass for `papiflyfx-docking-code` in headless mode.
