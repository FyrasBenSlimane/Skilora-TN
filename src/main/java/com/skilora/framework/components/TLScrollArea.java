package com.skilora.framework.components;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

/**
 * TLScrollArea - shadcn/ui Scroll Area (theme-adaptive ScrollPane).
 */
public class TLScrollArea extends ScrollPane {

    public TLScrollArea() {
        this(null);
    }

    public TLScrollArea(Node content) {
        getStyleClass().add("scroll-area");
        setFitToWidth(true);
        setFitToHeight(true);
        setPannable(false);
        if (content != null)
            setContent(content);

        // PERFORMANCE: Enable caching for smoother scrolling
        setCache(true);
        setCacheHint(javafx.scene.CacheHint.SPEED);
    }
}
