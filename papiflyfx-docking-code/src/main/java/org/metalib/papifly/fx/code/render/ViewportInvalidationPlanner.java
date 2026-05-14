package org.metalib.papifly.fx.code.render;

import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.document.DocumentChangeEvent;

/**
 * Plans viewport dirty-line updates from document change events.
 */
final class ViewportInvalidationPlanner {

    private static final int SAFETY_LINES = 2;

    InvalidationPlan plan(
        Document document,
        DocumentChangeEvent event,
        int firstVisibleLine,
        int visibleLineCount,
        int prefetchLines
    ) {
        if (document == null || event == null || document.getLineCount() <= 0) {
            return InvalidationPlan.forFullRedraw();
        }
        if (event.type() == DocumentChangeEvent.ChangeType.SET_TEXT
            || event.type() == DocumentChangeEvent.ChangeType.UNDO
            || event.type() == DocumentChangeEvent.ChangeType.REDO) {
            return InvalidationPlan.forFullRedraw();
        }
        if (visibleLineCount <= 0) {
            return InvalidationPlan.forFullRedraw();
        }

        int maxLine = document.getLineCount() - 1;
        int safeVisibleStart = clamp(firstVisibleLine, 0, maxLine);
        int safeVisibleEnd = clamp(firstVisibleLine + visibleLineCount + prefetchLines - 1, 0, maxLine);
        if (safeVisibleEnd < safeVisibleStart) {
            return InvalidationPlan.forFullRedraw();
        }

        int safeOffset = clamp(event.offset(), 0, document.length());
        int impactOffset = safeOffset + Math.max(event.oldLength(), event.newLength());
        int safeImpactOffset = clamp(impactOffset, 0, document.length());

        int startLine = document.getLineForOffset(safeOffset);
        int endLine = document.getLineForOffset(safeImpactOffset);
        int boundedEndLine = clamp(endLine + SAFETY_LINES, 0, maxLine);

        if (startLine > safeVisibleEnd) {
            return InvalidationPlan.none();
        }

        // Any change above the viewport may shift visible line content.
        if (startLine < safeVisibleStart) {
            return InvalidationPlan.lines(safeVisibleStart, safeVisibleEnd);
        }

        int dirtyStart = Math.max(startLine, safeVisibleStart);
        int dirtyEnd = Math.min(boundedEndLine, safeVisibleEnd);
        if (dirtyEnd < dirtyStart) {
            return InvalidationPlan.none();
        }
        return InvalidationPlan.lines(dirtyStart, dirtyEnd);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    record InvalidationPlan(int startLine, int endLine, boolean fullRedraw) {

        static InvalidationPlan lines(int startLine, int endLine) {
            return new InvalidationPlan(startLine, endLine, false);
        }

        static InvalidationPlan none() {
            return new InvalidationPlan(-1, -1, false);
        }

        static InvalidationPlan forFullRedraw() {
            return new InvalidationPlan(-1, -1, true);
        }

        boolean hasLineRange() {
            return !fullRedraw && startLine >= 0 && endLine >= startLine;
        }
    }
}
