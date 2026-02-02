package com.skilora.framework.components;

import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * TLAccordion - shadcn/ui Accordion (multiple expand/collapse sections).
 * Uses TitledPane internally with .accordion, .accordion-item, .accordion-trigger, .accordion-content.
 */
public class TLAccordion extends VBox {

    private final List<TitledPane> panes = new ArrayList<>();

    public TLAccordion() {
        getStyleClass().add("accordion");
        setSpacing(-1);
    }

    /**
     * Add a section with title and content.
     */
    public void addSection(String title, javafx.scene.Node content) {
        TitledPane pane = new TitledPane();
        pane.getStyleClass().add("accordion-item");
        pane.setText(title);
        if (content != null) {
            VBox wrap = new VBox(content);
            wrap.getStyleClass().add("accordion-content");
            pane.setContent(wrap);
        }
        panes.add(pane);
        getChildren().add(pane);
    }

    public int getSectionCount() {
        return panes.size();
    }

    public void setExpanded(int index) {
        for (int i = 0; i < panes.size(); i++)
            panes.get(i).setExpanded(i == index);
    }
}
