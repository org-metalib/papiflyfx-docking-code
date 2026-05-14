package org.metalib.papifly.fx.code.folding;

import org.metalib.papifly.fx.code.lexer.TokenMap;
import org.metalib.papifly.fx.code.lexer.YamlLexer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

final class YamlFoldProvider implements FoldProvider {

    @Override
    public String languageId() {
        return YamlLexer.LANGUAGE_ID;
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
        List<LineInfo> infos = new ArrayList<>(safeLines.size());
        for (String line : safeLines) {
            infos.add(classify(line));
        }

        List<FoldRegion> regions = new ArrayList<>();
        for (int lineIndex = 0; lineIndex < infos.size(); lineIndex++) {
            if (cancelled != null && cancelled.getAsBoolean()) {
                return baseline == null ? FoldMap.empty() : baseline;
            }
            LineInfo info = infos.get(lineIndex);
            if (info.kind() == LineKind.BLOCK_SCALAR_HEADER) {
                addBlockScalarRegion(regions, infos, lineIndex, info.indent());
                continue;
            }
            if (info.kind() == LineKind.MAPPING_HEADER || info.kind() == LineKind.SEQUENCE_HEADER) {
                addIndentedRegion(regions, infos, lineIndex, info.indent());
            }
        }
        addFlowRegions(regions, safeLines, cancelled, baseline);

        FoldMap baseMap = new FoldMap(regions);
        Set<Integer> preserved = baseline == null ? Set.of() : baseline.collapsedHeaderLines();
        return baseMap.withCollapsedHeaders(preserved);
    }

    private static void addIndentedRegion(List<FoldRegion> regions, List<LineInfo> infos, int startLine, int indent) {
        int endLine = -1;
        for (int i = startLine + 1; i < infos.size(); i++) {
            LineInfo candidate = infos.get(i);
            if (candidate.kind() == LineKind.BLANK || candidate.kind() == LineKind.COMMENT) {
                continue;
            }
            if (candidate.indent() <= indent) {
                break;
            }
            endLine = i;
        }
        if (endLine > startLine) {
            regions.add(new FoldRegion(startLine, endLine, FoldKind.YAML_MAPPING, depthForIndent(indent), false));
        }
    }

    private static void addBlockScalarRegion(List<FoldRegion> regions, List<LineInfo> infos, int startLine, int indent) {
        int endLine = -1;
        boolean sawBody = false;
        for (int i = startLine + 1; i < infos.size(); i++) {
            LineInfo candidate = infos.get(i);
            if (candidate.kind() == LineKind.BLANK) {
                if (sawBody) {
                    endLine = i;
                }
                continue;
            }
            if (candidate.indent() <= indent) {
                break;
            }
            sawBody = true;
            endLine = i;
        }
        if (endLine > startLine) {
            regions.add(new FoldRegion(startLine, endLine, FoldKind.YAML_BLOCK_SCALAR, depthForIndent(indent), false));
        }
    }

