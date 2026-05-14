package org.metalib.papifly.fx.code.folding;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.lexer.TokenMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaScriptFoldProviderTest {

    @Test
    void detectsTemplateAndTemplateExpressionRegions() {
        JavaScriptFoldProvider provider = new JavaScriptFoldProvider();
        FoldMap foldMap = provider.recompute(
            List.of(
                "const message = `hello ${",
                "  user.name + `${suffix}`",
                "}`;"
            ),
            TokenMap.empty(),
            FoldMap.empty(),
            0,
            () -> false
        );

        assertTrue(foldMap.regions().stream().anyMatch(region -> region.kind() == FoldKind.JS_TEMPLATE_BLOCK));
        assertTrue(foldMap.regions().stream().anyMatch(region -> region.kind() == FoldKind.JS_TEMPLATE_EXPR));
    }
}

