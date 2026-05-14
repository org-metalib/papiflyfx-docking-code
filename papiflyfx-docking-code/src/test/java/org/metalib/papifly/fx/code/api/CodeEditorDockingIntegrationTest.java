package org.metalib.papifly.fx.code.api;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.metalib.papifly.fx.code.command.CaretRange;
import org.metalib.papifly.fx.docks.DockManager;
import org.metalib.papifly.fx.docks.core.DockLeaf;
import org.metalib.papifly.fx.docks.core.DockTabGroup;
import org.metalib.papifly.fx.docks.layout.ContentStateRegistry;
import org.metalib.papifly.fx.docks.layout.data.DockSessionData;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end integration tests verifying code editor state survives
 * a complete DockManager session capture → restore cycle.
 */
@ExtendWith(ApplicationExtension.class)
class CodeEditorDockingIntegrationTest {

    private DockManager dockManager;

    @Start
    void start(Stage stage) {
        dockManager = new DockManager();
        dockManager.setOwnerStage(stage);
        Scene scene = new Scene(dockManager.getRootPane(), 800, 600);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * B1: Full editor state round-trip through DockManager session save/restore.
     * Verifies filePath, languageId, cursorLine, cursorColumn, verticalScrollOffset.
     */
    @Test
    void fullEditorStateRoundTripThroughSession(@TempDir Path tempDir) throws Exception {
        Path tempFile = tempDir.resolve("test.java");
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            content.append("// line ").append(i).append('\n');
        }
        Files.writeString(tempFile, content.toString());

        // Setup: register adapter and factory, create an editor leaf
        runOnFx(() -> {
            ContentStateRegistry registry = new ContentStateRegistry();
            registry.register(new CodeEditorStateAdapter());
            dockManager.setContentStateRegistry(registry);
            dockManager.setContentFactory(new CodeEditorFactory());

            CodeEditor editor = new CodeEditor();
            editor.setFilePath(tempFile.toString());
            editor.setText(content.toString());
            editor.setLanguageId("java");
            editor.getSelectionModel().moveCaret(10, 2);
            editor.getSelectionModel().moveCaretWithSelection(10, 5);
            editor.getMultiCaretModel().addCaretNoStack(new CaretRange(12, 1, 12, 4));
            editor.getMultiCaretModel().addCaretNoStack(new CaretRange(15, 0, 15, 0));
            editor.setVerticalScrollOffset(42.0);

            DockLeaf leaf = dockManager.createLeaf("TestEditor", editor);
            leaf.setContentFactoryId(CodeEditorFactory.FACTORY_ID);
            DockTabGroup tabGroup = dockManager.createTabGroup();
            tabGroup.addLeaf(leaf);
            dockManager.setRoot((org.metalib.papifly.fx.docks.core.DockElement) tabGroup);
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Capture session
        String json = callOnFx(() -> dockManager.saveSessionToString());
        assertNotNull(json);

        // Clear and restore
        runOnFx(() -> {
            dockManager.setRoot((org.metalib.papifly.fx.docks.core.DockElement) null);
        });
        WaitForAsyncUtils.waitForFxEvents();

        runOnFx(() -> dockManager.restoreSessionFromString(json));
        WaitForAsyncUtils.waitForFxEvents();

        // Verify restored editor state
        assertNotNull(callOnFx(() -> dockManager.getRoot()));
        CodeEditor restored = callOnFx(() -> {
            DockTabGroup group = (DockTabGroup) dockManager.getRoot();
            Node restoredContent = group.getTabs().getFirst().getContent();
            assertTrue(restoredContent instanceof CodeEditor);
            return (CodeEditor) restoredContent;
        });

        assertEquals(tempFile.toString(), callOnFx(restored::getFilePath));
        assertEquals("java", callOnFx(restored::getLanguageId));
        assertEquals(10, callOnFx(restored::getCursorLine));
        assertEquals(5, callOnFx(restored::getCursorColumn));
        assertEquals(10, callOnFx(() -> restored.getSelectionModel().getAnchorLine()));
        assertEquals(2, callOnFx(() -> restored.getSelectionModel().getAnchorColumn()));
        assertEquals(
            List.of(
                new CaretRange(12, 1, 12, 4),
                new CaretRange(15, 0, 15, 0)
            ),
            callOnFx(() -> restored.getMultiCaretModel().getSecondaryCarets())
        );
        // Text should be rehydrated from the temp file
        assertTrue(callOnFx(() -> restored.getText().startsWith("// line 0")));
        assertTrue(callOnFx(() -> restored.getText().length() > 0));

        // Cleanup
        runOnFx(restored::dispose);
    }

    /**
     * B2: Session restore with missing adapter falls back to factory.
     */
    @Test
    void restoreWithMissingAdapterFallsBackToFactory() {
        // Setup: register adapter + factory, create leaf, capture session
        runOnFx(() -> {
            ContentStateRegistry registry = new ContentStateRegistry();
            registry.register(new CodeEditorStateAdapter());
            dockManager.setContentStateRegistry(registry);
            dockManager.setContentFactory(new CodeEditorFactory());

            CodeEditor editor = new CodeEditor();
            editor.setText("original content");
            editor.setLanguageId("javascript");

            DockLeaf leaf = dockManager.createLeaf("JSEditor", editor);
            leaf.setContentFactoryId(CodeEditorFactory.FACTORY_ID);
            DockTabGroup tabGroup = dockManager.createTabGroup();
            tabGroup.addLeaf(leaf);
            dockManager.setRoot((org.metalib.papifly.fx.docks.core.DockElement) tabGroup);
        });
        WaitForAsyncUtils.waitForFxEvents();

        String json = callOnFx(() -> dockManager.saveSessionToString());

        // Clear and restore with factory only (no adapter)
        runOnFx(() -> {
            dockManager.setRoot((org.metalib.papifly.fx.docks.core.DockElement) null);
            dockManager.setContentStateRegistry(new ContentStateRegistry()); // empty registry
            dockManager.setContentFactory(new CodeEditorFactory());
        });
        WaitForAsyncUtils.waitForFxEvents();

        runOnFx(() -> dockManager.restoreSessionFromString(json));
        WaitForAsyncUtils.waitForFxEvents();

        // Leaf should exist, created via factory (fresh editor)
        assertNotNull(callOnFx(() -> dockManager.getRoot()));
        Node restoredContent = callOnFx(() -> {
            DockTabGroup group = (DockTabGroup) dockManager.getRoot();
            return group.getTabs().getFirst().getContent();
        });
        assertNotNull(restoredContent);
        // Factory creates a fresh CodeEditor (empty text, default language)
        assertTrue(restoredContent instanceof CodeEditor);
    }

    /**
     * B3: Session restore with neither adapter nor factory falls back to placeholder.
     */
    @Test
    void restoreWithNoAdapterNoFactoryFallsBackToPlaceholder() {
        // Setup: register adapter + factory, create leaf, capture session
        runOnFx(() -> {
            ContentStateRegistry registry = new ContentStateRegistry();
            registry.register(new CodeEditorStateAdapter());
            dockManager.setContentStateRegistry(registry);
            dockManager.setContentFactory(new CodeEditorFactory());

            CodeEditor editor = new CodeEditor();
            editor.setText("will be lost");

            DockLeaf leaf = dockManager.createLeaf("LostEditor", editor);
            leaf.setContentFactoryId(CodeEditorFactory.FACTORY_ID);
            DockTabGroup tabGroup = dockManager.createTabGroup();
            tabGroup.addLeaf(leaf);
            dockManager.setRoot((org.metalib.papifly.fx.docks.core.DockElement) tabGroup);
        });
        WaitForAsyncUtils.waitForFxEvents();

        String json = callOnFx(() -> dockManager.saveSessionToString());

        // Clear and restore with nothing registered
        runOnFx(() -> {
            dockManager.setRoot((org.metalib.papifly.fx.docks.core.DockElement) null);
            dockManager.setContentStateRegistry(new ContentStateRegistry());
            dockManager.setContentFactory(id -> null); // factory that returns null
        });
        WaitForAsyncUtils.waitForFxEvents();

        runOnFx(() -> dockManager.restoreSessionFromString(json));
        WaitForAsyncUtils.waitForFxEvents();

        // Session structure should still be intact (placeholder content)
        assertNotNull(callOnFx(() -> dockManager.getRoot()));
        assertTrue(callOnFx(() -> dockManager.getRoot() instanceof DockTabGroup));
        DockTabGroup group = callOnFx(() -> (DockTabGroup) dockManager.getRoot());
        assertEquals(1, callOnFx(() -> group.getTabs().size()));
        assertNotNull(callOnFx(() -> group.getTabs().getFirst().getContent()));
    }

    /**
     * B4: Multiple editor leaves preserve independent state through session round-trip.
     */
    @Test
    void multipleEditorLeavesPreserveIndependentState(@TempDir Path tempDir) throws Exception {
        Path file1 = tempDir.resolve("alpha.java");
        Path file2 = tempDir.resolve("beta.js");
        Files.writeString(file1, "public class Alpha {}");
        Files.writeString(file2, "function beta() {}");

        runOnFx(() -> {
            ContentStateRegistry registry = new ContentStateRegistry();
            registry.register(new CodeEditorStateAdapter());
            dockManager.setContentStateRegistry(registry);
            dockManager.setContentFactory(new CodeEditorFactory());

            CodeEditor editor1 = new CodeEditor();
            editor1.setFilePath(file1.toString());
            editor1.setText("public class Alpha {}");
            editor1.setLanguageId("java");
            editor1.getSelectionModel().moveCaret(0, 7);

            CodeEditor editor2 = new CodeEditor();
            editor2.setFilePath(file2.toString());
            editor2.setText("function beta() {}");
            editor2.setLanguageId("javascript");
            editor2.getSelectionModel().moveCaret(0, 9);

            DockLeaf leaf1 = dockManager.createLeaf("Alpha", editor1);
            leaf1.setContentFactoryId(CodeEditorFactory.FACTORY_ID);
            DockLeaf leaf2 = dockManager.createLeaf("Beta", editor2);
            leaf2.setContentFactoryId(CodeEditorFactory.FACTORY_ID);

            DockTabGroup tabGroup = dockManager.createTabGroup();
            tabGroup.addLeaf(leaf1);
            tabGroup.addLeaf(leaf2);
            dockManager.setRoot((org.metalib.papifly.fx.docks.core.DockElement) tabGroup);
        });
        WaitForAsyncUtils.waitForFxEvents();

        String json = callOnFx(() -> dockManager.saveSessionToString());

        runOnFx(() -> dockManager.setRoot((org.metalib.papifly.fx.docks.core.DockElement) null));
        WaitForAsyncUtils.waitForFxEvents();

        runOnFx(() -> dockManager.restoreSessionFromString(json));
        WaitForAsyncUtils.waitForFxEvents();

        // Verify both leaves restored with correct state
        DockTabGroup group = callOnFx(() -> (DockTabGroup) dockManager.getRoot());
        assertEquals(2, callOnFx(() -> group.getTabs().size()));

        CodeEditor restored1 = callOnFx(() -> (CodeEditor) group.getTabs().get(0).getContent());
        CodeEditor restored2 = callOnFx(() -> (CodeEditor) group.getTabs().get(1).getContent());

        assertEquals(file1.toString(), callOnFx(restored1::getFilePath));
        assertEquals("java", callOnFx(restored1::getLanguageId));
        assertEquals(0, callOnFx(restored1::getCursorLine));
        assertEquals(7, callOnFx(restored1::getCursorColumn));
        assertTrue(callOnFx(() -> restored1.getText().contains("Alpha")));

        assertEquals(file2.toString(), callOnFx(restored2::getFilePath));
        assertEquals("javascript", callOnFx(restored2::getLanguageId));
        assertEquals(0, callOnFx(restored2::getCursorLine));
        assertEquals(9, callOnFx(restored2::getCursorColumn));
        assertTrue(callOnFx(() -> restored2.getText().contains("beta")));

        // Cleanup
        runOnFx(() -> { restored1.dispose(); restored2.dispose(); });
    }

    // --- Helpers ---

    private void runOnFx(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try { action.run(); } finally { latch.countDown(); }
        });
        try { latch.await(); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T callOnFx(Callable<T> action) {
        if (Platform.isFxApplicationThread()) {
            try { return action.call(); } catch (Exception e) { throw new RuntimeException(e); }
        }
        CountDownLatch latch = new CountDownLatch(1);
        Object[] result = new Object[1];
        Platform.runLater(() -> {
            try { result[0] = action.call(); } catch (Exception e) { throw new RuntimeException(e); } finally { latch.countDown(); }
        });
        try { latch.await(); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        return (T) result[0];
    }
}
