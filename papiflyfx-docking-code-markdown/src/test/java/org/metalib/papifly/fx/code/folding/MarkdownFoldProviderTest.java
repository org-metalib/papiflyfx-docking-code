package org.metalib.papifly.fx.code.folding;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.lexer.TokenMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownFoldProviderTest {

    @Test
    void detectsFenceAndHeadingRegions() {
        MarkdownFoldProvider provider = new MarkdownFoldProvider();
        FoldMap foldMap = provider.recompute(
            List.of(
                "# Top",
                "intro",
                "```java",
                "class Demo {}",
                "```",
                "## Child",
                "details"
            ),
            TokenMap.empty(),
            FoldMap.empty(),
            0,
            () -> false
        );

        assertTrue(foldMap.regions().stream().anyMatch(region -> region.kind() == FoldKind.MARKDOWN_FENCE));
        assertTrue(foldMap.regions().stream().anyMatch(region -> region.kind() == FoldKind.MARKDOWN_SECTION));
    }
}

