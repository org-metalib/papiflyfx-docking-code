package org.metalib.papifly.fx.code.render;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.code.lexer.Token;
import org.metalib.papifly.fx.code.lexer.TokenType;
import org.metalib.papifly.fx.code.theme.CodeEditorTheme;

import java.util.List;
import java.util.Objects;

/**
 * Paints tokenized text content for visible lines.
 */
final class TextPass implements RenderPass {

    @Override
    public void renderFull(RenderContext context) {
        GraphicsContext gc = context.graphics();
        gc.setFont(context.glyphCache().getFont());
        for (RenderLine renderLine : context.renderLines()) {
            drawTokenizedLine(context, renderLine);
        }
    }

    @Override
    public void renderLine(RenderContext context, RenderLine renderLine) {
        GraphicsContext gc = context.graphics();
        gc.setFont(context.glyphCache().getFont());
        drawTokenizedLine(context, renderLine);
    }

    private void drawTokenizedLine(RenderContext context, RenderLine renderLine) {
        GraphicsContext gc = context.graphics();
        String text = renderLine.text();
        if (text.isEmpty()) {
            return;
        }
        double baseX = context.textOriginX();
        Paint foreground = context.theme().editorForeground();
        gc.setFill(foreground);
        gc.fillText(text, baseX, renderLine.y() + context.baseline());
        List<Token> tokens = renderLine.tokens();
        if (tokens.isEmpty()) {
            return;
        }

        int rowStart = renderLine.startColumn();
        int rowEnd = renderLine.endColumn();
        int runStart = -1;
        int runEnd = -1;
        Paint runColor = null;
        int textLength = text.length();
        for (Token token : tokens) {
            int tokenStart = Math.max(rowStart, token.startColumn());
            int tokenEnd = Math.min(rowEnd, token.endColumn());
            int start = Math.max(0, Math.min(tokenStart - rowStart, textLength));
            int end = Math.max(start, Math.min(tokenEnd - rowStart, textLength));
            if (end <= start) {
                continue;
            }
            Paint color = tokenColor(context, token);
            if (samePaint(color, foreground)) {
                if (runColor != null) {
                    drawSegment(context, text, runStart, runEnd, renderLine.y(), runColor, baseX);
                    runStart = -1;
                    runEnd = -1;
                    runColor = null;
                }
                continue;
            }
            if (runColor != null && samePaint(runColor, color) && start <= runEnd) {
                runEnd = Math.max(runEnd, end);
                continue;
            }
            if (runColor != null) {
                drawSegment(context, text, runStart, runEnd, renderLine.y(), runColor, baseX);
            }
            runStart = start;
            runEnd = end;
            runColor = color;
        }
        if (runColor != null) {
            drawSegment(context, text, runStart, runEnd, renderLine.y(), runColor, baseX);
        }
    }

    private void drawSegment(
        RenderContext context,
        String text,
        int startColumn,
        int endColumn,
        double y,
        Paint color,
        double baseX
    ) {
        if (endColumn <= startColumn) {
            return;
        }
        GraphicsContext gc = context.graphics();
        gc.setFill(color);
        gc.fillText(
            text.substring(startColumn, endColumn),
            baseX + (startColumn * context.charWidth()),
            y + context.baseline()
        );
    }

    private boolean samePaint(Paint a, Paint b) {
        return Objects.equals(a, b);
    }

    private Paint tokenColor(RenderContext context, Token token) {
        return resolveTokenColor(context.theme(), token);
    }

    static Paint resolveTokenColor(CodeEditorTheme theme, Token token) {
        if (theme == null || token == null) {
            return theme == null ? null : theme.editorForeground();
        }
        if (token.styleScope() != null) {
            Paint scopedColor = theme.syntaxScopeColor(token.styleScope()).orElse(null);
            if (scopedColor != null) {
                return scopedColor;
            }
        }
        TokenType tokenType = token.type();
        if (tokenType == null) {
            return theme.editorForeground();
        }
        return switch (tokenType) {
            case KEYWORD -> theme.keywordColor();
            case STRING -> theme.stringColor();
            case JSON_KEY -> theme.jsonKeyColor();
            case YAML_KEY -> theme.yamlKeyColor();
            case YAML_ANCHOR, YAML_ALIAS -> theme.yamlAnchorColor();
            case YAML_TAG -> theme.yamlTagColor();
            case COMMENT -> theme.commentColor();
            case NUMBER -> theme.numberColor();
            case BOOLEAN -> theme.booleanColor();
            case NULL_LITERAL -> theme.nullLiteralColor();
            case HEADLINE -> theme.headlineColor();
            case LIST_ITEM -> theme.listItemColor();
            case CODE_BLOCK -> theme.codeBlockColor();
            case TEXT -> theme.editorForeground();
            default -> theme.editorForeground();
        };
    }
}
