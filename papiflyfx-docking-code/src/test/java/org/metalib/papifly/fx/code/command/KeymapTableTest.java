package org.metalib.papifly.fx.code.command;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeymapTableTest {

    // Helper: create a KeyBinding and look it up directly
    private Optional<EditorCommand> lookup(KeyCode code, boolean shift, boolean shortcut, boolean alt) {
        KeyBinding binding = new KeyBinding(code, shift, shortcut, alt);
        // We can't easily create KeyEvent without FX toolkit, so test via the map directly.
        // The resolve() method does the same extraction, just from a KeyEvent.
        // We'll test the table contents through KeyBinding lookups.
        try {
            var field = KeymapTable.class.getDeclaredField("TABLE");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var table = (java.util.Map<KeyBinding, EditorCommand>) field.get(null);
            return Optional.ofNullable(table.get(binding));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void basicArrowKeysMapToMove() {
        assertEquals(Optional.of(EditorCommand.MOVE_LEFT), lookup(KeyCode.LEFT, false, false, false));
        assertEquals(Optional.of(EditorCommand.MOVE_RIGHT), lookup(KeyCode.RIGHT, false, false, false));
        assertEquals(Optional.of(EditorCommand.MOVE_UP), lookup(KeyCode.UP, false, false, false));
        assertEquals(Optional.of(EditorCommand.MOVE_DOWN), lookup(KeyCode.DOWN, false, false, false));
    }

    @Test
    void shiftArrowKeysMapToSelect() {
        assertEquals(Optional.of(EditorCommand.SELECT_LEFT), lookup(KeyCode.LEFT, true, false, false));
        assertEquals(Optional.of(EditorCommand.SELECT_RIGHT), lookup(KeyCode.RIGHT, true, false, false));
        assertEquals(Optional.of(EditorCommand.SELECT_UP), lookup(KeyCode.UP, true, false, false));
        assertEquals(Optional.of(EditorCommand.SELECT_DOWN), lookup(KeyCode.DOWN, true, false, false));
    }

    @Test
    void homeEndMapToLineStartEnd() {
        assertEquals(Optional.of(EditorCommand.LINE_START), lookup(KeyCode.HOME, false, false, false));
        assertEquals(Optional.of(EditorCommand.LINE_END), lookup(KeyCode.END, false, false, false));
    }

    @Test
    void shiftHomeEndMapToSelectLineStartEnd() {
        assertEquals(Optional.of(EditorCommand.SELECT_TO_LINE_START), lookup(KeyCode.HOME, true, false, false));
        assertEquals(Optional.of(EditorCommand.SELECT_TO_LINE_END), lookup(KeyCode.END, true, false, false));
    }

    @Test
    void pageNavigationBindings() {
        assertEquals(Optional.of(EditorCommand.MOVE_PAGE_UP), lookup(KeyCode.PAGE_UP, false, false, false));
        assertEquals(Optional.of(EditorCommand.MOVE_PAGE_DOWN), lookup(KeyCode.PAGE_DOWN, false, false, false));
        assertEquals(Optional.of(EditorCommand.SELECT_PAGE_UP), lookup(KeyCode.PAGE_UP, true, false, false));
        assertEquals(Optional.of(EditorCommand.SELECT_PAGE_DOWN), lookup(KeyCode.PAGE_DOWN, true, false, false));
    }

    @Test
    void pageScrollBindings() {
        assertEquals(Optional.of(EditorCommand.SCROLL_PAGE_UP), lookup(KeyCode.PAGE_UP, false, false, true));
        assertEquals(Optional.of(EditorCommand.SCROLL_PAGE_DOWN), lookup(KeyCode.PAGE_DOWN, false, false, true));

        if (KeymapTable.isMac()) {
            assertEquals(Optional.of(EditorCommand.SCROLL_PAGE_UP), lookup(KeyCode.PAGE_UP, false, true, false));
            assertEquals(Optional.of(EditorCommand.SCROLL_PAGE_DOWN), lookup(KeyCode.PAGE_DOWN, false, true, false));
        } else {
            assertTrue(lookup(KeyCode.PAGE_UP, false, true, false).isEmpty());
            assertTrue(lookup(KeyCode.PAGE_DOWN, false, true, false).isEmpty());
        }
    }

    @Test
    void editingKeys() {
        assertEquals(Optional.of(EditorCommand.BACKSPACE), lookup(KeyCode.BACK_SPACE, false, false, false));
        assertEquals(Optional.of(EditorCommand.DELETE), lookup(KeyCode.DELETE, false, false, false));
        assertEquals(Optional.of(EditorCommand.ENTER), lookup(KeyCode.ENTER, false, false, false));
    }

    @Test
    void clipboardShortcuts() {
        assertEquals(Optional.of(EditorCommand.COPY), lookup(KeyCode.C, false, true, false));
        assertEquals(Optional.of(EditorCommand.CUT), lookup(KeyCode.X, false, true, false));
        assertEquals(Optional.of(EditorCommand.PASTE), lookup(KeyCode.V, false, true, false));
    }

    @Test
    void undoRedo() {
        assertEquals(Optional.of(EditorCommand.UNDO), lookup(KeyCode.Z, false, true, false));
        assertEquals(Optional.of(EditorCommand.REDO), lookup(KeyCode.Z, true, true, false));
        assertEquals(Optional.of(EditorCommand.REDO), lookup(KeyCode.Y, false, true, false));
    }

    @Test
    void selectAll() {
        assertEquals(Optional.of(EditorCommand.SELECT_ALL), lookup(KeyCode.A, false, true, false));
    }

    @Test
    void searchAndGoToLine() {
        assertEquals(Optional.of(EditorCommand.OPEN_SEARCH), lookup(KeyCode.F, false, true, false));
        assertEquals(Optional.of(EditorCommand.GO_TO_LINE), lookup(KeyCode.G, false, true, false));
    }

    @Test
    void wordNavigation() {
        boolean mac = KeymapTable.isMac();
        if (mac) {
            assertEquals(Optional.of(EditorCommand.MOVE_WORD_LEFT), lookup(KeyCode.LEFT, false, false, true));
            assertEquals(Optional.of(EditorCommand.MOVE_WORD_RIGHT), lookup(KeyCode.RIGHT, false, false, true));
            assertEquals(Optional.of(EditorCommand.SELECT_WORD_LEFT), lookup(KeyCode.LEFT, true, false, true));
            assertEquals(Optional.of(EditorCommand.SELECT_WORD_RIGHT), lookup(KeyCode.RIGHT, true, false, true));
            assertEquals(Optional.of(EditorCommand.DELETE_WORD_LEFT), lookup(KeyCode.BACK_SPACE, false, false, true));
            assertEquals(Optional.of(EditorCommand.DELETE_WORD_RIGHT), lookup(KeyCode.DELETE, false, false, true));
        } else {
            assertEquals(Optional.of(EditorCommand.MOVE_WORD_LEFT), lookup(KeyCode.LEFT, false, true, false));
            assertEquals(Optional.of(EditorCommand.MOVE_WORD_RIGHT), lookup(KeyCode.RIGHT, false, true, false));
            assertEquals(Optional.of(EditorCommand.SELECT_WORD_LEFT), lookup(KeyCode.LEFT, true, true, false));
            assertEquals(Optional.of(EditorCommand.SELECT_WORD_RIGHT), lookup(KeyCode.RIGHT, true, true, false));
            assertEquals(Optional.of(EditorCommand.DELETE_WORD_LEFT), lookup(KeyCode.BACK_SPACE, false, true, false));
            assertEquals(Optional.of(EditorCommand.DELETE_WORD_RIGHT), lookup(KeyCode.DELETE, false, true, false));
        }
    }

    @Test
    void documentBoundaries() {
        boolean mac = KeymapTable.isMac();
        if (mac) {
            assertEquals(Optional.of(EditorCommand.DOCUMENT_START), lookup(KeyCode.UP, false, true, false));
            assertEquals(Optional.of(EditorCommand.DOCUMENT_END), lookup(KeyCode.DOWN, false, true, false));
            assertEquals(Optional.of(EditorCommand.SELECT_TO_DOCUMENT_START), lookup(KeyCode.UP, true, true, false));
            assertEquals(Optional.of(EditorCommand.SELECT_TO_DOCUMENT_END), lookup(KeyCode.DOWN, true, true, false));
            assertEquals(Optional.of(EditorCommand.DOCUMENT_START), lookup(KeyCode.HOME, false, true, false));
            assertEquals(Optional.of(EditorCommand.DOCUMENT_END), lookup(KeyCode.END, false, true, false));
            assertEquals(Optional.of(EditorCommand.SELECT_TO_DOCUMENT_START), lookup(KeyCode.HOME, true, true, false));
            assertEquals(Optional.of(EditorCommand.SELECT_TO_DOCUMENT_END), lookup(KeyCode.END, true, true, false));
        } else {
            assertEquals(Optional.of(EditorCommand.DOCUMENT_START), lookup(KeyCode.HOME, false, true, false));
            assertEquals(Optional.of(EditorCommand.DOCUMENT_END), lookup(KeyCode.END, false, true, false));
            assertEquals(Optional.of(EditorCommand.SELECT_TO_DOCUMENT_START), lookup(KeyCode.HOME, true, true, false));
            assertEquals(Optional.of(EditorCommand.SELECT_TO_DOCUMENT_END), lookup(KeyCode.END, true, true, false));
        }
    }

    @Test
    void lineOperations() {
        assertEquals(Optional.of(EditorCommand.DELETE_LINE), lookup(KeyCode.K, true, true, false));
        assertEquals(Optional.of(EditorCommand.MOVE_LINE_UP), lookup(KeyCode.UP, false, false, true));
        assertEquals(Optional.of(EditorCommand.MOVE_LINE_DOWN), lookup(KeyCode.DOWN, false, false, true));
        assertEquals(Optional.of(EditorCommand.DUPLICATE_LINE_UP), lookup(KeyCode.UP, true, false, true));
        assertEquals(Optional.of(EditorCommand.DUPLICATE_LINE_DOWN), lookup(KeyCode.DOWN, true, false, true));
        assertEquals(Optional.of(EditorCommand.JOIN_LINES), lookup(KeyCode.J, false, true, false));
    }

    @Test
    void multiCaretBindings() {
        // Ctrl/Cmd+D -> SELECT_NEXT_OCCURRENCE
        assertEquals(Optional.of(EditorCommand.SELECT_NEXT_OCCURRENCE), lookup(KeyCode.D, false, true, false));
        // Ctrl/Cmd+Shift+L -> SELECT_ALL_OCCURRENCES
        assertEquals(Optional.of(EditorCommand.SELECT_ALL_OCCURRENCES), lookup(KeyCode.L, true, true, false));
        // Ctrl/Cmd+Alt+Up -> ADD_CURSOR_UP
        assertEquals(Optional.of(EditorCommand.ADD_CURSOR_UP), lookup(KeyCode.UP, false, true, true));
        // Ctrl/Cmd+Alt+Down -> ADD_CURSOR_DOWN
        assertEquals(Optional.of(EditorCommand.ADD_CURSOR_DOWN), lookup(KeyCode.DOWN, false, true, true));
        // Ctrl/Cmd+U -> UNDO_LAST_OCCURRENCE
        assertEquals(Optional.of(EditorCommand.UNDO_LAST_OCCURRENCE), lookup(KeyCode.U, false, true, false));
    }

    @Test
    void foldingBindings() {
        assertEquals(Optional.of(EditorCommand.TOGGLE_FOLD), lookup(KeyCode.OPEN_BRACKET, false, true, true));
        assertEquals(Optional.of(EditorCommand.FOLD_RECURSIVE), lookup(KeyCode.CLOSE_BRACKET, false, true, true));
        assertEquals(Optional.of(EditorCommand.UNFOLD_RECURSIVE), lookup(KeyCode.BACK_SLASH, false, true, true));
        assertEquals(Optional.of(EditorCommand.FOLD_ALL), lookup(KeyCode.MINUS, false, true, true));
        assertEquals(Optional.of(EditorCommand.UNFOLD_ALL), lookup(KeyCode.EQUALS, false, true, true));
    }

    @Test
    void unmappedKeyReturnsEmpty() {
        assertTrue(lookup(KeyCode.Q, false, false, false).isEmpty());
        assertTrue(lookup(KeyCode.DIGIT0, false, false, false).isEmpty());
    }
}
