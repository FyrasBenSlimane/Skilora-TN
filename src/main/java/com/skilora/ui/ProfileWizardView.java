package com.skilora.ui;

import com.skilora.model.entity.usermanagement.User;
import com.skilora.controller.usermanagement.ProfileWizardController;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

/**
 * Wrapper for ProfileWizardView.fxml to maintain compatibility with MainView.
 */
public class ProfileWizardView extends VBox {

    private static final Logger logger = LoggerFactory.getLogger(ProfileWizardView.class);

    private final User user;
    private ProfileWizardController controller;
    private Runnable onProfileUpdated;

    public ProfileWizardView(User user) {
        this.user = user;
        loadFXML();
    }

    private void loadFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/skilora/view/usermanagement/ProfileWizardView.fxml"));
            loader.setRoot(this); // Set this VBox as root
            loader.load();

            this.controller = loader.getController();

            // Pass data to controller
            if (this.controller != null) {
                this.controller.initializeContext(user, this::handleProfileUpdate);
            }

        } catch (IOException e) {
            logger.error("Failed to load ProfileWizardView FXML", e);
            getChildren().add(new javafx.scene.control.Label("Erreur de chargement du profil"));
        }
    }

    public void setOnProfileUpdated(Runnable onProfileUpdated) {
        this.onProfileUpdated = onProfileUpdated;
        // Update controller if already loaded
        if (this.controller != null) {
            this.controller.initializeContext(user, this::handleProfileUpdate);
        }
    }

    private void handleProfileUpdate() {
        if (onProfileUpdated != null) {
            onProfileUpdated.run();
        }
    }
}
