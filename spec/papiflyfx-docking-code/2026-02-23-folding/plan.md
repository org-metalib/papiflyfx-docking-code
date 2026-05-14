# papiflyfx-docking-code folding implementation plan

## Problem statement

`papiflyfx-docking-code` currently persists `foldedLines` in editor state, but there is no runtime folding model, no gutter controls, and no viewport line elision.  
Goal: implement robust, incremental code folding to improve navigation while preserving current editor performance and state compatibility.

## Current progress and immediate next steps

- [x] Research completed in `research.md` (architecture + lexer + folding gap analysis).
- [x] Implementation plan authored in this document.
- [x] Start implementation at **Phase 1 (foundation model + folding pipeline skeleton)**.
- [x] Deliver first usable UX milestone: fold toggle in gutter + collapsed rendering for structural folds.
- [x] Publish implementation summary report in `summary.md`.

---

## Scope

### In scope

1. Runtime fold model (`FoldRegion`, `FoldMap`, collapsed state).
2. Incremental fold recomputation aligned with current incremental lexer workflow.
3. Viewport + wrap integration to hide folded content lines.
4. Gutter fold affordances (glyphs + mouse toggle).
5. Keyboard commands for fold operations.
6. Language-aware fold providers, including:
   - Java: braces/comments + explicit text block support (`"""`).
   - JavaScript: braces/comments + template literal/interpolation-aware folds.
   - JSON: object/array structural folds.
   - Markdown: fenced code blocks + heading section folds.
7. Persistence migration from line-only folding to region-aware folding refs with backward compatibility.

### Out of scope (for this rollout)

1. Full AST parser integration for all languages.
2. Arbitrary user-defined custom fold regions.
3. Semantic fold levels derived from full symbol analysis.

---

## Design principles

1. **Incremental first**: match existing `IncrementalLexerEngine`/`IncrementalLexerPipeline` behavior style.
2. **Language pluggability**: folding logic per language via providers.
3. **UI consistency**: all coordinate transforms (caret, hit-test, scroll, wrap, search reveal) must respect folded visibility.
4. **Safe migration**: keep old `foldedLines` readable until all persisted states move to new schema.
5. **Small vertical slices**: ship minimal structural folding first, then language-specific advanced constructs.

---

## Target architecture

### 1) Folding domain model (new package)

Proposed package: `org.metalib.papifly.fx.code.folding`

```java
public enum FoldKind {
    BRACE_BLOCK,
    BLOCK_COMMENT,
    JAVA_TEXT_BLOCK,
    JS_TEMPLATE_BLOCK,
    JS_TEMPLATE_EXPR,
    JSON_OBJECT,
    JSON_ARRAY,
    MARKDOWN_FENCE,
    MARKDOWN_SECTION
}

public record FoldRegion(
    int startLine,          // header line; user toggles here
    int endLine,            // inclusive end line
    FoldKind kind,
    int depth,
    boolean collapsed
) {
    public FoldRegion {
        if (startLine < 0 || endLine < startLine) {
            throw new IllegalArgumentException("Invalid fold region bounds");
        }
    }
}
```

```java
public final class FoldMap {
    private final List<FoldRegion> regions;
    // indexes: by start line, by containing line, by id

    public List<FoldRegion> regionsStartingAt(int line) { ... }
    public FoldRegion innermostRegionAt(int line) { ... }
    public boolean isHiddenLine(int line) { ... } // true for collapsed interior lines
    public FoldMap toggleAtHeaderLine(int line) { ... } // immutable update
}
```

### 2) Fold provider abstraction

```java
public interface FoldProvider {
    String languageId();

    FoldMap recompute(
        List<String> lines,
        TokenMap tokenMap,
        FoldMap baseline,
        int dirtyStartLine,
        java.util.function.BooleanSupplier cancelled
    );
}
```

Registry (similar to `LexerRegistry`):

