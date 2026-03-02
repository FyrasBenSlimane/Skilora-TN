package com.skilora.framework.components;

import javafx.scene.control.TableView;

/**
 * TLTable - shadcn/ui Table (theme-adaptive TableView).
 */
public class TLTable<T> extends TableView<T> {

    private static final String STYLESHEET =
            TLTable.class.getResource("/com/skilora/framework/styles/tl-table.css").toExternalForm();

    public TLTable() {
        getStylesheets().add(STYLESHEET);
        getStyleClass().add("table-view");

        // NOTE: Bitmap caching (setCache/setCacheHint) removed — it corrupts
        //       rendering on virtualized controls like TableView.

        // PERFORMANCE: Enable fixed cell size for virtualization optimization
        // TableView already virtualizes rows, but this hint improves performance
        setFixedCellSize(42); // Standard row height for consistency
    }
}
