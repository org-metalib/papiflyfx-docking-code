package org.metalib.papifly.fx.code.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextSourceTest {

    @Test
    void insertDeleteReplaceWorkAsExpected() {
        TextSource source = new TextSource("abc");

        source.insert(1, "X");
        assertEquals("aXbc", source.getText());

        String deleted = source.delete(1, 2);
        assertEquals("X", deleted);
        assertEquals("abc", source.getText());

        String replaced = source.replace(1, 3, "YZ");
        assertEquals("bc", replaced);
        assertEquals("aYZ", source.getText());
    }

    @Test
    void invalidRangesThrow() {
        TextSource source = new TextSource("abc");

        assertThrows(IndexOutOfBoundsException.class, () -> source.insert(5, "x"));
        assertThrows(IndexOutOfBoundsException.class, () -> source.delete(-1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> source.replace(2, 1, "x"));
    }

    @Test
    void emptySourceOperations() {
        TextSource source = new TextSource();
        assertEquals("", source.getText());
        assertEquals(0, source.length());
        assertTrue(source.isEmpty());

        source.insert(0, "hello");
        assertEquals("hello", source.getText());
        assertEquals(5, source.length());
    }

    @Test
    void nullTextInConstructorGivesEmpty() {
        TextSource source = new TextSource(null);
        assertEquals("", source.getText());
        assertEquals(0, source.length());
    }

    @Test
    void setTextReplacesContent() {
        TextSource source = new TextSource("old");
        source.setText("new content");
        assertEquals("new content", source.getText());
    }

    @Test
    void setTextWithNullClearsContent() {
        TextSource source = new TextSource("old");
        source.setText(null);
        assertEquals("", source.getText());
    }

    @Test
    void insertAtEndWorks() {
        TextSource source = new TextSource("abc");
        source.insert(3, "def");
        assertEquals("abcdef", source.getText());
    }

    @Test
    void insertAtStartWorks() {
        TextSource source = new TextSource("abc");
        source.insert(0, "xyz");
        assertEquals("xyzabc", source.getText());
    }

    @Test
    void deleteLastCharacterWorks() {
        TextSource source = new TextSource("abc");
        String deleted = source.delete(2, 3);
        assertEquals("c", deleted);
        assertEquals("ab", source.getText());
    }

    @Test
    void deleteAllContentWorks() {
        TextSource source = new TextSource("abc");
        String deleted = source.delete(0, 3);
        assertEquals("abc", deleted);
        assertEquals("", source.getText());
        assertTrue(source.isEmpty());
    }

    @Test
    void insertNullTextIsNoOp() {
        TextSource source = new TextSource("abc");
        source.insert(1, null);
        assertEquals("abc", source.getText());
    }

    @Test
    void insertEmptyTextIsNoOp() {
        TextSource source = new TextSource("abc");
        source.insert(1, "");
        assertEquals("abc", source.getText());
    }

    // --- Line ending normalization ---

    @Test
    void constructorNormalizesWindowsLineEndings() {
        TextSource source = new TextSource("a\r\nb\r\nc");
        assertEquals("a\nb\nc", source.getText());
    }

    @Test
    void constructorNormalizesOldMacLineEndings() {
        TextSource source = new TextSource("a\rb\rc");
        assertEquals("a\nb\nc", source.getText());
    }

    @Test
    void setTextNormalizesLineEndings() {
        TextSource source = new TextSource();
        source.setText("line1\r\nline2\rline3");
        assertEquals("line1\nline2\nline3", source.getText());
    }

    @Test
    void insertNormalizesLineEndings() {
        TextSource source = new TextSource("ab");
        source.insert(1, "\r\nX\r");
        assertEquals("a\nX\nb", source.getText());
    }

    @Test
    void replaceNormalizesLineEndings() {
        TextSource source = new TextSource("abc");
        source.replace(1, 2, "\r\n");
        assertEquals("a\nc", source.getText());
    }

    @Test
    void mixedLineEndingsNormalized() {
        TextSource source = new TextSource("a\r\nb\rc\nd");
        assertEquals("a\nb\nc\nd", source.getText());
    }
}
