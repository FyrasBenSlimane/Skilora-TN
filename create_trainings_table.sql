-- =============================================
-- SKILORA - TRAININGS TABLE
-- Creates the trainings table for the CRUD module
-- =============================================

USE skilora;

-- Create trainings table
CREATE TABLE IF NOT EXISTS trainings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    cost DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    duration INT NOT NULL COMMENT 'Duration in hours',
    level VARCHAR(20) NOT NULL COMMENT 'BEGINNER, INTERMEDIATE, ADVANCED',
    category VARCHAR(50) NOT NULL COMMENT 'DEVELOPMENT, DESIGN, MARKETING, DATA_SCIENCE, LANGUAGES, SOFT_SKILLS',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_level (level),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Verify table was created
SELECT 'Trainings table created successfully!' AS status;
SHOW TABLES LIKE 'trainings';
DESCRIBE trainings;
