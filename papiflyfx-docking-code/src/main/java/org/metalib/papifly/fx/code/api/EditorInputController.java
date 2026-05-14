package org.metalib.papifly.fx.code.api;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.metalib.papifly.fx.code.command.EditorCommand;
import org.metalib.papifly.fx.code.command.KeymapTable;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Handles keyboard event routing and overlay-aware command gating.
 */
final class EditorInputController {

    private final BooleanSupplier disposedSupplier;
    private final BooleanSupplier searchOpenSupplier;
    private final Runnable searchCloseAction;
    private final BooleanSupplier goToLineOpenSupplier;
    private final Runnable goToLineCloseAction;
    private final BooleanSupplier overlayFocusWithinSupplier;
    private final Runnable requestFocusAction;
    private final Runnable resetCaretBlinkAction;
    private final Consumer<EditorCommand> commandExecutor;
    private final Consumer<String> typedCharacterHandler;

    EditorInputController(
        BooleanSupplier disposedSupplier,
        BooleanSupplier searchOpenSupplier,
        Runnable searchCloseAction,
        BooleanSupplier goToLineOpenSupplier,
        Runnable goToLineCloseAction,
        BooleanSupplier overlayFocusWithinSupplier,
        Runnable requestFocusAction,
        Runnable resetCaretBlinkAction,
        Consumer<EditorCommand> commandExecutor,
        Consumer<String> typedCharacterHandler
    ) {
        this.disposedSupplier = disposedSupplier;
        this.searchOpenSupplier = searchOpenSupplier;
        this.searchCloseAction = searchCloseAction;
        this.goToLineOpenSupplier = goToLineOpenSupplier;
        this.goToLineCloseAction = goToLineCloseAction;
        this.overlayFocusWithinSupplier = overlayFocusWithinSupplier;
        this.requestFocusAction = requestFocusAction;
        this.resetCaretBlinkAction = resetCaretBlinkAction;
        this.commandExecutor = commandExecutor;
        this.typedCharacterHandler = typedCharacterHandler;
    }

    boolean handleKeyTyped(KeyEvent event) {
        if (disposedSupplier.getAsBoolean() || overlayFocusWithinSupplier.getAsBoolean()) {
            return false;
        }
        String ch = event.getCharacter();
        if (ch.isEmpty() || ch.charAt(0) < 32 || ch.charAt(0) == 127) {
            return false;
        }
        if (event.isControlDown() || event.isMetaDown()) {
            return false;
        }
        resetCaretBlinkAction.run();
        typedCharacterHandler.accept(ch);
        event.consume();
        return true;
    }

    boolean handleKeyPressed(KeyEvent event) {
        if (disposedSupplier.getAsBoolean()) {
            return false;
        }

        if (event.getCode() == KeyCode.ESCAPE) {
            if (searchOpenSupplier.getAsBoolean()) {
                searchCloseAction.run();
                requestFocusAction.run();
                event.consume();
                return true;
            }
            if (goToLineOpenSupplier.getAsBoolean()) {
                goToLineCloseAction.run();
                requestFocusAction.run();
                event.consume();
                return true;
            }
        }

        Optional<EditorCommand> resolved = KeymapTable.resolve(event);
        if (resolved.isEmpty()) {
            return false;
        }
        EditorCommand command = resolved.get();

        // "Always-on" commands execute even when overlays are focused.
        if (command == EditorCommand.OPEN_SEARCH
            || command == EditorCommand.OPEN_REPLACE
            || command == EditorCommand.GO_TO_LINE) {
            commandExecutor.accept(command);
            event.consume();
            return true;
        }

        if (overlayFocusWithinSupplier.getAsBoolean()) {
            return false;
        }

        resetCaretBlinkAction.run();
        commandExecutor.accept(command);
        event.consume();
        return true;
    }
}
