package org.metalib.papifly.fx.code.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.command.CaretRange;
import org.metalib.papifly.fx.code.command.MultiCaretModel;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.render.SelectionModel;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorEditControllerTest {

    private Document document;
    private SelectionModel selectionModel;
    private MultiCaretModel multiCaretModel;
    private AtomicBoolean markDirtyCalled;
    private AtomicReference<String> clipboard;
    private EditorEditController controller;

    @BeforeEach
    void setUp() {
        document = new Document("abc\ndef");
        selectionModel = new SelectionModel();
        multiCaretModel = new MultiCaretModel(selectionModel);
        markDirtyCalled = new AtomicBoolean(false);
        clipboard = new AtomicReference<>("");
        controller = new EditorEditController(
            document,
            selectionModel,
            multiCaretModel,
            () -> markDirtyCalled.set(true),
            this::moveCaretToOffset,
            clipboard::get,
            clipboard::set
        );
    }

    @Test
    void insertTypedCharacterReplacesSelectionAndMovesCaret() {
        selectionModel.moveCaret(0, 1);
        selectionModel.moveCaretWithSelection(0, 3);

        controller.insertTypedCharacter("Z");

        assertEquals("aZ\ndef", document.getText());
        assertEquals(0, selectionModel.getCaretLine());
        assertEquals(2, selectionModel.getCaretColumn());
        assertFalse(selectionModel.hasSelection());
    }

    @Test
    void backspaceAcrossMultiCaretIsCompoundAndClearsSecondaries() {
        selectionModel.moveCaret(0, 3);
        multiCaretModel.addCaretNoStack(new CaretRange(1, 3, 1, 3));

        controller.handleBackspace();

        assertEquals("ab\nde", document.getText());
        assertTrue(markDirtyCalled.get());
        assertFalse(multiCaretModel.hasMultipleCarets());
        assertTrue(document.undo());
        assertEquals("abc\ndef", document.getText());
    }

    @Test
    void copyWritesSelectionToClipboardAndPasteUsesClipboard() {
        selectionModel.moveCaret(0, 0);
        selectionModel.moveCaretWithSelection(0, 3);

        controller.handleCopy();

        assertEquals("abc", clipboard.get());

        selectionModel.moveCaret(1, 0);
        controller.handlePaste();

        assertEquals("abc\nabcdef", document.getText());
    }

    @Test
    void cutAcrossMultiCaretSelectionsDeletesAllSelections() {
        document = new Document("foo bar foo");
        selectionModel = new SelectionModel();
        multiCaretModel = new MultiCaretModel(selectionModel);
        markDirtyCalled.set(false);
        controller = new EditorEditController(
            document,
            selectionModel,
            multiCaretModel,
            () -> markDirtyCalled.set(true),
            this::moveCaretToOffset,
            clipboard::get,
            clipboard::set
        );

        selectionModel.moveCaret(0, 0);
        selectionModel.moveCaretWithSelection(0, 3);
        multiCaretModel.addCaretNoStack(new CaretRange(0, 8, 0, 11));

        controller.handleCut();

        assertEquals(" bar ", document.getText());
        assertTrue(markDirtyCalled.get());
        assertFalse(multiCaretModel.hasMultipleCarets());
    }

    private void moveCaretToOffset(int offset) {
        int safeOffset = Math.max(0, Math.min(offset, document.length()));
        int line = document.getLineForOffset(safeOffset);
        int column = document.getColumnForOffset(safeOffset);
        selectionModel.moveCaret(line, column);
    }
}
