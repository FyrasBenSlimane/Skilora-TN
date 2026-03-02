package com.skilora.formation.controller;

import com.skilora.formation.entity.Certificate;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.format.DateTimeFormatter;

/**
 * CertificateRenderer — generates a visual certificate card for completed formations.
 *
 * <p>Creates a rich, dark-themed certificate with:
 * <ul>
 *   <li>Gold ornamental border</li>
 *   <li>Professional typography</li>
 *   <li>Certificate number and QR hash</li>
 *   <li>Export to PNG and Print functionality</li>
 * </ul>
 */
public class CertificateRenderer {

    private static final Logger logger = LoggerFactory.getLogger(CertificateRenderer.class);

    private static final double CERT_WIDTH = 800;
    private static final double CERT_HEIGHT = 560;
    private static final Color GOLD = Color.web("#c9a84c");
    private static final Color GOLD_LIGHT = Color.web("#d4b86a");
    private static final Color TEXT_MUTED = Color.web("#71717a");

    /**
     * Build the full certificate view as a JavaFX node.
     *
     * @param cert          the Certificate entity
     * @param recipientName the full name of the certificate holder
     * @param formationTitle the title of the completed formation
     * @return a StackPane containing the rendered certificate
     */
    public static StackPane render(Certificate cert, String recipientName, String formationTitle) {
        StackPane card = new StackPane();
        card.setPrefSize(CERT_WIDTH, CERT_HEIGHT);
        card.setMaxSize(CERT_WIDTH, CERT_HEIGHT);
        card.setStyle("-fx-background-color: #111113; -fx-background-radius: 12;");

        // Gold border
        card.setBorder(new Border(new BorderStroke(
                GOLD, BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(2)
        )));

        // Drop shadow
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web("#c9a84c", 0.15));
        shadow.setRadius(30);
        shadow.setSpread(0.05);
        card.setEffect(shadow);