```java
public final class FoldProviderRegistry {
    public static FoldProvider resolve(String languageId) { ... } // fallback provider
}
```

### 3) Folding pipeline (parallel to lexer pipeline)

Add a debounced async pipeline that:
- listens to `Document` changes (dirty start line merge),
- listens to token-map updates (optional tighter accuracy),
- recomputes fold map incrementally,
- applies latest revision only.

```java
public final class IncrementalFoldingPipeline implements AutoCloseable {
    public IncrementalFoldingPipeline(
        Document document,
        java.util.function.Supplier<TokenMap> tokenMapSupplier,
        java.util.function.Consumer<FoldMap> foldMapConsumer
    ) { ... }

    public void setLanguageId(String languageId) { ... }
    public FoldMap getFoldMap() { ... }
    public void setCollapsedHeaders(Set<Integer> headerLines) { ... } // external toggles
}
```

### 4) Visible line mapping layer

Introduce `VisibleLineMap` to unify non-wrap and wrap behavior when folds hide logical lines.

```java
public final class VisibleLineMap {
    public void rebuild(int logicalLineCount, FoldMap foldMap) { ... }
    public int visibleToLogical(int visibleLine) { ... }
    public int logicalToVisible(int logicalLine) { ... }
    public int visibleCount() { ... }
    public boolean isHiddenLogicalLine(int logicalLine) { ... }
}
```

`Viewport` and `GutterView` should consume this map; `WrapMap` should be built from visible logical lines only.

### 5) Editor integration points

`CodeEditor` will become orchestration point for lexer + folding + viewport/gutter state:

```java
// CodeEditor fields (planned)
private FoldMap foldMap = FoldMap.empty();
private final VisibleLineMap visibleLineMap = new VisibleLineMap();
private final IncrementalFoldingPipeline foldingPipeline;

public void toggleFoldAtLine(int line) { ... }
public void foldAll() { ... }
public void unfoldAll() { ... }
```

Initialization wiring:

```java
this.lexerPipeline = resolvedLexerPipelineFactory.apply(this.document, viewport::setTokenMap);
this.foldingPipeline = new IncrementalFoldingPipeline(
    this.document,
    lexerPipeline::getTokenMap,
    this::applyFoldMap
);
this.languageListener = (obs, oldValue, newValue) -> {
    lexerPipeline.setLanguageId(newValue);
    foldingPipeline.setLanguageId(newValue);
};
```

---

## Detailed phased implementation plan

## Phase 1 — Foundation model and pipeline skeleton (Completed)

**Objective**: establish fold domain, registry, and async incremental pipeline without changing UX yet.

### Tasks

1. Create `folding` package and core types:
   - `FoldKind`, `FoldRegion`, `FoldMap`.
   - `VisibleLineMap`.
2. Add `FoldProvider` + `FoldProviderRegistry`.
3. Implement baseline providers:
   - `PlainTextFoldProvider` (returns empty),
   - `StructuralFoldProvider` (brace/comment generic),
   - `MarkdownFoldProvider` (fence only in this phase).
4. Implement `IncrementalFoldingPipeline`:
   - debounce + revision gating (mirror lexer pipeline behavior),
   - merged dirty start line tracking,
   - cancellation handling.
5. Wire pipeline lifecycle into `CodeEditor` (`dispose()` included), but keep UI behavior unchanged until Phase 2.

### Deliverable

- Compiling fold infrastructure with unit tests for map/provider/pipeline basics.

---

## Phase 2 — Runtime folding UX (viewport + gutter + interactions) (Completed)

**Objective**: make folding visible and interactive for structural folds.

### Tasks

1. `Viewport` integration:
   - add `setVisibleLineMap(VisibleLineMap)` and `setFoldMap(FoldMap)`,
   - replace direct logical line iteration in `computeVisibleRange`, `buildRenderLines`, `getHitPosition`, `ensureCaretVisible`,
   - ensure wrapped mode uses visible-line-filtered rows.
