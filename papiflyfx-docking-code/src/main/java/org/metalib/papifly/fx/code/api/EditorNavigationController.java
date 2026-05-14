package org.metalib.papifly.fx.code.api;

import org.metalib.papifly.fx.code.command.CaretRange;
import org.metalib.papifly.fx.code.command.LineBlock;
import org.metalib.papifly.fx.code.command.LineEditService;
import org.metalib.papifly.fx.code.command.MultiCaretModel;
import org.metalib.papifly.fx.code.command.WordBoundary;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.render.SelectionModel;
import org.metalib.papifly.fx.code.render.Viewport;
import org.metalib.papifly.fx.code.render.WrapMap;

import java.util.Objects;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * Handles caret navigation and line/multi-caret command orchestration.
 */
final class EditorNavigationController {

    private enum WordDirection {
        LEFT,
        RIGHT
    }

    private final Document document;
    private final SelectionModel selectionModel;
    private final MultiCaretModel multiCaretModel;
    private final LineEditService lineEditService;
    private final OccurrenceSelectionService occurrenceSelectionService;
    private final EditorCaretCoordinator caretCoordinator;
    private final Viewport viewport;
    private final Runnable markViewportDirty;
    private final DoubleConsumer setVerticalScrollOffset;
    private final DoubleSupplier currentScrollOffsetSupplier;

    EditorNavigationController(
        Document document,
        SelectionModel selectionModel,
        MultiCaretModel multiCaretModel,
        LineEditService lineEditService,
        OccurrenceSelectionService occurrenceSelectionService,
        EditorCaretCoordinator caretCoordinator,
        Viewport viewport,
        Runnable markViewportDirty,
        DoubleConsumer setVerticalScrollOffset,
        DoubleSupplier currentScrollOffsetSupplier
    ) {
        this.document = Objects.requireNonNull(document, "document");
        this.selectionModel = Objects.requireNonNull(selectionModel, "selectionModel");
        this.multiCaretModel = Objects.requireNonNull(multiCaretModel, "multiCaretModel");
        this.lineEditService = Objects.requireNonNull(lineEditService, "lineEditService");
        this.occurrenceSelectionService = Objects.requireNonNull(occurrenceSelectionService, "occurrenceSelectionService");
        this.caretCoordinator = Objects.requireNonNull(caretCoordinator, "caretCoordinator");
        this.viewport = Objects.requireNonNull(viewport, "viewport");
        this.markViewportDirty = Objects.requireNonNull(markViewportDirty, "markViewportDirty");
        this.setVerticalScrollOffset = Objects.requireNonNull(setVerticalScrollOffset, "setVerticalScrollOffset");
        this.currentScrollOffsetSupplier = Objects.requireNonNull(currentScrollOffsetSupplier,
            "currentScrollOffsetSupplier");
    }

    void moveLeft(boolean shift) {
        int line = selectionModel.getCaretLine();
        int col = selectionModel.getCaretColumn();
        if (col > 0) {
            caretCoordinator.moveCaret(line, col - 1, shift);
        } else {
            int previousVisible = viewport.previousVisibleLogicalLine(line);
            if (previousVisible != line) {
                int prevLineLen = document.getLineText(previousVisible).length();
                caretCoordinator.moveCaret(previousVisible, prevLineLen, shift);
            }
        }
    }

    void moveRight(boolean shift) {
        int line = selectionModel.getCaretLine();
        int col = selectionModel.getCaretColumn();
        int lineLen = document.getLineText(line).length();
        if (col < lineLen) {
            caretCoordinator.moveCaret(line, col + 1, shift);
        } else {
            int nextVisible = viewport.nextVisibleLogicalLine(line);
            if (nextVisible != line) {
                caretCoordinator.moveCaret(nextVisible, 0, shift);
            }
        }
    }

    void moveUp(boolean shift) {
        int line = selectionModel.getCaretLine();
        int targetLine = viewport.previousVisibleLogicalLine(line);
        if (targetLine != line) {
            caretCoordinator.moveCaretVertically(targetLine, shift);
        }
    }

    void moveDown(boolean shift) {
        int line = selectionModel.getCaretLine();
        int targetLine = viewport.nextVisibleLogicalLine(line);
        if (targetLine != line) {
            caretCoordinator.moveCaretVertically(targetLine, shift);
        }
    }

    void pageUp(boolean shift) {
        handlePageMove(-1, shift);
    }

    void pageDown(boolean shift) {
        handlePageMove(1, shift);
    }

    void scrollPageUp() {
        handleScrollPage(-1);
    }

    void scrollPageDown() {
        handleScrollPage(1);
    }

    void lineStart(boolean shift) {
        caretCoordinator.moveCaret(selectionModel.getCaretLine(), 0, shift);
    }

    void lineEnd(boolean shift) {
        int line = selectionModel.getCaretLine();
        caretCoordinator.moveCaret(line, document.getLineText(line).length(), shift);
    }

