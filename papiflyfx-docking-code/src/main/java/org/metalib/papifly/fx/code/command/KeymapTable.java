package org.metalib.papifly.fx.code.command;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Maps physical key combinations to {@link EditorCommand} identifiers.
 * <p>
 * Builds a platform-appropriate table on first use. On macOS the "word"
 * modifier is {@code Alt}; on Windows/Linux it is {@code Ctrl}.
 */
public final class KeymapTable {

    private static final Map<KeyBinding, EditorCommand> TABLE = buildTable();

    private KeymapTable() {}

    /**
     * Resolves a JavaFX key event to an editor command.
     *
     * @param event key event to resolve
     * @return mapped editor command when a binding exists
     */
    public static Optional<EditorCommand> resolve(KeyEvent event) {
        boolean shift = event.isShiftDown();
        boolean shortcut = event.isControlDown() || event.isMetaDown();
        boolean alt = event.isAltDown();
        KeyBinding binding = new KeyBinding(event.getCode(), shift, shortcut, alt);
        return Optional.ofNullable(TABLE.get(binding));
    }

    static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    private static Map<KeyBinding, EditorCommand> buildTable() {
        Map<KeyBinding, EditorCommand> map = new HashMap<>();
        boolean mac = isMac();

        // --- basic navigation (no modifiers) ---
        put(map, KeyCode.LEFT, false, false, false, EditorCommand.MOVE_LEFT);
        put(map, KeyCode.RIGHT, false, false, false, EditorCommand.MOVE_RIGHT);
        put(map, KeyCode.UP, false, false, false, EditorCommand.MOVE_UP);
        put(map, KeyCode.DOWN, false, false, false, EditorCommand.MOVE_DOWN);
        put(map, KeyCode.HOME, false, false, false, EditorCommand.LINE_START);
        put(map, KeyCode.END, false, false, false, EditorCommand.LINE_END);
        put(map, KeyCode.PAGE_UP, false, false, false, EditorCommand.MOVE_PAGE_UP);
        put(map, KeyCode.PAGE_DOWN, false, false, false, EditorCommand.MOVE_PAGE_DOWN);

        // --- shift selection ---
        put(map, KeyCode.LEFT, true, false, false, EditorCommand.SELECT_LEFT);
        put(map, KeyCode.RIGHT, true, false, false, EditorCommand.SELECT_RIGHT);
        put(map, KeyCode.UP, true, false, false, EditorCommand.SELECT_UP);
        put(map, KeyCode.DOWN, true, false, false, EditorCommand.SELECT_DOWN);
        put(map, KeyCode.HOME, true, false, false, EditorCommand.SELECT_TO_LINE_START);
        put(map, KeyCode.END, true, false, false, EditorCommand.SELECT_TO_LINE_END);
        put(map, KeyCode.PAGE_UP, true, false, false, EditorCommand.SELECT_PAGE_UP);
        put(map, KeyCode.PAGE_DOWN, true, false, false, EditorCommand.SELECT_PAGE_DOWN);

        // --- editing ---
        put(map, KeyCode.BACK_SPACE, false, false, false, EditorCommand.BACKSPACE);
        put(map, KeyCode.DELETE, false, false, false, EditorCommand.DELETE);
        put(map, KeyCode.ENTER, false, false, false, EditorCommand.ENTER);

        // --- clipboard / undo / redo / select-all (shortcut) ---
        put(map, KeyCode.A, false, true, false, EditorCommand.SELECT_ALL);
        put(map, KeyCode.C, false, true, false, EditorCommand.COPY);
        put(map, KeyCode.X, false, true, false, EditorCommand.CUT);
        put(map, KeyCode.V, false, true, false, EditorCommand.PASTE);
        put(map, KeyCode.Z, false, true, false, EditorCommand.UNDO);
        put(map, KeyCode.Z, true, true, false, EditorCommand.REDO);
        put(map, KeyCode.Y, false, true, false, EditorCommand.REDO);

        // --- search / go-to-line ---
        put(map, KeyCode.F, false, true, false, EditorCommand.OPEN_SEARCH);
        put(map, KeyCode.G, false, true, false, EditorCommand.GO_TO_LINE);
        // Open replace: Cmd+Option+F on mac, Ctrl+H on others
        if (mac) {
            put(map, KeyCode.F, false, true, true, EditorCommand.OPEN_REPLACE);
        } else {
            put(map, KeyCode.H, false, true, false, EditorCommand.OPEN_REPLACE);
        }

        // --- word navigation ---
        // macOS: Alt+Arrow   Windows/Linux: Ctrl+Arrow
        if (mac) {
            put(map, KeyCode.LEFT, false, false, true, EditorCommand.MOVE_WORD_LEFT);
            put(map, KeyCode.RIGHT, false, false, true, EditorCommand.MOVE_WORD_RIGHT);
            put(map, KeyCode.LEFT, true, false, true, EditorCommand.SELECT_WORD_LEFT);
            put(map, KeyCode.RIGHT, true, false, true, EditorCommand.SELECT_WORD_RIGHT);
            put(map, KeyCode.BACK_SPACE, false, false, true, EditorCommand.DELETE_WORD_LEFT);
            put(map, KeyCode.DELETE, false, false, true, EditorCommand.DELETE_WORD_RIGHT);
        } else {
            put(map, KeyCode.LEFT, false, true, false, EditorCommand.MOVE_WORD_LEFT);
            put(map, KeyCode.RIGHT, false, true, false, EditorCommand.MOVE_WORD_RIGHT);
            put(map, KeyCode.LEFT, true, true, false, EditorCommand.SELECT_WORD_LEFT);
            put(map, KeyCode.RIGHT, true, true, false, EditorCommand.SELECT_WORD_RIGHT);
            put(map, KeyCode.BACK_SPACE, false, true, false, EditorCommand.DELETE_WORD_LEFT);
            put(map, KeyCode.DELETE, false, true, false, EditorCommand.DELETE_WORD_RIGHT);
        }

        // --- document start / end ---
        if (mac) {
            // Cmd+Up / Cmd+Down on macOS
            put(map, KeyCode.UP, false, true, false, EditorCommand.DOCUMENT_START);
            put(map, KeyCode.DOWN, false, true, false, EditorCommand.DOCUMENT_END);
            put(map, KeyCode.UP, true, true, false, EditorCommand.SELECT_TO_DOCUMENT_START);
            put(map, KeyCode.DOWN, true, true, false, EditorCommand.SELECT_TO_DOCUMENT_END);

            // Alias for full-size keyboards and editors that expose Home/End navigation.
            put(map, KeyCode.HOME, false, true, false, EditorCommand.DOCUMENT_START);
            put(map, KeyCode.END, false, true, false, EditorCommand.DOCUMENT_END);
            put(map, KeyCode.HOME, true, true, false, EditorCommand.SELECT_TO_DOCUMENT_START);
            put(map, KeyCode.END, true, true, false, EditorCommand.SELECT_TO_DOCUMENT_END);
        } else {
            put(map, KeyCode.HOME, false, true, false, EditorCommand.DOCUMENT_START);
            put(map, KeyCode.END, false, true, false, EditorCommand.DOCUMENT_END);
            put(map, KeyCode.HOME, true, true, false, EditorCommand.SELECT_TO_DOCUMENT_START);
            put(map, KeyCode.END, true, true, false, EditorCommand.SELECT_TO_DOCUMENT_END);
        }

        // --- line operations ---
        // Delete line: Ctrl+Shift+K (both platforms)
        put(map, KeyCode.K, true, true, false, EditorCommand.DELETE_LINE);

        // Move line: Alt+Up / Alt+Down (both platforms)
        put(map, KeyCode.UP, false, false, true, EditorCommand.MOVE_LINE_UP);
        put(map, KeyCode.DOWN, false, false, true, EditorCommand.MOVE_LINE_DOWN);

        // Duplicate line: Alt+Shift+Up / Alt+Shift+Down
        put(map, KeyCode.UP, true, false, true, EditorCommand.DUPLICATE_LINE_UP);
        put(map, KeyCode.DOWN, true, false, true, EditorCommand.DUPLICATE_LINE_DOWN);

        // Join lines: Ctrl+J
        put(map, KeyCode.J, false, true, false, EditorCommand.JOIN_LINES);

        // --- page scroll without caret move ---
        // Alt+PageUp/PageDown on both platforms.
        put(map, KeyCode.PAGE_UP, false, false, true, EditorCommand.SCROLL_PAGE_UP);
        put(map, KeyCode.PAGE_DOWN, false, false, true, EditorCommand.SCROLL_PAGE_DOWN);
        // Cmd+PageUp/PageDown on macOS.
        if (mac) {
            put(map, KeyCode.PAGE_UP, false, true, false, EditorCommand.SCROLL_PAGE_UP);
            put(map, KeyCode.PAGE_DOWN, false, true, false, EditorCommand.SCROLL_PAGE_DOWN);
        }

        // --- macOS word-move keys override some Alt+Arrow entries ---
        // On macOS Alt+Up/Down are already word nav on some editors,
        // but VS Code uses them for line move, so we keep line-move.

        // --- multi-caret (Phase 3) ---
        // Select next occurrence: Cmd+D / Ctrl+D
        put(map, KeyCode.D, false, true, false, EditorCommand.SELECT_NEXT_OCCURRENCE);
        // Select all occurrences: Cmd+Shift+L / Ctrl+Shift+L
        put(map, KeyCode.L, true, true, false, EditorCommand.SELECT_ALL_OCCURRENCES);
        // Add cursor up/down: Cmd+Alt+Up / Ctrl+Alt+Up, Cmd+Alt+Down / Ctrl+Alt+Down
        put(map, KeyCode.UP, false, true, true, EditorCommand.ADD_CURSOR_UP);
        put(map, KeyCode.DOWN, false, true, true, EditorCommand.ADD_CURSOR_DOWN);
        // Undo last occurrence: Cmd+U / Ctrl+U
        put(map, KeyCode.U, false, true, false, EditorCommand.UNDO_LAST_OCCURRENCE);

        // --- folding ---
        put(map, KeyCode.OPEN_BRACKET, false, true, true, EditorCommand.TOGGLE_FOLD);
        put(map, KeyCode.CLOSE_BRACKET, false, true, true, EditorCommand.FOLD_RECURSIVE);
        put(map, KeyCode.BACK_SLASH, false, true, true, EditorCommand.UNFOLD_RECURSIVE);
        put(map, KeyCode.MINUS, false, true, true, EditorCommand.FOLD_ALL);
        put(map, KeyCode.EQUALS, false, true, true, EditorCommand.UNFOLD_ALL);

        return Map.copyOf(map);
    }

    private static void put(Map<KeyBinding, EditorCommand> map,
                             KeyCode code, boolean shift, boolean shortcut, boolean alt,
                             EditorCommand command) {
        map.put(new KeyBinding(code, shift, shortcut, alt), command);
    }
}
