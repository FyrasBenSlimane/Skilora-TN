package com.skilora.framework.components;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;

/**
 * TLCarousel - shadcn/ui Carousel (horizontal slide of items).
 * StackPane of pages; prev/next buttons or index property to change page.
 */
public class TLCarousel extends HBox {

    private final StackPane contentStack;
    private final List<Node> items = new ArrayList<>();
    private final IntegerProperty currentIndex = new SimpleIntegerProperty(0);

    public TLCarousel() {
        getStyleClass().add("carousel");
        setSpacing(8);
        contentStack = new StackPane();
        contentStack.getStyleClass().add("carousel-content");
        getChildren().add(contentStack);

        currentIndex.addListener((o, oldVal, newVal) -> {
            int idx = newVal.intValue();
            if (idx >= 0 && idx < items.size()) {
                contentStack.getChildren().setAll(items.get(idx));
            }
        });
    }

    public void addItem(Node node) {
        node.getStyleClass().add("carousel-item");
        items.add(node);
        if (items.size() == 1) contentStack.getChildren().setAll(node);
    }

    public void next() {
        if (items.isEmpty()) return;
        currentIndex.set((currentIndex.get() + 1) % items.size());
    }

    public void previous() {
        if (items.isEmpty()) return;
        int n = items.size();
        currentIndex.set((currentIndex.get() - 1 + n) % n);
    }

    public int getCurrentIndex() {
        return currentIndex.get();
    }

    public void setCurrentIndex(int index) {
        if (index >= 0 && index < items.size()) currentIndex.set(index);
    }

    public IntegerProperty currentIndexProperty() {
        return currentIndex;
    }

    public int getItemCount() {
        return items.size();
    }
}