    void selectAll() {
        selectionModel.selectAll(document);
        markViewportDirty.run();
    }

    void undo() {
        int beforeLength = document.length();
        int beforeOffset = selectionModel.getCaretOffset(document);
        if (document.undo()) {
            int afterLength = document.length();
            int targetOffset = Math.max(0, Math.min(beforeOffset + (afterLength - beforeLength), afterLength));
            caretCoordinator.moveCaretToOffset(targetOffset);
        }
    }

    void redo() {
        int beforeLength = document.length();
        int beforeOffset = selectionModel.getCaretOffset(document);
        if (document.redo()) {
            int afterLength = document.length();
            int targetOffset = Math.max(0, Math.min(beforeOffset + (afterLength - beforeLength), afterLength));
            caretCoordinator.moveCaretToOffset(targetOffset);
        }
    }

    void moveWordLeft() {
        moveWord(WordDirection.LEFT, false);
    }

    void moveWordRight() {
        moveWord(WordDirection.RIGHT, false);
    }

    void selectWordLeft() {
        moveWord(WordDirection.LEFT, true);
    }

    void selectWordRight() {
        moveWord(WordDirection.RIGHT, true);
    }

    void deleteWordLeft() {
        deleteWord(WordDirection.LEFT);
    }

    void deleteWordRight() {
        deleteWord(WordDirection.RIGHT);
    }

    void documentStart(boolean shift) {
        int line = viewport.totalVisibleLogicalLines() <= 0 ? 0 : viewport.visibleToLogicalLine(0);
        caretCoordinator.moveCaret(line, 0, shift);
    }

    void documentEnd(boolean shift) {
        int lastVisible = Math.max(0, viewport.totalVisibleLogicalLines() - 1);
        int lastLine = viewport.totalVisibleLogicalLines() <= 0
            ? Math.max(0, document.getLineCount() - 1)
            : viewport.visibleToLogicalLine(lastVisible);
        int lastCol = document.getLineText(lastLine).length();
        caretCoordinator.moveCaret(lastLine, lastCol, shift);
    }

    void deleteLine() {
        LineBlock block = lineEditService.resolveSelectionOrCaretBlock(document, selectionModel);
        if (lineEditService.deleteBlock(document, block)) {
            int targetLine = Math.min(block.startLine(), document.getLineCount() - 1);
            caretCoordinator.moveCaret(targetLine, 0, false);
        }
    }

    void moveLineUp() {
        LineBlock block = lineEditService.resolveSelectionOrCaretBlock(document, selectionModel);
        if (!lineEditService.moveBlockUp(document, block)) {
            return;
        }
        int col = selectionModel.getCaretColumn();
        caretCoordinator.moveCaret(selectionModel.getCaretLine() - 1, col, false);
    }

    void moveLineDown() {
        LineBlock block = lineEditService.resolveSelectionOrCaretBlock(document, selectionModel);
        if (!lineEditService.moveBlockDown(document, block)) {
            return;
        }
        int col = selectionModel.getCaretColumn();
        caretCoordinator.moveCaret(selectionModel.getCaretLine() + 1, col, false);
    }

    void duplicateLineUp() {
        LineBlock block = lineEditService.resolveSelectionOrCaretBlock(document, selectionModel);
        lineEditService.duplicateBlockUp(document, block);
    }

    void duplicateLineDown() {
        LineBlock block = lineEditService.resolveSelectionOrCaretBlock(document, selectionModel);
        lineEditService.duplicateBlockDown(document, block);
        int linesInserted = block.lineCount();
        int col = selectionModel.getCaretColumn();
        caretCoordinator.moveCaret(selectionModel.getCaretLine() + linesInserted, col, false);
    }

    void joinLines() {
        lineEditService.joinLineWithNext(document, selectionModel.getCaretLine());
    }

    void selectNextOccurrence() {
        occurrenceSelectionService.selectNextOccurrence();
    }

    void selectAllOccurrences() {
        occurrenceSelectionService.selectAllOccurrences();
    }

    void addCursorUp() {
        int caretLine = selectionModel.getCaretLine();
        int caretCol = selectionModel.getCaretColumn();
        int newLine = viewport.previousVisibleLogicalLine(caretLine);
        if (newLine == caretLine) {
            return;
        }
        int newCol = Math.min(caretCol, document.getLineText(newLine).length());
        multiCaretModel.addCaret(new CaretRange(newLine, newCol, newLine, newCol));
        markViewportDirty.run();
    }

    void addCursorDown() {
        int caretLine = selectionModel.getCaretLine();
        int caretCol = selectionModel.getCaretColumn();
        int newLine = viewport.nextVisibleLogicalLine(caretLine);
        if (newLine == caretLine) {
            return;
        }
        int newCol = Math.min(caretCol, document.getLineText(newLine).length());
        multiCaretModel.addCaret(new CaretRange(newLine, newCol, newLine, newCol));
        markViewportDirty.run();
    }

