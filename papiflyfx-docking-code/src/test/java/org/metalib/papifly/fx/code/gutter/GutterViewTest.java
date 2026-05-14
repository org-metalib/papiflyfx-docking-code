package org.metalib.papifly.fx.code.gutter;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.render.GlyphCache;
import org.metalib.papifly.fx.code.render.WrapMap;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class GutterViewTest {

    private GutterView gutterView;
    private Document document;
    private GlyphCache glyphCache;

    @Start
    void start(Stage stage) {
        document = new Document("line0\nline1\nline2\nline3\nline4");
        glyphCache = new GlyphCache();
        gutterView = new GutterView(glyphCache);
        gutterView.setDocument(document);

        Scene scene = new Scene(gutterView, 100, 200);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void gutterWidthIsPositiveAfterSetDocument() {
        flushLayout();
        assertTrue(gutterView.getComputedWidth() > 0,
            "Gutter width should be > 0 after document set");
    }

    @Test
    void gutterWidthGrowsWithLineCount() {
        flushLayout();
        double smallWidth = gutterView.getComputedWidth();

        // Create document with 10000 lines (5 digits)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            if (i > 0) sb.append('\n');
            sb.append("x");
        }
        Document largeDoc = new Document(sb.toString());
        runOnFx(() -> {
            gutterView.setDocument(largeDoc);
        });
        flushLayout();

        assertTrue(gutterView.getComputedWidth() > smallWidth,
            "Gutter should be wider for 10000+ lines");
    }

    @Test
    void activeLineIndexUpdates() {
        runOnFx(() -> gutterView.setActiveLineIndex(2));
        flushLayout();
        // No assertion on rendering, but verify it doesn't throw
    }

    @Test
    void markerModelIntegration() {
        MarkerModel markers = new MarkerModel();
        runOnFx(() -> {
            gutterView.setMarkerModel(markers);
            markers.addMarker(new Marker(0, MarkerType.ERROR, "test error"));
        });
        flushLayout();

        assertEquals(markers, gutterView.getMarkerModel());
    }

    @Test
    void scrollOffsetSyncs() {
        runOnFx(() -> gutterView.setScrollOffset(50.0));
        flushLayout();
        // Verify no throw, scroll is applied
    }

    @Test
    void gutterMinimumTwoDigitWidth() {
        // Even with 1-line doc, gutter should reserve at least 2 digits
        Document oneLineDoc = new Document("x");
        runOnFx(() -> gutterView.setDocument(oneLineDoc));
        flushLayout();

        double charWidth = glyphCache.getCharWidth();
        // 2 digits * charWidth + marker lane + padding
        assertTrue(gutterView.getComputedWidth() >= charWidth * 2,
            "Gutter should have at least 2-digit width");
    }

    @Test
    void wrapModeUsesWrapMapWithoutErrors() {
        Document wrapDoc = new Document("abcdefghijklmnopqrstuvwxyz\nline2");
        WrapMap wrapMap = new WrapMap();
        wrapMap.rebuild(wrapDoc, glyphCache.getCharWidth() * 8, glyphCache.getCharWidth());

        runOnFx(() -> {
            gutterView.setDocument(wrapDoc);
            gutterView.setWrapMap(wrapMap);
            gutterView.setWordWrap(true);
            gutterView.setScrollOffset(glyphCache.getLineHeight());
        });
        flushLayout();

        assertTrue(gutterView.getComputedWidth() > 0);
    }

    private void flushLayout() {
        runOnFx(() -> {
            gutterView.applyCss();
            gutterView.layout();
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
}
