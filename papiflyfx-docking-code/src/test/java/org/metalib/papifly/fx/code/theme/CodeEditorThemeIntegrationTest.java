package org.metalib.papifly.fx.code.theme;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.code.api.CodeEditor;
import org.metalib.papifly.fx.code.api.GoToLineController;
import org.metalib.papifly.fx.code.search.SearchController;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.ui.UiMetrics;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
class CodeEditorThemeIntegrationTest {

    private CodeEditor editor;
    private ObjectProperty<Theme> themeProperty;

    @Start
    void start(Stage stage) {
        editor = new CodeEditor();
        editor.setText("hello world");
        themeProperty = new SimpleObjectProperty<>(Theme.dark());

        Scene scene = new Scene(editor, 400, 300);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void bindThemePropertyAppliesDarkTheme() {
        runOnFx(() -> editor.bindThemeProperty(themeProperty));
        WaitForAsyncUtils.waitForFxEvents();

        CodeEditorTheme editorTheme = editor.getEditorTheme();
        assertNotNull(editorTheme);
        assertEquals(Theme.dark().background(), editorTheme.editorBackground());
    }

    @Test
    void switchToLightThemeUpdatesEditor() {
        runOnFx(() -> editor.bindThemeProperty(themeProperty));
        WaitForAsyncUtils.waitForFxEvents();

        runOnFx(() -> themeProperty.set(Theme.light()));
        WaitForAsyncUtils.waitForFxEvents();

        CodeEditorTheme editorTheme = editor.getEditorTheme();
        assertEquals(Theme.light().background(), editorTheme.editorBackground());
        // Foreground should be dark for light theme
        Color fg = (Color) editorTheme.editorForeground();
        assertTrue(fg.getBrightness() < 0.3);
    }

    @Test
    void unbindStopsUpdates() {
        runOnFx(() -> editor.bindThemeProperty(themeProperty));
        WaitForAsyncUtils.waitForFxEvents();

        runOnFx(() -> editor.unbindThemeProperty());
        runOnFx(() -> themeProperty.set(Theme.light()));
        WaitForAsyncUtils.waitForFxEvents();

        // After unbinding, editor should still have the dark background from before unbind
        CodeEditorTheme editorTheme = editor.getEditorTheme();
        assertEquals(Theme.dark().background(), editorTheme.editorBackground());
    }

    @Test
    void setEditorThemeDirectly() {
        CodeEditorTheme light = CodeEditorTheme.light();
        runOnFx(() -> editor.setEditorTheme(light));
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(light, editor.getEditorTheme());
        assertEquals(light, editor.getViewport().getTheme());
        assertEquals(light, editor.getGutterView().getTheme());
        assertEquals(light, editor.getSearchController().getTheme());
        assertEquals(light, editor.getGoToLineController().getTheme());
    }

    @Test
    void setNullEditorThemeDefaultsToDark() {
        runOnFx(() -> editor.setEditorTheme(null));
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(CodeEditorTheme.dark(), editor.getEditorTheme());
    }

    @Test
    void disposeUnbindsTheme() {
        runOnFx(() -> editor.bindThemeProperty(themeProperty));
        WaitForAsyncUtils.waitForFxEvents();

        runOnFx(() -> editor.dispose());
        // Should not throw when theme changes after dispose
        runOnFx(() -> themeProperty.set(Theme.light()));
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void viewportGutterSearchAllReceiveTheme() {
        runOnFx(() -> editor.bindThemeProperty(themeProperty));
        WaitForAsyncUtils.waitForFxEvents();

        CodeEditorTheme expected = CodeEditorThemeMapper.map(Theme.dark());
        assertEquals(expected, editor.getViewport().getTheme());
        assertEquals(expected, editor.getGutterView().getTheme());
        assertEquals(expected, editor.getSearchController().getTheme());
        assertEquals(expected, editor.getGoToLineController().getTheme());
    }

    @Test
    void searchOverlayStyleVariablesUpdateAfterThemeSwitch() {
        runOnFx(() -> editor.bindThemeProperty(themeProperty));
        WaitForAsyncUtils.waitForFxEvents();

        String darkStyle = editor.getSearchController().getStyle();

        runOnFx(() -> themeProperty.set(Theme.light()));
        WaitForAsyncUtils.waitForFxEvents();

        String lightStyle = editor.getSearchController().getStyle();
        assertNotEquals(darkStyle, lightStyle);
        assertTrue(lightStyle.contains("-pf-ui-surface-overlay"));
        assertTrue(lightStyle.contains("-pf-ui-accent"));
    }

    @Test
    void searchAndGoToLineUseSharedCompactMetrics() {
        runOnFx(() -> {
            editor.bindThemeProperty(themeProperty);
            editor.openSearch();
            editor.getGoToLineController().open(3, 120);
            editor.applyCss();
            editor.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        SearchController search = editor.getSearchController();
        GoToLineController goToLine = editor.getGoToLineController();
        TextField searchField = runOnFxAndGet(() -> (TextField) search.lookup(".pf-search-field"));
        TextField goToField = runOnFxAndGet(() -> (TextField) goToLine.lookup(".pf-goto-field"));
        Button searchIconButton = runOnFxAndGet(() -> (Button) search.lookup(".pf-search-icon-button"));

        Insets searchPadding = runOnFxAndGet(search::getPadding);
        Insets goToPadding = runOnFxAndGet(goToLine::getPadding);
        Insets searchMargin = runOnFxAndGet(() -> javafx.scene.layout.StackPane.getMargin(search));
        Insets goToMargin = runOnFxAndGet(() -> javafx.scene.layout.StackPane.getMargin(goToLine));

        assertInsetsEquals(new Insets(UiMetrics.SPACE_3, UiMetrics.SPACE_3, UiMetrics.SPACE_3, UiMetrics.SPACE_3), searchPadding);
        assertInsetsEquals(new Insets(UiMetrics.SPACE_3, UiMetrics.SPACE_3, UiMetrics.SPACE_3, UiMetrics.SPACE_3), goToPadding);
        assertEquals(UiMetrics.SPACE_2, runOnFxAndGet(search::getSpacing), 0.01);
        assertEquals(UiMetrics.SPACE_2, runOnFxAndGet(goToLine::getSpacing), 0.01);
        assertEquals(UiMetrics.CONTROL_HEIGHT_COMPACT, runOnFxAndGet(searchField::getHeight), 1.0);
        assertEquals(UiMetrics.CONTROL_HEIGHT_COMPACT, runOnFxAndGet(goToField::getHeight), 1.0);
        assertEquals(UiMetrics.ICON_BUTTON_SIZE_COMPACT, runOnFxAndGet(searchIconButton::getWidth), 1.0);
        assertInsetsEquals(new Insets(0.0, UiMetrics.SPACE_4, 0.0, 0.0), searchMargin);
        assertInsetsEquals(new Insets(UiMetrics.SPACE_2, UiMetrics.SPACE_4, 0.0, 0.0), goToMargin);
    }

    @Test
    void selectedSearchChipUsesThemeSelectedColor() {
        runOnFx(() -> {
            editor.bindThemeProperty(themeProperty);
            editor.openSearch();
            editor.applyCss();
            editor.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        ToggleButton toggle = runOnFxAndGet(() -> (ToggleButton) editor.getSearchController().lookup(".pf-search-chip"));
        Color defaultBackground = runOnFxAndGet(() -> backgroundColor(toggle));

        runOnFx(() -> {
            toggle.setSelected(true);
            toggle.applyCss();
            toggle.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        Color selectedBackground = runOnFxAndGet(() -> backgroundColor(toggle));
        Color expectedSelected = runOnFxAndGet(() ->
            {
                Color accent = (Color) editor.getEditorTheme().searchOverlayAccentBorder();
                return new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.16);
            });

        assertNotNull(defaultBackground);
        assertNotNull(selectedBackground);
        assertFalse(colorsClose(defaultBackground, selectedBackground, 0.01));
        assertTrue(colorsClose(expectedSelected, selectedBackground, 0.03));
    }

    private static void assertInsetsEquals(Insets expected, Insets actual) {
        assertEquals(expected.getTop(), actual.getTop(), 0.01);
        assertEquals(expected.getRight(), actual.getRight(), 0.01);
        assertEquals(expected.getBottom(), actual.getBottom(), 0.01);
        assertEquals(expected.getLeft(), actual.getLeft(), 0.01);
    }

    private static Color backgroundColor(Region region) {
        Background background = region.getBackground();
        if (background == null || background.getFills().isEmpty()) {
            return null;
        }
        if (background.getFills().getFirst().getFill() instanceof Color color) {
            return color;
        }
        return null;
    }

    private static boolean colorsClose(Color expected, Color actual, double tolerance) {
        return Math.abs(expected.getRed() - actual.getRed()) <= tolerance
            && Math.abs(expected.getGreen() - actual.getGreen()) <= tolerance
            && Math.abs(expected.getBlue() - actual.getBlue()) <= tolerance
            && Math.abs(expected.getOpacity() - actual.getOpacity()) <= tolerance;
    }

    private <T> T runOnFxAndGet(java.util.concurrent.Callable<T> action) {
        if (javafx.application.Platform.isFxApplicationThread()) {
            try {
                return action.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        final Object[] result = new Object[1];
        final RuntimeException[] error = new RuntimeException[1];
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        javafx.application.Platform.runLater(() -> {
            try {
                result[0] = action.call();
            } catch (Exception e) {
                error[0] = new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        if (error[0] != null) {
            throw error[0];
        }
        @SuppressWarnings("unchecked")
        T typed = (T) result[0];
        return typed;
    }

    private void runOnFx(Runnable action) {
        if (javafx.application.Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        javafx.application.Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
