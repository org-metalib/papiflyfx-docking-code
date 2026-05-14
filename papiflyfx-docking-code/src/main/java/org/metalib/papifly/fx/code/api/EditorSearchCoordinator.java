package org.metalib.papifly.fx.code.api;

import org.metalib.papifly.fx.code.document.Document;
import org.metalib.papifly.fx.code.render.SelectionModel;
import org.metalib.papifly.fx.code.render.Viewport;
import org.metalib.papifly.fx.code.search.SearchController;
import org.metalib.papifly.fx.code.search.SearchMatch;
import org.metalib.papifly.fx.code.search.SearchModel;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Coordinates search model, overlay controller, and viewport highlights.
 */
final class EditorSearchCoordinator {

    private final Document document;
    private final SelectionModel selectionModel;
    private final SearchModel searchModel;
    private final SearchController searchController;
    private final Viewport viewport;
    private final BiConsumer<Integer, Integer> moveCaretAction;
    private final Runnable requestFocusAction;

    EditorSearchCoordinator(
        Document document,
        SelectionModel selectionModel,
        SearchModel searchModel,
        SearchController searchController,
        Viewport viewport,
        BiConsumer<Integer, Integer> moveCaretAction,
        Runnable requestFocusAction
    ) {
        this.document = Objects.requireNonNull(document, "document");
        this.selectionModel = Objects.requireNonNull(selectionModel, "selectionModel");
        this.searchModel = Objects.requireNonNull(searchModel, "searchModel");
        this.searchController = Objects.requireNonNull(searchController, "searchController");
        this.viewport = Objects.requireNonNull(viewport, "viewport");
        this.moveCaretAction = Objects.requireNonNull(moveCaretAction, "moveCaretAction");
        this.requestFocusAction = Objects.requireNonNull(requestFocusAction, "requestFocusAction");
    }

    void bind() {
        searchController.setDocument(document);
        searchController.setSelectionRangeSupplier(this::currentSelectionRange);
        searchController.setOnNavigate(this::navigateToSearchMatch);
        searchController.setOnClose(this::onSearchClosed);
        searchController.setOnSearchChanged(this::onSearchResultsChanged);
    }

    void refreshIfOpen() {
        if (!searchController.isOpen()) {
            return;
        }
        int caretOffset = selectionModel.getCaretOffset(document);
        searchController.refreshSelectionScope();
        searchModel.search(document);
        searchModel.selectNearestMatch(caretOffset);
        onSearchResultsChanged();
        searchController.refreshMatchDisplay();
    }

    void clearAndDispose() {
        searchController.setOnNavigate(null);
        searchController.setOnClose(null);
        searchController.setOnSearchChanged(null);
        searchController.setSelectionRangeSupplier(null);
        searchController.setDocument(null);
        searchController.close();
        viewport.setSearchMatches(List.of(), -1);
    }

    private int[] currentSelectionRange() {
        if (!selectionModel.hasSelection()) {
            return null;
        }
        return new int[]{
            selectionModel.getSelectionStartOffset(document),
            selectionModel.getSelectionEndOffset(document)
        };
    }

    private void navigateToSearchMatch(SearchMatch match) {
        moveCaretAction.accept(match.line(), match.startColumn());
        onSearchResultsChanged();
    }

    private void onSearchClosed() {
        viewport.setSearchMatches(List.of(), -1);
        requestFocusAction.run();
    }

    private void onSearchResultsChanged() {
        viewport.setSearchMatches(searchModel.getMatches(), searchModel.getCurrentMatchIndex());
    }
}

