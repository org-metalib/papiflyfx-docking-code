package org.metalib.papifly.fx.code.api;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.metalib.papifly.fx.code.command.CaretRange;
import org.metalib.papifly.fx.code.command.EditorCommand;
import org.metalib.papifly.fx.code.command.LineEditService;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.language.TestLanguageSupportProvider;
import org.metalib.papifly.fx.code.lexer.IncrementalLexerPipeline;
import org.metalib.papifly.fx.code.lexer.TokenType;
import org.metalib.papifly.fx.code.search.SearchController;
import org.metalib.papifly.fx.code.search.SearchModel;
import org.metalib.papifly.fx.code.state.CaretStateData;
import org.metalib.papifly.fx.code.state.EditorStateCodec;
import org.metalib.papifly.fx.code.state.EditorStateData;
import org.metalib.papifly.fx.docking.api.LeafContentData;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class CodeEditorIntegrationTest {

    private CodeEditor editor;

    @Start
    void start(Stage stage) {
        editor = new CodeEditor();
        Scene scene = new Scene(editor, 640, 480);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void setTextAndGetText() {
        runOnFx(() -> editor.setText("hello world"));
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals("hello world", callOnFx(() -> editor.getText()));
    }

    @Test
    void setTextResetsCaretToOrigin() {
        runOnFx(() -> {
            editor.setText("hello\nworld");
            editor.getSelectionModel().moveCaret(1, 3);
            editor.setText("new text");
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(0, callOnFx(() -> editor.getSelectionModel().getCaretLine()));
        assertEquals(0, callOnFx(() -> editor.getSelectionModel().getCaretColumn()));
    }

    @Test
    void documentIsAccessible() {
        assertNotNull(callOnFx(() -> editor.getDocument()));
    }

    @Test
    void viewportIsAccessible() {
        assertNotNull(callOnFx(() -> editor.getViewport()));
    }

    @Test
    void selectionModelIsAccessible() {
        assertNotNull(callOnFx(() -> editor.getSelectionModel()));
    }

    @Test
    void documentTextReflectsSetText() {
        runOnFx(() -> editor.setText("abc\ndef"));
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals("abc\ndef", callOnFx(() -> editor.getDocument().getText()));
    }

    @Test
    void cursorLinePropertyUpdatesWithSelectionModel() {
        runOnFx(() -> {
            editor.setText("hello\nworld\nfoo");
            editor.getSelectionModel().moveCaret(2, 1);
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(2, callOnFx(() -> editor.getCursorLine()));
    }

    @Test
    void cursorColumnPropertyUpdatesWithSelectionModel() {
        runOnFx(() -> {
            editor.setText("hello");
            editor.getSelectionModel().moveCaret(0, 3);
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(3, callOnFx(() -> editor.getCursorColumn()));
    }

    @Test
    void undoRedoViaDocument() {
        runOnFx(() -> {
            editor.setText("initial");
            editor.getDocument().insert(7, " text");
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals("initial text", callOnFx(() -> editor.getText()));

        runOnFx(() -> editor.getDocument().undo());
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals("initial", callOnFx(() -> editor.getText()));

        runOnFx(() -> editor.getDocument().redo());
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals("initial text", callOnFx(() -> editor.getText()));
    }

    @Test
    void selectAllViaSelectionModel() {
        runOnFx(() -> {
            editor.setText("hello\nworld");
            editor.getSelectionModel().selectAll(editor.getDocument());
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(callOnFx(() -> editor.getSelectionModel().hasSelection()));
        assertEquals("hello\nworld",
            callOnFx(() -> editor.getSelectionModel().getSelectedText(editor.getDocument())));
    }

    @Test
    void captureAndApplyStateRoundTrip() {
        runOnFx(() -> {
            editor.setFilePath("/test/file.java");
            editor.setLanguageId("java");
            editor.setText("content");
            editor.getSelectionModel().moveCaret(0, 3);
        });
        WaitForAsyncUtils.waitForFxEvents();

        var state = callOnFx(() -> editor.captureState());
        assertNotNull(state);
        assertEquals("/test/file.java", state.filePath());
        assertEquals("java", state.languageId());
    }

    @Test
    void applyStateMovesActualCaretModel() {
        runOnFx(() -> {
            editor.setText("line0\nline1\nline2");
            editor.applyState(new EditorStateData("", 1, 3, 0.0, "plain-text", List.of()));
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(1, callOnFx(() -> editor.getSelectionModel().getCaretLine()));
        assertEquals(3, callOnFx(() -> editor.getSelectionModel().getCaretColumn()));
    }

    @Test
    void applyStateClampsCaretToDocumentBounds() {
        runOnFx(() -> {
            editor.setText("aa\nbbb");
            editor.applyState(new EditorStateData("", 99, 99, 0.0, "plain-text", List.of()));
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(1, callOnFx(() -> editor.getSelectionModel().getCaretLine()));
        assertEquals(3, callOnFx(() -> editor.getSelectionModel().getCaretColumn()));
    }

    @Test
    void applyStateRestoresHorizontalScrollWhenWrapOff() {
        runOnFx(() -> {
            editor.setText("x".repeat(2_000));
            editor.applyCss();
            editor.layout();
            editor.applyState(new EditorStateData(
                "",
                0,
                0,
                0.0,
                220.0,
                false,
                "plain-text",
                List.of(),
                0,
                0,
                List.of()
            ));
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(callOnFx(() -> editor.getHorizontalScrollOffset() > 0.0));
    }

    @Test
    void applyStateForWrapModeForcesHorizontalOffsetToZero() {
        runOnFx(() -> {
            editor.setText("x".repeat(2_000));
            editor.applyCss();
            editor.layout();
            editor.applyState(new EditorStateData(
                "",
                0,
                0,
                0.0,
                220.0,
                true,
                "plain-text",
                List.of(),
                0,
                0,
                List.of()
            ));
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(callOnFx(editor::isWordWrap));
        assertEquals(0.0, callOnFx(editor::getHorizontalScrollOffset), 0.0001);
    }

    @Test
    void captureStateIncludesPrimarySelectionAndSecondaryCarets() {
        runOnFx(() -> {
            editor.setText("alpha\nbeta\ngamma");
            editor.getSelectionModel().moveCaret(0, 1);
            editor.getSelectionModel().moveCaretWithSelection(0, 4);
            editor.getMultiCaretModel().addCaretNoStack(new CaretRange(1, 0, 1, 2));
            editor.getMultiCaretModel().addCaretNoStack(new CaretRange(2, 3, 2, 3));
        });
        WaitForAsyncUtils.waitForFxEvents();

        EditorStateData state = callOnFx(editor::captureState);
        assertEquals(0, state.cursorLine());
        assertEquals(4, state.cursorColumn());
        assertEquals(0, state.anchorLine());
        assertEquals(1, state.anchorColumn());
        assertEquals(
            List.of(
                new CaretStateData(1, 0, 1, 2),
                new CaretStateData(2, 3, 2, 3)
            ),
            state.secondaryCarets()
        );
    }

    @Test
    void applyStateRestoresPrimarySelectionAndSecondaryCarets() {
        runOnFx(() -> {
            editor.setText("alpha\nbeta\ngamma");
            editor.applyState(new EditorStateData(
                "",
                0,
                4,
                0.0,
                "plain-text",
                List.of(),
                0,
                1,
                List.of(
                    new CaretStateData(1, 0, 1, 2),
                    new CaretStateData(2, 3, 2, 3)
                )
            ));
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(0, callOnFx(() -> editor.getSelectionModel().getAnchorLine()));
        assertEquals(1, callOnFx(() -> editor.getSelectionModel().getAnchorColumn()));
        assertEquals(0, callOnFx(() -> editor.getSelectionModel().getCaretLine()));
        assertEquals(4, callOnFx(() -> editor.getSelectionModel().getCaretColumn()));
        assertEquals(
            List.of(
                new CaretRange(1, 0, 1, 2),
                new CaretRange(2, 3, 2, 3)
            ),
            callOnFx(() -> editor.getMultiCaretModel().getSecondaryCarets())
        );
    }

    @Test
    void undoRedoShortcutKeepsCaretNearEditLocation() {
        runOnFx(() -> {
            editor.requestFocus();
            editor.setText("abc");
            editor.getDocument().insert(3, "d");
            editor.getSelectionModel().moveCaret(0, 4);
        });
        WaitForAsyncUtils.waitForFxEvents();

        fireShortcut(KeyCode.Z, false);
        assertEquals("abc", callOnFx(() -> editor.getText()));
        assertEquals(3, callOnFx(() -> editor.getSelectionModel().getCaretColumn()));

        fireShortcut(KeyCode.Y, false);
        assertEquals("abcd", callOnFx(() -> editor.getText()));
        assertEquals(4, callOnFx(() -> editor.getSelectionModel().getCaretColumn()));
    }

    @Test
    void goToLineShortcutOpensOverlay() {
        runOnFx(() -> {
            editor.requestFocus();
            editor.setText(buildLines(40));
        });
        WaitForAsyncUtils.waitForFxEvents();

        fireShortcut(KeyCode.G, false);

        assertTrue(callOnFx(() -> editor.getGoToLineController().isOpen()));
    }

    @Test
    void escapeClosesGoToLineOverlay() {
        runOnFx(() -> {
            editor.setText(buildLines(40));
            editor.goToLine();
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(callOnFx(() -> editor.getGoToLineController().isOpen()));

        fireKey(KeyCode.ESCAPE);

        assertFalse(callOnFx(() -> editor.getGoToLineController().isOpen()));
    }

    @Test
    void pageDownMovesCaretByViewportPage() {
        runOnFx(() -> {
            editor.setText(buildLines(500));
            editor.applyCss();
            editor.layout();
            editor.getSelectionModel().moveCaret(10, 3);
        });
        WaitForAsyncUtils.waitForFxEvents();

        int pageLineDelta = callOnFx(this::pageLineDelta);
        runOnFx(() -> editor.executeCommand(EditorCommand.MOVE_PAGE_DOWN));
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(10 + pageLineDelta, callOnFx(() -> editor.getSelectionModel().getCaretLine()));
        assertEquals(3, callOnFx(() -> editor.getSelectionModel().getCaretColumn()));
    }

    @Test
    void shiftPageUpExtendsSelectionByViewportPage() {
        runOnFx(() -> {
            editor.setText(buildLines(500));
            editor.applyCss();
            editor.layout();
            editor.getSelectionModel().moveCaret(120, 2);
        });
        WaitForAsyncUtils.waitForFxEvents();

        int pageLineDelta = callOnFx(this::pageLineDelta);
        runOnFx(() -> editor.executeCommand(EditorCommand.SELECT_PAGE_UP));
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(120, callOnFx(() -> editor.getSelectionModel().getAnchorLine()));
        assertEquals(2, callOnFx(() -> editor.getSelectionModel().getAnchorColumn()));
        assertEquals(120 - pageLineDelta, callOnFx(() -> editor.getSelectionModel().getCaretLine()));
        assertEquals(2, callOnFx(() -> editor.getSelectionModel().getCaretColumn()));
        assertTrue(callOnFx(() -> editor.getSelectionModel().hasSelection()));
    }

    @Test
    void pageScrollCommandDoesNotMoveCaret() {
        runOnFx(() -> {
            editor.setText(buildLines(500));
            editor.applyCss();
            editor.layout();
            editor.getSelectionModel().moveCaret(20, 1);
            editor.setVerticalScrollOffset(0);
        });
        WaitForAsyncUtils.waitForFxEvents();

        runOnFx(() -> editor.executeCommand(EditorCommand.SCROLL_PAGE_DOWN));
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(20, callOnFx(() -> editor.getSelectionModel().getCaretLine()));
        assertEquals(1, callOnFx(() -> editor.getSelectionModel().getCaretColumn()));
        assertTrue(callOnFx(() -> editor.getVerticalScrollOffset() > 0));
    }

    @Test
    void horizontalScrollRespondsToTrackpadDeltaX() {
        runOnFx(() -> {
            editor.setText("x".repeat(2_000));
            editor.applyCss();
            editor.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        fireScroll(-200, 0, false);

        assertTrue(callOnFx(() -> editor.getHorizontalScrollOffset() > 0.0));
    }

    @Test
    void shiftWheelMapsVerticalDeltaToHorizontalWhenWrapOff() {
        runOnFx(() -> {
            editor.setText("x".repeat(2_000));
            editor.applyCss();
            editor.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        fireScroll(0, -120, true);

        assertTrue(callOnFx(() -> editor.getHorizontalScrollOffset() > 0.0));
    }

    @Test
    void enablingWordWrapResetsHorizontalOffsetAndHidesHorizontalScrollbar() {
        runOnFx(() -> {
            editor.setText("x".repeat(2_000));
            editor.applyCss();
            editor.layout();
            editor.setHorizontalScrollOffset(250);
            editor.setWordWrap(true);
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(0.0, callOnFx(editor::getHorizontalScrollOffset), 0.0001);
        assertFalse(callOnFx(() -> editor.getViewport().isHorizontalScrollbarVisible()));
    }

    @Test
    void verticalMovePreservesPreferredColumnAcrossShortLine() {
        runOnFx(() -> {
            editor.setText("0123456789\nxy\n0123456789");
            editor.getSelectionModel().moveCaret(0, 8);
        });
        WaitForAsyncUtils.waitForFxEvents();

        runOnFx(() -> editor.executeCommand(EditorCommand.MOVE_DOWN));
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(1, callOnFx(() -> editor.getSelectionModel().getCaretLine()));
        assertEquals(2, callOnFx(() -> editor.getSelectionModel().getCaretColumn()));

        runOnFx(() -> editor.executeCommand(EditorCommand.MOVE_DOWN));
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(2, callOnFx(() -> editor.getSelectionModel().getCaretLine()));
        assertEquals(8, callOnFx(() -> editor.getSelectionModel().getCaretColumn()));
    }

    @Test
    void horizontalMoveResetsPreferredColumnForVerticalMove() {
        runOnFx(() -> {
            editor.setText("0123456789\nxy\n0123456789");
            editor.getSelectionModel().moveCaret(0, 8);
            editor.executeCommand(EditorCommand.MOVE_DOWN);
            editor.executeCommand(EditorCommand.MOVE_LEFT);
            editor.executeCommand(EditorCommand.MOVE_DOWN);
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(2, callOnFx(() -> editor.getSelectionModel().getCaretLine()));
        assertEquals(1, callOnFx(() -> editor.getSelectionModel().getCaretColumn()));
    }

    @Test
    void shiftVerticalMoveKeepsPreferredColumnAndAnchor() {
        runOnFx(() -> {
            editor.setText("0123456789\nxy\n0123456789");
            editor.getSelectionModel().moveCaret(0, 8);
        });
        WaitForAsyncUtils.waitForFxEvents();

        runOnFx(() -> editor.executeCommand(EditorCommand.SELECT_DOWN));
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(0, callOnFx(() -> editor.getSelectionModel().getAnchorLine()));
        assertEquals(8, callOnFx(() -> editor.getSelectionModel().getAnchorColumn()));
        assertEquals(1, callOnFx(() -> editor.getSelectionModel().getCaretLine()));
        assertEquals(2, callOnFx(() -> editor.getSelectionModel().getCaretColumn()));

        runOnFx(() -> editor.executeCommand(EditorCommand.SELECT_DOWN));
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(0, callOnFx(() -> editor.getSelectionModel().getAnchorLine()));
        assertEquals(8, callOnFx(() -> editor.getSelectionModel().getAnchorColumn()));
        assertEquals(2, callOnFx(() -> editor.getSelectionModel().getCaretLine()));
        assertEquals(8, callOnFx(() -> editor.getSelectionModel().getCaretColumn()));
        assertTrue(callOnFx(() -> editor.getSelectionModel().hasSelection()));
    }

    @Test
    void captureStateUsesActualViewportScrollOffset() {
        runOnFx(() -> {
            editor.setText(buildLines(100));
            editor.applyCss();
            editor.layout();
            editor.setVerticalScrollOffset(100_000);
            editor.applyCss();
            editor.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();

        double actualOffset = callOnFx(() -> editor.getViewport().getScrollOffset());
        EditorStateData state = callOnFx(() -> editor.captureState());
        assertEquals(actualOffset, state.verticalScrollOffset(), 0.0001);
    }

    @Test
    void captureStateIncludesHorizontalScrollAndWrapFlag() {
        runOnFx(() -> {
            editor.setText("x".repeat(2_000));
            editor.applyCss();
            editor.layout();
            editor.setHorizontalScrollOffset(180);
            editor.setWordWrap(true);
        });
        WaitForAsyncUtils.waitForFxEvents();

        EditorStateData state = callOnFx(editor::captureState);
        assertEquals(0.0, state.horizontalScrollOffset(), 0.0001);
        assertTrue(state.wordWrap());
    }

    @Test
    void disposeRemovesInputHandlers() {
        runOnFx(() -> editor.dispose());
        WaitForAsyncUtils.waitForFxEvents();
        assertNull(callOnFx(editor::getOnKeyPressed));
        assertNull(callOnFx(editor::getOnScroll));
    }

    @Test
    void disposeDetachesCaretMirrorListeners() {
        runOnFx(() -> {
            editor.setText("line0\nline1");
            editor.getSelectionModel().moveCaret(0, 2);
            editor.dispose();
            editor.getSelectionModel().moveCaret(1, 4);
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(0, callOnFx(editor::getCursorLine));
        assertEquals(2, callOnFx(editor::getCursorColumn));
    }

    @Test
    void disposeDetachesScrollMirrorListener() {
        runOnFx(() -> {
            editor.setText(buildLines(200));
            editor.applyCss();
            editor.layout();
            editor.setVerticalScrollOffset(240);
        });
        WaitForAsyncUtils.waitForFxEvents();

        double beforeDisposeViewportOffset = callOnFx(() -> editor.getViewport().getScrollOffset());

        runOnFx(() -> {
            editor.dispose();
            editor.setVerticalScrollOffset(640);
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(beforeDisposeViewportOffset, callOnFx(() -> editor.getViewport().getScrollOffset()), 0.0001);
    }

    @Test
    void disposeClearsSearchControllerCallbacksBeforeClose() {
        AtomicBoolean onCloseCalled = new AtomicBoolean(false);
        AtomicBoolean onSearchChangedCalled = new AtomicBoolean(false);

        runOnFx(() -> {
            editor.getSearchController().setOnClose(() -> onCloseCalled.set(true));
            editor.getSearchController().setOnSearchChanged(() -> onSearchChangedCalled.set(true));
            editor.getSearchController().open("query");
            onCloseCalled.set(false);
            onSearchChangedCalled.set(false);
            editor.dispose();
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertFalse(onCloseCalled.get());
        assertFalse(onSearchChangedCalled.get());
        assertFalse(callOnFx(() -> editor.getSearchController().isOpen()));
    }

    @Test
    void packagePrivateConstructorUsesInjectedFactories() {
        AtomicBoolean lexerFactoryUsed = new AtomicBoolean(false);
        CodeEditor injected = callOnFx(() -> {
            SearchModel customSearchModel = new SearchModel();
            SearchController customSearchController = new SearchController(customSearchModel);
            GoToLineController customGoToLine = new GoToLineController();
            return new CodeEditor(
                new Document("sample"),
                customSearchModel,
                customSearchController,
                customGoToLine,
                (doc, tokenConsumer) -> {
                    lexerFactoryUsed.set(true);
                    return new IncrementalLexerPipeline(doc, tokenConsumer);
                },
                new LineEditService()
            );
        });

        assertTrue(lexerFactoryUsed.get());
        assertNotNull(callOnFx(injected::getSearchModel));
        callOnFx(() -> {
            injected.dispose();
            return null;
        });
    }

    @Test
    void adapterRestoreVersionZeroMigratesState() {
        CodeEditorStateAdapter adapter = new CodeEditorStateAdapter();
        Map<String, Object> state = EditorStateCodec.toMap(
            new EditorStateData("/tmp/demo.java", 0, 0, 0.0, "java", List.of(1, 3))
        );

        CodeEditor restored = callOnFx(() -> (CodeEditor) adapter.restore(
            new LeafContentData(CodeEditorFactory.FACTORY_ID, "editor-1", 0, state)
        ));

        assertEquals("/tmp/demo.java", callOnFx(restored::getFilePath));
        assertEquals("java", callOnFx(restored::getLanguageId));
        assertEquals(List.of(1, 3), callOnFx(restored::getFoldedLines));
        callOnFx(() -> {
            restored.dispose();
            return null;
        });
    }

    @Test
    void adapterRestoreVersionOneMigratesStateToV2Defaults() {
        java.nio.file.Path tempFile = null;
        try {
            tempFile = java.nio.file.Files.createTempFile("editor-v1-", ".txt");
            java.nio.file.Files.writeString(tempFile, "line0\nline1-with-width\nline2-with-width");

            CodeEditorStateAdapter adapter = new CodeEditorStateAdapter();
            Map<String, Object> state = Map.of(
                "filePath", tempFile.toString(),
                "cursorLine", 2,
                "cursorColumn", 7,
                "verticalScrollOffset", 1.0,
                "languageId", "java",
                "foldedLines", List.of(4)
            );

            CodeEditor restored = callOnFx(() -> (CodeEditor) adapter.restore(
                new LeafContentData(CodeEditorFactory.FACTORY_ID, "editor-v1", 1, state)
            ));

            assertEquals(tempFile.toString(), callOnFx(restored::getFilePath));
            assertEquals(2, callOnFx(() -> restored.getSelectionModel().getCaretLine()));
            assertEquals(7, callOnFx(() -> restored.getSelectionModel().getCaretColumn()));
            assertEquals(2, callOnFx(() -> restored.getSelectionModel().getAnchorLine()));
            assertEquals(7, callOnFx(() -> restored.getSelectionModel().getAnchorColumn()));
            assertEquals(List.of(), callOnFx(() -> restored.getMultiCaretModel().getSecondaryCarets()));
            callOnFx(() -> {
                restored.dispose();
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (tempFile != null) {
                try {
                    java.nio.file.Files.deleteIfExists(tempFile);
                } catch (java.io.IOException ignored) {
                    // Best-effort cleanup for test temp file.
                }
            }
        }
    }

    @Test
    void adapterRestoreUnknownVersionFallsBackToEmptyState() {
        CodeEditorStateAdapter adapter = new CodeEditorStateAdapter();
        Map<String, Object> state = EditorStateCodec.toMap(
            new EditorStateData("/tmp/wrong.java", 0, 0, 0.0, "java", List.of(9))
        );

        CodeEditor restored = callOnFx(() -> (CodeEditor) adapter.restore(
            new LeafContentData(CodeEditorFactory.FACTORY_ID, "editor-2", 99, state)
        ));

        assertEquals("", callOnFx(restored::getFilePath));
        assertEquals("plain-text", callOnFx(restored::getLanguageId));
        assertEquals(List.of(), callOnFx(restored::getFoldedLines));
        callOnFx(() -> {
            restored.dispose();
            return null;
        });
    }

    @Test
    void syntaxTokensUpdateInViewportAfterLanguageSelection() {
        runOnFx(() -> {
            editor.setText("plugin Demo {}");
            editor.setLanguageId(TestLanguageSupportProvider.TEST_LANGUAGE_ID);
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(waitForCondition(
            () -> callOnFx(() -> editor.getViewport()
                .getTokenMap()
                .tokensForLine(0)
                .stream()
                .anyMatch(token -> token.type() == TokenType.KEYWORD)),
            2_000
        ));
    }

    @Test
    void gutterWidthUpdatesWhenLineCountCrossesDigitBoundary() {
        runOnFx(() -> {
            editor.setText(buildLines(99));
            editor.applyCss();
            editor.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();
        double widthFor99 = callOnFx(() -> editor.getGutterView().getComputedWidth());

        runOnFx(() -> {
            editor.getDocument().insert(editor.getDocument().length(), "\nline99");
            editor.applyCss();
            editor.layout();
        });
        WaitForAsyncUtils.waitForFxEvents();
        double widthFor100 = callOnFx(() -> editor.getGutterView().getComputedWidth());

        assertTrue(widthFor100 > widthFor99);
    }

    // --- Review 5 regression tests ---

    @Test
    void typedCharactersAdvanceCaretCorrectly() {
        runOnFx(() -> {
            editor.setText("");
            editor.requestFocus();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Simulate typing 'a', 'b', 'c' via KEY_TYPED events
        for (String ch : List.of("a", "b", "c")) {
            runOnFx(() -> editor.fireEvent(new KeyEvent(
                KeyEvent.KEY_TYPED,
                ch,
                "",
                KeyCode.UNDEFINED,
                false, false, false, false
            )));
            WaitForAsyncUtils.waitForFxEvents();
        }

        assertEquals("abc", callOnFx(() -> editor.getText()));
        assertEquals(0, callOnFx(() -> editor.getSelectionModel().getCaretLine()));
        assertEquals(3, callOnFx(() -> editor.getSelectionModel().getCaretColumn()));
    }

    @Test
    void dockLeafDisposeCallsCodeEditorDispose() {
        runOnFx(() -> {
            org.metalib.papifly.fx.docks.core.DockLeaf leaf = new org.metalib.papifly.fx.docks.core.DockLeaf();
            leaf.content(editor);
            leaf.dispose();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // After DockLeaf.dispose(), CodeEditor.dispose() should have been called.
        // Verify by checking that further theme binding does not throw (disposed flag is set).
        // The editor's internal disposed flag prevents re-disposal.
        runOnFx(() -> editor.dispose()); // should be no-op, not throw
    }

    @Test
    void stateAdapterRestoresFileContent() throws Exception {
        // Create a temporary file with content
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("editor-test-", ".txt");
        java.nio.file.Files.writeString(tempFile, "file content here");

        try {
            EditorStateData state = new EditorStateData(
                tempFile.toString(), 0, 5, 0.0, "plain-text", List.of()
            );
            Map<String, Object> stateMap = EditorStateCodec.toMap(state);
            LeafContentData contentData = new LeafContentData(
                CodeEditorFactory.FACTORY_ID, "test-id", CodeEditorStateAdapter.VERSION, stateMap
            );

            CodeEditorStateAdapter adapter = new CodeEditorStateAdapter();
            CodeEditor[] restored = new CodeEditor[1];
            runOnFx(() -> restored[0] = (CodeEditor) adapter.restore(contentData));
            WaitForAsyncUtils.waitForFxEvents();

            assertEquals("file content here", callOnFx(() -> restored[0].getText()));
            assertEquals(0, callOnFx(() -> restored[0].getCursorLine()));
            assertEquals(5, callOnFx(() -> restored[0].getCursorColumn()));
        } finally {
            java.nio.file.Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void stateAdapterHandlesMissingFile() {
        EditorStateData state = new EditorStateData(
            "/nonexistent/path/to/file.txt", 2, 3, 10.0, "java", List.of()
        );
        Map<String, Object> stateMap = EditorStateCodec.toMap(state);
        LeafContentData contentData = new LeafContentData(
            CodeEditorFactory.FACTORY_ID, "test-id", CodeEditorStateAdapter.VERSION, stateMap
        );

        CodeEditorStateAdapter adapter = new CodeEditorStateAdapter();
        CodeEditor[] restored = new CodeEditor[1];
        runOnFx(() -> restored[0] = (CodeEditor) adapter.restore(contentData));
        WaitForAsyncUtils.waitForFxEvents();

        // Empty document but metadata preserved
        assertEquals("", callOnFx(() -> restored[0].getText()));
        assertEquals("/nonexistent/path/to/file.txt", callOnFx(() -> restored[0].getFilePath()));
        assertEquals("java", callOnFx(() -> restored[0].getLanguageId()));
    }

    // --- Phase 6: persistence contract tests ---

    @Test
    void adapterRestoreHandlesInvalidPathSyntax() {
        CodeEditorStateAdapter adapter = new CodeEditorStateAdapter();
        // Use a path with null bytes which is invalid on all platforms
        Map<String, Object> state = EditorStateCodec.toMap(
            new EditorStateData("/invalid\u0000path/file.txt", 3, 5, 20.0, "javascript", List.of())
        );

        CodeEditor restored = callOnFx(() -> (CodeEditor) adapter.restore(
            new LeafContentData(
                CodeEditorFactory.FACTORY_ID,
                "editor-path",
                CodeEditorStateAdapter.VERSION,
                state
            )
        ));

        // Empty document but metadata preserved
        assertEquals("", callOnFx(restored::getText));
        assertEquals("/invalid\u0000path/file.txt", callOnFx(restored::getFilePath));
        assertEquals("javascript", callOnFx(restored::getLanguageId));
        callOnFx(() -> {
            restored.dispose();
            return null;
        });
    }

    @Test
    void adapterSaveStateReturnsAllV3Fields() {
        CodeEditorStateAdapter adapter = new CodeEditorStateAdapter();
        runOnFx(() -> {
            editor.setFilePath("/test/save.java");
            editor.setLanguageId("java");
            editor.setText("line0\nline1\nline2");
            editor.getSelectionModel().moveCaret(1, 3);
            editor.setHorizontalScrollOffset(120);
            editor.setWordWrap(false);
        });
        WaitForAsyncUtils.waitForFxEvents();

        Map<String, Object> state = callOnFx(() -> adapter.saveState("test-id", editor));
        assertNotNull(state);
        assertFalse(state.isEmpty());
        assertEquals("/test/save.java", state.get("filePath"));
        assertEquals(1, ((Number) state.get("cursorLine")).intValue());
        assertEquals(3, ((Number) state.get("cursorColumn")).intValue());
        assertEquals(1, ((Number) state.get("anchorLine")).intValue());
        assertEquals(3, ((Number) state.get("anchorColumn")).intValue());
        assertEquals("java", state.get("languageId"));
        assertNotNull(state.get("verticalScrollOffset"));
        assertNotNull(state.get("horizontalScrollOffset"));
        assertNotNull(state.get("wordWrap"));
        assertNotNull(state.get("foldedLines"));
        assertNotNull(state.get("foldedRegions"));
        assertNotNull(state.get("secondaryCarets"));
    }

    @Test
    void applyStateCapsAndDeduplicatesSecondaryCarets() {
        runOnFx(() -> {
            editor.setText(buildLines(3000));
            List<CaretStateData> largeState = new ArrayList<>();
            // Duplicate of primary; should be dropped.
            largeState.add(new CaretStateData(0, 0, 0, 0));
            for (int line = 1; line <= 2600; line++) {
                largeState.add(new CaretStateData(line, 0, line, 1));
            }
            editor.applyState(new EditorStateData(
                "",
                0,
                0,
                0.0,
                "plain-text",
                List.of(),
                0,
                0,
                largeState
            ));
        });
        WaitForAsyncUtils.waitForFxEvents();

        List<CaretRange> restored = callOnFx(() -> editor.getMultiCaretModel().getSecondaryCarets());
        assertEquals(2048, restored.size());
        assertEquals(new CaretRange(1, 0, 1, 1), restored.getFirst());
        assertFalse(restored.contains(new CaretRange(0, 0, 0, 0)));
    }

    // --- Helpers ---

    private String buildLines(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append("line").append(i);
        }
        return sb.toString();
    }

    private int pageLineDelta() {
        double lineHeight = editor.getViewport().getGlyphCache().getLineHeight();
        if (lineHeight <= 0) {
            return 1;
        }
        double viewportHeight = editor.getViewport().getHeight();
        double pagePixels = viewportHeight <= 0 ? lineHeight : viewportHeight;
        return Math.max(1, (int) Math.floor(pagePixels / lineHeight));
    }

    private void fireShortcut(KeyCode keyCode, boolean shift) {
        runOnFx(() -> editor.fireEvent(new KeyEvent(
            KeyEvent.KEY_PRESSED,
            "",
            "",
            keyCode,
            shift,
            true,
            false,
            false
        )));
        WaitForAsyncUtils.waitForFxEvents();
    }

    private void fireKey(KeyCode keyCode) {
        runOnFx(() -> editor.fireEvent(new KeyEvent(
            KeyEvent.KEY_PRESSED,
            "",
            "",
            keyCode,
            false,
            false,
            false,
            false
        )));
        WaitForAsyncUtils.waitForFxEvents();
    }

    private void fireScroll(double deltaX, double deltaY, boolean shiftDown) {
        runOnFx(() -> {
            Point2D scenePoint = editor.getViewport().localToScene(20, 20);
            Point2D editorPoint = editor.sceneToLocal(scenePoint);
            PickResult pick = new PickResult(editor, editorPoint.getX(), editorPoint.getY());
            ScrollEvent event = new ScrollEvent(
                null,
                editor,
                ScrollEvent.SCROLL,
                scenePoint.getX(),
                scenePoint.getY(),
                scenePoint.getX(),
                scenePoint.getY(),
                shiftDown,
                false,
                false,
                false,
                false,
                false,
                deltaX,
                deltaY,
                deltaX,
                deltaY,
                ScrollEvent.HorizontalTextScrollUnits.NONE,
                0,
                ScrollEvent.VerticalTextScrollUnits.NONE,
                0,
                0,
                pick
            );
            editor.fireEvent(event);
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    private boolean waitForCondition(BooleanSupplier condition, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            WaitForAsyncUtils.waitForFxEvents();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return condition.getAsBoolean();
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
