package com.skilora.framework.components;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Small widget that shows a countdown or relative time until an interview (e.g. "In 2d 5h" or "Today at 14:00").
 * Used in ApplicationsController, ApplicationDetailsController, EmployerDashboardController, InterviewsController.
 */
public class InterviewCountdownWidget extends HBox {

    private final LocalDateTime scheduledDate;
    private final Label label;

    public InterviewCountdownWidget(LocalDateTime scheduledDate) {
        this.scheduledDate = scheduledDate;
        getStyleClass().add("interview-countdown-widget");
        label = new Label();
        label.getStyleClass().add("text-muted");
        label.setStyle("-fx-font-size: 12px;");
        getChildren().add(label);
        updateText();
        // Optional: refresh every minute so "In 5 min" updates
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(60), e -> updateText())
        );
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();
    }

    /**
     * Factory: returns null if date is null, otherwise a new widget.
     */
    public static InterviewCountdownWidget of(LocalDateTime scheduledDate) {
        if (scheduledDate == null) return null;
        return new InterviewCountdownWidget(scheduledDate);
    }

    private void updateText() {
        if (scheduledDate == null) return;
        LocalDateTime now = LocalDateTime.now();
        if (scheduledDate.isBefore(now)) {
            label.setText("⏱ " + com.skilora.utils.I18n.get("interview.countdown.past"));
            return;
        }
        long days = ChronoUnit.DAYS.between(now.toLocalDate(), scheduledDate.toLocalDate());
        long hours = ChronoUnit.HOURS.between(now, scheduledDate) % 24;
        long minutes = ChronoUnit.MINUTES.between(now, scheduledDate) % 60;
        String text;
        if (days > 0) {
            text = "⏱ " + com.skilora.utils.I18n.get("interview.countdown.days", days, hours);
        } else if (hours > 0) {
            text = "⏱ " + com.skilora.utils.I18n.get("interview.countdown.hours", hours, minutes);
        } else if (minutes > 0) {
            text = "⏱ " + com.skilora.utils.I18n.get("interview.countdown.minutes", minutes);
        } else {
            text = "⏱ " + com.skilora.utils.I18n.get("interview.countdown.soon");
        }
        label.setText(text);
    }
}
