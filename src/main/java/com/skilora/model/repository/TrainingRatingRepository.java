package com.skilora.model.repository;

import com.skilora.model.entity.formation.TrainingRating;
import java.util.Optional;

/**
 * TrainingRatingRepository
 * 
 * Repository interface for training rating operations.
 */
public interface TrainingRatingRepository {
    
    /**
     * Save or update a training rating
     */
    TrainingRating save(TrainingRating rating);
    
    /**
     * Find a rating by user ID and training ID
     */
    Optional<TrainingRating> findByUserIdAndTrainingId(int userId, int trainingId);
    
    /**
     * Check if a user has already rated a training
     */
    boolean existsByUserIdAndTrainingId(int userId, int trainingId);
    
    /**
     * Delete a rating
     */
    void delete(int id);
    
    /**
     * Get statistics for a training
     */
    RatingStatistics getStatistics(int trainingId);
    
    /**
     * Rating statistics data class
     */
    class RatingStatistics {
        private final long totalRatings;
        private final long totalLikes;
        private final long totalDislikes;
        private final double averageRating;
        
        public RatingStatistics(long totalRatings, long totalLikes, long totalDislikes, double averageRating) {
            this.totalRatings = totalRatings;
            this.totalLikes = totalLikes;
            this.totalDislikes = totalDislikes;
            this.averageRating = averageRating;
        }
        
        public long getTotalRatings() {
            return totalRatings;
        }
        
        public long getTotalLikes() {
            return totalLikes;
        }
        
        public long getTotalDislikes() {
            return totalDislikes;
        }
        
        public double getAverageRating() {
            return averageRating;
        }
    }
}
