package com.skilora.framework.components;

import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.beans.DefaultProperty;
import javafx.collections.ObservableList;

/**
 * TLCard - Skilora Card Component
 * 
 * Clean card container with consistent styling.
 * Usage: TLCard card = new TLCard(); card.setHeader("Title");
 * card.setContent(yourContent);
 */
@DefaultProperty("content")
public class TLCard extends VBox {

    private VBox header;
    private VBox body;
    private VBox footer;

    public TLCard() {
        initialize();
    }

    private void initialize() {
        getStyleClass().addAll("card", "card-elevated");
        setSpacing(0);

        header = new VBox();
        header.getStyleClass().add("card-header");
        header.setVisible(false);
        header.setManaged(false);

        body = new VBox();
        body.getStyleClass().add("card-content");

        footer = new VBox();
        footer.getStyleClass().add("card-footer");
        footer.setVisible(false);
        footer.setManaged(false);

        getChildren().addAll(header, body, footer);

        setupHoverAnimation();
    }

    private void setupHoverAnimation() {
        // PERFORMANCE: Disabled scale animations and CacheHint.SPEED
        // Simple controls don't benefit from bitmap caching; it wastes GPU texture memory
    }

    public void setHeader(String title) {
        header.getChildren().clear();
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");
        header.getChildren().add(titleLabel);
        header.setVisible(true);
        header.setManaged(true);
    }

    public void setHeader(Node node) {
        header.getChildren().clear();
        header.getChildren().add(node);
        header.setVisible(true);
        header.setManaged(true);
    }

    // FXML Support: Single Node Setter
    public void setBody(Node node) {
        body.getChildren().clear();
        if (node != null) {
            body.getChildren().add(node);
        }
    }

    // Programmatic Support: Multiple Nodes
    public void setBody(Node... nodes) {
        body.getChildren().clear();
        body.getChildren().addAll(nodes);
    }

    // FXML Support: Get content ObservableList for @DefaultProperty
    public ObservableList<Node> getContent() {
        return body.getChildren();
    }

    // FXML Support: Single Node Setter for 'content' default property
    public void setContent(Node node) {
        setBody(node);
    }

    // Programmatic Support: Multiple Nodes
    public void setContent(Node... nodes) {
        setBody(nodes);
    }

    public void setFooter(Node node) {
        footer.getChildren().clear();
        if (node != null) {
            footer.getChildren().add(node);
        }
        footer.setVisible(true);
        footer.setManaged(true);
    }

    public void setFooter(Node... nodes) {
        footer.getChildren().clear();
        footer.getChildren().addAll(nodes);
        footer.setVisible(true);
        footer.setManaged(true);
    }

    public VBox getBody() {
        return body;
    }
}
