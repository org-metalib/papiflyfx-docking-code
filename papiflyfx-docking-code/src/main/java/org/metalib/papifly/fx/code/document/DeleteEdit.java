package org.metalib.papifly.fx.code.document;

/**
 * Delete text command.
 */
public final class DeleteEdit implements EditCommand {

    private final int startOffset;
    private final int endOffset;
    private String deletedText;

    /**
     * Creates delete command for range [startOffset, endOffset).
     *
     * @param startOffset inclusive start offset
     * @param endOffset exclusive end offset
     */
    public DeleteEdit(int startOffset, int endOffset) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    @Override
    public void apply(TextSource textSource) {
        if (deletedText == null) {
            deletedText = textSource.delete(startOffset, endOffset);
            return;
        }
        textSource.delete(startOffset, startOffset + deletedText.length());
    }

    @Override
    public void undo(TextSource textSource) {
        if (deletedText == null || deletedText.isEmpty()) {
            return;
        }
        textSource.insert(startOffset, deletedText);
    }

    @Override
    public boolean applyLineIndex(LineIndex lineIndex) {
        int length = deletedLength();
        if (length == 0) {
            return true;
        }
        lineIndex.applyDelete(startOffset, startOffset + length);
        return true;
    }

    @Override
    public boolean undoLineIndex(LineIndex lineIndex) {
        if (deletedText == null || deletedText.isEmpty()) {
            return true;
        }
        lineIndex.applyInsert(startOffset, deletedText);
        return true;
    }

    private int deletedLength() {
        if (deletedText != null) {
            return deletedText.length();
        }
        return Math.max(0, endOffset - startOffset);
    }
}
