package com.skilora.controller.usermanagement;

import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLPasswordField;
import com.skilora.framework.components.TLTextField;
import com.skilora.framework.layouts.TLWindow;
import com.skilora.model.entity.usermanagement.User;
import com.skilora.model.enums.Role;
import com.skilora.service.usermanagement.MediaCache;
import com.skilora.service.usermanagement.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * RegisterController - Handles the registration view with the same layout as login
 * (hero video left, form right).
 */
public class RegisterController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);

    @FXML
    private StackPane heroContainer;
    @FXML
    private Label errorLabel;
    @FXML
    private TLTextField usernameField;
    @FXML
    private TLTextField emailField;
    @FXML
    private TLTextField fullNameField;
    @FXML
    private TLPasswordField passwordField;
    @FXML
    private TLButton registerBtn;
    @FXML
    private TLButton signInLink;

    private MediaPlayer activeMediaPlayer;
    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeVideo();
    }

    private void showError(String message) {
        if (errorLabel == null) return;
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    @FXML
    private void handleRegister() {
        hideError();

        String username = usernameField != null ? usernameField.getText() : null;
        String email = emailField != null ? emailField.getText() : null;
        String fullName = fullNameField != null ? fullNameField.getText() : null;
        String password = passwordField != null ? passwordField.getText() : null;

        if (username == null || username.trim().isEmpty()) {
            showError("Please enter a username.");
            return;
        }
        if (email == null || email.trim().isEmpty()) {
            showError("Please enter your email.");
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            showError("Please enter a password.");
            return;
        }

        final String usernameFinal = username.trim();
        final String emailFinal = email.trim();
        String fn = fullName != null ? fullName.trim() : "";
        if (fn.isEmpty()) fn = usernameFinal;
        final String fullNameFinal = fn;

        registerBtn.setDisable(true);
        registerBtn.setText("Creating account...");

        Thread registerThread = new Thread(() -> {
            try {
                UserService us = UserService.getInstance();
                if (us.findByUsername(usernameFinal).isPresent()) {
                    Platform.runLater(() -> {
                        showError("This username is already taken.");
                        registerBtn.setDisable(false);
                        registerBtn.setText("Create Account");
                    });
                    return;
                }
                if (us.findByEmail(emailFinal).isPresent()) {
                    Platform.runLater(() -> {
                        showError("This email is already registered.");
                        registerBtn.setDisable(false);
                        registerBtn.setText("Create Account");
                    });
                    return;
                }

                User user = new User(usernameFinal, password, Role.USER, fullNameFinal);
                user.setEmail(emailFinal);
                us.create(user);

                Platform.runLater(() -> {
                    registerBtn.setDisable(false);
                    registerBtn.setText("Create Account");
                    if (activeMediaPlayer != null) {
                        activeMediaPlayer.stop();
                        activeMediaPlayer.dispose();
                        activeMediaPlayer = null;
                    }
                    MediaCache.getInstance().dispose();
                    openLogin();
                });
            } catch (Exception e) {
                logger.error("Registration failed", e);
                Platform.runLater(() -> {
                    showError(e.getMessage() != null ? e.getMessage() : "Registration failed. Please try again.");
                    registerBtn.setDisable(false);
                    registerBtn.setText("Create Account");
                });
            }
        }, "RegisterThread");
        registerThread.setDaemon(true);
        registerThread.start();
    }

    @FXML
    private void handleBack() {
        if (activeMediaPlayer != null) {
            activeMediaPlayer.stop();
            activeMediaPlayer.dispose();
            activeMediaPlayer = null;
        }
        MediaCache.getInstance().dispose();
        openLogin();
    }

    private void openLogin() {
        if (stage == null) {
            if (signInLink != null && signInLink.getScene() != null)
                stage = (Stage) signInLink.getScene().getWindow();
            if (stage == null) return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/skilora/view/usermanagement/LoginView.fxml"));
            HBox loginRoot = loader.load();
            LoginController loginController = loader.getController();
            if (loginController != null) {
                loginController.setStage(stage);
            }
            TLWindow window = new TLWindow(stage, "Sign In", loginRoot);
            stage.getScene().setRoot(window);
        } catch (Exception e) {
            logger.error("Failed to open login view", e);
        }
    }

    private void initializeVideo() {
        if (heroContainer == null) return;

        heroContainer.setStyle("-fx-background-color: transparent;");

        try {
            URL resource = getClass().getResource("/com/skilora/assets/videos/hero.mp4");
            if (resource == null) {
                File f = new File("src/main/resources/com/skilora/assets/videos/hero.mp4");
                if (f.exists()) resource = f.toURI().toURL();
            }
            if (resource == null) {
                heroContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #09090b, #18181b);");
                return;
            }

            Media media = new Media(resource.toExternalForm());
            MediaPlayer mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setMute(true);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            activeMediaPlayer = mediaPlayer;

            MediaView mediaView = new MediaView(mediaPlayer);
            mediaView.setPreserveRatio(false);

            Pane videoPane = new Pane(mediaView);
            videoPane.setMouseTransparent(true);

            Runnable updateSize = () -> {
                double w = heroContainer.getWidth();
                double h = heroContainer.getHeight();
                if (w <= 0 || h <= 0) return;
                videoPane.setPrefSize(w, h);
                videoPane.setMaxSize(w, h);
                Rectangle clip = new Rectangle(0, 0, w, h);
                videoPane.setClip(clip);
                double videoW = media.getWidth();
                double videoH = media.getHeight();
                if (videoW <= 0 || videoH <= 0) {
                    mediaView.setFitWidth(w);
                    mediaView.setFitHeight(h);
                    mediaView.setX(0);
                    mediaView.setY(0);
                    return;
                }
                double scale = Math.max(w / videoW, h / videoH);
                double fw = videoW * scale;
                double fh = videoH * scale;
                mediaView.setFitWidth(fw);
                mediaView.setFitHeight(fh);
                mediaView.setX((w - fw) / 2.0);
                mediaView.setY((h - fh) / 2.0);
            };

            heroContainer.widthProperty().addListener(obs -> updateSize.run());
            heroContainer.heightProperty().addListener(obs -> updateSize.run());
            mediaPlayer.setOnReady(updateSize);

            Platform.runLater(() -> {
                heroContainer.getChildren().removeIf(node -> !(node instanceof AnchorPane));
                heroContainer.getChildren().add(0, videoPane);
                Rectangle overlay = new Rectangle();
                overlay.widthProperty().bind(heroContainer.widthProperty());
                overlay.heightProperty().bind(heroContainer.heightProperty());
                overlay.setMouseTransparent(true);
                overlay.setFill(new javafx.scene.paint.LinearGradient(0, 0, 1, 1, true,
                        javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop(0, javafx.scene.paint.Color.rgb(0, 0, 0, 0.4)),
                        new javafx.scene.paint.Stop(1, javafx.scene.paint.Color.rgb(0, 0, 0, 0.7))));
                heroContainer.getChildren().add(1, overlay);
                mediaPlayer.play();
            });
            mediaPlayer.setOnError(() -> logger.error("Video error: {}", mediaPlayer.getError().getMessage()));
        } catch (Exception e) {
            logger.warn("Could not load hero video, using gradient", e);
            heroContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, #09090b, #18181b);");
        }
    }
}
