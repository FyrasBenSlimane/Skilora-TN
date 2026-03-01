-- =============================================================================
-- SKILORA DATABASE SCHEMA
-- Unified script including User Management, Job Management, and Formation modules.
-- =============================================================================

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

-- 1. Create Database
CREATE DATABASE IF NOT EXISTS `skilora` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `skilora`;

-- Disable foreign key checks for clean table creation
SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------------------
-- 2. Core User Management Tables
-- --------------------------------------------------------

-- Table: users
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(20) NOT NULL,
  `full_name` varchar(100) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `photo_url` text DEFAULT NULL,
  `is_verified` tinyint(1) DEFAULT 0,
  `is_active` tinyint(1) DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Table: profiles
DROP TABLE IF EXISTS `profiles`;
CREATE TABLE `profiles` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `first_name` varchar(100) DEFAULT NULL,
  `last_name` varchar(100) DEFAULT NULL,
  `phone` varchar(50) DEFAULT NULL,
  `photo_url` text DEFAULT NULL,
  `cv_url` text DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `birth_date` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_profiles_user_id` (`user_id`),
  CONSTRAINT `profiles_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Table: skills
DROP TABLE IF EXISTS `skills`;
CREATE TABLE `skills` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `profile_id` int(11) NOT NULL,
  `skill_name` varchar(100) DEFAULT NULL,
  `proficiency_level` varchar(50) DEFAULT NULL,
  `years_experience` int(11) DEFAULT NULL,
  `verified` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_skills_profile_id` (`profile_id`),
  CONSTRAINT `skills_ibfk_1` FOREIGN KEY (`profile_id`) REFERENCES `profiles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Table: experiences
DROP TABLE IF EXISTS `experiences`;
CREATE TABLE `experiences` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `profile_id` int(11) NOT NULL,
  `company` varchar(100) DEFAULT NULL,
  `position` varchar(100) DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `description` text DEFAULT NULL,
  `current_job` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_experiences_profile_id` (`profile_id`),
  CONSTRAINT `experiences_ibfk_1` FOREIGN KEY (`profile_id`) REFERENCES `profiles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Table: biometric_data
