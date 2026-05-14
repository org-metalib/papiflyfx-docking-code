package org.metalib.papifly.fx.code.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WordBoundaryTest {

    // --- findWordLeft ---

    @Test
    void wordLeftFromMiddleOfWord() {
        assertEquals(0, WordBoundary.findWordLeft("hello world", 3));
    }

    @Test
    void wordLeftFromEndOfFirstWord() {
        assertEquals(0, WordBoundary.findWordLeft("hello world", 5));
    }

    @Test
    void wordLeftFromStartOfSecondWord() {
        assertEquals(0, WordBoundary.findWordLeft("hello world", 6));
    }

    @Test
    void wordLeftFromMiddleOfSecondWord() {
        assertEquals(6, WordBoundary.findWordLeft("hello world", 8));
    }

    @Test
    void wordLeftFromEndOfLine() {
        assertEquals(6, WordBoundary.findWordLeft("hello world", 11));
    }

    @Test
    void wordLeftAtColumnZero() {
        assertEquals(0, WordBoundary.findWordLeft("hello", 0));
    }

    @Test
    void wordLeftEmptyString() {
        assertEquals(0, WordBoundary.findWordLeft("", 0));
    }

    @Test
    void wordLeftNullString() {
        assertEquals(0, WordBoundary.findWordLeft(null, 5));
    }

    @Test
    void wordLeftAllWhitespace() {
        assertEquals(0, WordBoundary.findWordLeft("   ", 3));
    }

    @Test
    void wordLeftUnderscoreIsWordChar() {
        // foo_bar is one word
        assertEquals(0, WordBoundary.findWordLeft("foo_bar baz", 4));
    }

    @Test
    void wordLeftPunctuationBoundary() {
        // "a.b" at col 2 -> stops at dot (col 2 is 'b', going left encounters '.' which is punctuation)
        assertEquals(2, WordBoundary.findWordLeft("a.b", 3));
    }

    @Test
    void wordLeftFromAfterDot() {
        assertEquals(1, WordBoundary.findWordLeft("a.b", 2));
    }

    @Test
    void wordLeftLeadingWhitespace() {
        assertEquals(0, WordBoundary.findWordLeft("  hello", 2));
    }

    // --- findWordRight ---

    @Test
    void wordRightFromStartOfLine() {
        assertEquals(6, WordBoundary.findWordRight("hello world", 0));
    }

    @Test
    void wordRightFromMiddleOfFirstWord() {
        assertEquals(6, WordBoundary.findWordRight("hello world", 3));
    }

    @Test
    void wordRightFromSpaceBetweenWords() {
        assertEquals(6, WordBoundary.findWordRight("hello world", 5));
    }

    @Test
    void wordRightFromStartOfSecondWord() {
        assertEquals(11, WordBoundary.findWordRight("hello world", 6));
    }

    @Test
    void wordRightAtEndOfLine() {
        assertEquals(11, WordBoundary.findWordRight("hello world", 11));
    }

    @Test
    void wordRightEmptyString() {
        assertEquals(0, WordBoundary.findWordRight("", 0));
    }

    @Test
    void wordRightNullString() {
        assertEquals(0, WordBoundary.findWordRight(null, 0));
    }

    @Test
    void wordRightAllWhitespace() {
        assertEquals(3, WordBoundary.findWordRight("   ", 0));
    }

    @Test
    void wordRightUnderscoreIsWordChar() {
        assertEquals(8, WordBoundary.findWordRight("foo_bar baz", 0));
    }

    @Test
    void wordRightPunctuationBoundary() {
        // "a.b" at col 0 -> skips 'a' (word), no whitespace -> stops at 1
        assertEquals(1, WordBoundary.findWordRight("a.b", 0));
    }

    @Test
    void wordRightFromDot() {
        // "a.b" at col 1 -> '.' is punctuation, skip it -> stop at 2
        assertEquals(2, WordBoundary.findWordRight("a.b", 1));
    }

    @Test
    void wordRightTrailingWhitespace() {
        // Skips word "hello" then trailing spaces -> end of line
        assertEquals(7, WordBoundary.findWordRight("hello  ", 0));
    }

    // --- isWordChar ---

    @Test
    void wordCharClassification() {
        assertEquals(true, WordBoundary.isWordChar('a'));
        assertEquals(true, WordBoundary.isWordChar('Z'));
        assertEquals(true, WordBoundary.isWordChar('5'));
        assertEquals(true, WordBoundary.isWordChar('_'));
        assertEquals(false, WordBoundary.isWordChar('.'));
        assertEquals(false, WordBoundary.isWordChar(' '));
        assertEquals(false, WordBoundary.isWordChar('-'));
        assertEquals(false, WordBoundary.isWordChar('('));
    }
}
