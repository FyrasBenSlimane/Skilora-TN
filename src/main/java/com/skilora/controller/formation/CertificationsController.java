package com.skilora.controller.formation;

import com.skilora.framework.components.*;
import com.skilora.model.entity.formation.Certificate;
import com.skilora.model.entity.formation.Training;
import com.skilora.model.entity.usermanagement.User;
import com.skilora.service.formation.*;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;
import com.skilora.utils.PDFGenerator;
import com.skilora.service.usermanagement.UserService;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.skilora.utils.QRCodeGenerator;
import java.awt.image.BufferedImage;
import com.google.zxing.WriterException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class CertificationsController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(CertificationsController.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    @FXML private Label certificatesCountLabel;
    @FXML private TLButton refreshBtn;
    @FXML private VBox certificatesContainer;
    @FXML private VBox emptyState;

    private User currentUser;
    private Stage primaryStage;
    private final CertificateService certificateService = CertificateService.getInstance();
    private final TrainingService trainingService = TrainingService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Data loaded via setCurrentUser
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadCertificates();
    }

    /**
     * Set the primary stage for showing dialogs (FileChooser)
     */
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    @FXML
    private void handleRefresh() {
        // Force refresh by ensuring certificates are generated for completed trainings
        loadCertificates();
    }

    /**
     * Public method to refresh the certificates list
     */
    public void refresh() {
        logger.debug("Refresh requested for CertificationsController");
        loadCertificates();
    }

    private void loadCertificates() {
        if (currentUser == null) return;

        Thread loadThread = new Thread(() -> {
            try {
                logger.info("Loading certificates for user id={}", currentUser.getId());
                List<Certificate> certificates = certificateService.getUserCertificates(currentUser.getId());
                logger.info("Loaded {} certificates for user id={}", certificates.size(), currentUser.getId());
                
                Platform.runLater(() -> {
                    renderCertificates(certificates);
                });
            } catch (Exception e) {
                logger.error("Error loading certificates", e);
                Platform.runLater(() -> {
                    DialogUtils.showError(I18n.get("common.error"), 
                        I18n.get("certificates.load_error", e.getMessage()));
                });
            }
        }, "LoadCertificatesThread");
        loadThread.setDaemon(true);
        loadThread.start();
    }

    private void renderCertificates(List<Certificate> certificates) {
        certificatesContainer.getChildren().clear();

        if (certificates.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            certificatesCountLabel.setText("0");
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);
        certificatesCountLabel.setText(String.valueOf(certificates.size()));

        for (Certificate cert : certificates) {
            try {
                Training training = trainingService.getTrainingById(cert.getTrainingId()).orElse(null);
                if (training != null) {
                    TLCard card = createCertificateCard(cert, training);
                    certificatesContainer.getChildren().add(card);
                }
            } catch (Exception e) {
                logger.error("Error rendering certificate card for cert id={}", cert.getId(), e);
            }
        }
    }

    private TLCard createCertificateCard(Certificate cert, Training training) {
        TLCard card = new TLCard();
        card.setStyle("-fx-padding: 24;");

        VBox content = new VBox(16);
        content.setStyle("-fx-padding: 0;");

        // Header with training title
        VBox header = new VBox(8);
        Label titleLabel = new Label(training.getTitle());
        titleLabel.getStyleClass().add("h3");
        titleLabel.setWrapText(true);
        
        Label certNumberLabel = new Label("Certificat #" + cert.getCertificateNumber());
        certNumberLabel.getStyleClass().add("text-muted");
        certNumberLabel.setStyle("-fx-font-size: 12px; -fx-font-family: 'Courier New', monospace;");
        
        header.getChildren().addAll(titleLabel, certNumberLabel);

        // Details
        VBox details = new VBox(8);
        details.setStyle("-fx-padding: 12 0;");
        
        HBox dateRow = new HBox(8);
        dateRow.setAlignment(Pos.CENTER_LEFT);
        Label dateIcon = new Label("📅");
        Label dateLabel = new Label("Complété le: " + 
            (cert.getCompletedAt() != null ? cert.getCompletedAt().format(DATE_FORMAT) : 
             cert.getIssuedAt().format(DATE_FORMAT)));
        dateLabel.getStyleClass().add("text-muted");
        dateRow.getChildren().addAll(dateIcon, dateLabel);
        
        HBox categoryRow = new HBox(8);
        categoryRow.setAlignment(Pos.CENTER_LEFT);
        Label categoryIcon = new Label("📚");
        Label categoryLabel = new Label("Catégorie: " + training.getCategory().getDisplayName());
        categoryLabel.getStyleClass().add("text-muted");
        categoryRow.getChildren().addAll(categoryIcon, categoryLabel);
        
        details.getChildren().addAll(dateRow, categoryRow);

        // Action buttons
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);
        
        TLButton downloadBtn = new TLButton("📥 Télécharger", TLButton.ButtonVariant.PRIMARY);
        downloadBtn.setOnAction(e -> handleDownloadCertificate(cert, training));
        
        TLButton viewBtn = new TLButton("👁 Voir", TLButton.ButtonVariant.OUTLINE);
        viewBtn.setOnAction(e -> handleViewCertificate(cert, training));
        
        TLButton qrBtn = new TLButton("📱 QR Code", TLButton.ButtonVariant.OUTLINE);
        qrBtn.setOnAction(e -> handleShowQRCode(cert, training));
        
        actions.getChildren().addAll(downloadBtn, viewBtn, qrBtn);

        content.getChildren().addAll(header, details, actions);
        card.setContent(content);

        return card;
    }

    /**
     * Handles the "Download" button action.
     * Opens a FileChooser dialog to let the user choose where to save the certificate file.
     * The file is physically copied to the selected location.
     */
    private void handleDownloadCertificate(Certificate cert, Training training) {
        if (primaryStage == null) {
            logger.warn("Primary stage not set, cannot show FileChooser. Attempting to get from scene.");
            // Try to get stage from any node in the scene
            if (certificatesContainer.getScene() != null && certificatesContainer.getScene().getWindow() instanceof Stage) {
                primaryStage = (Stage) certificatesContainer.getScene().getWindow();
            } else {
                DialogUtils.showError(I18n.get("common.error"), 
                    "Impossible d'afficher le dialogue de sauvegarde. Veuillez réessayer.");
                return;
            }
        }

        Thread downloadThread = new Thread(() -> {
            try {
                // Generate certificate file if it doesn't exist
                String certFilePath = cert.getPdfPath();
                if (certFilePath == null || certFilePath.isEmpty() || !new File(certFilePath).exists()) {
                    logger.info("Generating certificate file for certificate id={}", cert.getId());
                    certFilePath = PDFGenerator.generateCertificate(cert, training, currentUser);
                    certificateService.updatePdfPath(cert.getId(), certFilePath);
                }

                final String sourceFilePath = certFilePath;
                File sourceFile = new File(sourceFilePath);
                
                if (!sourceFile.exists()) {
                    Platform.runLater(() -> {
                        DialogUtils.showError(I18n.get("common.error"), 
                            "Le fichier certificat n'a pas été trouvé.");
                    });
                    return;
                }

                // Generate a suggested file name
                String userName = currentUser.getFullName() != null && !currentUser.getFullName().trim().isEmpty()
                    ? currentUser.getFullName() : currentUser.getUsername();
                String trainingTitle = training.getTitle();
                String suggestedFileName = String.format("Certificate_%s_%s.html", 
                    sanitizeFileName(userName), sanitizeFileName(trainingTitle));

                Platform.runLater(() -> {
                    try {
                        // Show FileChooser dialog
                        FileChooser fileChooser = new FileChooser();
                        fileChooser.setTitle("Enregistrer le certificat");
                        fileChooser.setInitialFileName(suggestedFileName);
                        
                        // Set extension filter for HTML files (certificates are HTML)
                        FileChooser.ExtensionFilter htmlFilter = new FileChooser.ExtensionFilter(
                            "Fichiers HTML (*.html)", "*.html");
                        FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter(
                            "Tous les fichiers (*.*)", "*.*");
                        fileChooser.getExtensionFilters().addAll(htmlFilter, allFilter);
                        fileChooser.setSelectedExtensionFilter(htmlFilter);

                        // Show save dialog
                        File selectedFile = fileChooser.showSaveDialog(primaryStage);
                        
                        if (selectedFile == null) {
                            // User cancelled the dialog
                            logger.info("User cancelled certificate download dialog");
                            return;
                        }

                        // Ensure the file has .html extension if not specified
                        String selectedPath = selectedFile.getAbsolutePath();
                        if (!selectedPath.toLowerCase().endsWith(".html") && 
                            !selectedPath.toLowerCase().endsWith(".htm")) {
                            selectedFile = new File(selectedPath + ".html");
                        }

                        // Copy the source file to the selected location
                        final File targetFile = selectedFile;
                        Thread copyThread = new Thread(() -> {
                            try {
                                Files.copy(Paths.get(sourceFilePath), targetFile.toPath(), 
                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                
                                logger.info("Certificate saved successfully to: {}", targetFile.getAbsolutePath());
                                
                                Platform.runLater(() -> {
                                    DialogUtils.showSuccess(I18n.get("common.success"), 
                                        "Certificat enregistré avec succès:\n" + targetFile.getName());
                                });
                            } catch (IOException e) {
                                logger.error("Error copying certificate file to: {}", targetFile.getAbsolutePath(), e);
                                Platform.runLater(() -> {
                                    DialogUtils.showError(I18n.get("common.error"), 
                                        "Erreur lors de l'enregistrement du certificat: " + e.getMessage());
                                });
                            }
                        }, "CopyCertificateThread");
                        copyThread.setDaemon(true);
                        copyThread.start();

                    } catch (Exception e) {
                        logger.error("Error showing FileChooser or saving certificate", e);
                        DialogUtils.showError(I18n.get("common.error"), 
                            "Erreur lors de l'enregistrement: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                logger.error("Error preparing certificate download", e);
                Platform.runLater(() -> {
                    DialogUtils.showError(I18n.get("common.error"), 
                        "Erreur lors de la préparation du téléchargement: " + e.getMessage());
                });
            }
        }, "DownloadCertificateThread");
        downloadThread.setDaemon(true);
        downloadThread.start();
    }

    /**
     * Sanitize file name by removing invalid characters
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "Unknown";
        // Replace invalid file name characters with underscores
        return fileName.replaceAll("[<>:\"/\\\\|?*]", "_")
                      .replaceAll("\\s+", "_")
                      .trim();
    }

    /**
     * Handles the "View" button action.
     * Opens the certificate file in the default browser/viewer for preview only.
     * This does NOT save the file permanently to the user's computer.
     */
    private void handleViewCertificate(Certificate cert, Training training) {
        Thread viewThread = new Thread(() -> {
            try {
                // Generate certificate file if it doesn't exist
                String certFilePath = cert.getPdfPath();
                if (certFilePath == null || certFilePath.isEmpty() || !new File(certFilePath).exists()) {
                    logger.info("Generating certificate file for viewing, certificate id={}", cert.getId());
                    certFilePath = PDFGenerator.generateCertificate(cert, training, currentUser);
                    certificateService.updatePdfPath(cert.getId(), certFilePath);
                }

                final String filePath = certFilePath;
                File file = new File(filePath);
                
                if (!file.exists()) {
                    Platform.runLater(() -> {
                        DialogUtils.showError(I18n.get("common.error"), 
                            "Le fichier certificat n'a pas été trouvé.");
                    });
                    return;
                }

                // Open the file in the default browser/viewer (preview only, no save)
                Platform.runLater(() -> {
                    try {
                        if (java.awt.Desktop.isDesktopSupported()) {
                            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                            desktop.open(file);
                            logger.info("Opened certificate file for preview: {}", filePath);
                        } else {
                            DialogUtils.showInfo(I18n.get("common.info"), 
                                "Certificat disponible à: " + filePath);
                        }
                    } catch (Exception e) {
                        logger.error("Error opening certificate file for preview", e);
                        DialogUtils.showError(I18n.get("common.error"), 
                            "Erreur lors de l'ouverture du certificat: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                logger.error("Error preparing certificate for viewing", e);
                Platform.runLater(() -> {
                    DialogUtils.showError(I18n.get("common.error"), 
                        "Erreur lors de la préparation de l'aperçu: " + e.getMessage());
                });
            }
        }, "ViewCertificateThread");
        viewThread.setDaemon(true);
        viewThread.start();
    }

    private void handleShowQRCode(Certificate cert, Training training) {
        try {
            logger.info("=== QR Code Generation Started ===");
            logger.info("Certificate ID: {}, Training: {}", cert.getId(), training.getTitle());
            
            // 1. Validate certificate has ID or token
            String certId = cert.getCertificateNumber();
            if (certId == null || certId.trim().isEmpty()) {
                logger.warn("Certificate number is empty, trying verification token");
                certId = cert.getVerificationToken();
                if (certId == null || certId.trim().isEmpty()) {
                    String error = "Le certificat n'a pas d'identifiant valide. Impossible de générer le QR code sans identifiant de certificat.";
                    logger.error(error);
                    DialogUtils.showError("Erreur", error);
                    return;
                }
            }
            certId = certId.trim();
            logger.info("Using certificate ID: {}", certId);
            
            // 2. Check if server is running
            boolean serverRunning = com.skilora.service.formation.VerificationServer.isRunning();
            int serverPort = com.skilora.service.formation.VerificationServer.getPort();
            
            if (!serverRunning || serverPort == -1) {
                logger.warn("Verification server is not running. Attempting to start...");
                com.skilora.service.formation.VerificationServer.start();
                
                // Wait a bit for server to start
                try {
                    Thread.sleep(1000); // Increased wait time to 1 second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Check again if server started
                serverRunning = com.skilora.service.formation.VerificationServer.isRunning();
                serverPort = com.skilora.service.formation.VerificationServer.getPort();
                
                if (!serverRunning || serverPort == -1) {
                    String error = "Le serveur de vérification ne peut pas démarrer. " +
                            "Tous les ports sont occupés (8080-8129). " +
                            "Veuillez fermer les applications qui utilisent ces ports ou changer app.server.port dans application.properties";
                    logger.error(error);
                    DialogUtils.showError("Erreur Serveur", error);
                    return;
                }
            }
            
            logger.info("Server status: running={}, port={}", serverRunning, serverPort);
            
            // 3. Get local IP address
            String localIP = com.skilora.utils.NetworkUtils.getLocalIPAddress();
            if (localIP == null || localIP.isEmpty()) {
                String error = "Impossible de détecter l'adresse IP locale. Assurez-vous que votre ordinateur est connecté au WiFi.";
                logger.error(error);
                DialogUtils.showError("Erreur", error);
                return;
            }
            logger.info("Local IP address: {}", localIP);
            
            // 4. Build verification URL
            String qrUrl = String.format("http://%s:%d/verify/%s", localIP, serverPort, certId);
            logger.info("QR Code URL: {}", qrUrl);
            
            // 5. Validate URL
            if (qrUrl == null || qrUrl.trim().isEmpty() || !qrUrl.startsWith("http://")) {
                String error = "URL de vérification invalide: " + qrUrl;
                logger.error(error);
                DialogUtils.showError("Erreur", error);
                return;
            }
            
            // 6. Generate QR code image
            logger.info("Generating QR code image (400x400)...");
            BufferedImage qrImage;
            try {
                qrImage = QRCodeGenerator.generateQRCodeImage(qrUrl, 400, 400);
                logger.info("QR code image generated successfully");
            } catch (WriterException e) {
                String error = "Échec de la génération du QR code. Impossible de créer l'image du QR code: " + e.getMessage();
                logger.error("Failed to generate QR code image: {}", e.getMessage(), e);
                DialogUtils.showError("Erreur", error);
                return;
            }
            
            // 7. Convert to JavaFX Image
            Image jfxImage = SwingFXUtils.toFXImage(qrImage, null);
            if (jfxImage == null || jfxImage.isError()) {
                String error = "Échec de la conversion. Impossible de convertir l'image du QR code.";
                logger.error("Failed to convert QR code to JavaFX Image");
                DialogUtils.showError("Erreur", error);
                return;
            }
            logger.info("QR code image converted to JavaFX Image successfully");
            
            // 8. Create and show dialog
            TLDialog<ButtonType> dialog = new TLDialog<>();
            dialog.setDialogTitle("Code QR de Vérification");
            
            String description = String.format(
                "Scannez ce code QR avec votre téléphone pour afficher le certificat.\n\n" +
                "✓ Certificat: %s\n" +
                "✓ Formation: %s\n" +
                "✓ Téléphone sur le même WiFi requis\n" +
                "✓ Le certificat s'affichera dans le navigateur",
                certId, training.getTitle()
            );
            dialog.setDescription(description);
            
            VBox container = new VBox(20);
            container.setAlignment(Pos.CENTER);
            container.setPadding(new Insets(30));
            container.setStyle("-fx-background-color: transparent;");
            
            // QR Code Image with white background
            StackPane qrContainer = new StackPane();
            qrContainer.setStyle(
                "-fx-background-color: white; " +
                "-fx-background-radius: 15; " +
                "-fx-padding: 20; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 20, 0, 0, 10);"
            );
            
            ImageView qrView = new ImageView(jfxImage);
            qrView.setFitWidth(300);
            qrView.setFitHeight(300);
            qrView.setPreserveRatio(true);
            qrView.setSmooth(true);
            
            qrContainer.getChildren().add(qrView);
            
            // URL Label
            Label urlLabel = new Label(qrUrl);
            urlLabel.setStyle(
                "-fx-font-size: 11px; " +
                "-fx-text-fill: #a1a1aa; " +
                "-fx-wrap-text: true; " +
                "-fx-max-width: 400;"
            );
            urlLabel.setWrapText(true);
            urlLabel.setAlignment(Pos.CENTER);
            
            // Instruction
            Label instructionLabel = new Label("Scannez avec l'appareil photo de votre téléphone");
            instructionLabel.setStyle(
                "-fx-font-size: 14px; " +
                "-fx-text-fill: #fafafa; " +
                "-fx-font-weight: 600;"
            );
            
            container.getChildren().addAll(instructionLabel, qrContainer, urlLabel);
            dialog.setContent(container);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            
            // Premium Entrance Animation
            container.setOpacity(0);
            container.setScaleX(0.8);
            container.setScaleY(0.8);
            
            dialog.setOnShowing(event -> {
                FadeTransition fadeIn = new FadeTransition(Duration.millis(500), container);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                
                ScaleTransition scaleUp = new ScaleTransition(Duration.millis(500), container);
                scaleUp.setFromX(0.8);
                scaleUp.setFromY(0.8);
                scaleUp.setToX(1.0);
                scaleUp.setToY(1.0);
                scaleUp.setInterpolator(Interpolator.EASE_OUT);
                
                fadeIn.play();
                scaleUp.play();
            });
            
            dialog.showAndWait();
            
            logger.info("=== QR Code Generation Completed Successfully ===");
            
        } catch (IllegalArgumentException e) {
            logger.error("Cannot build verification URL for QR dialog: {}", e.getMessage(), e);
            DialogUtils.showError("Erreur QR Code",
                    "Impossible de générer le code QR: le certificat n'a pas de lien de vérification valide.\n\n" +
                    "Détails: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error showing QR code dialog: {}", e.getMessage(), e);
            DialogUtils.showError("Erreur QR Code",
                    "Une erreur inattendue s'est produite lors de la génération du code QR.\n\n" +
                    "Erreur: " + e.getMessage());
        }
    }

    private void openCertificateFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists() && java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(file);
            } else {
                DialogUtils.showInfo(I18n.get("common.info"), 
                    "Certificat disponible à: " + filePath);
            }
        } catch (Exception e) {
            logger.error("Error opening certificate file", e);
            DialogUtils.showError(I18n.get("common.error"), 
                "Erreur lors de l'ouverture: " + e.getMessage());
        }
    }
}