2. `GutterView` integration:
   - render fold glyphs on fold header lines,
   - expose hit-test API for fold glyph area.
3. Pointer input integration:
   - route gutter clicks to `CodeEditor.toggleFoldAtLine(...)`.
4. Editor API:
   - add `toggleFoldAtLine`, `foldRegion`, `unfoldRegion`, `foldAll`, `unfoldAll`.
5. Behavior rules:
   - caret cannot remain on hidden logical line (clamp to header/end as defined),
   - searching/go-to-line should reveal folded target region before placing caret.

### Snippet: viewport render loop concept

```java
int firstVisible = visibleLineMap.firstVisibleLineForOffset(scrollOffset, lineHeight);
for (int i = 0; i < visibleLineCount; i++) {
    int visibleLine = firstVisible + i;
    int logicalLine = visibleLineMap.visibleToLogical(visibleLine);
    String text = document.getLineText(logicalLine);
    renderLines.add(new RenderLine(logicalLine, 0, text.length(), text, y, tokenMap.tokensForLine(logicalLine)));
}
```

### Deliverable

- Users can fold/unfold via gutter; folded interior lines are not rendered.

---

## Phase 3 — Language-specific folding depth (text blocks + nested templates) (Completed)

**Objective**: add high-value language specifics requested in research.

### Tasks

1. **Java text blocks**:
   - extend Java lexer states (or provider scanner) to detect `"""` start/end robustly,
   - emit `JAVA_TEXT_BLOCK` fold regions.
2. **JavaScript nested template expressions**:
   - support `${...}` depth tracking inside backticks,
   - produce template body and interpolation fold regions (`JS_TEMPLATE_BLOCK`, `JS_TEMPLATE_EXPR`).
3. **Markdown heading folds**:
   - implement section folding based on heading level hierarchy.
4. **JSON structural fidelity**:
   - produce object/array folds only outside strings.

### Snippet: Java text block state extension (illustrative)

```java
private static final int STATE_JAVA_TEXT_BLOCK = 5;

if (state == STATE_DEFAULT && matches(text, index, "\"\"\"")) {
    int end = text.indexOf("\"\"\"", index + 3);
    if (end < 0) {
        tokens.add(new Token(index, text.length() - index, TokenType.STRING));
        return new LexResult(tokens, LexState.of(STATE_JAVA_TEXT_BLOCK));
    }
    tokens.add(new Token(index, (end + 3) - index, TokenType.STRING));
    index = end + 3;
}
```

### Deliverable

- Accurate folding for text blocks and nested template constructs.

---

## Phase 4 — Persistence migration (v4 state) (Completed)

**Objective**: replace brittle line-only fold persistence with region-aware refs, while preserving backward compatibility.

### Tasks

1. Extend state DTO:
   - add `foldedRegions` (new field),
   - keep `foldedLines` readable for migration.
2. Add `FoldRegionRef`:
   - minimally `startLine`, `kind`, `headerTextHash` (or similar stable hint).
3. Update codec:
   - `toMap` writes `foldedRegions` + optionally `foldedLines` during transition,
   - `fromMap` reads both, prioritizes `foldedRegions`.
4. Bump `CodeEditorStateAdapter.VERSION` from `3` to `4`.
5. Migration strategy:
   - if only `foldedLines` present, map to nearest fold header regions after recompute.

### Snippet: state shape direction

```java
public record FoldRegionRef(int startLine, String kind, String headerHash) {}

public record EditorStateData(
    ...,
    List<Integer> foldedLines,          // legacy read path
    List<FoldRegionRef> foldedRegions,  // new write/read path
    ...
) { ... }
```

### Deliverable

- Persisted folding survives edits/restores better than raw line-number lists.

---

## Phase 5 — Commands, keymap, polishing, and hardening (Completed)

**Objective**: complete interaction and quality bar.

### Tasks

