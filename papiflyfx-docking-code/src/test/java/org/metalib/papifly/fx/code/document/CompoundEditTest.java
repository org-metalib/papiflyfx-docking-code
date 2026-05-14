package org.metalib.papifly.fx.code.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompoundEditTest {

    @Test
    void compoundEditGroupsMultipleInsertsIntoOneUndo() {
        Document document = new Document("abcdef");

        document.beginCompoundEdit();
        document.insert(6, "X");
        document.insert(3, "Y");
        document.insert(0, "Z");
        document.endCompoundEdit();

        assertEquals("ZabcYdefX", document.getText());
        assertTrue(document.canUndo());

        // Single undo should revert all three inserts
        document.undo();
        assertEquals("abcdef", document.getText());

        // Single redo should re-apply all three
        document.redo();
        assertEquals("ZabcYdefX", document.getText());
    }

    @Test
    void compoundEditWithDeletesUndoesAll() {
        Document document = new Document("hello world");

        document.beginCompoundEdit();
        document.delete(5, 6); // remove space
        document.delete(0, 1); // remove 'h'
        document.endCompoundEdit();

        assertEquals("elloworld", document.getText());

        document.undo();
        assertEquals("hello world", document.getText());
    }

    @Test
    void compoundEditWithMixedOps() {
        Document document = new Document("aaa bbb ccc");

        document.beginCompoundEdit();
        document.replace(8, 11, "CCC");
        document.replace(4, 7, "BBB");
        document.replace(0, 3, "AAA");
        document.endCompoundEdit();

        assertEquals("AAA BBB CCC", document.getText());

        document.undo();
        assertEquals("aaa bbb ccc", document.getText());

        document.redo();
        assertEquals("AAA BBB CCC", document.getText());
    }

    @Test
    void emptyCompoundEditDoesNotPushToUndoStack() {
        Document document = new Document("abc");
        document.beginCompoundEdit();
        document.endCompoundEdit();

        assertFalse(document.canUndo());
    }

    @Test
    void compoundEditClearsRedoStack() {
        Document document = new Document("abc");
        document.insert(3, "d");
        document.undo();
        assertTrue(document.canRedo());

        document.beginCompoundEdit();
        document.insert(0, "X");
        document.endCompoundEdit();

        assertFalse(document.canRedo());
    }

    @Test
    void isCompoundEditActiveReflectsState() {
        Document document = new Document("abc");
        assertFalse(document.isCompoundEditActive());

        document.beginCompoundEdit();
        assertTrue(document.isCompoundEditActive());

        document.endCompoundEdit();
        assertFalse(document.isCompoundEditActive());
    }
}
