package org.metalib.papifly.fx.code.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;

/**
 * Incremental per-line lexer engine with state propagation.
 */
public final class IncrementalLexerEngine {

    private IncrementalLexerEngine() {
    }

    /**
     * Splits document text into line snapshots.
     *
     * @param text full document text snapshot
     * @return list of logical lines (always at least one element)
     */
    public static List<String> splitLines(String text) {
        if (text == null || text.isEmpty()) {
            return List.of("");
        }
        List<String> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines.add(text.substring(start, i));
                start = i + 1;
            }
        }
        lines.add(text.substring(start));
        return lines;
    }

    /**
     * Re-lexes the document text from a dirty start line.
     *
     * @param previous previous token map snapshot
     * @param text full document text snapshot
     * @param dirtyStartLine first potentially changed line index
     * @param lexer lexer implementation to apply
     * @return recalculated token map
     */
    public static TokenMap relex(TokenMap previous, String text, int dirtyStartLine, Lexer lexer) {
        return relex(previous, splitLines(text), dirtyStartLine, lexer);
    }

    /**
     * Re-lexes line snapshots from a dirty start line.
     *
     * @param previous previous token map snapshot
     * @param lines line snapshots to lex
     * @param dirtyStartLine first potentially changed line index
     * @param lexer lexer implementation to apply
     * @return recalculated token map
     */
    public static TokenMap relex(TokenMap previous, List<String> lines, int dirtyStartLine, Lexer lexer) {
        Objects.requireNonNull(lexer, "lexer");
        TokenMap baseline = previous == null ? TokenMap.empty() : previous;
        List<String> safeLines = lines == null || lines.isEmpty() ? List.of("") : lines;
        
        // Ensure dirtyStartLine is within bounds
        int safeStartLine = Math.max(0, Math.min(dirtyStartLine, safeLines.size()));

        // We use an ArrayList to build the result. TokenMap constructor currently copies this.
        List<LineTokens> output = new ArrayList<>(safeLines.size());
        
        // 1. Identify common prefix that hasn't changed.
        int maxPrefix = Math.min(Math.min(safeStartLine, baseline.lineCount()), safeLines.size());
        for (int i = 0; i < maxPrefix; i++) {
            if ((i & 1023) == 0) {
                ensureNotInterrupted();
            }
            LineTokens previousLine = baseline.lineAt(i);
            // If text changed, we must re-lex from here.
            if (previousLine == null || !Objects.equals(previousLine.text(), safeLines.get(i))) {
                break;
            }
            output.add(previousLine);
        }
        safeStartLine = output.size();

        // 2. Lex from safeStartLine until end or until states stabilize.
        LexState entryState = safeStartLine == 0 ? lexer.initialState() : output.get(safeStartLine - 1).exitState();
        for (int lineIndex = safeStartLine; lineIndex < safeLines.size(); lineIndex++) {
            ensureNotInterrupted();
            String lineText = safeLines.get(lineIndex);
            
            // Optimization: if text and entry state match what we had before,
            // and line count is unchanged, try to copy remaining tail from baseline.
            // We must verify every remaining line still text-matches to avoid stale tokens.
            LineTokens reusable = tryReuseLine(baseline, lineIndex, lineText, entryState);
            if (reusable != null && safeLines.size() == baseline.lineCount()) {
                boolean allMatch = true;
                for (int j = lineIndex; j < safeLines.size(); j++) {
                    if (((j - lineIndex) & 1023) == 0) {
                        ensureNotInterrupted();
                    }
                    LineTokens candidate = baseline.lineAt(j);
                    if (candidate == null || !Objects.equals(candidate.text(), safeLines.get(j))) {
                        allMatch = false;
                        break;
                    }
                }
                if (allMatch) {
                    for (int j = lineIndex; j < safeLines.size(); j++) {
                        output.add(baseline.lineAt(j));
                    }
                    return new TokenMap(output);
                }
            }

            LexResult lexResult = lexer.lexLine(lineText, entryState);
            LineTokens computed = new LineTokens(lineText, lexResult.tokens(), entryState, lexResult.exitState());
            output.add(computed);
            entryState = computed.exitState();
        }

        return new TokenMap(output);
    }

    private static LineTokens tryReuseLine(TokenMap baseline, int lineIndex, String lineText, LexState entryState) {
        LineTokens previousLine = baseline.lineAt(lineIndex);
        if (previousLine == null) {
            return null;
        }
        if (!Objects.equals(previousLine.text(), lineText)) {
            return null;
        }
        if (!Objects.equals(previousLine.entryState(), entryState)) {
            return null;
        }
        return previousLine;
    }

    private static void ensureNotInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("incremental lexing interrupted");
        }
    }
}
