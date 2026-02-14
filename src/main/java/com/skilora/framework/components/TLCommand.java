package com.skilora.framework.components;

import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * TLCommand - shadcn/ui Command (search + filtered list).
 * TextField for search + ListView for results; theme-adaptive.
 */
public class TLCommand<T> extends VBox {

    private final TextField searchField;
    private final ListView<T> listView;

    public TLCommand() {
        getStyleClass().add("command");
        setSpacing(8);

        searchField = new TextField();
        searchField.getStyleClass().add("command-input");
        searchField.setPromptText("Search...");

        listView = new ListView<>();
        listView.getStyleClass().add("command-list");
        listView.setMaxHeight(200);

        // PERFORMANCE: Enable caching and fixed cell size for virtualization
        listView.setCache(true);
        listView.setCacheHint(javafx.scene.CacheHint.SPEED);
        listView.setFixedCellSize(32); // Standard item height for consistent virtualization

        getChildren().addAll(searchField, listView);

        // PERFORMANCE: Cache the overall component
        setCache(true);
        setCacheHint(javafx.scene.CacheHint.SPEED);
    }

    public TextField getSearchField() {
        return searchField;
    }

    public ListView<T> getListView() {
        return listView;
    }
}
