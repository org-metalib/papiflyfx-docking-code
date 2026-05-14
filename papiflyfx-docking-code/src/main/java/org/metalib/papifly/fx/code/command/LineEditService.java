package org.metalib.papifly.fx.code.command;

import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.render.SelectionModel;

/**
 * Shared line-level edit operations used by editor commands.
 */
public class LineEditService {

    /**
     * Creates a line-edit service.
     */
    public LineEditService() {
        // Default constructor for editor composition.
    }

    /**
     * Resolves selected line block or current caret line block.
     *
     * @param document source document
     * @param selectionModel source selection state
     * @return resolved line block for selection or caret
     */
    public LineBlock resolveSelectionOrCaretBlock(Document document, SelectionModel selectionModel) {
        return LineBlock.fromSelectionOrCaret(document, selectionModel);
    }

    /**
     * Deletes the provided block.
     *
     * @param document source document
     * @param block line block to delete
     * @return {@code true} when deletion was applied
     */
    public boolean deleteBlock(Document document, LineBlock block) {
        int startOffset = block.startOffset();
        int endOffset = block.endOffset();
        if (block.endLine() >= document.getLineCount() - 1) {
            endOffset = document.length();
            if (block.startLine() > 0) {
                startOffset = document.getLineStartOffset(block.startLine()) - 1;
            }
        }
        if (startOffset >= endOffset) {
            return false;
        }
        document.delete(startOffset, endOffset);
        return true;
    }

    /**
     * Moves the provided block one line up.
     *
     * @param document source document
     * @param block line block to move
     * @return {@code true} when move was applied
     */
    public boolean moveBlockUp(Document document, LineBlock block) {
        if (block.startLine() <= 0) {
            return false;
        }
        LineBlock previousLine = LineBlock.fromLines(document, block.startLine() - 1, block.startLine() - 1);
        String combined = ensureTrailingNewline(block.text()) + ensureTrailingNewline(previousLine.text());
        if (block.reachesDocumentEnd() && !document.endsWithNewline()) {
            combined = stripSingleTrailingNewline(combined);
        }
        document.replace(previousLine.startOffset(), block.endOffset(), combined);
        return true;
    }

    /**
     * Moves the provided block one line down.
     *
     * @param document source document
     * @param block line block to move
     * @return {@code true} when move was applied
     */
    public boolean moveBlockDown(Document document, LineBlock block) {
        if (block.endLine() >= document.getLineCount() - 1) {
            return false;
        }
        LineBlock nextLine = LineBlock.fromLines(document, block.endLine() + 1, block.endLine() + 1);
        String combined = ensureTrailingNewline(nextLine.text()) + ensureTrailingNewline(block.text());
        if (nextLine.reachesDocumentEnd() && !document.endsWithNewline()) {
            combined = stripSingleTrailingNewline(combined);
        }
        document.replace(block.startOffset(), nextLine.endOffset(), combined);
        return true;
    }

    /**
     * Duplicates the block above itself.
     *
     * @param document source document
     * @param block line block to duplicate
     */
    public void duplicateBlockUp(Document document, LineBlock block) {
        document.insert(block.startOffset(), ensureTrailingNewline(block.text()));
    }

    /**
     * Duplicates the block below itself.
     *
     * @param document source document
     * @param block line block to duplicate
     */
    public void duplicateBlockDown(Document document, LineBlock block) {
        if (block.reachesDocumentEnd() && !block.text().endsWith("\n")) {
            document.insert(block.endOffset(), "\n" + block.text());
            return;
        }
        document.insert(block.endOffset(), ensureTrailingNewline(block.text()));
    }

    /**
     * Joins the provided line with the next line using a single space.
     *
     * @param document source document
     * @param line zero-based line index to join with its successor
     * @return {@code true} when join was applied
     */
    public boolean joinLineWithNext(Document document, int line) {
        if (line >= document.getLineCount() - 1) {
            return false;
        }
        int lineEnd = document.getLineStartOffset(line) + document.getLineText(line).length();
        document.replace(lineEnd, lineEnd + 1, " ");
        return true;
    }

    private static String ensureTrailingNewline(String value) {
        if (value.endsWith("\n")) {
            return value;
        }
        return value + '\n';
    }

    private static String stripSingleTrailingNewline(String value) {
        if (value.endsWith("\n")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
