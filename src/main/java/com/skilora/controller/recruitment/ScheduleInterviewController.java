package com.skilora.controller.recruitment;

import com.skilora.framework.components.TLDatePicker;
import com.skilora.framework.components.TLSelect;
import com.skilora.framework.components.TLTextarea;
import com.skilora.framework.components.TLTextField;
import com.skilora.model.entity.recruitment.Application;
import com.skilora.model.entity.recruitment.Interview;
import com.skilora.model.entity.recruitment.Interview.InterviewType;
import com.skilora.service.recruitment.InterviewService;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * ScheduleInterviewController - Handles interview scheduling dialog
 */
public class ScheduleInterviewController {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleInterviewController.class);

    @FXML private Label candidateLabel;
    @FXML private Label jobLabel;
    @FXML private TLDatePicker datePicker;
    @FXML private TLTextField timeField;
    @FXML private TLTextField locationField;
    @FXML private TLSelect<String> typeSelect;
    @FXML private TLTextarea notesArea;

    private Application application;
    private Interview existingInterview;
    private Runnable onSuccess;
    private final InterviewService interviewService = InterviewService.getInstance();

    @FXML
    public void initialize() {
        // Setup interview type options
        if (typeSelect != null) {
            typeSelect.getItems().addAll(
                InterviewType.IN_PERSON.getDisplayName(),
                InterviewType.VIDEO.getDisplayName(),
                InterviewType.PHONE.getDisplayName(),
                InterviewType.ONLINE.getDisplayName()
            );
            typeSelect.setValue(InterviewType.IN_PERSON.getDisplayName());
        }
        
        // Setup time field placeholder
        if (timeField != null) {
            timeField.setPromptText("HH:mm (ex: 14:30)");
        }
    }

    public void setup(Application app, Interview existingInterview, Runnable onSuccess) {
        this.application = app;
        this.existingInterview = existingInterview;
        this.onSuccess = onSuccess;
        
        // Populate candidate and job info
        if (candidateLabel != null) {
            candidateLabel.setText(app.getCandidateName() != null ? app.getCandidateName() : 
                I18n.get("interviews.candidate_num", app.getCandidateProfileId()));
        }
        
        if (jobLabel != null) {
            jobLabel.setText(app.getJobTitle() != null ? app.getJobTitle() : 
                I18n.get("interviews.job_num", app.getJobOfferId()));
        }
        
        // If editing existing interview, populate fields
        if (existingInterview != null) {
            if (datePicker != null && existingInterview.getInterviewDate() != null) {
                datePicker.setValue(existingInterview.getInterviewDate().toLocalDate());
            }
            
            if (timeField != null && existingInterview.getInterviewDate() != null) {
                LocalTime time = existingInterview.getInterviewDate().toLocalTime();
                timeField.setText(String.format("%02d:%02d", time.getHour(), time.getMinute()));
            }
            
            if (locationField != null) {
                locationField.setText(existingInterview.getLocation());
            }
            
            if (typeSelect != null && existingInterview.getInterviewType() != null) {
                typeSelect.setValue(existingInterview.getInterviewType().getDisplayName());
            }
            
            if (notesArea != null) {
                notesArea.setText(existingInterview.getNotes());
            }
        } else {
            // Default to tomorrow at 10:00
            if (datePicker != null) {
                datePicker.setValue(LocalDate.now().plusDays(1));
            }
            if (timeField != null) {
                timeField.setText("10:00");
            }
        }
    }

    public boolean validateAndSave() {
        // Validate fields
        if (datePicker == null || datePicker.getValue() == null) {
            DialogUtils.showError("Erreur", I18n.get("interviews.error.date_required"));
            return false;
        }
        
        if (timeField == null || timeField.getText() == null || timeField.getText().trim().isEmpty()) {
            DialogUtils.showError("Erreur", I18n.get("interviews.error.time_required"));
            return false;
        }
        
        // Parse time
        LocalTime time;
        try {
            String[] timeParts = timeField.getText().trim().split(":");
            if (timeParts.length != 2) {
                throw new IllegalArgumentException("Invalid time format");
            }
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                throw new IllegalArgumentException("Invalid time values");
            }
            time = LocalTime.of(hour, minute);
        } catch (Exception e) {
            DialogUtils.showError("Erreur", I18n.get("interviews.error.invalid_time"));
            return false;
        }
        
        // Combine date and time
        LocalDateTime interviewDateTime = LocalDateTime.of(datePicker.getValue(), time);
        
        // Check if date is in the past
        if (interviewDateTime.isBefore(LocalDateTime.now())) {
            DialogUtils.showError("Erreur", I18n.get("interviews.error.past_date"));
            return false;
        }
        
        // Get interview type
        InterviewType interviewType = InterviewType.IN_PERSON;
        if (typeSelect != null && typeSelect.getValue() != null) {
            String typeValue = typeSelect.getValue();
            for (InterviewType type : InterviewType.values()) {
                if (type.getDisplayName().equals(typeValue)) {
                    interviewType = type;
                    break;
                }
            }
        }
        
        String location = locationField != null ? locationField.getText() : null;
        String notes = notesArea != null ? notesArea.getText() : null;
        
        // Save interview
        try {
            if (existingInterview != null) {
                // Update existing interview
                boolean success = interviewService.updateInterview(
                    existingInterview.getId(),
                    interviewDateTime,
                    location,
                    interviewType,
                    notes,
                    Interview.InterviewStatus.SCHEDULED
                );
                
                if (success) {
                    DialogUtils.showInfo("Succès", I18n.get("interviews.updated"));
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                    return true;
                } else {
                    DialogUtils.showError("Erreur", I18n.get("interviews.error.update_failed"));
                    return false;
                }
            } else {
                // Create new interview
                int interviewId = interviewService.scheduleInterview(
                    application.getId(),
                    interviewDateTime,
                    location,
                    interviewType,
                    notes
                );
                
                if (interviewId > 0) {
                    DialogUtils.showInfo("Succès", I18n.get("interviews.scheduled_success"));
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                    return true;
                } else {
                    DialogUtils.showError("Erreur", I18n.get("interviews.error.schedule_failed"));
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("Error saving interview", e);
            DialogUtils.showError("Erreur", 
                I18n.get("interviews.error.save_failed") + ": " + e.getMessage());
            return false;
        }
    }
}

