package org.metalib.papifly.fx.code.document;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentTest {

    @Test
    void undoRedoFlowWorksAcrossEdits() {
        Document document = new Document();

        document.insert(0, "abc");
        document.insert(3, "\nxyz");
        document.delete(1, 2);
        document.replace(0, 1, "A");

        assertEquals("Ac\nxyz", document.getText());
        assertEquals(2, document.getLineCount());

        assertTrue(document.undo());
        assertEquals("ac\nxyz", document.getText());

        assertTrue(document.undo());
        assertEquals("abc\nxyz", document.getText());

        assertTrue(document.undo());
        assertEquals("abc", document.getText());

        assertTrue(document.redo());
        assertEquals("abc\nxyz", document.getText());

        assertTrue(document.redo());
        assertEquals("ac\nxyz", document.getText());
    }

    @Test
    void redoIsClearedAfterNewEdit() {
        Document document = new Document("abc");

        document.insert(3, "d");
        assertEquals("abcd", document.getText());

        assertTrue(document.undo());
        assertEquals("abc", document.getText());
        assertTrue(document.canRedo());

        document.insert(3, "x");
        assertEquals("abcx", document.getText());
        assertFalse(document.canRedo());
    }

    @Test
    void lineQueriesAreConsistent() {
        Document document = new Document("hello\nworld");

        assertEquals(2, document.getLineCount());
        assertEquals("hello", document.getLineText(0));
        assertEquals("world", document.getLineText(1));
        assertEquals("ello", document.getSubstring(1, 5));
        assertEquals(1, document.getLineForOffset(7));
        assertEquals(2, document.getColumnForOffset(8));
        assertEquals(8, document.toOffset(1, 2));
    }

    @Test
    void endsWithNewlineReflectsDocumentTail() {
        Document document = new Document("hello\n");
        assertTrue(document.endsWithNewline());

        document.delete(document.length() - 1, document.length());
        assertFalse(document.endsWithNewline());
    }

    @Test
    void undoRedoPreservesLineIndexAfterMultilineReplace() {
        Document document = new Document("a\nb\nc");

        document.replace(2, 3, "x\ny");
        assertEquals("a\nx\ny\nc", document.getText());
        assertEquals(4, document.getLineCount());
        assertEquals(6, document.getLineStartOffset(3));

        assertTrue(document.undo());
        assertEquals("a\nb\nc", document.getText());
        assertEquals(3, document.getLineCount());
        assertEquals(4, document.getLineStartOffset(2));

        assertTrue(document.redo());
        assertEquals("a\nx\ny\nc", document.getText());
        assertEquals(4, document.getLineCount());
        assertEquals(6, document.getLineStartOffset(3));
    }

    @Test
    void setTextClearsUndoHistory() {
        Document document = new Document();
        document.insert(0, "abc");
        assertTrue(document.canUndo());

        document.setText("new text");
        assertEquals("new text", document.getText());
        assertFalse(document.canUndo());
        assertFalse(document.canRedo());
    }

    @Test
    void emptyDocumentBehavior() {
        Document document = new Document();
        assertEquals("", document.getText());
        assertEquals(0, document.length());
        assertEquals(1, document.getLineCount());
        assertEquals("", document.getLineText(0));
        assertFalse(document.canUndo());
        assertFalse(document.canRedo());
        assertFalse(document.undo());
        assertFalse(document.redo());
    }

    @Test
    void insertAtDocumentEnd() {
        Document document = new Document("abc");
        document.insert(3, "def");
        assertEquals("abcdef", document.getText());
        assertEquals(6, document.length());
    }

    @Test
    void deleteLastCharacter() {
        Document document = new Document("abc");
        document.delete(2, 3);
        assertEquals("ab", document.getText());
    }

    @Test
    void deleteAllContent() {
        Document document = new Document("abc");
        document.delete(0, 3);
        assertEquals("", document.getText());
        assertEquals(0, document.length());
        assertEquals(1, document.getLineCount());
    }

    @Test
    void insertNullTextIsNoOp() {
        Document document = new Document("abc");
        document.insert(1, null);
        assertEquals("abc", document.getText());
        assertFalse(document.canUndo());
    }

    @Test
    void deleteEqualOffsetsIsNoOp() {
        Document document = new Document("abc");
        document.delete(1, 1);
        assertEquals("abc", document.getText());
        assertFalse(document.canUndo());
    }

    @Test
    void replaceWithEmptyRangeIsInsert() {
        Document document = new Document("abc");
        document.replace(1, 1, "X");
        assertEquals("aXbc", document.getText());
    }

    // --- Change listener tests ---

    @Test
    void changeListenerFiredOnInsert() {
        Document document = new Document();
        List<DocumentChangeEvent> events = new ArrayList<>();
        document.addChangeListener(events::add);

        document.insert(0, "hello");

        assertEquals(1, events.size());
        assertEquals(DocumentChangeEvent.ChangeType.INSERT, events.get(0).type());
        assertEquals(0, events.get(0).offset());
        assertEquals(0, events.get(0).oldLength());
        assertEquals(5, events.get(0).newLength());
    }

    @Test
    void changeListenerFiredOnDelete() {
        Document document = new Document("hello");
        List<DocumentChangeEvent> events = new ArrayList<>();
        document.addChangeListener(events::add);

        document.delete(1, 3);

        assertEquals(1, events.size());
        assertEquals(DocumentChangeEvent.ChangeType.DELETE, events.get(0).type());
        assertEquals(1, events.get(0).offset());
        assertEquals(2, events.get(0).oldLength());
        assertEquals(0, events.get(0).newLength());
    }

    @Test
    void changeListenerFiredOnReplace() {
        Document document = new Document("hello");
        List<DocumentChangeEvent> events = new ArrayList<>();
        document.addChangeListener(events::add);

        document.replace(1, 3, "XYZ");

        assertEquals(1, events.size());
        assertEquals(DocumentChangeEvent.ChangeType.REPLACE, events.get(0).type());
        assertEquals(1, events.get(0).offset());
        assertEquals(2, events.get(0).oldLength());
        assertEquals(3, events.get(0).newLength());
    }

    @Test
    void changeListenerFiredOnSetText() {
        Document document = new Document("old");
        List<DocumentChangeEvent> events = new ArrayList<>();
        document.addChangeListener(events::add);

        document.setText("new text");

        assertEquals(1, events.size());
        assertEquals(DocumentChangeEvent.ChangeType.SET_TEXT, events.get(0).type());
        assertEquals(3, events.get(0).oldLength());
        assertEquals(8, events.get(0).newLength());
    }

    @Test
    void changeListenerFiredOnUndoAndRedo() {
        Document document = new Document();
        document.insert(0, "hello");
        List<DocumentChangeEvent> events = new ArrayList<>();
        document.addChangeListener(events::add);

        document.undo();
        assertEquals(1, events.size());
        assertEquals(DocumentChangeEvent.ChangeType.UNDO, events.get(0).type());

        document.redo();
        assertEquals(2, events.size());
        assertEquals(DocumentChangeEvent.ChangeType.REDO, events.get(1).type());
    }

    @Test
    void removedListenerNotFired() {
        Document document = new Document();
        List<DocumentChangeEvent> events = new ArrayList<>();
        DocumentChangeListener listener = events::add;
        document.addChangeListener(listener);
        document.removeChangeListener(listener);

        document.insert(0, "hello");

        assertTrue(events.isEmpty());
    }

    @Test
    void listenerExceptionDoesNotStopRemainingListeners() {
        Document document = new Document();
        List<DocumentChangeEvent> events = new ArrayList<>();
        document.addChangeListener(event -> {
            throw new RuntimeException("boom");
        });
        document.addChangeListener(events::add);

        document.insert(0, "x");

        assertEquals(1, events.size());
        assertEquals(DocumentChangeEvent.ChangeType.INSERT, events.get(0).type());
    }

    // --- Line ending normalization through Document ---

    @Test
    void windowsLineEndingsNormalizedInDocument() {
        Document document = new Document("a\r\nb\r\nc");
        assertEquals("a\nb\nc", document.getText());
        assertEquals(3, document.getLineCount());
    }

    @Test
    void insertNormalizesLineEndingsInDocument() {
        Document document = new Document("ab");
        document.insert(1, "\r\n");
        assertEquals("a\nb", document.getText());
        assertEquals(2, document.getLineCount());
    }

    @Test
    void insertCrLfUndoRedoUsesNormalizedLength() {
        Document document = new Document("ab");

        document.insert(1, "\r\n");
        assertEquals("a\nb", document.getText());

        assertTrue(document.undo());
        assertEquals("ab", document.getText());

        assertTrue(document.redo());
        assertEquals("a\nb", document.getText());
    }

    @Test
    void replaceCrLfUndoRedoUsesNormalizedLength() {
        Document document = new Document("abcd");

        document.replace(1, 3, "\r\n");
        assertEquals("a\nd", document.getText());

        assertTrue(document.undo());
        assertEquals("abcd", document.getText());

        assertTrue(document.redo());
        assertEquals("a\nd", document.getText());
    }

    @Test
    void compoundEditsWithCrLfUndoRedoRemainConsistent() {
        Document document = new Document("0123");

        document.beginCompoundEdit();
        document.replace(1, 2, "\r\n");
        document.insert(0, "\r\n");
        document.endCompoundEdit();
        assertEquals("\n0\n23", document.getText());

        assertTrue(document.undo());
        assertEquals("0123", document.getText());

        assertTrue(document.redo());
        assertEquals("\n0\n23", document.getText());
    }

    // --- Incremental line index correctness ---

    @Test
    void incrementalInsertMaintainsLineIndex() {
        Document document = new Document("line1\nline2\nline3");
        assertEquals(3, document.getLineCount());

        // Insert newline in middle
        document.insert(5, "\nnew");
        assertEquals(4, document.getLineCount());
        assertEquals("line1", document.getLineText(0));
        assertEquals("new", document.getLineText(1));
        assertEquals("line2", document.getLineText(2));
        assertEquals("line3", document.getLineText(3));
    }

    @Test
    void incrementalDeleteMaintainsLineIndex() {
        Document document = new Document("line1\nline2\nline3");
        assertEquals(3, document.getLineCount());

        // Delete line2 including its newline
        document.delete(5, 11);
        assertEquals(2, document.getLineCount());
        assertEquals("line1", document.getLineText(0));
        assertEquals("line3", document.getLineText(1));
    }

    @Test
    void incrementalReplaceMaintainsLineIndex() {
        Document document = new Document("aaa\nbbb\nccc");
        assertEquals(3, document.getLineCount());

        // Replace "bbb" with "x\ny\nz"
        document.replace(4, 7, "x\ny\nz");
        assertEquals(5, document.getLineCount());
        assertEquals("aaa", document.getLineText(0));
        assertEquals("x", document.getLineText(1));
        assertEquals("y", document.getLineText(2));
        assertEquals("z", document.getLineText(3));
        assertEquals("ccc", document.getLineText(4));
    }

    @Test
    void incrementalUpdatesMatchFullRebuildAfterManyEdits() {
        Document document = new Document("hello\nworld\nfoo\nbar");
        document.insert(5, "\nnewline");
        document.delete(0, 6);
        document.insert(document.length(), "\nend");
        document.replace(0, 7, "start");

        // Verify by comparing against a fresh document
        Document reference = new Document(document.getText());
        assertEquals(reference.getLineCount(), document.getLineCount());
        for (int i = 0; i < reference.getLineCount(); i++) {
            assertEquals(reference.getLineText(i), document.getLineText(i),
                "Line " + i + " mismatch");
            assertEquals(reference.getLineStartOffset(i), document.getLineStartOffset(i),
                "Line start offset mismatch at line " + i);
        }
    }

    // --- Performance guard ---

    @Test
    void perEditPerformanceGuard_largeDocument() {
        // Build a large document
        StringBuilder sb = new StringBuilder();
        int lineCount = 50_000;
        for (int i = 0; i < lineCount; i++) {
            if (i > 0) sb.append('\n');
            sb.append("line ").append(i);
        }
        Document document = new Document(sb.toString());
        assertEquals(lineCount, document.getLineCount());

        // Measure 100 single-character inserts
        long start = System.nanoTime();
        int insertCount = 100;
        for (int i = 0; i < insertCount; i++) {
            document.insert(0, "x");
        }
        long elapsed = System.nanoTime() - start;
        double avgMicros = (elapsed / 1000.0) / insertCount;

        // Incremental updates should average under 1ms per edit even on 50k lines
        assertTrue(avgMicros < 2000,
            "Average per-edit time should be under 2ms, was " + avgMicros + " µs");

        // Verify correctness
        assertEquals(lineCount, document.getLineCount());
    }
}
