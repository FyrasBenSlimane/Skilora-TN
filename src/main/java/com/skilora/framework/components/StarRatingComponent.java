package com.skilora.framework.components;

import javafx.animation.ScaleTransition;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.effect.Glow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StarRatingComponent
 * 
 * Premium interactive 5-star rating component with smooth animations,
 * gold gradient when selected, and subtle glow effects.
 * Designed to match the dark premium theme.
 */
public class StarRatingComponent extends HBox {
    private static final Logger logger = LoggerFactory.getLogger(StarRatingComponent.class);
    
    private final IntegerProperty rating;
    private final IntegerProperty hoveredRating;
    private final StarShape[] stars;
    private boolean interactive;
    
    // Star SVG path (filled star)
    private static final String STAR_PATH = "M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z";
    
    // Dark theme colors
    private static final Color STAR_UNFILLED = Color.web("#3f3f46"); // ZINC_700
    private static final Color STAR_UNFILLED_STROKE = Color.web("#52525b"); // ZINC_600
    private static final Color STAR_HOVER = Color.web("#71717a"); // ZINC_500
    
    public StarRatingComponent(boolean interactive) {
        this.interactive = interactive;
        this.rating = new SimpleIntegerProperty(0);
        this.hoveredRating = new SimpleIntegerProperty(0);
        this.stars = new StarShape[5];
        
        setSpacing(8); // Design system spacing
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(0));
        
        initializeStars();
        setupListeners();
    }
    
    public StarRatingComponent() {
        this(true);
    }
    
    private void initializeStars() {
        for (int i = 0; i < 5; i++) {
            StarShape star = new StarShape(i + 1);
            stars[i] = star;
            getChildren().add(star);
            
            if (interactive) {
                final int starIndex = i + 1;
                
                star.setOnMouseEntered(e -> {
                    hoveredRating.set(starIndex);
                    updateStarColors();
                });
                
                star.setOnMouseExited(e -> {
                    hoveredRating.set(0);
                    updateStarColors();
                });
                
                star.setOnMouseClicked(e -> {
                    rating.set(starIndex);
                    updateStarColors();
                    logger.debug("Star rating set to: {}", starIndex);
                });
            }
        }
        
        updateStarColors();
    }
    
    private void setupListeners() {
        rating.addListener((obs, oldVal, newVal) -> {
            if (newVal.intValue() == 0) {
                hoveredRating.set(0);
            }
            updateStarColors();
        });
    }
    
    private void updateStarColors() {
        int currentRating = hoveredRating.get() > 0 ? hoveredRating.get() : rating.get();
        
        for (int i = 0; i < 5; i++) {
            if (i < currentRating) {
                stars[i].setFilled(true);
            } else {
                stars[i].setFilled(false);
            }
        }
    }
    
    public int getRating() {
        return rating.get();
    }
    
    public IntegerProperty ratingProperty() {
        return rating;
    }
    
    public void setRating(int rating) {
        if (rating < 0 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 0 and 5");
        }
        this.rating.set(rating);
    }
    
    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
        for (StarShape star : stars) {
            star.setMouseTransparent(!interactive);
        }
    }
    
    public boolean isInteractive() {
        return interactive;
    }
    
    /**
     * Inner class for individual star shape with premium animations
     */
    private class StarShape extends Region {
        private final int starNumber;
        private final SVGPath starPath;
        private boolean filled;
        private ScaleTransition hoverTransition;
        private Glow glowEffect;
        
        public StarShape(int starNumber) {
            this.starNumber = starNumber;
            this.filled = false;
            
            starPath = new SVGPath();
            starPath.setContent(STAR_PATH);
            starPath.setFill(STAR_UNFILLED);
            starPath.setStroke(STAR_UNFILLED_STROKE);
            starPath.setStrokeWidth(0.5);
            
            // Size - larger for better visibility
            starPath.setScaleX(1.0);
            starPath.setScaleY(1.0);
            
            // Glow effect for filled stars
            glowEffect = new Glow(0.3);
            
            // Smooth scale transition
            hoverTransition = new ScaleTransition(Duration.millis(150), starPath);
            hoverTransition.setFromX(1.0);
            hoverTransition.setFromY(1.0);
            hoverTransition.setToX(1.15);
            hoverTransition.setToY(1.15);
            
            getChildren().add(starPath);
            
            // Set preferred size
            setPrefWidth(32);
            setPrefHeight(32);
            setMinWidth(32);
            setMinHeight(32);
            
            // Hover effect with smooth animation
            if (interactive) {
                setCursor(javafx.scene.Cursor.HAND);
                
                setOnMouseEntered(e -> {
                    hoverTransition.setRate(1.0);
                    hoverTransition.play();
                    if (!filled) {
                        starPath.setFill(STAR_HOVER);
                    }
                });
                
                setOnMouseExited(e -> {
                    hoverTransition.setRate(-1.0);
                    hoverTransition.play();
                    if (!filled) {
                        starPath.setFill(STAR_UNFILLED);
                    }
                });
            }
        }
        
        public void setFilled(boolean filled) {
            this.filled = filled;
            if (filled) {
                // Premium gold gradient
                LinearGradient goldGradient = new LinearGradient(
                    0, 0, 1, 1,
                    true,
                    javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#FFD700")), // Gold
                    new Stop(0.5, Color.web("#FFA500")), // Orange
                    new Stop(1, Color.web("#FF8C00")) // Dark orange
                );
                starPath.setFill(goldGradient);
                starPath.setStroke(Color.web("#FFD700"));
                starPath.setStrokeWidth(0.8);
                starPath.setEffect(glowEffect);
            } else {
                starPath.setFill(STAR_UNFILLED);
                starPath.setStroke(STAR_UNFILLED_STROKE);
                starPath.setStrokeWidth(0.5);
                starPath.setEffect(null);
            }
        }
        
        public boolean isFilled() {
            return filled;
        }
    }
}
