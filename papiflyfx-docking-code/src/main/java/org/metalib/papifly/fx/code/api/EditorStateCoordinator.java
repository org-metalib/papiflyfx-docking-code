package org.metalib.papifly.fx.code.api;

import org.metalib.papifly.fx.code.command.CaretRange;
import org.metalib.papifly.fx.code.command.MultiCaretModel;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.render.SelectionModel;
import org.metalib.papifly.fx.code.render.Viewport;
import org.metalib.papifly.fx.code.state.CaretStateData;
import org.metalib.papifly.fx.code.state.EditorStateData;
import org.metalib.papifly.fx.code.state.FoldRegionRef;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

/**
 * Coordinates capture/apply of editor state and caret restoration logic.
 */
final class EditorStateCoordinator {

    private final Document document;
    private final SelectionModel selectionModel;
    private final MultiCaretModel multiCaretModel;
    private final Viewport viewport;
    private final int maxRestoredSecondaryCarets;
    private final Runnable clearPreferredVerticalColumn;

    EditorStateCoordinator(
        Document document,
        SelectionModel selectionModel,
        MultiCaretModel multiCaretModel,
        Viewport viewport,
        int maxRestoredSecondaryCarets,
        Runnable clearPreferredVerticalColumn
    ) {
        this.document = document;
        this.selectionModel = selectionModel;
        this.multiCaretModel = multiCaretModel;
        this.viewport = viewport;
        this.maxRestoredSecondaryCarets = maxRestoredSecondaryCarets;
        this.clearPreferredVerticalColumn = clearPreferredVerticalColumn;
    }

    EditorStateData captureState(
        Supplier<String> filePathSupplier,
        Supplier<String> languageIdSupplier,
        Supplier<List<Integer>> foldedLinesSupplier,
        Supplier<List<FoldRegionRef>> foldedRegionsSupplier,
        Supplier<Boolean> wordWrapSupplier
    ) {
        List<CaretStateData> secondaryCarets = multiCaretModel.getSecondaryCarets()
            .stream()
            .map(caret -> new CaretStateData(
                caret.anchorLine(),
                caret.anchorColumn(),
                caret.caretLine(),
                caret.caretColumn()
            ))
            .toList();
        return new EditorStateData(
            filePathSupplier.get(),
            selectionModel.getCaretLine(),
            selectionModel.getCaretColumn(),
            viewport.getScrollOffset(),
            viewport.getHorizontalScrollOffset(),
            Boolean.TRUE.equals(wordWrapSupplier.get()),
            languageIdSupplier.get(),
            foldedLinesSupplier.get(),
            foldedRegionsSupplier.get(),
            selectionModel.getAnchorLine(),
            selectionModel.getAnchorColumn(),
            secondaryCarets
        );
    }

    void applyState(
        EditorStateData state,
        Consumer<String> filePathSetter,
        Consumer<String> languageIdSetter,
        Consumer<List<Integer>> foldedLinesSetter,
        Consumer<List<FoldRegionRef>> foldedRegionsSetter,
        Consumer<Boolean> wordWrapSetter,
        DoubleConsumer verticalScrollSetter,
        DoubleConsumer horizontalScrollSetter
    ) {
        if (state == null) {
            return;
        }
        filePathSetter.accept(state.filePath());
        languageIdSetter.accept(state.languageId());
        foldedLinesSetter.accept(state.foldedLines());
        foldedRegionsSetter.accept(state.foldedRegions());
        wordWrapSetter.accept(state.wordWrap());
        applyPrimaryCaretState(state.anchorLine(), state.anchorColumn(), state.cursorLine(), state.cursorColumn());
        applySecondaryCaretState(state.secondaryCarets());
        verticalScrollSetter.accept(state.verticalScrollOffset());
        horizontalScrollSetter.accept(state.horizontalScrollOffset());
    }

    void movePrimaryCaret(int line, int column) {
        clearPreferredVerticalColumn.run();
        int safeLine = clampLine(line);
        int safeColumn = clampColumn(safeLine, column);
        selectionModel.moveCaret(safeLine, safeColumn);
        viewport.markDirty();
    }

    private void applyPrimaryCaretState(int anchorLine, int anchorColumn, int caretLine, int caretColumn) {
        clearPreferredVerticalColumn.run();
        int safeAnchorLine = clampLine(anchorLine);
        int safeAnchorColumn = clampColumn(safeAnchorLine, anchorColumn);
        int safeCaretLine = clampLine(caretLine);
        int safeCaretColumn = clampColumn(safeCaretLine, caretColumn);
        selectionModel.moveCaret(safeAnchorLine, safeAnchorColumn);
        if (safeAnchorLine != safeCaretLine || safeAnchorColumn != safeCaretColumn) {
            selectionModel.moveCaretWithSelection(safeCaretLine, safeCaretColumn);
        }
        viewport.markDirty();
    }

    private void applySecondaryCaretState(List<CaretStateData> secondaryCarets) {
        if (secondaryCarets == null || secondaryCarets.isEmpty()) {
            multiCaretModel.clearSecondaryCarets();
            return;
        }
        int targetCount = Math.min(secondaryCarets.size(), maxRestoredSecondaryCarets);
        Set<CaretRange> unique = new LinkedHashSet<>(targetCount);
        CaretRange primary = CaretRange.fromSelectionModel(selectionModel);
        for (CaretStateData caret : secondaryCarets) {
            if (unique.size() >= targetCount) {
                break;
            }
            if (caret == null) {
                continue;
            }
            int safeAnchorLine = clampLine(caret.anchorLine());
            int safeAnchorColumn = clampColumn(safeAnchorLine, caret.anchorColumn());
            int safeCaretLine = clampLine(caret.caretLine());
            int safeCaretColumn = clampColumn(safeCaretLine, caret.caretColumn());
            CaretRange normalized = new CaretRange(safeAnchorLine, safeAnchorColumn, safeCaretLine, safeCaretColumn);
            if (!normalized.equals(primary)) {
                unique.add(normalized);
            }
        }
        if (unique.isEmpty()) {
            multiCaretModel.clearSecondaryCarets();
            return;
        }
        multiCaretModel.setSecondaryCarets(List.copyOf(unique));
    }

    private int clampLine(int line) {
        int maxLine = Math.max(0, document.getLineCount() - 1);
        return Math.max(0, Math.min(line, maxLine));
    }

    private int clampColumn(int line, int column) {
        int maxColumn = document.getLineText(line).length();
        return Math.max(0, Math.min(column, maxColumn));
    }
}
