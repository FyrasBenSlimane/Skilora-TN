package com.skilora.formation.controller;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;
import com.skilora.formation.entity.Certificate;
import com.skilora.formation.service.CertificateVerificationServer;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.layouts.TLAppLayout;
import com.skilora.utils.SvgIcons;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

/**
 * CertificateViewController — modal dialog displaying a visual certificate
 * with options to download as PDF and show QR code.
 *
 * <p>Usage:
 * <pre>
 *   CertificateViewController.show(parentStage, certificate, "Ahmed B.", "JavaFX Mastery");
 * </pre>
 */
public class CertificateViewController {

    private static final Logger logger = LoggerFactory.getLogger(CertificateViewController.class);

    /**
     * Show the certificate in a modal dialog (no formation director signature).
     */
    public static void show(Stage owner, Certificate cert, String recipientName, String formationTitle) {
        show(owner, cert, recipientName, formationTitle, null);
    }

    /**
     * Show the certificate in a modal dialog.
     *
     * @param owner                   the parent stage
     * @param cert                    the Certificate entity
     * @param recipientName           the full name of the holder
     * @param formationTitle         the formation title
     * @param directorSignatureBase64 optional base64 PNG of the formation director's signature (from Formation.directorSignature)
     */
    public static void show(Stage owner, Certificate cert, String recipientName, String formationTitle, String directorSignatureBase64) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("Certificate — " + (recipientName != null ? recipientName : ""));

        // Render the certificate card (includes director signature image when provided)
        StackPane certNode = CertificateRenderer.render(cert, recipientName, formationTitle, directorSignatureBase64);

        // Wrap certificate in a scrollable area
        ScrollPane certScroll = new ScrollPane(certNode);
        certScroll.setFitToWidth(true);
        certScroll.setFitToHeight(true);
        certScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        certScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        certScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        certScroll.setPannable(true);
        VBox.setVgrow(certScroll, Priority.ALWAYS);

