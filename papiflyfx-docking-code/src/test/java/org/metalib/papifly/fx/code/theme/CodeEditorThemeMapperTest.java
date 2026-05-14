package org.metalib.papifly.fx.code.theme;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.docking.api.ThemeColors;
import org.metalib.papifly.fx.docking.api.ThemeDimensions;
import org.metalib.papifly.fx.docking.api.ThemeFonts;

import static org.junit.jupiter.api.Assertions.*;

class CodeEditorThemeMapperTest {

    @Test
    void mapNullReturnsDefaultDark() {
        CodeEditorTheme result = CodeEditorThemeMapper.map(null);
        assertNotNull(result);
        assertEquals(CodeEditorTheme.dark(), result);
    }

    @Test
    void mapDarkThemeProducesDarkPalette() {
        CodeEditorTheme result = CodeEditorThemeMapper.map(Theme.dark());
        assertNotNull(result);
        // Background comes from the base theme
        assertEquals(Theme.dark().background(), result.editorBackground());
        // Gutter now uses a subtle panel variant rather than a flat clone of the editor canvas.
        Color gutter = (Color) result.gutterBackground();
        assertTrue(gutter.getBrightness() > 0.10 && gutter.getBrightness() < 0.20);
        // Accent flows to bookmark and search accent border
        assertEquals(Theme.dark().accentColor(), result.markerBookmarkColor());
        assertEquals(Theme.dark().accentColor(), result.searchOverlayAccentBorder());
        assertEquals(Theme.dark().accentColor(), result.searchOverlayControlFocusedBorder());
        assertEquals(Theme.dark().accentColor(), result.searchOverlayIntegratedToggleActive());
    }

    @Test
    void mapLightThemeProducesLightPalette() {
        CodeEditorTheme result = CodeEditorThemeMapper.map(Theme.light());
        assertNotNull(result);
        assertEquals(Theme.light().background(), result.editorBackground());
        // Light palette foreground should be dark
        Color fg = (Color) result.editorForeground();
        assertTrue(fg.getBrightness() < 0.3, "Light theme foreground should be dark");
    }

    @Test
    void isDarkDetectsDarkColors() {
        assertTrue(CodeEditorThemeMapper.isDark(Color.BLACK));
        assertTrue(CodeEditorThemeMapper.isDark(Color.web("#1e1e1e")));
        assertFalse(CodeEditorThemeMapper.isDark(Color.WHITE));
        assertFalse(CodeEditorThemeMapper.isDark(Color.web("#f0f0f0")));
    }

    @Test
    void isDarkHandlesNonColorPaint() {
        // Non-Color paint defaults to dark
        assertTrue(CodeEditorThemeMapper.isDark(javafx.scene.paint.LinearGradient.valueOf(
            "from 0% 0% to 100% 100%, white 0%, black 100%")));
    }

    @Test
    void darkAndLightThemesHaveDifferentForeground() {
        CodeEditorTheme dark = CodeEditorThemeMapper.map(Theme.dark());
        CodeEditorTheme light = CodeEditorThemeMapper.map(Theme.light());
        assertNotEquals(dark.editorForeground(), light.editorForeground());
    }

    @Test
    void darkPaletteFactoryRoundTrips() {
        CodeEditorTheme dark = CodeEditorTheme.dark();
        assertNotNull(dark.editorBackground());
        assertNotNull(dark.keywordColor());
        assertNotNull(dark.stringColor());
        assertNotNull(dark.jsonKeyColor());
        assertNotNull(dark.yamlKeyColor());
        assertNotNull(dark.yamlAnchorColor());
        assertNotNull(dark.yamlTagColor());
        assertNotNull(dark.commentColor());
        assertNotNull(dark.numberColor());
        assertNotNull(dark.caretColor());
        assertNotNull(dark.selectionColor());
        assertNotNull(dark.lineNumberColor());
        assertNotNull(dark.lineNumberActiveColor());
        assertNotNull(dark.scrollbarTrackColor());
        assertNotNull(dark.scrollbarThumbColor());
        assertNotNull(dark.scrollbarThumbHoverColor());
        assertNotNull(dark.scrollbarThumbActiveColor());
    }

