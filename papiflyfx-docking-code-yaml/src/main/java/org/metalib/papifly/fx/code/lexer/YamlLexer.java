package org.metalib.papifly.fx.code.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Best-effort YAML language lexer for configuration-oriented documents.
 */
public final class YamlLexer implements Lexer {

    private static final int STATE_DEFAULT = 0;
    private static final int STATE_DOUBLE_QUOTED = 1;
    private static final int STATE_SINGLE_QUOTED = 2;
    private static final int STATE_BLOCK_SCALAR = 3;

    /**
     * Stable id for YAML language.
     */
    public static final String LANGUAGE_ID = "yaml";
    /**
     * Syntax style scope for YAML mapping keys.
     */
    public static final String SCOPE_YAML_KEY = "yaml.key";
    /**
     * Syntax style scope for YAML anchors.
     */
    public static final String SCOPE_YAML_ANCHOR = "yaml.anchor";
    /**
     * Syntax style scope for YAML aliases.
     */
    public static final String SCOPE_YAML_ALIAS = "yaml.alias";
    /**
     * Syntax style scope for YAML tags.
     */
    public static final String SCOPE_YAML_TAG = "yaml.tag";

    /**
     * Creates a YAML lexer instance.
     */
    public YamlLexer() {
    }

    @Override
    public String languageId() {
        return LANGUAGE_ID;
    }

    @Override
    public LexResult lexLine(String lineText, LexState entryState) {
        String text = lineText == null ? "" : lineText;
        List<Token> tokens = new ArrayList<>();
        int state = entryState == null ? STATE_DEFAULT : entryState.code();
        BlockScalarState blockScalarState = state == STATE_BLOCK_SCALAR
            ? blockScalarState(entryState)
            : null;
        int index = 0;

        if (state == STATE_BLOCK_SCALAR) {
            BlockScalarLine continuation = continueBlockScalar(text, blockScalarState);
            if (continuation.inScalar()) {
                addToken(tokens, 0, text.length(), TokenType.PLAIN);
                return new LexResult(tokens, blockScalarLexState(continuation.state()));
            }
            state = STATE_DEFAULT;
            blockScalarState = null;
        }

        if (state == STATE_DOUBLE_QUOTED) {
            StringContinuation continuation = continueDoubleQuoted(text, false);
            addToken(tokens, 0, continuation.endIndex(), TokenType.STRING);
            index = continuation.endIndex();
            if (!continuation.closed()) {
                return new LexResult(tokens, LexState.of(STATE_DOUBLE_QUOTED));
            }
            state = STATE_DEFAULT;
        } else if (state == STATE_SINGLE_QUOTED) {
            StringContinuation continuation = continueSingleQuoted(text, false);
            addToken(tokens, 0, continuation.endIndex(), TokenType.STRING);
            index = continuation.endIndex();
            if (!continuation.closed()) {
                return new LexResult(tokens, LexState.of(STATE_SINGLE_QUOTED));
            }
            state = STATE_DEFAULT;
        }

        if (isDocumentMarker(text)) {
            addToken(tokens, 0, text.length(), TokenType.PUNCTUATION);
            return new LexResult(tokens, LexState.DEFAULT);
        }

        while (index < text.length()) {
            char ch = text.charAt(index);
            if (Character.isWhitespace(ch)) {
                index++;
                continue;
            }
            if (ch == '#') {
                addToken(tokens, index, text.length(), TokenType.COMMENT);
                break;
            }
            if (ch == '"') {
                StringContinuation continuation = continueDoubleQuoted(text.substring(index), true);
                int tokenEnd = index + continuation.endIndex();
                addToken(tokens, index, tokenEnd, TokenType.STRING);
                if (!continuation.closed()) {
                    state = STATE_DOUBLE_QUOTED;
                    break;
                }
                index = tokenEnd;
                continue;
            }
            if (ch == '\'') {
                StringContinuation continuation = continueSingleQuoted(text.substring(index), true);
                int tokenEnd = index + continuation.endIndex();
                addToken(tokens, index, tokenEnd, TokenType.STRING);
                if (!continuation.closed()) {
                    state = STATE_SINGLE_QUOTED;
                    break;
                }
                index = tokenEnd;
                continue;
            }
            if (ch == '&' || ch == '*') {
                int end = scanAnchorOrAlias(text, index);
                if (end > index + 1) {
                    addToken(tokens, index, end, TokenType.IDENTIFIER,
                        ch == '&' ? SCOPE_YAML_ANCHOR : SCOPE_YAML_ALIAS);
                    index = end;
                    continue;
                }
            }
            if (ch == '!') {
                int end = scanTag(text, index);
                if (end > index + 1) {
                    addToken(tokens, index, end, TokenType.IDENTIFIER, SCOPE_YAML_TAG);
                    index = end;
                    continue;
                }
            }
            if (startsWith(text, index, "<<") && mappingColonAfter(text, index + 2) >= 0) {
                addToken(tokens, index, index + 2, TokenType.OPERATOR);
                index += 2;
                continue;
            }
            if (ch == '-' && index + 1 < text.length() && Character.isWhitespace(text.charAt(index + 1))) {
                addToken(tokens, index, index + 1, TokenType.PUNCTUATION);
                index++;
                continue;
            }
            int keyEnd = scanYamlKey(text, index);
            if (keyEnd > index) {
                addToken(tokens, index, keyEnd, TokenType.IDENTIFIER, SCOPE_YAML_KEY);
                index = keyEnd;
                continue;
            }
            BlockScalarIndicator blockScalar = scanBlockScalarIndicator(text, index);
            if (blockScalar != null) {
                addToken(tokens, index, blockScalar.endIndex(), TokenType.PUNCTUATION);
                state = STATE_BLOCK_SCALAR;
                blockScalarState = new BlockScalarState(
                    leadingSpaces(text),
                    blockScalar.contentIndent(),
                    blockScalar.style(),
                    blockScalar.chomping()
                );
                index = blockScalar.endIndex();
                continue;
            }
            int numberEnd = scanNumber(text, index);
            if (numberEnd > index) {
                addToken(tokens, index, numberEnd, TokenType.NUMBER);
                index = numberEnd;
                continue;
            }
            if (ch == '~' && isValueBoundary(text, index + 1)) {
                addToken(tokens, index, index + 1, TokenType.NULL_LITERAL);
                index++;
                continue;
            }
            if (Character.isLetter(ch)) {
                int wordEnd = scanWord(text, index);
                if (isValueBoundary(text, wordEnd)) {
                    TokenType scalarType = scalarWordType(text.substring(index, wordEnd));
                    if (scalarType != null) {
                        addToken(tokens, index, wordEnd, scalarType);
                        index = wordEnd;
                        continue;
                    }
                }
            }
            if ("{}[]:,".indexOf(ch) >= 0) {
                addToken(tokens, index, index + 1, TokenType.PUNCTUATION);
                index++;
                continue;
            }
            int plainEnd = scanPlainScalar(text, index);
            addToken(tokens, index, plainEnd, TokenType.PLAIN);
            index = Math.max(index + 1, plainEnd);
        }

        return new LexResult(tokens, state == STATE_BLOCK_SCALAR
            ? blockScalarLexState(blockScalarState)
            : LexState.of(state));
    }