DROP TABLE IF EXISTS `biometric_data`;
CREATE TABLE `biometric_data` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `face_encoding` blob DEFAULT NULL,
  `last_login` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  CONSTRAINT `biometric_data_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Table: login_attempts
DROP TABLE IF EXISTS `login_attempts`;
CREATE TABLE `login_attempts` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `attempted_at` datetime DEFAULT current_timestamp(),
  `success` tinyint(1) DEFAULT 0,
  `ip_address` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_login_attempts_username` (`username`,`attempted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Table: password_reset_tokens
DROP TABLE IF EXISTS `password_reset_tokens`;
CREATE TABLE `password_reset_tokens` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `otp_code` varchar(6) NOT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  `expires_at` datetime NOT NULL,
  `used` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_password_reset_user` (`user_id`,`used`),
  CONSTRAINT `password_reset_tokens_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Table: user_preferences
DROP TABLE IF EXISTS `user_preferences`;
CREATE TABLE `user_preferences` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `pref_key` varchar(100) NOT NULL,
  `pref_value` varchar(500) DEFAULT NULL,
  `updated_at` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_pref` (`user_id`,`pref_key`),
  CONSTRAINT `user_preferences_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- 3. Job Management Tables
-- --------------------------------------------------------

-- Table: companies
DROP TABLE IF EXISTS `companies`;
CREATE TABLE `companies` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `owner_id` int(11) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `country` varchar(50) DEFAULT NULL,
  `industry` varchar(100) DEFAULT NULL,
  `website` varchar(255) DEFAULT NULL,
  `logo_url` text DEFAULT NULL,
  `is_verified` tinyint(1) DEFAULT 0,
  `size` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_companies_owner` (`owner_id`),
  CONSTRAINT `companies_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Table: job_offers
DROP TABLE IF EXISTS `job_offers`;
CREATE TABLE `job_offers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `company_id` int(11) NOT NULL,
  `title` varchar(100) NOT NULL,
  `description` text DEFAULT NULL,
  `requirements` text DEFAULT NULL,
  `min_salary` decimal(10,2) DEFAULT NULL,
  `max_salary` decimal(10,2) DEFAULT NULL,
  `currency` varchar(10) DEFAULT 'EUR',
  `location` varchar(100) DEFAULT NULL,
  `work_type` varchar(50) DEFAULT NULL,
  `posted_date` datetime DEFAULT current_timestamp(),
  `deadline` date DEFAULT NULL,
  `status` varchar(20) DEFAULT 'OPEN',
  PRIMARY KEY (`id`),
  KEY `idx_job_offers_company_id` (`company_id`),
  KEY `idx_job_offers_status` (`status`),
  CONSTRAINT `job_offers_ibfk_1` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Table: applications
DROP TABLE IF EXISTS `applications`;
CREATE TABLE `applications` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `job_offer_id` int(11) NOT NULL,
  `candidate_profile_id` int(11) NOT NULL,
  `status` varchar(30) DEFAULT 'PENDING',
  `applied_date` datetime DEFAULT current_timestamp(),
  `cover_letter` text DEFAULT NULL,
  `custom_cv_url` text DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_applications_job_offer_id` (`job_offer_id`),
  KEY `idx_applications_candidate_id` (`candidate_profile_id`),
  KEY `idx_applications_status` (`status`),
  CONSTRAINT `applications_ibfk_1` FOREIGN KEY (`job_offer_id`) REFERENCES `job_offers` (`id`) ON DELETE CASCADE,
  CONSTRAINT `applications_ibfk_2` FOREIGN KEY (`candidate_profile_id`) REFERENCES `profiles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- 4. Formation and Certification Tables
-- --------------------------------------------------------

-- Table: trainings
DROP TABLE IF EXISTS `trainings`;
CREATE TABLE `trainings` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `title` varchar(255) NOT NULL,
    `description` text NOT NULL,
    `cost` decimal(10, 2) DEFAULT NULL,
    `duration` int(11) NOT NULL COMMENT 'Duration in hours',
    `level` varchar(20) NOT NULL COMMENT 'BEGINNER, INTERMEDIATE, ADVANCED',
    `category` varchar(50) NOT NULL COMMENT 'DEVELOPMENT, DESIGN, MARKETING, DATA_SCIENCE, LANGUAGES, SOFT_SKILLS, MANAGEMENT',
    `lesson_count` int(11) NOT NULL DEFAULT 0,
    `created_at` timestamp DEFAULT current_timestamp(),
    `updated_at` timestamp DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    PRIMARY KEY (`id`),
    INDEX `idx_category` (`category`),
    INDEX `idx_level` (`level`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: training_lessons
DROP TABLE IF EXISTS `training_lessons`;
CREATE TABLE `training_lessons` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `training_id` int(11) NOT NULL,
    `title` varchar(255) NOT NULL,
    `description` text,
    `content` longtext,
    `order_index` int(11) NOT NULL DEFAULT 0,
    `duration_minutes` int(11) DEFAULT 0,
    `created_at` timestamp DEFAULT current_timestamp(),
    `updated_at` timestamp DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    PRIMARY KEY (`id`),
    KEY `idx_training_id` (`training_id`),
    KEY `idx_order_index` (`order_index`),
    CONSTRAINT `training_lessons_ibfk_1` FOREIGN KEY (`training_id`) REFERENCES `trainings` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: training_materials
DROP TABLE IF EXISTS `training_materials`;
CREATE TABLE `training_materials` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `training_id` int(11) NOT NULL,
    `lesson_id` int(11) DEFAULT NULL,
    `name` varchar(255) NOT NULL,
    `description` text,
    `file_url` varchar(500) NOT NULL,
    `file_type` varchar(50),
    `file_size_bytes` bigint(20) DEFAULT 0,
    `created_at` timestamp DEFAULT current_timestamp(),
    PRIMARY KEY (`id`),
    KEY `idx_training_id` (`training_id`),
    KEY `idx_lesson_id` (`lesson_id`),
    CONSTRAINT `training_materials_ibfk_1` FOREIGN KEY (`training_id`) REFERENCES `trainings` (`id`) ON DELETE CASCADE,
    CONSTRAINT `training_materials_ibfk_2` FOREIGN KEY (`lesson_id`) REFERENCES `training_lessons` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: training_enrollments
DROP TABLE IF EXISTS `training_enrollments`;
CREATE TABLE `training_enrollments` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `user_id` int(11) NOT NULL,
    `training_id` int(11) NOT NULL,
    `enrolled_at` timestamp DEFAULT current_timestamp(),
    `last_accessed_at` timestamp DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    `completed` tinyint(1) DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_user_training` (`user_id`, `training_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_training_id` (`training_id`),
    CONSTRAINT `training_enrollments_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `training_enrollments_ibfk_2` FOREIGN KEY (`training_id`) REFERENCES `trainings` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: lesson_progress
DROP TABLE IF EXISTS `lesson_progress`;
CREATE TABLE `lesson_progress` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `enrollment_id` int(11) NOT NULL,
    `lesson_id` int(11) NOT NULL,
    `completed` tinyint(1) DEFAULT 0,
    `progress_percentage` int(11) DEFAULT 0,
    `started_at` timestamp DEFAULT current_timestamp(),
    `completed_at` timestamp NULL DEFAULT NULL,
    `last_accessed_at` timestamp DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_enrollment_lesson` (`enrollment_id`, `lesson_id`),
    KEY `idx_enrollment_id` (`enrollment_id`),
    KEY `idx_lesson_id` (`lesson_id`),
    CONSTRAINT `lesson_progress_ibfk_1` FOREIGN KEY (`enrollment_id`) REFERENCES `training_enrollments` (`id`) ON DELETE CASCADE,
    CONSTRAINT `lesson_progress_ibfk_2` FOREIGN KEY (`lesson_id`) REFERENCES `training_lessons` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: certificates
DROP TABLE IF EXISTS `certificates`;
CREATE TABLE `certificates` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `user_id` int(11) NOT NULL,
    `training_id` int(11) NOT NULL,
    `certificate_number` varchar(100) NOT NULL UNIQUE,
    `verification_token` varchar(100) NOT NULL UNIQUE,
    `issued_at` timestamp DEFAULT current_timestamp(),
    `completed_at` timestamp NOT NULL DEFAULT current_timestamp(),
    `pdf_path` varchar(500),
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_user_training_cert` (`user_id`, `training_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_training_id` (`training_id`),
    CONSTRAINT `certificates_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `certificates_ibfk_2` FOREIGN KEY (`training_id`) REFERENCES `trainings` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: training_ratings
DROP TABLE IF EXISTS `training_ratings`;
CREATE TABLE `training_ratings` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `user_id` int(11) NOT NULL,
    `training_id` int(11) NOT NULL,
    `is_liked` tinyint(1) DEFAULT NULL,
    `star_rating` int(11) NOT NULL,
    `created_at` timestamp DEFAULT current_timestamp(),
    `updated_at` timestamp DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    PRIMARY KEY (`id`),
    UNIQUE KEY `unique_user_training_rating` (`user_id`, `training_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_training_id` (`training_id`),
    KEY `idx_star_rating` (`star_rating`),
    KEY `idx_is_liked` (`is_liked`),
    CONSTRAINT `training_ratings_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `training_ratings_ibfk_2` FOREIGN KEY (`training_id`) REFERENCES `trainings` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- Initial Data Seeding
-- -----------------------------------------------------------------------------
INSERT INTO `users` (`id`, `username`, `email`, `password`, `role`, `full_name`, `is_verified`, `is_active`) VALUES
(1, 'admin', 'admin@skilora.com', 'admin123', 'ADMIN', 'System Administrator', 1, 1),
(2, 'user', 'user@skilora.com', 'user123', 'USER', 'John Doe', 1, 1),
(3, 'employer', 'employer@skilora.com', 'emp123', 'EMPLOYER', 'Tech Solutions Inc.', 1, 1);

INSERT INTO `profiles` (`user_id`, `first_name`, `last_name`) VALUES (1, 'System', 'Administrator'), (2, 'John', 'Doe');

INSERT INTO `companies` (`id`, `owner_id`, `name`, `country`, `industry`) VALUES
(1, NULL, 'Skilora Feed', 'Global', 'Aggregator'),
(2, 3, 'Tech Solutions Inc.', 'Tunisia', 'Technology');

COMMIT;