    private static void addFlowRegions(
        List<FoldRegion> regions,
        List<String> lines,
        BooleanSupplier cancelled,
        FoldMap baseline
    ) {
        Deque<FlowStart> stack = new ArrayDeque<>();
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            if (cancelled != null && cancelled.getAsBoolean()) {
                regions.clear();
                regions.addAll(baseline == null ? List.of() : baseline.regions());
                return;
            }
            scanFlowDelimiters(lines.get(lineIndex), lineIndex, stack, regions);
        }
    }

    private static void scanFlowDelimiters(
        String line,
        int lineIndex,
        Deque<FlowStart> stack,
        List<FoldRegion> regions
    ) {
        String text = line == null ? "" : line;
        boolean inSingle = false;
        boolean inDouble = false;
        boolean escaped = false;
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            if (inDouble) {
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (ch == '\\') {
                    escaped = true;
                    continue;
                }
                if (ch == '"') {
                    inDouble = false;
                }
                continue;
            }
            if (inSingle) {
                if (ch == '\'' && index + 1 < text.length() && text.charAt(index + 1) == '\'') {
                    index++;
                    continue;
                }
                if (ch == '\'') {
                    inSingle = false;
                }
                continue;
            }
            if (ch == '#') {
                break;
            }
            if (ch == '"') {
                inDouble = true;
                continue;
            }
            if (ch == '\'') {
                inSingle = true;
                continue;
            }
            if (ch == '{' || ch == '[') {
                stack.push(new FlowStart(lineIndex, ch, stack.size() + 1));
                continue;
            }
            if (ch == '}' || ch == ']') {
                closeFlowRegion(regions, stack, lineIndex, ch);
            }
        }
    }

    private static void closeFlowRegion(
        List<FoldRegion> regions,
        Deque<FlowStart> stack,
        int lineIndex,
        char closing
    ) {
        char expectedOpening = closing == '}' ? '{' : '[';
        while (!stack.isEmpty()) {
            FlowStart start = stack.pop();
            if (start.opening() != expectedOpening) {
                continue;
            }
            if (lineIndex > start.lineIndex()) {
                regions.add(new FoldRegion(start.lineIndex(), lineIndex, FoldKind.YAML_FLOW, start.depth(), false));
            }
            return;
        }
    }

    private static LineInfo classify(String line) {
        String text = line == null ? "" : line;
        String trimmed = text.trim();
        int indent = leadingIndent(text);
        if (trimmed.isEmpty()) {
            return new LineInfo(indent, LineKind.BLANK);
        }
        if (trimmed.startsWith("#")) {
            return new LineInfo(indent, LineKind.COMMENT);
        }
        String uncommented = stripInlineComment(trimmed).stripTrailing();
        if (isBlockScalarHeader(uncommented)) {
            return new LineInfo(indent, LineKind.BLOCK_SCALAR_HEADER);
        }
        if (isMappingHeader(uncommented)) {
            return new LineInfo(indent, LineKind.MAPPING_HEADER);
        }
        if (isSequenceHeader(uncommented)) {
            return new LineInfo(indent, LineKind.SEQUENCE_HEADER);
        }
        return new LineInfo(indent, LineKind.OTHER);
    }

    private static boolean isMappingHeader(String text) {
        int colon = text.indexOf(':');
        if (colon <= 0 || colon != text.length() - 1) {
            return false;
        }
        for (int i = 0; i < colon; i++) {
            char ch = text.charAt(i);
            if (!(Character.isLetterOrDigit(ch) || ch == '_' || ch == '.' || ch == '/' || ch == '-')) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSequenceHeader(String text) {
        if ("-".equals(text)) {
            return true;
        }
        return text.startsWith("- ") || text.startsWith("-\t");
    }

    private static boolean isBlockScalarHeader(String text) {
        return text.endsWith("|")
            || text.endsWith("|+")
            || text.endsWith("|-")
            || text.endsWith(">")
            || text.endsWith(">+")
            || text.endsWith(">-");
    }

    private static String stripInlineComment(String text) {
        boolean inSingle = false;
        boolean inDouble = false;
        boolean escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inDouble) {
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (ch == '\\') {
                    escaped = true;
                    continue;
                }
                if (ch == '"') {
                    inDouble = false;
                }
                continue;
            }
            if (inSingle) {
                if (ch == '\'' && i + 1 < text.length() && text.charAt(i + 1) == '\'') {
                    i++;
                    continue;
                }
                if (ch == '\'') {
                    inSingle = false;
                }
                continue;
            }
            if (ch == '"') {
                inDouble = true;
                continue;
            }
            if (ch == '\'') {
                inSingle = true;
                continue;
            }
            if (ch == '#') {
                return text.substring(0, i);
            }
        }
        return text;
    }

    private static int leadingIndent(String text) {
        int indent = 0;
        while (indent < text.length()) {
            char ch = text.charAt(indent);
            if (ch == ' ') {
                indent++;
                continue;
            }
            if (ch == '\t') {
                indent++;
                continue;
            }
            break;
        }
        return indent;
    }

    private static int depthForIndent(int indent) {
        return Math.max(1, (indent / 2) + 1);
    }

    private enum LineKind {
        BLANK,
        COMMENT,
        MAPPING_HEADER,
        SEQUENCE_HEADER,
        BLOCK_SCALAR_HEADER,
        OTHER
    }

    private record LineInfo(int indent, LineKind kind) {
    }

    private record FlowStart(int lineIndex, char opening, int depth) {
    }
}