1. Extend `EditorCommand`:
   - `TOGGLE_FOLD`, `FOLD_ALL`, `UNFOLD_ALL`, `FOLD_RECURSIVE`, `UNFOLD_RECURSIVE` (final subset can be reduced if needed).
2. Add key bindings in `KeymapTable`.
3. Register handlers in `EditorCommandRegistry`.
4. Ensure multi-caret and selection behavior remains stable across folded lines.
5. Optimize hot paths:
   - avoid full `VisibleLineMap` rebuild where possible,
   - profile large documents (10k+ lines).
6. UX polish:
   - collapsed placeholder text (`...`) optional,
   - active-line styling on fold headers.

### Deliverable

- Full folding workflow via mouse and keyboard with stable editor behavior.

---

## Test plan (required before merge)

## Unit tests

1. `FoldMapTest`
   - nested regions, toggle semantics, hidden-line checks.
2. `VisibleLineMapTest`
   - visible/logical conversion with mixed collapsed regions.
3. `StructuralFoldProviderTest`
   - braces/comments with nested and edge cases.
4. `JavaFoldProviderTest`
   - multiline text blocks, escaped delimiter scenarios.
5. `JavaScriptFoldProviderTest`
   - nested `${...}` depth transitions.
6. `MarkdownFoldProviderTest`
   - fenced blocks + heading hierarchy.

## Incremental tests

1. `IncrementalFoldingPipelineTest`
   - debounce/revision ordering,
   - merged dirty start behavior,
   - cancellation handling,
   - stale result dropping.

## Integration tests

1. `ViewportTest` additions:
   - folded lines are excluded from render/hit-testing.
2. `GutterViewTest` additions:
   - fold glyph rendering and hit detection.
3. `CodeEditorIntegrationTest` additions:
   - fold toggle APIs,
   - state capture/apply for folded regions (v4),
   - migration from legacy `foldedLines`.

## Regression tests

1. Search/go-to-line reveals folded targets.
2. Caret clamping when collapsing the caret’s current region.
3. Word-wrap + folding coexistence (no row mapping corruption).

---

## Detailed TODO backlog (do not implement yet)

### Phase 1 backlog

- [x] Create folding model classes (`FoldKind`, `FoldRegion`, `FoldMap`).
- [x] Create `VisibleLineMap` and mapping tests.
- [x] Create provider API + registry.
- [x] Implement plain/structural/markdown-fence providers.
- [x] Implement `IncrementalFoldingPipeline`.
- [x] Wire lifecycle in `CodeEditor` (construct/dispose).

### Phase 2 backlog

- [x] Add fold-aware mappings in `Viewport`.
- [x] Add fold glyph painting + hit-test methods in `GutterView`.
- [x] Add gutter click routing to fold toggle action.
- [x] Add public folding APIs on `CodeEditor`.
- [x] Implement reveal-on-navigation for folded target lines.

### Phase 3 backlog

- [x] Implement Java text block fold detection.
- [x] Implement JavaScript nested template fold detection.
- [x] Implement Markdown heading section folds.
- [x] Tighten JSON structural fold detection.

### Phase 4 backlog

- [x] Add v4 state schema (`foldedRegions`) to DTO/codec.
- [x] Add migration logic from `foldedLines` to fold refs.
- [x] Bump adapter version and expand migration tests.
- [x] Validate backward compatibility against v0/v1/v2/v3 restore paths.

### Phase 5 backlog

- [x] Add fold commands and key bindings.
- [x] Register command handlers.
- [x] Add performance guards/benchmarks for large files.
- [x] Complete full test matrix and fix edge regressions.

---

## Recommended execution order

1. Ship **Phase 1 + Phase 2** behind a minimal, stable UX.
2. Add **Phase 3** language-specific correctness.
3. Perform **Phase 4** persistence migration once runtime model is stable.
4. Finish with **Phase 5** command polish/performance hardening.

This order minimizes risk by validating core rendering/interaction behavior before schema changes.
