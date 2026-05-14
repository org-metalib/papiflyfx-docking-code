package org.metalib.papifly.fx.code.folding;

import org.metalib.papifly.fx.code.lexer.JavaScriptLexer;
import org.metalib.papifly.fx.code.lexer.TokenMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

final class JavaScriptFoldProvider implements FoldProvider {

    private enum Mode {
        CODE,
        BLOCK_COMMENT,
        SINGLE_QUOTE,
        DOUBLE_QUOTE,
        TEMPLATE
    }

    @Override
    public String languageId() {
        return JavaScriptLexer.LANGUAGE_ID;
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
        Deque<Integer> templateStarts = new ArrayDeque<>();
        Deque<TemplateExpressionState> templateExpressions = new ArrayDeque<>();
        Mode mode = Mode.CODE;
        boolean escaped = false;
        int blockCommentStart = -1;
        for (int lineIndex = 0; lineIndex < safeLines.size(); lineIndex++) {
            if (cancelled != null && cancelled.getAsBoolean()) {
                return baseline == null ? FoldMap.empty() : baseline;
            }
            String text = safeLines.get(lineIndex);
            for (int i = 0; i < text.length(); i++) {
                char current = text.charAt(i);
                char next = i + 1 < text.length() ? text.charAt(i + 1) : '\0';
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
                if (mode == Mode.TEMPLATE) {
                    if (escaped) {
                        escaped = false;
                        continue;
                    }
                    if (current == '\\') {
                        escaped = true;
                        continue;
                    }
                    if (current == '`') {
                        int startLine = templateStarts.isEmpty() ? lineIndex : templateStarts.pop();
                        if (lineIndex > startLine) {
                            regions.add(new FoldRegion(
                                startLine,
                                lineIndex,
                                FoldKind.JS_TEMPLATE_BLOCK,
                                1,
                                false
                            ));
                        }
                        mode = Mode.CODE;
                        continue;
                    }
                    if (current == '$' && next == '{') {
                        templateExpressions.push(new TemplateExpressionState(lineIndex, 1));
                        braceStack.push(new OpenBlock(lineIndex, braceStack.size() + 1));
                        mode = Mode.CODE;
                        i++;
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
                if (current == '`') {
                    templateStarts.push(lineIndex);
                    mode = Mode.TEMPLATE;
                    escaped = false;
                    continue;
                }
                if (current == '{') {
                    braceStack.push(new OpenBlock(lineIndex, braceStack.size() + 1));
                    if (!templateExpressions.isEmpty()) {
                        TemplateExpressionState top = templateExpressions.pop();
                        templateExpressions.push(top.incremented());
                    }
                    continue;
                }
                if (current == '}') {
                    if (!braceStack.isEmpty()) {
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
                    if (!templateExpressions.isEmpty()) {
                        TemplateExpressionState top = templateExpressions.pop().decremented();
                        if (top.braceDepth() <= 0) {
                            if (lineIndex > top.startLine()) {
                                regions.add(new FoldRegion(
                                    top.startLine(),
                                    lineIndex,
                                    FoldKind.JS_TEMPLATE_EXPR,
                                    1,
                                    false
                                ));
                            }
                            mode = Mode.TEMPLATE;
                        } else {
                            templateExpressions.push(top);
                        }
                    }
                }
            }
        }

        int lastLine = Math.max(0, safeLines.size() - 1);
        if (mode == Mode.BLOCK_COMMENT && blockCommentStart >= 0 && lastLine > blockCommentStart) {
            regions.add(new FoldRegion(blockCommentStart, lastLine, FoldKind.BLOCK_COMMENT, 1, false));
        }
        while (!templateStarts.isEmpty()) {
            int startLine = templateStarts.pop();
            if (lastLine > startLine) {
                regions.add(new FoldRegion(startLine, lastLine, FoldKind.JS_TEMPLATE_BLOCK, 1, false));
            }
        }
        while (!templateExpressions.isEmpty()) {
            TemplateExpressionState state = templateExpressions.pop();
            if (lastLine > state.startLine()) {
                regions.add(new FoldRegion(state.startLine(), lastLine, FoldKind.JS_TEMPLATE_EXPR, 1, false));
            }
        }
        FoldMap baseMap = new FoldMap(regions);
        Set<Integer> preserved = baseline == null ? Set.of() : baseline.collapsedHeaderLines();
        return baseMap.withCollapsedHeaders(preserved);
    }

    private record OpenBlock(int line, int depth) {
    }

    private record TemplateExpressionState(int startLine, int braceDepth) {
        private TemplateExpressionState incremented() {
            return new TemplateExpressionState(startLine, braceDepth + 1);
        }

        private TemplateExpressionState decremented() {
            return new TemplateExpressionState(startLine, braceDepth - 1);
        }
    }
}

