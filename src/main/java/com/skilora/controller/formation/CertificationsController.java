package com.skilora.controller.formation;

import com.skilora.framework.components.*;
import com.skilora.model.entity.formation.Certificate;
import com.skilora.model.entity.formation.Training;
import com.skilora.model.entity.usermanagement.User;
import com.skilora.service.formation.*;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;
import com.skilora.utils.PDFGenerator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
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
        
        actions.getChildren().addAll(downloadBtn, viewBtn);

        content.getChildren().addAll(header, details, actions);
        card.setContent(content);

        return card;
    }

    private void handleDownloadCertificate(Certificate cert, Training training) {
        Thread downloadThread = new Thread(() -> {
            try {
                // Generate PDF if not exists
                String pdfPath = cert.getPdfPath();
                if (pdfPath == null || pdfPath.isEmpty() || !new File(pdfPath).exists()) {
                    logger.info("Generating PDF for certificate id={}", cert.getId());
                    pdfPath = PDFGenerator.generateCertificate(cert, training, currentUser);
                    certificateService.updatePdfPath(cert.getId(), pdfPath);
                }

                final String finalPdfPath = pdfPath;
                File file = new File(finalPdfPath);
                
                if (file.exists()) {
                    Platform.runLater(() -> {
                        try {
                            // Try to open the file directly, fallback to opening folder
                            if (java.awt.Desktop.isDesktopSupported()) {
                                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                                desktop.open(file);
                                DialogUtils.showSuccess(I18n.get("common.success"), 
                                    I18n.get("certificates.download.success", file.getName()));
                            } else {
                                DialogUtils.showInfo(I18n.get("common.info"), 
                                    "Certificat disponible à: " + finalPdfPath);
                            }
                        } catch (Exception e) {
                            logger.error("Error opening certificate file", e);
                            DialogUtils.showError(I18n.get("common.error"), 
                                "Erreur lors de l'ouverture du certificat: " + e.getMessage());
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        DialogUtils.showError(I18n.get("common.error"), 
                            "Le fichier certificat n'a pas été trouvé.");
                    });
                }
            } catch (Exception e) {
                logger.error("Error downloading certificate", e);
                Platform.runLater(() -> {
                    DialogUtils.showError(I18n.get("common.error"), 
                        "Erreur lors du téléchargement: " + e.getMessage());
                });
            }
        }, "DownloadCertificateThread");
        downloadThread.setDaemon(true);
        downloadThread.start();
    }

    private void handleViewCertificate(Certificate cert, Training training) {
        // Open certificate in browser/viewer
        String pdfPath = cert.getPdfPath();
        if (pdfPath == null || pdfPath.isEmpty() || !new File(pdfPath).exists()) {
            // Generate if not exists
            Thread genThread = new Thread(() -> {
                try {
                    String path = PDFGenerator.generateCertificate(cert, training, currentUser);
                    certificateService.updatePdfPath(cert.getId(), path);
                    Platform.runLater(() -> openCertificateFile(path));
                } catch (Exception e) {
                    logger.error("Error generating certificate for viewing", e);
                    Platform.runLater(() -> {
                        DialogUtils.showError(I18n.get("common.error"), 
                            "Erreur lors de la génération du certificat: " + e.getMessage());
                    });
                }
            }, "GenerateCertificateThread");
            genThread.setDaemon(true);
            genThread.start();
        } else {
            openCertificateFile(pdfPath);
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
