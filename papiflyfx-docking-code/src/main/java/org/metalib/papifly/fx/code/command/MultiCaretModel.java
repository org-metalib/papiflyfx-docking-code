package org.metalib.papifly.fx.code.command;

import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.render.SelectionModel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

/**
 * Manages multiple carets for simultaneous editing.
 * <p>
 * The primary caret lives in the existing {@link SelectionModel}. Secondary
 * carets are stored as immutable {@link CaretRange} snapshots. An occurrence
 * stack supports undoing the last "select next occurrence" addition.
 */
public class MultiCaretModel {

    private final SelectionModel primary;
    private final List<CaretRange> secondaryCarets = new ArrayList<>();
    private final Deque<CaretRange> occurrenceStack = new ArrayDeque<>();

    /**
     * Creates a multi-caret model bound to the primary selection model.
     *
     * @param primary primary selection model
     */
    public MultiCaretModel(SelectionModel primary) {
        this.primary = primary;
    }

    /**
     * Returns {@code true} when more than one caret is active.
     *
     * @return {@code true} when at least one secondary caret is active
     */
    public boolean hasMultipleCarets() {
        return !secondaryCarets.isEmpty();
    }

    /**
     * Returns all carets (primary + secondaries) sorted by document offset ascending.
     *
     * @param document source document for offset ordering
     * @return ordered list containing primary and secondary carets
     */
    public List<CaretRange> allCarets(Document document) {
        List<CaretRange> all = new ArrayList<>(secondaryCarets.size() + 1);
        all.add(CaretRange.fromSelectionModel(primary));
        all.addAll(secondaryCarets);
        all.sort(Comparator.comparingInt(cr -> cr.getCaretOffset(document)));
        return all;
    }

    /**
     * Returns the secondary carets (excluding the primary).
     *
     * @return immutable list of secondary carets
     */
    public List<CaretRange> getSecondaryCarets() {
        return List.copyOf(secondaryCarets);
    }

    /**
     * Adds a secondary caret and pushes it onto the occurrence stack.
     *
     * @param caret secondary caret to add
     */
    public void addCaret(CaretRange caret) {
        secondaryCarets.add(caret);
        occurrenceStack.push(caret);
    }

    /**
     * Adds a secondary caret without pushing to the occurrence stack.
     *
     * @param caret secondary caret to add
     */
    public void addCaretNoStack(CaretRange caret) {
        secondaryCarets.add(caret);
    }

    /**
     * Replaces all secondary carets with the given list and clears the occurrence stack.
     * Used by box selection where all secondaries are rebuilt on each drag event.
     *
     * @param carets new secondary caret list
     */
    public void setSecondaryCarets(List<CaretRange> carets) {
        secondaryCarets.clear();
        secondaryCarets.addAll(carets);
        occurrenceStack.clear();
    }

    /**
     * Removes all secondary carets and clears the occurrence stack.
     */
    public void clearSecondaryCarets() {
        secondaryCarets.clear();
        occurrenceStack.clear();
    }

    /**
     * Undoes the last occurrence selection (Ctrl+U / Cmd+U).
     * <p>
     * Pops the most recent entry from the occurrence stack, removes the
     * matching secondary caret, and — if the primary was the last added —
     * promotes the previous secondary to primary.
     */
    public void undoLastOccurrence() {
        if (occurrenceStack.isEmpty()) {
            return;
        }
        CaretRange last = occurrenceStack.pop();
        secondaryCarets.remove(last);
    }

    /**
     * Sorts all carets by document offset and merges overlapping or
     * duplicate ranges. After normalization, the primary caret is updated
     * to the first caret in the sorted order and the rest become secondaries.
     *
     * @param document source document for offset calculations
     */
    public void normalizeAndMerge(Document document) {
        List<CaretRange> all = allCarets(document);
        if (all.size() <= 1) {
            return;
        }

        // Sort by start offset, then by end offset
        all.sort((a, b) -> {
            int cmp = Integer.compare(a.getStartOffset(document), b.getStartOffset(document));
            if (cmp != 0) return cmp;
            return Integer.compare(a.getEndOffset(document), b.getEndOffset(document));
        });

        // Merge overlapping/adjacent ranges
        List<CaretRange> merged = new ArrayList<>();
        CaretRange current = all.get(0);
        for (int i = 1; i < all.size(); i++) {
            CaretRange next = all.get(i);
            if (next.getStartOffset(document) <= current.getEndOffset(document)) {
                // Overlapping or adjacent — merge: take the wider range
                int mergedStartOffset = current.getStartOffset(document);
                int mergedEndOffset = Math.max(current.getEndOffset(document), next.getEndOffset(document));
                // Caret goes to the end of the merged range
                int caretLine = document.getLineForOffset(mergedEndOffset);
                int caretCol = document.getColumnForOffset(mergedEndOffset);
                int anchorLine = document.getLineForOffset(mergedStartOffset);
                int anchorCol = document.getColumnForOffset(mergedStartOffset);
                if (mergedStartOffset == mergedEndOffset) {
                    current = new CaretRange(caretLine, caretCol, caretLine, caretCol);
                } else {
                    current = new CaretRange(anchorLine, anchorCol, caretLine, caretCol);
                }
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);

        // First merged range becomes primary, rest become secondaries
        CaretRange first = merged.get(0);
        if (first.hasSelection()) {
            primary.moveCaret(first.anchorLine(), first.anchorColumn());
            primary.moveCaretWithSelection(first.caretLine(), first.caretColumn());
        } else {
            primary.moveCaret(first.caretLine(), first.caretColumn());
        }

        secondaryCarets.clear();
        for (int i = 1; i < merged.size(); i++) {
            secondaryCarets.add(merged.get(i));
        }
    }
}
