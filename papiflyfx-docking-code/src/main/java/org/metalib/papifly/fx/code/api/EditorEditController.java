package org.metalib.papifly.fx.code.api;

import org.metalib.papifly.fx.code.command.CaretRange;
import org.metalib.papifly.fx.code.command.MultiCaretModel;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.render.SelectionModel;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * Coordinates text-edit commands and multi-caret edit execution.
 */
final class EditorEditController {

    private final Document document;
    private final SelectionModel selectionModel;
    private final MultiCaretModel multiCaretModel;
    private final Runnable markViewportDirty;
    private final IntConsumer moveCaretToOffset;
    private final Supplier<String> clipboardTextSupplier;
    private final Consumer<String> clipboardTextConsumer;

    EditorEditController(
        Document document,
        SelectionModel selectionModel,
        MultiCaretModel multiCaretModel,
        Runnable markViewportDirty,
        IntConsumer moveCaretToOffset,
        Supplier<String> clipboardTextSupplier,
        Consumer<String> clipboardTextConsumer
    ) {
        this.document = Objects.requireNonNull(document, "document");
        this.selectionModel = Objects.requireNonNull(selectionModel, "selectionModel");
        this.multiCaretModel = Objects.requireNonNull(multiCaretModel, "multiCaretModel");
        this.markViewportDirty = Objects.requireNonNull(markViewportDirty, "markViewportDirty");
        this.moveCaretToOffset = Objects.requireNonNull(moveCaretToOffset, "moveCaretToOffset");
        this.clipboardTextSupplier = Objects.requireNonNull(clipboardTextSupplier, "clipboardTextSupplier");
        this.clipboardTextConsumer = Objects.requireNonNull(clipboardTextConsumer, "clipboardTextConsumer");
    }

    void insertTypedCharacter(String character) {
        if (character == null || character.isEmpty()) {
            return;
        }
        if (multiCaretModel.hasMultipleCarets()) {
            executeAtAllCarets(caret -> {
                int start = caret.getStartOffset(document);
                int end = caret.getEndOffset(document);
                if (caret.hasSelection()) {
                    document.replace(start, end, character);
                } else {
                    document.insert(start, character);
                }
            });
            markViewportDirty.run();
            return;
        }
        deleteSelectionIfAny();
        int offset = selectionModel.getCaretOffset(document);
        document.insert(offset, character);
        moveCaretToOffset.accept(offset + character.length());
    }

    void handleBackspace() {
        if (multiCaretModel.hasMultipleCarets()) {
            executeAtAllCarets(caret -> {
                if (caret.hasSelection()) {
                    document.delete(caret.getStartOffset(document), caret.getEndOffset(document));
                } else {
                    int offset = caret.getCaretOffset(document);
                    if (offset > 0) {
                        document.delete(offset - 1, offset);
                    }
                }
            });
            markViewportDirty.run();
            return;
        }
        if (selectionModel.hasSelection()) {
            deleteSelectionIfAny();
            return;
        }
        int offset = selectionModel.getCaretOffset(document);
        if (offset > 0) {
            document.delete(offset - 1, offset);
            moveCaretToOffset.accept(offset - 1);
        }
    }

    void handleDelete() {
        if (multiCaretModel.hasMultipleCarets()) {
            executeAtAllCarets(caret -> {
                if (caret.hasSelection()) {
                    document.delete(caret.getStartOffset(document), caret.getEndOffset(document));
                } else {
                    int offset = caret.getCaretOffset(document);
                    if (offset < document.length()) {
                        document.delete(offset, offset + 1);
                    }
                }
            });
            markViewportDirty.run();
            return;
        }
        if (selectionModel.hasSelection()) {
            deleteSelectionIfAny();
            return;
        }
        int offset = selectionModel.getCaretOffset(document);
        if (offset < document.length()) {
            document.delete(offset, offset + 1);
        }
    }

    void handleEnter() {
        if (multiCaretModel.hasMultipleCarets()) {
            executeAtAllCarets(caret -> {
                int start = caret.getStartOffset(document);
                int end = caret.getEndOffset(document);
                if (caret.hasSelection()) {
                    document.replace(start, end, "\n");
                } else {
                    document.insert(start, "\n");
                }
            });
            markViewportDirty.run();
            return;
        }
        deleteSelectionIfAny();
        int offset = selectionModel.getCaretOffset(document);
        document.insert(offset, "\n");
        moveCaretToOffset.accept(offset + 1);
    }

    void handleCopy() {
        if (selectionModel.hasSelection()) {
            clipboardTextConsumer.accept(selectionModel.getSelectedText(document));
        }
    }

    void handleCut() {
        handleCopy();
        if (multiCaretModel.hasMultipleCarets()) {
            executeAtAllCarets(caret -> {
                if (caret.hasSelection()) {
                    document.delete(caret.getStartOffset(document), caret.getEndOffset(document));
                }
            });
            markViewportDirty.run();
            return;
        }
        deleteSelectionIfAny();
    }

    void handlePaste() {
        String text = clipboardTextSupplier.get();
        if (text == null || text.isEmpty()) {
            return;
        }
        if (multiCaretModel.hasMultipleCarets()) {
            executeAtAllCarets(caret -> {
                int start = caret.getStartOffset(document);
                int end = caret.getEndOffset(document);
                if (caret.hasSelection()) {
                    document.replace(start, end, text);
                } else {
                    document.insert(start, text);
                }
            });
            markViewportDirty.run();
            return;
        }
        deleteSelectionIfAny();
        int offset = selectionModel.getCaretOffset(document);
        document.insert(offset, text);
        moveCaretToOffset.accept(offset + text.length());
    }

    private void deleteSelectionIfAny() {
        if (!selectionModel.hasSelection()) {
            return;
        }
        int start = selectionModel.getSelectionStartOffset(document);
        int end = selectionModel.getSelectionEndOffset(document);
        document.delete(start, end);
        moveCaretToOffset.accept(start);
    }

    private void executeAtAllCarets(Consumer<CaretRange> editAction) {
        List<CaretRange> carets = multiCaretModel.allCarets(document);
        carets.sort(Comparator.comparingInt((CaretRange cr) -> cr.getCaretOffset(document)).reversed());

        document.beginCompoundEdit();
        try {
            for (CaretRange caret : carets) {
                editAction.accept(caret);
            }
        } finally {
            document.endCompoundEdit();
        }
        multiCaretModel.clearSecondaryCarets();
    }
}
