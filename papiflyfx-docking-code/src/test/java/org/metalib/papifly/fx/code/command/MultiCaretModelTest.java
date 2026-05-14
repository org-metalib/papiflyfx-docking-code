package org.metalib.papifly.fx.code.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.render.SelectionModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiCaretModelTest {

    private Document document;
    private SelectionModel selectionModel;
    private MultiCaretModel model;

    @BeforeEach
    void setUp() {
        document = new Document("hello world\nfoo bar\nbaz qux");
        selectionModel = new SelectionModel();
        selectionModel.moveCaret(0, 0);
        model = new MultiCaretModel(selectionModel);
    }

    @Test
    void singleCaretByDefault() {
        assertFalse(model.hasMultipleCarets());
        List<CaretRange> all = model.allCarets(document);
        assertEquals(1, all.size());
        assertEquals(0, all.get(0).caretLine());
        assertEquals(0, all.get(0).caretColumn());
    }

    @Test
    void addSecondaryCaretMakesMultiple() {
        model.addCaret(new CaretRange(1, 0, 1, 0));
        assertTrue(model.hasMultipleCarets());
        assertEquals(2, model.allCarets(document).size());
    }

    @Test
    void allCaretsSortedByOffset() {
        // Primary at (0,0), add secondaries at (2,0) then (1,0)
        model.addCaret(new CaretRange(2, 0, 2, 0));
        model.addCaret(new CaretRange(1, 0, 1, 0));

        List<CaretRange> all = model.allCarets(document);
        assertEquals(3, all.size());
        assertTrue(all.get(0).getCaretOffset(document) <= all.get(1).getCaretOffset(document));
        assertTrue(all.get(1).getCaretOffset(document) <= all.get(2).getCaretOffset(document));
    }

    @Test
    void clearSecondaryCarets() {
        model.addCaret(new CaretRange(1, 0, 1, 0));
        model.addCaret(new CaretRange(2, 0, 2, 0));
        assertTrue(model.hasMultipleCarets());

        model.clearSecondaryCarets();
        assertFalse(model.hasMultipleCarets());
        assertEquals(1, model.allCarets(document).size());
    }

    @Test
    void undoLastOccurrenceRemovesLastAdded() {
        CaretRange first = new CaretRange(1, 0, 1, 3);
        CaretRange second = new CaretRange(2, 0, 2, 3);
        model.addCaret(first);
        model.addCaret(second);
        assertEquals(3, model.allCarets(document).size());

        model.undoLastOccurrence();
        assertEquals(2, model.allCarets(document).size());
        // The second one was last added, so it should be removed
        List<CaretRange> secondaries = model.getSecondaryCarets();
        assertEquals(1, secondaries.size());
        assertEquals(first, secondaries.get(0));
    }

    @Test
    void undoLastOccurrenceOnEmptyStackIsNoOp() {
        model.undoLastOccurrence(); // should not throw
        assertFalse(model.hasMultipleCarets());
    }

    @Test
    void normalizeAndMergeDedupesOverlapping() {
        // Primary selects chars 0-5 on line 0
        selectionModel.moveCaret(0, 0);
        selectionModel.moveCaretWithSelection(0, 5);

        // Add overlapping secondary 3-8 on line 0
        model.addCaret(new CaretRange(0, 3, 0, 8));

        model.normalizeAndMerge(document);

        // Should merge into single range 0-8
        List<CaretRange> all = model.allCarets(document);
        assertEquals(1, all.size());
        assertEquals(0, all.get(0).getStartColumn());
        assertEquals(8, all.get(0).getEndColumn());
    }

    @Test
    void normalizeAndMergeKeepsNonOverlapping() {
        selectionModel.moveCaret(0, 0);
        model.addCaret(new CaretRange(2, 0, 2, 0));

        model.normalizeAndMerge(document);

        assertEquals(2, model.allCarets(document).size());
    }
}
