package com.skilora.service.formation;

import com.skilora.model.entity.formation.Training;
import com.skilora.model.enums.TrainingCategory;
import com.skilora.model.repository.TrainingRepository;
import com.skilora.model.repository.impl.TrainingRepositoryImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * TrainingService
 * 
 * Handles all training-related business logic.
 * This follows the MVC pattern where Service = Business logic layer.
 * Uses Repository for data access (NOT direct SQL).
 * 
 * Note: No JavaFX imports allowed in this class.
 */
public class TrainingService {

    private static final Logger logger = LoggerFactory.getLogger(TrainingService.class);
    
    private final TrainingRepository repository;
    private static volatile TrainingService instance;

    private TrainingService() {
        this.repository = new TrainingRepositoryImpl();
    }

    /**
     * Get singleton instance
     */
    public static TrainingService getInstance() {
        if (instance == null) {
            synchronized (TrainingService.class) {
                if (instance == null) {
                    instance = new TrainingService();
                }
            }
        }
        return instance;
    }

    // ==================== CRUD Operations ====================

    /**
     * Get all trainings
     */
    public List<Training> getAllTrainings() {
        try {
            return repository.findAll();
        } catch (Exception e) {
            logger.error("Error getting all trainings", e);
            throw new RuntimeException("Failed to retrieve trainings: " + e.getMessage(), e);
        }
    }

    /**
     * Get training by ID
     */
    public Optional<Training> getTrainingById(int id) {
        try {
            return repository.findById(id);
        } catch (Exception e) {
            logger.error("Error getting training by id: {}", id, e);
            throw new RuntimeException("Failed to retrieve training: " + e.getMessage(), e);
        }
    }

    /**
     * Save a training (create or update)
     */
    public Training saveTraining(Training training) {
        if (training == null) {
            throw new IllegalArgumentException("Training cannot be null");
        }
        
        // Validation
        if (training.getTitle() == null || training.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Training title is required");
        }
        
        if (training.getDescription() == null || training.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Training description is required");
        }
        
        // Cost is optional, but if provided, must be non-negative
        if (training.getCost() != null && training.getCost() < 0) {
            throw new IllegalArgumentException("Training cost cannot be negative");
        }
        
        if (training.getDuration() <= 0) {
            throw new IllegalArgumentException("Training duration must be positive");
        }
        
        if (training.getLevel() == null) {
            throw new IllegalArgumentException("Training level is required");
        }
        
        if (training.getCategory() == null) {
            throw new IllegalArgumentException("Training category is required");
        }
        
        try {
            return repository.save(training);
        } catch (Exception e) {
            logger.error("Error saving training", e);
            throw new RuntimeException("Failed to save training: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a training
     */
    public boolean deleteTraining(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Invalid training ID");
        }
        
        try {
            boolean deleted = repository.delete(id);
            if (deleted) {
                logger.info("Training deleted successfully: id={}", id);
            } else {
                logger.warn("Training not found for deletion: id={}", id);
            }
            return deleted;
        } catch (Exception e) {
            logger.error("Error deleting training id: {}", id, e);
            throw new RuntimeException("Failed to delete training: " + e.getMessage(), e);
        }
    }

    // ==================== Search Operations ====================

    /**
     * Search trainings by keyword (searches in title and description)
     */
    public List<Training> searchTrainings(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllTrainings();
        }
        
        try {
            return repository.searchByKeyword(keyword.trim());
        } catch (Exception e) {
            logger.error("Error searching trainings with keyword: {}", keyword, e);
            throw new RuntimeException("Failed to search trainings: " + e.getMessage(), e);
        }
    }

    /**
     * Get trainings by category
     */
    public List<Training> getTrainingsByCategory(TrainingCategory category) {
        if (category == null) {
            return getAllTrainings();
        }
        
        try {
            return repository.findByCategory(category);
        } catch (Exception e) {
            logger.error("Error getting trainings by category: {}", category, e);
            throw new RuntimeException("Failed to retrieve trainings by category: " + e.getMessage(), e);
        }
    }

    /**
     * Search trainings by category and keyword
     */
    public List<Training> searchTrainingsByCategoryAndKeyword(TrainingCategory category, String keyword) {
        if (category == null) {
            return searchTrainings(keyword);
        }
        
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return repository.findByCategory(category);
            } else {
                return repository.findByCategoryAndKeyword(category, keyword.trim());
            }
        } catch (Exception e) {
            logger.error("Error searching trainings by category and keyword: category={}, keyword={}", 
                        category, keyword, e);
            throw new RuntimeException("Failed to search trainings: " + e.getMessage(), e);
        }
    }

    /**
     * Check if training exists
     */
    public boolean trainingExists(int id) {
        try {
            return repository.existsById(id);
        } catch (Exception e) {
            logger.error("Error checking training existence: id={}", id, e);
            return false;
        }
    }
}
