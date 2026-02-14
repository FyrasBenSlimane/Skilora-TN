package com.skilora.framework.components;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * TLTabs - shadcn/ui Tabs (list of triggers + content stack).
 */
public class TLTabs extends VBox {

    private final HBox tabList;
    private final StackPane contentStack;
    private final List<VBox> contentPanels = new ArrayList<>();
    private final IntegerProperty selectedIndex = new SimpleIntegerProperty(-1);

    public TLTabs() {
        getStyleClass().add("tabs");
        setSpacing(0);

        tabList = new HBox();
        tabList.getStyleClass().add("tabs-list");
        tabList.setSpacing(4);

        contentStack = new StackPane();
        contentStack.getStyleClass().add("tabs-content-container");

        getChildren().addAll(tabList, contentStack);

        selectedIndex.addListener((o, oldVal, newVal) -> {
            int idx = newVal.intValue();
            for (int i = 0; i < tabList.getChildren().size(); i++) {
                Node n = tabList.getChildren().get(i);
                if (n instanceof Button) {
                    n.getStyleClass().remove("tabs-trigger-active");
                    n.getStyleClass().add("tabs-trigger");
                    if (i == idx) n.getStyleClass().add("tabs-trigger-active");
                }
            }
            for (int i = 0; i < contentPanels.size(); i++) {
                contentPanels.get(i).setVisible(i == idx);
                contentPanels.get(i).setManaged(i == idx);
            }
        });
    }

    /**
     * Add a tab with label and content.
     */
    public void addTab(String label, javafx.scene.Node... content) {
        int index = tabList.getChildren().size();
        Button trigger = new Button(label);
        trigger.getStyleClass().add("tabs-trigger");
        if (index == 0) trigger.getStyleClass().add("tabs-trigger-active");
        trigger.setOnAction(e -> selectIndex(index));

        VBox panel = new VBox(content);
        panel.getStyleClass().add("tabs-content");
        panel.setVisible(index == 0);
        panel.setManaged(index == 0);

        tabList.getChildren().add(trigger);
        contentPanels.add(panel);
        contentStack.getChildren().add(panel);

        if (selectedIndex.get() < 0) selectedIndex.set(0);
    }

    public void selectIndex(int index) {
        if (index >= 0 && index < contentPanels.size())
            selectedIndex.set(index);
    }

    public int getSelectedIndex() {
        return selectedIndex.get();
    }

    public IntegerProperty selectedIndexProperty() {
        return selectedIndex;
    }
}
