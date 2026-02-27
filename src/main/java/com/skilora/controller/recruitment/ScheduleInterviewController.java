package com.skilora.controller.recruitment;

import com.skilora.model.entity.recruitment.Application;
import com.skilora.model.entity.recruitment.Interview;
import com.skilora.model.entity.recruitment.Interview.InterviewStatus;
import com.skilora.model.entity.recruitment.Interview.InterviewType;
import com.skilora.model.entity.usermanagement.Profile;
import com.skilora.service.recruitment.InterviewService;
import com.skilora.service.usermanagement.ProfileService;
import com.skilora.service.notification.WhatsAppNotificationService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Controller for the schedule/edit interview dialog.
 */
public class ScheduleInterviewController {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleInterviewController.class);

    @FXML private VBox root;
    @FXML private DatePicker datePicker;
    @FXML private TextField timeField;
    @FXML private TextField locationField;
    @FXML private ComboBox<InterviewType> typeCombo;
    @FXML private TextArea notesArea;

    private Application application;
    private Interview existingInterview;
    private Runnable onSuccess;

    public void setup(Application app, Interview existing, Runnable onSuccess) {
        this.application = app;
        this.existingInterview = existing;
        this.onSuccess = onSuccess;
        if (typeCombo != null) {
            typeCombo.getItems().setAll(InterviewType.values());
            typeCombo.setValue(InterviewType.IN_PERSON);
        }
        if (existing != null) {
            if (datePicker != null && existing.getInterviewDate() != null) {
                datePicker.setValue(existing.getInterviewDate().toLocalDate());
            }
            if (timeField != null && existing.getInterviewDate() != null) {
                timeField.setText(existing.getInterviewDate().toLocalTime().toString().substring(0, 5));
            }
            if (locationField != null) locationField.setText(existing.getLocation());
            if (typeCombo != null && existing.getInterviewType() != null) typeCombo.setValue(existing.getInterviewType());
            if (notesArea != null) notesArea.setText(existing.getNotes());
        } else if (datePicker != null) {
            datePicker.setValue(LocalDate.now());
        }
    }

    /**
     * Looks up the candidate's phone number from their profile and sends a
     * WhatsApp notification. Runs on a background thread; never blocks the UI.
     */
    private void sendWhatsAppNotification(Interview interview, Application application) {
        Thread notifier = new Thread(() -> {
            try {
                Profile profile = ProfileService.getInstance()
                        .findProfileById(application.getCandidateProfileId());
                String phone = (profile != null) ? profile.getPhone() : null;

                WhatsAppNotificationService.getInstance()
                        .sendInterviewScheduledNotification(interview, application, phone);
            } catch (Exception e) {
                logger.warn("Could not send WhatsApp notification: {}", e.getMessage());
            }
        }, "WhatsApp-Notification-Trigger");
        notifier.setDaemon(true);
        notifier.start();
    }

    /** Validates inputs and saves the interview. Returns true if saved successfully. */
    public boolean validateAndSave() {
        if (application == null) return false;
        LocalDate date = datePicker != null ? datePicker.getValue() : LocalDate.now();
        if (date == null) return false;
        LocalTime time = LocalTime.of(14, 0);
        if (timeField != null && timeField.getText() != null && !timeField.getText().trim().isEmpty()) {
            try {
                String[] parts = timeField.getText().trim().split(":");
                time = LocalTime.of(Integer.parseInt(parts[0]), parts.length > 1 ? Integer.parseInt(parts[1]) : 0);
            } catch (Exception ignored) {}
        }
        LocalDateTime dateTime = LocalDateTime.of(date, time);
        String location = locationField != null ? locationField.getText() : null;
        InterviewType type = typeCombo != null && typeCombo.getValue() != null ? typeCombo.getValue() : InterviewType.IN_PERSON;
        String notes = notesArea != null ? notesArea.getText() : null;

        try {
            Interview toSave = existingInterview != null ? existingInterview : new Interview();
            toSave.setApplicationId(application.getId());
            toSave.setInterviewDate(dateTime);
            toSave.setLocation(location);
            toSave.setInterviewType(type);
            toSave.setNotes(notes);
            toSave.setStatus(InterviewStatus.SCHEDULED);

            // Populate transient fields used in the WhatsApp message
            toSave.setCandidateName(application.getCandidateName());
            toSave.setJobTitle(application.getJobTitle());
            toSave.setCompanyName(application.getCompanyName());

            InterviewService.getInstance().saveOrUpdate(toSave);

            // Send WhatsApp notification to the candidate (async, non-blocking)
            sendWhatsAppNotification(toSave, application);

            if (onSuccess != null) onSuccess.run();
            return true;
        } catch (Exception e) {
            logger.error("Save interview failed", e);
            return false;
        }
    }
}
