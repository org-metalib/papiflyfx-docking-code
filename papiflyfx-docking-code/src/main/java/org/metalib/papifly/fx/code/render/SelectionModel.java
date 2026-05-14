package org.metalib.papifly.fx.code.render;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.metalib.papifly.fx.code.document.Document;

/**
 * Tracks caret position and text selection within a document.
 */
public class SelectionModel {

    private final IntegerProperty caretLine = new SimpleIntegerProperty(this, "caretLine", 0);
    private final IntegerProperty caretColumn = new SimpleIntegerProperty(this, "caretColumn", 0);
    private final IntegerProperty anchorLine = new SimpleIntegerProperty(this, "anchorLine", 0);
    private final IntegerProperty anchorColumn = new SimpleIntegerProperty(this, "anchorColumn", 0);

    /**
     * Creates a selection model with caret and anchor at origin.
     */
    public SelectionModel() {
        // Default constructor.
    }

    /**
     * Returns the caret line (zero-based).
     *
     * @return zero-based caret line
     */
    public int getCaretLine() {
        return caretLine.get();
    }

    /**
     * Returns the caret column (zero-based).
     *
     * @return zero-based caret column
     */
    public int getCaretColumn() {
        return caretColumn.get();
    }

    /**
     * Returns the read-only caret-line property.
     *
     * @return read-only caret-line property
     */
    public ReadOnlyIntegerProperty caretLineProperty() {
        return caretLine;
    }

    /**
     * Returns the read-only caret-column property.
     *
     * @return read-only caret-column property
     */
    public ReadOnlyIntegerProperty caretColumnProperty() {
        return caretColumn;
    }

    /**
     * Returns the read-only anchor line property.
     *
     * @return read-only anchor-line property
     */
    public ReadOnlyIntegerProperty anchorLineProperty() {
        return anchorLine;
    }

    /**
     * Returns the read-only anchor column property.
     *
     * @return read-only anchor-column property
     */
    public ReadOnlyIntegerProperty anchorColumnProperty() {
        return anchorColumn;
    }

    /**
     * Returns the selection anchor line.
     *
     * @return zero-based anchor line
     */
    public int getAnchorLine() {
        return anchorLine.get();
    }

    /**
     * Returns the selection anchor column.
     *
     * @return zero-based anchor column
     */
    public int getAnchorColumn() {
        return anchorColumn.get();
    }

    /**
     * Returns true if there is a non-empty selection.
     *
     * @return {@code true} when anchor and caret positions differ
     */
    public boolean hasSelection() {
        return caretLine.get() != anchorLine.get() || caretColumn.get() != anchorColumn.get();
    }

    /**
     * Moves the caret to the specified position and clears the selection.
     *
     * @param line target zero-based line index
     * @param column target zero-based column index
     */
    public void moveCaret(int line, int column) {
        caretLine.set(line);
        caretColumn.set(column);
        anchorLine.set(line);
        anchorColumn.set(column);
    }

    /**
     * Moves the caret while keeping the anchor fixed, extending the selection.
     *
     * @param line target zero-based line index
     * @param column target zero-based column index
     */
    public void moveCaretWithSelection(int line, int column) {
        caretLine.set(line);
        caretColumn.set(column);
    }

    /**
     * Clears the selection by moving the anchor to the caret.
     */
    public void clearSelection() {
        anchorLine.set(caretLine.get());
        anchorColumn.set(caretColumn.get());
    }

    /**
     * Selects all text in the document.
     *
     * @param document source document
     */
    public void selectAll(Document document) {
        anchorLine.set(0);
        anchorColumn.set(0);
        int lastLine = document.getLineCount() - 1;
        int lastCol = document.getLineText(lastLine).length();
        caretLine.set(lastLine);
        caretColumn.set(lastCol);
    }

    /**
     * Returns the selected text from the document, or empty string if no selection.
     *
     * @param document source document
     * @return selected text, or empty string when no selection exists
     */
    public String getSelectedText(Document document) {
        if (!hasSelection()) {
            return "";
        }
        int startOffset = getSelectionStartOffset(document);
        int endOffset = getSelectionEndOffset(document);
        return document.getSubstring(startOffset, endOffset);
    }

    /**
     * Returns the document offset for the start of the selection (lower bound).
     *
     * @param document source document
     * @return selection start offset
     */
    public int getSelectionStartOffset(Document document) {
        return document.toOffset(getSelectionStartLine(), getSelectionStartColumn());
    }

    /**
     * Returns the document offset for the end of the selection (upper bound).
     *
     * @param document source document
     * @return selection end offset
     */
    public int getSelectionEndOffset(Document document) {
        return document.toOffset(getSelectionEndLine(), getSelectionEndColumn());
    }

    /**
     * Returns the line of the selection start (the earlier position).
     *
     * @return zero-based selection start line
     */
    public int getSelectionStartLine() {
        if (anchorLine.get() < caretLine.get()) {
            return anchorLine.get();
        }
        if (anchorLine.get() > caretLine.get()) {
            return caretLine.get();
        }
        return anchorColumn.get() <= caretColumn.get() ? anchorLine.get() : caretLine.get();
    }

    /**
     * Returns the column of the selection start.
     *
     * @return zero-based selection start column
     */
    public int getSelectionStartColumn() {
        if (anchorLine.get() < caretLine.get()) {
            return anchorColumn.get();
        }
        if (anchorLine.get() > caretLine.get()) {
            return caretColumn.get();
        }
        return Math.min(anchorColumn.get(), caretColumn.get());
    }

    /**
     * Returns the line of the selection end (the later position).
     *
     * @return zero-based selection end line
     */
    public int getSelectionEndLine() {
        if (anchorLine.get() > caretLine.get()) {
            return anchorLine.get();
        }
        if (anchorLine.get() < caretLine.get()) {
            return caretLine.get();
        }
        return anchorColumn.get() >= caretColumn.get() ? anchorLine.get() : caretLine.get();
    }

    /**
     * Returns the column of the selection end.
     *
     * @return zero-based selection end column
     */
    public int getSelectionEndColumn() {
        if (anchorLine.get() > caretLine.get()) {
            return anchorColumn.get();
        }
        if (anchorLine.get() < caretLine.get()) {
            return caretColumn.get();
        }
        return Math.max(anchorColumn.get(), caretColumn.get());
    }

    /**
     * Returns the document offset for the current caret position.
     *
     * @param document source document
     * @return current caret offset
     */
    public int getCaretOffset(Document document) {
        return document.toOffset(caretLine.get(), caretColumn.get());
    }
}
