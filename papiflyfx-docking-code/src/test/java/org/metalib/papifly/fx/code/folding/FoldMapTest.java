package org.metalib.papifly.fx.code.folding;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoldMapTest {

    @Test
    void togglingHeaderHidesOnlyRegionInterior() {
        FoldMap map = new FoldMap(List.of(
            new FoldRegion(1, 5, FoldKind.BRACE_BLOCK, 1, false),
            new FoldRegion(2, 3, FoldKind.BRACE_BLOCK, 2, false)
        ));
        FoldMap collapsed = map.toggleAtHeaderLine(1);

        assertTrue(collapsed.isCollapsedHeader(1));
        assertFalse(collapsed.isHiddenLine(1));
        assertTrue(collapsed.isHiddenLine(2));
        assertTrue(collapsed.isHiddenLine(5));
        assertFalse(collapsed.isHiddenLine(6));
    }

    @Test
    void collapseAllAndExpandAllRoundTrip() {
        FoldMap map = new FoldMap(List.of(
            new FoldRegion(0, 2, FoldKind.BRACE_BLOCK, 1, false),
            new FoldRegion(4, 7, FoldKind.BLOCK_COMMENT, 1, false)
        ));
        FoldMap collapsed = map.collapseAll();
        FoldMap expanded = collapsed.expandAll();

        assertEquals(2, collapsed.collapsedHeaderLines().size());
        assertTrue(expanded.collapsedHeaderLines().isEmpty());
    }
}

