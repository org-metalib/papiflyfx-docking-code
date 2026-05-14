package org.metalib.papifly.fx.code.api;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.util.Duration;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.metalib.papifly.fx.code.command.EditorCommand;
import org.metalib.papifly.fx.code.command.LineEditService;
import org.metalib.papifly.fx.code.command.MultiCaretModel;
import org.metalib.papifly.fx.code.language.LanguageSupportRegistry;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.document.DocumentChangeListener;
import org.metalib.papifly.fx.code.folding.FoldMap;
import org.metalib.papifly.fx.code.folding.FoldRegion;
import org.metalib.papifly.fx.code.folding.IncrementalFoldingPipeline;
import org.metalib.papifly.fx.code.gutter.GutterView;
import org.metalib.papifly.fx.code.gutter.MarkerModel;
import org.metalib.papifly.fx.code.lexer.IncrementalLexerPipeline;
import org.metalib.papifly.fx.code.lexer.TokenMap;
import org.metalib.papifly.fx.code.render.SelectionModel;
import org.metalib.papifly.fx.code.render.Viewport;
import org.metalib.papifly.fx.code.search.SearchController;
import org.metalib.papifly.fx.code.search.SearchModel;
import org.metalib.papifly.fx.code.state.EditorStateData;
import org.metalib.papifly.fx.code.state.FoldRegionRef;
import org.metalib.papifly.fx.code.theme.CodeEditorTheme;
import org.metalib.papifly.fx.code.theme.CodeEditorThemeMapper;
import org.metalib.papifly.fx.docking.api.DisposableContent;
import org.metalib.papifly.fx.docking.api.Theme;
import org.metalib.papifly.fx.ui.UiMetrics;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Canvas-based code editor component.
 * <p>
 * Renders document text via a virtualized {@link Viewport} and handles
 * keyboard/mouse input for editing, caret movement, and selection.
 * Includes a line number gutter, marker lane, search/replace overlay,
 * and go-to-line navigation.
 */
public class CodeEditor extends StackPane implements DisposableContent {

    private static final String DEFAULT_LANGUAGE = "plain-text";
    private static final double SCROLL_LINE_FACTOR = 3.0;
    private static final int MAX_RESTORED_SECONDARY_CARETS = 2_048;

    private final StringProperty filePath = new SimpleStringProperty(this, "filePath", "");
    private final IntegerProperty cursorLine = new SimpleIntegerProperty(this, "cursorLine", 0);
    private final IntegerProperty cursorColumn = new SimpleIntegerProperty(this, "cursorColumn", 0);
    private final DoubleProperty verticalScrollOffset = new SimpleDoubleProperty(this, "verticalScrollOffset", 0.0);
    private final DoubleProperty horizontalScrollOffset = new SimpleDoubleProperty(this, "horizontalScrollOffset", 0.0);
    private final BooleanProperty wordWrap = new SimpleBooleanProperty(this, "wordWrap", false);
    private final BooleanProperty autoDetectLanguage = new SimpleBooleanProperty(this, "autoDetectLanguage", false);
    private final StringProperty languageId = new SimpleStringProperty(this, "languageId", DEFAULT_LANGUAGE);
    private final IntegerProperty indentWidth = new SimpleIntegerProperty(this, "indentWidth", 4);
    private final BooleanProperty insertSpaces = new SimpleBooleanProperty(this, "insertSpaces", true);
    private final BooleanProperty ensureTrailingNewline =
        new SimpleBooleanProperty(this, "ensureTrailingNewline", true);
    private final BooleanProperty trimTrailingWhitespace =
        new SimpleBooleanProperty(this, "trimTrailingWhitespace", true);

    private List<Integer> foldedLines = List.of();
    private List<FoldRegionRef> foldedRegions = List.of();
    private FoldMap foldMap = FoldMap.empty();
    private boolean pendingPersistedFoldingRestore;

    private final Document document;
    private final Viewport viewport;
    private final SelectionModel selectionModel;
    private final MultiCaretModel multiCaretModel;
    private final GutterView gutterView;
    private final MarkerModel markerModel;
    private final SearchModel searchModel;
    private final SearchController searchController;
    private final GoToLineController goToLineController;
    private final EditorCommandExecutor commandExecutor;
    private final EditorStateCoordinator stateCoordinator;
    private final EditorInputController inputController;
    private final EditorEditController editController;
    private final EditorPointerController pointerController;
    private final EditorCaretCoordinator caretCoordinator;
    private final EditorNavigationController navigationController;
    private final EditorCommandRegistry commandRegistry;
    private final EditorLifecycleService lifecycleService;
    private final EditorSearchCoordinator searchCoordinator;

