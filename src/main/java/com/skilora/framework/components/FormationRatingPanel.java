package com.skilora.framework.components;

import com.skilora.formation.entity.FormationRating;
import com.skilora.formation.service.FormationRatingService;
import com.skilora.formation.service.FormationRatingService.RatingStatistics;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * FormationRatingPanel
 * 
 * Premium dark-themed rating panel with Like/Dislike buttons, star rating,
 * and professional statistics display. Designed to match the global dark premium design.
 * 
 * Adapted from branch TrainingRatingPanel.java.
 */
public class FormationRatingPanel extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(FormationRatingPanel.class);
    
    private final FormationRatingService ratingService;
    private final int userId;
    private final int formationId;
    
    private StarRatingComponent starRating;
    private Button likeButton;
    private Button dislikeButton;
    private Label statisticsLabel;
    private Label messageLabel;
    private VBox ratingControls;
    private VBox statisticsBox;
    private ProgressBar ratingProgressBar;
    
    private Boolean currentLikeStatus;
    private int currentStarRating;
    private boolean hasRated;
    
    // Dark theme colors matching design system
    private static final String BG_CARD = "#09090b";
    private static final String BG_SECONDARY = "#18181b";
    private static final String BG_MUTED = "#27272a";
    private static final String BORDER_COLOR = "#27272a";
    private static final String TEXT_PRIMARY = "#fafafa";
    private static final String TEXT_SECONDARY = "#a1a1aa";
    @SuppressWarnings("unused")
    private static final String TEXT_MUTED = "#71717a";
    private static final String SUCCESS_COLOR = "#22c55e";
    private static final String DESTRUCTIVE_COLOR = "#ef4444";
    
    public FormationRatingPanel(int userId, int formationId) {
        this.userId = userId;
        this.formationId = formationId;
        this.ratingService = FormationRatingService.getInstance();
        
        getStyleClass().add("rating-panel");
        setSpacing(24);
        setPadding(new Insets(24));
        setStyle(
            "-fx-background-color: " + BG_CARD + "; " +
            "-fx-background-radius: 6px; " +
            "-fx-border-color: " + BORDER_COLOR + "; " +
            "-fx-border-radius: 6px; " +
            "-fx-border-width: 1px;"
        );
        
        initializeComponents();
        loadExistingRating();
        loadStatistics();
    }
    
    private void initializeComponents() {
        // Title
        Label titleLabel = new Label("Évaluez cette formation");
        titleLabel.setStyle(
            "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
            "-fx-font-size: 20px; " +
            "-fx-font-weight: 600; " +
            "-fx-text-fill: " + TEXT_PRIMARY + ";"
        );
        
        // Message label (feedback)
        messageLabel = new Label();
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
        messageLabel.setWrapText(true);
        messageLabel.setPadding(new Insets(12, 16, 12, 16));
        messageLabel.setStyle(
            "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
            "-fx-font-size: 14px; " +
            "-fx-background-radius: 6px; " +
            "-fx-background-color: " + BG_MUTED + ";"
        );
        
        // Star rating section
        VBox starSection = new VBox(8);
        Label starLabel = new Label("Notez avec des étoiles");
        starLabel.setStyle(
            "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: 500; " +
            "-fx-text-fill: " + TEXT_SECONDARY + ";"
        );
        
        starRating = new StarRatingComponent(true);
        starRating.ratingProperty().addListener((obs, oldVal, newVal) -> {
            currentStarRating = newVal.intValue();
            if (currentStarRating > 0 && !hasRated) {
                enableSubmit();
            }
        });
        starSection.getChildren().addAll(starLabel, starRating);
        
        // Like/Dislike section
        VBox feedbackSection = new VBox(8);
        feedbackSection.setFillWidth(false);
        
        Label feedbackLabel = new Label("Votre avis");
        feedbackLabel.setStyle(
            "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: 500; " +
            "-fx-text-fill: " + TEXT_SECONDARY + ";"
        );
        
        HBox likeDislikeBox = new HBox(12);
        likeDislikeBox.setAlignment(Pos.CENTER_LEFT);
        likeDislikeBox.setFillHeight(false);
        likeDislikeBox.setMaxWidth(Double.MAX_VALUE);
        
        likeButton = createLikeDislikeButton("J'aime", true, 150);
        dislikeButton = createLikeDislikeButton("Je n'aime pas", false, 200);
        
        likeButton.setOnAction(e -> handleLikeDislike(true));
        dislikeButton.setOnAction(e -> handleLikeDislike(false));
        
        HBox.setHgrow(likeButton, Priority.NEVER);
        HBox.setHgrow(dislikeButton, Priority.NEVER);
        likeButton.setMaxWidth(Region.USE_PREF_SIZE);
        dislikeButton.setMaxWidth(Region.USE_PREF_SIZE);
        
        likeDislikeBox.getChildren().addAll(likeButton, dislikeButton);
        feedbackSection.getChildren().addAll(feedbackLabel, likeDislikeBox);
        
        // Submit button
        TLButton submitButton = new TLButton("Soumettre l'évaluation", TLButton.ButtonVariant.PRIMARY);
        submitButton.getStyleClass().removeAll("btn", "btn-primary");
        submitButton.setMinWidth(300);
        submitButton.setPrefWidth(300);
        submitButton.setMaxWidth(Double.MAX_VALUE);
        submitButton.setWrapText(false);
        submitButton.setContentDisplay(ContentDisplay.CENTER);
        submitButton.setAlignment(Pos.CENTER);
        submitButton.setStyle(
            "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 8px 16px; " +
            "-fx-background-color: -fx-primary; " +
            "-fx-text-fill: -fx-primary-foreground; " +
            "-fx-background-radius: 6px; " +
            "-fx-cursor: hand; " +
            "-fx-alignment: center; " +
            "-fx-min-width: 300px; " +
            "-fx-pref-width: 300px; " +
            "-fx-max-width: infinity;"
        );
        submitButton.setOnAction(e -> submitRating());
        submitButton.setDisable(true);
        submitButton.setId("submitRatingButton");
        
        // Rating controls container
        ratingControls = new VBox(16);
        ratingControls.setFillWidth(false);
        ratingControls.getChildren().addAll(titleLabel, starSection, feedbackSection, submitButton, messageLabel);
        
        // Statistics box
        statisticsBox = new VBox(16);
        statisticsBox.setPadding(new Insets(20));
        statisticsBox.setStyle(
            "-fx-background-color: " + BG_SECONDARY + "; " +
            "-fx-background-radius: 6px; " +
            "-fx-border-color: " + BORDER_COLOR + "; " +
            "-fx-border-radius: 6px; " +
            "-fx-border-width: 1px;"
        );
        
        Label statsTitle = new Label("Statistiques");
        statsTitle.setStyle(
            "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
            "-fx-font-size: 18px; " +
            "-fx-font-weight: 600; " +
            "-fx-text-fill: " + TEXT_PRIMARY + ";"
        );
        
        ratingProgressBar = new ProgressBar(0);
        ratingProgressBar.setPrefWidth(Region.USE_COMPUTED_SIZE);
        ratingProgressBar.setMaxWidth(Double.MAX_VALUE);
        ratingProgressBar.setPrefHeight(8);
        ratingProgressBar.setStyle(
            "-fx-background-color: " + BG_MUTED + "; " +
            "-fx-background-radius: 4px; " +
            "-fx-border-radius: 4px; " +
            "-fx-accent: linear-gradient(to right, #FFD700, #FFA500);"
        );
        
        statisticsLabel = new Label();
        statisticsLabel.setStyle(
            "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
            "-fx-font-size: 14px; " +
            "-fx-text-fill: " + TEXT_SECONDARY + "; " +
            "-fx-line-spacing: 4px;"
        );
        
        statisticsBox.getChildren().addAll(statsTitle, ratingProgressBar, statisticsLabel);
        getChildren().addAll(ratingControls, statisticsBox);
    }
    
    private Button createLikeDislikeButton(String text, boolean isLike, double fixedWidth) {
        Button button = new Button(text);
        button.setMinWidth(fixedWidth);
        button.setPrefWidth(fixedWidth);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setWrapText(false);
        button.setContentDisplay(ContentDisplay.CENTER);
        button.setAlignment(Pos.CENTER);
        
        String baseStyle = buildButtonStyle(BG_MUTED, TEXT_SECONDARY, BORDER_COLOR, fixedWidth);
        button.setStyle(baseStyle);
        
        button.setOnMouseEntered(e -> {
            String hoverBg = isLike ? "rgba(34, 197, 94, 0.15)" : "rgba(239, 68, 68, 0.15)";
            String hoverColor = isLike ? SUCCESS_COLOR : DESTRUCTIVE_COLOR;
            button.setStyle(buildButtonStyle(hoverBg, hoverColor, hoverColor, fixedWidth));
        });
        
        button.setOnMouseExited(e -> {
            if ((isLike && (currentLikeStatus == null || !currentLikeStatus)) ||
                (!isLike && (currentLikeStatus == null || currentLikeStatus))) {
                button.setStyle(buildButtonStyle(BG_MUTED, TEXT_SECONDARY, BORDER_COLOR, fixedWidth));
            }
        });
        
        return button;
    }
    
    private String buildButtonStyle(String bgColor, String textColor, String borderColor, double width) {
        return "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
               "-fx-font-size: 13px; " +
               "-fx-font-weight: 500; " +
               "-fx-background-color: " + bgColor + "; " +
               "-fx-text-fill: " + textColor + "; " +
               "-fx-padding: 8px 12px; " +
               "-fx-background-radius: 6px; " +
               "-fx-border-color: " + borderColor + "; " +
               "-fx-border-width: 1px; " +
               "-fx-border-radius: 6px; " +
               "-fx-cursor: hand; " +
               "-fx-alignment: center; " +
               "-fx-content-display: center; " +
               "-fx-text-alignment: center; " +
               "-fx-min-width: " + width + "px; " +
               "-fx-pref-width: " + width + "px; " +
               "-fx-max-width: infinity;";
    }
    
    private void handleLikeDislike(boolean isLike) {
        if (currentLikeStatus != null && currentLikeStatus == isLike) {
            currentLikeStatus = null;
        } else {
            currentLikeStatus = isLike;
        }
        updateLikeDislikeButtons();
        
        if (currentStarRating > 0 && !hasRated) {
            enableSubmit();
        }
    }
    
    private void updateLikeDislikeButtons() {
        if (currentLikeStatus == null || !currentLikeStatus) {
            likeButton.setStyle(buildButtonStyle(BG_MUTED, TEXT_SECONDARY, BORDER_COLOR, 150));
        } else {
            likeButton.setStyle(buildButtonStyle("rgba(34, 197, 94, 0.2)", SUCCESS_COLOR, SUCCESS_COLOR, 150));
        }
        
        if (currentLikeStatus == null || currentLikeStatus) {
            dislikeButton.setStyle(buildButtonStyle(BG_MUTED, TEXT_SECONDARY, BORDER_COLOR, 200));
        } else {
            dislikeButton.setStyle(buildButtonStyle("rgba(239, 68, 68, 0.2)", DESTRUCTIVE_COLOR, DESTRUCTIVE_COLOR, 200));
        }
    }
    
    private void enableSubmit() {
        Button submitButton = (Button) ratingControls.lookup("#submitRatingButton");
        if (submitButton != null) {
            submitButton.setDisable(false);
        }
    }
    
    private void submitRating() {
        if (currentStarRating < 1 || currentStarRating > 5) {
            showMessage("Veuillez sélectionner une note d'au moins 1 étoile.", true);
            return;
        }
        
        try {
            ratingService.submitRating(userId, formationId, currentLikeStatus, currentStarRating);
            hasRated = true;
            
            showMessage("Merci pour votre évaluation !", false);
            
            starRating.setInteractive(false);
            likeButton.setDisable(true);
            dislikeButton.setDisable(true);
            
            Button submitButton = (Button) ratingControls.lookup("#submitRatingButton");
            if (submitButton != null) {
                submitButton.setDisable(true);
                submitButton.setText("Évaluation soumise");
            }
            
            loadStatistics();
            
            logger.info("Rating submitted: userId={}, formationId={}, rating={}, liked={}", 
                userId, formationId, currentStarRating, currentLikeStatus);
        } catch (IllegalStateException e) {
            showMessage("Vous devez compléter la formation avant de pouvoir l'évaluer.", true);
            logger.warn("Cannot rate incomplete formation: {}", e.getMessage());
        } catch (Exception e) {
            showMessage("Une erreur s'est produite lors de la soumission de l'évaluation.", true);
            logger.error("Error submitting rating", e);
        }
    }
    
    private void loadExistingRating() {
        try {
            Optional<FormationRating> existingRating = ratingService.getUserRating(userId, formationId);
            if (existingRating.isPresent()) {
                FormationRating rating = existingRating.get();
                hasRated = true;
                currentStarRating = rating.getStarRating();
                currentLikeStatus = rating.getIsLiked();
                
                starRating.setRating(currentStarRating);
                starRating.setInteractive(false);
                updateLikeDislikeButtons();
                
                likeButton.setDisable(true);
                dislikeButton.setDisable(true);
                
                Button submitButton = (Button) ratingControls.lookup("#submitRatingButton");
                if (submitButton != null) {
                    submitButton.setDisable(true);
                    submitButton.setText("Évaluation soumise");
                }
                
                showMessage("Vous avez déjà évalué cette formation.", false);
            }
        } catch (Exception e) {
            logger.error("Error loading existing rating", e);
        }
    }
    
    private void loadStatistics() {
        try {
            RatingStatistics stats = ratingService.getStatistics(formationId);
            
            if (stats.getTotalRatings() == 0) {
                statisticsLabel.setText("Aucune évaluation pour le moment.");
                ratingProgressBar.setProgress(0);
            } else {
                double averageRating = stats.getAverageRating();
                ratingProgressBar.setProgress(averageRating / 5.0);
                
                String statsText = String.format(
                    "Note moyenne: %.1f / 5.0\n" +
                    "Total d'évaluations: %d\n" +
                    "J'aime: %d  •  Je n'aime pas: %d",
                    averageRating,
                    stats.getTotalRatings(),
                    stats.getLikeCount(),
                    stats.getDislikeCount()
                );
                statisticsLabel.setText(statsText);
            }
        } catch (Exception e) {
            logger.error("Error loading statistics", e);
            statisticsLabel.setText("Erreur lors du chargement des statistiques.");
            ratingProgressBar.setProgress(0);
        }
    }
    
    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
        
        String color = isError ? DESTRUCTIVE_COLOR : SUCCESS_COLOR;
        String bgAlpha = isError ? "rgba(239, 68, 68, 0.15)" : "rgba(34, 197, 94, 0.15)";
        
        messageLabel.setStyle(
            "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
            "-fx-font-size: 14px; " +
            "-fx-padding: 12px 16px; " +
            "-fx-background-color: " + bgAlpha + "; " +
            "-fx-text-fill: " + color + "; " +
            "-fx-background-radius: 6px; " +
            "-fx-border-color: " + color + "; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 6px;"
        );
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), messageLabel);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
        
        // Auto-hide after 5 seconds
        Thread autoHide = new Thread(() -> {
            try {
                Thread.sleep(5000);
                Platform.runLater(() -> {
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(200), messageLabel);
                    fadeOut.setFromValue(1);
                    fadeOut.setToValue(0);
                    fadeOut.setOnFinished(e -> {
                        messageLabel.setVisible(false);
                        messageLabel.setManaged(false);
                    });
                    fadeOut.play();
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "RatingMessageAutoHide");
        autoHide.setDaemon(true);
        autoHide.start();
    }
}
