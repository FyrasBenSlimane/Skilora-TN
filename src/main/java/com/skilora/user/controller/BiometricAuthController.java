package com.skilora.user.controller;

import com.skilora.user.service.BiometricService;
import com.skilora.user.service.CameraDevice;
import com.skilora.user.service.FastCameraManager;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.I18n;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import com.skilora.framework.components.TLProgress;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * BiometricAuthController - High-performance FXML Controller for BiometricAuthDialog.fxml
 * 
 * Features:
 * - Two-pass detection (fast HOG on downscaled → quality encoding on full-res)
 * - Multi-frame consensus verification (3 consecutive matches like Face ID)
 * - Duplicate face prevention during registration
 * - Real-time confidence feedback via progress bar
 * - Faster preview loop (30 FPS) with decoupled verification thread
 */
public class BiometricAuthController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(BiometricAuthController.class);

    // ── Tuning constants ────────────────────────────────────────────────────
    private static final int MAX_ATTEMPTS = 40;
    private static final int CONSENSUS_REQUIRED = 3;   // consecutive matches needed
    private static final int PREVIEW_INTERVAL_MS = 33;  // ~30 FPS
    private static final int VERIFY_INTERVAL_MS = 300;  // verification every 300ms
    private static final int REGISTER_INTERVAL_MS = 400;

    @FXML private BorderPane rootPane;
    @FXML private HBox headerPane;
    @FXML private Label titleLabel;
    @FXML private Button closeBtn;
    @FXML private ImageView previewView;
    @FXML private SVGPath viewfinder;
    @FXML private Label statusLabel;
    @FXML private TLProgress confidenceBar;
    @FXML private Label confidenceLabel;

    private Stage dialogStage;
    private String username;
    private Consumer<String> onSuccess;
    private boolean registrationMode = false;

    private CameraDevice webcam;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private int attempts = 0;
    private int consecutiveMatches = 0;
    private String lastMatchedUser = null;

    // Drag offsets
    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupDragHandlers();
        setupViewfinderAnimation();
        // Initialize optional UI elements
        if (confidenceBar != null) {
            confidenceBar.setProgress(0);
            confidenceBar.setVisible(false);
        }
        if (confidenceLabel != null) {
            confidenceLabel.setVisible(false);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FACTORY METHODS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Factory method for VERIFICATION / IDENTIFICATION mode.
     */
    public static void showDialog(Stage owner, String username, Consumer<String> onSuccess) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    BiometricAuthController.class.getResource("/com/skilora/view/user/BiometricAuthDialog.fxml"));
            BorderPane root = loader.load();

            BiometricAuthController controller = loader.getController();
            controller.username = username;
            controller.onSuccess = onSuccess;

            Stage dialogStage = new Stage();
            dialogStage.initOwner(owner);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            controller.dialogStage = dialogStage;

            controller.titleLabel.setText(username != null ? I18n.get("biometric.title.verification") : I18n.get("biometric.title.identification"));

            Scene scene = new Scene(root);
            scene.setFill(null);
            if (owner != null && owner.getScene() != null) {
                scene.getStylesheets().addAll(owner.getScene().getStylesheets());
                if (owner.getScene().getRoot().getStyleClass().contains("light")) {
                    root.getStyleClass().add("light");
                }
            }

            dialogStage.setScene(scene);
            dialogStage.setOnCloseRequest(e -> controller.handleClose());
            controller.startCamera();
            dialogStage.show();

        } catch (Exception e) {
            logger.error("Failed to load BiometricAuthDialog: " + e.getMessage(), e);
        }
    }

    /**
     * Factory method for REGISTRATION mode (with duplicate face prevention).
     */
    public static void showRegistrationDialog(Stage owner, String username, Consumer<String> onSuccess) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    BiometricAuthController.class.getResource("/com/skilora/view/user/BiometricAuthDialog.fxml"));
            BorderPane root = loader.load();

            BiometricAuthController controller = loader.getController();
            controller.username = username;
            controller.onSuccess = onSuccess;
            controller.registrationMode = true;

            Stage dialogStage = new Stage();
            dialogStage.initOwner(owner);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            controller.dialogStage = dialogStage;
            controller.titleLabel.setText(I18n.get("biometric.title.registration"));

            Scene scene = new Scene(root);
            scene.setFill(null);
            if (owner != null && owner.getScene() != null) {
                scene.getStylesheets().addAll(owner.getScene().getStylesheets());
                if (owner.getScene().getRoot().getStyleClass().contains("light")) {
                    root.getStyleClass().add("light");
                }
            }

            dialogStage.setScene(scene);
            dialogStage.setOnCloseRequest(e -> controller.handleClose());
            controller.startCamera();
            dialogStage.show();

        } catch (Exception e) {
            logger.error("Failed to load BiometricAuthDialog for registration: " + e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  UI SETUP
    // ═══════════════════════════════════════════════════════════════════════

    private void setupDragHandlers() {
        if (headerPane != null) {
            headerPane.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            headerPane.setOnMouseDragged(event -> {
                if (dialogStage != null) {
                    dialogStage.setX(event.getScreenX() - xOffset);
                    dialogStage.setY(event.getScreenY() - yOffset);
                }
            });
        }
    }

    private void setupViewfinderAnimation() {
        if (viewfinder != null) {
            FadeTransition pulse = new FadeTransition(Duration.seconds(1.2), viewfinder);
            pulse.setFromValue(0.6);
            pulse.setToValue(1.0);
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.setAutoReverse(true);
            pulse.play();
        }
    }

    @FXML
    public void handleClose() {
        stopCamera();
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    public boolean isVerificationMode() {
        return username != null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CAMERA & PROCESSING
    // ═══════════════════════════════════════════════════════════════════════

    private void startCamera() {
        AppThreadPool.execute(() -> {
            webcam = FastCameraManager.getInstance().getPrewarmedCamera();
            if (webcam == null) {
                webcam = FastCameraManager.getInstance().getCameraFast();
            }

            if (webcam != null) {
                isRunning.set(true);
                Platform.runLater(() -> {
                    statusLabel.setText(I18n.get("biometric.status.initializing_camera"));
                    showConfidenceUI(true);
                });

                // High-FPS preview thread (decoupled from verification)
                AppThreadPool.execute(() -> {
                    while (isRunning.get()) {
                        if (webcam.isOpen()) {
                            BufferedImage bimg = webcam.getImage();
                            if (bimg != null) {
                                WritableImage fxImg = SwingFXUtils.toFXImage(bimg, null);
                                Platform.runLater(() -> previewView.setImage(fxImg));
                            }
                        }
                        try {
                            Thread.sleep(PREVIEW_INTERVAL_MS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                });

                // Small warmup delay
                try { Thread.sleep(600); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }

                // Start the appropriate processing loop
                if (registrationMode) {
                    runRegistrationLoop();
                } else {
                    runVerificationLoop();
                }
            } else {
                Platform.runLater(() -> statusLabel.setText(I18n.get("biometric.status.camera_unavailable")));
            }
        });
    }

    // ── Registration Loop (with duplicate prevention) ───────────────────

    private void runRegistrationLoop() {
        Platform.runLater(() -> statusLabel.setText(I18n.get("biometric.status.position_face")));

        while (isRunning.get() && attempts < MAX_ATTEMPTS) {
            if (webcam != null && webcam.isOpen()) {
                BufferedImage bimg = webcam.getImage();
                if (bimg != null) {
                    Platform.runLater(() -> statusLabel.setText(I18n.get("biometric.status.analyzing_face")));

                    // Use the new API that returns full result (including duplicate info)
                    JSONObject result = BiometricService.getInstance().registerFaceResult(username, bimg);

                    if (result != null && result.optBoolean("success", false)) {
                        // Registration successful
                        Platform.runLater(() -> {
                            statusLabel.setText(I18n.get("biometric.status.registration_success"));
                            statusLabel.setStyle("-fx-text-fill: -fx-success; -fx-font-weight: bold;");
                            animateViewfinderSuccess();
                            updateConfidence(1.0);
                        });

                        sleepQuiet(1500);

                        Platform.runLater(() -> {
                            stopCamera();
                            if (dialogStage != null) dialogStage.close();
                            if (onSuccess != null) onSuccess.accept(username);
                        });
                        return;

                    } else if (result != null && result.optBoolean("duplicate", false)) {
                        // DUPLICATE DETECTED — face belongs to another account
                        String matchedUser = result.optString("matched_username", I18n.get("biometric.another_account"));
                        Platform.runLater(() -> {
                            statusLabel.setText(I18n.get("biometric.status.duplicate_face", matchedUser));
                            statusLabel.setStyle("-fx-text-fill: -fx-warning; -fx-font-weight: bold;");
                            animateViewfinderError();
                        });

                        sleepQuiet(3000);

                        Platform.runLater(() -> {
                            stopCamera();
                            if (dialogStage != null) dialogStage.close();
                        });
                        return;

                    } else {
                        // No face detected or error — continue trying
                        String msg = result != null ? result.optString("message", I18n.get("biometric.status.no_face_detected")) : I18n.get("biometric.status.no_face_detected");
                        Platform.runLater(() -> statusLabel.setText(msg));
                    }
                }
            }
            attempts++;
            sleepQuiet(REGISTER_INTERVAL_MS);
        }

        // Timeout
        if (isRunning.get()) {
            Platform.runLater(() -> {
                statusLabel.setText(I18n.get("biometric.status.timeout"));
                statusLabel.setStyle("-fx-text-fill: -fx-destructive; -fx-font-weight: bold;");
            });
            sleepQuiet(2000);
            Platform.runLater(() -> {
                stopCamera();
                if (dialogStage != null) dialogStage.close();
            });
        }
    }

    // ── Verification Loop (multi-frame consensus) ───────────────────────

    private void runVerificationLoop() {
        consecutiveMatches = 0;
        lastMatchedUser = null;

        Platform.runLater(() -> statusLabel.setText(
                username != null ? I18n.get("biometric.status.verifying") : I18n.get("biometric.status.identifying")));

        while (isRunning.get() && attempts < MAX_ATTEMPTS) {
            if (webcam != null && webcam.isOpen()) {
                BufferedImage bimg = webcam.getImage();
                if (bimg != null) {
                    boolean matchThisFrame = false;
                    String detectedUser = username;
                    double confidence = 0;

                    if (username != null) {
                        // Verification mode — check specific user
                        JSONObject result = BiometricService.getInstance().verifyFaceResult(username, bimg);
                        if (result.optBoolean("verified", false)) {
                            matchThisFrame = true;
                            confidence = result.optDouble("confidence", 0);
                        } else {
                            confidence = result.optDouble("confidence", 0);
                        }
                    } else {
                        // Identification mode — find any matching user
                        Optional<String> result = BiometricService.getInstance().identifyUser(bimg);
                        if (result.isPresent()) {
                            matchThisFrame = true;
                            detectedUser = result.get();
                            confidence = 0.8; // approximate
                        }
                    }

                    final double conf = confidence;

                    if (matchThisFrame) {
                        // Check consensus: same user matched consecutively?
                        if (detectedUser != null && detectedUser.equals(lastMatchedUser)) {
                            consecutiveMatches++;
                        } else {
                            consecutiveMatches = 1;
                            lastMatchedUser = detectedUser;
                        }

                        Platform.runLater(() -> {
                            updateConfidence(Math.min(conf, 1.0));
                            statusLabel.setText(I18n.get("biometric.status.face_detected", consecutiveMatches, CONSENSUS_REQUIRED));
                            animateViewfinderScanning();
                        });

                        if (consecutiveMatches >= CONSENSUS_REQUIRED) {
                            // CONSENSUS REACHED → verified
                            String finalUser = lastMatchedUser;
                            Platform.runLater(() -> {
                                statusLabel.setText(I18n.get("biometric.status.welcome", finalUser));
                                statusLabel.setStyle("-fx-text-fill: -fx-success; -fx-font-weight: bold;");
                                animateViewfinderSuccess();
                                updateConfidence(1.0);
                            });

                            sleepQuiet(1200);

                            Platform.runLater(() -> {
                                stopCamera();
                                if (dialogStage != null) dialogStage.close();
                                if (onSuccess != null) onSuccess.accept(finalUser);
                            });
                            return;
                        }
                    } else {
                        // No match this frame — reset streak
                        consecutiveMatches = 0;
                        lastMatchedUser = null;
                        Platform.runLater(() -> {
                            updateConfidence(Math.max(conf, 0));
                            statusLabel.setText(username != null ? I18n.get("biometric.status.verifying_short") : I18n.get("biometric.status.searching"));
                        });
                    }
                }
            }
            attempts++;
            sleepQuiet(VERIFY_INTERVAL_MS);
        }

        // Timeout
        if (isRunning.get()) {
            Platform.runLater(() -> {
                statusLabel.setText(I18n.get("biometric.status.verification_failed"));
                statusLabel.setStyle("-fx-text-fill: -fx-destructive; -fx-font-weight: bold;");
                animateViewfinderError();
            });
            sleepQuiet(2000);
            Platform.runLater(() -> {
                stopCamera();
                if (dialogStage != null) dialogStage.close();
            });
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  UI HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private void showConfidenceUI(boolean show) {
        if (confidenceBar != null) confidenceBar.setVisible(show);
        if (confidenceLabel != null) confidenceLabel.setVisible(show);
    }

    private void updateConfidence(double value) {
        if (confidenceBar != null) {
            confidenceBar.setProgress(value);
            // Color the bar based on confidence level
            if (value >= 0.7) {
                confidenceBar.setStyle("-fx-accent: -fx-success;");
            } else if (value >= 0.4) {
                confidenceBar.setStyle("-fx-accent: -fx-warning;");
            } else {
                confidenceBar.setStyle("-fx-accent: -fx-muted-foreground;");
            }
        }
        if (confidenceLabel != null) {
            int pct = (int) (value * 100);
            confidenceLabel.setText(pct + "%");
        }
    }

    private void animateViewfinderSuccess() {
        if (viewfinder != null) {
            viewfinder.setStyle("-fx-stroke: -fx-green; -fx-stroke-width: 4;");
        }
    }

    private void animateViewfinderError() {
        if (viewfinder != null) {
            viewfinder.setStyle("-fx-stroke: -fx-red; -fx-stroke-width: 4;");
        }
    }

    private void animateViewfinderScanning() {
        if (viewfinder != null) {
            viewfinder.setStyle("-fx-stroke: -fx-blue; -fx-stroke-width: 3;");
        }
    }

    private void sleepQuiet(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void stopCamera() {
        isRunning.set(false);
        if (webcam != null) {
            webcam.close();
        }
    }
}
