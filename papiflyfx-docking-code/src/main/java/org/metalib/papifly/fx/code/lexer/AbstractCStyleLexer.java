package org.metalib.papifly.fx.code.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

abstract class AbstractCStyleLexer implements Lexer {

    /**
     * Default lexing state.
     */
    protected static final int STATE_DEFAULT = 0;
    /**
     * Inside block comment state.
     */
    protected static final int STATE_BLOCK_COMMENT = 1;
    /**
     * Inside double-quoted string state.
     */
    protected static final int STATE_DOUBLE_QUOTE = 2;
    /**
     * Inside single-quoted string state.
     */
    protected static final int STATE_SINGLE_QUOTE = 3;
    /**
     * Inside template-quoted string state.
     */
    protected static final int STATE_TEMPLATE_QUOTE = 4;

    private final String languageId;
    private final Set<String> keywords;
    private final boolean templateQuoteEnabled;

    protected AbstractCStyleLexer(String languageId, Set<String> keywords, boolean templateQuoteEnabled) {
        this.languageId = languageId;
        this.keywords = keywords;
        this.templateQuoteEnabled = templateQuoteEnabled;
    }

    @Override
    public String languageId() {
        return languageId;
    }

    @Override
    public LexResult lexLine(String lineText, LexState entryState) {
        String text = lineText == null ? "" : lineText;
        List<Token> tokens = new ArrayList<>();
        int state = entryState == null ? STATE_DEFAULT : entryState.code();
        int index = 0;

        if (state == STATE_BLOCK_COMMENT) {
            int end = text.indexOf("*/");
            if (end < 0) {
                addToken(tokens, 0, text.length(), TokenType.COMMENT);
                return new LexResult(tokens, LexState.of(STATE_BLOCK_COMMENT));
            }
            addToken(tokens, 0, end + 2, TokenType.COMMENT);
            index = end + 2;
            state = STATE_DEFAULT;
        } else if (state == STATE_DOUBLE_QUOTE || state == STATE_SINGLE_QUOTE || state == STATE_TEMPLATE_QUOTE) {
            StringContinuation continuation = continueString(text, state, false);
            addToken(tokens, 0, continuation.endIndex(), TokenType.STRING);
            index = continuation.endIndex();
            state = continuation.closed() ? STATE_DEFAULT : state;
            if (!continuation.closed()) {
                return new LexResult(tokens, LexState.of(state));
            }
        }

        while (index < text.length()) {
            char ch = text.charAt(index);
            if (Character.isWhitespace(ch)) {
                index++;
                continue;
            }

            if (matches(text, index, "//")) {
                addToken(tokens, index, text.length(), TokenType.COMMENT);
                break;
            }
            if (matches(text, index, "/*")) {
                int end = text.indexOf("*/", index + 2);
                if (end < 0) {
                    addToken(tokens, index, text.length(), TokenType.COMMENT);
                    state = STATE_BLOCK_COMMENT;
                    break;
                }
                addToken(tokens, index, end + 2, TokenType.COMMENT);
                index = end + 2;
                continue;
            }

            if (isQuoteStart(ch)) {
                int quoteState = stateForQuote(ch);
                StringContinuation continuation = continueString(text.substring(index), quoteState, true);
                int tokenEnd = index + continuation.endIndex();
                addToken(tokens, index, tokenEnd, TokenType.STRING);
                if (!continuation.closed()) {
                    state = quoteState;
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

            if (isIdentifierStart(ch)) {
                int end = scanIdentifier(text, index);
                TokenType type = classifyWord(text.substring(index, end));
                if (type != TokenType.IDENTIFIER && type != TokenType.PLAIN) {
                    addToken(tokens, index, end, type);
                }
                index = end;
                continue;
            }

            if (isPunctuation(ch)) {
                addToken(tokens, index, index + 1, TokenType.PUNCTUATION);
                index++;
                continue;
            }

            if (isOperator(ch)) {
                addToken(tokens, index, index + 1, TokenType.OPERATOR);
                index++;
                continue;
            }

            index++;
        }

        return new LexResult(tokens, LexState.of(state));
    }

    /**
     * Classifies an identifier token candidate.
     *
     * @param word candidate word token
     * @return token classification for the word
     */
    protected TokenType classifyWord(String word) {
        if ("true".equals(word) || "false".equals(word)) {
            return TokenType.BOOLEAN;
        }
        if ("null".equals(word)) {
            return TokenType.NULL_LITERAL;
        }
        if (keywords.contains(word)) {
            return TokenType.KEYWORD;
        }
        return TokenType.IDENTIFIER;
    }

    private boolean matches(String text, int index, String match) {
        if (index + match.length() > text.length()) {
            return false;
        }
        return text.regionMatches(index, match, 0, match.length());
    }

    private StringContinuation continueString(String text, int state, boolean hasOpeningQuoteAtStart) {
        char quote = quoteForState(state);
        boolean escaped = false;
        int scanStart = hasOpeningQuoteAtStart ? 1 : 0;
        for (int i = scanStart; i < text.length(); i++) {
            char current = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == quote) {
                return new StringContinuation(i + 1, true);
            }
        }
        return new StringContinuation(text.length(), false);
    }

    private boolean isQuoteStart(char ch) {
        return ch == '"' || ch == '\'' || (templateQuoteEnabled && ch == '`');
    }

    private int stateForQuote(char quote) {
        return switch (quote) {
            case '"' -> STATE_DOUBLE_QUOTE;
            case '\'' -> STATE_SINGLE_QUOTE;
            case '`' -> STATE_TEMPLATE_QUOTE;
            default -> STATE_DEFAULT;
        };
    }

    private char quoteForState(int state) {
        return switch (state) {
            case STATE_DOUBLE_QUOTE -> '"';
            case STATE_SINGLE_QUOTE -> '\'';
            case STATE_TEMPLATE_QUOTE -> '`';
            default -> '"';
        };
    }

    private boolean isIdentifierStart(char ch) {
        return Character.isLetter(ch) || ch == '_' || ch == '$';
    }

    private int scanIdentifier(String text, int start) {
        int index = start + 1;
        while (index < text.length()) {
            char ch = text.charAt(index);
            if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '$') {
                index++;
                continue;
            }
            break;
        }
        return index;
    }

    private boolean isNumberStart(String text, int index) {
        char ch = text.charAt(index);
        if (Character.isDigit(ch)) {
            return true;
        }
        return ch == '-' && index + 1 < text.length() && Character.isDigit(text.charAt(index + 1));
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

    private boolean isPunctuation(char ch) {
        return "{}[]().,;".indexOf(ch) >= 0;
    }

    private boolean isOperator(char ch) {
        return "+-*/%=&|!<>?:^~".indexOf(ch) >= 0;
    }

    private static void addToken(List<Token> tokens, int start, int end, TokenType type) {
        if (end <= start) {
            return;
        }
        tokens.add(new Token(start, end - start, type));
    }

    private record StringContinuation(int endIndex, boolean closed) {
    }
}
