package org.metalib.papifly.fx.code.api;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.search.SearchController;
import org.metalib.papifly.fx.code.search.SearchModel;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class CodeEditorSearchFlowTest {

    private CodeEditor editor;
    private CountingSearchModel searchModel;
    private SearchController searchController;

    @Start
    void start(Stage stage) {
        searchModel = new CountingSearchModel();
        searchController = new SearchController(searchModel);
        editor = new CodeEditor(
            new Document(),
            searchModel,
            searchController,
            new GoToLineController(),
            null,
            null
        );
        stage.setScene(new Scene(editor, 640, 480));
        stage.show();
    }

    @Test
    void replaceCurrentPerformsSingleSearchScan() {
        runOnFx(() -> {
            editor.setText("foo foo foo");
            searchModel.setQuery("foo");
            searchModel.setReplacement("bar");
            invokeSearchController("executeSearch");
        });
        WaitForAsyncUtils.waitForFxEvents();
        int before = searchModel.searchInvocations;

        runOnFx(() -> invokeSearchController("replaceCurrent"));
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(before + 1, searchModel.searchInvocations);
    }

    @Test
    void replaceAllPerformsSingleSearchScan() {
        runOnFx(() -> {
            editor.setText("foo foo foo");
            searchModel.setQuery("foo");
            searchModel.setReplacement("bar");
            invokeSearchController("executeSearch");
        });
        WaitForAsyncUtils.waitForFxEvents();
        int before = searchModel.searchInvocations;

        runOnFx(() -> invokeSearchController("replaceAll"));
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(before + 1, searchModel.searchInvocations);
    }

    private void invokeSearchController(String methodName) {
        try {
            Method method = SearchController.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(searchController);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Failed to invoke SearchController." + methodName, exception);
        }
    }

    private static void runOnFx(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<RuntimeException> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (RuntimeException exception) {
                error.set(exception);
            } finally {
                latch.countDown();
            }
        });
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for FX action");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(exception);
        }
        if (error.get() != null) {
            throw error.get();
        }
    }

    private static final class CountingSearchModel extends SearchModel {
        private int searchInvocations;

        @Override
        public int search(Document document) {
            searchInvocations++;
            return super.search(document);
        }
    }
}

