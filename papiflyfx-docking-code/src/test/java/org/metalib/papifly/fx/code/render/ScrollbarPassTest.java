package org.metalib.papifly.fx.code.render;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.code.document.Document;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class ScrollbarPassTest {

    private Viewport viewport;
    private Document document;

    @Start
    void start(Stage stage) {
        document = new Document("line0");
        viewport = new Viewport(new SelectionModel());
        viewport.setDocument(document);
        Scene scene = new Scene(viewport, 320, 180);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void verticalScrollbarVisibleForTallContent() {
        runOnFx(() -> document.setText(buildLines(600, 12)));
        flushLayout();

        assertTrue(callOnFx(viewport::isVerticalScrollbarVisible));
        assertNotNull(callOnFx(viewport::getVerticalScrollbarGeometry));
    }

    @Test
    void wordWrapHidesHorizontalScrollbarAndResetsHorizontalOffset() {
        runOnFx(() -> {
            document.setText("x".repeat(1_000));
            viewport.setHorizontalScrollOffset(240.0);
        });
        flushLayout();
        assertTrue(callOnFx(viewport::isHorizontalScrollbarVisible));
        assertTrue(callOnFx(() -> viewport.getHorizontalScrollOffset() > 0.0));

        runOnFx(() -> viewport.setWordWrap(true));
        flushLayout();

        assertFalse(callOnFx(viewport::isHorizontalScrollbarVisible));
        assertEquals(0.0, callOnFx(viewport::getHorizontalScrollOffset), 0.0001);
    }

    @Test
    void verticalThumbRespectsMinimumSize() {
        runOnFx(() -> document.setText(buildLines(4_000, 4)));
        flushLayout();

        Viewport.ScrollbarGeometry geometry = callOnFx(viewport::getVerticalScrollbarGeometry);
        assertNotNull(geometry);
        assertTrue(geometry.thumbHeight() >= Viewport.MIN_THUMB_SIZE - 0.0001);
        assertTrue(geometry.trackHeight() > 0.0);
    }

    private String buildLines(int count, int width) {
        String text = "x".repeat(width);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(text);
        }
        return builder.toString();
    }

    private void flushLayout() {
        runOnFx(() -> {
            viewport.applyCss();
            viewport.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();
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
    private <T> T callOnFx(Callable<T> action) {
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
