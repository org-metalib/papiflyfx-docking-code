package org.metalib.papifly.fx.code.render;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.document.Document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WrapMapTest {

    @Test
    void rebuildCreatesExpectedVisualRows() {
        Document document = new Document("abcdef\nxy\n");
        WrapMap wrapMap = new WrapMap();

        wrapMap.rebuild(document, 3.0, 1.0);

        assertEquals(3, wrapMap.wrapColumns());
        assertEquals(4, wrapMap.totalVisualRows());
        assertEquals(0, wrapMap.lineToFirstVisualRow(0));
        assertEquals(2, wrapMap.lineToFirstVisualRow(1));
        assertEquals(3, wrapMap.lineToFirstVisualRow(2));
        assertEquals(2, wrapMap.lineVisualRowCount(0));
        assertEquals(1, wrapMap.lineVisualRowCount(1));
        assertEquals(1, wrapMap.lineVisualRowCount(2));

        assertEquals(new WrapMap.VisualRow(0, 0, 3), wrapMap.visualRow(0));
        assertEquals(new WrapMap.VisualRow(0, 3, 6), wrapMap.visualRow(1));
        assertEquals(new WrapMap.VisualRow(1, 0, 2), wrapMap.visualRow(2));
        assertEquals(new WrapMap.VisualRow(2, 0, 0), wrapMap.visualRow(3));
    }

    @Test
    void lineColumnToVisualRowUsesWrappedRowContainingColumn() {
        Document document = new Document("abcdefghij");
        WrapMap wrapMap = new WrapMap();
        wrapMap.rebuild(document, 4.0, 1.0);

        assertEquals(0, wrapMap.lineColumnToVisualRow(0, 0));
        assertEquals(0, wrapMap.lineColumnToVisualRow(0, 3));
        assertEquals(1, wrapMap.lineColumnToVisualRow(0, 4));
        assertEquals(2, wrapMap.lineColumnToVisualRow(0, 9));
        assertEquals(2, wrapMap.lineColumnToVisualRow(0, 10));
    }

    @Test
    void updateRebuildsWithNewWidth() {
        Document document = new Document("abcdef");
        WrapMap wrapMap = new WrapMap();
        wrapMap.rebuild(document, 6.0, 1.0);
        assertEquals(1, wrapMap.totalVisualRows());

        wrapMap.update(document, 0, 0, 2.0, 1.0);
        assertEquals(3, wrapMap.totalVisualRows());
        assertEquals(new WrapMap.VisualRow(0, 4, 6), wrapMap.visualRow(2));
    }

    @Test
    void rebuildWithEmptyDocumentStateIsSafe() {
        WrapMap wrapMap = new WrapMap();
        wrapMap.rebuild(null, 100.0, 8.0);

        assertEquals(0, wrapMap.totalVisualRows());
        assertTrue(!wrapMap.hasData());
    }
}
