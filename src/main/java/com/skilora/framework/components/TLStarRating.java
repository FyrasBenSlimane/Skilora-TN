package com.skilora.framework.components;

import com.skilora.utils.SvgIcons;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;

/**
 * Star rating component (S-05).
 * Displays 1-5 clickable stars. Fires a rating change event.
 *
 * Usage:
 *   TLStarRating rating = new TLStarRating();
 *   rating.setRating(3);
 *   rating.ratingProperty().addListener((obs, old, val) -> System.out.println("Rated: " + val));
 */
public class TLStarRating extends HBox {

    private static final String STYLESHEET =
            TLStarRating.class.getResource("/com/skilora/framework/styles/tl-star-rating.css").toExternalForm();

    private final IntegerProperty rating = new SimpleIntegerProperty(0);
    private final SVGPath[] stars = new SVGPath[5];
    private boolean readOnly = false;

    public TLStarRating() {
        this(0);
    }

    public TLStarRating(int initialRating) {
        getStylesheets().add(STYLESHEET);
        getStyleClass().add("star-rating");
        setSpacing(4);

        for (int i = 0; i < 5; i++) {
            final int starIndex = i + 1;
            SVGPath star = new SVGPath();
            star.setContent(SvgIcons.STAR);
            star.setScaleX(16.0 / 24.0);
            star.setScaleY(16.0 / 24.0);
            star.getStyleClass().addAll("star", "star-empty");
            star.setOnMouseClicked(e -> {
                if (!readOnly) setRating(starIndex);
            });
            star.setOnMouseEntered(e -> {
                if (!readOnly) highlightStars(starIndex);
            });
            star.setOnMouseExited(e -> {
                if (!readOnly) highlightStars(rating.get());
            });
            stars[i] = star;
            getChildren().add(star);
        }

        rating.addListener((obs, old, val) -> highlightStars(val.intValue()));
        setRating(initialRating);
    }

    private void highlightStars(int count) {
        for (int i = 0; i < 5; i++) {
            stars[i].getStyleClass().removeAll("star-filled", "star-empty");
            if (i < count) {
                stars[i].getStyleClass().add("star-filled");
            } else {
                stars[i].getStyleClass().add("star-empty");
            }
        }
    }

    public int getRating() { return rating.get(); }
    public void setRating(int value) { rating.set(Math.max(0, Math.min(5, value))); }
    public IntegerProperty ratingProperty() { return rating; }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        for (SVGPath star : stars) {
            star.setStyle(readOnly ? "-fx-cursor: default;" : "-fx-cursor: hand;");
        }
    }
}
