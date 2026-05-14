package org.metalib.papifly.fx.code.render;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.document.DocumentChangeEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewportInvalidationPlannerTest {

    @Test
    void setTextUsesFullRedrawFallback() {
        Document document = documentWithLines(100);
        ViewportInvalidationPlanner planner = new ViewportInvalidationPlanner();

        ViewportInvalidationPlanner.InvalidationPlan plan = planner.plan(
            document,
            new DocumentChangeEvent(0, 10, 10, DocumentChangeEvent.ChangeType.SET_TEXT),
            0,
            20,
            2
        );

        assertTrue(plan.fullRedraw());
    }

    @Test
    void insertInVisibleRangeReturnsBoundedWindow() {
        Document document = documentWithLines(100);
        ViewportInvalidationPlanner planner = new ViewportInvalidationPlanner();
        int offset = document.getLineStartOffset(10) + 2;

        ViewportInvalidationPlanner.InvalidationPlan plan = planner.plan(
            document,
            new DocumentChangeEvent(offset, 0, 1, DocumentChangeEvent.ChangeType.INSERT),
            8,
            12,
            2
        );

        assertFalse(plan.fullRedraw());
        assertTrue(plan.hasLineRange());
        assertEquals(10, plan.startLine());
        assertEquals(12, plan.endLine());
    }

    @Test
    void changeBelowVisibleRangeSkipsDirtyLines() {
        Document document = documentWithLines(100);
        ViewportInvalidationPlanner planner = new ViewportInvalidationPlanner();
        int offset = document.getLineStartOffset(60);

        ViewportInvalidationPlanner.InvalidationPlan plan = planner.plan(
            document,
            new DocumentChangeEvent(offset, 0, 1, DocumentChangeEvent.ChangeType.INSERT),
            0,
            20,
            2
        );

        assertFalse(plan.fullRedraw());
        assertFalse(plan.hasLineRange());
    }

    @Test
    void changeAboveVisibleRangeMarksVisibleWindow() {
        Document document = documentWithLines(100);
        ViewportInvalidationPlanner planner = new ViewportInvalidationPlanner();
        int offset = document.getLineStartOffset(5);

        ViewportInvalidationPlanner.InvalidationPlan plan = planner.plan(
            document,
            new DocumentChangeEvent(offset, 0, 1, DocumentChangeEvent.ChangeType.INSERT),
            20,
            10,
            2
        );

        assertFalse(plan.fullRedraw());
        assertTrue(plan.hasLineRange());
        assertEquals(20, plan.startLine());
        assertEquals(31, plan.endLine());
    }

    private static Document documentWithLines(int lineCount) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lineCount; i++) {
            builder.append("line").append(i);
            if (i < lineCount - 1) {
                builder.append('\n');
            }
        }
        return new Document(builder.toString());
    }
}

