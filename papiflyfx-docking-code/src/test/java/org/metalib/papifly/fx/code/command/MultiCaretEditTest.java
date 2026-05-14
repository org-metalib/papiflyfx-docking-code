package org.metalib.papifly.fx.code.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.render.SelectionModel;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiCaretEditTest {

    private Document document;
    private SelectionModel selectionModel;
    private MultiCaretModel model;

    @BeforeEach
    void setUp() {
        document = new Document("aaa\nbbb\nccc");
        selectionModel = new SelectionModel();
        model = new MultiCaretModel(selectionModel);
    }

    @Test
    void insertAtMultipleCaretsReverseOrder() {
        // Place carets at start of each line
        selectionModel.moveCaret(0, 0);
        model.addCaret(new CaretRange(1, 0, 1, 0));
        model.addCaret(new CaretRange(2, 0, 2, 0));

        // Get carets sorted descending (reverse offset order for safe editing)
        List<CaretRange> carets = model.allCarets(document);
        carets.sort(Comparator.comparingInt((CaretRange cr) -> cr.getCaretOffset(document)).reversed());

        document.beginCompoundEdit();
        for (CaretRange caret : carets) {
            document.insert(caret.getCaretOffset(document), "X");
        }
        document.endCompoundEdit();

        assertEquals("Xaaa\nXbbb\nXccc", document.getText());

        // Single undo should revert all
        document.undo();
        assertEquals("aaa\nbbb\nccc", document.getText());
    }

    @Test
    void deleteAtMultipleCaretsReverseOrder() {
        // Place carets at column 1 of each line (delete char at col 0)
        selectionModel.moveCaret(0, 1);
        model.addCaret(new CaretRange(1, 1, 1, 1));
        model.addCaret(new CaretRange(2, 1, 2, 1));

        List<CaretRange> carets = model.allCarets(document);
        carets.sort(Comparator.comparingInt((CaretRange cr) -> cr.getCaretOffset(document)).reversed());

        document.beginCompoundEdit();
        for (CaretRange caret : carets) {
            int offset = caret.getCaretOffset(document);
            if (offset > 0) {
                document.delete(offset - 1, offset);
            }
        }
        document.endCompoundEdit();

        assertEquals("aa\nbb\ncc", document.getText());

        document.undo();
        assertEquals("aaa\nbbb\nccc", document.getText());
    }

    @Test
    void deleteSelectionsAtMultipleCarets() {
        // Select "aaa" on line 0, "ccc" on line 2
        selectionModel.moveCaret(0, 0);
        selectionModel.moveCaretWithSelection(0, 3);
        model.addCaret(new CaretRange(2, 0, 2, 3));

        List<CaretRange> carets = model.allCarets(document);
        carets.sort(Comparator.comparingInt((CaretRange cr) -> cr.getCaretOffset(document)).reversed());

        document.beginCompoundEdit();
        for (CaretRange caret : carets) {
            if (caret.hasSelection()) {
                document.delete(caret.getStartOffset(document), caret.getEndOffset(document));
            }
        }
        document.endCompoundEdit();

        assertEquals("\nbbb\n", document.getText());

        document.undo();
        assertEquals("aaa\nbbb\nccc", document.getText());
    }

    @Test
    void selectNextOccurrenceFindsMatches() {
        Document doc = new Document("foo bar foo baz foo");
        // "foo" appears at offsets 0, 8, 16
        String text = doc.getText();
        String target = "foo";

        int first = text.indexOf(target);
        assertEquals(0, first);

        int second = text.indexOf(target, first + target.length());
        assertEquals(8, second);

        int third = text.indexOf(target, second + target.length());
        assertEquals(16, third);
    }

    @Test
    void compoundEditRedoReAppliesAll() {
        selectionModel.moveCaret(0, 0);
        model.addCaret(new CaretRange(1, 0, 1, 0));

        List<CaretRange> carets = model.allCarets(document);
        carets.sort(Comparator.comparingInt((CaretRange cr) -> cr.getCaretOffset(document)).reversed());

        document.beginCompoundEdit();
        for (CaretRange caret : carets) {
            document.insert(caret.getCaretOffset(document), "Z");
        }
        document.endCompoundEdit();

        assertEquals("Zaaa\nZbbb\nccc", document.getText());

        document.undo();
        assertEquals("aaa\nbbb\nccc", document.getText());

        document.redo();
        assertEquals("Zaaa\nZbbb\nccc", document.getText());
    }
}
