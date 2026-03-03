package com.skilora.formation.controller;

import com.skilora.formation.entity.Certificate;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;

/**
 * CertificateRenderer — generates a visual certificate card for completed formations.
 *
 * <p>Creates a rich, dark-themed certificate with:
 * <ul>
 *   <li>Gold ornamental border</li>
 *   <li>Professional typography</li>
 *   <li>Certificate number and QR hash</li>
 *   <li>Export to PDF functionality</li>
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
     * Build the full certificate view as a JavaFX node (no formation director signature).
     */
    public static StackPane render(Certificate cert, String recipientName, String formationTitle) {
        return render(cert, recipientName, formationTitle, null);
    }

    /**
     * Build the full certificate view as a JavaFX node.
     *
     * @param cert                   the Certificate entity
     * @param recipientName          the full name of the certificate holder
     * @param formationTitle         the title of the completed formation
     * @param directorSignatureBase64 optional base64-encoded PNG of the formation director's signature (from Formation.directorSignature)
     * @return a StackPane containing the rendered certificate
     */
    public static StackPane render(Certificate cert, String recipientName, String formationTitle, String directorSignatureBase64) {
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
        headerLabel.setStyle("-fx-text-fill: #c9a84c; -fx-letter-spacing: 8;");

        // Logo / Emblem
        Label emblem = new Label("✦ SKILORA ✦");
        emblem.setFont(Font.font("Inter", FontWeight.BOLD, 14));
        emblem.setTextFill(GOLD_LIGHT);
        emblem.setStyle("-fx-text-fill: #d4b86a;");

        // "This certifies that"
        Label certifiesLabel = new Label("This certifies that");
        certifiesLabel.setFont(Font.font("Inter", FontWeight.NORMAL, 12));
        certifiesLabel.setTextFill(TEXT_MUTED);
        certifiesLabel.setStyle("-fx-text-fill: #71717a;");

        // Recipient name
        Label nameLabel = new Label(recipientName != null ? recipientName : "—");
        nameLabel.setFont(Font.font("Inter", FontWeight.BOLD, 28));
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setStyle("-fx-text-fill: white;");

        // Separator
        Line nameLine = new Line(0, 0, 300, 0);
        nameLine.setStroke(Color.web("#27272a"));
        nameLine.setStrokeWidth(1);

        // "has successfully completed"
        Label completedLabel = new Label("has successfully completed the formation");
        completedLabel.setFont(Font.font("Inter", FontWeight.NORMAL, 12));
        completedLabel.setTextFill(TEXT_MUTED);
        completedLabel.setStyle("-fx-text-fill: #71717a;");

        // Formation title
        Label formationLabel = new Label(formationTitle != null ? formationTitle : "—");
        formationLabel.setFont(Font.font("Inter", FontWeight.SEMI_BOLD, 18));
        formationLabel.setTextFill(GOLD_LIGHT);
        formationLabel.setStyle("-fx-text-fill: #d4b86a;");
        formationLabel.setWrapText(true);
        formationLabel.setTextAlignment(TextAlignment.CENTER);

        // Date + Certificate number
        String dateStr = cert.getIssuedDate() != null
                ? cert.getIssuedDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))
                : "—";
        Label dateLabel = new Label("Issued: " + dateStr);
        dateLabel.setFont(Font.font("Inter", FontWeight.NORMAL, 10));
        dateLabel.setTextFill(TEXT_MUTED);
        dateLabel.setStyle("-fx-text-fill: #71717a;");

        Label certNumLabel = new Label("Certificate No: " + (cert.getCertificateNumber() != null ? cert.getCertificateNumber() : "—"));
        certNumLabel.setFont(Font.font("SF Mono", FontWeight.NORMAL, 9));
        certNumLabel.setTextFill(Color.web("#a1a1aa"));
        certNumLabel.setStyle("-fx-text-fill: #a1a1aa;");

        // Hash verification
        String hashShort = cert.getHashValue() != null && cert.getHashValue().length() > 16
                ? cert.getHashValue().substring(0, 16) + "..."
                : (cert.getHashValue() != null ? cert.getHashValue() : "—");
        Label hashLabel = new Label("Verification: " + hashShort);
        hashLabel.setFont(Font.font("SF Mono", FontWeight.NORMAL, 8));
        hashLabel.setTextFill(Color.web("#71717a"));
        hashLabel.setStyle("-fx-text-fill: #71717a;");

        // Bottom ornamental line
        Line bottomLine = createOrnamentalLine();

        // Spacing
        Region spacer1 = new Region();
        spacer1.setPrefHeight(4);
        Region spacer2 = new Region();
        spacer2.setPrefHeight(8);

        // ── Signature blocks (left = director signature image or placeholder; right = Skilora) ──
        HBox signatureRow = buildSignatureRow(directorSignatureBase64);

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
     * Build a row with two signature blocks: Director (image or placeholder) and Platform.
     *
     * @param directorSignatureBase64 optional base64 PNG of the formation director's signature
     */
    private static HBox buildSignatureRow(String directorSignatureBase64) {
        HBox row = new HBox(60);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(8, 0, 4, 0));
        VBox leftBlock = isDirectorSignaturePresent(directorSignatureBase64)
                ? buildDirectorSignatureImageBlock(directorSignatureBase64)
                : buildSignatureBlock("Director", "Formation Director");
        row.getChildren().addAll(leftBlock, buildSignatureBlock("Skilora", "Platform"));
        return row;
    }

    private static boolean isDirectorSignaturePresent(String base64) {
        return base64 != null && !base64.isBlank();
    }

    /**
     * Build the left signature block with the formation director's drawn signature image.
     * Uses a data URL so the Image loads without leaving streams open.
     */
    private static VBox buildDirectorSignatureImageBlock(String directorSignatureBase64) {
        VBox block = new VBox(4);
        block.setAlignment(Pos.CENTER);
        block.setMinWidth(160);
        try {
            String dataUrl = "data:image/png;base64," + directorSignatureBase64.trim();
            Image img = new Image(dataUrl);
            if (img.isError()) {
                logger.debug("Director signature image failed to load, using placeholder");
                return buildSignatureBlock("Director", "Formation Director");
            }
            ImageView iv = new ImageView(img);
            iv.setFitWidth(140);
            iv.setFitHeight(56);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            block.getChildren().add(iv);
        } catch (Exception e) {
            logger.debug("Could not decode director signature image, using placeholder: {}", e.getMessage());
            return buildSignatureBlock("Director", "Formation Director");
        }
        Label titleLabel = new Label("Formation Director");
        titleLabel.setFont(Font.font("Inter", FontWeight.NORMAL, 9));
        titleLabel.setTextFill(GOLD_LIGHT);
        titleLabel.setStyle("-fx-text-fill: #d4b86a;");
        block.getChildren().add(titleLabel);
        return block;
    }

    /**
     * Build a single signature block with a line, name, and title.
     * Uses gold for the signature name and line so they are clearly visible on the dark certificate.
     */
    private static VBox buildSignatureBlock(String name, String title) {
        VBox block = new VBox(4);
        block.setAlignment(Pos.CENTER);
        block.setMinWidth(160);

        Line sigLine = new Line(0, 0, 140, 0);
        sigLine.setStroke(GOLD);
        sigLine.setStrokeWidth(1.2);

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Inter", FontWeight.SEMI_BOLD, 11));
        nameLabel.setTextFill(GOLD);
        nameLabel.setStyle("-fx-text-fill: #c9a84c;");

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Inter", FontWeight.NORMAL, 9));
        titleLabel.setTextFill(GOLD_LIGHT);
        titleLabel.setStyle("-fx-text-fill: #d4b86a;");

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
