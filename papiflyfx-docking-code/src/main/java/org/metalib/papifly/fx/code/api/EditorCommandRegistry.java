package org.metalib.papifly.fx.code.api;

import org.metalib.papifly.fx.code.command.EditorCommand;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Registers command handlers on {@link EditorCommandExecutor}.
 */
final class EditorCommandRegistry {

    void register(EditorCommandExecutor executor, Map<EditorCommand, Runnable> handlers) {
        Objects.requireNonNull(executor, "executor");
        Objects.requireNonNull(handlers, "handlers");
        for (Map.Entry<EditorCommand, Runnable> entry : handlers.entrySet()) {
            executor.register(entry.getKey(), entry.getValue());
        }
    }

    void registerDefault(
        EditorCommandExecutor executor,
        EditorNavigationController navigationController,
        EditorEditController editController,
        Runnable openSearchAction,
        Runnable openReplaceAction,
        Runnable goToLineAction,
        Runnable toggleFoldAction,
        Runnable foldAllAction,
        Runnable unfoldAllAction,
        Runnable foldRecursiveAction,
        Runnable unfoldRecursiveAction
    ) {
        Objects.requireNonNull(navigationController, "navigationController");
        Objects.requireNonNull(editController, "editController");
        Objects.requireNonNull(openSearchAction, "openSearchAction");
        Objects.requireNonNull(openReplaceAction, "openReplaceAction");
        Objects.requireNonNull(goToLineAction, "goToLineAction");
        Objects.requireNonNull(toggleFoldAction, "toggleFoldAction");
        Objects.requireNonNull(foldAllAction, "foldAllAction");
        Objects.requireNonNull(unfoldAllAction, "unfoldAllAction");
        Objects.requireNonNull(foldRecursiveAction, "foldRecursiveAction");
        Objects.requireNonNull(unfoldRecursiveAction, "unfoldRecursiveAction");

        Map<EditorCommand, Runnable> handlers = new EnumMap<>(EditorCommand.class);

        // Navigation
        handlers.put(EditorCommand.MOVE_LEFT, () -> navigationController.moveLeft(false));
        handlers.put(EditorCommand.MOVE_RIGHT, () -> navigationController.moveRight(false));
        handlers.put(EditorCommand.MOVE_UP, () -> navigationController.moveUp(false));
        handlers.put(EditorCommand.MOVE_DOWN, () -> navigationController.moveDown(false));
        handlers.put(EditorCommand.MOVE_PAGE_UP, () -> navigationController.pageUp(false));
        handlers.put(EditorCommand.MOVE_PAGE_DOWN, () -> navigationController.pageDown(false));
        handlers.put(EditorCommand.SELECT_LEFT, () -> navigationController.moveLeft(true));
        handlers.put(EditorCommand.SELECT_RIGHT, () -> navigationController.moveRight(true));
        handlers.put(EditorCommand.SELECT_UP, () -> navigationController.moveUp(true));
        handlers.put(EditorCommand.SELECT_DOWN, () -> navigationController.moveDown(true));
        handlers.put(EditorCommand.SELECT_PAGE_UP, () -> navigationController.pageUp(true));
        handlers.put(EditorCommand.SELECT_PAGE_DOWN, () -> navigationController.pageDown(true));
        handlers.put(EditorCommand.SCROLL_PAGE_UP, navigationController::scrollPageUp);
        handlers.put(EditorCommand.SCROLL_PAGE_DOWN, navigationController::scrollPageDown);
        handlers.put(EditorCommand.LINE_START, () -> navigationController.lineStart(false));
        handlers.put(EditorCommand.LINE_END, () -> navigationController.lineEnd(false));
        handlers.put(EditorCommand.SELECT_TO_LINE_START, () -> navigationController.lineStart(true));
        handlers.put(EditorCommand.SELECT_TO_LINE_END, () -> navigationController.lineEnd(true));

        // Editing
        handlers.put(EditorCommand.BACKSPACE, editController::handleBackspace);
        handlers.put(EditorCommand.DELETE, editController::handleDelete);
        handlers.put(EditorCommand.ENTER, editController::handleEnter);

        // Clipboard and undo
        handlers.put(EditorCommand.SELECT_ALL, navigationController::selectAll);
        handlers.put(EditorCommand.UNDO, navigationController::undo);
        handlers.put(EditorCommand.REDO, navigationController::redo);
        handlers.put(EditorCommand.COPY, editController::handleCopy);
        handlers.put(EditorCommand.CUT, editController::handleCut);
        handlers.put(EditorCommand.PASTE, editController::handlePaste);

        // Search
        handlers.put(EditorCommand.OPEN_SEARCH, openSearchAction);
        handlers.put(EditorCommand.OPEN_REPLACE, openReplaceAction);
        handlers.put(EditorCommand.GO_TO_LINE, goToLineAction);

        // Word navigation
        handlers.put(EditorCommand.MOVE_WORD_LEFT, navigationController::moveWordLeft);
        handlers.put(EditorCommand.MOVE_WORD_RIGHT, navigationController::moveWordRight);
        handlers.put(EditorCommand.SELECT_WORD_LEFT, navigationController::selectWordLeft);
        handlers.put(EditorCommand.SELECT_WORD_RIGHT, navigationController::selectWordRight);
        handlers.put(EditorCommand.DELETE_WORD_LEFT, navigationController::deleteWordLeft);
        handlers.put(EditorCommand.DELETE_WORD_RIGHT, navigationController::deleteWordRight);

        // Document boundaries
        handlers.put(EditorCommand.DOCUMENT_START, () -> navigationController.documentStart(false));
        handlers.put(EditorCommand.DOCUMENT_END, () -> navigationController.documentEnd(false));
        handlers.put(EditorCommand.SELECT_TO_DOCUMENT_START, () -> navigationController.documentStart(true));
        handlers.put(EditorCommand.SELECT_TO_DOCUMENT_END, () -> navigationController.documentEnd(true));

        // Line operations
        handlers.put(EditorCommand.DELETE_LINE, navigationController::deleteLine);
        handlers.put(EditorCommand.MOVE_LINE_UP, navigationController::moveLineUp);
        handlers.put(EditorCommand.MOVE_LINE_DOWN, navigationController::moveLineDown);
        handlers.put(EditorCommand.DUPLICATE_LINE_UP, navigationController::duplicateLineUp);
        handlers.put(EditorCommand.DUPLICATE_LINE_DOWN, navigationController::duplicateLineDown);
        handlers.put(EditorCommand.JOIN_LINES, navigationController::joinLines);

        // Multi-caret
        handlers.put(EditorCommand.SELECT_NEXT_OCCURRENCE, navigationController::selectNextOccurrence);
        handlers.put(EditorCommand.SELECT_ALL_OCCURRENCES, navigationController::selectAllOccurrences);
        handlers.put(EditorCommand.ADD_CURSOR_UP, navigationController::addCursorUp);
        handlers.put(EditorCommand.ADD_CURSOR_DOWN, navigationController::addCursorDown);
        handlers.put(EditorCommand.UNDO_LAST_OCCURRENCE, navigationController::undoLastOccurrence);

        // Folding
        handlers.put(EditorCommand.TOGGLE_FOLD, toggleFoldAction);
        handlers.put(EditorCommand.FOLD_ALL, foldAllAction);
        handlers.put(EditorCommand.UNFOLD_ALL, unfoldAllAction);
        handlers.put(EditorCommand.FOLD_RECURSIVE, foldRecursiveAction);
        handlers.put(EditorCommand.UNFOLD_RECURSIVE, unfoldRecursiveAction);

        register(executor, handlers);
    }

    boolean isVerticalCaretCommand(EditorCommand command) {
        return switch (command) {
            case MOVE_UP, MOVE_DOWN, SELECT_UP, SELECT_DOWN,
                MOVE_PAGE_UP, MOVE_PAGE_DOWN, SELECT_PAGE_UP, SELECT_PAGE_DOWN -> true;
            default -> false;
        };
    }
}
