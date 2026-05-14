package org.metalib.papifly.fx.code.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.render.SelectionModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LineOperationsTest {

    private Document document;
    private SelectionModel selectionModel;
    private LineEditService service;

    @BeforeEach
    void setUp() {
        document = new Document();
        selectionModel = new SelectionModel();
        service = new LineEditService();
    }

    @Test
    void resolveSelectionOrCaretBlockFromCaret() {
        document.setText("aaa\nbbb\nccc");
        selectionModel.moveCaret(1, 2);

        LineBlock block = service.resolveSelectionOrCaretBlock(document, selectionModel);

        assertEquals(1, block.startLine());
        assertEquals(1, block.endLine());
        assertEquals("bbb\n", block.text());
    }

    @Test
    void resolveSelectionOrCaretBlockFromSelection() {
        document.setText("aaa\nbbb\nccc\nddd");
        selectionModel.moveCaret(1, 0);
        selectionModel.moveCaretWithSelection(2, 2);

        LineBlock block = service.resolveSelectionOrCaretBlock(document, selectionModel);

        assertEquals(1, block.startLine());
        assertEquals(2, block.endLine());
        assertEquals("bbb\nccc\n", block.text());
    }

    @Test
    void deleteBlockMiddle() {
        document.setText("aaa\nbbb\nccc");
        LineBlock block = LineBlock.fromLines(document, 1, 1);

        assertTrue(service.deleteBlock(document, block));
        assertEquals("aaa\nccc", document.getText());
    }

    @Test
    void deleteBlockLastLineWithoutTrailingNewline() {
        document.setText("aaa\nbbb\nccc");
        LineBlock block = LineBlock.fromLines(document, 2, 2);

        assertTrue(service.deleteBlock(document, block));
        assertEquals("aaa\nbbb", document.getText());
    }

    @Test
    void moveBlockUpSwapsWithPreviousLine() {
        document.setText("aaa\nbbb\nccc");
        LineBlock block = LineBlock.fromLines(document, 1, 1);

        assertTrue(service.moveBlockUp(document, block));
        assertEquals("bbb\naaa\nccc", document.getText());
    }

    @Test
    void moveBlockDownSwapsWithNextLine() {
        document.setText("aaa\nbbb\nccc");
        LineBlock block = LineBlock.fromLines(document, 0, 0);

        assertTrue(service.moveBlockDown(document, block));
        assertEquals("bbb\naaa\nccc", document.getText());
    }

    @Test
    void moveBlockDownAtBottomIsNoOp() {
        document.setText("aaa\nbbb\nccc");
        LineBlock block = LineBlock.fromLines(document, 2, 2);

        assertFalse(service.moveBlockDown(document, block));
        assertEquals("aaa\nbbb\nccc", document.getText());
    }

    @Test
    void duplicateBlockUpCopiesSelection() {
        document.setText("aaa\nbbb\nccc");
        LineBlock block = LineBlock.fromLines(document, 1, 1);

        service.duplicateBlockUp(document, block);
        assertEquals("aaa\nbbb\nbbb\nccc", document.getText());
    }

    @Test
    void duplicateBlockDownHandlesLastLineWithoutTrailingNewline() {
        document.setText("aaa\nbbb\nccc");
        LineBlock block = LineBlock.fromLines(document, 2, 2);

        service.duplicateBlockDown(document, block);
        assertEquals("aaa\nbbb\nccc\nccc", document.getText());
    }

    @Test
    void joinLineWithNextReplacesNewlineWithSpace() {
        document.setText("hello\nworld");

        assertTrue(service.joinLineWithNext(document, 0));
        assertEquals("hello world", document.getText());
    }
}
