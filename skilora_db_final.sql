-- ========================================================
-- SKILORA TUNISIA - COMPLETE DATABASE SETUP SCRIPT
-- Merged: Original tables + Finance module tables
-- ========================================================
-- Generation Time: Feb 11, 2026
-- Database: skilora

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `skilora`
--
CREATE DATABASE IF NOT EXISTS `skilora`;
USE `skilora`;

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE IF NOT EXISTS `users` (
  `id` int(11) NOT NULL,
  `username` varchar(50) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(20) NOT NULL,
  `full_name` varchar(100) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `photo_url` text DEFAULT NULL,
  `is_verified` tinyint(1) DEFAULT 0,
  `is_active` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `username`, `email`, `password`, `role`, `full_name`, `created_at`, `photo_url`, `is_verified`, `is_active`) VALUES
(1, 'admin', NULL, 'admin123', 'ADMIN', 'System Administrator', '2026-01-29 20:03:26', 'file:///C:/Users/NYX-PC/.skilora/images/user_1_1769983532814.jpg', 0, 1),
(2, 'user', NULL, 'user123', 'USER', 'John Doe', '2026-01-29 20:03:26', NULL, 0, 1),
(3, 'employer', NULL, 'emp123', 'EMPLOYER', 'Tech Solutions Inc.', '2026-01-29 20:03:26', NULL, 0, 1),
(4, 'zzz', NULL, '12345678', 'USER', 'zzz', '2026-02-04 22:23:58', NULL, 0, 1),
(5, 'nour', NULL, '$2a$12$lAjcQWtAyL5jHeaRB1hrOOCn4OZFAWOw267hHCped1gX93tpDkqVa', 'USER', 'nour ', '2026-02-07 15:32:30', NULL, 0, 1)
ON DUPLICATE KEY UPDATE username=username;

-- --------------------------------------------------------

--
-- Table structure for table `profiles`
--

CREATE TABLE IF NOT EXISTS `profiles` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `first_name` varchar(100) DEFAULT NULL,
  `last_name` varchar(100) DEFAULT NULL,
  `phone` varchar(50) DEFAULT NULL,
  `photo_url` text DEFAULT NULL,
  `cv_url` text DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `birth_date` date DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `profiles`
--

INSERT INTO `profiles` (`id`, `user_id`, `first_name`, `last_name`, `phone`, `photo_url`, `cv_url`, `location`, `birth_date`) VALUES
(1, 1, 'System', 'Administrator', '', 'file:///C:/Users/NYX-PC/.skilora/images/user_1_1769983532814.jpg', '', '', NULL)
ON DUPLICATE KEY UPDATE id=id;

-- --------------------------------------------------------

--
-- Table structure for table `companies`
--

