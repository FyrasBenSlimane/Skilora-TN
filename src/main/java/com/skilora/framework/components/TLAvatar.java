package com.skilora.framework.components;

import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

/**
 * TLAvatar - shadcn/ui Avatar (image or fallback initials).
 * Sizes: sm (40), default (48?), lg (96), xl (128).
 */
public class TLAvatar extends StackPane {

    public enum Size {
        SM, DEFAULT, LG, XL
    }

    private final Label fallbackLabel;
    private final ImageView imageView;
    private final Circle clip;

    public TLAvatar() {
        this(null, null);
    }

    public TLAvatar(String initials) {
        this(null, initials);
    }

    public TLAvatar(Image image, String fallbackInitials) {
        getStyleClass().add("avatar");
        getStyleClass().add("avatar-sm");
        setPrefSize(40, 40);
        setMinSize(40, 40);
        setMaxSize(40, 40);

        clip = new Circle();
        clip.centerXProperty().bind(widthProperty().divide(2));
        clip.centerYProperty().bind(heightProperty().divide(2));
        clip.radiusProperty().bind(Bindings.min(widthProperty(), heightProperty()).divide(2));
        setClip(clip);

        fallbackLabel = new Label(fallbackInitials != null ? fallbackInitials : "?");
        fallbackLabel.getStyleClass().add("avatar-fallback");

        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        // PERFORMANCE: Disable smooth scaling for small avatars - much faster rendering
        imageView.setSmooth(false);
        imageView.fitWidthProperty().bind(widthProperty());
        imageView.fitHeightProperty().bind(heightProperty());
        imageView.setVisible(false);

        // PERFORMANCE: Enable caching for GPU-accelerated rendering
        setCache(true);
        setCacheHint(javafx.scene.CacheHint.SPEED);

        if (image != null) {
            imageView.setImage(image);
            imageView.setVisible(true);
            getChildren().add(imageView);
        } else {
            getChildren().add(fallbackLabel);
        }
    }

    public void setImage(Image image) {
        if (image != null) {
            imageView.setImage(image);
            imageView.setVisible(true);
            if (!getChildren().contains(imageView))
                getChildren().add(0, imageView);
            fallbackLabel.setVisible(false);
        } else {
            imageView.setVisible(false);
            fallbackLabel.setVisible(true);
        }
    }

    public void setFallback(String initials) {
        fallbackLabel.setText(initials != null && !initials.isEmpty() ? initials : "?");
    }

    public void setSize(Size size) {
        getStyleClass().removeAll("avatar-sm", "avatar-lg", "avatar-xl");
        getStyleClass().add(switch (size) {
            case SM -> "avatar-sm";
            case LG -> "avatar-lg";
            case XL -> "avatar-xl";
            default -> "avatar-sm";
        });
    }
}
