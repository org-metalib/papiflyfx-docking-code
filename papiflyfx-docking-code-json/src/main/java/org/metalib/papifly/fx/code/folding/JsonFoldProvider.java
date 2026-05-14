package org.metalib.papifly.fx.code.folding;

import org.metalib.papifly.fx.code.lexer.JsonLexer;
import org.metalib.papifly.fx.code.lexer.TokenMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

final class JsonFoldProvider implements FoldProvider {

    @Override
    public String languageId() {
        return JsonLexer.LANGUAGE_ID;
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
        Deque<OpenJsonBlock> stack = new ArrayDeque<>();
        boolean inString = false;
        boolean escaped = false;
        for (int lineIndex = 0; lineIndex < safeLines.size(); lineIndex++) {
            if (cancelled != null && cancelled.getAsBoolean()) {
                return baseline == null ? FoldMap.empty() : baseline;
            }
            String text = safeLines.get(lineIndex);
            for (int i = 0; i < text.length(); i++) {
                char current = text.charAt(i);
                if (inString) {
                    if (escaped) {
                        escaped = false;
                        continue;
                    }
                    if (current == '\\') {
                        escaped = true;
                        continue;
                    }
                    if (current == '"') {
                        inString = false;
                    }
                    continue;
                }
                if (current == '"') {
                    inString = true;
                    escaped = false;
                    continue;
                }
                if (current == '{') {
                    stack.push(new OpenJsonBlock(lineIndex, FoldKind.JSON_OBJECT, stack.size() + 1));
                    continue;
                }
                if (current == '[') {
                    stack.push(new OpenJsonBlock(lineIndex, FoldKind.JSON_ARRAY, stack.size() + 1));
                    continue;
                }
                if (current == '}') {
                    closeBlock(stack, FoldKind.JSON_OBJECT, lineIndex, regions);
                    continue;
                }
                if (current == ']') {
                    closeBlock(stack, FoldKind.JSON_ARRAY, lineIndex, regions);
                }
            }
        }
        FoldMap baseMap = new FoldMap(regions);
        Set<Integer> preserved = baseline == null ? Set.of() : baseline.collapsedHeaderLines();
        return baseMap.withCollapsedHeaders(preserved);
    }

    private static void closeBlock(
        Deque<OpenJsonBlock> stack,
        FoldKind expectedKind,
        int closeLine,
        List<FoldRegion> regions
    ) {
        if (stack.isEmpty()) {
            return;
        }
        OpenJsonBlock matched = null;
        Deque<OpenJsonBlock> dropped = new ArrayDeque<>();
        while (!stack.isEmpty()) {
            OpenJsonBlock candidate = stack.pop();
            if (candidate.kind() == expectedKind) {
                matched = candidate;
                break;
            }
            dropped.push(candidate);
        }
        while (!dropped.isEmpty()) {
            stack.push(dropped.pop());
        }
        if (matched == null) {
            return;
        }
        if (closeLine > matched.line()) {
            regions.add(new FoldRegion(matched.line(), closeLine, matched.kind(), matched.depth(), false));
        }
    }

    private record OpenJsonBlock(int line, FoldKind kind, int depth) {
    }
}

