package org.metalib.papifly.fx.code.api;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.code.command.CaretRange;
import org.metalib.papifly.fx.code.render.GlyphCache;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class MouseGestureTest {

    private CodeEditor editor;
    private Stage stage;
    private double lineHeight;
    private double charWidth;

    @Start
    void start(Stage stage) {
        this.stage = stage;
        editor = new CodeEditor();
        Scene scene = new Scene(editor, 640, 480);
        stage.setScene(scene);
        stage.show();
    }

    private void initMetrics() {
        GlyphCache gc = editor.getViewport().getGlyphCache();
        lineHeight = gc.getLineHeight();
        charWidth = gc.getCharWidth();
    }

    // --- Double-click word selection ---

    @Test
    void doubleClickSelectsWord() {
        runOnFx(() -> {
            editor.setText("hello world foo");
            editor.applyCss();
            editor.layout();
            initMetrics();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Double-click on "world" (line 0, col ~7 -> middle of "world")
        double x = charWidth * 7;
        double y = lineHeight * 0.5;
        fireMousePressed(x, y, MouseButton.PRIMARY, 2, false, false);
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(callOnFx(() -> editor.getSelectionModel().hasSelection()));
        assertEquals("world", callOnFx(() ->
            editor.getSelectionModel().getSelectedText(editor.getDocument())));
    }

    @Test
    void doubleClickSelectsWordInOffsetContainer() {
        runOnFx(() -> {
            editor.setText("hello world foo");
            installOffsetContainer(52, 110);
            initMetrics();
        });
        WaitForAsyncUtils.waitForFxEvents();

        double viewportX = charWidth * 7;
        double viewportY = lineHeight * 0.5;
        fireMousePressed(viewportX, viewportY, MouseButton.PRIMARY, 2, false, false);
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(callOnFx(() -> editor.getSelectionModel().hasSelection()));
        assertEquals("world", callOnFx(() ->
            editor.getSelectionModel().getSelectedText(editor.getDocument())));
    }

    @Test
    void doubleClickOnEmptyLineDoesNotCrash() {
        runOnFx(() -> {
            editor.setText("hello\n\nworld");
            editor.applyCss();
            editor.layout();
            initMetrics();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Double-click on empty line 1
        double x = charWidth * 0;
        double y = lineHeight * 1.5;
        fireMousePressed(x, y, MouseButton.PRIMARY, 2, false, false);
        WaitForAsyncUtils.waitForFxEvents();

        // Should not crash; caret on line 1
        assertEquals(1, callOnFx(() -> editor.getSelectionModel().getCaretLine()));
    }

    // --- Triple-click line selection ---

    @Test
    void tripleClickSelectsLine() {
        runOnFx(() -> {
            editor.setText("first line\nsecond line\nthird line");
            editor.applyCss();
            editor.layout();
            initMetrics();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Triple-click on line 1 ("second line")
        double x = charWidth * 3;
        double y = lineHeight * 1.5;
        fireMousePressed(x, y, MouseButton.PRIMARY, 3, false, false);
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(callOnFx(() -> editor.getSelectionModel().hasSelection()));
        assertEquals("second line", callOnFx(() ->
            editor.getSelectionModel().getSelectedText(editor.getDocument())));
    }

    // --- Alt+Click adds secondary caret ---

    @Test
    void altClickAddsSecondaryCaret() {
        runOnFx(() -> {
            editor.setText("aaa\nbbb\nccc");
            editor.applyCss();
            editor.layout();
            initMetrics();
            // Place primary caret at (0, 1)
            editor.getSelectionModel().moveCaret(0, 1);
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Alt+Click on line 2, col 2
        double x = charWidth * 2;
        double y = lineHeight * 2.5;
        fireMousePressed(x, y, MouseButton.PRIMARY, 1, false, true);
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(callOnFx(() -> editor.getMultiCaretModel().hasMultipleCarets()));
        List<CaretRange> allCarets = callOnFx(() ->
            editor.getMultiCaretModel().allCarets(editor.getDocument()));
        assertEquals(2, allCarets.size());
    }

    @Test
    void multipleAltClicksAddMultipleCarets() {
        runOnFx(() -> {
            editor.setText("aaa\nbbb\nccc");
            editor.applyCss();
            editor.layout();
            initMetrics();
            editor.getSelectionModel().moveCaret(0, 0);
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Alt+Click on line 1
        fireMousePressed(charWidth * 1, lineHeight * 1.5, MouseButton.PRIMARY, 1, false, true);
        WaitForAsyncUtils.waitForFxEvents();

        // Alt+Click on line 2
        fireMousePressed(charWidth * 1, lineHeight * 2.5, MouseButton.PRIMARY, 1, false, true);
        WaitForAsyncUtils.waitForFxEvents();

        List<CaretRange> allCarets = callOnFx(() ->
            editor.getMultiCaretModel().allCarets(editor.getDocument()));
        assertEquals(3, allCarets.size());
    }

    // --- Normal click collapses multi-caret ---

    @Test
    void normalClickCollapsesMultiCaret() {
        runOnFx(() -> {
            editor.setText("aaa\nbbb\nccc");
            editor.applyCss();
            editor.layout();
            initMetrics();
            editor.getSelectionModel().moveCaret(0, 0);
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Alt+Click to add a secondary caret
        fireMousePressed(charWidth * 1, lineHeight * 1.5, MouseButton.PRIMARY, 1, false, true);
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(callOnFx(() -> editor.getMultiCaretModel().hasMultipleCarets()));

        // Normal click should collapse
        fireMousePressed(charWidth * 0, lineHeight * 0.5, MouseButton.PRIMARY, 1, false, false);
        WaitForAsyncUtils.waitForFxEvents();

        assertFalse(callOnFx(() -> editor.getMultiCaretModel().hasMultipleCarets()));
    }

    // --- Box selection via Shift+Alt+Drag ---

    @Test
    void boxSelectionCreatesMultipleCarets() {
        runOnFx(() -> {
            editor.setText("abcdef\nghijkl\nmnopqr\nstuvwx");
            editor.applyCss();
            editor.layout();
            initMetrics();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Shift+Alt+Click at (0, 1) to start box selection
        double startX = charWidth * 1;
        double startY = lineHeight * 0.5;
        fireMousePressed(startX, startY, MouseButton.PRIMARY, 1, true, true);
        WaitForAsyncUtils.waitForFxEvents();

        // Drag to (2, 4) — should create carets on lines 0, 1, 2
        double endX = charWidth * 4;
        double endY = lineHeight * 2.5;
        fireMouseDragged(endX, endY);
        WaitForAsyncUtils.waitForFxEvents();

        // Release
        fireMouseReleased(endX, endY);
        WaitForAsyncUtils.waitForFxEvents();

        List<CaretRange> allCarets = callOnFx(() ->
            editor.getMultiCaretModel().allCarets(editor.getDocument()));
        assertEquals(3, allCarets.size());

        // Each caret should span columns 1-4
        for (CaretRange caret : allCarets) {
            assertEquals(1, caret.getStartColumn());
            assertEquals(4, caret.getEndColumn());
        }
    }

    @Test
    void boxSelectionUsesViewportCoordinatesInOffsetContainer() {
        runOnFx(() -> {
            editor.setText("abcdef\nghijkl\nmnopqr\nstuvwx");
            installOffsetContainer(48, 96);
            initMetrics();
        });
        WaitForAsyncUtils.waitForFxEvents();

        fireMousePressed(charWidth, lineHeight * 0.5, MouseButton.PRIMARY, 1, true, true);
        WaitForAsyncUtils.waitForFxEvents();

        fireMouseDragged(charWidth * 4, lineHeight * 2.5);
        WaitForAsyncUtils.waitForFxEvents();

        fireMouseReleased(charWidth * 4, lineHeight * 2.5);
        WaitForAsyncUtils.waitForFxEvents();

        List<CaretRange> allCarets = callOnFx(() ->
            editor.getMultiCaretModel().allCarets(editor.getDocument()));
        assertEquals(3, allCarets.size());
        for (CaretRange caret : allCarets) {
            assertEquals(1, caret.getStartColumn());
            assertEquals(4, caret.getEndColumn());
        }
    }

    @Test
    void boxSelectionClampsToLineLength() {
        runOnFx(() -> {
            editor.setText("abcdefgh\nab\nabcdef");
            editor.applyCss();
            editor.layout();
            initMetrics();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Start box at (0, 1)
        fireMousePressed(charWidth * 1, lineHeight * 0.5, MouseButton.PRIMARY, 1, true, true);
        WaitForAsyncUtils.waitForFxEvents();

        // Drag to (2, 6) — line 1 "ab" only has length 2, so should clamp
        fireMouseDragged(charWidth * 6, lineHeight * 2.5);
        WaitForAsyncUtils.waitForFxEvents();

        fireMouseReleased(charWidth * 6, lineHeight * 2.5);
        WaitForAsyncUtils.waitForFxEvents();

        List<CaretRange> allCarets = callOnFx(() ->
            editor.getMultiCaretModel().allCarets(editor.getDocument()));
        assertEquals(3, allCarets.size());

        // Line 1 ("ab") should have caret clamped: start=1, end=2
        CaretRange line1Caret = allCarets.stream()
            .filter(c -> c.caretLine() == 1)
            .findFirst()
            .orElseThrow();
        assertEquals(1, line1Caret.getStartColumn());
        assertEquals(2, line1Caret.getEndColumn());
    }

    @Test
    void boxSelectionGesturesAreDisabledInWrapMode() {
        runOnFx(() -> {
            editor.setText("abcdef\nghijkl\nmnopqr");
            editor.setWordWrap(true);
            editor.applyCss();
            editor.layout();
            initMetrics();
        });
        WaitForAsyncUtils.waitForFxEvents();

        fireMousePressed(charWidth, lineHeight * 0.5, MouseButton.PRIMARY, 1, true, true);
        WaitForAsyncUtils.waitForFxEvents();
        fireMouseDragged(charWidth * 4, lineHeight * 2.5);
        WaitForAsyncUtils.waitForFxEvents();
        fireMouseReleased(charWidth * 4, lineHeight * 2.5);
        WaitForAsyncUtils.waitForFxEvents();

        assertFalse(callOnFx(() -> editor.getMultiCaretModel().hasMultipleCarets()));
        assertTrue(callOnFx(() -> editor.getSelectionModel().hasSelection()));
    }

    // --- Middle-click box selection ---

    @Test
    void middleClickStartsBoxSelection() {
        runOnFx(() -> {
            editor.setText("abcdef\nghijkl\nmnopqr");
            editor.applyCss();
            editor.layout();
            initMetrics();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Middle-click at (0, 1)
        fireMousePressed(charWidth * 1, lineHeight * 0.5, MouseButton.MIDDLE, 1, false, false);
        WaitForAsyncUtils.waitForFxEvents();

        // Drag to (2, 4)
        fireMouseDragged(charWidth * 4, lineHeight * 2.5);
        WaitForAsyncUtils.waitForFxEvents();

        fireMouseReleased(charWidth * 4, lineHeight * 2.5);
        WaitForAsyncUtils.waitForFxEvents();

        List<CaretRange> allCarets = callOnFx(() ->
            editor.getMultiCaretModel().allCarets(editor.getDocument()));
        assertEquals(3, allCarets.size());
    }

    // --- Helpers ---

    private void fireMousePressed(double x, double y, MouseButton button, int clickCount,
                                   boolean shift, boolean alt) {
        runOnFx(() -> {
            Point2D scenePoint = toScenePoint(x, y);
            Point2D editorPoint = editor.sceneToLocal(scenePoint);
            editor.fireEvent(createMouseEvent(
                MouseEvent.MOUSE_PRESSED,
                scenePoint,
                editorPoint,
                button,
                clickCount,
                shift,
                alt,
                button == MouseButton.PRIMARY,
                button == MouseButton.MIDDLE,
                button == MouseButton.SECONDARY
            ));
        });
    }

    private void fireMouseDragged(double x, double y) {
        runOnFx(() -> {
            Point2D scenePoint = toScenePoint(x, y);
            Point2D editorPoint = editor.sceneToLocal(scenePoint);
            editor.fireEvent(createMouseEvent(
                MouseEvent.MOUSE_DRAGGED,
                scenePoint,
                editorPoint,
                MouseButton.PRIMARY,
                1,
                false,
                false,
                true,
                false,
                false
            ));
        });
    }

    private void fireMouseReleased(double x, double y) {
        runOnFx(() -> {
            Point2D scenePoint = toScenePoint(x, y);
            Point2D editorPoint = editor.sceneToLocal(scenePoint);
            editor.fireEvent(createMouseEvent(
                MouseEvent.MOUSE_RELEASED,
                scenePoint,
                editorPoint,
                MouseButton.PRIMARY,
                1,
                false,
                false,
                false,
                false,
                false
            ));
        });
    }

    private MouseEvent createMouseEvent(
        javafx.event.EventType<MouseEvent> eventType,
        Point2D scenePoint,
        Point2D editorPoint,
        MouseButton button,
        int clickCount,
        boolean shift,
        boolean alt,
        boolean primaryDown,
        boolean middleDown,
        boolean secondaryDown
    ) {
        Point2D screenPoint = editor.localToScreen(editorPoint);
        double screenX = screenPoint != null ? screenPoint.getX() : scenePoint.getX();
        double screenY = screenPoint != null ? screenPoint.getY() : scenePoint.getY();
        PickResult pickResult = new PickResult(editor, editorPoint.getX(), editorPoint.getY());
        return new MouseEvent(
            null,
            editor,
            eventType,
            scenePoint.getX(),
            scenePoint.getY(),
            screenX,
            screenY,
            button,
            clickCount,
            shift,
            false,
            alt,
            false,
            primaryDown,
            middleDown,
            secondaryDown,
            true,
            false,
            true,
            pickResult
        );
    }

    private Point2D toScenePoint(double viewportX, double viewportY) {
        Point2D scenePoint = editor.getViewport().localToScene(viewportX, viewportY);
        if (scenePoint == null) {
            throw new IllegalStateException("Unable to map viewport-local point to scene coordinates");
        }
        return scenePoint;
    }

    private void installOffsetContainer(double topHeight, double leftWidth) {
        BorderPane offsetContainer = new BorderPane();
        Region top = new Region();
        top.setPrefHeight(topHeight);
        Region left = new Region();
        left.setPrefWidth(leftWidth);
        offsetContainer.setTop(top);
        offsetContainer.setLeft(left);
        offsetContainer.setCenter(editor);
        stage.getScene().setRoot(offsetContainer);
        offsetContainer.applyCss();
        offsetContainer.layout();
        editor.applyCss();
        editor.layout();
    }

    private void runOnFx(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
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

    @SuppressWarnings("unchecked")
    private <T> T callOnFx(java.util.concurrent.Callable<T> action) {
        if (Platform.isFxApplicationThread()) {
            try {
                return action.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        CountDownLatch latch = new CountDownLatch(1);
        Object[] result = new Object[1];
        Platform.runLater(() -> {
            try {
                result[0] = action.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
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
        return (T) result[0];
    }
}
