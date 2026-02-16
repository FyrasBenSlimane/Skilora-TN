package com.skilora.framework.components;

import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Cursor;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * TLSwitch - shadcn/ui Switch toggle for JavaFX.
 *
 * Custom toggle switch with track + thumb animation, matching shadcn New York v4.
 * Uses CSS classes: .switch, .switch-track, .switch-track-on, .switch-track-off, .switch-thumb
 *
 * Usage:
 *   TLSwitch sw = new TLSwitch();
 *   TLSwitch sw = new TLSwitch(true);  // initially on
 *   sw.selectedProperty().addListener(...);
 */
public class TLSwitch extends StackPane {

    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final Rectangle track;
    private final Circle thumb;
    private static final double TRACK_WIDTH = 44;
    private static final double TRACK_HEIGHT = 24;
    private static final double THUMB_RADIUS = 9;

    public TLSwitch() {
        this(false);
    }

    public TLSwitch(boolean initialState) {
        getStyleClass().add("switch");
        setCursor(Cursor.HAND);

        // Track
        track = new Rectangle(TRACK_WIDTH, TRACK_HEIGHT);
        track.setArcWidth(TRACK_HEIGHT);
        track.setArcHeight(TRACK_HEIGHT);
        track.getStyleClass().add("switch-track");
        track.getStyleClass().add(initialState ? "switch-track-on" : "switch-track-off");

        // Thumb
        thumb = new Circle(THUMB_RADIUS);
        thumb.getStyleClass().add("switch-thumb");

        setMinSize(TRACK_WIDTH, TRACK_HEIGHT);
        setPrefSize(TRACK_WIDTH, TRACK_HEIGHT);
        setMaxSize(TRACK_WIDTH, TRACK_HEIGHT);

        getChildren().addAll(track, thumb);

        // Position thumb
        double thumbTravel = (TRACK_WIDTH / 2) - THUMB_RADIUS - 3;
        thumb.setTranslateX(initialState ? thumbTravel : -thumbTravel);

        selected.set(initialState);

        // Click handler
        setOnMouseClicked(e -> setSelected(!isSelected()));

        // Animate thumb on state change
        selected.addListener((obs, wasSelected, isSelected) -> {
            double targetX = isSelected ? thumbTravel : -thumbTravel;

            TranslateTransition tt = new TranslateTransition(Duration.millis(150), thumb);
            tt.setToX(targetX);
            tt.play();

            // Update track styling
            track.getStyleClass().remove(isSelected ? "switch-track-off" : "switch-track-on");
            if (!track.getStyleClass().contains(isSelected ? "switch-track-on" : "switch-track-off")) {
                track.getStyleClass().add(isSelected ? "switch-track-on" : "switch-track-off");
            }
        });
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean value) {
        selected.set(value);
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public void setSwitchDisabled(boolean disabled) {
        setDisable(disabled);
        if (disabled) {
            if (!getStyleClass().contains("switch-disabled")) {
                getStyleClass().add("switch-disabled");
            }
            setCursor(Cursor.DEFAULT);
        } else {
            getStyleClass().remove("switch-disabled");
            setCursor(Cursor.HAND);
        }
    }
}
