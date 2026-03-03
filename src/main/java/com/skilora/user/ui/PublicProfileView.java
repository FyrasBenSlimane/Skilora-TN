package com.skilora.user.ui;

import com.skilora.user.controller.PublicProfileController;
import com.skilora.user.entity.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class PublicProfileView extends ScrollPane {

    private static final Logger logger = LoggerFactory.getLogger(PublicProfileView.class);
    private PublicProfileController controller;
    private final User targetUser;

    public PublicProfileView(User targetUser) {
        this.targetUser = targetUser;
        loadFXML();
    }

    private void loadFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/user/PublicProfileView.fxml"));
            loader.setRoot(this);
            loader.load();

            // Constrain content to viewport width so the profile never scrolls horizontally.
            setFitToWidth(true);
            setPannable(false);
            setHbarPolicy(ScrollBarPolicy.NEVER);
            Node content = getContent();
            if (content instanceof Region region) {
                region.prefWidthProperty().bind(widthProperty());
                region.minWidthProperty().bind(widthProperty());
                region.maxWidthProperty().bind(widthProperty());
            }

            this.controller = loader.getController();

            if (this.controller != null) {
                this.controller.loadUser(targetUser);
            }

        } catch (IOException e) {
            logger.error("Failed to load PublicProfileView FXML", e);
        }
    }
}