    @Test
    void lightPaletteFactoryRoundTrips() {
        CodeEditorTheme light = CodeEditorTheme.light();
        assertNotNull(light.editorBackground());
        assertNotNull(light.keywordColor());
        assertNotNull(light.stringColor());
        assertNotNull(light.jsonKeyColor());
        assertNotNull(light.yamlKeyColor());
        assertNotNull(light.yamlAnchorColor());
        assertNotNull(light.yamlTagColor());
        assertNotNull(light.commentColor());
        assertNotNull(light.numberColor());
        assertNotNull(light.caretColor());
        assertNotNull(light.selectionColor());
        assertNotNull(light.lineNumberColor());
        assertNotNull(light.lineNumberActiveColor());
        assertNotNull(light.scrollbarTrackColor());
        assertNotNull(light.scrollbarThumbColor());
        assertNotNull(light.scrollbarThumbHoverColor());
        assertNotNull(light.scrollbarThumbActiveColor());
    }

    @Test
    void paletteFactoriesIncludeDiscoveredSyntaxScopeDefaults() {
        assertEquals(Color.web("#123456"),
            CodeEditorTheme.dark().syntaxScopeColor(TestSyntaxStyleProvider.TEST_SCOPE).orElseThrow());
        assertEquals(Color.web("#654321"),
            CodeEditorTheme.light().syntaxScopeColor(TestSyntaxStyleProvider.TEST_SCOPE).orElseThrow());
        assertEquals(Color.web("#123456"),
            CodeEditorThemeMapper.map(Theme.dark()).syntaxScopeColor(TestSyntaxStyleProvider.TEST_SCOPE).orElseThrow());
    }

    @Test
    void defaultStructuralKeyColorsStayDistinctFromStringValues() {
        CodeEditorTheme dark = CodeEditorTheme.dark();
        assertEquals(Color.web("#9cdcfe"), dark.jsonKeyColor());
        assertEquals(Color.web("#4ec9b0"), dark.yamlKeyColor());
        assertEquals(Color.web("#d7ba7d"), dark.yamlAnchorColor());
        assertEquals(Color.web("#c586c0"), dark.yamlTagColor());
        assertNotEquals(dark.stringColor(), dark.jsonKeyColor());
        assertNotEquals(dark.jsonKeyColor(), dark.yamlKeyColor());
        assertNotEquals(dark.editorForeground(), dark.yamlAnchorColor());
        assertNotEquals(dark.editorForeground(), dark.yamlTagColor());

        CodeEditorTheme light = CodeEditorTheme.light();
        assertEquals(Color.web("#0451a5"), light.jsonKeyColor());
        assertEquals(Color.web("#267f99"), light.yamlKeyColor());
        assertEquals(Color.web("#795e26"), light.yamlAnchorColor());
        assertEquals(Color.web("#af00db"), light.yamlTagColor());
        assertNotEquals(light.stringColor(), light.jsonKeyColor());
        assertNotEquals(light.jsonKeyColor(), light.yamlKeyColor());
        assertNotEquals(light.editorForeground(), light.yamlAnchorColor());
        assertNotEquals(light.editorForeground(), light.yamlTagColor());
    }

    @Test
    void customAccentColorPropagates() {
        Theme custom = Theme.of(
            new ThemeColors(
                Color.rgb(30, 30, 30),
                Color.rgb(45, 45, 45),
                Color.rgb(60, 60, 60),
                Color.RED,
                Color.rgb(200, 200, 200),
                Color.WHITE,
                Color.rgb(60, 60, 60),
                Color.rgb(80, 80, 80),
                Color.rgb(0, 122, 204, 0.3),
                Color.rgb(70, 70, 70),
                Color.rgb(90, 90, 90),
                Color.rgb(40, 40, 40)
            ),
            new ThemeFonts(
                javafx.scene.text.Font.font(12),
                javafx.scene.text.Font.font(12)
            ),
            new ThemeDimensions(
                4.0,
                1.0,
                28.0,
                24.0,
                javafx.geometry.Insets.EMPTY,
                8.0,
                24.0
            )
        );
        CodeEditorTheme result = CodeEditorThemeMapper.map(custom);
        assertEquals(Color.RED, result.markerBookmarkColor());
        assertEquals(Color.RED, result.searchOverlayAccentBorder());
        assertEquals(Color.RED, result.searchOverlayControlFocusedBorder());
        assertEquals(Color.RED, result.searchOverlayIntegratedToggleActive());
    }
}
