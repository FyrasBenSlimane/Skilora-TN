-- =============================================
-- SKILORA - TRAINING RATINGS TABLE
-- Creates the training_ratings table for the rating system
-- =============================================

USE skilora;

-- Create training_ratings table
-- Note: CHECK constraint removed for MySQL/MariaDB compatibility
-- Validation is enforced at application level in TrainingRatingService
CREATE TABLE IF NOT EXISTS training_ratings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    training_id INT NOT NULL,
    is_liked TINYINT(1) DEFAULT NULL,
    star_rating INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Constraints
    UNIQUE KEY unique_user_training_rating (user_id, training_id),
    INDEX idx_user_id (user_id),
    INDEX idx_training_id (training_id),
    INDEX idx_star_rating (star_rating),
    INDEX idx_is_liked (is_liked),
    
    -- Foreign keys
    CONSTRAINT training_ratings_ibfk_1 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT training_ratings_ibfk_2 FOREIGN KEY (training_id) REFERENCES trainings(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Verify table was created
SELECT 'Training ratings table created successfully!' AS status;
SHOW TABLES LIKE 'training_ratings';
DESCRIBE training_ratings;
