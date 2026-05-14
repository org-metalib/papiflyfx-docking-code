package org.metalib.papifly.fx.code.folding;

import org.metalib.papifly.fx.code.lexer.PlainTextLexer;
import org.metalib.papifly.fx.code.lexer.TokenMap;

import java.util.List;
import java.util.function.BooleanSupplier;

final class PlainTextFoldProvider implements FoldProvider {

    @Override
    public String languageId() {
        return PlainTextLexer.LANGUAGE_ID;
    }

    @Override
    public FoldMap recompute(
        List<String> lines,
        TokenMap tokenMap,
        FoldMap baseline,
        int dirtyStartLine,
        BooleanSupplier cancelled
    ) {
        return FoldMap.empty();
    }
}