    void undoLastOccurrence() {
        multiCaretModel.undoLastOccurrence();
        markViewportDirty.run();
    }

    private void handlePageMove(int direction, boolean shift) {
        if (viewport.isWordWrap()) {
            handleWrappedPageMove(direction, shift);
            return;
        }
        int lineDelta = caretCoordinator.computePageLineDelta();
        int caretLine = selectionModel.getCaretLine();
        int currentVisible = viewport.logicalToVisibleLine(caretLine);
        if (currentVisible < 0) {
            currentVisible = 0;
        }
        int maxVisible = Math.max(0, viewport.totalVisibleLogicalLines() - 1);
        int targetVisible = Math.max(0, Math.min(currentVisible + (direction * lineDelta), maxVisible));
        int targetLine = viewport.visibleToLogicalLine(targetVisible);
        caretCoordinator.moveCaretVertically(targetLine, shift);
    }

    private void handleWrappedPageMove(int direction, boolean shift) {
        WrapMap wrapMap = viewport.getWrapMap();
        if (wrapMap == null || wrapMap.totalVisualRows() <= 0) {
            int lineDelta = caretCoordinator.computePageLineDelta();
            int caretLine = selectionModel.getCaretLine();
            int targetLine = Math.max(0, Math.min(caretLine + (direction * lineDelta), document.getLineCount() - 1));
            caretCoordinator.moveCaretVertically(targetLine, shift);
            return;
        }
        int caretLine = selectionModel.getCaretLine();
        int caretColumn = selectionModel.getCaretColumn();
        int currentVisualRow = wrapMap.lineColumnToVisualRow(caretLine, caretColumn);
        int rowDelta = caretCoordinator.computePageLineDelta();
        int targetVisualRow = Math.max(0,
            Math.min(currentVisualRow + (direction * rowDelta), wrapMap.totalVisualRows() - 1));
        WrapMap.VisualRow row = wrapMap.visualRow(targetVisualRow);
        int targetColumn = Math.max(row.startColumn(), Math.min(caretColumn, row.endColumn()));
        caretCoordinator.moveCaret(row.lineIndex(), targetColumn, shift);
    }

    private void handleScrollPage(int direction) {
        double pagePixels = caretCoordinator.computePagePixelDelta();
        double newOffset = currentScrollOffsetSupplier.getAsDouble() + (direction * pagePixels);
        setVerticalScrollOffset.accept(newOffset);
    }

    private void moveWord(WordDirection direction, boolean extendSelection) {
        int line = selectionModel.getCaretLine();
        int column = selectionModel.getCaretColumn();
        String lineText = document.getLineText(line);
        int target = findWordBoundary(lineText, column, direction);
        if (target == column) {
            if (direction == WordDirection.LEFT && line > 0) {
                line--;
                target = document.getLineText(line).length();
            } else if (direction == WordDirection.RIGHT && line < document.getLineCount() - 1) {
                line++;
                target = 0;
            }
        }
        caretCoordinator.moveCaret(line, target, extendSelection);
    }

    private void deleteWord(WordDirection direction) {
        if (selectionModel.hasSelection()) {
            deleteSelectionIfAny();
            return;
        }
        int line = selectionModel.getCaretLine();
        int column = selectionModel.getCaretColumn();
        String lineText = document.getLineText(line);
        int targetColumn = findWordBoundary(lineText, column, direction);
        if (targetColumn == column) {
            int caretOffset = selectionModel.getCaretOffset(document);
            if (direction == WordDirection.LEFT && line > 0) {
                document.delete(caretOffset - 1, caretOffset);
                caretCoordinator.moveCaretToOffset(caretOffset - 1);
            } else if (direction == WordDirection.RIGHT && line < document.getLineCount() - 1) {
                document.delete(caretOffset, caretOffset + 1);
            }
            return;
        }
        int lineStart = document.getLineStartOffset(line);
        if (direction == WordDirection.LEFT) {
            document.delete(lineStart + targetColumn, lineStart + column);
            caretCoordinator.moveCaret(line, targetColumn, false);
            return;
        }
        document.delete(lineStart + column, lineStart + targetColumn);
    }

    private int findWordBoundary(String lineText, int column, WordDirection direction) {
        return direction == WordDirection.LEFT
            ? WordBoundary.findWordLeft(lineText, column)
            : WordBoundary.findWordRight(lineText, column);
    }

    private void deleteSelectionIfAny() {
        if (!selectionModel.hasSelection()) {
            return;
        }
        int start = selectionModel.getSelectionStartOffset(document);
        int end = selectionModel.getSelectionEndOffset(document);
        document.delete(start, end);
        caretCoordinator.moveCaretToOffset(start);
    }
}
