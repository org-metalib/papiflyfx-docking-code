package org.metalib.papifly.fx.code.language;

import org.metalib.papifly.fx.code.folding.FoldMap;
import org.metalib.papifly.fx.code.folding.FoldKind;
import org.metalib.papifly.fx.code.folding.FoldProvider;
import org.metalib.papifly.fx.code.folding.FoldRegion;
import org.metalib.papifly.fx.code.lexer.Lexer;
import org.metalib.papifly.fx.code.lexer.LexResult;
import org.metalib.papifly.fx.code.lexer.LexState;
import org.metalib.papifly.fx.code.lexer.Token;
import org.metalib.papifly.fx.code.lexer.TokenMap;
import org.metalib.papifly.fx.code.lexer.TokenType;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

public final class TestLanguageSupportProvider implements LanguageSupportProvider {

    public static final String TEST_LANGUAGE_ID = "test-plugin-lang";

    @Override
    public Collection<LanguageSupport> getLanguageSupports() {
        return List.of(
            new LanguageSupport(
                TEST_LANGUAGE_ID, "Test Plugin Language",
                Set.of("tpl"), Set.of("tpl"),
                Set.of(),
                TestLexer::new, TestFoldProvider::new
            )
        );
    }

    static final class TestLexer implements Lexer {
        @Override
        public String languageId() {
            return TEST_LANGUAGE_ID;
        }

        @Override
        public LexResult lexLine(String lineText, LexState entryState) {
            String text = lineText == null ? "" : lineText;
            if (text.startsWith("plugin")) {
                return new LexResult(List.of(new Token(0, "plugin".length(), TokenType.KEYWORD)), LexState.DEFAULT);
            }
            if (text.startsWith("second")) {
                return new LexResult(List.of(new Token(0, "second".length(), TokenType.KEYWORD)), LexState.DEFAULT);
            }
            return new LexResult(List.of(), LexState.DEFAULT);
        }
    }

    static final class TestFoldProvider implements FoldProvider {
        @Override
        public String languageId() {
            return TEST_LANGUAGE_ID;
        }

        @Override
        public FoldMap recompute(List<String> lines, TokenMap tokenMap, FoldMap baseline,
                                 int dirtyStartLine, BooleanSupplier cancelled) {
            if (lines != null && lines.size() > 1) {
                return new FoldMap(List.of(new FoldRegion(0, lines.size() - 1, FoldKind.BRACE_BLOCK, 1, false)));
            }
            return FoldMap.empty();
        }
    }
}
