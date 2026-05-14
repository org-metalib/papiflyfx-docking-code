package org.metalib.papifly.fx.code.folding;

import org.metalib.papifly.fx.code.lexer.MarkdownLexer;
import org.metalib.papifly.fx.code.lexer.TokenMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

final class MarkdownFoldProvider implements FoldProvider {

    @Override
    public String languageId() {
        return MarkdownLexer.LANGUAGE_ID;
    }

    @Override
    public FoldMap recompute(
        List<String> lines,
        TokenMap tokenMap,
        FoldMap baseline,
        int dirtyStartLine,
        BooleanSupplier cancelled
    ) {
        List<String> safeLines = lines == null || lines.isEmpty() ? List.of("") : lines;
        List<FoldRegion> regions = new ArrayList<>();
        List<Heading> headings = new ArrayList<>();
        int fenceStartLine = -1;
        boolean inFence = false;
        for (int lineIndex = 0; lineIndex < safeLines.size(); lineIndex++) {
            if (cancelled != null && cancelled.getAsBoolean()) {
                return baseline == null ? FoldMap.empty() : baseline;
            }
            String text = safeLines.get(lineIndex);
            String trimmed = text.trim();
            if (trimmed.startsWith("```")) {
                if (inFence) {
                    if (lineIndex > fenceStartLine) {
                        regions.add(new FoldRegion(fenceStartLine, lineIndex, FoldKind.MARKDOWN_FENCE, 1, false));
                    }
                    inFence = false;
                    fenceStartLine = -1;
                } else {
                    inFence = true;
                    fenceStartLine = lineIndex;
                }
                continue;
            }
            if (inFence) {
                continue;
            }
            int headingLevel = detectHeadingLevel(text);
            if (headingLevel > 0) {
                headings.add(new Heading(lineIndex, headingLevel));
            }
        }
        int lastLine = Math.max(0, safeLines.size() - 1);
        if (inFence && fenceStartLine >= 0 && lastLine > fenceStartLine) {
            regions.add(new FoldRegion(fenceStartLine, lastLine, FoldKind.MARKDOWN_FENCE, 1, false));
        }
        for (int i = 0; i < headings.size(); i++) {
            Heading current = headings.get(i);
            int endLine = lastLine;
            for (int j = i + 1; j < headings.size(); j++) {
                Heading next = headings.get(j);
                if (next.level() <= current.level()) {
                    endLine = next.line() - 1;
                    break;
                }
            }
            if (endLine > current.line()) {
                regions.add(new FoldRegion(
                    current.line(),
                    endLine,
                    FoldKind.MARKDOWN_SECTION,
                    current.level(),
                    false
                ));
            }
        }
        FoldMap baseMap = new FoldMap(regions);
        Set<Integer> preserved = baseline == null ? Set.of() : baseline.collapsedHeaderLines();
        return baseMap.withCollapsedHeaders(preserved);
    }

    private static int detectHeadingLevel(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int index = 0;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        int hashCount = 0;
        while (index < text.length() && text.charAt(index) == '#') {
            hashCount++;
            index++;
        }
        if (hashCount == 0 || hashCount > 6) {
            return 0;
        }
        if (index >= text.length() || !Character.isWhitespace(text.charAt(index))) {
            return 0;
        }
        return hashCount;
    }

    private record Heading(int line, int level) {
    }
}