    private final ChangeListener<Number> caretLineListener;
    private final ChangeListener<Number> caretColumnListener = (obs, oldValue, newValue) ->
        cursorColumn.set(newValue.intValue());
    private final ChangeListener<Number> scrollOffsetListener = (obs, oldValue, newValue) ->
        applyScrollOffset(newValue.doubleValue());
    private final ChangeListener<Number> horizontalScrollOffsetListener = (obs, oldValue, newValue) ->
        applyHorizontalScrollOffset(newValue.doubleValue());
    private final ChangeListener<Boolean> wordWrapListener = (obs, oldValue, newValue) ->
        applyWordWrap(newValue);
    private final ChangeListener<String> languageListener;
    private final ChangeListener<Boolean> focusListener;
    private final DocumentChangeListener gutterWidthListener;
    private final DocumentChangeListener searchRefreshListener;
    private final PauseTransition searchRefreshDebounce;
    private final MarkerModel.MarkerChangeListener markerModelChangeListener;
    private final IncrementalLexerPipeline lexerPipeline;
    private final IncrementalFoldingPipeline foldingPipeline;

    private ObjectProperty<Theme> boundThemeProperty;
    private ChangeListener<Theme> themeChangeListener;
    private int gutterDigits;
    private boolean disposed;

    /**
     * Creates an empty editor.
     */
    public CodeEditor() {
        this(new Document());
    }

    /**
     * Creates an editor with the given document.
     *
     * @param document initial document model, or {@code null} to create an empty document
     */
    public CodeEditor(Document document) {
        this(document, null, null, null, null, null);
    }

    CodeEditor(
        Document document,
        SearchModel searchModel,
        SearchController searchController,
        GoToLineController goToLineController,
        BiFunction<Document, Consumer<TokenMap>, IncrementalLexerPipeline> lexerPipelineFactory,
        LineEditService lineEditService
    ) {
        this.document = document == null ? new Document() : document;
        this.selectionModel = new SelectionModel();
        this.multiCaretModel = new MultiCaretModel(selectionModel);
        this.viewport = new Viewport(selectionModel);
        this.viewport.setMultiCaretModel(multiCaretModel);
        this.viewport.setDocument(this.document);
        LineEditService resolvedLineEditService = lineEditService == null ? new LineEditService() : lineEditService;
        this.commandRegistry = new EditorCommandRegistry();
        this.lifecycleService = new EditorLifecycleService();

        // Gutter
        this.markerModel = new MarkerModel();
        this.gutterView = new GutterView(viewport.getGlyphCache());
        this.gutterView.setDocument(this.document);
        this.gutterView.setMarkerModel(markerModel);
        this.gutterView.setWrapMap(viewport.getWrapMap());
        this.gutterView.setVisibleLineMap(viewport.getVisibleLineMap());
        this.gutterView.setFoldMap(foldMap);
        this.gutterView.setOnFoldToggle(this::toggleFoldAtLine);
        this.gutterView.setWordWrap(wordWrap.get());
        this.gutterDigits = computeGutterDigits(this.document.getLineCount());
        this.gutterWidthListener = event -> {
            refreshGutterWidthIfNeeded();
            gutterView.markDirty();
        };
        this.markerModelChangeListener = gutterView::markDirty;
        this.caretLineListener = (obs, oldValue, newValue) -> {
            cursorLine.set(newValue.intValue());
            gutterView.setActiveLineIndex(newValue.intValue());
        };
        this.caretCoordinator = new EditorCaretCoordinator(
            this.document,
            this.selectionModel,
            this.viewport,
            this.gutterView,
            this.verticalScrollOffset,
            this.horizontalScrollOffset,
            () -> disposed
        );
        this.stateCoordinator = new EditorStateCoordinator(
            this.document,
            this.selectionModel,
            this.multiCaretModel,
            this.viewport,
            MAX_RESTORED_SECONDARY_CARETS,
            caretCoordinator::clearPreferredVerticalColumn
        );

        // Search
        this.searchModel = searchModel == null ? new SearchModel() : searchModel;
        this.searchController = searchController == null ? new SearchController(this.searchModel) : searchController;
        this.goToLineController = goToLineController == null ? new GoToLineController() : goToLineController;
        this.goToLineController.setOnGoToLine(this::goToLine);
        this.goToLineController.setOnClose(this::onGoToLineClosed);
        this.searchCoordinator = new EditorSearchCoordinator(
            this.document,
            this.selectionModel,
            this.searchModel,
            this.searchController,
            this.viewport,
            this::moveCaretAndReveal,
            this::requestFocus
        );
        this.searchCoordinator.bind();

        // Debounced search refresh on document edits
        this.searchRefreshDebounce = new PauseTransition(Duration.millis(150));
        this.searchRefreshDebounce.setOnFinished(evt -> searchCoordinator.refreshIfOpen());
        this.searchRefreshListener = event -> {
            if (this.searchController.isOpen()) {
                searchRefreshDebounce.playFromStart();
            }
        };

        // Layout: gutter left, viewport center, search overlay on top
        BorderPane editorArea = new BorderPane();
        editorArea.setLeft(gutterView);
        editorArea.setCenter(viewport);

        setMinSize(0, 0);
        setPrefSize(640, 480);
        setFocusTraversable(true);
        getChildren().add(editorArea);

        // Search overlay anchored to top-right
        StackPane.setAlignment(this.searchController, Pos.TOP_RIGHT);
        getChildren().add(this.searchController);
        // Go-to-line overlay anchored to top-right
        StackPane.setAlignment(this.goToLineController, Pos.TOP_RIGHT);
        getChildren().add(this.goToLineController);
        this.viewport.setOnScrollbarVisibilityChanged(this::updateOverlayMargins);
        updateOverlayMargins();

        BiFunction<Document, Consumer<TokenMap>, IncrementalLexerPipeline> resolvedLexerPipelineFactory =
            lexerPipelineFactory == null ? IncrementalLexerPipeline::new : lexerPipelineFactory;
        this.lexerPipeline = resolvedLexerPipelineFactory.apply(this.document, viewport::setTokenMap);
        this.foldingPipeline = new IncrementalFoldingPipeline(this.document, lexerPipeline::getTokenMap, this::applyFoldMap);
        this.languageListener = (obs, oldValue, newValue) -> {
            lexerPipeline.setLanguageId(newValue);
            foldingPipeline.setLanguageId(newValue);
        };
        this.focusListener = (obs, oldFocused, focused) -> viewport.setCaretBlinkActive(focused);
        lexerPipeline.setLanguageId(languageId.get());
        foldingPipeline.setLanguageId(languageId.get());
        OccurrenceSelectionService resolvedOccurrenceSelectionService = new OccurrenceSelectionService(
            this.document,
            this.selectionModel,
            this.multiCaretModel,
            viewport::markDirty
        );
        this.editController = createEditController();
        this.navigationController = new EditorNavigationController(
            this.document,
            this.selectionModel,
            this.multiCaretModel,
            resolvedLineEditService,
            resolvedOccurrenceSelectionService,
            this.caretCoordinator,
            this.viewport,
            this.viewport::markDirty,
            this::setVerticalScrollOffset,
            this.viewport::getScrollOffset
        );
        this.pointerController = createPointerController();
        this.commandExecutor = createCommandExecutor();
        this.inputController = createInputController();
        lifecycleService.bindListeners(
            selectionModel,
            caretLineListener,
            caretColumnListener,
            this.document,
            gutterWidthListener,
            searchRefreshListener,
            markerModel,
            markerModelChangeListener,
            verticalScrollOffset,
            scrollOffsetListener,
            horizontalScrollOffset,
            horizontalScrollOffsetListener,
            wordWrap,
            wordWrapListener,
            languageId,
            languageListener
        );
        lifecycleService.bindInputHandlers(
            this,
            event -> inputController.handleKeyPressed(event),
            event -> inputController.handleKeyTyped(event),
            event -> pointerController.handleMousePressed(event),
            event -> pointerController.handleMouseDragged(event),
            event -> pointerController.handleMouseReleased(event),
            event -> pointerController.handleMouseMoved(event),
            event -> pointerController.handleScroll(event),
            focusListener,
            () -> viewport.setCaretBlinkActive(isFocused())
        );
    }

