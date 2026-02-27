package com.skilora.framework.components;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * InterviewCountdownWidget
 *
 * A self-contained, auto-updating countdown badge that shows how much time
 * remains before a scheduled interview.
 *
 * Features
 * ────────
 *  • Updates every 30 seconds automatically (switches to every second when
 *    fewer than 2 minutes remain).
 *  • Colour-coded by urgency:
 *      navy   → more than 2 days
 *      blue   → 6 h – 2 days
 *      amber  → 1 h – 6 h
 *      orange → < 1 h
 *      red    → < 5 min / imminent
 *      green  → happening right now
 *      gray   → already passed
 *  • Automatically starts its internal Timeline when attached to a Scene
 *    and stops it when detached — no memory leaks.
 *
 * Usage
 * ─────
 *   InterviewCountdownWidget widget = new InterviewCountdownWidget(interview.getInterviewDate());
 *   someContainer.getChildren().add(widget);
 */
public class InterviewCountdownWidget extends HBox {

    private static final int TICK_LONG_SEC  = 30;   // normal update interval
    private static final int TICK_SHORT_SEC = 1;    // sub-2-minute update interval

    private final Label     countdownLabel = new Label();
    private final Timeline  timeline;
    private final LocalDateTime interviewDate;

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructor
    // ─────────────────────────────────────────────────────────────────────────

    public InterviewCountdownWidget(LocalDateTime interviewDate) {
        this.interviewDate = interviewDate;

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(0);

        countdownLabel.getStyleClass().add("interview-countdown-badge");
        countdownLabel.setStyle(badgeBaseStyle());

        getChildren().add(countdownLabel);

        // Initial render (synchronous, already on FX thread at construction time)
        renderCountdown();

        // Build timeline; interval is adjusted dynamically in renderCountdown()
        timeline = buildTimeline(TICK_LONG_SEC);

        // Start when added to a Scene, stop when removed
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                timeline.play();
            } else {
                timeline.stop();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Core rendering
    // ─────────────────────────────────────────────────────────────────────────

    private void renderCountdown() {
        long totalSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), interviewDate);

        String text  = buildText(totalSeconds);
        String color = pickColor(totalSeconds);
        String style = badgeBaseStyle()
                + "-fx-background-color:" + color + "; "
                + "-fx-text-fill:" + pickTextColor(totalSeconds) + ";";

        if (Platform.isFxApplicationThread()) {
            applyToLabel(text, style);
        } else {
            Platform.runLater(() -> applyToLabel(text, style));
        }

        // Switch to faster ticking when fewer than 2 minutes remain
        boolean needsFast = totalSeconds > 0 && totalSeconds <= 120;
        int targetInterval = needsFast ? TICK_SHORT_SEC : TICK_LONG_SEC;
        if (timeline != null) {
            KeyFrame current = timeline.getKeyFrames().isEmpty()
                    ? null : timeline.getKeyFrames().get(0);
            if (current == null || (long) current.getTime().toSeconds() != targetInterval) {
                boolean wasRunning = timeline.getStatus() == Animation.Status.RUNNING;
                timeline.stop();
                timeline.getKeyFrames().setAll(
                        new KeyFrame(Duration.seconds(targetInterval), e -> renderCountdown()));
                if (wasRunning) timeline.play();
            }
        }
    }

    private void applyToLabel(String text, String style) {
        countdownLabel.setText(text);
        countdownLabel.setStyle(style);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Text & colour helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Formats the countdown text based on remaining seconds.
     */
    static String buildText(long totalSeconds) {
        if (totalSeconds < -3600) {
            return "\u231B Entretien pass\u00e9";          // ⌛ Entretien passé
        }
        if (totalSeconds < -60) {
            long ago = (-totalSeconds) / 60;
            return "\u231B Il y a " + ago + " min";
        }
        if (totalSeconds < 0) {
            return "\uD83D\uDD34 En cours maintenant";     // 🔴 En cours maintenant
        }
        if (totalSeconds < 60) {
            return "\u26A1 Imminent\u00a0!";               // ⚡ Imminent !
        }
        if (totalSeconds < 300) {
            long mins = totalSeconds / 60;
            long secs = totalSeconds % 60;
            return String.format("\u26A1 Dans %d min %d sec", mins, secs);
        }
        if (totalSeconds < 3600) {
            long mins = totalSeconds / 60;
            return String.format("\u23F1 Dans %d min", mins);  // ⏱
        }
        if (totalSeconds < 86400) {
            long hours = totalSeconds / 3600;
            long mins  = (totalSeconds % 3600) / 60;
            if (mins == 0) return String.format("\u23F1 Dans %d h", hours);
            return String.format("\u23F1 Dans %d h %d min", hours, mins);
        }
        // >= 1 day
        long days  = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        if (hours == 0) return String.format("\uD83D\uDCC5 Dans %d j", days);
        return String.format("\uD83D\uDCC5 Dans %d j %d h", days, hours);
    }

    /** Background colour as CSS hex string. */
    private static String pickColor(long totalSeconds) {
        if (totalSeconds < -3600) return "#374151";   // gray  – passed
        if (totalSeconds < 0)     return "#166534";   // dark green – happening
        if (totalSeconds < 300)   return "#dc2626";   // red   – imminent
        if (totalSeconds < 3600)  return "#ea580c";   // orange < 1 h
        if (totalSeconds < 21600) return "#d97706";   // amber 1–6 h
        if (totalSeconds < 172800)return "#2563eb";   // blue  6 h – 2 days
        return "#1e40af";                             // navy  > 2 days
    }

    /** Text colour (white or soft gray). */
    private static String pickTextColor(long totalSeconds) {
        return totalSeconds < -3600 ? "#9ca3af" : "white";
    }

    private static String badgeBaseStyle() {
        return "-fx-font-weight:bold; -fx-padding:4 12; "
                + "-fx-background-radius:20; -fx-font-size:12;";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Timeline factory
    // ─────────────────────────────────────────────────────────────────────────

    private Timeline buildTimeline(int intervalSeconds) {
        Timeline tl = new Timeline(
                new KeyFrame(Duration.seconds(intervalSeconds), e -> renderCountdown()));
        tl.setCycleCount(Animation.INDEFINITE);
        return tl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────────────

    /** Manually stops the internal timeline (e.g., when the parent window closes). */
    public void stop() {
        if (timeline != null) timeline.stop();
    }

    /**
     * Convenience factory that returns an {@code InterviewCountdownWidget} or
     * {@code null} when the interview date is null.
     */
    public static InterviewCountdownWidget of(LocalDateTime interviewDate) {
        if (interviewDate == null) return null;
        return new InterviewCountdownWidget(interviewDate);
    }
}
