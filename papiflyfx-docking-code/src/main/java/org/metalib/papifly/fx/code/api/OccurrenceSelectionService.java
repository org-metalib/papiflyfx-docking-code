package org.metalib.papifly.fx.code.api;

import org.metalib.papifly.fx.code.command.CaretRange;
import org.metalib.papifly.fx.code.command.MultiCaretModel;
import org.metalib.papifly.fx.code.command.WordBoundary;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.render.SelectionModel;

import java.util.List;
import java.util.Objects;

/**
 * Handles selection expansion commands for next/all occurrences.
 */
final class OccurrenceSelectionService {

    private final Document document;
    private final SelectionModel selectionModel;
    private final MultiCaretModel multiCaretModel;
    private final Runnable markViewportDirty;

    OccurrenceSelectionService(
        Document document,
        SelectionModel selectionModel,
        MultiCaretModel multiCaretModel,
        Runnable markViewportDirty
    ) {
        this.document = Objects.requireNonNull(document, "document");
        this.selectionModel = Objects.requireNonNull(selectionModel, "selectionModel");
        this.multiCaretModel = Objects.requireNonNull(multiCaretModel, "multiCaretModel");
        this.markViewportDirty = Objects.requireNonNull(markViewportDirty, "markViewportDirty");
    }

    void selectNextOccurrence() {
        if (!selectionModel.hasSelection()) {
            selectWordUnderCaret();
            return;
        }

        String selectedText = selectionModel.getSelectedText(document);
        if (selectedText.isEmpty()) {
            return;
        }

        String fullText = document.getText();
        List<CaretRange> allCarets = multiCaretModel.allCarets(document);
        if (allCarets.isEmpty()) {
            return;
        }
        CaretRange lastCaret = allCarets.get(allCarets.size() - 1);
        int searchFrom = lastCaret.getEndOffset(document);

        int found = fullText.indexOf(selectedText, searchFrom);
        if (found < 0) {
            found = fullText.indexOf(selectedText);
        }
        if (found < 0) {
            return;
        }

        int foundEnd = found + selectedText.length();
        if (containsOccurrence(allCarets, found, foundEnd)) {
            return;
        }

        int anchorLine = document.getLineForOffset(found);
        int anchorColumn = document.getColumnForOffset(found);
        int caretLine = document.getLineForOffset(foundEnd);
        int caretColumn = document.getColumnForOffset(foundEnd);
        multiCaretModel.addCaret(new CaretRange(anchorLine, anchorColumn, caretLine, caretColumn));
        markViewportDirty.run();
    }

    void selectAllOccurrences() {
        if (!selectionModel.hasSelection() && !selectWordUnderCaret()) {
            return;
        }
        String selectedText = selectionModel.getSelectedText(document);
        if (selectedText.isEmpty()) {
            return;
        }

        String fullText = document.getText();
        int searchFrom = 0;
        boolean first = true;
        while (true) {
            int found = fullText.indexOf(selectedText, searchFrom);
            if (found < 0) {
                break;
            }
            int foundEnd = found + selectedText.length();
            int anchorLine = document.getLineForOffset(found);
            int anchorColumn = document.getColumnForOffset(found);
            int caretLine = document.getLineForOffset(foundEnd);
            int caretColumn = document.getColumnForOffset(foundEnd);
            if (first) {
                selectionModel.moveCaret(anchorLine, anchorColumn);
                selectionModel.moveCaretWithSelection(caretLine, caretColumn);
                first = false;
            } else {
                multiCaretModel.addCaretNoStack(new CaretRange(anchorLine, anchorColumn, caretLine, caretColumn));
            }
            searchFrom = foundEnd;
        }
        markViewportDirty.run();
    }

    private boolean selectWordUnderCaret() {
        int line = selectionModel.getCaretLine();
        int column = selectionModel.getCaretColumn();
        String lineText = document.getLineText(line);
        if (lineText.isEmpty() || (column >= lineText.length() && column > 0)) {
            return false;
        }

        int wordStart = column;
        while (wordStart > 0 && WordBoundary.isWordChar(lineText.charAt(wordStart - 1))) {
            wordStart--;
        }
        int wordEnd = column;
        while (wordEnd < lineText.length() && WordBoundary.isWordChar(lineText.charAt(wordEnd))) {
            wordEnd++;
        }
        if (wordStart == wordEnd) {
            return false;
        }
        selectionModel.moveCaret(line, wordStart);
        selectionModel.moveCaretWithSelection(line, wordEnd);
        markViewportDirty.run();
        return true;
    }

    private boolean containsOccurrence(List<CaretRange> existingCarets, int startOffset, int endOffset) {
        for (CaretRange existing : existingCarets) {
            if (existing.getStartOffset(document) == startOffset && existing.getEndOffset(document) == endOffset) {
                return true;
            }
        }
        return false;
    }
}

