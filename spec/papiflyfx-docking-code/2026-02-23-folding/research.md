# papiflyfx-docking-code: deep research and folding strategy

## 1) What this module is and how it is built

`papiflyfx-docking-code` is a JavaFX, canvas-rendered code editor module designed to be embedded inside the docking framework and restored through docking session persistence.

At a high level it provides:
- text model + undo/redo (`document` package),
- viewport rendering + virtualization (`render` package),
- keyboard/pointer navigation/editing controllers (`controller` package),
- search/replace + go-to-line overlays (`search` and API overlay classes),
- syntax highlighting via incremental lexing (`lexer` package),
- state capture/restore for docking sessions (`state` + `api` adapter/factory classes),
- integration tests proving round-trips through `DockManager`.

Main entry point:
- `org.metalib.papifly.fx.code.api.CodeEditor`

Important composition in `CodeEditor`:
- `Document` text model
- `SelectionModel` + `MultiCaretModel`
- `Viewport` (canvas rendering and scrolling)
- `GutterView`
- `IncrementalLexerPipeline`
- `SearchModel` + `SearchController`
- `GoToLineController`

This module is architected around separation of concerns: model/editor commands are separate from rendering passes, and lexing is async/incremental with revision safety.

---

## 2) Runtime architecture and data flow

### 2.1 Editing flow

1. User input reaches `EditorInputController`, `EditorEditController`, `EditorPointerController`, `EditorNavigationController`.
2. Controllers apply `InsertEdit` / `DeleteEdit` / `ReplaceEdit` / `CompoundEdit` on `Document`.
3. `Document` updates `TextSource`, updates `LineIndex` incrementally, emits `DocumentChangeEvent`.
4. `CodeEditor` reacts: invalidates viewport regions, updates caret/selection state, schedules lexer work.

Notable model specifics:
- `Document` keeps offset/line conversion via `LineIndex`.
- undo/redo stacks are command-based (commands also support incremental line-index update methods).
- bulk edits are grouped by `compoundEdit(...)`.

### 2.2 Render flow

`Viewport` performs pass-based rendering on a canvas:
- `BackgroundPass`
- `SearchPass`
- `SelectionPass`
- `TextPass`
- `CaretPass`
- `ScrollbarPass`

Supporting structures:
- `RenderContext` and `RenderLine`
- `WrapMap` for visual row mapping when wrap mode is enabled
- `ViewportInvalidationPlanner` to avoid full repaints when possible

`GutterView` is separate from text canvas and currently paints line numbers + marker lane; it is wrap-aware for line numbering.

### 2.3 Search and overlays

- Search is model-driven (`SearchModel`) and UI-driven (`SearchController` + overlay).
- Supports plain text/regex, case sensitivity, whole-word, replace-current/replace-all, selection scope, preserve-case behavior.
- Test coverage is broad, including regex cache invalidation and replacement semantics.
- Go-to-line has its own overlay/controller and validation path.

### 2.4 Docking/session integration

- `CodeEditorStateAdapter` and `CodeEditorFactory` integrate with DockManager content restore.
- Service loader registration exists in `META-INF/services/...ContentStateAdapter`.
- `EditorStateCoordinator` captures/applies editor state fields.
- Integration tests validate full docking round-trip and fallback behavior when adapter/factory is missing.

---

## 3) Lexer subsystem: how it works today

## 3.1 Core lexer contract

`Lexer` contract is line-based:
- `LexResult lexLine(String text, LexState entryState)`
- output is `List<Token>` + exit `LexState`

Core types:
- `LexState` is a tiny immutable wrapper around an `int code`.
- `Token` is line-local (`startColumn`, `length`, `TokenType`).
- `TokenType` is highlighting-oriented (keyword, string, comment, etc.).
- `LineTokens` stores one line snapshot (`text`, tokens, entry/exit states).
- `TokenMap` holds all `LineTokens` and supports immutable replacement ranges.

Important implication: tokens and states are optimized for highlighting, not structural region trees.

## 3.2 Incremental lexing engine

`IncrementalLexerEngine` behavior:
- starts at a dirty line (`dirtyStartLine`) and computes state from previous line's exit state;
- re-lexes forward line-by-line;
- if old line text and entry state match newly computed values, the engine can reuse old suffix and stop early;
- supports cancellation checks via callback.

This gives practical incremental performance and keeps invalidation bounded in common edits.

## 3.3 Async pipeline orchestration

