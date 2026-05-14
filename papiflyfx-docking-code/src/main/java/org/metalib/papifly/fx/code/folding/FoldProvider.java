package org.metalib.papifly.fx.code.folding;

import org.metalib.papifly.fx.code.lexer.TokenMap;

import java.util.List;
import java.util.function.BooleanSupplier;

public interface FoldProvider {

    String languageId();

    FoldMap recompute(
        List<String> lines,
        TokenMap tokenMap,
        FoldMap baseline,
        int dirtyStartLine,
        BooleanSupplier cancelled
    );
}

