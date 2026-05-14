package org.metalib.papifly.fx.code.api;

import org.metalib.papifly.fx.code.command.EditorCommand;
import org.metalib.papifly.fx.code.command.MultiCaretModel;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Dispatches editor commands and centralizes pre-dispatch command policies.
 */
final class EditorCommandExecutor {

    private final MultiCaretModel multiCaretModel;
    private final Runnable clearPreferredVerticalColumn;
    private final Predicate<EditorCommand> isVerticalCaretCommand;
    private final Supplier<Boolean> disposedSupplier;
    private final Map<EditorCommand, Runnable> handlers = new EnumMap<>(EditorCommand.class);

    EditorCommandExecutor(
        MultiCaretModel multiCaretModel,
        Runnable clearPreferredVerticalColumn,
        Predicate<EditorCommand> isVerticalCaretCommand,
        Supplier<Boolean> disposedSupplier
    ) {
        this.multiCaretModel = Objects.requireNonNull(multiCaretModel, "multiCaretModel");
        this.clearPreferredVerticalColumn = Objects.requireNonNull(clearPreferredVerticalColumn,
            "clearPreferredVerticalColumn");
        this.isVerticalCaretCommand = Objects.requireNonNull(isVerticalCaretCommand, "isVerticalCaretCommand");
        this.disposedSupplier = Objects.requireNonNull(disposedSupplier, "disposedSupplier");
    }

    void register(EditorCommand command, Runnable handler) {
        handlers.put(Objects.requireNonNull(command, "command"), Objects.requireNonNull(handler, "handler"));
    }

    void execute(EditorCommand command) {
        if (disposedSupplier.get()) {
            return;
        }
        if (!isVerticalCaretCommand.test(command)) {
            clearPreferredVerticalColumn.run();
        }

        // Commands that collapse multi-caret back to single caret
        boolean collapsesMultiCaret = switch (command) {
            case SELECT_NEXT_OCCURRENCE, SELECT_ALL_OCCURRENCES,
                 ADD_CURSOR_UP, ADD_CURSOR_DOWN, UNDO_LAST_OCCURRENCE -> false;
            case BACKSPACE, DELETE, ENTER, CUT, PASTE,
                 SCROLL_PAGE_UP, SCROLL_PAGE_DOWN -> false;
            default -> true;
        };
        if (collapsesMultiCaret && multiCaretModel.hasMultipleCarets()) {
            multiCaretModel.clearSecondaryCarets();
        }

        Runnable handler = handlers.get(command);
        if (handler != null) {
            handler.run();
        }
    }
}
