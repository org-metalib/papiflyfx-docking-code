package org.metalib.papifly.fx.code.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LineIndexTest {

    @Test
    void lineAndColumnMappingWorksWithTrailingNewline() {
        String text = "ab\nc\n";
        LineIndex index = new LineIndex(text);

        assertEquals(3, index.getLineCount());

        assertEquals(0, index.getLineForOffset(0, text.length()));
        assertEquals(0, index.getLineForOffset(2, text.length()));
        assertEquals(1, index.getLineForOffset(3, text.length()));
        assertEquals(1, index.getLineForOffset(4, text.length()));
        assertEquals(2, index.getLineForOffset(5, text.length()));

        assertEquals(1, index.getColumnForOffset(4, text.length()));

        assertEquals(2, index.toOffset(0, 99, text.length()));
        assertEquals(5, index.toOffset(2, 99, text.length()));
    }

    @Test
    void lineEndOffsetExcludesNewline() {
        String text = "abc\ndef";
        LineIndex index = new LineIndex(text);

        assertEquals(3, index.getLineEndOffset(0, text.length()));
        assertEquals(7, index.getLineEndOffset(1, text.length()));
    }

    @Test
    void emptyTextHasOneLine() {
        LineIndex index = new LineIndex("");
        assertEquals(1, index.getLineCount());
        assertEquals(0, index.getLineStartOffset(0));
        assertEquals(0, index.getLineEndOffset(0, 0));
    }

    @Test
    void singleLineNoNewline() {
        String text = "hello";
        LineIndex index = new LineIndex(text);
        assertEquals(1, index.getLineCount());
        assertEquals(0, index.getLineStartOffset(0));
        assertEquals(5, index.getLineEndOffset(0, text.length()));
        assertEquals(0, index.getLineForOffset(0, text.length()));
        assertEquals(0, index.getLineForOffset(4, text.length()));
    }

    @Test
    void nullTextHasOneLine() {
        LineIndex index = new LineIndex(null);
        assertEquals(1, index.getLineCount());
    }

    @Test
    void invalidLineThrows() {
        LineIndex index = new LineIndex("abc");
        assertThrows(IndexOutOfBoundsException.class, () -> index.getLineStartOffset(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> index.getLineStartOffset(1));
    }

    @Test
    void invalidOffsetThrows() {
        String text = "abc";
        LineIndex index = new LineIndex(text);
        assertThrows(IndexOutOfBoundsException.class, () -> index.getLineForOffset(-1, text.length()));
        assertThrows(IndexOutOfBoundsException.class, () -> index.getLineForOffset(4, text.length()));
    }

    @Test
    void offsetAtEndOfTextIsValid() {
        String text = "abc";
        LineIndex index = new LineIndex(text);
        assertEquals(0, index.getLineForOffset(3, text.length()));
        assertEquals(3, index.getColumnForOffset(3, text.length()));
    }

    @Test
    void rebuildUpdatesIndex() {
        LineIndex index = new LineIndex("a\nb");
        assertEquals(2, index.getLineCount());

        index.rebuild("a\nb\nc\nd");
        assertEquals(4, index.getLineCount());
    }

    @Test
    void columnClampingOnToOffset() {
        String text = "ab\ncde";
        LineIndex index = new LineIndex(text);
        // Line 0 has 2 chars, column 100 should clamp to offset 2
        assertEquals(2, index.toOffset(0, 100, text.length()));
        // Line 1 has 3 chars, column 100 should clamp to offset 6
        assertEquals(6, index.toOffset(1, 100, text.length()));
    }

    // --- Incremental update tests ---

    @Test
    void applyInsert_singleCharMiddle() {
        // "ab\ncd" -> "ab\ncXd" (insert 'X' at offset 4)
        LineIndex index = new LineIndex("ab\ncd");
        assertEquals(2, index.getLineCount());

        index.applyInsert(4, "X");
        // Still 2 lines
        assertEquals(2, index.getLineCount());
        assertEquals(0, index.getLineStartOffset(0));
        assertEquals(3, index.getLineStartOffset(1));
    }

    @Test
    void applyInsert_newlineCreatesLine() {
        // "ab\ncd" -> "ab\nc\nd" (insert '\n' at offset 4)
        LineIndex index = new LineIndex("ab\ncd");
        assertEquals(2, index.getLineCount());

        index.applyInsert(4, "\n");
        assertEquals(3, index.getLineCount());
        assertEquals(0, index.getLineStartOffset(0));
        assertEquals(3, index.getLineStartOffset(1));
        assertEquals(5, index.getLineStartOffset(2));
    }

    @Test
    void applyInsert_multipleNewlines() {
        // "abc" -> "a\n\nbc"
        LineIndex index = new LineIndex("abc");
        assertEquals(1, index.getLineCount());

        index.applyInsert(1, "\n\n");
        assertEquals(3, index.getLineCount());
        assertEquals(0, index.getLineStartOffset(0));
        assertEquals(2, index.getLineStartOffset(1));
        assertEquals(3, index.getLineStartOffset(2));
    }

    @Test
    void applyDelete_removesNewline() {
        // "ab\ncd\nef" -> "abcd\nef" (delete '\n' at offset 2..3)
        LineIndex index = new LineIndex("ab\ncd\nef");
        assertEquals(3, index.getLineCount());

        index.applyDelete(2, 3);
        assertEquals(2, index.getLineCount());
        assertEquals(0, index.getLineStartOffset(0));
        assertEquals(5, index.getLineStartOffset(1));
    }

    @Test
    void applyDelete_removesMultipleNewlines() {
        // "a\nb\nc\nd" -> "ad" (delete offset 1..6)
        LineIndex index = new LineIndex("a\nb\nc\nd");
        assertEquals(4, index.getLineCount());

        index.applyDelete(1, 6);
        assertEquals(1, index.getLineCount());
        assertEquals(0, index.getLineStartOffset(0));
    }

    @Test
    void applyInsert_matchesFullRebuild() {
        String original = "line1\nline2\nline3";
        String inserted = "\nnew";
        int offset = 5;

        LineIndex incremental = new LineIndex(original);
        incremental.applyInsert(offset, inserted);

        String full = original.substring(0, offset) + inserted + original.substring(offset);
        LineIndex rebuilt = new LineIndex(full);

        assertEquals(rebuilt.getLineCount(), incremental.getLineCount());
        for (int i = 0; i < rebuilt.getLineCount(); i++) {
            assertEquals(rebuilt.getLineStartOffset(i), incremental.getLineStartOffset(i),
                "Line start mismatch at line " + i);
        }
    }
}
