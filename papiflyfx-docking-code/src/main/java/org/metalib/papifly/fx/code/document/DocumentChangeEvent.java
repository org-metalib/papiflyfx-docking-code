package org.metalib.papifly.fx.code.document;

/**
 * Event describing a document content change.
 *
 * @param offset    start offset of the change
 * @param oldLength length of text removed (0 for pure insert)
 * @param newLength length of text inserted (0 for pure delete)
 * @param type      the kind of change
 */
public record DocumentChangeEvent(
    int offset,
    int oldLength,
    int newLength,
    ChangeType type
) {

    /**
     * Types of document changes.
     */
    public enum ChangeType {
        /**
         * Text was inserted.
         */
        INSERT,
        /**
         * Text was deleted.
         */
        DELETE,
        /**
         * A range was replaced with new text.
         */
        REPLACE,
        /**
         * Change was produced by undo operation.
         */
        UNDO,
        /**
         * Change was produced by redo operation.
         */
        REDO,
        /**
         * Whole-document replacement via setText.
         */
        SET_TEXT
    }

    /**
     * Creates a SET_TEXT event (full replacement).
     */
    static DocumentChangeEvent setText(int oldLength, int newLength) {
        return new DocumentChangeEvent(0, oldLength, newLength, ChangeType.SET_TEXT);
    }

    /**
     * Creates an INSERT event.
     */
    static DocumentChangeEvent insert(int offset, int insertedLength) {
        return new DocumentChangeEvent(offset, 0, insertedLength, ChangeType.INSERT);
    }

    /**
     * Creates a DELETE event.
     */
    static DocumentChangeEvent delete(int offset, int deletedLength) {
        return new DocumentChangeEvent(offset, deletedLength, 0, ChangeType.DELETE);
    }

    /**
     * Creates a REPLACE event.
     */
    static DocumentChangeEvent replace(int offset, int oldLength, int newLength) {
        return new DocumentChangeEvent(offset, oldLength, newLength, ChangeType.REPLACE);
    }
}