`IncrementalLexerPipeline` adds:
- document-listener hookup,
- debounced scheduling (`PauseTransition`),
- merged dirty-start tracking,
- background lexing (`CompletableFuture`),
- revision gating before apply (drop stale results),
- fallback to `PlainTextLexer` if selected lexer throws.

`TokenMap` is exposed to viewport through a consumer so rendering updates after successful lex apply.

## 3.4 Concrete lexers and state machines

### Abstract C-style base (`AbstractCStyleLexer`)

States:
- default
- block comment
- double quote
- single quote
- template quote (not optional)

It handles:
- `//` single-line comments,
- `/* ... */` multiline comments with cross-line state,
- quote continuations across lines using state.

### Java lexer

- Extends C-style base with Java keyword set.
- Uses standard string/comment handling from the base class.
- **Does not explicitly implement Java text blocks (`"""`)** as a dedicated grammar/state.

### JavaScript lexer

- Extends C-style base with template quote enabled.
- Backtick strings are recognized.
- `${...}` interpolation is not structurally tokenized with nested expression depth; it is effectively treated within string scanning behavior.

### JSON lexer

- Own lexer with states for string continuation and escapes.
- No comments, no block scopes by language design.

### Markdown lexer

- Own state for fenced code block (` ``` ` fences).
- Distinguishes headings/list markers/punctuation/plain text at a lightweight level.
- Good example of multiline mode switching in current architecture.

### Plain text lexer

- no semantic tokenization.

## 3.5 Lexer tests and confidence

Tests cover:
- language token basics (Java/JS/JSON/Markdown),
- multiline continuation behavior,
- incremental engine reuse boundaries/cancellation,
- pipeline ordering/debounce/stale-result handling/fallback behavior.

Net: highlighting pipeline quality is strong; structural parsing for fold trees is not part of current contract.

---

## 4) Folding status today: what exists vs what is missing

## 4.1 Existing traces of folding

There is a persisted concept:
- `CodeEditor` exposes `getFoldedLines()/setFoldedLines(...)`.
- `EditorStateData` stores `foldedLines`.
- `EditorStateCodec` serializes `foldedLines`.
- integration tests verify folded-line round-trip through state.

So folding has state placeholders, but:
- `EditorStateData` marks folded lines as MVP-empty semantics,
- there is no runtime fold model, no collapse behavior, no UI affordance.

## 4.2 Missing runtime pieces

1. No fold-region data model (`startLine/endLine/kind/depth/...`).
2. No fold computation engine.
3. No bridge from lex/text structure to visibility mapping.
4. No gutter fold glyphs, toggle hit-testing, or keyboard fold commands.
5. No viewport elision logic to hide folded body lines.
6. No robust persistence identity (line numbers drift after edits).

## 4.3 Why current lexer contract is insufficient for full folding

- Line-local tokens do not encode open/close structure as first-class events.
- `LexState` is a single `int` mode code, not a general nested context stack.
- Token types focus on syntax coloring, not region boundaries.
- No per-language fold provider abstraction exists.

Result: multiline highlighting works; hierarchical folding is not derivable reliably for all target language constructs.

---

## 5) Specific language gaps related to "text blocks" and nesting

The request mentions languages with text blocks and nested text blocks. Current state:

- **Java**: text blocks (`"""`) are not modeled explicitly; current quote logic is quote-char-based and does not implement Java text block delimiters/semantics.
- **JavaScript**: template literals are recognized by backticks, but nested `${...}` expression structure and nested brace depth are not tracked as fold-capable regions.
- **Markdown**: fenced blocks are modeled (useful for fold candidates), but heading-based section folding and robust nested region treeing are not yet formalized.
- **JSON**: braces/brackets give structural folding opportunities, but current lexer output does not emit fold boundaries explicitly.

---

## 6) Improvement ideas for folding support

## 6.1 Introduce fold domain model (new core types)

Recommended minimum model:

```java
record FoldRegion(
    int startLine,      // line with header
    int endLine,        // inclusive last line in region
    FoldKind kind,      // BRACE, COMMENT, TEXT_BLOCK, TEMPLATE, MARKDOWN_SECTION, FENCE, ...
    int depth,
    boolean collapsed
) {}
```

Supporting indexes:
- `FoldMap` (line -> regions starting/ending/covering)
- `CollapsedIntervals` (fast visibility checks)

## 6.2 Add a fold computation layer (recommended architecture)

Best balance: **hybrid fold engine**:
- keep lexers primarily responsible for highlighting,
- add language-specific `FoldProvider` that consumes text + tokens + lexer state hints,
- produce `FoldMap` incrementally from dirty line onward (similar to lex engine).

Why hybrid:
- avoids overloading token model with too much structure,
- allows fast language-specific rules without full parser complexity,
- can reuse existing incremental/revision plumbing patterns.

Suggested API:

```java
interface FoldProvider {
    FoldComputationResult recompute(
        List<String> lines,
        TokenMap tokenMap,
        FoldMap previous,
        int dirtyStartLine,
        CancellationToken cancellation);
}
```

## 6.3 Not Optional lexer enhancement: structural signals

For higher accuracy/performance, lexers can emit not optional fold signals:

```java
record FoldSignal(int line, FoldSignalType type, FoldKind kind, int aux) {}
```

Examples:
- OPEN_BRACE / CLOSE_BRACE outside strings/comments
- BLOCK_COMMENT_START / BLOCK_COMMENT_END
- JAVA_TEXT_BLOCK_START / END
- TEMPLATE_EXPR_START / END
- MARKDOWN_FENCE_START / END

Fold providers can consume these signals instead of re-scanning raw text for delimiters.

## 6.4 Evolve `LexState` for nested contexts

Current `LexState(int code)` is coarse for deeply nested constructs.

Move to richer immutable state payload (more expressive, broader refactor).

Practical recommendation: start with encoded depth for JS template interpolation and Java text block mode; defer full state-object refactor unless complexity grows.

## 6.5 Viewport + wrap integration for folded visibility

Introduce a mapping layer before render-line construction:
- logical document lines -> visible lines (excluding folded interiors),
- integrate with wrap mode (`WrapMap`) so wrapped rows of hidden logical lines are skipped.

Likely direction:
- new `VisibleLineMap` feeding both `Viewport` and `GutterView`.
- caret/navigation/search must map through visible/logical transforms.

## 6.6 Gutter folding UX

Extend `GutterView`:
- render fold affordance on fold-header lines (collapsed/expanded glyph),
- hit-test mouse clicks,
- provide toggle callbacks (`toggleFoldAtLine`, `foldRegion`, `unfoldRegion`).

Keyboard commands to add:
- fold/unfold current region,
- fold all / unfold all,

## 6.7 Persistence strategy upgrade

Current `foldedLines` is brittle after edits.

Recommended persisted form:
- collapsed `FoldRegionRef` list (start anchor + kind + maybe end anchor or hash),
- restore by reconciling anchors against newly computed fold map.

Keep backward compatibility:
- read old `foldedLines` when present,
- map line numbers to nearest fold header region on first load,
- persist new schema going forward.

---

## 7) Recommended phased plan

### Phase 1: Minimal functional folding
- Add `FoldRegion`, `FoldMap`, `VisibleLineMap`.
- Implement brace/comment/fence folding providers.
- Add gutter toggle UI.
- Add viewport elision with stable caret clamping.
- Keep persisted `foldedLines` as temporary compatibility source.

### Phase 2: Language-aware advanced regions
- Java: explicit `"""` text block state + fold regions.
- JavaScript: template interpolation depth tracking (`${...}` nesting) and template/body folds.
- Markdown: heading-section folds with proper level nesting.

### Phase 3: State robustness and UX polish
- Replace plain `foldedLines` persistence with region refs.
- Add commands (fold all/level).
- Ensure search, go-to-line, selection, and multi-caret behavior are correct across folded areas.

---

## 8) Testing strategy to keep risk low

Add tests at multiple layers:

1. Unit tests for fold providers:
   - brace nesting,
   - multiline comments,
   - Java text blocks,
   - JS nested template expressions,
   - markdown fences/headings.

2. Incremental recompute tests:
   - dirty-line edits only update affected fold subtree,
   - unchanged tails are reused.

3. Viewport/gutter integration tests:
   - folded lines are not rendered,
   - line number mapping is correct in wrap/no-wrap,
   - toggle click behavior.

4. State persistence tests:
   - collapsed regions round-trip,
   - migration from old `foldedLines` data.

5. Interaction tests:
   - caret movement into folded range unfolds or skips consistently,
   - search result reveal behavior.

---

## 9) Key conclusion

The current editor is already strong in document modeling, rendering virtualization, and incremental highlighting; this is a good foundation for folding.

However, folding is currently only a serialized placeholder and not a runtime feature. To support robust folding (including text blocks and nested constructs), the module needs:
- a real fold domain model,
- incremental fold computation (ideally language-aware),
- viewport/gutter visibility integration,
- and stronger persisted fold identity than raw line numbers.

The safest path is a phased rollout that first delivers brace/comment/fence folds, then extends lexers/providers for Java text blocks and nested JS template structures.
