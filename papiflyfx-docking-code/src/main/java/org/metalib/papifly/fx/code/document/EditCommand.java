package org.metalib.papifly.fx.code.document;

/**
 * Internal command contract for document undo/redo operations.
 */
interface EditCommand {

    /**
     * Applies the command.
     */
    void apply(TextSource textSource);

    /**
     * Reverts the command.
     */
    void undo(TextSource textSource);

    /**
     * Applies incremental line-index changes for this command.
     * Returns {@code true} when handled incrementally, {@code false} when
     * caller should fall back to full index rebuild.
     */
    default boolean applyLineIndex(LineIndex lineIndex) {
        return false;
    }

    /**
     * Applies incremental line-index changes for undo of this command.
     * Returns {@code true} when handled incrementally, {@code false} when
     * caller should fall back to full index rebuild.
     */
    default boolean undoLineIndex(LineIndex lineIndex) {
        return false;
    }
}
