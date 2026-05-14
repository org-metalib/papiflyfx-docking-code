package org.metalib.papifly.fx.code.document;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Editable document model with line index, undo/redo support, and change notification.
 */
public class Document {

    private static final System.Logger LOGGER = System.getLogger(Document.class.getName());

    private final TextSource textSource;
    private final LineIndex lineIndex;
    private final Deque<EditCommand> undoStack = new ArrayDeque<>();
    private final Deque<EditCommand> redoStack = new ArrayDeque<>();
    private final List<DocumentChangeListener> listeners = new CopyOnWriteArrayList<>();
    private List<EditCommand> compoundBuffer;

    /**
     * Creates an empty document.
     */
    public Document() {
        this("");
    }

    /**
     * Creates a document initialized with text.
     *
     * @param initialText initial document text, normalized by {@link TextSource}
     */
    public Document(String initialText) {
        this.textSource = new TextSource(initialText);
        this.lineIndex = new LineIndex(textSource.getText());
    }

    /**
     * Returns a snapshot of lines.
     *
     * @return immutable snapshot of current document lines
     */
    public List<String> getLinesSnapshot() {
        int count = getLineCount();
        List<String> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lines.add(getLineText(i));
        }
        return lines;
    }

    /**
     * Adds a change listener.
     *
     * @param listener listener to register
     */
    public void addChangeListener(DocumentChangeListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /**
     * Removes a change listener.
     *
     * @param listener listener to remove
     */
    public void removeChangeListener(DocumentChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Returns full document text.
     *
     * @return full document text
     */
    public String getText() {
        return textSource.getText();
    }

    /**
     * Returns true when the document ends with a newline.
     *
     * @return {@code true} when document text is non-empty and ends with '\n'
     */
    public boolean endsWithNewline() {
        int length = textSource.length();
        if (length == 0) {
            return false;
        }
        return textSource.substring(length - 1, length).charAt(0) == '\n';
    }

    /**
     * Returns substring in the range [startOffset, endOffset).
     *
     * @param startOffset inclusive start offset
     * @param endOffset exclusive end offset
     * @return substring in the provided range
     */
    public String getSubstring(int startOffset, int endOffset) {
        return textSource.substring(startOffset, endOffset);
    }

    /**
     * Sets full document text and clears history.
     *
     * @param text replacement text for the entire document
     */
    public void setText(String text) {
        int oldLength = textSource.length();
        textSource.setText(text);
        rebuildIndex();
        clearHistory();
        fireChange(DocumentChangeEvent.setText(oldLength, textSource.length()));
    }

    /**
     * Returns text length.
     *
     * @return current document character count
     */
    public int length() {
        return textSource.length();
    }

    /**
     * Returns line count.
     *
     * @return number of logical lines in the document
     */
    public int getLineCount() {
        return lineIndex.getLineCount();
    }

    /**
     * Returns line start offset.
     *
     * @param line zero-based line index
     * @return offset of the first character in the line
     */
    public int getLineStartOffset(int line) {
        return lineIndex.getLineStartOffset(line);
    }

    /**
     * Returns line end offset (exclusive, without trailing newline).
     *
     * @param line zero-based line index
     * @return exclusive end offset of line text
     */
    public int getLineEndOffset(int line) {
        return lineIndex.getLineEndOffset(line, length());
    }

    /**
     * Returns line text without trailing newline.
     *
     * @param line zero-based line index
     * @return line text without trailing newline
     */
    public String getLineText(int line) {
        int start = getLineStartOffset(line);
        int end = getLineEndOffset(line);
        return textSource.substring(start, end);
    }

    /**
     * Returns line index for offset.
     *
     * @param offset document offset
     * @return zero-based line index containing the offset
     */
    public int getLineForOffset(int offset) {
        return lineIndex.getLineForOffset(offset, length());
    }

    /**
     * Returns column for offset.
     *
     * @param offset document offset
     * @return zero-based column index for the offset
     */
    public int getColumnForOffset(int offset) {
        return lineIndex.getColumnForOffset(offset, length());
    }

    /**
     * Returns offset for line and column.
     *
     * @param line zero-based line index
     * @param column zero-based column index
     * @return clamped document offset
     */
    public int toOffset(int line, int column) {
        return lineIndex.toOffset(line, column, length());
    }

    /**
     * Inserts text at offset and records undo.
     *
     * @param offset insertion offset
     * @param text text to insert
     */
    public void insert(int offset, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String normalized = TextSource.normalizeLineEndings(text);
        EditCommand command = new InsertEdit(offset, normalized);
        command.apply(textSource);
        lineIndex.applyInsert(offset, normalized);
        recordEdit(command);
        fireChange(DocumentChangeEvent.insert(offset, normalized.length()));
    }

    /**
     * Deletes range [start, end) and records undo.
     *
     * @param startOffset inclusive start offset
     * @param endOffset exclusive end offset
     */
    public void delete(int startOffset, int endOffset) {
        if (startOffset == endOffset) {
            return;
        }
        EditCommand command = new DeleteEdit(startOffset, endOffset);
        command.apply(textSource);
        lineIndex.applyDelete(startOffset, endOffset);
        recordEdit(command);
        fireChange(DocumentChangeEvent.delete(startOffset, endOffset - startOffset));
    }

    /**
     * Replaces range [start, end) with replacement and records undo.
     *
     * @param startOffset inclusive start offset
     * @param endOffset exclusive end offset
     * @param replacement replacement text, {@code null} treated as empty text
     */
    public void replace(int startOffset, int endOffset, String replacement) {
        String safeReplacement = replacement == null ? "" : replacement;
        if (startOffset == endOffset) {
            insert(startOffset, safeReplacement);
            return;
        }
        String normalized = TextSource.normalizeLineEndings(safeReplacement);
        EditCommand command = new ReplaceEdit(startOffset, endOffset, normalized);
        command.apply(textSource);
        lineIndex.applyDelete(startOffset, endOffset);
        if (!normalized.isEmpty()) {
            lineIndex.applyInsert(startOffset, normalized);
        }
        recordEdit(command);
        fireChange(DocumentChangeEvent.replace(startOffset, endOffset - startOffset, normalized.length()));
    }

    /**
     * Returns true when undo is available.
     *
     * @return {@code true} when there is at least one command to undo
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Returns true when redo is available.
     *
     * @return {@code true} when there is at least one command to redo
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Undoes the last edit.
     *
     * @return {@code true} when an edit was undone
     */
    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        int lengthBefore = textSource.length();
        EditCommand command = undoStack.pop();
        command.undo(textSource);
        applyUndoIndexOrRebuild(command);
        redoStack.push(command);
        fireChange(new DocumentChangeEvent(0, lengthBefore, textSource.length(),
            DocumentChangeEvent.ChangeType.UNDO));
        return true;
    }

    /**
     * Redoes the last undone edit.
     *
     * @return {@code true} when an edit was redone
     */
    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }
        int lengthBefore = textSource.length();
        EditCommand command = redoStack.pop();
        command.apply(textSource);
        applyRedoIndexOrRebuild(command);
        undoStack.push(command);
        fireChange(new DocumentChangeEvent(0, lengthBefore, textSource.length(),
            DocumentChangeEvent.ChangeType.REDO));
        return true;
    }

    /**
     * Begins accumulating edits into a compound group.
     * <p>
     * While a compound edit is active, individual edits are applied
     * immediately but not pushed onto the undo stack. Call
     * {@link #endCompoundEdit()} to wrap them into a single undo entry.
     */
    public void beginCompoundEdit() {
        compoundBuffer = new ArrayList<>();
    }

    /**
     * Ends the current compound edit session and pushes the accumulated
     * edits as a single {@link CompoundEdit} onto the undo stack.
     */
    public void endCompoundEdit() {
        if (compoundBuffer != null && !compoundBuffer.isEmpty()) {
            undoStack.push(new CompoundEdit(List.copyOf(compoundBuffer)));
            redoStack.clear();
        }
        compoundBuffer = null;
    }

    /**
     * Returns {@code true} if a compound edit session is currently active.
     *
     * @return {@code true} when edits are currently being buffered
     */
    public boolean isCompoundEditActive() {
        return compoundBuffer != null;
    }

    /**
     * Clears undo/redo history.
     */
    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
    }

    private void recordEdit(EditCommand command) {
        if (compoundBuffer != null) {
            compoundBuffer.add(command);
        } else {
            undoStack.push(command);
            redoStack.clear();
        }
    }

    private void rebuildIndex() {
        lineIndex.rebuild(textSource.getText());
    }

    private void applyUndoIndexOrRebuild(EditCommand command) {
        if (!command.undoLineIndex(lineIndex)) {
            rebuildIndex();
        }
    }

    private void applyRedoIndexOrRebuild(EditCommand command) {
        if (!command.applyLineIndex(lineIndex)) {
            rebuildIndex();
        }
    }

    private void fireChange(DocumentChangeEvent event) {
        for (DocumentChangeListener listener : listeners) {
            try {
                listener.documentChanged(event);
            } catch (RuntimeException exception) {
                LOGGER.log(System.Logger.Level.WARNING, "Document change listener failed", exception);
            }
        }
    }
}
