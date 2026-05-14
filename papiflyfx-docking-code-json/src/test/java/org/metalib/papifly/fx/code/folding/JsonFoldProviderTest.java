package org.metalib.papifly.fx.code.folding;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.lexer.TokenMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonFoldProviderTest {

    @Test
    void detectsObjectAndArrayRegions() {
        JsonFoldProvider provider = new JsonFoldProvider();
        FoldMap foldMap = provider.recompute(
            List.of(
                "{",
                "  \"items\": [",
                "    { \"id\": 1 }",
                "  ]",
                "}"
            ),
            TokenMap.empty(),
            FoldMap.empty(),
            0,
            () -> false
        );

        assertTrue(foldMap.regions().stream().anyMatch(region -> region.kind() == FoldKind.JSON_OBJECT));
        assertTrue(foldMap.regions().stream().anyMatch(region -> region.kind() == FoldKind.JSON_ARRAY));
    }
}

