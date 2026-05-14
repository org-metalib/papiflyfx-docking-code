package org.metalib.papifly.fx.code.search;

import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;
import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.theme.CodeEditorTheme;
import org.metalib.papifly.fx.searchui.SearchOverlayBase;
import org.metalib.papifly.fx.ui.UiChipToggle;
import org.metalib.papifly.fx.ui.UiCommonPalette;
import org.metalib.papifly.fx.ui.UiCommonThemeSupport;
import org.metalib.papifly.fx.ui.UiMetrics;
import org.metalib.papifly.fx.ui.UiStyleSupport;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Overlay UI for find/replace functionality.
 * <p>
 * Uses compact, chip-style controls and icon-only navigation actions while
 * keeping search model behavior and keyboard flow stable.
 */
public class SearchController extends SearchOverlayBase {

    private static final String STYLESHEET_NAME = "search-overlay.css";
    private static final PseudoClass NO_RESULTS_PSEUDO_CLASS = PseudoClass.getPseudoClass("no-results");
    private static final double FIELD_HEIGHT = UiMetrics.CONTROL_HEIGHT_COMPACT;
    private static final double OVERLAY_MIN_WIDTH = UiMetrics.SPACE_6 * 22.0;
    private static final double OVERLAY_PREF_WIDTH = UiMetrics.SPACE_6 * 26.0;
    private static final double OVERLAY_MAX_WIDTH = UiMetrics.SPACE_6 * 32.0;
    private static final double RESULT_LABEL_MIN_WIDTH = UiMetrics.SPACE_4 * 5.0;

    private CodeEditorTheme theme = CodeEditorTheme.dark();
    private final SearchModel searchModel;
    private final TextField searchField;
    private final TextField replaceField;
    private final Label matchCountLabel;
    private final ToggleButton regexToggle;
    private final ToggleButton caseSensitiveToggle;
    private final ToggleButton wholeWordToggle;
    private final ToggleButton inSelectionToggle;
    private final ToggleButton preserveCaseToggle;
    private final Button skipButton;
    private final Button replaceButton;
    private final Button replaceAllButton;
    private final Button chevronButton;
    private final SVGPath chevronIcon;
    private final HBox replaceRow;
    private final List<SVGPath> iconNodes = new ArrayList<>();

    private boolean replaceMode;
    private boolean programmaticUpdate;
    private Document document;
    private Supplier<int[]> selectionRangeSupplier;
    private Consumer<SearchMatch> onNavigate;
    private Runnable onClose;
    private Runnable onSearchChanged;

