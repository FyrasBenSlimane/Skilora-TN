package com.skilora.user.ui;

import com.skilora.framework.components.TLDialog;
import com.skilora.utils.I18n;
import com.skilora.utils.ImageUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Optional;

/**
 * Professional dialog to crop a profile photo to a circle.
 * Supports zoom, drag-to-pan, auto-resize (fill circle), and pan clamping
 * so the crop area never leaves the image.
 */
public final class PhotoCropDialog {

    private static final double PREVIEW_SIZE = 400;
    private static final double CROP_RADIUS = 140;

    /** Zoom range: 0.5x (fit) to 3x for detail. */
    private static final double ZOOM_MIN = 0.5;
    private static final double ZOOM_MAX = 3.0;
    private static final double ZOOM_DEFAULT = 1.0;

    private PhotoCropDialog() {}

    /**
     * Opens a file chooser, then if a file is selected shows the crop dialog.
     */
    public static Optional<String> chooseAndCrop(Stage ownerStage) {
        FileChooser fc = new FileChooser();
        fc.setTitle(I18n.get("profile.photo.choose_title"));
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(I18n.get("profile.photo.filter_images"),
                "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp", "*.bmp"));
        File file = fc.showOpenDialog(ownerStage);
        if (file == null) return Optional.empty();
        String err = ImageUtils.validateImageFile(file);
        if (err != null) return Optional.empty();
        return showCropDialog(ownerStage, file);
    }

    /**
     * Shows the crop dialog for an already-selected image file.
     */
    public static Optional<String> showCropDialog(Stage ownerStage, File imageFile) {
        BufferedImage bufferedImage;
        try {
            bufferedImage = ImageIO.read(imageFile);
        } catch (Exception e) {
            return Optional.empty();
        }
        if (bufferedImage == null) return Optional.empty();

        Image fxImage = new Image(imageFile.toURI().toString());
        if (fxImage.isError()) return Optional.empty();

        double imgW = fxImage.getWidth();
        double imgH = fxImage.getHeight();
        if (imgW <= 0 || imgH <= 0) return Optional.empty();

        // Initial scale: fit in preview, but at least fill the circle (so small images don't look tiny)
        double fitScale = Math.min(PREVIEW_SIZE / imgW, PREVIEW_SIZE / imgH);
        double fillCircleScale = (2 * CROP_RADIUS) / Math.min(imgW, imgH);
        final double initialScale = Math.min(
            Math.max(fitScale, fillCircleScale),
            Math.max(PREVIEW_SIZE / imgW, PREVIEW_SIZE / imgH) * 1.5);

        ImageView imageView = new ImageView(fxImage);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        // Pan offset (in view coords); updated by drag and clamped
        double[] offsetX = { 0 };
        double[] offsetY = { 0 };

        // Zoom level (1 = initial scale)
        double[] zoom = { ZOOM_DEFAULT };
        double[] displayScale = { initialScale * zoom[0] };
        double[] displayW = { imgW * displayScale[0] };
        double[] displayH = { imgH * displayScale[0] };

        Runnable updateImageSize = () -> {
            displayScale[0] = initialScale * zoom[0];
            displayW[0] = imgW * displayScale[0];
            displayH[0] = imgH * displayScale[0];
            imageView.setFitWidth(displayW[0]);
            imageView.setFitHeight(displayH[0]);
            clampPan(offsetX, offsetY, displayW[0], displayH[0], displayScale[0], imgW, imgH);
            imageView.setTranslateX(offsetX[0]);
            imageView.setTranslateY(offsetY[0]);
        };

        // Initial size and center
        imageView.setFitWidth(displayW[0]);
        imageView.setFitHeight(displayH[0]);
        // StackPane already centers the image — initial translate must be 0
        offsetX[0] = 0;
        offsetY[0] = 0;
        clampPan(offsetX, offsetY, displayW[0], displayH[0], displayScale[0], imgW, imgH);
        imageView.setTranslateX(offsetX[0]);
        imageView.setTranslateY(offsetY[0]);

        final double[] startDragX = { 0 };
        final double[] startDragY = { 0 };
        imageView.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                startDragX[0] = e.getSceneX() - offsetX[0];
                startDragY[0] = e.getSceneY() - offsetY[0];
            }
        });
        imageView.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                offsetX[0] = e.getSceneX() - startDragX[0];
                offsetY[0] = e.getSceneY() - startDragY[0];
                clampPan(offsetX, offsetY, displayW[0], displayH[0], displayScale[0], imgW, imgH);
                imageView.setTranslateX(offsetX[0]);
                imageView.setTranslateY(offsetY[0]);
            }
        });

        StackPane imagePane = new StackPane(imageView);
        imagePane.setAlignment(Pos.CENTER);
        imagePane.setMinSize(PREVIEW_SIZE, PREVIEW_SIZE);
        imagePane.setMaxSize(PREVIEW_SIZE, PREVIEW_SIZE);
        // Clip so the image never bleeds outside the preview square
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(PREVIEW_SIZE, PREVIEW_SIZE);
        imagePane.setClip(clip);

        // Circle centered via StackPane — use radius-only constructor so bounds = 2r×2r
        Circle overlay = new Circle(CROP_RADIUS);
        overlay.setFill(Color.TRANSPARENT);
        overlay.setStroke(Color.WHITE);
        overlay.setStrokeWidth(2);
        overlay.setMouseTransparent(true);

        // Dark vignette: a rectangle with the crop circle punched out
        javafx.scene.shape.Shape vignette = javafx.scene.shape.Shape.subtract(
            new javafx.scene.shape.Rectangle(PREVIEW_SIZE, PREVIEW_SIZE),
            new Circle(CROP_RADIUS));
        vignette.setFill(javafx.scene.paint.Color.color(0, 0, 0, 0.48));
        vignette.setMouseTransparent(true);

        StackPane preview = new StackPane(imagePane, vignette, overlay);
        preview.setMinSize(PREVIEW_SIZE, PREVIEW_SIZE);
        preview.setMaxSize(PREVIEW_SIZE, PREVIEW_SIZE);
        preview.setStyle("-fx-background-color: #1f2937; -fx-background-radius: 8;");

        // Zoom slider
        Slider zoomSlider = new Slider(ZOOM_MIN, ZOOM_MAX, ZOOM_DEFAULT);
        zoomSlider.setMajorTickUnit(0.5);
        zoomSlider.setMinorTickCount(1);
        zoomSlider.setShowTickLabels(false);
        zoomSlider.setPrefWidth(200);
        zoomSlider.getStyleClass().add("crop-zoom-slider");
        Label zoomLabel = new Label();
        zoomLabel.getStyleClass().add("text-muted");
        Runnable updateZoomLabel = () -> zoomLabel.setText(I18n.get("profile.photo.crop.zoom_percent", (int) (zoom[0] * 100)));
        zoomSlider.valueProperty().addListener((o, old, val) -> {
            zoom[0] = val.doubleValue();
            updateImageSize.run();
            updateZoomLabel.run();
        });
        updateZoomLabel.run();

        HBox zoomRow = new HBox(12);
        zoomRow.setAlignment(Pos.CENTER);
        zoomRow.getChildren().addAll(
            new Label(I18n.get("profile.photo.crop.zoom", "Zoom")),
            zoomSlider,
            zoomLabel
        );

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(preview, zoomRow);

        TLDialog<Boolean> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("profile.photo.crop.title", "Recadrer la photo"));
        dialog.setDescription(I18n.get("profile.photo.crop.description", "Faites glisser l'image pour positionner le cadre, réglez le zoom si besoin, puis validez."));
        dialog.setContent(content);
        dialog.addButton(javafx.scene.control.ButtonType.CANCEL);
        dialog.addButton(javafx.scene.control.ButtonType.OK);
        dialog.setResultConverter(bt -> javafx.scene.control.ButtonType.OK.equals(bt) ? Boolean.TRUE : null);
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        if (okBtn != null) okBtn.setText(I18n.get("profile.photo.crop.apply", "Utiliser cette photo"));

        Optional<Boolean> result = dialog.showAndWait();
        if (result.isEmpty() || !Boolean.TRUE.equals(result.get())) {
            return Optional.empty();
        }

        // Map circle center (at imagePane center) to source image pixels.
        // The StackPane lays the image out at (PREVIEW_SIZE - displayW)/2 from left.
        // Circle is at pane center (PREVIEW_SIZE/2). Relative to image left edge:
        //   circleInImageX = PREVIEW_SIZE/2 - (PREVIEW_SIZE - displayW)/2 - offsetX
        //                  = displayW/2 - offsetX
        double centerViewX = displayW[0] / 2.0 - offsetX[0];
        double centerViewY = displayH[0] / 2.0 - offsetY[0];
        double centerImgX = centerViewX / displayScale[0];
        double centerImgY = centerViewY / displayScale[0];
        int radiusPx = (int) Math.round(CROP_RADIUS / displayScale[0]);

        int cx = (int) Math.round(centerImgX);
        int cy = (int) Math.round(centerImgY);
        int r = Math.max(1, Math.min(radiusPx, Math.min(bufferedImage.getWidth(), bufferedImage.getHeight()) / 2));
        cx = Math.max(r, Math.min(bufferedImage.getWidth() - r - 1, cx));
        cy = Math.max(r, Math.min(bufferedImage.getHeight() - r - 1, cy));

        String dataUri = ImageUtils.cropToCircleToBase64(bufferedImage, cx, cy, r);
        return dataUri != null ? Optional.of(dataUri) : Optional.empty();
    }

    /**
     * Clamp pan so the circle center stays over the image (with at least radius margin).
     * View center in image view coords = (displayW/2 - offsetX, displayH/2 - offsetY).
     * We need 0 <= (displayW/2 - offsetX) / displayScale <= imgW and same for Y,
     * and circle radius in source = CROP_RADIUS/displayScale, so center must be in
     * [radius_source, imgW - radius_source] and [radius_source, imgH - radius_source].
     */
    private static void clampPan(double[] offsetX, double[] offsetY,
                                  double displayW, double displayH, double displayScale,
                                  double imgW, double imgH) {
        double centerViewX = displayW / 2 - offsetX[0];
        double centerViewY = displayH / 2 - offsetY[0];
        double radiusSrc = CROP_RADIUS / displayScale;
        double minCenterX = radiusSrc * displayScale;
        double maxCenterX = (imgW - radiusSrc) * displayScale;
        double minCenterY = radiusSrc * displayScale;
        double maxCenterY = (imgH - radiusSrc) * displayScale;
        centerViewX = Math.max(minCenterX, Math.min(maxCenterX, centerViewX));
        centerViewY = Math.max(minCenterY, Math.min(maxCenterY, centerViewY));
        offsetX[0] = displayW / 2 - centerViewX;
        offsetY[0] = displayH / 2 - centerViewY;
    }
}
