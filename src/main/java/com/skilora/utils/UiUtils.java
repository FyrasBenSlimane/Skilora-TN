package com.skilora.utils;

import javafx.animation.PauseTransition;
import javafx.scene.control.TextField;
import javafx.util.Duration;

/**
 * UiUtils — Shared UI helper methods for JavaFX controllers.
 * Eliminates repeated patterns like search debouncing.
 */
public final class UiUtils {

    private UiUtils() {}

    /**
     * Attaches a debounced key-release listener to a TextField.
     * The action fires only after the user stops typing for the given delay.
     *
     * @param field   the TextField to watch
     * @param delayMs debounce delay in milliseconds
     * @param action  the Runnable to execute after debounce
     * @return the PauseTransition (can be stored if manual control is needed)
     */
    public static PauseTransition debounce(TextField field, long delayMs, Runnable action) {
        PauseTransition pause = new PauseTransition(Duration.millis(delayMs));
        pause.setOnFinished(e -> action.run());
        field.setOnKeyReleased(e -> pause.playFromStart());
        return pause;
    }
}
