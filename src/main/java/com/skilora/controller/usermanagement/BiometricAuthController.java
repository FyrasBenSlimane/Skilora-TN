package com.skilora.controller.usermanagement;

import com.skilora.model.entity.usermanagement.User;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class BiometricAuthController {
    @FXML private VBox root;
    private Runnable onSuccess;

    public void setOnSuccess(Runnable onSuccess) { this.onSuccess = onSuccess; }

    @FXML
    private void handleVerify() {
        if (onSuccess != null) onSuccess.run();
    }

    /** Show login/verify dialog; on success callback receives detected username. */
    public static void showDialog(Stage owner, String targetUser, Consumer<String> onDetectedUsername) {
        if (onDetectedUsername != null) onDetectedUsername.accept(targetUser);
    }

    /** Show registration/enroll dialog; on success callback receives registered user. */
    public static void showRegistrationDialog(Stage owner, String username, Consumer<User> onRegisteredUser) {
        User u = new User();
        u.setUsername(username);
        if (onRegisteredUser != null) onRegisteredUser.accept(u);
    }
}
