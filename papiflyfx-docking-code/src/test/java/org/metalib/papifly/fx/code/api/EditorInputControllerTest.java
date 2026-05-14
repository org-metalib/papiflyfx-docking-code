package org.metalib.papifly.fx.code.api;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.command.EditorCommand;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorInputControllerTest {

    private AtomicBoolean disposed;
    private AtomicBoolean searchOpen;
    private AtomicBoolean goToLineOpen;
    private AtomicBoolean overlayFocused;
    private AtomicBoolean searchClosed;
    private AtomicBoolean goToLineClosed;
    private AtomicBoolean focusRequested;
    private AtomicBoolean blinkReset;
    private AtomicReference<EditorCommand> executedCommand;
    private AtomicReference<String> typedCharacter;
    private EditorInputController controller;

    @BeforeEach
    void setUp() {
        disposed = new AtomicBoolean(false);
        searchOpen = new AtomicBoolean(false);
        goToLineOpen = new AtomicBoolean(false);
        overlayFocused = new AtomicBoolean(false);
        searchClosed = new AtomicBoolean(false);
        goToLineClosed = new AtomicBoolean(false);
        focusRequested = new AtomicBoolean(false);
        blinkReset = new AtomicBoolean(false);
        executedCommand = new AtomicReference<>();
        typedCharacter = new AtomicReference<>();
        controller = new EditorInputController(
            disposed::get,
            searchOpen::get,
            () -> searchClosed.set(true),
            goToLineOpen::get,
            () -> goToLineClosed.set(true),
            overlayFocused::get,
            () -> focusRequested.set(true),
            () -> blinkReset.set(true),
            executedCommand::set,
            typedCharacter::set
        );
    }

    @Test
    void typedCharacterIsRoutedWhenAllowed() {
        KeyEvent event = typed("a", false, false);

        boolean handled = controller.handleKeyTyped(event);

        assertTrue(handled);
        assertEquals("a", typedCharacter.get());
        assertTrue(blinkReset.get());
        assertTrue(event.isConsumed());
    }

    @Test
    void typedCharacterWithControlModifierIsIgnored() {
        KeyEvent event = typed("a", true, false);

        boolean handled = controller.handleKeyTyped(event);

        assertFalse(handled);
        assertEquals(null, typedCharacter.get());
        assertFalse(event.isConsumed());
    }

    @Test
    void escapeClosesSearchOverlay() {
        searchOpen.set(true);
        KeyEvent event = pressed(KeyCode.ESCAPE, false, false, false, false);

        boolean handled = controller.handleKeyPressed(event);

        assertTrue(handled);
        assertTrue(searchClosed.get());
        assertTrue(focusRequested.get());
        assertTrue(event.isConsumed());
    }

    @Test
    void alwaysOnCommandExecutesWhenOverlayFocused() {
        overlayFocused.set(true);
        KeyEvent event = pressed(KeyCode.F, false, true, false, false); // Ctrl+F

        boolean handled = controller.handleKeyPressed(event);

        assertTrue(handled);
        assertEquals(EditorCommand.OPEN_SEARCH, executedCommand.get());
        assertTrue(event.isConsumed());
    }

    @Test
    void editingCommandIsBlockedWhenOverlayFocused() {
        overlayFocused.set(true);
        KeyEvent event = pressed(KeyCode.LEFT, false, false, false, false);

        boolean handled = controller.handleKeyPressed(event);

        assertFalse(handled);
        assertEquals(null, executedCommand.get());
        assertFalse(event.isConsumed());
    }

    @Test
    void editingCommandExecutesWhenOverlayNotFocused() {
        KeyEvent event = pressed(KeyCode.LEFT, false, false, false, false);

        boolean handled = controller.handleKeyPressed(event);

        assertTrue(handled);
        assertEquals(EditorCommand.MOVE_LEFT, executedCommand.get());
        assertTrue(blinkReset.get());
        assertTrue(event.isConsumed());
    }

    private static KeyEvent typed(String text, boolean control, boolean meta) {
        return new KeyEvent(
            KeyEvent.KEY_TYPED,
            text,
            "",
            KeyCode.UNDEFINED,
            false,
            control,
            false,
            meta
        );
    }

    private static KeyEvent pressed(
        KeyCode code,
        boolean shift,
        boolean control,
        boolean alt,
        boolean meta
    ) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, shift, control, alt, meta);
    }
}
