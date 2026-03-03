package com.skilora.framework.components;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * TLTabs - shadcn/ui Tabs component for JavaFX.
 *
 * Provides a pill-style tab bar (bg-muted container with active tab highlighted)
 * matching shadcn New York v4 Tabs. Eliminates duplicated tab patterns across controllers.
 *
 * CSS classes: .tabs, .tabs-list, .tabs-trigger, .tabs-trigger-active, .tabs-content-container
 *
 * Usage:
 *   TLTabs tabs = new TLTabs();
 *   tabs.addTab("overview", "Overview", overviewContent);
 *   tabs.addTab("analytics", "Analytics", analyticsContent);
 *   tabs.setOnTabChanged(tabId -> loadData(tabId));
 */
public class TLTabs extends VBox {

    private final HBox tabsList;
    private final StackPane contentContainer;
    private final Map<String, TabEntry> tabs = new LinkedHashMap<>();
    private String activeTabId;
    private Consumer<String> onTabChanged;

    public TLTabs() {
        getStyleClass().add("tabs");

        tabsList = new HBox();
        tabsList.getStyleClass().add("tabs-list");

        contentContainer = new StackPane();
        contentContainer.getStyleClass().add("tabs-content-container");

        getChildren().addAll(tabsList, contentContainer);
    }

    /**
     * Add a tab with an ID, display label, and content node.
     * The first tab added becomes active by default.
     */
    public TLTabs addTab(String tabId, String label, Node content) {
        Label trigger = new Label(label);
        trigger.getStyleClass().add("tabs-trigger");

        HBox triggerContainer = new HBox(4);
        triggerContainer.setAlignment(Pos.CENTER);
        triggerContainer.getChildren().add(trigger);
        triggerContainer.setOnMouseClicked(e -> selectTab(tabId));

        TabEntry entry = new TabEntry(tabId, trigger, triggerContainer, content);
        tabs.put(tabId, entry);
        tabsList.getChildren().add(triggerContainer);

        // First tab is auto-selected
        if (tabs.size() == 1) {
            selectTab(tabId);
        }

        return this;
    }

    /**
     * Add a tab with lazy content loading via a supplier.
     * Content is created the first time the tab is selected.
     */
    public TLTabs addTab(String tabId, String label, java.util.function.Supplier<Node> contentSupplier) {
        Label trigger = new Label(label);
        trigger.getStyleClass().add("tabs-trigger");

        HBox triggerContainer = new HBox(4);
        triggerContainer.setAlignment(Pos.CENTER);
        triggerContainer.getChildren().add(trigger);
        triggerContainer.setOnMouseClicked(e -> selectTab(tabId));

        TabEntry entry = new TabEntry(tabId, trigger, triggerContainer, contentSupplier);
        tabs.put(tabId, entry);
        tabsList.getChildren().add(triggerContainer);

        if (tabs.size() == 1) {
            selectTab(tabId);
        }

        return this;
    }

    /**
     * Select a tab by its ID.
     */
    public void selectTab(String tabId) {
        TabEntry entry = tabs.get(tabId);
        if (entry == null) return;

        // Update active styling on all triggers
        for (TabEntry t : tabs.values()) {
            t.trigger.getStyleClass().remove("tabs-trigger-active");
        }
        entry.trigger.getStyleClass().add("tabs-trigger-active");

        // Resolve lazy content
        Node content = entry.getContent();

        // Show content
        contentContainer.getChildren().clear();
        if (content != null) {
            contentContainer.getChildren().add(content);
        }

        String previousTab = activeTabId;
        activeTabId = tabId;

        // Fire change callback
        if (onTabChanged != null && !tabId.equals(previousTab)) {
            onTabChanged.accept(tabId);
        }
    }

    /**
     * Get the currently active tab ID.
     */
    public String getActiveTabId() {
        return activeTabId;
    }

    /**
     * Set a callback fired when the active tab changes.
     */
    public void setOnTabChanged(Consumer<String> handler) {
        this.onTabChanged = handler;
    }

    /**
     * Replace the content of an existing tab.
     */
    public void setTabContent(String tabId, Node content) {
        TabEntry entry = tabs.get(tabId);
        if (entry != null) {
            entry.content = content;
            entry.contentSupplier = null;
            if (tabId.equals(activeTabId)) {
                contentContainer.getChildren().clear();
                contentContainer.getChildren().add(content);
            }
        }
    }

    /**
     * Set a numeric badge on a tab trigger. Pass 0 to hide the badge.
     * The badge is displayed as a small red circle with count next to the tab label.
     */
    public void setTabBadge(String tabId, int count) {
        TabEntry entry = tabs.get(tabId);
        if (entry == null) return;

        // Remove existing badge if any
        if (entry.badgeLabel != null) {
            entry.triggerContainer.getChildren().remove(entry.badgeLabel);
            entry.badgeLabel = null;
        }

        if (count > 0) {
            Label badge = new Label(count > 99 ? "99+" : String.valueOf(count));
            badge.setStyle(
                "-fx-background-color: #ef4444; -fx-background-radius: 999; " +
                "-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; " +
                "-fx-padding: 1 5 1 5; -fx-min-width: 18; -fx-alignment: center;"
            );
            entry.badgeLabel = badge;
            entry.triggerContainer.getChildren().add(badge);
        }
    }

    /**
     * Internal tab data holder.
     */
    private static class TabEntry {
        final String id;
        final Label trigger;
        final HBox triggerContainer;
        Node content;
        java.util.function.Supplier<Node> contentSupplier;
        Label badgeLabel;

        TabEntry(String id, Label trigger, HBox triggerContainer, Node content) {
            this.id = id;
            this.trigger = trigger;
            this.triggerContainer = triggerContainer;
            this.content = content;
        }

        TabEntry(String id, Label trigger, HBox triggerContainer, java.util.function.Supplier<Node> contentSupplier) {
            this.id = id;
            this.trigger = trigger;
            this.triggerContainer = triggerContainer;
            this.contentSupplier = contentSupplier;
        }

        Node getContent() {
            if (content == null && contentSupplier != null) {
                content = contentSupplier.get();
                contentSupplier = null; // resolve once
            }
            return content;
        }
    }
}
