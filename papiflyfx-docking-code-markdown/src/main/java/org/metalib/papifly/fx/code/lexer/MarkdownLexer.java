package org.metalib.papifly.fx.code.lexer;

import java.util.ArrayList;
import java.util.List;

/**
 * Markdown language lexer (MVP subset).
 */
public final class MarkdownLexer implements Lexer {

    /**
     * Stable id for Markdown language.
     */
    public static final String LANGUAGE_ID = "markdown";
    /**
     * Syntax style scope for Markdown headings.
     */
    public static final String SCOPE_MARKDOWN_HEADLINE = "markdown.headline";
    /**
     * Syntax style scope for Markdown list item markers.
     */
    public static final String SCOPE_MARKDOWN_LIST_ITEM = "markdown.list-item";
    /**
     * Syntax style scope for Markdown fenced code blocks.
     */
    public static final String SCOPE_MARKDOWN_CODE_BLOCK = "markdown.code-block";

    private static final int STATE_DEFAULT = 0;
    private static final int STATE_CODE_BLOCK = 1;

    /**
     * Creates a Markdown lexer instance.
     */
    public MarkdownLexer() {
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

        if (state == STATE_CODE_BLOCK) {
            if (text.isEmpty()) {
                return new LexResult(tokens, LexState.of(STATE_CODE_BLOCK));
            }
            if (text.trim().startsWith("```")) {
                tokens.add(new Token(0, text.length(), TokenType.TEXT, SCOPE_MARKDOWN_CODE_BLOCK));
                return new LexResult(tokens, LexState.of(STATE_DEFAULT));
            } else {
                tokens.add(new Token(0, text.length(), TokenType.TEXT, SCOPE_MARKDOWN_CODE_BLOCK));
                return new LexResult(tokens, LexState.of(STATE_CODE_BLOCK));
            }
        }

        if (text.isEmpty()) {
            return new LexResult(tokens, LexState.of(STATE_DEFAULT));
        }

        if (text.trim().startsWith("```")) {
            tokens.add(new Token(0, text.length(), TokenType.TEXT, SCOPE_MARKDOWN_CODE_BLOCK));
            return new LexResult(tokens, LexState.of(STATE_CODE_BLOCK));
        }

        if (text.trim().startsWith("#")) {
            tokens.add(new Token(0, text.length(), TokenType.TEXT, SCOPE_MARKDOWN_HEADLINE));
            return new LexResult(tokens, LexState.of(STATE_DEFAULT));
        }

        // List item check
        String trimmed = text.trim();
        int firstNonSpace = 0;
        while (firstNonSpace < text.length() && Character.isWhitespace(text.charAt(firstNonSpace))) {
            firstNonSpace++;
        }

        boolean isUnorderedList = trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ");
        int orderedDigitLen = orderedListDigitLength(trimmed);
        if (isUnorderedList || orderedDigitLen > 0) {
            int markerLen = isUnorderedList ? 2 : orderedDigitLen + 2; // digits + ". "
            int markerEnd = firstNonSpace + markerLen;

            tokens.add(new Token(
                firstNonSpace,
                markerEnd - firstNonSpace,
                TokenType.TEXT,
                SCOPE_MARKDOWN_LIST_ITEM
            ));
            if (markerEnd < text.length()) {
                lexText(text, markerEnd, tokens);
            }
            return new LexResult(tokens, LexState.of(STATE_DEFAULT));
        }

        lexText(text, 0, tokens);
        return new LexResult(tokens, LexState.of(STATE_DEFAULT));
    }

    /**
     * Returns the number of leading digits if trimmed starts with "digits. " (ordered list marker),
     * or 0 if not an ordered list item.
     */
    private static int orderedListDigitLength(String trimmed) {
        int i = 0;
        while (i < trimmed.length() && Character.isDigit(trimmed.charAt(i))) {
            i++;
        }
        if (i == 0) {
            return 0;
        }
        // Require ". " after digits
        if (i + 1 < trimmed.length() && trimmed.charAt(i) == '.' && trimmed.charAt(i + 1) == ' ') {
            return i;
        }
        return 0;
    }

    private void lexText(String text, int start, List<Token> tokens) {
        int index = start;
        while (index < text.length()) {
            char ch = text.charAt(index);
            if (isPunctuation(ch)) {
                tokens.add(new Token(index, 1, TokenType.PUNCTUATION));
                index++;
            } else {
                int nextPunc = index;
                while (nextPunc < text.length() && !isPunctuation(text.charAt(nextPunc))) {
                    nextPunc++;
                }
                tokens.add(new Token(index, nextPunc - index, TokenType.TEXT));
                index = nextPunc;
            }
        }
    }

    private boolean isPunctuation(char ch) {
        return "*_[]()<>`!#".indexOf(ch) >= 0;
    }
}
