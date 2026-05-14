package org.metalib.papifly.fx.code.lexer;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON language lexer.
 */
public final class JsonLexer implements Lexer {

    private static final int STATE_DEFAULT = 0;
    private static final int STATE_STRING = 1;

    /**
     * Stable id for JSON language.
     */
    public static final String LANGUAGE_ID = "json";
    /**
     * Syntax style scope for JSON object keys.
     */
    public static final String SCOPE_JSON_KEY = "json.key";

    /**
     * Creates a JSON lexer instance.
     */
    public JsonLexer() {
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
        int index = 0;

        if (state == STATE_STRING) {
            StringContinuation continuation = continueString(text, false);
            addToken(tokens, 0, continuation.endIndex(), TokenType.STRING);
            index = continuation.endIndex();
            if (!continuation.closed()) {
                return new LexResult(tokens, LexState.of(STATE_STRING));
            }
            state = STATE_DEFAULT;
        }

        while (index < text.length()) {
            char ch = text.charAt(index);
            if (Character.isWhitespace(ch)) {
                index++;
                continue;
            }
            if (ch == '"') {
                StringContinuation continuation = continueString(text.substring(index), true);
                int tokenEnd = index + continuation.endIndex();
                boolean objectKey = continuation.closed() && isObjectKey(text, tokenEnd);
                addToken(tokens, index, tokenEnd, TokenType.STRING, objectKey ? SCOPE_JSON_KEY : null);
                if (!continuation.closed()) {
                    state = STATE_STRING;
                    break;
                }
                index = tokenEnd;
                continue;
            }
            if (isNumberStart(text, index)) {
                int end = scanNumber(text, index);
                addToken(tokens, index, end, TokenType.NUMBER);
                index = end;
                continue;
            }
            if (Character.isLetter(ch)) {
                int end = scanWord(text, index);
                String word = text.substring(index, end);
                if ("true".equals(word) || "false".equals(word)) {
                    addToken(tokens, index, end, TokenType.BOOLEAN);
                } else if ("null".equals(word)) {
                    addToken(tokens, index, end, TokenType.NULL_LITERAL);
                }
                index = end;
                continue;
            }
            if ("{}[],:".indexOf(ch) >= 0) {
                addToken(tokens, index, index + 1, TokenType.PUNCTUATION);
                index++;
                continue;
            }
            index++;
        }

        return new LexResult(tokens, LexState.of(state));
    }

    private StringContinuation continueString(String text, boolean hasOpeningQuoteAtStart) {
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

    private boolean isObjectKey(String text, int afterStringIndex) {
        int index = afterStringIndex;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index < text.length() && text.charAt(index) == ':';
    }

    private boolean isNumberStart(String text, int index) {
        char ch = text.charAt(index);
        if (Character.isDigit(ch)) {
            return true;
        }
        return ch == '-' && index + 1 < text.length() && Character.isDigit(text.charAt(index + 1));
    }

    private int scanWord(String text, int start) {
        int index = start + 1;
        while (index < text.length() && Character.isLetter(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private int scanNumber(String text, int start) {
        int index = start;
        if (text.charAt(index) == '-') {
            index++;
        }
        while (index < text.length() && Character.isDigit(text.charAt(index))) {
            index++;
        }
        if (index < text.length() && text.charAt(index) == '.') {
            index++;
            while (index < text.length() && Character.isDigit(text.charAt(index))) {
                index++;
            }
        }
        if (index < text.length() && (text.charAt(index) == 'e' || text.charAt(index) == 'E')) {
            int exponentStart = index;
            index++;
            if (index < text.length() && (text.charAt(index) == '+' || text.charAt(index) == '-')) {
                index++;
            }
            int digitStart = index;
            while (index < text.length() && Character.isDigit(text.charAt(index))) {
                index++;
            }
            if (digitStart == index) {
                index = exponentStart;
            }
        }
        return index;
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
