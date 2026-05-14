package org.metalib.papifly.fx.code.command;

/**
 * Identifies every editor action that can be triggered by a keystroke.
 */
public enum EditorCommand {
    /**
     * Undo last edit operation.
     */
    UNDO,
    /**
     * Redo last undone edit operation.
     */
    REDO,
    /**
     * Copy current selection.
     */
    COPY,
    /**
     * Cut current selection.
     */
    CUT,
    /**
     * Paste clipboard content.
     */
    PASTE,
    /**
     * Select entire document.
     */
    SELECT_ALL,
    /**
     * Move caret one character left.
     */
    MOVE_LEFT,
    /**
     * Move caret one character right.
     */
    MOVE_RIGHT,
    /**
     * Move caret one visual line up.
     */
    MOVE_UP,
    /**
     * Move caret one visual line down.
     */
    MOVE_DOWN,
    /**
     * Extend selection one character left.
     */
    SELECT_LEFT,
    /**
     * Extend selection one character right.
     */
    SELECT_RIGHT,
    /**
     * Extend selection one visual line up.
     */
    SELECT_UP,
    /**
     * Extend selection one visual line down.
     */
    SELECT_DOWN,
    /**
     * Move caret up by one page.
     */
    MOVE_PAGE_UP,
    /**
     * Move caret down by one page.
     */
    MOVE_PAGE_DOWN,
    /**
     * Extend selection up by one page.
     */
    SELECT_PAGE_UP,
    /**
     * Extend selection down by one page.
     */
    SELECT_PAGE_DOWN,
    /**
     * Scroll viewport one page up without moving selection anchor.
     */
    SCROLL_PAGE_UP,
    /**
     * Scroll viewport one page down without moving selection anchor.
     */
    SCROLL_PAGE_DOWN,
    /**
     * Move caret to line start.
     */
    LINE_START,
    /**
     * Move caret to line end.
     */
    LINE_END,
    /**
     * Extend selection to line start.
     */
    SELECT_TO_LINE_START,
    /**
     * Extend selection to line end.
     */
    SELECT_TO_LINE_END,
    /**
     * Delete character before caret.
     */
    BACKSPACE,
    /**
     * Delete character after caret.
     */
    DELETE,
    /**
     * Insert newline.
     */
    ENTER,
    /**
     * Move caret to previous word boundary.
     */
    MOVE_WORD_LEFT,
    /**
     * Move caret to next word boundary.
     */
    MOVE_WORD_RIGHT,
    /**
     * Extend selection to previous word boundary.
     */
    SELECT_WORD_LEFT,
    /**
     * Extend selection to next word boundary.
     */
    SELECT_WORD_RIGHT,
    /**
     * Delete word to the left of caret.
     */
    DELETE_WORD_LEFT,
    /**
     * Delete word to the right of caret.
     */
    DELETE_WORD_RIGHT,
    /**
     * Move caret to document start.
     */
    DOCUMENT_START,
    /**
     * Move caret to document end.
     */
    DOCUMENT_END,
    /**
     * Extend selection to document start.
     */
    SELECT_TO_DOCUMENT_START,
    /**
     * Extend selection to document end.
     */
    SELECT_TO_DOCUMENT_END,
    /**
     * Delete current line.
     */
    DELETE_LINE,
    /**
     * Move current line up.
     */
    MOVE_LINE_UP,
    /**
     * Move current line down.
     */
    MOVE_LINE_DOWN,
    /**
     * Duplicate current line above.
     */
    DUPLICATE_LINE_UP,
    /**
     * Duplicate current line below.
     */
    DUPLICATE_LINE_DOWN,
    /**
     * Join current line with following line.
     */
    JOIN_LINES,
    /**
     * Select next occurrence of current selection.
     */
    SELECT_NEXT_OCCURRENCE,
    /**
     * Select all occurrences of current selection.
     */
    SELECT_ALL_OCCURRENCES,
    /**
     * Add caret above current caret.
     */
    ADD_CURSOR_UP,
    /**
     * Add caret below current caret.
     */
    ADD_CURSOR_DOWN,
    /**
     * Undo most recent multi-occurrence selection addition.
     */
    UNDO_LAST_OCCURRENCE,
    /**
     * Toggle fold state at current line.
     */
    TOGGLE_FOLD,
    /**
     * Collapse all foldable regions.
     */
    FOLD_ALL,
    /**
     * Expand all folded regions.
     */
    UNFOLD_ALL,
    /**
     * Collapse nested regions recursively from current context.
     */
    FOLD_RECURSIVE,
    /**
     * Expand nested regions recursively from current context.
     */
    UNFOLD_RECURSIVE,
    /**
     * Open search overlay.
     */
    OPEN_SEARCH,
    /**
     * Open replace overlay.
     */
    OPEN_REPLACE,
    /**
     * Open go-to-line overlay.
     */
    GO_TO_LINE
}
