package com.skilora.recruitment.controller;

import com.skilora.recruitment.entity.Application;
import com.skilora.recruitment.entity.Interview;
import com.skilora.recruitment.service.ApplicationService;
import com.skilora.recruitment.service.InterviewService;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;

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
 * Controller for the schedule / edit interview dialog.
 * Adapted from branch: uses String-based type and status (local Interview entity model).
 */
public class ScheduleInterviewController {
    private static final Logger logger = LoggerFactory.getLogger(ScheduleInterviewController.class);

    /** Interview type options (displayed in the combo box) */
    private static final String TYPE_IN_PERSON = "IN_PERSON";
    private static final String TYPE_VIDEO     = "VIDEO";
    private static final String TYPE_PHONE     = "PHONE";

    @FXML private VBox root;
    @FXML private DatePicker datePicker;
    @FXML private TextField timeField;
    @FXML private TextField locationField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextArea notesArea;

    private Application application;
    private Interview existingInterview;
    private Runnable onSuccess;

    public void setup(Application app, Interview existing, Runnable onSuccess) {
        this.application = app;
        this.existingInterview = existing;
        this.onSuccess = onSuccess;

        if (typeCombo != null) {
            typeCombo.getItems().setAll(TYPE_IN_PERSON, TYPE_VIDEO, TYPE_PHONE);
            typeCombo.setValue(TYPE_IN_PERSON);
        }

        if (existing != null) {
            if (datePicker != null && existing.getScheduledDate() != null)
                datePicker.setValue(existing.getScheduledDate().toLocalDate());
            if (timeField != null && existing.getScheduledDate() != null)
                timeField.setText(existing.getScheduledDate().toLocalTime().toString().substring(0, 5));
            if (locationField != null) locationField.setText(existing.getLocation());
            if (typeCombo != null && existing.getType() != null) typeCombo.setValue(existing.getType());
            if (notesArea != null) notesArea.setText(existing.getNotes());
        } else if (datePicker != null) {
            datePicker.setValue(LocalDate.now());
        }
    }

    /**
     * Validates inputs and saves the interview.
     *
     * @return true if successfully saved
     */
    public boolean validateAndSave() {
        if (application == null) return false;

        LocalDate date = datePicker != null ? datePicker.getValue() : LocalDate.now();
        if (date == null) return false;

        LocalTime time = LocalTime.of(14, 0);
        if (timeField != null && timeField.getText() != null && !timeField.getText().trim().isEmpty()) {
            try {
                String[] parts = timeField.getText().trim().split(":");
                time = LocalTime.of(Integer.parseInt(parts[0]),
                        parts.length > 1 ? Integer.parseInt(parts[1]) : 0);
            } catch (Exception ignored) { /* keep default */ }
        }

        LocalDateTime dateTime = LocalDateTime.of(date, time);
        String location = locationField != null ? locationField.getText() : null;
        String type = typeCombo != null && typeCombo.getValue() != null ? typeCombo.getValue() : TYPE_IN_PERSON;
        String notes = notesArea != null ? notesArea.getText() : null;

        try {
            Interview toSave = existingInterview != null ? existingInterview : new Interview();
            toSave.setApplicationId(application.getId());
            toSave.setScheduledDate(dateTime);
            toSave.setLocation(location);
            toSave.setType(type);
            toSave.setNotes(notes);
            toSave.setStatus("SCHEDULED");

            // Populate transient fields
            toSave.setCandidateName(application.getCandidateName());
            toSave.setJobTitle(application.getJobTitle());
            toSave.setCompanyName(application.getCompanyName());

            InterviewService service = InterviewService.getInstance();
            if (existingInterview != null && existingInterview.getId() > 0) {
                boolean ok = service.update(toSave);
                if (!ok) {
                    DialogUtils.showError(I18n.get("common.error", "Erreur"),
                            I18n.get("interviews.save_failed", "Impossible d'enregistrer les modifications de l'entretien."));
                    return false;
                }
            } else {
                int id = service.create(toSave);
                if (id <= 0) {
                    DialogUtils.showError(I18n.get("common.error", "Erreur"),
                            I18n.get("interviews.save_failed", "Impossible d'enregistrer l'entretien. Vérifiez la connexion et réessayez."));
                    return false;
                }
                // Move application to "interview" phase
                try {
                    ApplicationService.getInstance().updateStatus(application.getId(), Application.Status.INTERVIEW);
                } catch (Exception ex) {
                    logger.warn("Could not update application status to INTERVIEW: {}", ex.getMessage());
                }
            }

            if (onSuccess != null) onSuccess.run();
            return true;
        } catch (Exception e) {
            logger.error("Save interview failed", e);
            DialogUtils.showError(I18n.get("common.error", "Erreur"),
                    I18n.get("interviews.save_failed", "Impossible d'enregistrer l'entretien.") + " " + e.getMessage());
            return false;
        }
    }
}
