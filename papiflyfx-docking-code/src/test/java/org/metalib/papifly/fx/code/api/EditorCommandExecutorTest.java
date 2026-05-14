package org.metalib.papifly.fx.code.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.command.CaretRange;
import org.metalib.papifly.fx.code.command.EditorCommand;
import org.metalib.papifly.fx.code.command.MultiCaretModel;
import org.metalib.papifly.fx.code.render.SelectionModel;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorCommandExecutorTest {

    private MultiCaretModel multiCaretModel;
    private AtomicInteger clearPreferredCalls;
    private AtomicBoolean disposed;
    private EditorCommandExecutor executor;

    @BeforeEach
    void setUp() {
        SelectionModel selectionModel = new SelectionModel();
        this.multiCaretModel = new MultiCaretModel(selectionModel);
        this.clearPreferredCalls = new AtomicInteger();
        this.disposed = new AtomicBoolean(false);
        this.executor = new EditorCommandExecutor(
            multiCaretModel,
            () -> clearPreferredCalls.incrementAndGet(),
            this::isVerticalCaretCommand,
            disposed::get
        );
    }

    @Test
    void executeInvokesRegisteredHandler() {
        AtomicBoolean executed = new AtomicBoolean(false);
        executor.register(EditorCommand.MOVE_LEFT, () -> executed.set(true));

        executor.execute(EditorCommand.MOVE_LEFT);

        assertTrue(executed.get());
    }

    @Test
    void nonVerticalCommandClearsPreferredColumn() {
        executor.register(EditorCommand.MOVE_LEFT, () -> {
        });

        executor.execute(EditorCommand.MOVE_LEFT);

        assertEquals(1, clearPreferredCalls.get());
    }

    @Test
    void verticalCommandKeepsPreferredColumn() {
        executor.register(EditorCommand.MOVE_UP, () -> {
        });

        executor.execute(EditorCommand.MOVE_UP);

        assertEquals(0, clearPreferredCalls.get());
    }

    @Test
    void collapsesMultiCaretForDefaultCommands() {
        multiCaretModel.addCaretNoStack(new CaretRange(0, 1, 0, 1));
        assertTrue(multiCaretModel.hasMultipleCarets());

        executor.execute(EditorCommand.MOVE_LEFT);

        assertFalse(multiCaretModel.hasMultipleCarets());
    }

    @Test
    void keepsMultiCaretForExplicitMultiCaretCommands() {
        multiCaretModel.addCaretNoStack(new CaretRange(0, 1, 0, 1));
        assertTrue(multiCaretModel.hasMultipleCarets());

        executor.execute(EditorCommand.SELECT_NEXT_OCCURRENCE);

        assertTrue(multiCaretModel.hasMultipleCarets());
    }

    @Test
    void disposedEditorSkipsExecution() {
        AtomicBoolean executed = new AtomicBoolean(false);
        executor.register(EditorCommand.MOVE_LEFT, () -> executed.set(true));
        disposed.set(true);

        executor.execute(EditorCommand.MOVE_LEFT);

        assertFalse(executed.get());
        assertEquals(0, clearPreferredCalls.get());
    }

    private boolean isVerticalCaretCommand(EditorCommand command) {
        return switch (command) {
            case MOVE_UP, MOVE_DOWN, SELECT_UP, SELECT_DOWN,
                 MOVE_PAGE_UP, MOVE_PAGE_DOWN, SELECT_PAGE_UP, SELECT_PAGE_DOWN -> true;
            default -> false;
        };
    }
}