    /**
     * Creates a search overlay controller.
     *
     * @param searchModel backing search/replace model
     */
    public SearchController(SearchModel searchModel) {
        this.searchModel = searchModel;

        getStyleClass().addAll("pf-search-overlay", "pf-ui-popup-surface");
        setPadding(new Insets(UiMetrics.SPACE_1, UiMetrics.SPACE_2, UiMetrics.SPACE_1, UiMetrics.SPACE_2));
        setSpacing(UiMetrics.SPACE_1);
        setMinWidth(OVERLAY_MIN_WIDTH);
        setPrefWidth(OVERLAY_PREF_WIDTH);
        setMaxWidth(OVERLAY_MAX_WIDTH);
        setMaxHeight(Region.USE_PREF_SIZE);
        setManaged(false);
        setVisible(false);
        UiStyleSupport.ensureCommonStylesheetLoaded(this);
        ensureStylesheetLoaded();

        searchField = createTextField("Find");
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (programmaticUpdate) {
                return;
            }
            searchModel.setQuery(newValue);
            executeSearch();
        });
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (e.isShiftDown()) {
                    navigatePrevious();
                } else {
                    navigateNext();
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                close();
                e.consume();
            }
        });

        replaceField = createTextField("Replace");
        replaceField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (programmaticUpdate) {
                return;
            }
            searchModel.setReplacement(newValue);
        });
        replaceField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                replaceCurrent();
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                close();
                e.consume();
            }
        });

        matchCountLabel = new Label();
        matchCountLabel.getStyleClass().addAll("pf-search-result-label", "pf-ui-result-label");
        matchCountLabel.setMinWidth(RESULT_LABEL_MIN_WIDTH);
        matchCountLabel.setAlignment(Pos.CENTER_RIGHT);

        caseSensitiveToggle = createChipToggle("Aa");
        caseSensitiveToggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            if (programmaticUpdate) {
                return;
            }
            searchModel.setCaseSensitive(selected);
            executeSearch();
        });

        wholeWordToggle = createChipToggle("W");
        wholeWordToggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            if (programmaticUpdate) {
                return;
            }
            searchModel.setWholeWord(selected);
            executeSearch();
        });

        regexToggle = createChipToggle(".*");
        regexToggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            if (programmaticUpdate) {
                return;
            }
            searchModel.setRegexMode(selected);
            executeSearch();
        });

        inSelectionToggle = createChipToggle("In");
        SVGPath inSelectionIcon = createIcon(SearchIcons.FILTER, 10);
        inSelectionToggle.setGraphic(inSelectionIcon);
        inSelectionToggle.setContentDisplay(ContentDisplay.LEFT);
        inSelectionToggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            if (programmaticUpdate) {
                return;
            }
            searchModel.setSearchInSelection(selected);
            executeSearch();
        });
        inSelectionToggle.setDisable(true);

        preserveCaseToggle = createChipToggle("Aa");
        preserveCaseToggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            if (programmaticUpdate) {
                return;
            }
            searchModel.setPreserveCase(selected);
        });

        Button prevButton = createIconButton(createIcon(SearchIcons.ARROW_UP, 10), this::navigatePrevious);
        Button nextButton = createIconButton(createIcon(SearchIcons.ARROW_DOWN, 10), this::navigateNext);
        chevronIcon = createIcon(SearchIcons.CHEVRON_RIGHT, 10);
        chevronButton = createIconButton(chevronIcon, this::toggleReplaceMode);
        Button closeButton = createIconButton(createIcon(SearchIcons.CLOSE, 10), this::close);

        skipButton = createActionButton("Skip", this::skipCurrent, true);
        replaceButton = createActionButton("Replace", this::replaceCurrent, false);
        replaceAllButton = createActionButton("All", this::replaceAll, false);

        StackPane searchInput = createSearchInput();
        HBox searchMiddle = new HBox(UiMetrics.SPACE_1, caseSensitiveToggle, wholeWordToggle, regexToggle, inSelectionToggle, matchCountLabel);
        searchMiddle.setAlignment(Pos.CENTER_LEFT);
        HBox searchRight = new HBox(UiMetrics.SPACE_1, prevButton, nextButton, chevronButton, closeButton);
        searchRight.setAlignment(Pos.CENTER_RIGHT);
        HBox searchRow = new HBox(UiMetrics.SPACE_1, searchInput, searchMiddle, searchRight);
        searchRow.getStyleClass().add("pf-search-row");
        searchRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchInput, Priority.ALWAYS);

        HBox replaceMiddle = new HBox(UiMetrics.SPACE_1, preserveCaseToggle);
        replaceMiddle.setAlignment(Pos.CENTER_LEFT);
        HBox replaceRight = new HBox(UiMetrics.SPACE_1, skipButton, replaceButton, replaceAllButton);
        replaceRight.setAlignment(Pos.CENTER_RIGHT);
        replaceRow = new HBox(UiMetrics.SPACE_1, replaceField, replaceMiddle, replaceRight);
        replaceRow.getStyleClass().add("pf-search-row");
        replaceRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(replaceField, Priority.ALWAYS);
        replaceRow.setManaged(false);
        replaceRow.setVisible(false);

        getChildren().addAll(searchRow, replaceRow);

        withProgrammaticUpdate(() -> {
            caseSensitiveToggle.setSelected(searchModel.isCaseSensitive());
            wholeWordToggle.setSelected(searchModel.isWholeWord());
            regexToggle.setSelected(searchModel.isRegexMode());
            inSelectionToggle.setSelected(searchModel.isSearchInSelection());
            preserveCaseToggle.setSelected(searchModel.isPreserveCase());
        });

        updateMatchLabel();
        applyThemeColors();
    }

    /**
     * Sets the document to search in.
     *
     * @param document document to search
     */
    public void setDocument(Document document) {
        this.document = document;
    }

    /**
     * Sets a supplier that provides active selection offsets as {start, end}.
     *
     * @param selectionRangeSupplier supplier of current selection range
     */
    public void setSelectionRangeSupplier(Supplier<int[]> selectionRangeSupplier) {
        this.selectionRangeSupplier = selectionRangeSupplier;
        refreshSelectionScope();
    }

    /**
     * Re-evaluates selection scope based on the current selection supplier.
     */
    public void refreshSelectionScope() {
        int[] scope = selectionRangeSupplier == null ? null : selectionRangeSupplier.get();
        boolean validScope = scope != null && scope.length >= 2 && scope[0] < scope[1];
        if (validScope) {
            searchModel.setSelectionScope(scope[0], scope[1]);
            inSelectionToggle.setDisable(false);
            return;
        }
        searchModel.clearSelectionScope();
        inSelectionToggle.setDisable(true);
        if (searchModel.isSearchInSelection()) {
            searchModel.setSearchInSelection(false);
            withProgrammaticUpdate(() -> inSelectionToggle.setSelected(false));
        }
    }

    /**
     * Sets the editor theme and refreshes overlay styling.
     *
     * @param theme editor theme palette
     */
    public void setTheme(CodeEditorTheme theme) {
        this.theme = theme == null ? CodeEditorTheme.dark() : theme;
        applyThemeColors();
    }

    /**
     * Returns the current editor theme.
     *
     * @return active search overlay theme
     */
    public CodeEditorTheme getTheme() {
        return theme;
    }

    /**
     * Sets the callback invoked when navigating to a match.
     *
     * @param onNavigate callback invoked for match navigation
     */
    public void setOnNavigate(Consumer<SearchMatch> onNavigate) {
        this.onNavigate = onNavigate;
    }

    /**
     * Sets the callback invoked when the search panel is closed.
     *
     * @param onClose callback invoked on close
     */
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    /**
     * Sets the callback invoked when search results change (for highlight refresh).
     *
     * @param onSearchChanged callback invoked when search state changes
     */
    public void setOnSearchChanged(Runnable onSearchChanged) {
        this.onSearchChanged = onSearchChanged;
    }

    /**
     * Shows the search overlay and focuses the search field.
     */
    public void open() {
        refreshSelectionScope();
        showOverlay();
        searchField.requestFocus();
        searchField.selectAll();
    }

    /**
     * Shows the search overlay with existing query text selected.
     *
     * @param initialQuery initial query text
     */
    public void open(String initialQuery) {
        if (initialQuery != null && !initialQuery.isEmpty()) {
            withProgrammaticUpdate(() -> searchField.setText(initialQuery));
            searchModel.setQuery(initialQuery);
            executeSearch();
        }
        open();
    }

    /**
     * Opens the search overlay directly in replace mode.
     *
     * @param initialQuery initial query text
     */
    public void openInReplaceMode(String initialQuery) {
        if (!replaceMode) {
            toggleReplaceMode();
        }
        open(initialQuery);
    }

    /**
     * Hides the search overlay and clears highlights.
     */
    public void close() {
        hideOverlay();
        searchModel.clear();
        withProgrammaticUpdate(() -> {
            searchField.clear();
            replaceField.clear();
        });
        updateMatchLabel();
        if (onSearchChanged != null) {
            onSearchChanged.run();
        }
        if (onClose != null) {
            onClose.run();
        }
    }

    /**
     * Returns true if the search overlay is currently visible.
     *
     * @return {@code true} when overlay is visible
     */
    public boolean isOpen() {
        return isVisible();
    }

    /**
     * Returns true if replace mode is active (replace row visible).
     *
     * @return {@code true} when replace row is visible
     */
    public boolean isReplaceMode() {
        return replaceMode;
    }

    /**
     * Returns the search model.
     *
     * @return backing search model
     */
    public SearchModel getSearchModel() {
        return searchModel;
    }

    /**
     * Refreshes the match label and button states from current search model state.
     */
    public void refreshMatchDisplay() {
        updateMatchLabel();
    }

    private void toggleReplaceMode() {
        replaceMode = !replaceMode;
        replaceRow.setManaged(replaceMode);
        replaceRow.setVisible(replaceMode);
        chevronIcon.setContent(replaceMode ? SearchIcons.CHEVRON_DOWN : SearchIcons.CHEVRON_RIGHT);
    }

    private void executeSearch() {
        refreshSelectionScope();
        if (document != null) {
            searchModel.search(document);
        }
        publishSearchState(true);
    }

    private void navigateNext() {
        SearchMatch match = searchModel.nextMatch();
        if (match != null && onNavigate != null) {
            onNavigate.accept(match);
        }
        updateMatchLabel();
    }

    private void navigatePrevious() {
        SearchMatch match = searchModel.previousMatch();
        if (match != null && onNavigate != null) {
            onNavigate.accept(match);
        }
        updateMatchLabel();
    }

    private void skipCurrent() {
        navigateNext();
    }

    private void replaceCurrent() {
        if (document == null) {
            return;
        }
        if (searchModel.replaceCurrent(document)) {
            publishSearchState(true);
            return;
        }
        updateMatchLabel();
    }

    private void replaceAll() {
        if (document == null) {
            return;
        }
        if (searchModel.replaceAll(document) > 0) {
            publishSearchState(true);
            return;
        }
        updateMatchLabel();
    }

    private void updateMatchLabel() {
        int count = searchModel.getMatchCount();
        boolean noResults = !searchModel.getQuery().isEmpty() && count == 0;
        if (count == 0) {
            matchCountLabel.setText(searchModel.getQuery().isEmpty() ? "" : "No results");
        } else {
            int current = searchModel.getCurrentMatchIndex() + 1;
            matchCountLabel.setText(current + " of " + count);
        }
        searchField.pseudoClassStateChanged(NO_RESULTS_PSEUDO_CLASS, noResults);
        skipButton.setDisable(count == 0);
        replaceButton.setDisable(count == 0);
        replaceAllButton.setDisable(count == 0);
    }

    private void publishSearchState(boolean navigateCurrent) {
        updateMatchLabel();
        if (onSearchChanged != null) {
            onSearchChanged.run();
        }
        if (!navigateCurrent || onNavigate == null) {
            return;
        }
        SearchMatch current = searchModel.getCurrentMatch();
        if (current != null) {
            onNavigate.accept(current);
        }
    }

    private TextField createTextField(String promptText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.getStyleClass().addAll("pf-search-field", "pf-ui-compact-field");
        field.setMinHeight(FIELD_HEIGHT);
        field.setPrefHeight(FIELD_HEIGHT);
        field.setMaxHeight(FIELD_HEIGHT);
        HBox.setHgrow(field, Priority.ALWAYS);
        return field;
    }

    private StackPane createSearchInput() {
        StackPane input = new StackPane();
        input.getStyleClass().add("pf-search-input-wrap");
        SVGPath searchIcon = createIcon(SearchIcons.SEARCH, 11);
        Label leadingIcon = new Label();
        leadingIcon.getStyleClass().add("pf-search-leading-icon");
        leadingIcon.setGraphic(searchIcon);
        leadingIcon.setMouseTransparent(true);
        searchField.setPadding(new Insets(0, UiMetrics.SPACE_2, 0, UiMetrics.SPACE_5));
        StackPane.setAlignment(searchField, Pos.CENTER_LEFT);
        StackPane.setAlignment(leadingIcon, Pos.CENTER_LEFT);
        input.getChildren().addAll(searchField, leadingIcon);
        HBox.setHgrow(input, Priority.ALWAYS);
        return input;
    }

    private ToggleButton createChipToggle(String text) {
        ToggleButton toggle = new UiChipToggle(text);
        toggle.getStyleClass().addAll("pf-search-chip", "pf-ui-chip-toggle");
        return toggle;
    }

    private Button createIconButton(SVGPath icon, Runnable action) {
        Button button = new Button();
        button.getStyleClass().addAll("pf-search-icon-button", "pf-ui-icon-button");
        button.setGraphic(icon);
        button.setOnAction(e -> action.run());
        return button;
    }

    private Button createActionButton(String text, Runnable action, boolean secondary) {
        Button button = new Button(text);
        button.getStyleClass().addAll("pf-search-action-button", "pf-ui-compact-action-button");
        if (secondary) {
            button.getStyleClass().addAll("pf-search-action-secondary", "pf-ui-compact-action-button-secondary");
        }
        button.setOnAction(e -> action.run());
        button.setDisable(true);
        return button;
    }

    private SVGPath createIcon(String svgPath, double size) {
        SVGPath icon = SearchIcons.createIcon(svgPath, size);
        icon.getStyleClass().add("pf-ui-icon");
        iconNodes.add(icon);
        return icon;
    }

    private void withProgrammaticUpdate(Runnable action) {
        boolean previous = programmaticUpdate;
        programmaticUpdate = true;
        try {
            action.run();
        } finally {
            programmaticUpdate = previous;
        }
    }

    private void ensureStylesheetLoaded() {
        URL stylesheetUrl = SearchController.class.getResource(STYLESHEET_NAME);
        if (stylesheetUrl == null) {
            return;
        }
        String stylesheet = stylesheetUrl.toExternalForm();
        if (!getStylesheets().contains(stylesheet)) {
            getStylesheets().add(stylesheet);
        }
    }

    private void applyThemeColors() {
        boolean dark = UiCommonThemeSupport.isDark(theme.editorBackground());
        UiCommonPalette palette = new UiCommonPalette(
            theme.searchOverlayBackground(),
            theme.searchOverlayPanelBorder(),
            theme.searchOverlayPrimaryText(),
            theme.searchOverlaySecondaryText(),
            theme.searchOverlayControlDisabledText(),
            theme.searchOverlayControlBackground(),
            theme.searchOverlayControlHoverBackground(),
            theme.searchOverlayControlActiveBackground(),
            theme.searchOverlayControlFocusedBorder(),
            theme.searchOverlayAccentBorder(),
            UiCommonThemeSupport.semanticColor(dark, UiCommonThemeSupport.SemanticTone.SUCCESS),
            UiCommonThemeSupport.semanticColor(dark, UiCommonThemeSupport.SemanticTone.WARNING),
            theme.searchOverlayNoResultsBorder(),
            theme.searchOverlayAccentBorder(),
            theme.searchOverlayShadowColor()
        );
        setStyle(UiStyleSupport.metricVariables()
            + UiStyleSupport.fontVariables(null)
            + UiCommonThemeSupport.themeVariables(palette));
        Color iconColor = UiStyleSupport.asColor(
            theme.searchOverlayPrimaryText(),
            UiStyleSupport.asColor(theme.editorForeground(), Color.TRANSPARENT)
        );
        for (SVGPath icon : iconNodes) {
            icon.setFill(iconColor);
        }
    }
}