    /**
     * Returns the document model.
     *
     * @return active document model
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Convenience: sets document text content.
     *
     * @param text replacement document content
     */
    public void setText(String text) {
        document.setText(text);
        foldedLines = List.of();
        foldedRegions = List.of();
        applyFoldMapAndSyncPipeline(FoldMap.empty());
        caretCoordinator.clearPreferredVerticalColumn();
        selectionModel.moveCaret(0, 0);
        setVerticalScrollOffset(0);
        setHorizontalScrollOffset(0);
        gutterView.recomputeWidth();
    }

    /**
     * Convenience: returns document text content.
     *
     * @return full document text
     */
    public String getText() {
        return document.getText();
    }

    /**
     * Returns the viewport for direct access.
     *
     * @return viewport instance used to render the document
     */
    public Viewport getViewport() {
        return viewport;
    }

    /**
     * Returns the selection model.
     *
     * @return primary selection model
     */
    public SelectionModel getSelectionModel() {
        return selectionModel;
    }

    /**
     * Returns the multi-caret model.
     *
     * @return multi-caret model for secondary carets
     */
    public MultiCaretModel getMultiCaretModel() {
        return multiCaretModel;
    }

    /**
     * Returns the gutter view.
     *
     * @return gutter view bound to this editor
     */
    public GutterView getGutterView() {
        return gutterView;
    }

    /**
     * Returns the marker model.
     *
     * @return marker model used by the gutter
     */
    public MarkerModel getMarkerModel() {
        return markerModel;
    }

    /**
     * Returns the search model.
     *
     * @return search model backing the overlay
     */
    public SearchModel getSearchModel() {
        return searchModel;
    }

    /**
     * Returns the search controller (overlay UI).
     *
     * @return search overlay controller
     */
    public SearchController getSearchController() {
        return searchController;
    }

    /**
     * Returns the go-to-line overlay controller.
     *
     * @return go-to-line overlay controller
     */
    public GoToLineController getGoToLineController() {
        return goToLineController;
    }

