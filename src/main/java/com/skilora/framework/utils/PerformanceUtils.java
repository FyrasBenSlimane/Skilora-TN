package com.skilora.framework.utils;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PerformanceUtils - Utility methods for performance optimization.
 * 
 * Provides debounce, throttle, and other performance-related utilities.
 */
public final class PerformanceUtils {

    private PerformanceUtils() {
    } // Utility class

    /**
     * Create a debounced version of a Runnable.
     * The action will only execute after the specified delay has passed
     * without any new calls.
     * 
     * Useful for: search fields, resize handlers, scroll handlers
     * 
     * @param delayMs Delay in milliseconds
     * @param action  Action to debounce
     * @return A Runnable that debounces the action
     */
    public static Runnable debounce(int delayMs, Runnable action) {
        PauseTransition pause = new PauseTransition(Duration.millis(delayMs));
        pause.setOnFinished(e -> action.run());

        return () -> {
            pause.playFromStart();
        };
    }

    /**
     * Create a throttled version of a Runnable.
     * The action will execute at most once per specified interval.
     * 
     * Useful for: scroll handlers, mouse move handlers
     * 
     * @param intervalMs Minimum interval between executions in milliseconds
     * @param action     Action to throttle
     * @return A Runnable that throttles the action
     */
    public static Runnable throttle(int intervalMs, Runnable action) {
        AtomicLong lastExecutionTime = new AtomicLong(0);
        AtomicBoolean scheduled = new AtomicBoolean(false);

        return () -> {
            long now = System.currentTimeMillis();
            long lastExec = lastExecutionTime.get();

            if (now - lastExec >= intervalMs) {
                // Execute immediately
                lastExecutionTime.set(now);
                action.run();
            } else if (!scheduled.getAndSet(true)) {
                // Schedule for later
                long delay = intervalMs - (now - lastExec);
                PauseTransition pause = new PauseTransition(Duration.millis(delay));
                pause.setOnFinished(e -> {
                    lastExecutionTime.set(System.currentTimeMillis());
                    scheduled.set(false);
                    action.run();
                });
                pause.play();
            }
        };
    }

    /**
     * Run an action on the JavaFX Application Thread if not already on it.
     * More efficient than always using Platform.runLater().
     */
    public static void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    /**
     * Schedule an action to run after a delay on the FX thread.
     * Useful for delayed UI updates.
     */
    public static void runLater(int delayMs, Runnable action) {
        PauseTransition pause = new PauseTransition(Duration.millis(delayMs));
        pause.setOnFinished(e -> action.run());
        pause.play();
    }

    /**
     * Execute an action only if another action hasn't been scheduled
     * within the coalescence window. Useful for batching rapid updates.
     * 
     * @param windowMs Coalescence window in milliseconds
     * @param action   Action to execute
     * @return A Runnable that coalesces calls
     */
    public static Runnable coalesce(int windowMs, Runnable action) {
        AtomicLong scheduledTime = new AtomicLong(0);

        return () -> {
            long now = System.currentTimeMillis();
            scheduledTime.set(now);

            PauseTransition pause = new PauseTransition(Duration.millis(windowMs));
            pause.setOnFinished(e -> {
                // Only execute if this is the most recent call
                if (scheduledTime.get() == now) {
                    action.run();
                }
            });
            pause.play();
        };
    }
}
