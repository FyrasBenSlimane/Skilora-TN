package com.skilora.framework.components;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;

import java.util.ArrayList;
import java.util.List;

/**
 * TLBreadcrumb - shadcn/ui Breadcrumb (theme-adaptive).
 * HBox of clickable items + separators; last item is current (non-clickable).
 */
public class TLBreadcrumb extends HBox {

    private static final String SEPARATOR = "/";

    public TLBreadcrumb() {
        getStyleClass().add("breadcrumb");
        setSpacing(0);
    }

    /**
     * Set items. Last one is treated as current (no hover, not clickable).
     */
    public void setItems(String... labels) {
        getChildren().clear();
        if (labels == null || labels.length == 0) return;
        for (int i = 0; i < labels.length; i++) {
            if (i > 0) {
                Label sep = new Label(SEPARATOR);
                sep.getStyleClass().add("breadcrumb-separator");
                getChildren().add(sep);
            }
            boolean isLast = (i == labels.length - 1);
            if (isLast) {
                Label current = new Label(labels[i]);
                current.getStyleClass().addAll("breadcrumb-item", "breadcrumb-item-current");
                getChildren().add(current);
            } else {
                Button btn = new Button(labels[i]);
                btn.getStyleClass().add("breadcrumb-item");
                final int index = i;
                btn.setOnAction(e -> onItemClicked(index, labels[index]));
                getChildren().add(btn);
            }
        }
    }

    /** Override to handle navigation. */
    protected void onItemClicked(int index, String label) {
        // no-op; subclass or set handler
    }

    /** Get list of label strings for current items (for rebuilding). */
    public List<String> getItemLabels() {
        List<String> out = new ArrayList<>();
        getChildren().forEach(node -> {
            if (node instanceof Button) out.add(((Button) node).getText());
            else if (node instanceof Label && node.getStyleClass().contains("breadcrumb-item-current"))
                out.add(((Label) node).getText());
        });
        return out;
    }
}
