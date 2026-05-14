package org.metalib.papifly.fx.code.command;

import javafx.scene.input.KeyCode;

/**
 * Immutable key combination used as a lookup key in the keymap table.
 *
 * @param code key code of the binding
 * @param shift {@code true} when shift must be pressed
 * @param shortcut {@code true} when platform shortcut modifier must be pressed
 * @param alt {@code true} when alt must be pressed
 */
public record KeyBinding(KeyCode code, boolean shift, boolean shortcut, boolean alt) {
}
