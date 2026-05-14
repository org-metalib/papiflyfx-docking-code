package org.metalib.papifly.fx.code.search;

import org.metalib.papifly.fx.code.document.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Search/replace model that finds matches in a document.
 * Supports plain text and regex modes, case-sensitive and case-insensitive,
 * and whole-word matching.
 */
public class SearchModel {

    private String query = "";
    private String replacement = "";
    private boolean regexMode;
    private boolean caseSensitive;
    private boolean wholeWord;
    private boolean preserveCase;
    private boolean searchInSelection;
    private int selectionStartOffset = -1;
    private int selectionEndOffset = -1;
    private List<SearchMatch> matches = List.of();
    private int currentMatchIndex = -1;
    private Pattern cachedPattern;
    private String cachedPatternQuery = "";
    private boolean cachedPatternCaseSensitive;
    private boolean cachedPatternWholeWord;
    private boolean cachedPatternInitialized;

    /**
     * Creates an empty search model.
     */
    public SearchModel() {
        // Default constructor.
    }

    /**
     * Returns the current search query.
     *
     * @return search query string
     */
    public String getQuery() {
        return query;
    }

    /**
     * Sets the search query.
     *
     * @param query search query text
     */
    public void setQuery(String query) {
        this.query = query == null ? "" : query;
    }

    /**
     * Returns the replacement string.
     *
     * @return replacement string
     */
    public String getReplacement() {
        return replacement;
    }

    /**
     * Sets the replacement string.
     *
     * @param replacement replacement text
     */
    public void setReplacement(String replacement) {
        this.replacement = replacement == null ? "" : replacement;
    }

    /**
     * Returns true if regex mode is enabled.
     *
     * @return {@code true} when regex mode is enabled
     */
    public boolean isRegexMode() {
        return regexMode;
    }

    /**
     * Enables or disables regex mode.
     *
     * @param regexMode {@code true} to enable regex mode
     */
    public void setRegexMode(boolean regexMode) {
        this.regexMode = regexMode;
    }

    /**
     * Returns true if case-sensitive search is enabled.
     *
     * @return {@code true} when case-sensitive matching is enabled
     */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    /**
     * Enables or disables case-sensitive search.
     *
     * @param caseSensitive {@code true} to enable case-sensitive search
     */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    /**
     * Returns true if whole-word matching is enabled.
     *
     * @return {@code true} when whole-word matching is enabled
     */
    public boolean isWholeWord() {
        return wholeWord;
    }

    /**
     * Enables or disables whole-word matching.
     *
     * @param wholeWord {@code true} to enable whole-word matching
     */
    public void setWholeWord(boolean wholeWord) {
        this.wholeWord = wholeWord;
    }

    /**
     * Returns true if replacement should preserve the case pattern of each match.
     *
     * @return {@code true} when preserve-case replacement is enabled
     */
    public boolean isPreserveCase() {
        return preserveCase;
    }

    /**
     * Enables or disables preserve-case replacement behavior.
     *
     * @param preserveCase {@code true} to enable preserve-case replacement
     */
    public void setPreserveCase(boolean preserveCase) {
        this.preserveCase = preserveCase;
    }

    /**
     * Returns true if search should be constrained to the active selection scope.
     *
     * @return {@code true} when search is constrained to selection
     */
    public boolean isSearchInSelection() {
        return searchInSelection;
    }

    /**
     * Enables or disables in-selection search scope.
     *
     * @param searchInSelection {@code true} to search only inside selection scope
     */
    public void setSearchInSelection(boolean searchInSelection) {
        this.searchInSelection = searchInSelection;
    }

    /**
     * Sets the active selection scope using document offsets.
     *
     * @param startOffset one selection boundary offset
     * @param endOffset other selection boundary offset
     */
    public void setSelectionScope(int startOffset, int endOffset) {
        int normalizedStart = Math.max(0, Math.min(startOffset, endOffset));
        int normalizedEnd = Math.max(0, Math.max(startOffset, endOffset));
        this.selectionStartOffset = normalizedStart;
        this.selectionEndOffset = normalizedEnd;
    }

