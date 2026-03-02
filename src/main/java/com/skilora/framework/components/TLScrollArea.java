package com.skilora.framework.components;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

/**
 * TLScrollArea - shadcn/ui Scroll Area (theme-adaptive ScrollPane).
 */
public class TLScrollArea extends ScrollPane {

    private static final String STYLESHEET =
            TLScrollArea.class.getResource("/com/skilora/framework/styles/tl-scroll-area.css").toExternalForm();

    public TLScrollArea() {
        this(null);
    }

    public TLScrollArea(Node content) {
        getStylesheets().add(STYLESHEET);
        getStyleClass().add("scroll-area");
        setFitToWidth(true);
        setFitToHeight(false);
        setPannable(false);
        if (content != null)
            setContent(content);

        // NOTE: Bitmap caching removed — it corrupts rendering on scrollable content.
    }
}