        // Content layout
        VBox content = new VBox(12);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40, 50, 30, 50));

        // Top ornamental line
        Line topLine = createOrnamentalLine();

        // "CERTIFICATE OF COMPLETION" header
        Label headerLabel = new Label("CERTIFICATE OF COMPLETION");
        headerLabel.setFont(Font.font("Inter", FontWeight.NORMAL, 11));
        headerLabel.setTextFill(GOLD);
        headerLabel.setStyle("-fx-letter-spacing: 8;");

        // Logo / Emblem
        Label emblem = new Label("✦ SKILORA ✦");
        emblem.setFont(Font.font("Inter", FontWeight.BOLD, 14));
        emblem.setTextFill(GOLD_LIGHT);

        // "This certifies that"
        Label certifiesLabel = new Label("This certifies that");
        certifiesLabel.setFont(Font.font("Inter", FontWeight.NORMAL, 12));
        certifiesLabel.setTextFill(TEXT_MUTED);

        // Recipient name
        Label nameLabel = new Label(recipientName != null ? recipientName : "—");
        nameLabel.setFont(Font.font("Inter", FontWeight.BOLD, 28));
        nameLabel.setTextFill(Color.WHITE);

        // Separator
        Line nameLine = new Line(0, 0, 300, 0);
        nameLine.setStroke(Color.web("#27272a"));
        nameLine.setStrokeWidth(1);

        // "has successfully completed"
        Label completedLabel = new Label("has successfully completed the formation");
        completedLabel.setFont(Font.font("Inter", FontWeight.NORMAL, 12));
        completedLabel.setTextFill(TEXT_MUTED);

        // Formation title
        Label formationLabel = new Label(formationTitle != null ? formationTitle : "—");
        formationLabel.setFont(Font.font("Inter", FontWeight.SEMI_BOLD, 18));
        formationLabel.setTextFill(GOLD_LIGHT);
        formationLabel.setWrapText(true);
        formationLabel.setTextAlignment(TextAlignment.CENTER);

        // Date + Certificate number
        String dateStr = cert.getIssuedDate() != null
                ? cert.getIssuedDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))
                : "—";
        Label dateLabel = new Label("Issued: " + dateStr);
        dateLabel.setFont(Font.font("Inter", FontWeight.NORMAL, 10));
        dateLabel.setTextFill(TEXT_MUTED);

        Label certNumLabel = new Label("Certificate No: " + (cert.getCertificateNumber() != null ? cert.getCertificateNumber() : "—"));
        certNumLabel.setFont(Font.font("SF Mono", FontWeight.NORMAL, 9));
        certNumLabel.setTextFill(Color.web("#3f3f46"));

        // Hash verification
        String hashShort = cert.getHashValue() != null && cert.getHashValue().length() > 16
                ? cert.getHashValue().substring(0, 16) + "..."
                : (cert.getHashValue() != null ? cert.getHashValue() : "—");
        Label hashLabel = new Label("Verification: " + hashShort);
        hashLabel.setFont(Font.font("SF Mono", FontWeight.NORMAL, 8));
        hashLabel.setTextFill(Color.web("#27272a"));

        // Bottom ornamental line
        Line bottomLine = createOrnamentalLine();

        // Spacing
        Region spacer1 = new Region();
        spacer1.setPrefHeight(4);
        Region spacer2 = new Region();
        spacer2.setPrefHeight(8);

        // ── Signature blocks ──
        HBox signatureRow = buildSignatureRow();

        content.getChildren().addAll(
                topLine,
                emblem,
                headerLabel,
                spacer1,
                certifiesLabel,
                nameLabel,
                nameLine,
                completedLabel,
                formationLabel,
                spacer2,
                signatureRow,
                dateLabel,
                certNumLabel,
                hashLabel,
                bottomLine
        );

        card.getChildren().add(content);
        return card;
    }

    /**
     * Export the certificate to a PNG file.
     *
     * @param certNode the rendered certificate StackPane
     * @param stage    the parent stage for the file chooser
     */
    public static void exportToPNG(StackPane certNode, Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Certificate");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        chooser.setInitialFileName("skilora_certificate.png");

        File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        try {
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            WritableImage snapshot = certNode.snapshot(params, null);

            BufferedImage bImage = javafx.embed.swing.SwingFXUtils.fromFXImage(snapshot, null);
            ImageIO.write(bImage, "png", file);

            logger.info("Certificate exported to: {}", file.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Failed to export certificate: {}", e.getMessage(), e);
        }
    }

    /**
     * Print the certificate using the system printer.
     *
     * @param certNode the rendered certificate StackPane
     */
    public static void print(StackPane certNode) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(certNode.getScene().getWindow())) {
            boolean success = job.printPage(certNode);
            if (success) {
                job.endJob();
                logger.info("Certificate printed successfully.");
            }
        }
    }

    /**
     * Build a row with two signature blocks: Director and Platform.
     */
    private static HBox buildSignatureRow() {
        HBox row = new HBox(60);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(8, 0, 4, 0));
        row.getChildren().addAll(
                buildSignatureBlock("Director", "Formation Director"),
                buildSignatureBlock("Skilora", "Platform")
        );
        return row;
    }

    /**
     * Build a single signature block with a line, name, and title.
     */
    private static VBox buildSignatureBlock(String name, String title) {
        VBox block = new VBox(4);
        block.setAlignment(Pos.CENTER);
        block.setMinWidth(160);

        Line sigLine = new Line(0, 0, 140, 0);
        sigLine.setStroke(Color.web("#3f3f46"));
        sigLine.setStrokeWidth(0.8);

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Inter", FontWeight.SEMI_BOLD, 10));
        nameLabel.setTextFill(Color.web("#d4d4d8"));

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Inter", FontWeight.NORMAL, 8));
        titleLabel.setTextFill(TEXT_MUTED);

        block.getChildren().addAll(sigLine, nameLabel, titleLabel);
        return block;
    }

    private static Line createOrnamentalLine() {
        Line line = new Line(0, 0, 200, 0);
        line.setStroke(GOLD);
        line.setStrokeWidth(0.5);
        line.setOpacity(0.4);
        return line;
    }
}
