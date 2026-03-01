package com.skilora.service.formation;

import com.skilora.model.entity.formation.TrainingRating;
import com.skilora.model.repository.TrainingRatingRepository;
import com.skilora.model.repository.impl.TrainingRatingRepositoryImpl;
import com.skilora.model.repository.TrainingRatingRepository.RatingStatistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * TrainingRatingService
 * 
 * Handles business logic for training ratings.
 * Ensures users can only rate completed formations and only once per formation.
 */
public class TrainingRatingService {

    private static final Logger logger = LoggerFactory.getLogger(TrainingRatingService.class);
    
    private final TrainingRatingRepository repository;
    private final TrainingEnrollmentService enrollmentService;
    private static volatile TrainingRatingService instance;

    private TrainingRatingService() {
        this.repository = new TrainingRatingRepositoryImpl();
        this.enrollmentService = TrainingEnrollmentService.getInstance();
    }

    public static TrainingRatingService getInstance() {
        if (instance == null) {
            synchronized (TrainingRatingService.class) {
                if (instance == null) {
                    instance = new TrainingRatingService();
                }
            }
        }
        return instance;
    }

    /**
     * Submit or update a rating for a training.
     * Validates that:
     * 1. The user has completed the training
     * 2. The star rating is between 1 and 5
     * 3. If updating, the user already has a rating
     * 
     * @param userId The user ID
     * @param trainingId The training ID
     * @param isLiked TRUE for like, FALSE for dislike, NULL for no preference
     * @param starRating Rating from 1 to 5 stars
     * @return The saved rating
     * @throws IllegalArgumentException if validation fails
     */
    public TrainingRating submitRating(int userId, int trainingId, Boolean isLiked, int starRating) {
        logger.info("Submitting rating: userId={}, trainingId={}, isLiked={}, starRating={}", 
            userId, trainingId, isLiked, starRating);
        
        // Validate star rating
        if (starRating < 1 || starRating > 5) {
            throw new IllegalArgumentException("Star rating must be between 1 and 5");
        }
        
        // Check if training is completed
        boolean isCompleted = enrollmentService.isTrainingCompleted(userId, trainingId);
        if (!isCompleted) {
            throw new IllegalStateException("Cannot rate a training that has not been completed");
        }
        
        // Check if rating already exists
        Optional<TrainingRating> existingRating = repository.findByUserIdAndTrainingId(userId, trainingId);
        
        TrainingRating rating;
        if (existingRating.isPresent()) {
            // Update existing rating
            rating = existingRating.get();
            rating.setIsLiked(isLiked);
            rating.setStarRating(starRating);
            logger.info("Updating existing rating: id={}", rating.getId());
        } else {
            // Create new rating
            rating = new TrainingRating(userId, trainingId, isLiked, starRating);
            logger.info("Creating new rating");
        }
        
        return repository.save(rating);
    }

    /**
     * Get the user's rating for a training, if it exists
     */
    public Optional<TrainingRating> getUserRating(int userId, int trainingId) {
        return repository.findByUserIdAndTrainingId(userId, trainingId);
    }

    /**
     * Check if user has already rated this training
     */
    public boolean hasUserRated(int userId, int trainingId) {
        return repository.existsByUserIdAndTrainingId(userId, trainingId);
    }

    /**
     * Get aggregated statistics for a training
     */
    public RatingStatistics getStatistics(int trainingId) {
        return repository.getStatistics(trainingId);
    }

    /**
     * Check if user can rate this training (must be completed and not already rated)
     */
    public boolean canUserRate(int userId, int trainingId) {
        boolean isCompleted = enrollmentService.isTrainingCompleted(userId, trainingId);
        boolean alreadyRated = repository.existsByUserIdAndTrainingId(userId, trainingId);
        return isCompleted && !alreadyRated;
    }

    /**
     * Check if user can update their rating (must be completed and already rated)
     */
    public boolean canUserUpdateRating(int userId, int trainingId) {
        boolean isCompleted = enrollmentService.isTrainingCompleted(userId, trainingId);
        boolean alreadyRated = repository.existsByUserIdAndTrainingId(userId, trainingId);
        return isCompleted && alreadyRated;
    }
}