CREATE TABLE IF NOT EXISTS `companies` (
  `id` int(11) NOT NULL,
  `owner_id` int(11) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `country` varchar(50) DEFAULT NULL,
  `industry` varchar(100) DEFAULT NULL,
  `website` varchar(255) DEFAULT NULL,
  `logo_url` text DEFAULT NULL,
  `is_verified` tinyint(1) DEFAULT 0,
  `size` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `companies`
--

INSERT INTO `companies` (`id`, `owner_id`, `name`, `country`, `industry`, `website`, `logo_url`, `is_verified`, `size`) VALUES
(1, NULL, 'Skilora Feed', 'Global', 'Aggregator', NULL, NULL, 0, NULL),
(2, 3, 'Tech Solutions Inc.', 'Tunisia', NULL, NULL, NULL, 0, NULL)
ON DUPLICATE KEY UPDATE id=id;

-- --------------------------------------------------------

--
-- Table structure for table `job_offers`
--

CREATE TABLE IF NOT EXISTS `job_offers` (
  `id` int(11) NOT NULL,
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
  `status` varchar(20) DEFAULT 'OPEN'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `job_offers`
--

INSERT INTO `job_offers` (`id`, `company_id`, `title`, `description`, `requirements`, `min_salary`, `max_salary`, `currency`, `location`, `work_type`, `posted_date`, `deadline`, `status`) VALUES
(1, 1, 'Responsable marketing digital', 'Poste de responsable marketing digital', NULL, NULL, NULL, 'EUR', 'BEN AROUS', 'ONSITE', '2026-02-07 17:26:09', NULL, 'OPEN'),
(2, 1, 'Ingenieur genie civil', 'Poste ingenieur en genie civil', NULL, NULL, NULL, 'EUR', 'NABEUL', 'ONSITE', '2026-02-07 17:26:09', NULL, 'OPEN'),
(3, 1, 'Chef du personnel', 'Poste chef du personnel', NULL, NULL, NULL, 'EUR', 'TUNIS', 'ONSITE', '2026-02-07 17:26:09', NULL, 'OPEN'),
(4, 1, 'Conseiller clientele a distance', 'Poste conseiller clientele', NULL, NULL, NULL, 'EUR', 'SOUSSE', 'ONSITE', '2026-02-07 17:26:09', NULL, 'OPEN'),
(5, 1, 'Assistant administratif', 'Poste assistant administratif', NULL, NULL, NULL, 'EUR', 'SFAX', 'ONSITE', '2026-02-07 17:26:09', NULL, 'OPEN')
ON DUPLICATE KEY UPDATE id=id;

-- --------------------------------------------------------

--
-- Table structure for table `applications`
--

CREATE TABLE IF NOT EXISTS `applications` (
  `id` int(11) NOT NULL,
  `job_offer_id` int(11) NOT NULL,
  `candidate_profile_id` int(11) NOT NULL,
  `status` varchar(30) DEFAULT 'PENDING',
  `applied_date` datetime DEFAULT current_timestamp(),
  `cover_letter` text DEFAULT NULL,
  `custom_cv_url` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `experiences`
--

CREATE TABLE IF NOT EXISTS `experiences` (
  `id` int(11) NOT NULL,
  `profile_id` int(11) NOT NULL,
  `company` varchar(100) DEFAULT NULL,
  `position` varchar(100) DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `description` text DEFAULT NULL,
  `current_job` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `skills`
--

CREATE TABLE IF NOT EXISTS `skills` (
  `id` int(11) NOT NULL,
  `profile_id` int(11) NOT NULL,
  `skill_name` varchar(100) DEFAULT NULL,
  `proficiency_level` varchar(50) DEFAULT NULL,
  `years_experience` int(11) DEFAULT NULL,
  `verified` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `biometric_data`
--

CREATE TABLE IF NOT EXISTS `biometric_data` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `face_encoding` blob DEFAULT NULL,
  `last_login` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `biometric_data`
--

INSERT INTO `biometric_data` (`id`, `user_id`, `face_encoding`, `last_login`) VALUES
(1, 5, 0x7b7d, '2026-02-07 16:37:15')
ON DUPLICATE KEY UPDATE id=id;

-- --------------------------------------------------------

--
-- Table structure for table `login_attempts`
--

CREATE TABLE IF NOT EXISTS `login_attempts` (
  `id` int(11) NOT NULL,
  `username` varchar(50) NOT NULL,
  `attempted_at` datetime DEFAULT current_timestamp(),
  `success` tinyint(1) DEFAULT 0,
  `ip_address` varchar(45) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `login_attempts`
--

INSERT INTO `login_attempts` (`id`, `username`, `attempted_at`, `success`, `ip_address`) VALUES
(1, 'admin', '2026-02-09 02:59:59', 1, NULL),
(2, 'admin', '2026-02-09 03:24:22', 1, NULL),
(3, 'nour', '2026-02-09 03:24:32', 1, NULL),
(4, 'admin', '2026-02-09 13:54:39', 1, NULL),
(5, 'employer', '2026-02-09 18:35:34', 0, NULL),
(6, 'employer', '2026-02-09 18:35:37', 1, NULL),
(7, 'user', '2026-02-09 18:36:35', 1, NULL)
ON DUPLICATE KEY UPDATE id=id;

-- --------------------------------------------------------

--
-- Table structure for table `password_reset_tokens`
--

CREATE TABLE IF NOT EXISTS `password_reset_tokens` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `otp_code` varchar(6) NOT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  `expires_at` datetime NOT NULL,
  `used` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `user_preferences`
--

CREATE TABLE IF NOT EXISTS `user_preferences` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `pref_key` varchar(100) NOT NULL,
  `pref_value` varchar(500) DEFAULT NULL,
  `updated_at` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `user_preferences`
--

INSERT INTO `user_preferences` (`id`, `user_id`, `pref_key`, `pref_value`, `updated_at`) VALUES
(1, 1, 'dark_mode', 'false', '2026-02-09 18:35:03'),
(2, 1, 'animations', 'true', '2026-02-09 18:35:03'),
(3, 1, 'notifications', 'true', '2026-02-09 18:35:03'),
(4, 1, 'sound_notifications', 'false', '2026-02-09 18:35:03'),
(5, 1, 'language', 'English', '2026-02-09 18:35:13')
ON DUPLICATE KEY UPDATE id=id;

-- --------------------------------------------------------
-- ========================================================
-- FINANCE MODULE TABLES (NEW)
-- ========================================================

--
-- Table structure for table `contracts`
--

CREATE TABLE IF NOT EXISTS `contracts` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `company_id` int(11) NOT NULL,
  `type` varchar(50) DEFAULT NULL,
  `position` varchar(100) DEFAULT NULL,
  `salary` double DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `bank_accounts`
--

CREATE TABLE IF NOT EXISTS `bank_accounts` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `bank_name` varchar(100) DEFAULT NULL,
  `iban` varchar(50) DEFAULT NULL,
  `swift` varchar(20) DEFAULT NULL,
  `currency` varchar(10) DEFAULT NULL,
  `is_primary` tinyint(1) DEFAULT 0,
  `is_verified` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `bonuses`
--

CREATE TABLE IF NOT EXISTS `bonuses` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `amount` double DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `date_awarded` date DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `payslips`
--

CREATE TABLE IF NOT EXISTS `payslips` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `month` int(11) DEFAULT NULL,
  `year` int(11) DEFAULT NULL,
  `base_salary` double DEFAULT NULL,
  `overtime_hours` double DEFAULT NULL,
  `overtime_total` double DEFAULT NULL,
  `bonuses` double DEFAULT NULL,
  `other_deductions` double DEFAULT NULL,
  `currency` varchar(10) DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- ========================================================
-- INDEXES AND CONSTRAINTS
-- ========================================================

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `username` (`username`),
  ADD KEY `idx_users_email` (`email`);

--
-- Indexes for table `profiles`
--
ALTER TABLE `profiles`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_profiles_user_id` (`user_id`);

--
-- Indexes for table `companies`
--
ALTER TABLE `companies`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_companies_owner` (`owner_id`);

--
-- Indexes for table `job_offers`
--
ALTER TABLE `job_offers`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_job_offers_company_id` (`company_id`),
  ADD KEY `idx_job_offers_status` (`status`);

--
-- Indexes for table `applications`
--
ALTER TABLE `applications`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_applications_job_offer_id` (`job_offer_id`),
  ADD KEY `idx_applications_candidate_id` (`candidate_profile_id`),
  ADD KEY `idx_applications_status` (`status`);

--
-- Indexes for table `experiences`
--
ALTER TABLE `experiences`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_experiences_profile_id` (`profile_id`);

--
-- Indexes for table `skills`
--
ALTER TABLE `skills`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_skills_profile_id` (`profile_id`);

--
-- Indexes for table `biometric_data`
--
ALTER TABLE `biometric_data`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `user_id` (`user_id`);

--
-- Indexes for table `login_attempts`
--
ALTER TABLE `login_attempts`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_login_attempts_username` (`username`,`attempted_at`);

--
-- Indexes for table `password_reset_tokens`
--
ALTER TABLE `password_reset_tokens`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_password_reset_user` (`user_id`,`used`);

--
-- Indexes for table `user_preferences`
--
ALTER TABLE `user_preferences`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_user_pref` (`user_id`,`pref_key`);

--
-- Indexes for table `contracts` (NEW)
--
ALTER TABLE `contracts`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_contracts_user_id` (`user_id`),
  ADD KEY `idx_contracts_company_id` (`company_id`);

--
-- Indexes for table `bank_accounts` (NEW)
--
ALTER TABLE `bank_accounts`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_bank_accounts_user_id` (`user_id`);

--
-- Indexes for table `bonuses` (NEW)
--
ALTER TABLE `bonuses`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_bonuses_user_id` (`user_id`);

--
-- Indexes for table `payslips` (NEW)
--
ALTER TABLE `payslips`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_payslips_user_id` (`user_id`);

-- --------------------------------------------------------
-- ========================================================
-- AUTO_INCREMENT
-- ========================================================

ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

ALTER TABLE `profiles`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

ALTER TABLE `companies`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

ALTER TABLE `job_offers`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

ALTER TABLE `applications`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `experiences`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `skills`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `biometric_data`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

ALTER TABLE `login_attempts`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

ALTER TABLE `password_reset_tokens`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `user_preferences`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

ALTER TABLE `contracts`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `bank_accounts`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `bonuses`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `payslips`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

-- --------------------------------------------------------
-- ========================================================
-- FOREIGN KEY CONSTRAINTS
-- ========================================================

--
-- Constraints for table `profiles`
--
ALTER TABLE `profiles`
  ADD CONSTRAINT `profiles_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `companies`
--
ALTER TABLE `companies`
  ADD CONSTRAINT `companies_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `job_offers`
--
ALTER TABLE `job_offers`
  ADD CONSTRAINT `job_offers_ibfk_1` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `applications`
--
ALTER TABLE `applications`
  ADD CONSTRAINT `applications_ibfk_1` FOREIGN KEY (`job_offer_id`) REFERENCES `job_offers` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `applications_ibfk_2` FOREIGN KEY (`candidate_profile_id`) REFERENCES `profiles` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `experiences`
--
ALTER TABLE `experiences`
  ADD CONSTRAINT `experiences_ibfk_1` FOREIGN KEY (`profile_id`) REFERENCES `profiles` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `skills`
--
ALTER TABLE `skills`
  ADD CONSTRAINT `skills_ibfk_1` FOREIGN KEY (`profile_id`) REFERENCES `profiles` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `biometric_data`
--
ALTER TABLE `biometric_data`
  ADD CONSTRAINT `biometric_data_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `password_reset_tokens`
--
ALTER TABLE `password_reset_tokens`
  ADD CONSTRAINT `password_reset_tokens_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `user_preferences`
--
ALTER TABLE `user_preferences`
  ADD CONSTRAINT `user_preferences_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `contracts` (NEW)
--
ALTER TABLE `contracts`
  ADD CONSTRAINT `contracts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `contracts_ibfk_2` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `bank_accounts` (NEW)
--
ALTER TABLE `bank_accounts`
  ADD CONSTRAINT `bank_accounts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `bonuses` (NEW)
--
ALTER TABLE `bonuses`
  ADD CONSTRAINT `bonuses_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `payslips` (NEW)
--
ALTER TABLE `payslips`
  ADD CONSTRAINT `payslips_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
