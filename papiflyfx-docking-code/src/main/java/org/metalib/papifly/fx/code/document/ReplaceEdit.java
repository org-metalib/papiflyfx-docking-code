package org.metalib.papifly.fx.code.document;

import java.util.Objects;

/**
 * Replace text command.
 */
public final class ReplaceEdit implements EditCommand {

    private final int startOffset;
    private final int endOffset;
    private final String replacement;
    private String originalText;

    /**
     * Creates replace command for range [startOffset, endOffset).
     *
     * @param startOffset inclusive start offset
     * @param endOffset exclusive end offset
     * @param replacement replacement text
     */
    public ReplaceEdit(int startOffset, int endOffset, String replacement) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.replacement = Objects.requireNonNullElse(replacement, "");
    }

    @Override
    public void apply(TextSource textSource) {
        if (originalText == null) {
            originalText = textSource.replace(startOffset, endOffset, replacement);
            return;
        }
        textSource.replace(startOffset, startOffset + originalText.length(), replacement);
    }

    @Override
    public void undo(TextSource textSource) {
        if (originalText == null) {
            return;
        }
        textSource.replace(startOffset, startOffset + replacement.length(), originalText);
    }

    @Override
    public boolean applyLineIndex(LineIndex lineIndex) {
        int oldLength = originalLength();
        int newLength = replacement.length();
        if (oldLength > 0) {
            lineIndex.applyDelete(startOffset, startOffset + oldLength);
        }
        if (newLength > 0) {
            lineIndex.applyInsert(startOffset, replacement);
        }
        return true;
    }

    @Override
    public boolean undoLineIndex(LineIndex lineIndex) {
        if (originalText == null) {
            return false;
        }
        int newLength = replacement.length();
        if (newLength > 0) {
            lineIndex.applyDelete(startOffset, startOffset + newLength);
        }
        if (!originalText.isEmpty()) {
            lineIndex.applyInsert(startOffset, originalText);
        }
        return true;
    }

    private int originalLength() {
        if (originalText != null) {
            return originalText.length();
        }
        return Math.max(0, endOffset - startOffset);
    }
}
