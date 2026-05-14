package org.metalib.papifly.fx.code.folding;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisibleLineMapTest {

    @Test
    void mapsVisibleAndLogicalLinesWhenCollapsed() {
        FoldMap foldMap = new FoldMap(List.of(
            new FoldRegion(1, 4, FoldKind.BRACE_BLOCK, 1, true)
        ));
        VisibleLineMap visibleLineMap = new VisibleLineMap();
        visibleLineMap.rebuild(6, foldMap);

        assertEquals(3, visibleLineMap.visibleCount());
        assertEquals(0, visibleLineMap.visibleToLogical(0));
        assertEquals(1, visibleLineMap.visibleToLogical(1));
        assertEquals(5, visibleLineMap.visibleToLogical(2));
        assertTrue(visibleLineMap.isHiddenLogicalLine(2));
        assertEquals(1, visibleLineMap.nearestVisibleLogicalLine(2));
    }
}