    /**
     * Clears any previously configured selection scope.
     */
    public void clearSelectionScope() {
        this.selectionStartOffset = -1;
        this.selectionEndOffset = -1;
    }

    /**
     * Returns the current list of matches (unmodifiable).
     *
     * @return immutable list of current matches
     */
    public List<SearchMatch> getMatches() {
        return matches;
    }

    /**
     * Returns the index of the currently selected match, or -1 if none.
     *
     * @return current match index, or {@code -1} if no match is selected
     */
    public int getCurrentMatchIndex() {
        return currentMatchIndex;
    }

    /**
     * Returns the currently selected match, or null if none.
     *
     * @return current match, or {@code null} when no match is selected
     */
    public SearchMatch getCurrentMatch() {
        if (currentMatchIndex >= 0 && currentMatchIndex < matches.size()) {
            return matches.get(currentMatchIndex);
        }
        return null;
    }

    /**
     * Returns the total match count.
     *
     * @return number of current matches
     */
    public int getMatchCount() {
        return matches.size();
    }

    /**
     * Executes the search against the given document and populates matches.
     * Returns the number of matches found.
     *
     * @param document document to search
     * @return number of matches found
     */
    public int search(Document document) {
        currentMatchIndex = -1;
        if (query.isEmpty() || document == null) {
            matches = List.of();
            return 0;
        }

        String text = document.getText();
        int[] range = resolveSearchRange(text.length());
        if (range == null) {
            matches = List.of();
            return 0;
        }
        int scopeStart = range[0];
        int scopeEnd = range[1];
        List<SearchMatch> found;

        if (regexMode) {
            found = searchRegex(text, document, scopeStart, scopeEnd);
        } else {
            found = searchPlainText(text, document, scopeStart, scopeEnd);
        }

        matches = Collections.unmodifiableList(found);
        if (!matches.isEmpty()) {
            currentMatchIndex = 0;
        }
        return matches.size();
    }

    /**
     * Advances to the next match. Wraps around to the first match.
     * Returns the new current match, or null if no matches.
     *
     * @return next match, or {@code null} when no matches exist
     */
    public SearchMatch nextMatch() {
        if (matches.isEmpty()) {
            return null;
        }
        currentMatchIndex = (currentMatchIndex + 1) % matches.size();
        return matches.get(currentMatchIndex);
    }

    /**
     * Goes to the previous match. Wraps around to the last match.
     * Returns the new current match, or null if no matches.
     *
     * @return previous match, or {@code null} when no matches exist
     */
    public SearchMatch previousMatch() {
        if (matches.isEmpty()) {
            return null;
        }
        currentMatchIndex = (currentMatchIndex - 1 + matches.size()) % matches.size();
        return matches.get(currentMatchIndex);
    }

