package com.skilora.framework.components;

import javafx.scene.control.TableView;

/**
 * TLTable - shadcn/ui Table (theme-adaptive TableView).
 */
public class TLTable<T> extends TableView<T> {

    public TLTable() {
        getStyleClass().add("table-view");

        // PERFORMANCE: Enable caching for GPU-accelerated rendering
        setCache(true);
        setCacheHint(javafx.scene.CacheHint.SPEED);

        // PERFORMANCE: Enable fixed cell size for virtualization optimization
        // TableView already virtualizes rows, but this hint improves performance
        setFixedCellSize(42); // Standard row height for consistency
    }
}
