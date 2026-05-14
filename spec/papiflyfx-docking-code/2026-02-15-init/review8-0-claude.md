# Review 8 (Claude-0): Phase 8 Implementation Plan — Hardening, Benchmarks, and Docs

Date: 2026-02-17
Author: Claude Opus 4.6
Target: `implementation.md` Phase 8 + `spec.md` §8 Acceptance Criteria

## 1. Objective

Phase 8 closes the MVP by:
1. Adding a **performance benchmark harness** that measures spec §8 acceptance metrics.
2. Adding **end-to-end FX integration tests** for docking ↔ editor state round-trip.
3. Producing **usage documentation** sufficient for host-app embedding.
4. Reporting measured metrics against spec thresholds.

## 2. Current State Assessment

### Already in place
- All Phases 0–7 complete, 204 tests passing (0 failures).
- One embedded perf guard: `DocumentTest.perEditPerformanceGuard_largeDocument()` validates <1ms per edit on 50k-line doc.
- Performance optimizations: incremental `LineIndex`, lazy lexer snapshot, `BitSet` dirty-region viewport.
- `papiflyfx-docking-code/README.md` exists but contains only a brief overview pointing to spec files.
- `CodeEditorFactory` and `CodeEditorStateAdapter` are functional; registration is manual (no `META-INF/services`).

### Gaps to fill
| Gap | Spec reference |
|-----|----------------|
| No large-file open/render benchmark (100k lines, ≤2.0s) | spec §8 criterion 1 |
| No typing latency benchmark (p95 ≤16ms) | spec §8 criterion 2 |
| No scroll rendering benchmark (p95 ≤16ms) | spec §8 criterion 3 |
| No memory overhead measurement (≤350MB for 100k lines) | spec §8 criterion 4 |
| No end-to-end docking integration test with editor *text content* through DockManager save/restore | spec §9, impl §5 |
| README lacks factory/adapter registration examples, quickstart, and integration guide | impl Phase 8 |
| No ServiceLoader descriptor for `ContentStateAdapter` auto-discovery | impl §4 Phase 0 (optional, now appropriate) |

## 3. Workstream Breakdown

### Workstream A: Performance Benchmark Harness

**Goal**: Create a dedicated benchmark test class that measures spec §8 acceptance metrics in a reproducible, headless-safe way.

**File**: `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/benchmark/CodeEditorBenchmarkTest.java`

**Test scenarios** (each a separate `@Test` method):

#### A1. Large file open and first render (spec §8.1)
- Generate a 100k-line synthetic Java file in-memory (alternating code patterns to exercise lexer).
- Measure wall-clock time from `new CodeEditor()` + `setText(text)` + `lexerPipeline` completion + first `viewport.layoutChildren()` call.
- Assert ≤ 2000ms.
- Use a `CountDownLatch` or similar to await async lexer completion before timing stops.

#### A2. Typing latency (spec §8.2)
- Open a 100k-line document.
- Wait for initial lex to complete (warm up).
- Simulate 100 single-character insert edits at the viewport's visible area.
- Measure per-edit time: `document.insert()` + `moveCaretToOffset()` + `viewport.layoutChildren()` (forced redraw).
- Compute p95 of the 100 measurements.
- Assert p95 ≤ 16ms.

#### A3. Scroll rendering (spec §8.3)
- Open a 100k-line document, wait for lex.
- Simulate 200 scroll-offset changes (incrementing by ~lineHeight * 5 each step).
- Measure per-scroll: `viewport.setScrollOffset()` + `viewport.layoutChildren()`.
- Compute p95.
- Assert p95 ≤ 16ms.

#### A4. Memory overhead (spec §8.4)
- Force GC, record baseline heap via `Runtime.getRuntime()`.
- Create a 100k-line document in `CodeEditor`, wait for lex.
- Force GC again, record used heap.
- Delta must be ≤ 350MB.
- Note: this is an approximate measurement; JVM heap variability is expected. Use a generous tolerance (e.g., assert ≤ 400MB as soft ceiling, log actual value, flag if >350MB).

