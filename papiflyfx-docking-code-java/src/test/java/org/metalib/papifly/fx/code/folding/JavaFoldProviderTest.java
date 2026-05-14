package org.metalib.papifly.fx.code.folding;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.lexer.TokenMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFoldProviderTest {

    @Test
    void detectsBraceCommentAndTextBlockRegions() {
        JavaFoldProvider provider = new JavaFoldProvider();
        FoldMap foldMap = provider.recompute(
            List.of(
                "class Demo {",
                "  /* open",
                "     comment */",
                "  String t = \"\"\"",
                "     line",
                "     block",
                "  \"\"\";",
                "}"
            ),
            TokenMap.empty(),
            FoldMap.empty(),
            0,
            () -> false
        );

        assertTrue(foldMap.regions().stream().anyMatch(region -> region.kind() == FoldKind.BRACE_BLOCK));
        assertTrue(foldMap.regions().stream().anyMatch(region -> region.kind() == FoldKind.BLOCK_COMMENT));
        assertTrue(foldMap.regions().stream().anyMatch(region -> region.kind() == FoldKind.JAVA_TEXT_BLOCK));
    }
}