        // ── Action bar ──
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(16, 0, 0, 0));

        TLButton downloadPdfBtn = new TLButton("Download PDF", TLButton.ButtonVariant.OUTLINE);
        downloadPdfBtn.setGraphic(SvgIcons.icon(SvgIcons.SAVE, 14));
        downloadPdfBtn.setOnAction(e -> saveCertificateAsPdf(certNode, recipientName, formationTitle, dialog));

        TLButton qrBtn = new TLButton("QR Code", TLButton.ButtonVariant.OUTLINE);
        qrBtn.setGraphic(SvgIcons.icon(SvgIcons.SHIELD_CHECK, 14));
        qrBtn.setOnAction(e -> showQRCodeDialog(cert, formationTitle, dialog));

        // Spacer pushes close button to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TLButton closeBtn = new TLButton("Close", TLButton.ButtonVariant.SECONDARY);
        closeBtn.setOnAction(e -> dialog.close());

        actions.getChildren().addAll(downloadPdfBtn, qrBtn, spacer, closeBtn);

        // ── Container ──
        VBox container = new VBox(0);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(24, 28, 20, 28));
        container.setStyle("-fx-background-color: #09090b; -fx-background-radius: 16;");
        container.setBorder(new Border(new BorderStroke(
                Color.web("#1f1f23"),
                BorderStrokeStyle.SOLID,
                new CornerRadii(16),
                new BorderWidths(1)
        )));
        container.setMaxWidth(920);
        container.setMaxHeight(700);
        container.getChildren().addAll(certScroll, actions);

        StackPane root = new StackPane(container);
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);");
        root.setPadding(new Insets(30));
        root.getStyleClass().add("root");
        // Click outside to close
        root.setOnMouseClicked(e -> {
            if (e.getTarget() == root) dialog.close();
        });

        Scene scene = new Scene(root, 980, 780);
        scene.setFill(Color.TRANSPARENT);
        TLAppLayout.applyStylesheets(scene);

        // Close on escape
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) dialog.close();
        });

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    /**
     * Save the certificate as a single-page PDF (snapshot of the rendered certificate).
     * Public so the inline certificate view in FormationsController can reuse it.
     */
    public static void saveCertificateAsPdf(StackPane certNode, String recipientName, String formationTitle, Stage owner) {
        String safeName = sanitizeFileName(recipientName != null ? recipientName : "Certificate");
        String safeTitle = sanitizeFileName(formationTitle != null ? formationTitle : "Formation");
        String suggestedName = "Certificate_" + safeName + "_" + safeTitle + ".pdf";

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Certificate as PDF");
        fileChooser.setInitialFileName(suggestedName);
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Document (*.pdf)", "*.pdf"),
                new FileChooser.ExtensionFilter("All files (*.*)", "*.*")
        );

        File target = fileChooser.showSaveDialog(owner);
        if (target == null) return;

        try {
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            WritableImage snapshot = certNode.snapshot(params, null);
            BufferedImage bImage = SwingFXUtils.fromFXImage(snapshot, null);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bImage, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, new FileOutputStream(target));
            document.open();

            com.lowagie.text.Image img = com.lowagie.text.Image.getInstance(imageBytes);
            float pageWidth = document.getPageSize().getWidth() - 72;
            float pageHeight = document.getPageSize().getHeight() - 72;
            img.scaleToFit(pageWidth, pageHeight);
            img.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(img);

            document.close();
            logger.info("Certificate PDF saved to: {}", target.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Error saving certificate PDF", e);
        }
    }

    /**
     * Show a QR code verification dialog for the certificate.
     * Public so it can be called from FormationsController inline view.
     */
    public static void showQRCodeDialog(Certificate cert, String formationTitle, Stage owner) {
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
            // Ensure the server is running
            if (!CertificateVerificationServer.isRunning()) {
                CertificateVerificationServer.start();
            }
            int serverPort = CertificateVerificationServer.getPort();
            if (serverPort < 0) {
                logger.warn("Verification server is not running, cannot generate QR code");
                return;
            }

            String localIP = CertificateVerificationServer.getLocalIPAddress();
            if (localIP == null || localIP.isEmpty()) {
                logger.warn("Cannot determine local IP for QR code");
                return;
            }

            String qrUrl = "http://" + localIP + ":" + serverPort + "/verify/" + certId.trim();
            String localhostUrl = "http://localhost:" + serverPort + "/verify/" + certId.trim();
            logger.info("QR Code URL: {}", qrUrl);

            boolean isLanIP = localIP.startsWith("192.168.") || localIP.startsWith("10.") ||
                    (localIP.startsWith("172.") && Integer.parseInt(localIP.split("\\.")[1]) >= 16
                            && Integer.parseInt(localIP.split("\\.")[1]) <= 31);

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

            VBox content = new VBox(16);
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

            // Show warning if IP is not a proper LAN address
            if (!isLanIP) {
                Label warnLabel = new Label("⚠ Your Mac doesn't have a proper WiFi IP. "
                        + "Phone must be on the same WiFi network.\n"
                        + "On this computer, open: " + localhostUrl);
                warnLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #f59e0b; -fx-wrap-text: true; -fx-max-width: 380;");
                warnLabel.setWrapText(true);
                warnLabel.setAlignment(Pos.CENTER);
                content.getChildren().addAll(titleLabel, qrContainer, urlLabel, warnLabel);
            } else {
                content.getChildren().addAll(titleLabel, qrContainer, urlLabel);
            }

            TLButton closeBtn = new TLButton("Close");
            closeBtn.getStyleClass().add("btn-ghost");
            closeBtn.setOnAction(e -> qrDialog.close());

            content.getChildren().add(closeBtn);

            // Entrance animation
            content.setOpacity(0);
            content.setScaleX(0.85);
            content.setScaleY(0.85);

            StackPane qrRoot = new StackPane(content);
            qrRoot.setStyle("-fx-background-color: rgba(0,0,0,0.85);");
            qrRoot.setPadding(new Insets(40));
            qrRoot.getStyleClass().add("root");

            Scene qrScene = new Scene(qrRoot, 500, 560);
            qrScene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            TLAppLayout.applyStylesheets(qrScene);
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