    private static boolean isDocumentMarker(String text) {
        return "---".equals(text) || "...".equals(text);
    }

    private static StringContinuation continueDoubleQuoted(String text, boolean hasOpeningQuoteAtStart) {
        boolean escaped = false;
        int scanStart = hasOpeningQuoteAtStart ? 1 : 0;
        for (int i = scanStart; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                return new StringContinuation(i + 1, true);
            }
        }
        return new StringContinuation(text.length(), false);
    }

    private static StringContinuation continueSingleQuoted(String text, boolean hasOpeningQuoteAtStart) {
        int scanStart = hasOpeningQuoteAtStart ? 1 : 0;
        for (int i = scanStart; i < text.length(); i++) {
            if (text.charAt(i) != '\'') {
                continue;
            }
            if (i + 1 < text.length() && text.charAt(i + 1) == '\'') {
                i++;
                continue;
            }
            return new StringContinuation(i + 1, true);
        }
        return new StringContinuation(text.length(), false);
    }

    private static int scanAnchorOrAlias(String text, int start) {
        int index = start + 1;
        while (index < text.length() && isNameChar(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private static int scanTag(String text, int start) {
        int index = start + 1;
        if (index < text.length() && text.charAt(index) == '!') {
            index++;
        }
        while (index < text.length()) {
            char ch = text.charAt(index);
            if (Character.isWhitespace(ch) || "{}[],:#".indexOf(ch) >= 0) {
                break;
            }
            index++;
        }
        return index;
    }

    private static int scanYamlKey(String text, int start) {
        if (!isKeyChar(text.charAt(start))) {
            return -1;
        }
        int end = start + 1;
        while (end < text.length() && isKeyChar(text.charAt(end))) {
            end++;
        }
        int colon = mappingColonAfter(text, end);
        return colon >= 0 ? end : -1;
    }

    private static int mappingColonAfter(String text, int index) {
        int cursor = index;
        while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) {
            cursor++;
        }
        if (cursor >= text.length() || text.charAt(cursor) != ':') {
            return -1;
        }
        int afterColon = cursor + 1;
        if (afterColon >= text.length()) {
            return cursor;
        }
        char next = text.charAt(afterColon);
        return Character.isWhitespace(next) || next == '#' || next == ',' || next == '}' || next == ']'
            ? cursor
            : -1;
    }

    private static BlockScalarLine continueBlockScalar(String text, BlockScalarState state) {
        BlockScalarState effectiveState = state == null
            ? new BlockScalarState(0, 1, '|', '\0')
            : state;
        if (text.isBlank()) {
            return new BlockScalarLine(true, effectiveState);
        }
        int indent = leadingSpaces(text);
        int contentIndent = effectiveState.contentIndent();
        if (contentIndent < 0) {
            if (indent <= effectiveState.headerIndent()) {
                return new BlockScalarLine(false, effectiveState);
            }
            contentIndent = indent;
        }
        if (indent < contentIndent) {
            return new BlockScalarLine(false, effectiveState);
        }
        return new BlockScalarLine(true, new BlockScalarState(
            effectiveState.headerIndent(),
            contentIndent,
            effectiveState.style(),
            effectiveState.chomping()
        ));
    }

    private static LexState blockScalarLexState(BlockScalarState state) {
        BlockScalarState effectiveState = state == null
            ? new BlockScalarState(0, 1, '|', '\0')
            : state;
        return LexState.blockScalar(
            STATE_BLOCK_SCALAR,
            effectiveState.headerIndent(),
            effectiveState.contentIndent(),
            effectiveState.style(),
            effectiveState.chomping()
        );
    }

    private static BlockScalarState blockScalarState(LexState state) {
        if (state == null) {
            return new BlockScalarState(0, 1, '|', '\0');
        }
        int headerIndent = state.blockScalarHeaderIndent() >= 0 ? state.blockScalarHeaderIndent() : 0;
        int contentIndent = state.blockScalarContentIndent();
        if (contentIndent < 0 && state.blockScalarHeaderIndent() < 0) {
            contentIndent = 1;
        }
        char style = state.blockScalarStyle() == '\0' ? '|' : state.blockScalarStyle();
        return new BlockScalarState(headerIndent, contentIndent, style, state.blockScalarChomping());
    }

    private static BlockScalarIndicator scanBlockScalarIndicator(String text, int start) {
        char ch = text.charAt(start);
        if (ch != '|' && ch != '>') {
            return null;
        }
        int index = start + 1;
        char chomping = '\0';
        int indentIndicator = -1;
        while (index < text.length()) {
            char indicator = text.charAt(index);
            if ((indicator == '+' || indicator == '-') && chomping == '\0') {
                chomping = indicator;
                index++;
                continue;
            }
            if (indicator >= '1' && indicator <= '9' && indentIndicator < 0) {
                indentIndicator = indicator - '0';
                index++;
                continue;
            }
            break;
        }
        if (index >= text.length() || Character.isWhitespace(text.charAt(index)) || text.charAt(index) == '#') {
            int contentIndent = indentIndicator < 0 ? -1 : leadingSpaces(text) + indentIndicator;
            return new BlockScalarIndicator(index, ch, chomping, contentIndent);
        }
        return null;
    }

    private record BlockScalarIndicator(int endIndex, char style, char chomping, int contentIndent) {
    }

    private record BlockScalarLine(boolean inScalar, BlockScalarState state) {
    }

    private record BlockScalarState(int headerIndent, int contentIndent, char style, char chomping) {
    }

    private static int scanNumber(String text, int start) {
        int specialEnd = scanSpecialFloat(text, start);
        if (specialEnd > start) {
            return specialEnd;
        }
        int index = start;
        if (text.charAt(index) == '-' || text.charAt(index) == '+') {
            index++;
        }
        if (index >= text.length()) {
            return -1;
        }
        if (startsWithIgnoreCase(text, index, "0x")) {
            return scanRadixNumber(text, index + 2, 16) ? scanRadixEnd(text, index + 2, 16) : -1;
        }
        if (startsWithIgnoreCase(text, index, "0o")) {
            return scanRadixNumber(text, index + 2, 8) ? scanRadixEnd(text, index + 2, 8) : -1;
        }
        int digitStart = index;
        while (index < text.length() && Character.isDigit(text.charAt(index))) {
            index++;
        }
        boolean hasDigits = index > digitStart;
        if (index < text.length() && text.charAt(index) == '.') {
            int fractionStart = ++index;
            while (index < text.length() && Character.isDigit(text.charAt(index))) {
                index++;
            }
            hasDigits = hasDigits || index > fractionStart;
        }
        if (!hasDigits) {
            return -1;
        }
        if (index < text.length() && (text.charAt(index) == 'e' || text.charAt(index) == 'E')) {
            int exponentStart = index;
            index++;
            if (index < text.length() && (text.charAt(index) == '+' || text.charAt(index) == '-')) {
                index++;
            }
            int exponentDigitStart = index;
            while (index < text.length() && Character.isDigit(text.charAt(index))) {
                index++;
            }
            if (exponentDigitStart == index) {
                index = exponentStart;
            }
        }
        return isValueBoundary(text, index) ? index : -1;
    }

    private static int scanSpecialFloat(String text, int start) {
        int index = start;
        if (text.charAt(index) == '-' || text.charAt(index) == '+') {
            index++;
        }
        if (startsWithIgnoreCase(text, index, ".inf")) {
            int end = index + 4;
            return isValueBoundary(text, end) ? end : -1;
        }
        if (startsWithIgnoreCase(text, index, ".nan")) {
            int end = index + 4;
            return isValueBoundary(text, end) ? end : -1;
        }
        return -1;
    }

    private static boolean scanRadixNumber(String text, int start, int radix) {
        int index = start;
        while (index < text.length() && Character.digit(text.charAt(index), radix) >= 0) {
            index++;
        }
        return index > start && isValueBoundary(text, index);
    }

    private static int scanRadixEnd(String text, int start, int radix) {
        int index = start;
        while (index < text.length() && Character.digit(text.charAt(index), radix) >= 0) {
            index++;
        }
        return index;
    }

    private static int scanWord(String text, int start) {
        int index = start + 1;
        while (index < text.length() && Character.isLetter(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private static TokenType scalarWordType(String word) {
        String lower = word.toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "true", "false", "yes", "no", "on", "off" -> TokenType.BOOLEAN;
            case "null" -> TokenType.NULL_LITERAL;
            default -> null;
        };
    }

    private static int scanPlainScalar(String text, int start) {
        int index = start;
        while (index < text.length()) {
            char ch = text.charAt(index);
            if (ch == '#' || ch == ',' || "{}[]".indexOf(ch) >= 0) {
                break;
            }
            if (ch == ':' && index + 1 < text.length() && Character.isWhitespace(text.charAt(index + 1))) {
                break;
            }
            index++;
        }
        while (index > start && Character.isWhitespace(text.charAt(index - 1))) {
            index--;
        }
        return index;
    }

    private static boolean isValueBoundary(String text, int index) {
        if (index >= text.length()) {
            return true;
        }
        char ch = text.charAt(index);
        return Character.isWhitespace(ch) || "{}[],:#".indexOf(ch) >= 0;
    }

    private static int leadingSpaces(String text) {
        int index = 0;
        while (index < text.length() && text.charAt(index) == ' ') {
            index++;
        }
        return index;
    }

    private static boolean isKeyChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_' || ch == '-' || ch == '.' || ch == '/';
    }

    private static boolean isNameChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_' || ch == '-' || ch == '.';
    }

    private static boolean startsWith(String text, int index, String prefix) {
        return text.regionMatches(index, prefix, 0, prefix.length());
    }

    private static boolean startsWithIgnoreCase(String text, int index, String prefix) {
        return text.regionMatches(true, index, prefix, 0, prefix.length());
    }

    private static void addToken(List<Token> tokens, int start, int end, TokenType type) {
        addToken(tokens, start, end, type, null);
    }

    private static void addToken(List<Token> tokens, int start, int end, TokenType type, String styleScope) {
        if (end <= start) {
            return;
        }
        tokens.add(new Token(start, end - start, type, styleScope));
    }

    private record StringContinuation(int endIndex, boolean closed) {
    }
}
