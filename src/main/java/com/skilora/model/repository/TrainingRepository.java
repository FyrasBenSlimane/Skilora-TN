package com.skilora.model.repository;

import com.skilora.model.entity.formation.Training;
import com.skilora.model.enums.TrainingCategory;

import java.util.List;
import java.util.Optional;

/**
 * TrainingRepository Interface
 * 
 * Defines data access operations for Training entities.
 * Follows Repository pattern - abstraction for data persistence.
 */
public interface TrainingRepository {
    
    /**
     * Find a training by ID
     */
    Optional<Training> findById(int id);
    
    /**
     * Find all trainings
     */
    List<Training> findAll();
    
    /**
     * Save a training (insert or update)
     * Returns the saved training with generated ID if new
     */
    Training save(Training training);
    
    /**
     * Update an existing training
     */
    boolean update(Training training);
    
    /**
     * Delete a training by ID
     */
    boolean delete(int id);
    
    /**
     * Check if a training exists by ID
     */
    boolean existsById(int id);
    
    /**
     * Search trainings by keyword (searches in title and description)
     */
    List<Training> searchByKeyword(String keyword);
    
    /**
     * Find trainings by category
     */
    List<Training> findByCategory(TrainingCategory category);
    
    /**
     * Find trainings by category and keyword
     */
    List<Training> findByCategoryAndKeyword(TrainingCategory category, String keyword);
}
