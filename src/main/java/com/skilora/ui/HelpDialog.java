package com.skilora.ui;

import com.skilora.framework.components.*;
import com.skilora.utils.I18n;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelpDialog {

    private static final Logger logger = LoggerFactory.getLogger(HelpDialog.class);

    public static void show(javafx.stage.Window owner) {
        TLDialog<Void> dialog = new TLDialog<>();
        
        if (owner != null) {
            dialog.initOwner(owner);
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
        }

        dialog.setDialogTitle(I18n.get("help.title"));
        dialog.setDescription(I18n.get("help.description"));

        VBox content = new VBox(20);
        content.setPadding(new Insets(16));

        // Quick Links Section
        VBox quickLinksSection = createSection(I18n.get("help.quick_links"),
            createHelpItem(I18n.get("help.documentation"), I18n.get("help.documentation.desc"), null),
            createHelpItem(I18n.get("help.tutorials"), I18n.get("help.tutorials.desc"), null),
            createHelpItem(I18n.get("help.faq"), I18n.get("help.faq.desc"), null)
        );

        // Contact Section
        VBox contactSection = createSection(I18n.get("help.contact"),
            createHelpItem(I18n.get("help.email"), "support@skilora.tn", "mailto:support@skilora.tn"),
            createHelpItem(I18n.get("help.phone"), "+216 XX XXX XXX", "tel:+216XXXXXXXX"),
            createHelpItem(I18n.get("help.website"), "www.skilora.tn", "https://skilora.tn")
        );

        // Version Info
        Label versionLabel = new Label(I18n.get("help.version"));
        versionLabel.getStyleClass().add("text-muted");
        versionLabel.setStyle("-fx-font-size: 12px;");

        content.getChildren().addAll(quickLinksSection, contactSection, versionLabel);
        dialog.setContent(content);

        ButtonType closeBtn = new ButtonType(I18n.get("common.close"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(closeBtn);

        dialog.showAndWait();
    }

    private static VBox createSection(String title, HBox... items) {
        VBox section = new VBox(12);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("h4");
        
        TLSeparator separator = new TLSeparator();
        
        VBox itemsBox = new VBox(8);
        itemsBox.getChildren().addAll(items);
        
        section.getChildren().addAll(titleLabel, separator, itemsBox);
        return section;
    }

    private static HBox createHelpItem(String title, String description, String link) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(8));
        item.setStyle("-fx-background-color: -fx-background; -fx-background-radius: 8px;");

        VBox textBox = new VBox(4);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold;");
        
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("text-muted");
        descLabel.setStyle("-fx-font-size: 12px;");
        
        textBox.getChildren().addAll(titleLabel, descLabel);
        
        item.getChildren().add(textBox);
        
        if (link != null) {
            item.setOnMouseClicked(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create(link));
                } catch (Exception ex) {
                    logger.error("Could not open link: " + link, ex);
                }
            });
            item.setStyle(item.getStyle() + " -fx-cursor: hand;");
        }

        return item;
    }
}
