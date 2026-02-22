package com.skilora.controller;

import com.skilora.framework.components.*;
import com.skilora.model.dto.SearchResult;
import com.skilora.model.service.SearchService;
import com.skilora.utils.I18n;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

import java.util.List;

public class SearchController {

    @FXML
    private TLTextField searchField;
    @FXML
    private TLButton searchBtn;
    @FXML
    private VBox resultsList;
    @FXML
    private Label resultsCountLabel;

    private SearchService searchService;

    public void initialize() {
        searchService = SearchService.getInstance();

        searchBtn.setOnAction(e -> performSearch());
        searchField.getControl().setOnAction(e -> performSearch());

        resultsCountLabel.setText(I18n.get("search.initial_msg"));
    }

    private void performSearch() {
        String query = searchField.getControl().getText();
        if (query == null || query.trim().isEmpty())
            return;

        resultsList.getChildren().clear();
        resultsCountLabel.setText(I18n.get("search.loading"));

        List<SearchResult> results = searchService.searchAll(query);

        resultsCountLabel.setText(I18n.get("search.results_count", results.size()));

        if (results.isEmpty()) {
            Label noResults = new Label(I18n.get("search.no_results"));
            noResults.getStyleClass().add("muted");
            resultsList.getChildren().add(noResults);
        } else {
            for (SearchResult result : results) {
                resultsList.getChildren().add(createResultCard(result));
            }
        }
    }

    private Node createResultCard(SearchResult result) {
        TLCard card = new TLCard();

        HBox content = new HBox(16);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new javafx.geometry.Insets(12));

        // Icon
        SVGPath icon = new SVGPath();
        icon.setContent(result.getIcon());
        icon.getStyleClass().add("svg-path");
        StackPane iconCircle = new StackPane(icon);
        iconCircle.setPrefSize(40, 40);
        iconCircle.getStyleClass().add("icon-circle-secondary");

        // Texts
        VBox texts = new VBox(4);
        TLTypography title = new TLTypography(result.getName(), TLTypography.Variant.H3);
        TLTypography type = new TLTypography(result.getEntityType(), TLTypography.Variant.XS);
        type.getStyleClass().add("badge-secondary");

        HBox titleRow = new HBox(8, title, type);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        TLTypography details = new TLTypography(result.getDetails(), TLTypography.Variant.P);
        details.setMuted(true);

        texts.getChildren().addAll(titleRow, details);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TLButton viewBtn = new TLButton(I18n.get("common.view"), TLButton.ButtonVariant.OUTLINE);
        viewBtn.setOnAction(e -> {
            // Logic to navigate to specific entity details could be added here
            System.out.println("Viewing " + result.getEntityType() + " ID: " + result.getEntityId());
        });

        content.getChildren().addAll(iconCircle, texts, spacer, viewBtn);
        card.setBody(content);

        return card;
    }
}
