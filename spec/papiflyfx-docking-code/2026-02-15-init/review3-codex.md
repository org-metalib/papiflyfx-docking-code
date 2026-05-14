# Review 3 (Codex): Phase 3 Current Changes

Date: 2026-02-16  
Scope reviewed:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/Document.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/IncrementalLexerEngine.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/IncrementalLexerPipeline.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/LexerRegistry.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/MarkdownLexer.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/TokenType.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/lexer/MarkdownLexerTest.java`

## Findings

### 1. HIGH: `IncrementalLexerEngine` can return stale lines after first reusable line
Code references:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/IncrementalLexerEngine.java:76`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/IncrementalLexerEngine.java:85`

What is wrong:
- The new early-stop optimization returns immediately on the first reusable line when line count is unchanged.
- That assumes all following lines are unchanged, which is not guaranteed for multi-line or merged rapid edits.

Concrete reproduction (local):
- Baseline lines: `["a0","a1","a2","a3"]`
- Updated lines: `["a0x","a1","a2x","a3"]`, `dirtyStartLine=0`
- Observed output line 2 stayed `"a2"` (stale), not `"a2x"`.

Impact:
- Token map can become stale for later lines in the same revision.
- Visible result is wrong highlighting/content mapping after rapid edits in different regions.

Suggestions:
1. Remove the unconditional suffix copy return and continue validating/processing remaining lines.
2. If keeping early-stop, only copy tail after confirming all remaining `safeLines[j]` still text-match baseline.
3. Add regression tests:
   - Engine test for non-contiguous changed lines with unchanged line count.
   - Pipeline test for two rapid edits on distant lines before debounce fires.

### 2. MEDIUM: Ordered Markdown list detection is broken for numbers >= 10
Code references:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/MarkdownLexer.java:49`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/MarkdownLexer.java:58`

What is wrong:
- Condition `trimmed.startsWith(". ", 1)` only matches one-digit markers like `1. `.
- The `trimmed.startsWith("10. ")` special case is unreachable because the branch is never entered for `"10. ..."`.

Impact:
- Common ordered list items (`10.`, `11.`, etc.) are not tokenized as list markers.

Suggestions:
1. Parse an arbitrary run of leading digits, then require `. ` after digits.
2. Compute marker length from parsed digit span instead of hardcoding `3`/`4`.
3. Add tests for `10. item`, `123. item`, and negative cases like `1.item`.

### 3. MEDIUM: Full line snapshot creation moved onto the UI/change thread
Code references:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/document/Document.java:39`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/IncrementalLexerPipeline.java:70`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/IncrementalLexerPipeline.java:117`

What changed:
- Pipeline now captures `List<String>` snapshots via `document.getLinesSnapshot()` for initial lex, language changes, and document changes.

Risk:
- For large files, materializing every line string during edit handling can add visible typing latency on the caller thread.
- This pushes more O(n) work into the UI-side path instead of worker-side lex pipeline.

Suggestions:
1. Keep UI-side snapshot minimal (e.g., immutable full-text snapshot), and split lines on worker.
2. If line snapshots stay, add a simple perf guard test/benchmark around large documents (for example, 50k+ lines) and rapid edits.

## Validation Notes
- Executed: `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true test`
- Result: success, `117` tests passing.