    /**
     * Binds this editor to a docking {@link Theme} property.
     * <p>
     * The editor listens for changes and maps each new {@link Theme} to a
     * {@link CodeEditorTheme} via {@link CodeEditorThemeMapper}, refreshing
     * all visual components at runtime.
     *
     * @param themeProperty observable docking theme source, or {@code null}
     */
    public void bindThemeProperty(ObjectProperty<Theme> themeProperty) {
        unbindThemeProperty();
        if (themeProperty == null) {
            return;
        }
        this.boundThemeProperty = themeProperty;
        this.themeChangeListener = (obs, oldTheme, newTheme) -> applyDockingTheme(newTheme);
        themeProperty.addListener(themeChangeListener);
        applyDockingTheme(themeProperty.get());
    }

    /**
     * Unbinds a previously bound docking theme property.
     */
    public void unbindThemeProperty() {
        if (boundThemeProperty != null && themeChangeListener != null) {
            boundThemeProperty.removeListener(themeChangeListener);
        }
        boundThemeProperty = null;
        themeChangeListener = null;
    }

    /**
     * Directly applies a {@link CodeEditorTheme} to the editor and its sub-components.
     *
     * @param editorTheme theme to apply, or {@code null} to use the default dark theme
     */
    public void setEditorTheme(CodeEditorTheme editorTheme) {
        CodeEditorTheme resolved = editorTheme == null ? CodeEditorTheme.dark() : editorTheme;
        viewport.setTheme(resolved);
        gutterView.setTheme(resolved);
        searchController.setTheme(resolved);
        goToLineController.setTheme(resolved);
    }

    /**
     * Returns the current editor theme from the viewport.
     *
     * @return current effective editor theme
     */
    public CodeEditorTheme getEditorTheme() {
        return viewport.getTheme();
    }

    private void applyDockingTheme(Theme dockingTheme) {
        setEditorTheme(CodeEditorThemeMapper.map(dockingTheme));
    }

    /**
     * Opens the search/replace overlay. Shortcut: Ctrl/Cmd+F.
     */
    public void openSearch() {
        goToLineController.close();
        String selectedText = selectionModel.hasSelection()
            ? selectionModel.getSelectedText(document)
            : null;
        searchController.open(selectedText);
    }

    /**
     * Opens the search/replace overlay in replace mode. Shortcut: Ctrl+H / Cmd+Option+F.
     */
    public void openReplace() {
        goToLineController.close();
        String selectedText = selectionModel.hasSelection()
            ? selectionModel.getSelectedText(document)
            : null;
        searchController.openInReplaceMode(selectedText);
    }

    /**
     * Opens a go-to-line dialog. Shortcut: Ctrl/Cmd+G.
     */
    public void goToLine() {
        searchController.close();
        goToLineController.open(selectionModel.getCaretLine() + 1, document.getLineCount());
    }

    /**
     * Navigates to the specified 1-based line number.
     *
     * @param lineNumber one-based target line number
     */
    public void goToLine(int lineNumber) {
        int targetLine = Math.max(1, Math.min(lineNumber, document.getLineCount()));
        moveCaretAndReveal(targetLine - 1, 0);
    }

    // --- Key handling ---

    /**
     * Dispatches an {@link EditorCommand} to the appropriate handler method.
     */
    void executeCommand(EditorCommand cmd) {
        commandExecutor.execute(cmd);
    }

    private EditorCommandExecutor createCommandExecutor() {
        EditorCommandExecutor executor = new EditorCommandExecutor(
            multiCaretModel,
            caretCoordinator::clearPreferredVerticalColumn,
            commandRegistry::isVerticalCaretCommand,
            () -> disposed
        );
        commandRegistry.registerDefault(
            executor,
            navigationController,
            editController,
            this::openSearch,
            this::openReplace,
            this::goToLine,
            () -> toggleFoldAtLine(selectionModel.getCaretLine()),
            this::foldAll,
            this::unfoldAll,
            () -> foldRecursivelyAtLine(selectionModel.getCaretLine()),
            () -> unfoldRecursivelyAtLine(selectionModel.getCaretLine())
        );
        return executor;
    }

    private EditorInputController createInputController() {
        return new EditorInputController(
            () -> disposed,
            searchController::isOpen,
            searchController::close,
            goToLineController::isOpen,
            goToLineController::close,
            () -> (searchController.isOpen() && searchController.isFocusWithin())
                || (goToLineController.isOpen() && goToLineController.isFocusWithin()),
            this::requestFocus,
            viewport::resetCaretBlink,
            this::executeCommand,
            editController::insertTypedCharacter
        );
    }

    private EditorEditController createEditController() {
        return new EditorEditController(
            document,
            selectionModel,
            multiCaretModel,
            viewport::markDirty,
            caretCoordinator::moveCaretToOffset,
            () -> Clipboard.getSystemClipboard().getString(),
            this::putClipboardText
        );
    }

    private EditorPointerController createPointerController() {
        return new EditorPointerController(
            () -> disposed,
            caretCoordinator::clearPreferredVerticalColumn,
            this::requestFocus,
            viewport::resetCaretBlink,
            viewport,
            document,
            selectionModel,
            multiCaretModel,
            viewport::markDirty,
            this::setVerticalScrollOffset,
            this::setHorizontalScrollOffset,
            this::isWordWrap,
            SCROLL_LINE_FACTOR
        );
    }

