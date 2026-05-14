# Language Plugin Implementation Progress

## Phase 0 - Baseline and branch safety
- [x] Baseline tests confirmed: 380 tests, 0 failures

## Phase 1 - Add core language package
- [x] Create `LanguageSupport` record
- [x] Create `LanguageSupportProvider` interface
- [x] Create `ConflictPolicy` enum
- [x] Create `BootstrapOptions` record
- [x] Create `UserFileAssociationMapping` interface
- [x] Create `LanguageRegistryListener` interface
- [x] Create `RegistryDiagnostic` record
- [x] Create `LanguageSupportRegistry`
- [x] Add unit tests (`LanguageSupportRegistryTest` - 16 tests)

## Phase 2 - Move built-ins into provider model
- [x] Create `BuiltInLanguageSupportProvider`
- [x] Remove `LexerRegistry`
- [x] Remove `FoldProviderRegistry`

## Phase 3 - Wire pipelines to LanguageSupportRegistry
- [x] Update `IncrementalLexerPipeline`
- [x] Update `IncrementalFoldingPipeline`
- [x] Verify pipeline tests pass

## Phase 4 - CodeEditor integration and UX polish
- [x] Add `autoDetectLanguage` property and `detectLanguageFromFilePath()` API
- [x] Auto-detect on `setFilePath(...)` when enabled
- [x] User file association override support via `UserFileAssociationMapping`

## Phase 5 - SPI and host boot profiles
- [x] Add ServiceLoader support in `bootstrap()`
- [x] Add `refreshServiceProviders()` for runtime hot-load
- [x] Add test-only `TestLanguageSupportProvider`
- [x] Add `META-INF/services` registration for test provider
- [x] Add `LanguageSupportBootstrapTest` (6 tests)
- [x] Add `LanguageSupportServiceLoaderTest` (5 tests)

## Phase 6 - Compatibility-relaxed cleanup
- [x] Remove `LexerRegistry.java`
- [x] Remove `FoldProviderRegistry.java`
- [x] Update all internal references to new registry
- [x] Final test pass: 412 tests, 0 failures

## Summary
- All 6 phases completed
- 32 new tests added (380 → 412 total)
- Zero regressions
