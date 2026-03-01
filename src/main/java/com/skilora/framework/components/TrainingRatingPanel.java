package com.skilora.framework.components;

import com.skilora.model.entity.formation.TrainingRating;
import com.skilora.model.repository.TrainingRatingRepository.RatingStatistics;
import com.skilora.service.formation.TrainingRatingService;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * TrainingRatingPanel
 * 
 * Premium dark-themed rating panel with Like/Dislike buttons, star rating,
 * and professional statistics display. Designed to match the global dark premium design.
 */
public class TrainingRatingPanel extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(TrainingRatingPanel.class);
    
    private final TrainingRatingService ratingService;
    private final int userId;
    private final int trainingId;
    
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
    private static final String BG_CARD = "#09090b"; // ZINC_950
    private static final String BG_SECONDARY = "#18181b"; // ZINC_900
    private static final String BG_MUTED = "#27272a"; // ZINC_800
    private static final String BORDER_COLOR = "#27272a"; // ZINC_800
    private static final String TEXT_PRIMARY = "#fafafa"; // ZINC_50
    private static final String TEXT_SECONDARY = "#a1a1aa"; // ZINC_400
    private static final String TEXT_MUTED = "#71717a"; // ZINC_500
    private static final String ACCENT_PRIMARY = "#fafafa"; // Primary accent
    private static final String SUCCESS_COLOR = "#22c55e"; // Green
    private static final String DESTRUCTIVE_COLOR = "#ef4444"; // Red
    
    public TrainingRatingPanel(int userId, int trainingId) {
        this.userId = userId;
        this.trainingId = trainingId;
        this.ratingService = TrainingRatingService.getInstance();
        
        getStyleClass().add("rating-panel");
        setSpacing(24); // Design system: SPACING_LG
        setPadding(new Insets(24)); // Design system: SPACING_LG
        setStyle(
            "-fx-background-color: " + BG_CARD + "; " +
            "-fx-background-radius: 6px; " + // Design system: RADIUS_MD
            "-fx-border-color: " + BORDER_COLOR + "; " +
            "-fx-border-radius: 6px; " +
            "-fx-border-width: 1px;"
        );
        
        initializeComponents();
        loadExistingRating();
        loadStatistics();
    }
    
    private void initializeComponents() {
        // Title with premium typography
        Label titleLabel = new Label("Évaluez cette formation");
        titleLabel.setStyle(
            "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
            "-fx-font-size: 20px; " + // Design system: text-xl
            "-fx-font-weight: 600; " + // semibold
            "-fx-text-fill: " + TEXT_PRIMARY + ";"
        );
        
        // Message label (for feedback) with fade animation
        messageLabel = new Label();
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
        messageLabel.setWrapText(true);
        messageLabel.setPadding(new Insets(12, 16, 12, 16));
        messageLabel.setStyle(
            "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
            "-fx-font-size: 14px; " + // Design system: text-sm
            "-fx-background-radius: 6px; " +
            "-fx-background-color: " + BG_MUTED + ";"
        );
        
        // Star rating section
        VBox starSection = new VBox(8);
        starSection.setSpacing(8);
        
        Label starLabel = new Label("Notez avec des étoiles");
        starLabel.setStyle(
            "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: 500; " + // medium
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
        feedbackSection.setSpacing(8);
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
        likeDislikeBox.setPrefWidth(Region.USE_COMPUTED_SIZE); // Allow HBox to size to content
        
        likeButton = createLikeDislikeButton("J'aime", true);
        dislikeButton = createLikeDislikeButton("Je n'aime pas", false);
        
        likeButton.setOnAction(e -> handleLikeDislike(true));
        dislikeButton.setOnAction(e -> handleLikeDislike(false));
        
        // Ensure buttons don't shrink and can display full text
        HBox.setHgrow(likeButton, Priority.NEVER);
        HBox.setHgrow(dislikeButton, Priority.NEVER);
        
        // Force buttons to maintain their preferred width
        likeButton.setMaxWidth(Region.USE_PREF_SIZE);
        dislikeButton.setMaxWidth(Region.USE_PREF_SIZE);
        
        likeDislikeBox.getChildren().addAll(likeButton, dislikeButton);
        feedbackSection.getChildren().addAll(feedbackLabel, likeDislikeBox);
        
        // Submit button with premium styling
        TLButton submitButton = new TLButton("Soumettre l'évaluation", TLButton.ButtonVariant.PRIMARY);
        
        // Remove default button classes that might limit width
        submitButton.getStyleClass().removeAll("btn", "btn-primary");
        
        // Ensure button can display full text without truncation
        // Use exact fixed width as specified by user
        double submitMinWidth = 300; // "Soumettre l'évaluation" - 300px as specified
        double submitPrefWidth = 300; // Fixed preferred width
        
        submitButton.setMinWidth(submitMinWidth);
        submitButton.setPrefWidth(submitPrefWidth); // Fixed preferred width
        submitButton.setMaxWidth(Double.MAX_VALUE);
        submitButton.setWrapText(false);
        submitButton.setContentDisplay(javafx.scene.control.ContentDisplay.CENTER);
        submitButton.setAlignment(javafx.geometry.Pos.CENTER);
        
        submitButton.setStyle(
            "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
            "-fx-font-size: 13px; " + // Slightly smaller font for better fit
            "-fx-font-weight: 500; " +
            "-fx-padding: 8px 16px; " + // Reduced padding: 8px vertical, 16px horizontal
            "-fx-background-color: -fx-primary; " +
            "-fx-text-fill: -fx-primary-foreground; " +
            "-fx-background-radius: 6px; " +
            "-fx-cursor: hand; " +
            "-fx-alignment: center; " + // Center text
            "-fx-content-display: center; " + // Center content
            "-fx-text-alignment: center; " + // Center text alignment
            "-fx-min-width: " + submitMinWidth + "px; " +
            "-fx-pref-width: " + submitPrefWidth + "px; " + // Fixed preferred width
            "-fx-max-width: infinity;"
        );
        submitButton.setOnAction(e -> submitRating());
        submitButton.setDisable(true);
        submitButton.setId("submitRatingButton");
        
        // Rating controls container
        ratingControls = new VBox(16);
        ratingControls.setSpacing(16);
        ratingControls.setFillWidth(false); // Allow children to size to content
        ratingControls.getChildren().addAll(
            titleLabel,
            starSection,
            feedbackSection,
            submitButton,
            messageLabel
        );
        
        // Statistics box with premium design
        statisticsBox = new VBox(16);
        statisticsBox.setSpacing(16);
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
            "-fx-font-size: 18px; " + // Design system: text-lg
            "-fx-font-weight: 600; " +
            "-fx-text-fill: " + TEXT_PRIMARY + ";"
        );
        
        // Rating progress bar
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
    
    private Button createLikeDislikeButton(String text, boolean isLike) {
        Button button = new Button(text);
        
        // Ensure button can display full text without truncation
        // Use exact fixed widths as specified by user
        double minWidth;
        double prefWidth;
        if (text.equals("J'aime")) {
            minWidth = 150; // "J'aime" - 150px as specified
            prefWidth = 150;
        } else if (text.equals("Je n'aime pas")) {
            minWidth = 200; // "Je n'aime pas" - 200px as specified
            prefWidth = 200;
        } else {
            minWidth = Math.max(200, text.length() * 12 + 60); // Fallback calculation
            prefWidth = minWidth;
        }
        
        button.setMinWidth(minWidth);
        button.setPrefWidth(prefWidth); // Fixed preferred width
        button.setMaxWidth(Double.MAX_VALUE);
        button.setWrapText(false);
        button.setContentDisplay(javafx.scene.control.ContentDisplay.CENTER);
        button.setAlignment(javafx.geometry.Pos.CENTER);
        
        // Remove any CSS constraints that might limit width
        // Reduce padding to give more space for text
        button.setStyle(
            "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
            "-fx-font-size: 13px; " + // Slightly smaller font for better fit
            "-fx-font-weight: 500; " +
            "-fx-background-color: " + BG_MUTED + "; " +
            "-fx-text-fill: " + TEXT_SECONDARY + "; " +
            "-fx-padding: 8px 12px; " + // Reduced padding: 8px vertical, 12px horizontal
            "-fx-background-radius: 6px; " +
            "-fx-border-color: " + BORDER_COLOR + "; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 6px; " +
            "-fx-cursor: hand; " +
            "-fx-alignment: center; " + // Center text
            "-fx-content-display: center; " + // Center content
            "-fx-text-alignment: center; " + // Center text alignment
            "-fx-min-width: " + minWidth + "px; " +
            "-fx-pref-width: " + prefWidth + "px; " + // Fixed preferred width
            "-fx-max-width: infinity;"
        );
        
        // Smooth hover transition - preserve width constraints
        button.setOnMouseEntered(e -> {
            if (isLike) {
                button.setStyle(
                    "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
                    "-fx-font-size: 13px; " + // Slightly smaller font
                    "-fx-font-weight: 500; " +
                    "-fx-background-color: rgba(34, 197, 94, 0.15); " +
                    "-fx-text-fill: " + SUCCESS_COLOR + "; " +
                    "-fx-padding: 8px 12px; " + // Reduced padding
                    "-fx-alignment: center; " +
                    "-fx-content-display: center; " +
                    "-fx-text-alignment: center; " +
                    "-fx-background-radius: 6px; " +
                    "-fx-border-color: " + SUCCESS_COLOR + "; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 6px; " +
                    "-fx-cursor: hand; " +
                    "-fx-min-width: " + minWidth + "px; " +
                    "-fx-pref-width: " + prefWidth + "px; " + // Fixed preferred width
                    "-fx-max-width: infinity;"
                );
            } else {
                button.setStyle(
                    "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
                    "-fx-font-size: 13px; " + // Slightly smaller font
                    "-fx-font-weight: 500; " +
                    "-fx-background-color: rgba(239, 68, 68, 0.15); " +
                    "-fx-text-fill: " + DESTRUCTIVE_COLOR + "; " +
                    "-fx-padding: 8px 12px; " + // Reduced padding
                    "-fx-alignment: center; " +
                    "-fx-content-display: center; " +
                    "-fx-text-alignment: center; " +
                    "-fx-background-radius: 6px; " +
                    "-fx-border-color: " + DESTRUCTIVE_COLOR + "; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 6px; " +
                    "-fx-cursor: hand; " +
                    "-fx-min-width: " + minWidth + "px; " +
                    "-fx-pref-width: " + prefWidth + "px; " + // Fixed preferred width
                    "-fx-max-width: infinity;"
                );
            }
        });
        
        button.setOnMouseExited(e -> {
            if ((isLike && (currentLikeStatus == null || !currentLikeStatus)) || 
                (!isLike && (currentLikeStatus == null || currentLikeStatus))) {
                button.setStyle(
                    "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
                    "-fx-font-size: 13px; " + // Slightly smaller font
                    "-fx-font-weight: 500; " +
                    "-fx-background-color: " + BG_MUTED + "; " +
                    "-fx-text-fill: " + TEXT_SECONDARY + "; " +
                    "-fx-padding: 8px 12px; " + // Reduced padding
                    "-fx-alignment: center; " +
                    "-fx-content-display: center; " +
                    "-fx-text-alignment: center; " +
                    "-fx-background-radius: 6px; " +
                    "-fx-border-color: " + BORDER_COLOR + "; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 6px; " +
                    "-fx-cursor: hand; " +
                    "-fx-min-width: " + minWidth + "px; " +
                    "-fx-pref-width: " + prefWidth + "px; " + // Fixed preferred width
                    "-fx-max-width: infinity;"
                );
            }
        });
        
        return button;
    }
    
    private void handleLikeDislike(boolean isLike) {
        if (currentLikeStatus != null && currentLikeStatus == isLike) {
            // Deselect if clicking the same button
            currentLikeStatus = null;
            updateLikeDislikeButtons();
        } else {
            currentLikeStatus = isLike;
            updateLikeDislikeButtons();
        }
        
        if (currentStarRating > 0 && !hasRated) {
            enableSubmit();
        }
    }
    
    private void updateLikeDislikeButtons() {
        // Use exact fixed widths as specified by user
        double likeMinWidth = 150; // "J'aime" - 150px as specified
        double likePrefWidth = 150;
        double dislikeMinWidth = 200; // "Je n'aime pas" - 200px as specified
        double dislikePrefWidth = 200;
        
        // Like button
        if (currentLikeStatus == null || !currentLikeStatus) {
            likeButton.setStyle(
                "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
                "-fx-font-size: 13px; " + // Slightly smaller font
                "-fx-font-weight: 500; " +
                "-fx-background-color: " + BG_MUTED + "; " +
                "-fx-text-fill: " + TEXT_SECONDARY + "; " +
                "-fx-padding: 8px 12px; " + // Reduced padding
                "-fx-background-radius: 6px; " +
                "-fx-border-color: " + BORDER_COLOR + "; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 6px; " +
                "-fx-cursor: hand; " +
                "-fx-alignment: center; " +
                "-fx-content-display: center; " +
                "-fx-text-alignment: center; " +
                "-fx-min-width: " + likeMinWidth + "px; " +
                "-fx-pref-width: " + likePrefWidth + "px; " + // Fixed preferred width
                "-fx-max-width: infinity;"
            );
        } else {
            likeButton.setStyle(
                "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
                "-fx-font-size: 13px; " + // Slightly smaller font
                "-fx-font-weight: 500; " +
                "-fx-background-color: rgba(34, 197, 94, 0.2); " +
                "-fx-text-fill: " + SUCCESS_COLOR + "; " +
                "-fx-padding: 8px 12px; " + // Reduced padding
                "-fx-background-radius: 6px; " +
                "-fx-border-color: " + SUCCESS_COLOR + "; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 6px; " +
                "-fx-cursor: hand; " +
                "-fx-alignment: center; " +
                "-fx-content-display: center; " +
                "-fx-text-alignment: center; " +
                "-fx-min-width: " + likeMinWidth + "px; " +
                "-fx-pref-width: " + likePrefWidth + "px; " + // Fixed preferred width
                "-fx-max-width: infinity;"
            );
        }
        
        // Dislike button
        if (currentLikeStatus == null || currentLikeStatus) {
            dislikeButton.setStyle(
                "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
                "-fx-font-size: 13px; " + // Slightly smaller font
                "-fx-font-weight: 500; " +
                "-fx-background-color: " + BG_MUTED + "; " +
                "-fx-text-fill: " + TEXT_SECONDARY + "; " +
                "-fx-padding: 8px 12px; " + // Reduced padding
                "-fx-background-radius: 6px; " +
                "-fx-border-color: " + BORDER_COLOR + "; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 6px; " +
                "-fx-cursor: hand; " +
                "-fx-alignment: center; " +
                "-fx-content-display: center; " +
                "-fx-text-alignment: center; " +
                "-fx-min-width: " + dislikeMinWidth + "px; " +
                "-fx-pref-width: " + dislikePrefWidth + "px; " + // Fixed preferred width
                "-fx-max-width: infinity;"
            );
        } else {
            dislikeButton.setStyle(
                "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
                "-fx-font-size: 13px; " + // Slightly smaller font
                "-fx-font-weight: 500; " +
                "-fx-background-color: rgba(239, 68, 68, 0.2); " +
                "-fx-text-fill: " + DESTRUCTIVE_COLOR + "; " +
                "-fx-padding: 8px 12px; " + // Reduced padding
                "-fx-background-radius: 6px; " +
                "-fx-border-color: " + DESTRUCTIVE_COLOR + "; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 6px; " +
                "-fx-cursor: hand; " +
                "-fx-alignment: center; " +
                "-fx-content-display: center; " +
                "-fx-text-alignment: center; " +
                "-fx-min-width: " + dislikeMinWidth + "px; " +
                "-fx-pref-width: " + dislikePrefWidth + "px; " + // Fixed preferred width
                "-fx-max-width: infinity;"
            );
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
            ratingService.submitRating(userId, trainingId, currentLikeStatus, currentStarRating);
            hasRated = true;
            
            showMessage("Merci pour votre évaluation !", false);
            
            // Disable controls
            starRating.setInteractive(false);
            likeButton.setDisable(true);
            dislikeButton.setDisable(true);
            
            Button submitButton = (Button) ratingControls.lookup("#submitRatingButton");
            if (submitButton != null) {
                submitButton.setDisable(true);
                submitButton.setText("Évaluation soumise");
            }
            
            // Refresh statistics
            loadStatistics();
            
            logger.info("Rating submitted successfully: userId={}, trainingId={}, rating={}, liked={}", 
                userId, trainingId, currentStarRating, currentLikeStatus);
        } catch (IllegalStateException e) {
            showMessage("Vous devez compléter la formation avant de pouvoir l'évaluer.", true);
            logger.warn("Cannot rate incomplete training: {}", e.getMessage());
        } catch (Exception e) {
            showMessage("Une erreur s'est produite lors de la soumission de l'évaluation.", true);
            logger.error("Error submitting rating", e);
        }
    }
    
    private void loadExistingRating() {
        try {
            Optional<TrainingRating> existingRating = ratingService.getUserRating(userId, trainingId);
            if (existingRating.isPresent()) {
                TrainingRating rating = existingRating.get();
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
            RatingStatistics stats = ratingService.getStatistics(trainingId);
            
            if (stats.getTotalRatings() == 0) {
                statisticsLabel.setText("Aucune évaluation pour le moment.");
                ratingProgressBar.setProgress(0);
            } else {
                double averageRating = stats.getAverageRating();
                double progress = averageRating / 5.0; // Normalize to 0-1
                ratingProgressBar.setProgress(progress);
                
                String statsText = String.format(
                    "Note moyenne: %.1f / 5.0\n" +
                    "Total d'évaluations: %d\n" +
                    "J'aime: %d  •  Je n'aime pas: %d",
                    averageRating,
                    stats.getTotalRatings(),
                    stats.getTotalLikes(),
                    stats.getTotalDislikes()
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
        
        if (isError) {
            messageLabel.setStyle(
                "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
                "-fx-font-size: 14px; " +
                "-fx-padding: 12px 16px; " +
                "-fx-background-color: rgba(239, 68, 68, 0.15); " +
                "-fx-text-fill: " + DESTRUCTIVE_COLOR + "; " +
                "-fx-background-radius: 6px; " +
                "-fx-border-color: " + DESTRUCTIVE_COLOR + "; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 6px;"
            );
        } else {
            messageLabel.setStyle(
                "-fx-font-family: 'Inter', 'System', 'Segoe UI', sans-serif; " +
                "-fx-font-size: 14px; " +
                "-fx-padding: 12px 16px; " +
                "-fx-background-color: rgba(34, 197, 94, 0.15); " +
                "-fx-text-fill: " + SUCCESS_COLOR + "; " +
                "-fx-background-radius: 6px; " +
                "-fx-border-color: " + SUCCESS_COLOR + "; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 6px;"
            );
        }
        
        // Fade in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), messageLabel);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
        
        // Auto-hide after 5 seconds with fade out
        Platform.runLater(() -> {
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
        });
    }
}
