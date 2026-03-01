package com.skilora.framework.components;

import javafx.scene.control.Pagination;

/**
 * TLPagination - shadcn/ui Pagination (theme-adaptive).
 */
public class TLPagination extends Pagination {

    public TLPagination() {
        this(1);
    }

    public TLPagination(int pageCount) {
        super(pageCount);
        getStyleClass().add("pagination");
    }

    public TLPagination(int pageCount, int currentPageIndex) {
        super(pageCount, currentPageIndex);
        getStyleClass().add("pagination");
    }
}
