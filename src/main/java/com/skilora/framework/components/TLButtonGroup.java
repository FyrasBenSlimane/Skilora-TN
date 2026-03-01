package com.skilora.framework.components;

import javafx.scene.Node;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

/**
 * TLButtonGroup - shadcn/ui Button Group.
 * HBox that visually groups buttons (first/middle/last radius); children get .btn and .btn-group-item.
 */
public class TLButtonGroup extends HBox {

    private final List<Node> buttons = new ArrayList<>();

    public TLButtonGroup() {
        getStyleClass().add("btn-group");
        setSpacing(0);
    }

    /** Add a button (TLButton or Button); applies group styling. */
    public void addButton(Node button) {
        if (button instanceof TLButton) {
            ((TLButton) button).getStyleClass().add("btn-group-item");
        } else if (button instanceof javafx.scene.control.Button) {
            ((javafx.scene.control.Button) button).getStyleClass().addAll("btn", "btn-outline", "btn-group-item");
        }
        buttons.add(button);
        getChildren().add(button);
        updateEdgeClasses();
    }

    public void clearButtons() {
        buttons.clear();
        getChildren().clear();
    }

    private void updateEdgeClasses() {
        for (int i = 0; i < buttons.size(); i++) {
            Node n = buttons.get(i);
            n.getStyleClass().removeAll("btn-group-first", "btn-group-last", "btn-group-middle");
            if (buttons.size() == 1) {
                n.getStyleClass().add("btn-group-first");
                n.getStyleClass().add("btn-group-last");
            } else {
                if (i == 0) n.getStyleClass().add("btn-group-first");
                else if (i == buttons.size() - 1) n.getStyleClass().add("btn-group-last");
                else n.getStyleClass().add("btn-group-middle");
            }
        }
    }
}