    /**
     * Selects the match nearest to the given document offset.
     *
     * @param offset document offset used as nearest-match anchor
     */
    public void selectNearestMatch(int offset) {
        if (matches.isEmpty()) {
            currentMatchIndex = -1;
            return;
        }
        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < matches.size(); i++) {
            int dist = Math.abs(matches.get(i).startOffset() - offset);
            if (dist < bestDistance) {
                bestDistance = dist;
                bestIndex = i;
            }
        }
        currentMatchIndex = bestIndex;
    }

    /**
     * Replaces the current match in the document.
     * When regex mode is active, capture-group references ($1, $2, etc.) in the
     * replacement string are expanded.
     * Returns true if replacement was performed.
     *
     * @param document document to mutate
     * @return {@code true} when replacement was applied
     */
    public boolean replaceCurrent(Document document) {
        SearchMatch match = getCurrentMatch();
        if (match == null || document == null) {
            return false;
        }
        String matchedText = document.getSubstring(match.startOffset(), match.endOffset());
        String effectiveReplacement = computeReplacement(matchedText);
        effectiveReplacement = applyPreserveCaseIfNeeded(effectiveReplacement, matchedText);
        document.replace(match.startOffset(), match.endOffset(), effectiveReplacement);
        search(document);
        return true;
    }

    /**
     * Replaces all matches in the document.
     * Replacements are applied from end to start to preserve match offsets and
     * to support per-match preserve-case behavior.
     * Returns the number of replacements made.
     *
     * @param document document to mutate
     * @return number of replacements performed
     */
    public int replaceAll(Document document) {
        if (matches.isEmpty() || document == null) {
            return 0;
        }
        int count = 0;
        // Replace from end to start to preserve offsets
        for (int i = matches.size() - 1; i >= 0; i--) {
            SearchMatch match = matches.get(i);
            String matchedText = document.getSubstring(match.startOffset(), match.endOffset());
            String effectiveReplacement = computeReplacement(matchedText);
            effectiveReplacement = applyPreserveCaseIfNeeded(effectiveReplacement, matchedText);
            document.replace(match.startOffset(), match.endOffset(), effectiveReplacement);
            count++;
        }
        search(document);
        return count;
    }

    /**
     * Clears search state.
     */
    public void clear() {
        query = "";
        replacement = "";
        matches = List.of();
        currentMatchIndex = -1;
        cachedPattern = null;
        cachedPatternQuery = "";
        cachedPatternCaseSensitive = false;
        cachedPatternWholeWord = false;
        cachedPatternInitialized = false;
        clearSelectionScope();
    }

    private String computeReplacement(String matchedText) {
        if (!regexMode) {
            return replacement;
        }
        try {
            Pattern pattern = compilePattern();
            if (pattern == null) {
                return replacement;
            }
            Matcher matcher = pattern.matcher(matchedText);
            if (matcher.matches()) {
                return matcher.replaceFirst(replacement);
            }
        } catch (PatternSyntaxException | IndexOutOfBoundsException e) {
            // Fall through to literal replacement
        }
        return replacement;
    }

    private Pattern compilePattern() {
        if (cachedPatternInitialized
            && query.equals(cachedPatternQuery)
            && caseSensitive == cachedPatternCaseSensitive
            && wholeWord == cachedPatternWholeWord) {
            return cachedPattern;
        }
        cachedPatternInitialized = true;
        cachedPatternQuery = query;
        cachedPatternCaseSensitive = caseSensitive;
        cachedPatternWholeWord = wholeWord;
        try {
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            String patternStr = wholeWord ? "\\b" + query + "\\b" : query;
            cachedPattern = Pattern.compile(patternStr, flags);
        } catch (PatternSyntaxException e) {
            cachedPattern = null;
        }
        return cachedPattern;
    }

    private List<SearchMatch> searchPlainText(String text, Document document, int scopeStart, int scopeEnd) {
        List<SearchMatch> found = new ArrayList<>();
        int index = scopeStart;
        int queryLength = query.length();
        int step = Math.max(1, queryLength);
        while (index < scopeEnd) {
            int foundIndex = caseSensitive
                ? text.indexOf(query, index)
                : indexOfIgnoreCase(text, query, index, scopeEnd);
            if (foundIndex < 0 || foundIndex >= scopeEnd) {
                break;
            }
            int endIndex = foundIndex + queryLength;
            if (endIndex > scopeEnd) {
                break;
            }
            if (wholeWord && !isWordBoundary(text, foundIndex, endIndex)) {
                index = foundIndex + step;
                continue;
            }
            int line = document.getLineForOffset(foundIndex);
            int endLine = document.getLineForOffset(Math.max(foundIndex, endIndex - 1));
            if (line != endLine) {
                index = foundIndex + step;
                continue;
            }
            int lineStart = document.getLineStartOffset(line);
            int startCol = foundIndex - lineStart;
            int endCol = endIndex - lineStart;
            found.add(new SearchMatch(foundIndex, endIndex, line, startCol, endCol));
            index = foundIndex + step;
        }
        return found;
    }

    private List<SearchMatch> searchRegex(String text, Document document, int scopeStart, int scopeEnd) {
        List<SearchMatch> found = new ArrayList<>();
        Pattern pattern = compilePattern();
        if (pattern == null) {
            return found;
        }
        Matcher matcher = pattern.matcher(text);
        matcher.region(scopeStart, scopeEnd);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            if (start == end) {
                // Skip zero-length matches to avoid infinite loop
                continue;
            }
            int startLine = document.getLineForOffset(start);
            int endLine = document.getLineForOffset(Math.max(start, end - 1));
            if (startLine != endLine) {
                continue;
            }
            int lineStart = document.getLineStartOffset(startLine);
            int startCol = start - lineStart;
            int endCol = end - lineStart;
            found.add(new SearchMatch(start, end, startLine, startCol, endCol));
        }
        return found;
    }

    private int[] resolveSearchRange(int textLength) {
        if (!searchInSelection) {
            return new int[]{0, textLength};
        }
        if (selectionStartOffset < 0 || selectionEndOffset <= selectionStartOffset || selectionEndOffset > textLength) {
            return null;
        }
        return new int[]{selectionStartOffset, selectionEndOffset};
    }

    private String applyPreserveCaseIfNeeded(String replacementValue, String matchedText) {
        if (!preserveCase || replacementValue.isEmpty() || matchedText.isEmpty()) {
            return replacementValue;
        }
        if (isAllLowerCase(matchedText)) {
            return replacementValue.toLowerCase(Locale.ROOT);
        }
        if (isAllUpperCase(matchedText)) {
            return replacementValue.toUpperCase(Locale.ROOT);
        }
        if (isInitialCapital(matchedText)) {
            String lower = replacementValue.toLowerCase(Locale.ROOT);
            return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        }
        return replacementValue;
    }

    private static boolean isAllLowerCase(String text) {
        boolean hasLetters = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isLetter(ch)) {
                hasLetters = true;
                if (!Character.isLowerCase(ch)) {
                    return false;
                }
            }
        }
        return hasLetters;
    }

    private static boolean isAllUpperCase(String text) {
        boolean hasLetters = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isLetter(ch)) {
                hasLetters = true;
                if (!Character.isUpperCase(ch)) {
                    return false;
                }
            }
        }
        return hasLetters;
    }

    private static boolean isInitialCapital(String text) {
        int firstLetter = -1;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetter(text.charAt(i))) {
                firstLetter = i;
                break;
            }
        }
        if (firstLetter < 0 || !Character.isUpperCase(text.charAt(firstLetter))) {
            return false;
        }
        for (int i = firstLetter + 1; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isLetter(ch) && !Character.isLowerCase(ch)) {
                return false;
            }
        }
        return true;
    }

    private static int indexOfIgnoreCase(String text, String query, int fromIndex, int scopeEndExclusive) {
        int queryLength = query.length();
        if (queryLength == 0) {
            return fromIndex;
        }
        int max = scopeEndExclusive - queryLength;
        if (fromIndex > max) {
            return -1;
        }
        char first = query.charAt(0);
        char lower = Character.toLowerCase(first);
        char upper = Character.toUpperCase(first);
        for (int i = fromIndex; i <= max; i++) {
            char current = text.charAt(i);
            if (current != lower && current != upper
                && Character.toLowerCase(current) != lower
                && Character.toUpperCase(current) != upper) {
                continue;
            }
            if (text.regionMatches(true, i, query, 0, queryLength)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isWordBoundary(String text, int start, int end) {
        if (start > 0 && isWordChar(text.charAt(start - 1))) {
            return false;
        }
        if (end < text.length() && isWordChar(text.charAt(end))) {
            return false;
        }
        return true;
    }

    private static boolean isWordChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_';
    }
}
