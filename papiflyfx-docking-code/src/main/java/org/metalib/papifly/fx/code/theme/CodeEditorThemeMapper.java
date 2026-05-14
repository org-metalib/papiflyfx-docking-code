package org.metalib.papifly.fx.code.theme;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.ui.UiCommonThemeSupport;

/**
 * Maps a docking {@link Theme} to a {@link CodeEditorTheme} by composition.
 * <p>
 * Dark/light detection uses the brightness of the base theme's background
 * color, choosing the appropriate editor palette and blending accent colors
 * from the base theme.
 */
public final class CodeEditorThemeMapper {

    private static final double DARK_THRESHOLD = 0.5;

    /**
     * Utility class.
     */
    private CodeEditorThemeMapper() {}

    /**
     * Creates a {@link CodeEditorTheme} derived from the given docking theme.
     *
     * @param theme docking theme source, or {@code null} for default dark theme
     * @return mapped editor theme
     */
    public static CodeEditorTheme map(Theme theme) {
        if (theme == null) {
            return CodeEditorTheme.dark();
        }

        Theme resolved = UiCommonThemeSupport.resolvedTheme(theme);
        boolean dark = isDark(resolved.background());
        CodeEditorTheme base = dark ? CodeEditorTheme.dark() : CodeEditorTheme.light();
        Color background = UiCommonThemeSupport.background(resolved);
        Color headerBackground = UiCommonThemeSupport.headerBackground(resolved);
        Color accent = UiCommonThemeSupport.accent(resolved);
        Color headerBackgroundActive = UiCommonThemeSupport.headerBackgroundActive(resolved);
        Color textPrimary = UiCommonThemeSupport.textPrimary(resolved);
        Color textActive = UiCommonThemeSupport.textActive(resolved);
        Color border = UiCommonThemeSupport.border(resolved);
        Color hover = UiCommonThemeSupport.hover(resolved);
        Color pressed = UiCommonThemeSupport.pressed(resolved);
        Color selection = alpha(accent, dark ? 0.24 : 0.18);
        Color lineNumber = blend(textPrimary, background, dark ? 0.42 : 0.56);
        Color currentLine = blend(background, headerBackgroundActive, dark ? 0.52 : 0.36);
        Color searchHighlight = alpha(accent, dark ? 0.18 : 0.12);
        Color scrollbarTrack = alpha(border, dark ? 0.22 : 0.12);
        Color scrollbarThumb = alpha(textPrimary, dark ? 0.32 : 0.24);
        Color scrollbarThumbHover = alpha(textPrimary, dark ? 0.46 : 0.36);
        Color scrollbarThumbActive = alpha(textPrimary, dark ? 0.58 : 0.48);
        Color overlayBackground = blend(headerBackground, background, dark ? 0.62 : 0.48);
        Color overlayControlBackground = blend(headerBackgroundActive, background, dark ? 0.24 : 0.08);
        Color overlayControlBorder = alpha(border, dark ? 0.88 : 0.72);
        Color overlaySecondaryText = blend(textPrimary, background, dark ? 0.36 : 0.48);
        Color gutterBackground = blend(background, headerBackground, dark ? 0.16 : 0.10);
        Color shadow = UiCommonThemeSupport.shadowColor(resolved, 0.25, 0.18);

        return new CodeEditorTheme(
            background,
            textPrimary,
            base.keywordColor(),
            base.stringColor(),
            base.jsonKeyColor(),
            base.yamlKeyColor(),
            base.yamlAnchorColor(),
            base.yamlTagColor(),
            base.commentColor(),
            base.numberColor(),
            textActive,
            selection,
            lineNumber,
            textPrimary,
            base.booleanColor(),
            base.nullLiteralColor(),
            base.headlineColor(),
            base.listItemColor(),
            base.codeBlockColor(),
            currentLine,
            searchHighlight,
            accent,
            gutterBackground,
            base.markerErrorColor(),
            base.markerWarningColor(),
            base.markerInfoColor(),
            base.markerBreakpointColor(),
            accent,
            scrollbarTrack,
            scrollbarThumb,
            scrollbarThumbHover,
            scrollbarThumbActive,
            overlayBackground,
            accent,
            overlayControlBackground,
            overlayControlBorder,
            textPrimary,
            overlaySecondaryText,
            border,
            hover,
            pressed,
            accent,
            overlaySecondaryText,
            base.searchOverlayNoResultsBorder(),
            shadow,
            accent,
            base.searchOverlayErrorBackground(),
            base.syntaxScopeColors()
        );
    }

    /**
     * Returns {@code true} if the paint is considered dark.
     *
     * @param paint paint to evaluate
     * @return {@code true} when brightness is below the dark threshold
     */
    static boolean isDark(Paint paint) {
        if (paint instanceof Color c) {
            return c.getBrightness() < DARK_THRESHOLD;
        }
        return true;
    }

    private static Color asColor(Paint paint, Color fallback) {
        if (paint instanceof Color color) {
            return color;
        }
        return fallback;
    }

    private static Color blend(Color base, Color mix, double weight) {
        return base.interpolate(mix, clamp(weight));
    }

    private static Color alpha(Color color, double opacity) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), clamp(opacity));
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
