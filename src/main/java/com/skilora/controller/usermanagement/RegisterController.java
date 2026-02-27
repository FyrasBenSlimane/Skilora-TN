package com.skilora.controller.usermanagement;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class RegisterController {
    @FXML private VBox root;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    private Stage stage;
    private Runnable onBack;
    private Runnable onRegister;

    public void setStage(Stage stage) { this.stage = stage; }
    public void setOnBack(Runnable onBack) { this.onBack = onBack; }
    public void setOnRegister(Runnable onRegister) { this.onRegister = onRegister; }

    @FXML private void handleRegister() { if (onRegister != null) onRegister.run(); }
    @FXML private void handleBack() { if (onBack != null) onBack.run(); }
}
