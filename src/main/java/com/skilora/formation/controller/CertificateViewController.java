package com.skilora.formation.controller;

import com.skilora.formation.entity.Certificate;
import com.skilora.formation.service.CertificateVerificationServer;
import com.skilora.framework.components.TLButton;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * CertificateViewController — modal dialog displaying a visual certificate
 * with options to export as PNG, print, download, and show QR code.
 *
 * <p>Usage:
 * <pre>
 *   CertificateViewController.show(parentStage, certificate, "Ahmed B.", "JavaFX Mastery");
 * </pre>
 */
public class CertificateViewController {

    private static final Logger logger = LoggerFactory.getLogger(CertificateViewController.class);

    /**
     * Show the certificate in a modal dialog.
     *
     * @param owner          the parent stage
     * @param cert           the Certificate entity
     * @param recipientName  the full name of the holder
     * @param formationTitle the formation title
     */
    public static void show(Stage owner, Certificate cert, String recipientName, String formationTitle) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("Certificate — " + (recipientName != null ? recipientName : ""));

        // Render the certificate card
        StackPane certNode = CertificateRenderer.render(cert, recipientName, formationTitle);

        // Action buttons
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(16, 0, 0, 0));

        TLButton exportBtn = new TLButton("Export PNG");
        exportBtn.getStyleClass().add("btn-outline");
        exportBtn.setOnAction(e -> CertificateRenderer.exportToPNG(certNode, dialog));

        TLButton printBtn = new TLButton("Print");
        printBtn.getStyleClass().add("btn-outline");
        printBtn.setOnAction(e -> CertificateRenderer.print(certNode));

        TLButton downloadBtn = new TLButton("Download");
        downloadBtn.getStyleClass().add("btn-outline");
        downloadBtn.setOnAction(e -> handleDownload(cert, recipientName, formationTitle, dialog));

        TLButton qrBtn = new TLButton("QR Code");
        qrBtn.getStyleClass().add("btn-outline");
        qrBtn.setOnAction(e -> handleShowQRCode(cert, formationTitle, dialog));

        TLButton closeBtn = new TLButton("Close");
        closeBtn.getStyleClass().add("btn-ghost");
        closeBtn.setOnAction(e -> dialog.close());

        actions.getChildren().addAll(exportBtn, printBtn, downloadBtn, qrBtn, closeBtn);

        // Container
        VBox container = new VBox(16);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(32));
        container.setStyle("-fx-background-color: #09090b; -fx-background-radius: 16;");
        container.setBorder(new Border(new BorderStroke(
                javafx.scene.paint.Color.web("#1f1f23"),
                BorderStrokeStyle.SOLID,
                new CornerRadii(16),
                new BorderWidths(1)
        )));
        container.getChildren().addAll(certNode, actions);

        StackPane root = new StackPane(container);
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);");
        root.setPadding(new Insets(40));

        Scene scene = new Scene(root, 960, 760);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        // Close on escape
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) dialog.close();
        });

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    /**
     * Download the certificate file via FileChooser.
     */
    private static void handleDownload(Certificate cert, String recipientName, String formationTitle, Stage owner) {
        String pdfPath = cert.getPdfUrl();
        if (pdfPath == null || pdfPath.isBlank()) {
            logger.info("No PDF path for certificate {}", cert.getId());
            return;
        }

        File source = new File(pdfPath);
        if (!source.exists()) {
            logger.warn("Certificate file not found: {}", pdfPath);
            return;
        }

        String safeName = sanitizeFileName(recipientName != null ? recipientName : "Certificate");
        String safeTitle = sanitizeFileName(formationTitle != null ? formationTitle : "Formation");
        String suggestedName = "Certificate_" + safeName + "_" + safeTitle + ".html";

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Certificate");
        fileChooser.setInitialFileName(suggestedName);
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("HTML files (*.html)", "*.html"),
                new FileChooser.ExtensionFilter("All files (*.*)", "*.*")
        );

        File target = fileChooser.showSaveDialog(owner);
        if (target == null) return;

        try {
            Files.copy(Paths.get(pdfPath), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Certificate saved to: {}", target.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Error saving certificate", e);
        }
    }

    /**
     * Show a QR code verification dialog for the certificate.
     */
    private static void handleShowQRCode(Certificate cert, String formationTitle, Stage owner) {
        try {
            String certId = cert.getCertificateNumber();
            if (certId == null || certId.isBlank()) {
                certId = cert.getVerificationToken();
            }
            if (certId == null || certId.isBlank()) {
                logger.warn("No certificate identifier for QR code");
                return;
            }

            // Get verification URL from CertificateVerificationServer
            String localIP = CertificateVerificationServer.getLocalIPAddress();
            if (localIP == null || localIP.isEmpty()) {
                logger.warn("Cannot determine local IP for QR code");
                return;
            }

            String qrUrl = "http://" + localIP + ":8443/verify/" + certId.trim();
            logger.info("QR Code URL: {}", qrUrl);

            // Generate QR code
            BufferedImage qrImage = com.google.zxing.client.j2se.MatrixToImageWriter.toBufferedImage(
                    new com.google.zxing.qrcode.QRCodeWriter().encode(
                            qrUrl,
                            com.google.zxing.BarcodeFormat.QR_CODE,
                            400, 400));

            Image jfxImage = SwingFXUtils.toFXImage(qrImage, null);

            // Build dialog
            Stage qrDialog = new Stage();
            qrDialog.initModality(Modality.APPLICATION_MODAL);
            qrDialog.initOwner(owner);
            qrDialog.initStyle(StageStyle.UNDECORATED);
            qrDialog.setTitle("QR Code - " + (formationTitle != null ? formationTitle : "Certificate"));

            VBox content = new VBox(20);
            content.setAlignment(Pos.CENTER);
            content.setPadding(new Insets(30));
            content.setStyle("-fx-background-color: #09090b; -fx-background-radius: 16;");

            Label titleLabel = new Label("Scan to verify certificate");
            titleLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #fafafa; -fx-font-weight: 600;");

            StackPane qrContainer = new StackPane();
            qrContainer.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 16; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 15, 0, 0, 8);");
            ImageView qrView = new ImageView(jfxImage);
            qrView.setFitWidth(280);
            qrView.setFitHeight(280);
            qrView.setPreserveRatio(true);
            qrContainer.getChildren().add(qrView);

            Label urlLabel = new Label(qrUrl);
            urlLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #71717a; -fx-wrap-text: true; -fx-max-width: 360;");
            urlLabel.setWrapText(true);
            urlLabel.setAlignment(Pos.CENTER);

            TLButton closeBtn = new TLButton("Close");
            closeBtn.getStyleClass().add("btn-ghost");
            closeBtn.setOnAction(e -> qrDialog.close());

            content.getChildren().addAll(titleLabel, qrContainer, urlLabel, closeBtn);

            // Entrance animation
            content.setOpacity(0);
            content.setScaleX(0.85);
            content.setScaleY(0.85);

            StackPane qrRoot = new StackPane(content);
            qrRoot.setStyle("-fx-background-color: rgba(0,0,0,0.85);");
            qrRoot.setPadding(new Insets(40));

            Scene qrScene = new Scene(qrRoot, 500, 560);
            qrScene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            qrScene.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) qrDialog.close();
            });

            qrDialog.setScene(qrScene);

            // Animate on showing
            FadeTransition fadeIn = new FadeTransition(Duration.millis(400), content);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(400), content);
            scaleUp.setFromX(0.85);
            scaleUp.setFromY(0.85);
            scaleUp.setToX(1.0);
            scaleUp.setToY(1.0);
            scaleUp.setInterpolator(Interpolator.EASE_OUT);

            javafx.application.Platform.runLater(() -> {
                fadeIn.play();
                scaleUp.play();
            });

            qrDialog.showAndWait();

        } catch (Exception e) {
            logger.error("Error generating QR code", e);
        }
    }

    private static String sanitizeFileName(String name) {
        if (name == null) return "Unknown";
        return name.replaceAll("[<>:\"/\\\\|?*]", "_").replaceAll("\\s+", "_").trim();
    }
}
