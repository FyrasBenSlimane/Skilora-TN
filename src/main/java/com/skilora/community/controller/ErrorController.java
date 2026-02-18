package com.skilora.community.controller;

import com.skilora.framework.components.TLButton;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * ErrorController - Display friendly error messages and recovery options
 */
public class ErrorController {

    @FXML private Label errorIcon;
    @FXML private Label errorCode;
    @FXML private Label errorTitle;
    @FXML private Label errorMessage;
    @FXML private TLButton homeBtn;
    @FXML private TLButton backBtn;
    @FXML private TLButton supportBtn;
    @FXML private VBox detailsBox;
    @FXML private Label technicalDetails;
    
    private Runnable onGoHome;
    private Runnable onGoBack;
    private Runnable onSupport;
    
    public void setCallbacks(Runnable onGoHome, Runnable onGoBack, Runnable onSupport) {
        this.onGoHome = onGoHome;
        this.onGoBack = onGoBack;
        this.onSupport = onSupport;
    }
    
    public void setError(ErrorType type, String message, String details) {
        switch (type) {
            case NOT_FOUND -> {
                errorIcon.setGraphic(SvgIcons.icon(SvgIcons.SEARCH, 64, "-fx-muted-foreground"));
                errorCode.setText("404");
                errorTitle.setText(I18n.get("errorpage.not_found"));
                errorMessage.setText(message != null ? message : 
                    I18n.get("errorpage.not_found.desc"));
            }
            case SERVER_ERROR -> {
                errorIcon.setGraphic(SvgIcons.icon(SvgIcons.ALERT_TRIANGLE, 64, "-fx-destructive"));
                errorCode.setText("500");
                errorTitle.setText(I18n.get("errorpage.server_error"));
                errorMessage.setText(message != null ? message : 
                    I18n.get("errorpage.server_error.desc"));
            }
            case NETWORK_ERROR -> {
                errorIcon.setGraphic(SvgIcons.icon(SvgIcons.WIFI, 64, "-fx-muted-foreground"));
                errorCode.setText("---");
                errorTitle.setText(I18n.get("errorpage.connection_error"));
                errorMessage.setText(message != null ? message : 
                    I18n.get("errorpage.connection_error.desc"));
            }
            case UNAUTHORIZED -> {
                errorIcon.setGraphic(SvgIcons.icon(SvgIcons.LOCK, 64, "-fx-muted-foreground"));
                errorCode.setText("403");
                errorTitle.setText(I18n.get("errorpage.access_denied"));
                errorMessage.setText(message != null ? message : 
                    I18n.get("errorpage.access_denied.desc"));
            }
            case GENERIC -> {
                errorIcon.setGraphic(SvgIcons.icon(SvgIcons.ALERT_TRIANGLE, 64, "-fx-amber"));
                errorCode.setText("!");
                errorTitle.setText(I18n.get("errorpage.generic"));
                errorMessage.setText(message != null ? message : 
                    I18n.get("errorpage.generic.desc"));
            }
        }
        homeBtn.setGraphic(SvgIcons.icon(SvgIcons.ARROW_LEFT, 14));
        
        if (details != null && !details.isEmpty()) {
            technicalDetails.setText(details);
            detailsBox.setVisible(true);
            detailsBox.setManaged(true);
        }
    }
    
    @FXML
    private void handleGoHome() {
        if (onGoHome != null) {
            onGoHome.run();
        }
    }
    
    @FXML
    private void handleGoBack() {
        if (onGoBack != null) {
            onGoBack.run();
        }
    }
    
    @FXML
    private void handleSupport() {
        if (onSupport != null) {
            onSupport.run();
        }
    }
    
    public enum ErrorType {
        NOT_FOUND,
        SERVER_ERROR,
        NETWORK_ERROR,
        UNAUTHORIZED,
        GENERIC
    }
}
