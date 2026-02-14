package com.skilora.framework.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

/**
 * TLVirtualList - High-performance virtualized ListView.
 * 
 * PERFORMANCE FEATURES:
 * - Fixed cell size for O(1) scroll calculations
 * - GPU caching enabled
 * - Lazy cell factory for efficient memory usage
 * - Supports custom cell factories
 * 
 * Usage:
 * TLVirtualList<String> list = new TLVirtualList<>(36);
 * list.setItems(myItems);
 * list.setCellFactory(item -> new Label(item));
 */
public class TLVirtualList<T> extends ListView<T> {

    private static final int DEFAULT_CELL_HEIGHT = 36;

    public TLVirtualList() {
        this(DEFAULT_CELL_HEIGHT);
    }

    public TLVirtualList(int cellHeight) {
        this(FXCollections.observableArrayList(), cellHeight);
    }

    public TLVirtualList(ObservableList<T> items) {
        this(items, DEFAULT_CELL_HEIGHT);
    }

    public TLVirtualList(ObservableList<T> items, int cellHeight) {
        super(items);

        // PERFORMANCE: Fixed cell size enables O(1) virtualization calculations
        setFixedCellSize(cellHeight);

        // PERFORMANCE: Enable GPU caching
        setCache(true);
        setCacheHint(javafx.scene.CacheHint.SPEED);

        // Style class for theming
        getStyleClass().add("virtual-list");
    }

    /**
     * Set a simple cell factory using a lambda that creates a node from an item.
     * This is a convenience method for common use cases.
     * 
     * @param nodeFactory Function that creates a Node from an item
     */
    public void setSimpleCellFactory(java.util.function.Function<T, javafx.scene.Node> nodeFactory) {
        setCellFactory(lv -> new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setGraphic(nodeFactory.apply(item));
                }
                // PERFORMANCE: Cache individual cells
                setCache(true);
                setCacheHint(javafx.scene.CacheHint.SPEED);
            }
        });
    }

    /**
     * Convenience method to set a cell factory that just displays text.
     */
    public void setTextCellFactory() {
        setCellFactory(lv -> new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                }
                setCache(true);
                setCacheHint(javafx.scene.CacheHint.SPEED);
            }
        });
    }
}
