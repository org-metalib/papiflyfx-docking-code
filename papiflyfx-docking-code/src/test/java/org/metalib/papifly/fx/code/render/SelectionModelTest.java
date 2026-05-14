package org.metalib.papifly.fx.code.render;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.document.Document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectionModelTest {

    @Test
    void initialPositionIsZeroZero() {
        SelectionModel model = new SelectionModel();
        assertEquals(0, model.getCaretLine());
        assertEquals(0, model.getCaretColumn());
        assertFalse(model.hasSelection());
    }

    @Test
    void moveCaretClearsSelection() {
        SelectionModel model = new SelectionModel();
        model.moveCaretWithSelection(1, 5);
        assertTrue(model.hasSelection());

        model.moveCaret(2, 3);
        assertFalse(model.hasSelection());
        assertEquals(2, model.getCaretLine());
        assertEquals(3, model.getCaretColumn());
    }

    @Test
    void moveCaretWithSelectionExtendsSelection() {
        SelectionModel model = new SelectionModel();
        model.moveCaret(1, 0);
        model.moveCaretWithSelection(2, 5);

        assertTrue(model.hasSelection());
        assertEquals(1, model.getAnchorLine());
        assertEquals(0, model.getAnchorColumn());
        assertEquals(2, model.getCaretLine());
        assertEquals(5, model.getCaretColumn());
    }

    @Test
    void selectionStartEndForwardSelection() {
        SelectionModel model = new SelectionModel();
        model.moveCaret(0, 2);
        model.moveCaretWithSelection(1, 3);

        assertEquals(0, model.getSelectionStartLine());
        assertEquals(2, model.getSelectionStartColumn());
        assertEquals(1, model.getSelectionEndLine());
        assertEquals(3, model.getSelectionEndColumn());
    }

    @Test
    void selectionStartEndBackwardSelection() {
        SelectionModel model = new SelectionModel();
        model.moveCaret(2, 5);
        model.moveCaretWithSelection(0, 1);

        assertEquals(0, model.getSelectionStartLine());
        assertEquals(1, model.getSelectionStartColumn());
        assertEquals(2, model.getSelectionEndLine());
        assertEquals(5, model.getSelectionEndColumn());
    }

    @Test
    void selectionStartEndSameLineForwardSelection() {
        SelectionModel model = new SelectionModel();
        model.moveCaret(1, 2);
        model.moveCaretWithSelection(1, 8);

        assertEquals(1, model.getSelectionStartLine());
        assertEquals(2, model.getSelectionStartColumn());
        assertEquals(1, model.getSelectionEndLine());
        assertEquals(8, model.getSelectionEndColumn());
    }

    @Test
    void selectionStartEndSameLineBackwardSelection() {
        SelectionModel model = new SelectionModel();
        model.moveCaret(1, 8);
        model.moveCaretWithSelection(1, 2);

        assertEquals(1, model.getSelectionStartLine());
        assertEquals(2, model.getSelectionStartColumn());
        assertEquals(1, model.getSelectionEndLine());
        assertEquals(8, model.getSelectionEndColumn());
    }

    @Test
    void clearSelectionMovesAnchorToCaret() {
        SelectionModel model = new SelectionModel();
        model.moveCaret(0, 0);
        model.moveCaretWithSelection(2, 5);
        assertTrue(model.hasSelection());

        model.clearSelection();
        assertFalse(model.hasSelection());
        assertEquals(2, model.getAnchorLine());
        assertEquals(5, model.getAnchorColumn());
    }

    @Test
    void selectAllCoversEntireDocument() {
        Document doc = new Document("hello\nworld\nfoo");
        SelectionModel model = new SelectionModel();

        model.selectAll(doc);

        assertTrue(model.hasSelection());
        assertEquals(0, model.getSelectionStartLine());
        assertEquals(0, model.getSelectionStartColumn());
        assertEquals(2, model.getSelectionEndLine());
        assertEquals(3, model.getSelectionEndColumn());
    }

    @Test
    void getSelectedTextReturnsCorrectSubstring() {
        Document doc = new Document("hello\nworld");
        SelectionModel model = new SelectionModel();
        model.moveCaret(0, 1);
        model.moveCaretWithSelection(0, 4);

        assertEquals("ell", model.getSelectedText(doc));
    }

    @Test
    void getSelectedTextWithNoSelectionReturnsEmpty() {
        Document doc = new Document("hello");
        SelectionModel model = new SelectionModel();
        model.moveCaret(0, 2);

        assertEquals("", model.getSelectedText(doc));
    }

    @Test
    void getSelectedTextMultiLine() {
        Document doc = new Document("hello\nworld");
        SelectionModel model = new SelectionModel();
        model.moveCaret(0, 3);
        model.moveCaretWithSelection(1, 2);

        assertEquals("lo\nwo", model.getSelectedText(doc));
    }

    @Test
    void caretOffsetComputation() {
        Document doc = new Document("hello\nworld");
        SelectionModel model = new SelectionModel();
        model.moveCaret(1, 2);

        assertEquals(8, model.getCaretOffset(doc));
    }
}