**Implementation notes**:
- Use `@Tag("benchmark")` (JUnit 5) so benchmarks can be excluded from fast CI via Maven profile.
- Add a surefire `<excludedGroups>benchmark</excludedGroups>` to the default test execution in `papiflyfx-docking-code/pom.xml` so benchmarks run only when explicitly requested.
- Provide a Maven command to run benchmarks: `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true -Dgroups=benchmark test`
- Use the existing `callOnFx()` pattern from `CodeEditorIntegrationTest` for FX-thread synchronization.
- Generate the 100k-line test file via a helper method `generateLargeJavaFile(int lineCount)` that produces syntactically varied content (imports, class, methods with loops/strings/numbers).

### Workstream B: End-to-End Docking Integration Tests

**Goal**: Verify that editor text content and full state survive a complete DockManager session capture → restore cycle.

**File**: `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorDockingIntegrationTest.java`

**Test scenarios**:

#### B1. Full editor state round-trip through DockManager
- Create a `DockManager` with `ContentStateRegistry` containing `CodeEditorStateAdapter`.
- Register `CodeEditorFactory`.
- Programmatically create a leaf with a `CodeEditor`, set text content, language, cursor position, scroll offset.
- Call `dockManager.captureSession()`.
- Dispose the manager.
- Create a new `DockManager`, register same adapter/factory.
- Call `dockManager.restoreSession(data)`.
- Assert: restored editor has matching `filePath`, `languageId`, `cursorLine`, `cursorColumn`, `verticalScrollOffset`.
- Assert: restored editor text content is present (via file rehydration or adapter state — note: current adapter stores filePath, not raw text. If file doesn't exist on restore, document is empty but metadata is preserved. Test should use a temp file that exists.)

#### B2. Session restore with missing adapter falls back to factory
- Capture a session with `CodeEditor`.
- On restore, register `CodeEditorFactory` but **not** `CodeEditorStateAdapter`.
- Assert: leaf is created via factory (fresh editor), no crash.

#### B3. Session restore with neither adapter nor factory falls back to placeholder
- Capture, then restore with no adapter and no factory registered.
- Assert: leaf shows placeholder content, session structure intact.

#### B4. Multiple editor leaves in session
- Create 2+ editor leaves with different content/state.
- Capture and restore.
- Assert each leaf's state is independently preserved.

**Implementation notes**:
- Extend the FX test base used by `DockManagerSessionFxTest` for headless FX thread management.
- Use temporary files (`@TempDir`) for file-backed editors to test rehydration.

### Workstream C: Documentation

**Goal**: Update module README and add usage examples for host-app integration.

#### C1. Module README update
**File**: `papiflyfx-docking-code/README.md`

Contents to add:
1. **Overview**: What `papiflyfx-docking-code` provides (dockable code editor content type).
2. **Maven dependency**: Snippet for adding the module.
3. **Quick start**:
   - Registering `CodeEditorFactory` and `CodeEditorStateAdapter` with `DockManager`.
   - Creating an editor leaf programmatically.
   - Binding to docking theme.
4. **Factory and adapter registration example**:
   ```java
   DockManager dockManager = new DockManager();
   ContentStateRegistry registry = dockManager.getContentStateRegistry();
   registry.register("code-editor", new CodeEditorStateAdapter());
   dockManager.registerContentFactory(new CodeEditorFactory());
   ```
5. **Session persistence flow**: Brief explanation of save → `LeafContentData` → restore → adapter → factory → placeholder chain.
6. **Editor API highlights**: `setText()`, `setLanguageId()`, `bindThemeProperty()`, `captureState()`, `applyState()`, `getMarkerModel()`, `openSearch()`, `goToLine()`.
7. **Supported languages**: Java, JSON, JavaScript, Markdown, plain-text fallback.
8. **Acceptance metrics table**: Placeholder with columns for metric, threshold, measured value (to be filled after benchmarks run).

#### C2. ServiceLoader descriptor (optional but recommended)
**File**: `papiflyfx-docking-code/src/main/resources/META-INF/services/org.metalib.papifly.fx.docks.layout.ContentStateAdapter`

Contents:
```
org.metalib.papifly.fx.code.api.CodeEditorStateAdapter
```

This allows host apps to auto-discover the adapter via `ServiceLoader` instead of manual registration. Only add if the `ContentStateRegistry` already supports ServiceLoader loading — verify first. If not supported, skip this and document manual registration only.

### Workstream D: PROGRESS.md and Metrics Report

**Goal**: Record Phase 8 completion, measured benchmark results, and final test counts.

**Tasks**:
1. After implementing workstreams A–C, run the full test suite and record results.
2. Run benchmarks and record measured values.
3. Update `PROGRESS.md`:
   - Add Phase 8 update log entry with workstream details.
   - Update phase status table: Phase 8 → ✅ Complete.
   - Fill in measured acceptance metrics.
   - Update total test counts.
   - Add benchmark test file to the test list.
   - Add docking integration test file to the test list.
4. Update validation results section with benchmark command.

## 4. File Change Summary

| Action | File | Workstream |
|--------|------|------------|
| **Create** | `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/benchmark/CodeEditorBenchmarkTest.java` | A |
| **Create** | `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorDockingIntegrationTest.java` | B |
| **Edit** | `papiflyfx-docking-code/pom.xml` (add `benchmark` tag exclusion to default surefire) | A |
| **Edit** | `papiflyfx-docking-code/README.md` (full rewrite with integration guide) | C |
| **Edit** | `spec/papiflyfx-docking-code/PROGRESS.md` (Phase 8 completion) | D |
| **Create** (conditional) | `papiflyfx-docking-code/src/main/resources/META-INF/services/...` | C2 |

## 5. Implementation Order

1. **Workstream A** (benchmarks) — implement first so metrics are available for docs.
2. **Workstream B** (integration tests) — independent of A, can be done in parallel.
3. **Workstream C** (docs) — after A/B so README can include measured metrics.
4. **Workstream D** (progress update) — last, once all results are final.

## 6. Exit Criteria

Per `implementation.md` Phase 8 and `spec.md` §8:
- [ ] Benchmark harness measures all four acceptance criteria from spec §8.
- [ ] Measured results are recorded in PROGRESS.md with pass/fail against thresholds.
- [ ] End-to-end docking integration test covers full session capture → restore with editor content.
- [ ] Module README contains working registration examples and quickstart.
- [ ] Full test suite (unit + integration + new tests) passes with 0 failures.
- [ ] Benchmarks run successfully in headless mode via documented Maven command.

## 7. Risks

| Risk | Mitigation |
|------|------------|
| Memory benchmark is noisy under JVM GC | Use soft ceiling (400MB), log actual value, run with `-Xmx512m` to detect leaks early |
| Typing/scroll p95 sensitive to CI machine speed | Benchmarks tagged and excluded from default runs; thresholds tested on baseline hardware |
| FX thread deadlock in benchmark synchronization | Reuse proven `callOnFx()` + `CountDownLatch` patterns from existing tests |
| ServiceLoader auto-registration may conflict with existing registry behavior | Verify `ContentStateRegistry` supports ServiceLoader before adding descriptor; skip if not |

## 8. Validation Commands

```bash
# Compile
mvn -pl papiflyfx-docking-code -am compile

# Regular tests (excludes benchmarks)
mvn -pl papiflyfx-docking-code,papiflyfx-docking-docks -am -Dtestfx.headless=true test

# Benchmarks only
mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true -Dgroups=benchmark test

# All tests including benchmarks
mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true -Dgroups=benchmark -DexcludedGroups= test
```