    private void putClipboardText(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text == null ? "" : text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void onGoToLineClosed() {
        requestFocus();
    }

    private void applyScrollOffset(double requestedOffset) {
        caretCoordinator.applyScrollOffset(requestedOffset);
    }

    private void applyHorizontalScrollOffset(double requestedOffset) {
        caretCoordinator.applyHorizontalScrollOffset(requestedOffset);
    }

    private void applyWordWrap(boolean enabled) {
        viewport.setWordWrap(enabled);
        gutterView.setWordWrap(enabled);
        if (enabled) {
            setHorizontalScrollOffset(0.0);
        } else {
            syncHorizontalScrollOffsetFromViewport();
        }
        updateOverlayMargins();
    }

    private void syncVerticalScrollOffsetFromViewport() {
        caretCoordinator.syncVerticalScrollOffsetFromViewport();
    }

    private void syncHorizontalScrollOffsetFromViewport() {
        caretCoordinator.syncHorizontalScrollOffsetFromViewport();
    }

    private void updateOverlayMargins() {
        double rightInset = viewport.isVerticalScrollbarVisible()
            ? Viewport.SCROLLBAR_WIDTH + UiMetrics.SPACE_2
            : UiMetrics.SPACE_4;
        StackPane.setMargin(this.searchController, new Insets(0, rightInset, 0, 0));
        StackPane.setMargin(this.goToLineController, new Insets(UiMetrics.SPACE_2, rightInset, 0, 0));
    }

    // --- State properties ---

    /**
     * Captures current editor state into a serializable DTO.
     *
     * @return serialized editor state snapshot
     */
    public EditorStateData captureState() {
        syncVerticalScrollOffsetFromViewport();
        syncHorizontalScrollOffsetFromViewport();
        return stateCoordinator.captureState(
            filePath::get,
            languageId::get,
            this::getFoldedLines,
            this::getFoldedRegions,
            this::isWordWrap
        );
    }

    /**
     * Applies state to the editor.
     *
     * @param state serialized state to apply; {@code null} leaves current state unchanged
     */
    public void applyState(EditorStateData state) {
        pendingPersistedFoldingRestore = true;
        stateCoordinator.applyState(
            state,
            this::setFilePath,
            this::setLanguageId,
            this::setFoldedLines,
            this::setFoldedRegions,
            this::setWordWrap,
            this::setVerticalScrollOffset,
            this::setHorizontalScrollOffset
        );
        if (state == null) {
            pendingPersistedFoldingRestore = false;
            return;
        }
        Platform.runLater(() -> {
            if (disposed) {
                return;
            }
            setVerticalScrollOffset(state.verticalScrollOffset());
            setHorizontalScrollOffset(state.horizontalScrollOffset());
        });
    }

    /**
     * Returns the logical file path associated with this editor.
     *
     * @return file path string, never {@code null}
     */
    public String getFilePath() {
        return filePath.get();
    }

    /**
     * Sets the logical file path associated with this editor.
     *
     * @param filePath file path to store, {@code null} is normalized to empty string
     */
    public void setFilePath(String filePath) {
        this.filePath.set(filePath == null ? "" : filePath);
        if (isAutoDetectLanguage()) {
            detectLanguageFromFilePath();
        }
    }

    /**
     * Returns the observable file-path property.
     *
     * @return observable file-path property
     */
    public StringProperty filePathProperty() {
        return filePath;
    }

    /**
     * Returns the current primary caret line.
     *
     * @return zero-based caret line
     */
    public int getCursorLine() {
        return cursorLine.get();
    }

    /**
     * Moves the primary caret to the given line, preserving current column.
     *
     * @param cursorLine target zero-based line index
     */
    public void setCursorLine(int cursorLine) {
        stateCoordinator.movePrimaryCaret(cursorLine, selectionModel.getCaretColumn());
    }

    /**
     * Returns the observable primary caret-line property.
     *
     * @return observable caret-line property
     */
    public IntegerProperty cursorLineProperty() {
        return cursorLine;
    }

    /**
     * Returns the current primary caret column.
     *
     * @return zero-based caret column
     */
    public int getCursorColumn() {
        return cursorColumn.get();
    }

    /**
     * Moves the primary caret to the given column on the current line.
     *
     * @param cursorColumn target zero-based column index
     */
    public void setCursorColumn(int cursorColumn) {
        stateCoordinator.movePrimaryCaret(selectionModel.getCaretLine(), cursorColumn);
    }

    /**
     * Returns the observable primary caret-column property.
     *
     * @return observable caret-column property
     */
    public IntegerProperty cursorColumnProperty() {
        return cursorColumn;
    }

    /**
     * Returns the current vertical scroll offset.
     *
     * @return vertical scroll offset in pixels
     */
    public double getVerticalScrollOffset() {
        return verticalScrollOffset.get();
    }

    /**
     * Sets the vertical scroll offset.
     *
     * @param verticalScrollOffset requested vertical offset in pixels
     */
    public void setVerticalScrollOffset(double verticalScrollOffset) {
        double safeOffset = Math.max(0.0, verticalScrollOffset);
        if (Double.compare(this.verticalScrollOffset.get(), safeOffset) == 0) {
            return;
        }
        this.verticalScrollOffset.set(safeOffset);
    }

    /**
     * Returns the observable vertical scroll-offset property.
     *
     * @return observable vertical scroll-offset property
     */
    public DoubleProperty verticalScrollOffsetProperty() {
        return verticalScrollOffset;
    }

    /**
     * Returns the current horizontal scroll offset.
     *
     * @return horizontal scroll offset in pixels
     */
    public double getHorizontalScrollOffset() {
        return horizontalScrollOffset.get();
    }

    /**
     * Sets the horizontal scroll offset.
     *
     * @param horizontalScrollOffset requested horizontal offset in pixels
     */
    public void setHorizontalScrollOffset(double horizontalScrollOffset) {
        double safeOffset = wordWrap.get() ? 0.0 : Math.max(0.0, horizontalScrollOffset);
        if (Double.compare(this.horizontalScrollOffset.get(), safeOffset) == 0) {
            return;
        }
        this.horizontalScrollOffset.set(safeOffset);
    }

    /**
     * Returns the observable horizontal scroll-offset property.
     *
     * @return observable horizontal scroll-offset property
     */
    public DoubleProperty horizontalScrollOffsetProperty() {
        return horizontalScrollOffset;
    }

    /**
     * Returns whether word-wrap is enabled.
     *
     * @return {@code true} when soft wrapping is enabled
     */
    public boolean isWordWrap() {
        return wordWrap.get();
    }

    /**
     * Enables or disables word-wrap mode.
     *
     * @param wordWrap {@code true} to enable wrapping
     */
    public void setWordWrap(boolean wordWrap) {
        if (wordWrap) {
            setHorizontalScrollOffset(0.0);
        }
        this.wordWrap.set(wordWrap);
    }

    /**
     * Returns the observable word-wrap property.
     *
     * @return observable word-wrap property
     */
    public BooleanProperty wordWrapProperty() {
        return wordWrap;
    }

    /**
     * Returns the active lexer language id.
     *
     * @return language id string
     */
    public String getLanguageId() {
        return languageId.get();
    }

    /**
     * Sets the active lexer language id.
     *
     * @param languageId requested language id, blank/null maps to default language
     */
    public void setLanguageId(String languageId) {
        this.languageId.set(languageId == null || languageId.isBlank() ? DEFAULT_LANGUAGE : languageId);
    }

    /**
     * Returns the observable language-id property.
     *
     * @return observable language-id property
     */
    public StringProperty languageIdProperty() {
        return languageId;
    }

    public boolean isAutoDetectLanguage() {
        return autoDetectLanguage.get();
    }

    public void setAutoDetectLanguage(boolean autoDetect) {
        this.autoDetectLanguage.set(autoDetect);
    }

    public BooleanProperty autoDetectLanguageProperty() {
        return autoDetectLanguage;
    }

    /**
     * Returns the effective indentation width for the current language.
     *
     * @return indentation width in columns
     */
    public int getIndentWidth() {
        return indentWidth.get();
    }

    /**
     * Sets the effective indentation width.
     *
     * @param indentWidth indentation width in columns, clamped to 1-16
     */
    public void setIndentWidth(int indentWidth) {
        this.indentWidth.set(Math.max(1, Math.min(16, indentWidth)));
    }

    /**
     * Indentation width property.
     *
     * @return indentation width property
     */
    public IntegerProperty indentWidthProperty() {
        return indentWidth;
    }

    /**
     * Returns whether indentation should insert spaces.
     *
     * @return true when spaces are preferred over tab characters
     */
    public boolean isInsertSpaces() {
        return insertSpaces.get();
    }

    /**
     * Sets whether indentation should insert spaces.
     *
     * @param insertSpaces true to prefer spaces
     */
    public void setInsertSpaces(boolean insertSpaces) {
        this.insertSpaces.set(insertSpaces);
    }

    /**
     * Insert-spaces property.
     *
     * @return insert-spaces property
     */
    public BooleanProperty insertSpacesProperty() {
        return insertSpaces;
    }

    /**
     * Returns whether saved content should end with a newline.
     *
     * @return true when trailing newline is preferred
     */
    public boolean isEnsureTrailingNewline() {
        return ensureTrailingNewline.get();
    }

    /**
     * Sets whether saved content should end with a newline.
     *
     * @param ensureTrailingNewline true to prefer a trailing newline
     */
    public void setEnsureTrailingNewline(boolean ensureTrailingNewline) {
        this.ensureTrailingNewline.set(ensureTrailingNewline);
    }

    /**
     * Ensure-trailing-newline property.
     *
     * @return ensure-trailing-newline property
     */
    public BooleanProperty ensureTrailingNewlineProperty() {
        return ensureTrailingNewline;
    }

    /**
     * Returns whether saved content should trim trailing whitespace.
     *
     * @return true when trailing whitespace should be trimmed
     */
    public boolean isTrimTrailingWhitespace() {
        return trimTrailingWhitespace.get();
    }

    /**
     * Sets whether saved content should trim trailing whitespace.
     *
     * @param trimTrailingWhitespace true to prefer trimming trailing whitespace
     */
    public void setTrimTrailingWhitespace(boolean trimTrailingWhitespace) {
        this.trimTrailingWhitespace.set(trimTrailingWhitespace);
    }

    /**
     * Trim-trailing-whitespace property.
     *
     * @return trim-trailing-whitespace property
     */
    public BooleanProperty trimTrailingWhitespaceProperty() {
        return trimTrailingWhitespace;
    }

    public boolean detectLanguageFromFilePath() {
        String path = getFilePath();
        if (path == null || path.isBlank()) {
            return false;
        }
        return LanguageSupportRegistry.defaultRegistry()
            .detectLanguageId(path)
            .map(id -> {
                setLanguageId(id);
                return true;
            })
            .orElse(false);
    }

    /**
     * Returns the currently folded line indices.
     *
     * @return immutable list of folded line indices
     */
    public List<Integer> getFoldedLines() {
        return foldedLines;
    }

    /**
     * Replaces the set of folded line indices.
     *
     * @param foldedLines line indices to treat as folded
     */
    public void setFoldedLines(List<Integer> foldedLines) {
        this.foldedLines = foldedLines == null ? List.of() : List.copyOf(foldedLines);
        if (foldedRegions.isEmpty()) {
            pendingPersistedFoldingRestore = true;
            applyPersistedFoldingIfReady();
        }
    }

    public List<FoldRegionRef> getFoldedRegions() {
        return foldedRegions;
    }

    public void setFoldedRegions(List<FoldRegionRef> foldedRegions) {
        this.foldedRegions = foldedRegions == null ? List.of() : List.copyOf(foldedRegions);
        pendingPersistedFoldingRestore = true;
        applyPersistedFoldingIfReady();
    }

    public void toggleFoldAtLine(int line) {
        revealLine(line);
        FoldMap next = foldMap.toggleAtHeaderLine(line);
        applyFoldMapAndSyncPipeline(next);
    }

    public void foldRegion(int line) {
        revealLine(line);
        FoldMap next = foldMap.withCollapsedHeaders(withAdded(foldMap.collapsedHeaderLines(), line));
        applyFoldMapAndSyncPipeline(next);
    }

    public void unfoldRegion(int line) {
        FoldMap next = foldMap.withCollapsedHeaders(withRemoved(foldMap.collapsedHeaderLines(), line));
        applyFoldMapAndSyncPipeline(next);
    }

    public void foldAll() {
        applyFoldMapAndSyncPipeline(foldMap.collapseAll());
    }

    public void unfoldAll() {
        applyFoldMapAndSyncPipeline(foldMap.expandAll());
    }

    public void foldRecursivelyAtLine(int line) {
        revealLine(line);
        FoldRegion root = foldMap.headerRegionAt(line);
        if (root == null) {
            foldRegion(line);
            return;
        }
        Set<Integer> headers = new LinkedHashSet<>(foldMap.collapsedHeaderLines());
        for (FoldRegion region : foldMap.regions()) {
            if (region.startLine() < root.startLine() || region.endLine() > root.endLine()) {
                continue;
            }
            headers.add(region.startLine());
        }
        applyFoldMapAndSyncPipeline(foldMap.withCollapsedHeaders(headers));
    }

    public void unfoldRecursivelyAtLine(int line) {
        FoldRegion root = foldMap.headerRegionAt(line);
        if (root == null) {
            unfoldRegion(line);
            return;
        }
        Set<Integer> headers = new LinkedHashSet<>(foldMap.collapsedHeaderLines());
        for (FoldRegion region : foldMap.regions()) {
            if (region.startLine() < root.startLine() || region.endLine() > root.endLine()) {
                continue;
            }
            headers.remove(region.startLine());
        }
        applyFoldMapAndSyncPipeline(foldMap.withCollapsedHeaders(headers));
    }

    private void moveCaretAndReveal(int line, int column) {
        revealLine(line);
        caretCoordinator.moveCaret(line, column, false);
    }

    private void applyFoldMap(FoldMap nextMap) {
        applyFoldMap(nextMap, false);
    }

    private void applyFoldMapAndSyncPipeline(FoldMap nextMap) {
        applyFoldMap(nextMap, true);
    }

    private void applyFoldMap(FoldMap nextMap, boolean syncPipeline) {
        FoldMap safeMap = nextMap == null ? FoldMap.empty() : nextMap;
        if (pendingPersistedFoldingRestore) {
            safeMap = applyPersistedFolding(safeMap);
            pendingPersistedFoldingRestore = false;
        }
        foldMap = safeMap;
        viewport.setFoldMap(safeMap);
        gutterView.setFoldMap(safeMap);
        ensurePrimaryCaretVisibleAfterFoldChange();
        if (syncPipeline || safeMap.hasCollapsedRegions()) {
            updatePersistedFoldStateFromMap(safeMap);
        }
        if (syncPipeline) {
            foldingPipeline.setCollapsedHeaders(safeMap.collapsedHeaderLines());
        }
        viewport.markDirty();
        gutterView.markDirty();
    }

    private void applyPersistedFoldingIfReady() {
        if (disposed || foldMap == null || foldMap.regions().isEmpty()) {
            return;
        }
        FoldMap mapped = applyPersistedFolding(foldMap);
        pendingPersistedFoldingRestore = false;
        applyFoldMapAndSyncPipeline(mapped);
    }

    private FoldMap applyPersistedFolding(FoldMap baseMap) {
        if (baseMap == null || baseMap.regions().isEmpty()) {
            return baseMap == null ? FoldMap.empty() : baseMap;
        }
        Set<Integer> headers = new LinkedHashSet<>();
        if (foldedRegions != null && !foldedRegions.isEmpty()) {
            for (FoldRegionRef ref : foldedRegions) {
                if (ref == null) {
                    continue;
                }
                FoldRegion region = baseMap.headerRegionAt(ref.startLine());
                if (region == null) {
                    continue;
                }
                if (!ref.kind().isBlank() && !region.kind().name().equals(ref.kind())) {
                    continue;
                }
                headers.add(region.startLine());
            }
        } else if (foldedLines != null && !foldedLines.isEmpty()) {
            headers.addAll(foldedLines);
        }
        return baseMap.withCollapsedHeaders(headers);
    }

    private void updatePersistedFoldStateFromMap(FoldMap map) {
        foldedLines = map.collapsedHeaderLines().stream().sorted().toList();
        foldedRegions = map.collapsedRegions().stream()
            .map(region -> new FoldRegionRef(region.startLine(), region.kind().name(), region.endLine()))
            .toList();
    }

    private void ensurePrimaryCaretVisibleAfterFoldChange() {
        int caretLine = selectionModel.getCaretLine();
        if (!viewport.isLogicalLineHidden(caretLine)) {
            return;
        }
        int targetLine = viewport.nearestVisibleLogicalLine(caretLine);
        int targetColumn = Math.min(selectionModel.getCaretColumn(), document.getLineText(targetLine).length());
        caretCoordinator.moveCaret(targetLine, targetColumn, false);
    }

    private void revealLine(int line) {
        if (line < 0 || line >= document.getLineCount()) {
            return;
        }
        FoldMap next = foldMap;
        boolean changed = false;
        boolean searching = true;
        while (searching) {
            searching = false;
            for (FoldRegion region : next.regions()) {
                if (line <= region.startLine() || line > region.endLine()) {
                    continue;
                }
                if (!next.isCollapsedHeader(region.startLine())) {
                    continue;
                }
                next = next.toggleAtHeaderLine(region.startLine());
                changed = true;
                searching = true;
                break;
            }
        }
        if (changed) {
            applyFoldMapAndSyncPipeline(next);
        }
    }

    private static Set<Integer> withAdded(Set<Integer> values, int line) {
        Set<Integer> updated = new LinkedHashSet<>(values);
        updated.add(line);
        return updated;
    }

    private static Set<Integer> withRemoved(Set<Integer> values, int line) {
        Set<Integer> updated = new LinkedHashSet<>(values);
        updated.remove(line);
        return updated;
    }

    /**
     * Releases listeners and rendering resources associated with this editor.
     */
    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        pointerController.dispose();
        multiCaretModel.clearSecondaryCarets();
        searchCoordinator.clearAndDispose();
        goToLineController.setOnGoToLine(null);
        goToLineController.setOnClose(null);
        goToLineController.close();
        searchRefreshDebounce.stop();
        unbindThemeProperty();
        lifecycleService.unbindInputHandlers(this, focusListener);
        viewport.setCaretBlinkActive(false);
        lifecycleService.unbindListeners(
            selectionModel,
            caretLineListener,
            caretColumnListener,
            document,
            gutterWidthListener,
            searchRefreshListener,
            markerModel,
            markerModelChangeListener,
            verticalScrollOffset,
            scrollOffsetListener,
            horizontalScrollOffset,
            horizontalScrollOffsetListener,
            wordWrap,
            wordWrapListener,
            languageId,
            languageListener
        );
        lexerPipeline.dispose();
        foldingPipeline.dispose();
        viewport.dispose();
    }

    private void refreshGutterWidthIfNeeded() {
        int nextDigits = computeGutterDigits(document.getLineCount());
        if (nextDigits == gutterDigits) {
            return;
        }
        gutterDigits = nextDigits;
        if (Platform.isFxApplicationThread()) {
            gutterView.recomputeWidth();
            return;
        }
        Platform.runLater(gutterView::recomputeWidth);
    }

    private static int computeGutterDigits(int lineCount) {
        return Math.max(2, String.valueOf(Math.max(1, lineCount)).length());
    }
}
