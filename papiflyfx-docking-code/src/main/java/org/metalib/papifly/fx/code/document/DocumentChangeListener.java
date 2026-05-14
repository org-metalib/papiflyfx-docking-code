package org.metalib.papifly.fx.code.document;

/**
 * Listener for document content changes.
 * Notified after every mutation (insert, delete, replace, undo, redo, setText).
 */
@FunctionalInterface
public interface DocumentChangeListener {

    /**
     * Called after the document content has changed.
     *
     * @param event details about the change
     */
    void documentChanged(DocumentChangeEvent event);
}
