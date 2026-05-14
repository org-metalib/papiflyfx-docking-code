package org.metalib.papifly.fx.code.api;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.document.DocumentChangeListener;
import org.metalib.papifly.fx.code.gutter.MarkerModel;
import org.metalib.papifly.fx.code.render.SelectionModel;

/**
 * Binds and unbinds editor listeners and input handlers.
 */
final class EditorLifecycleService {

    void bindInputHandlers(
        CodeEditor editor,
        EventHandler<? super KeyEvent> keyPressed,
        EventHandler<? super KeyEvent> keyTyped,
        EventHandler<? super MouseEvent> mousePressed,
        EventHandler<? super MouseEvent> mouseDragged,
        EventHandler<? super MouseEvent> mouseReleased,
        EventHandler<? super MouseEvent> mouseMoved,
        EventHandler<? super ScrollEvent> scroll,
        ChangeListener<Boolean> focusListener,
        Runnable syncCaretBlink
    ) {
        editor.setOnKeyPressed(keyPressed);
        editor.setOnKeyTyped(keyTyped);
        editor.setOnMousePressed(mousePressed);
        editor.setOnMouseDragged(mouseDragged);
        editor.setOnMouseReleased(mouseReleased);
        editor.setOnMouseMoved(mouseMoved);
        editor.setOnScroll(scroll);
        editor.focusedProperty().addListener(focusListener);
        syncCaretBlink.run();
    }

    void unbindInputHandlers(CodeEditor editor, ChangeListener<Boolean> focusListener) {
        editor.setOnKeyPressed(null);
        editor.setOnKeyTyped(null);
        editor.setOnMousePressed(null);
        editor.setOnMouseDragged(null);
        editor.setOnMouseReleased(null);
        editor.setOnMouseMoved(null);
        editor.setOnScroll(null);
        editor.focusedProperty().removeListener(focusListener);
    }

    void bindListeners(
        SelectionModel selectionModel,
        ChangeListener<Number> caretLineListener,
        ChangeListener<Number> caretColumnListener,
        Document document,
        DocumentChangeListener gutterWidthListener,
        DocumentChangeListener searchRefreshListener,
        MarkerModel markerModel,
        MarkerModel.MarkerChangeListener markerModelChangeListener,
        DoubleProperty verticalScrollOffset,
        ChangeListener<Number> scrollOffsetListener,
        DoubleProperty horizontalScrollOffset,
        ChangeListener<Number> horizontalScrollOffsetListener,
        BooleanProperty wordWrap,
        ChangeListener<Boolean> wordWrapListener,
        StringProperty languageId,
        ChangeListener<String> languageListener
    ) {
        selectionModel.caretLineProperty().addListener(caretLineListener);
        selectionModel.caretColumnProperty().addListener(caretColumnListener);
        document.addChangeListener(gutterWidthListener);
        document.addChangeListener(searchRefreshListener);
        markerModel.addChangeListener(markerModelChangeListener);
        verticalScrollOffset.addListener(scrollOffsetListener);
        horizontalScrollOffset.addListener(horizontalScrollOffsetListener);
        wordWrap.addListener(wordWrapListener);
        languageId.addListener(languageListener);
    }

    void unbindListeners(
        SelectionModel selectionModel,
        ChangeListener<Number> caretLineListener,
        ChangeListener<Number> caretColumnListener,
        Document document,
        DocumentChangeListener gutterWidthListener,
        DocumentChangeListener searchRefreshListener,
        MarkerModel markerModel,
        MarkerModel.MarkerChangeListener markerModelChangeListener,
        DoubleProperty verticalScrollOffset,
        ChangeListener<Number> scrollOffsetListener,
        DoubleProperty horizontalScrollOffset,
        ChangeListener<Number> horizontalScrollOffsetListener,
        BooleanProperty wordWrap,
        ChangeListener<Boolean> wordWrapListener,
        StringProperty languageId,
        ChangeListener<String> languageListener
    ) {
        selectionModel.caretLineProperty().removeListener(caretLineListener);
        selectionModel.caretColumnProperty().removeListener(caretColumnListener);
        document.removeChangeListener(gutterWidthListener);
        document.removeChangeListener(searchRefreshListener);
        markerModel.removeChangeListener(markerModelChangeListener);
        verticalScrollOffset.removeListener(scrollOffsetListener);
        horizontalScrollOffset.removeListener(horizontalScrollOffsetListener);
        wordWrap.removeListener(wordWrapListener);
        languageId.removeListener(languageListener);
    }
}
