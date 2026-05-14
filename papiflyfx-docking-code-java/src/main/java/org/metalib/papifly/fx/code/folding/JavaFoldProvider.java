package org.metalib.papifly.fx.code.folding;

import org.metalib.papifly.fx.code.lexer.JavaLexer;
import org.metalib.papifly.fx.code.lexer.TokenMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

final class JavaFoldProvider implements FoldProvider {

    private enum Mode {
        CODE,
        BLOCK_COMMENT,
        SINGLE_QUOTE,
        DOUBLE_QUOTE,
        TEXT_BLOCK
    }

    @Override
    public String languageId() {
        return JavaLexer.LANGUAGE_ID;
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
        Deque<OpenBlock> braceStack = new ArrayDeque<>();
        Mode mode = Mode.CODE;
        int blockCommentStart = -1;
        int textBlockStart = -1;
        boolean escaped = false;
        for (int lineIndex = 0; lineIndex < safeLines.size(); lineIndex++) {
            if (cancelled != null && cancelled.getAsBoolean()) {
                return baseline == null ? FoldMap.empty() : baseline;
            }
            String text = safeLines.get(lineIndex);
            for (int i = 0; i < text.length(); i++) {
                char current = text.charAt(i);
                char next = i + 1 < text.length() ? text.charAt(i + 1) : '\0';
                char next2 = i + 2 < text.length() ? text.charAt(i + 2) : '\0';
                if (mode == Mode.BLOCK_COMMENT) {
                    if (current == '*' && next == '/') {
                        if (lineIndex > blockCommentStart) {
                            regions.add(new FoldRegion(
                                blockCommentStart,
                                lineIndex,
                                FoldKind.BLOCK_COMMENT,
                                1,
                                false
                            ));
                        }
                        blockCommentStart = -1;
                        mode = Mode.CODE;
                        i++;
                    }
                    continue;
                }
                if (mode == Mode.SINGLE_QUOTE) {
                    if (escaped) {
                        escaped = false;
                        continue;
                    }
                    if (current == '\\') {
                        escaped = true;
                        continue;
                    }
                    if (current == '\'') {
                        mode = Mode.CODE;
                    }
                    continue;
                }
                if (mode == Mode.DOUBLE_QUOTE) {
                    if (escaped) {
                        escaped = false;
                        continue;
                    }
                    if (current == '\\') {
                        escaped = true;
                        continue;
                    }
                    if (current == '"') {
                        mode = Mode.CODE;
                    }
                    continue;
                }
                if (mode == Mode.TEXT_BLOCK) {
                    if (current == '"' && next == '"' && next2 == '"') {
                        if (lineIndex > textBlockStart) {
                            regions.add(new FoldRegion(
                                textBlockStart,
                                lineIndex,
                                FoldKind.JAVA_TEXT_BLOCK,
                                1,
                                false
                            ));
                        }
                        textBlockStart = -1;
                        mode = Mode.CODE;
                        i += 2;
                    }
                    continue;
                }
                if (current == '/' && next == '/') {
                    break;
                }
                if (current == '/' && next == '*') {
                    mode = Mode.BLOCK_COMMENT;
                    blockCommentStart = lineIndex;
                    i++;
                    continue;
                }
                if (current == '"' && next == '"' && next2 == '"') {
                    mode = Mode.TEXT_BLOCK;
                    textBlockStart = lineIndex;
                    i += 2;
                    continue;
                }
                if (current == '\'') {
                    mode = Mode.SINGLE_QUOTE;
                    escaped = false;
                    continue;
                }
                if (current == '"') {
                    mode = Mode.DOUBLE_QUOTE;
                    escaped = false;
                    continue;
                }
                if (current == '{') {
                    braceStack.push(new OpenBlock(lineIndex, braceStack.size() + 1));
                    continue;
                }
                if (current == '}' && !braceStack.isEmpty()) {
                    OpenBlock open = braceStack.pop();
                    if (lineIndex > open.line()) {
                        regions.add(new FoldRegion(
                            open.line(),
                            lineIndex,
                            FoldKind.BRACE_BLOCK,
                            open.depth(),
                            false
                        ));
                    }
                }
            }
        }
        int lastLine = Math.max(0, safeLines.size() - 1);
        if (mode == Mode.BLOCK_COMMENT && blockCommentStart >= 0 && lastLine > blockCommentStart) {
            regions.add(new FoldRegion(blockCommentStart, lastLine, FoldKind.BLOCK_COMMENT, 1, false));
        }
        if (mode == Mode.TEXT_BLOCK && textBlockStart >= 0 && lastLine > textBlockStart) {
            regions.add(new FoldRegion(textBlockStart, lastLine, FoldKind.JAVA_TEXT_BLOCK, 1, false));
        }
        FoldMap baseMap = new FoldMap(regions);
        Set<Integer> preserved = baseline == null ? Set.of() : baseline.collapsedHeaderLines();
        return baseMap.withCollapsedHeaders(preserved);
    }

    private record OpenBlock(int line, int depth) {
    }
}
