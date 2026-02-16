-- phpMyAdmin SQL Dump
-- version 5.2.0
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Feb 09, 2026 at 09:08 PM
-- Server version: 10.4.27-MariaDB
-- PHP Version: 8.2.0

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

-- --------------------------------------------------------

--
-- Table structure for table `achievements`
--

CREATE TABLE `achievements` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `badge_type` varchar(50) NOT NULL,
  `title` varchar(100) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `icon_url` text DEFAULT NULL,
  `earned_date` datetime DEFAULT current_timestamp(),
  `rarity` varchar(20) DEFAULT 'COMMON',
  `points` int(11) DEFAULT 10
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `achievements`
--

INSERT INTO `achievements` (`id`, `user_id`, `badge_type`, `title`, `description`, `icon_url`, `earned_date`, `rarity`, `points`) VALUES
(1, 1, 'FIRST_POST', 'Première Publication', 'Vous avez partagé votre premier post!', NULL, '2026-02-09 20:54:35', 'COMMON', 10);

-- --------------------------------------------------------

--
-- Table structure for table `applications`
--

CREATE TABLE `applications` (
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
-- Table structure for table `auto_responses`
--

CREATE TABLE `auto_responses` (
  `id` int(11) NOT NULL,
  `trigger_keyword` varchar(100) NOT NULL,
  `response_text` text NOT NULL,
  `category` varchar(50) DEFAULT NULL,
  `language` varchar(5) DEFAULT 'fr',
  `is_active` tinyint(1) DEFAULT 1,
  `usage_count` int(11) DEFAULT 0,
  `created_date` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `auto_responses`
--

INSERT INTO `auto_responses` (`id`, `trigger_keyword`, `response_text`, `category`, `language`, `is_active`, `usage_count`, `created_date`) VALUES
(1, 'mot de passe', 'Pour réinitialiser votre mot de passe, allez dans Paramètres > Sécurité ou cliquez sur \"Mot de passe oublié\" sur la page de connexion.', 'account', 'fr', 1, 0, '2026-02-09 20:32:44'),
(2, 'inscription', 'Pour vous inscrire, cliquez sur le bouton \"S\'inscrire\" sur la page de connexion et remplissez le formulaire.', 'account', 'fr', 1, 0, '2026-02-09 20:32:44'),
(3, 'formation', 'Nous proposons des formations gratuites et payantes. Consultez la section Formations pour voir le catalogue complet.', 'formation', 'fr', 1, 0, '2026-02-09 20:32:44'),
(4, 'emploi', 'Pour trouver un emploi, consultez notre fil d\'actualités emploi ou la section Offres d\'emploi.', 'job', 'fr', 1, 0, '2026-02-09 20:32:44'),
(5, 'contact', 'Vous pouvez nous contacter via ce chat ou en créant un ticket de support. Notre équipe vous répondra dans les 24 heures.', 'general', 'fr', 1, 0, '2026-02-09 20:32:44');

-- --------------------------------------------------------

--
-- Table structure for table `bank_accounts`
--

CREATE TABLE `bank_accounts` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `bank_name` varchar(100) NOT NULL,
  `account_holder` varchar(150) NOT NULL,
  `iban` varchar(34) DEFAULT NULL,
  `swift_bic` varchar(11) DEFAULT NULL,
  `rib` varchar(24) DEFAULT NULL,
  `currency` varchar(10) DEFAULT 'TND',
  `is_primary` tinyint(1) DEFAULT 0,
  `created_date` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `biometric_data`
--

CREATE TABLE `biometric_data` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `face_encoding` blob DEFAULT NULL,
  `last_login` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `biometric_data`
--

INSERT INTO `biometric_data` (`id`, `user_id`, `face_encoding`, `last_login`) VALUES
(1, 5, 0x7b7d, '2026-02-07 16:37:15');

-- --------------------------------------------------------

--
-- Table structure for table `blog_articles`
--

CREATE TABLE `blog_articles` (
  `id` int(11) NOT NULL,
  `author_id` int(11) NOT NULL,
  `title` varchar(300) NOT NULL,
  `content` text NOT NULL,
  `summary` text DEFAULT NULL,
  `cover_image_url` text DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `tags` varchar(500) DEFAULT NULL,
  `views_count` int(11) DEFAULT 0,
  `likes_count` int(11) DEFAULT 0,
  `is_published` tinyint(1) DEFAULT 0,
  `published_date` datetime DEFAULT NULL,
  `created_date` datetime DEFAULT current_timestamp(),
  `updated_date` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `certificates`
--

CREATE TABLE `certificates` (
  `id` int(11) NOT NULL,
  `enrollment_id` int(11) NOT NULL,
  `certificate_number` varchar(50) NOT NULL,
  `issued_date` datetime DEFAULT current_timestamp(),
  `qr_code` text DEFAULT NULL,
  `hash_value` varchar(128) DEFAULT NULL,
  `pdf_url` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `chatbot_conversations`
--

CREATE TABLE `chatbot_conversations` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `started_date` datetime DEFAULT current_timestamp(),
  `ended_date` datetime DEFAULT NULL,
  `status` varchar(20) DEFAULT 'ACTIVE',
  `escalated_to_ticket_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `chatbot_messages`
--

CREATE TABLE `chatbot_messages` (
  `id` int(11) NOT NULL,
  `conversation_id` int(11) NOT NULL,
  `sender` varchar(10) NOT NULL,
  `message` text NOT NULL,
  `intent` varchar(100) DEFAULT NULL,
  `confidence` decimal(5,4) DEFAULT NULL,
  `created_date` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `community_groups`
--

CREATE TABLE `community_groups` (
  `id` int(11) NOT NULL,
  `name` varchar(150) NOT NULL,
  `description` text DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `cover_image_url` text DEFAULT NULL,
  `creator_id` int(11) NOT NULL,
  `member_count` int(11) DEFAULT 1,
  `is_public` tinyint(1) DEFAULT 1,
  `created_date` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `companies`
--

CREATE TABLE `companies` (
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
(2, 3, 'Tech Solutions Inc.', 'Tunisia', NULL, NULL, NULL, 0, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `connections`
--

CREATE TABLE `connections` (
  `id` int(11) NOT NULL,
  `user_id_1` int(11) NOT NULL,
  `user_id_2` int(11) NOT NULL,
  `status` varchar(20) DEFAULT 'PENDING',
  `connection_type` varchar(30) DEFAULT 'PROFESSIONAL',
  `created_date` datetime DEFAULT current_timestamp(),
  `last_interaction` datetime DEFAULT NULL,
  `strength_score` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `conversations`
--

CREATE TABLE `conversations` (
  `id` int(11) NOT NULL,
  `participant_1` int(11) NOT NULL,
  `participant_2` int(11) NOT NULL,
  `last_message_date` datetime DEFAULT NULL,
  `is_archived_1` tinyint(1) DEFAULT 0,
  `is_archived_2` tinyint(1) DEFAULT 0,
  `created_date` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `employment_contracts`
--

CREATE TABLE `employment_contracts` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `employer_id` int(11) DEFAULT NULL,
  `job_offer_id` int(11) DEFAULT NULL,
  `salary_base` decimal(12,2) NOT NULL,
  `currency` varchar(10) DEFAULT 'TND',
  `start_date` date NOT NULL,
  `end_date` date DEFAULT NULL,
  `contract_type` varchar(20) DEFAULT 'CDI',
  `status` varchar(20) DEFAULT 'DRAFT',
  `pdf_url` text DEFAULT NULL,
  `is_signed` tinyint(1) DEFAULT 0,
  `signed_date` datetime DEFAULT NULL,
  `created_date` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `enrollments`
--

CREATE TABLE `enrollments` (
  `id` int(11) NOT NULL,
  `formation_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `status` varchar(30) DEFAULT 'IN_PROGRESS',
  `progress` decimal(5,2) DEFAULT 0.00,
  `enrolled_date` datetime DEFAULT current_timestamp(),
  `completed_date` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `events`
--

CREATE TABLE `events` (
  `id` int(11) NOT NULL,
  `organizer_id` int(11) NOT NULL,
  `title` varchar(200) NOT NULL,
  `description` text DEFAULT NULL,
  `event_type` varchar(30) DEFAULT 'MEETUP',
  `location` varchar(255) DEFAULT NULL,
  `is_online` tinyint(1) DEFAULT 0,
  `online_link` text DEFAULT NULL,
  `start_date` datetime NOT NULL,
  `end_date` datetime DEFAULT NULL,
  `max_attendees` int(11) DEFAULT 0,
  `current_attendees` int(11) DEFAULT 0,
  `image_url` text DEFAULT NULL,
  `status` varchar(20) DEFAULT 'UPCOMING',
  `created_date` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `event_rsvps`
--

CREATE TABLE `event_rsvps` (
  `id` int(11) NOT NULL,
  `event_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `status` varchar(20) DEFAULT 'GOING',
  `rsvp_date` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `exchange_rates`
--

CREATE TABLE `exchange_rates` (
  `id` int(11) NOT NULL,
  `from_currency` varchar(10) NOT NULL,
  `to_currency` varchar(10) NOT NULL,
  `rate` decimal(12,6) NOT NULL,
  `rate_date` date NOT NULL,
  `source` varchar(50) DEFAULT 'BCT',
  `last_updated` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `exchange_rates`
--

INSERT INTO `exchange_rates` (`id`, `from_currency`, `to_currency`, `rate`, `rate_date`, `source`, `last_updated`) VALUES
(1, 'EUR', 'TND', '3.400000', '2026-02-09', 'BCT', '2026-02-09 20:32:45'),
(2, 'USD', 'TND', '3.150000', '2026-02-09', 'BCT', '2026-02-09 20:32:45'),
(3, 'GBP', 'TND', '3.950000', '2026-02-09', 'BCT', '2026-02-09 20:32:45'),
(4, 'TND', 'EUR', '0.294100', '2026-02-09', 'BCT', '2026-02-09 20:32:45'),
(5, 'TND', 'USD', '0.317500', '2026-02-09', 'BCT', '2026-02-09 20:32:45');

-- --------------------------------------------------------

--
-- Table structure for table `experiences`
--

CREATE TABLE `experiences` (
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
-- Table structure for table `faq_articles`
--

CREATE TABLE `faq_articles` (
  `id` int(11) NOT NULL,
  `category` varchar(50) NOT NULL,
  `question` text NOT NULL,
  `answer` text NOT NULL,
  `language` varchar(5) DEFAULT 'fr',
  `helpful_count` int(11) DEFAULT 0,
  `view_count` int(11) DEFAULT 0,
  `is_published` tinyint(1) DEFAULT 1,
  `created_date` datetime DEFAULT current_timestamp(),
  `updated_date` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `faq_articles`
--

INSERT INTO `faq_articles` (`id`, `category`, `question`, `answer`, `language`, `helpful_count`, `view_count`, `is_published`, `created_date`, `updated_date`) VALUES
(1, 'account', 'Comment créer un compte ?', 'Cliquez sur \"S\'inscrire\" sur la page de connexion et remplissez le formulaire avec vos informations personnelles.', 'fr', 0, 0, 1, '2026-02-09 20:32:44', '2026-02-09 20:32:44'),
(2, 'account', 'Comment réinitialiser mon mot de passe ?', 'Cliquez sur \"Mot de passe oublié\" sur la page de connexion et suivez les instructions envoyées par email.', 'fr', 0, 0, 1, '2026-02-09 20:32:44', '2026-02-09 20:32:44'),
(3, 'job', 'Comment postuler à une offre ?', 'Allez dans la section \"Offres d\'emploi\", trouvez l\'offre qui vous intéresse et cliquez sur \"Postuler\".', 'fr', 0, 0, 1, '2026-02-09 20:32:44', '2026-02-09 20:32:44'),
(4, 'formation', 'Les formations sont-elles gratuites ?', 'Certaines formations sont gratuites, d\'autres sont payantes. Consultez la description de chaque formation pour plus de détails.', 'fr', 0, 0, 1, '2026-02-09 20:32:44', '2026-02-09 20:32:44'),
(5, 'technical', 'Comment contacter le support ?', 'Vous pouvez créer un ticket de support depuis cette page ou utiliser l\'assistant virtuel pour obtenir une aide immédiate.', 'fr', 0, 0, 1, '2026-02-09 20:32:44', '2026-02-09 20:32:44');

-- --------------------------------------------------------

--
-- Table structure for table `formations`
--

CREATE TABLE `formations` (
  `id` int(11) NOT NULL,
  `title` varchar(200) NOT NULL,
  `description` text DEFAULT NULL,
  `category` varchar(50) NOT NULL,
  `duration_hours` int(11) DEFAULT 0,
  `cost` decimal(10,2) DEFAULT 0.00,
  `currency` varchar(10) DEFAULT 'TND',
  `provider` varchar(100) DEFAULT NULL,
  `image_url` text DEFAULT NULL,
  `level` varchar(30) DEFAULT 'BEGINNER',
  `is_free` tinyint(1) DEFAULT 1,
  `created_by` int(11) DEFAULT NULL,
  `created_date` datetime DEFAULT current_timestamp(),
  `status` varchar(20) DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `formation_modules`
--

CREATE TABLE `formation_modules` (
  `id` int(11) NOT NULL,
  `formation_id` int(11) NOT NULL,
  `title` varchar(200) NOT NULL,
  `description` text DEFAULT NULL,
  `content_url` text DEFAULT NULL,
  `duration_minutes` int(11) DEFAULT 0,
  `order_index` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `group_members`
--

CREATE TABLE `group_members` (
  `id` int(11) NOT NULL,
  `group_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `role` varchar(20) DEFAULT 'MEMBER',
  `joined_date` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `hire_offers`
--

CREATE TABLE `hire_offers` (
  `id` int(11) NOT NULL,
  `application_id` int(11) NOT NULL,
  `salary_offered` decimal(12,2) NOT NULL,
  `currency` varchar(10) DEFAULT 'TND',
  `start_date` date DEFAULT NULL,
  `contract_type` varchar(20) DEFAULT 'CDI',
  `benefits` text DEFAULT NULL,
  `status` varchar(20) DEFAULT 'PENDING',
  `created_date` datetime DEFAULT current_timestamp(),
  `responded_date` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `interviews`
--

CREATE TABLE `interviews` (
  `id` int(11) NOT NULL,
  `application_id` int(11) NOT NULL,
  `scheduled_date` datetime NOT NULL,
  `duration_minutes` int(11) DEFAULT 60,
  `type` varchar(20) DEFAULT 'VIDEO',
  `location` varchar(255) DEFAULT NULL,
  `video_link` text DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `status` varchar(20) DEFAULT 'SCHEDULED',
  `feedback` text DEFAULT NULL,
  `rating` int(11) DEFAULT 0,
  `timezone` varchar(50) DEFAULT 'Africa/Tunis',
  `created_date` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `job_offers`
--

CREATE TABLE `job_offers` (
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
(1, 1, 'Responsable marketing digital', '', NULL, NULL, NULL, 'EUR', 'BEN AROUS', 'ONSITE', '2026-02-07 17:26:09', NULL, 'OPEN'),
(2, 1, 'Ingénieur / Ingénieure génie civil', '', NULL, NULL, NULL, 'EUR', 'NABEUL', 'ONSITE', '2026-02-07 17:26:09', NULL, 'OPEN'),
(3, 1, 'Chef du personnel', '', NULL, NULL, NULL, 'EUR', 'TUNIS', 'ONSITE', '2026-02-07 17:26:09', NULL, 'OPEN'),
(4, 1, 'Conseiller / Conseillère clientèle à distance', '', NULL, NULL, NULL, 'EUR', 'SOUSSE', 'ONSITE', '2026-02-07 17:26:09', NULL, 'OPEN'),
(5, 1, 'Assistant administratif / Assistante administrative', '', NULL, NULL, NULL, 'EUR', 'SFAX', 'ONSITE', '2026-02-07 17:26:09', NULL, 'OPEN'),
(6, 1, '[Hiring] Offering 200$ for  simple verification signup task', '\n\nOffering 200$ for  simple verification signup task.\n\nchat on telegram: peterold\n\n\n\ncrypto paypal chime cashapp', NULL, NULL, NULL, 'EUR', '', 'ONSITE', '2026-02-07 17:26:09', NULL, 'OPEN'),
(7, 1, '[For Hire] Senior Graphic Designer', 'Hey! I\'m Simon.\n\nI\'m a senior graphic designer, and have been designing for over 10 years professionally.\n\nI\'m passionate about designing long-lasting, professional and meaningful products.\n\nSome notable skills:\n\n• Branding\n\n• Webflow Development / Design\n\n• Print\n\n• Social Media\n\n• Document Design\n\nPortfolio: [http://simondm.com](http://simondm.com)\n\nPlease reach out to me via my website, email, or Reddit. No job too small, just let me know what you\'re looking for and an idea of your budget and', NULL, NULL, NULL, 'EUR', '', 'ONSITE', '2026-02-07 17:26:09', NULL, 'OPEN'),
(8, 1, '[FOR HIRE] Software Developer | MVPs | Backend &amp; APIs | Automation | AI-Augmented Dev', 'Hi, Computer Science graduate based in Toronto, looking to contribute to early-stage startups and small teams that want to ship quickly.\n\nI focus on building practical, working systems  MVPs, internal tools, and automation  with an emphasis on speed, clarity, and ownership. I’ve worked across backend services, simple frontends, and data-driven features, using modern AI tools to move faster while still understanding and maintaining what’s built.\n\nWhat I can help with\n\n* MVPs &amp; prototypes\n* Ba', NULL, NULL, NULL, 'EUR', '', 'ONSITE', '2026-02-07 17:26:09', NULL, 'OPEN'),
(9, 1, '[For Hire] I will audit your Google Ads to help you increase ROI', 'Hey there! I\'m a Google Ads specialist for service-based businesses. I\'ve worked with a variety of businesses such as plumbers, landscapers, lawyers, dentists, senior care, and more. \n\nFor $199 I will review your ad account and make suggestions to help you increase your click rate, lower cost per click, increase conversions, reduce waste, and generally optimize your campaigns for better ROI. You\'ll receive a written report and a video walk through. \n\nI know that small businesses like to manage t', NULL, NULL, NULL, 'EUR', '', 'ONSITE', '2026-02-07 17:26:09', NULL, 'OPEN'),
(10, 1, 'Is your WooCommerce, Shopify, or WordPress website slow or buggy? I can help [FOR HIRE]', 'Are you constantly losing customers at checkout, your customers complain your website load slowly, or your website ranks poorly on Google Search Engine making your almost invisible online?  Worry no more, I help small businesses and startups fix these as well as optimize SEO and their websites to run smoothly and boost conversions.\n\n**What I specialize in:**\n\n\\- WooCommerce &amp; Shopify: Speed up checkout, fix errors, and increase conversions  \n\\- WordPress: Bug fixes, security improvements, an', NULL, NULL, NULL, 'EUR', '', 'ONSITE', '2026-02-07 17:26:09', NULL, 'OPEN'),
(11, 1, 'Request for Proposal Policy review on Brazil’s Vessel Data Reporting', '  Location: Remote      Who we are   &nbsp;  Global Fishing Watch is an international nonprofit organization dedicated to advancing ocean governance through increased transparency of human activity at sea. By creating and publicly sharing map visualizations, data and analysis tools, we aim to enable scientific research and transform the way our ocean is managed. We believe human activity at sea should be public knowledge in order to safegu', NULL, NULL, NULL, 'EUR', '', 'ONSITE', '2026-02-07 17:26:09', NULL, 'OPEN'),
(12, 1, 'Search Engine Optimisation Intern', '  Sprinto is an AI-native GRC platform that helps organisations manage risks, audits, vendor oversight, and continuous monitoring from a single connected platform. With a team of   350+ employees   serving   3,000+ customers   across   75+ countries  , Sprinto combines ', NULL, NULL, NULL, 'EUR', 'India', 'ONSITE', '2026-02-07 17:26:09', NULL, 'OPEN'),
(13, 1, 'Senior Engineer Confluent Marketplace Partner Applied Solution', ' We’re not just building better tech. We’re rewriting how data moves and what the world can do with it. With Confluent, data doesn’t sit still. Our platform puts information in motion, streaming in near real-time so companies can react faster, build smarter, and deliver experiences as dynamic as the world around them.  It takes a certain kind of person to join this team. Those who ask hard questions, give honest feedback, and show up for each other. No egos, no solo acts. Just smart, curi', NULL, NULL, NULL, 'EUR', '', 'ONSITE', '2026-02-07 17:26:09', NULL, 'OPEN'),
(14, 1, 'Senior Product Designer Türkiye', '  All roles at JumpCloud are Remote unless otherwise specified in the Job Description.        About JumpCloud    JumpCloud® delivers a unified open directory platform that makes it easy to securely manage identities, devices, and access across your organization. With JumpCloud®, IT teams and MSPs enable users to work securely from anywhere and manage their Windows, Apple, Linux, and An', NULL, NULL, NULL, 'EUR', 'Ankara, Türkiye - Remote', 'REMOTE', '2026-02-07 17:26:09', NULL, 'OPEN'),
(15, 1, 'Manager User Group Program 11132', ' Coupa makes margins multiply through its community-generated AI and industry-leading total spend management platform for businesses large and small. Coupa AI is informed by trillions of dollars of direct and indirect spend data across a global network of 10M+ buyers and suppliers. We empower you with the ability to predict, prescribe, and automate smarter, more profitable business decisions to improve operating margins.      Why join Coupa?      🔹 Pioneering', NULL, NULL, NULL, 'EUR', 'US Remote', 'REMOTE', '2026-02-07 17:26:09', NULL, 'OPEN');

-- --------------------------------------------------------

--
-- Table structure for table `login_attempts`
--

CREATE TABLE `login_attempts` (
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
(7, 'user', '2026-02-09 18:36:35', 1, NULL),
(8, 'admin', '2026-02-09 20:32:50', 1, NULL),
(9, 'user', '2026-02-09 20:33:06', 1, NULL),
(10, 'admin', '2026-02-09 20:39:36', 1, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `mentorships`
--

CREATE TABLE `mentorships` (
  `id` int(11) NOT NULL,
  `mentor_id` int(11) NOT NULL,
  `mentee_id` int(11) NOT NULL,
  `status` varchar(20) DEFAULT 'PENDING',
  `topic` varchar(200) DEFAULT NULL,
  `goals` text DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `rating` int(11) DEFAULT 0,
  `feedback` text DEFAULT NULL,
  `created_date` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `messages`
--

CREATE TABLE `messages` (
  `id` int(11) NOT NULL,
  `conversation_id` int(11) NOT NULL,
  `sender_id` int(11) NOT NULL,
  `content` text NOT NULL,
  `is_read` tinyint(1) DEFAULT 0,
  `created_date` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `password_reset_tokens`
--

CREATE TABLE `password_reset_tokens` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `otp_code` varchar(6) NOT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  `expires_at` datetime NOT NULL,
  `used` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `payment_transactions`
--

CREATE TABLE `payment_transactions` (
  `id` int(11) NOT NULL,
  `payslip_id` int(11) DEFAULT NULL,
  `from_account_id` int(11) DEFAULT NULL,
  `to_account_id` int(11) DEFAULT NULL,
  `amount` decimal(12,2) NOT NULL,
  `currency` varchar(10) DEFAULT 'TND',
  `transaction_type` varchar(30) DEFAULT 'SALARY',
  `status` varchar(20) DEFAULT 'PENDING',
  `reference` varchar(50) DEFAULT NULL,
  `transaction_date` datetime DEFAULT current_timestamp(),
  `notes` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `payslips`
--

CREATE TABLE `payslips` (
  `id` int(11) NOT NULL,
  `contract_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `period_month` int(11) NOT NULL,
  `period_year` int(11) NOT NULL,
  `gross_salary` decimal(12,2) NOT NULL,
  `net_salary` decimal(12,2) NOT NULL,
  `cnss_employee` decimal(10,2) DEFAULT 0.00,
  `cnss_employer` decimal(10,2) DEFAULT 0.00,
  `irpp` decimal(10,2) DEFAULT 0.00,
  `other_deductions` decimal(10,2) DEFAULT 0.00,
  `bonuses` decimal(10,2) DEFAULT 0.00,
  `currency` varchar(10) DEFAULT 'TND',
  `payment_status` varchar(20) DEFAULT 'PENDING',
  `payment_date` datetime DEFAULT NULL,
  `pdf_url` text DEFAULT NULL,
  `created_date` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `posts`
--

CREATE TABLE `posts` (
  `id` int(11) NOT NULL,
  `author_id` int(11) NOT NULL,
  `content` text NOT NULL,
  `image_url` text DEFAULT NULL,
  `post_type` varchar(30) DEFAULT 'STATUS',
  `likes_count` int(11) DEFAULT 0,
  `comments_count` int(11) DEFAULT 0,
  `shares_count` int(11) DEFAULT 0,
  `is_published` tinyint(1) DEFAULT 1,
  `created_date` datetime DEFAULT current_timestamp(),
  `updated_date` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `post_comments`
--

CREATE TABLE `post_comments` (
  `id` int(11) NOT NULL,
  `post_id` int(11) NOT NULL,
  `author_id` int(11) NOT NULL,
  `content` text NOT NULL,
  `created_date` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `post_likes`
--

CREATE TABLE `post_likes` (
  `id` int(11) NOT NULL,
  `post_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `created_date` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `profiles`
--

CREATE TABLE `profiles` (
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
(1, 1, 'System', 'Administrator', '', 'file:///C:/Users/NYX-PC/.skilora/images/user_1_1769983532814.jpg', '', '', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `quizzes`
--

CREATE TABLE `quizzes` (
  `id` int(11) NOT NULL,
  `formation_id` int(11) NOT NULL,
  `module_id` int(11) DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `description` text DEFAULT NULL,
  `pass_score` int(11) DEFAULT 70,
  `max_attempts` int(11) DEFAULT 3,
  `time_limit_minutes` int(11) DEFAULT 30
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `quiz_questions`
--

CREATE TABLE `quiz_questions` (
  `id` int(11) NOT NULL,
  `quiz_id` int(11) NOT NULL,
  `question_text` text NOT NULL,
  `option_a` varchar(500) DEFAULT NULL,
  `option_b` varchar(500) DEFAULT NULL,
  `option_c` varchar(500) DEFAULT NULL,
  `option_d` varchar(500) DEFAULT NULL,
  `correct_option` char(1) NOT NULL,
  `points` int(11) DEFAULT 1,
  `order_index` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `quiz_results`
--

CREATE TABLE `quiz_results` (
  `id` int(11) NOT NULL,
  `quiz_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `score` int(11) DEFAULT 0,
  `max_score` int(11) DEFAULT 0,
  `passed` tinyint(1) DEFAULT 0,
  `attempt_number` int(11) DEFAULT 1,
  `taken_date` datetime DEFAULT current_timestamp(),
  `time_spent_seconds` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `salary_history`
--

CREATE TABLE `salary_history` (
  `id` int(11) NOT NULL,
  `contract_id` int(11) NOT NULL,
  `old_salary` decimal(12,2) DEFAULT NULL,
  `new_salary` decimal(12,2) NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `effective_date` date NOT NULL,
  `created_date` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `skills`
--

CREATE TABLE `skills` (
  `id` int(11) NOT NULL,
  `profile_id` int(11) NOT NULL,
  `skill_name` varchar(100) DEFAULT NULL,
  `proficiency_level` varchar(50) DEFAULT NULL,
  `years_experience` int(11) DEFAULT NULL,
  `verified` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `support_tickets`
--

CREATE TABLE `support_tickets` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `category` varchar(50) NOT NULL,
  `priority` varchar(20) DEFAULT 'MEDIUM',
  `status` varchar(20) DEFAULT 'OPEN',
  `subject` varchar(255) NOT NULL,
  `description` text DEFAULT NULL,
  `assigned_to` int(11) DEFAULT NULL,
  `created_date` datetime DEFAULT current_timestamp(),
  `updated_date` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `resolved_date` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `tax_configurations`
--

CREATE TABLE `tax_configurations` (
  `id` int(11) NOT NULL,
  `country` varchar(50) NOT NULL,
  `tax_type` varchar(50) NOT NULL,
  `rate` decimal(8,4) NOT NULL,
  `min_bracket` decimal(12,2) DEFAULT 0.00,
  `max_bracket` decimal(12,2) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `effective_date` date NOT NULL,
  `is_active` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tax_configurations`
--

INSERT INTO `tax_configurations` (`id`, `country`, `tax_type`, `rate`, `min_bracket`, `max_bracket`, `description`, `effective_date`, `is_active`) VALUES
(1, 'Tunisia', 'IRPP', '0.0000', '0.00', '5000.00', 'Tranche exoneree', '2025-01-01', 1),
(2, 'Tunisia', 'IRPP', '0.2600', '5000.01', '20000.00', 'Tranche 26%', '2025-01-01', 1),
(3, 'Tunisia', 'IRPP', '0.2800', '20000.01', '30000.00', 'Tranche 28%', '2025-01-01', 1),
(4, 'Tunisia', 'IRPP', '0.3200', '30000.01', '50000.00', 'Tranche 32%', '2025-01-01', 1),
(5, 'Tunisia', 'IRPP', '0.3500', '50000.01', NULL, 'Tranche 35%', '2025-01-01', 1),
(6, 'Tunisia', 'CNSS_EMPLOYEE', '0.0918', '0.00', NULL, 'CNSS part salariale 9.18%', '2025-01-01', 1),
(7, 'Tunisia', 'CNSS_EMPLOYER', '0.1657', '0.00', NULL, 'CNSS part patronale 16.57%', '2025-01-01', 1);

-- --------------------------------------------------------

--
-- Table structure for table `ticket_messages`
--

CREATE TABLE `ticket_messages` (
  `id` int(11) NOT NULL,
  `ticket_id` int(11) NOT NULL,
  `sender_id` int(11) NOT NULL,
  `message` text NOT NULL,
  `is_internal` tinyint(1) DEFAULT 0,
  `created_date` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
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
(5, 'nour', NULL, '$2a$12$lAjcQWtAyL5jHeaRB1hrOOCn4OZFAWOw267hHCped1gX93tpDkqVa', 'USER', 'nour ', '2026-02-07 15:32:30', NULL, 0, 1);

-- --------------------------------------------------------

--
-- Table structure for table `user_feedback`
--

CREATE TABLE `user_feedback` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `feedback_type` varchar(30) NOT NULL,
  `rating` int(11) DEFAULT 0,
  `comment` text DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `is_resolved` tinyint(1) DEFAULT 0,
  `created_date` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `user_preferences`
--

CREATE TABLE `user_preferences` (
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
(5, 1, 'language', 'English', '2026-02-09 18:35:13');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `achievements`
--
ALTER TABLE `achievements`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_achievements_user` (`user_id`);

--
-- Indexes for table `applications`
--
ALTER TABLE `applications`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_applications_job_offer_id` (`job_offer_id`),
  ADD KEY `idx_applications_candidate_id` (`candidate_profile_id`),
  ADD KEY `idx_applications_status` (`status`);

--
-- Indexes for table `auto_responses`
--
ALTER TABLE `auto_responses`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `bank_accounts`
--
ALTER TABLE `bank_accounts`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_bank_accounts_user` (`user_id`);

--
-- Indexes for table `biometric_data`
--
ALTER TABLE `biometric_data`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `user_id` (`user_id`);

--
-- Indexes for table `blog_articles`
--
ALTER TABLE `blog_articles`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_blog_author` (`author_id`),
  ADD KEY `idx_blog_published` (`is_published`,`published_date`);

--
-- Indexes for table `certificates`
--
ALTER TABLE `certificates`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `enrollment_id` (`enrollment_id`),
  ADD UNIQUE KEY `certificate_number` (`certificate_number`);

--
-- Indexes for table `chatbot_conversations`
--
ALTER TABLE `chatbot_conversations`
  ADD PRIMARY KEY (`id`),
  ADD KEY `escalated_to_ticket_id` (`escalated_to_ticket_id`),
  ADD KEY `idx_chatbot_conv_user` (`user_id`);

--
-- Indexes for table `chatbot_messages`
--
ALTER TABLE `chatbot_messages`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_chatbot_msg_conv` (`conversation_id`);

--
-- Indexes for table `community_groups`
--
ALTER TABLE `community_groups`
  ADD PRIMARY KEY (`id`),
  ADD KEY `creator_id` (`creator_id`);

--
-- Indexes for table `companies`
--
ALTER TABLE `companies`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_companies_owner` (`owner_id`);

--
-- Indexes for table `connections`
--
ALTER TABLE `connections`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_connection` (`user_id_1`,`user_id_2`),
  ADD KEY `idx_connections_user1` (`user_id_1`),
  ADD KEY `idx_connections_user2` (`user_id_2`),
  ADD KEY `idx_connections_status` (`status`);

--
-- Indexes for table `conversations`
--
ALTER TABLE `conversations`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_conversation` (`participant_1`,`participant_2`),
  ADD KEY `idx_conversations_p1` (`participant_1`),
  ADD KEY `idx_conversations_p2` (`participant_2`);

--
-- Indexes for table `employment_contracts`
--
ALTER TABLE `employment_contracts`
  ADD PRIMARY KEY (`id`),
  ADD KEY `job_offer_id` (`job_offer_id`),
  ADD KEY `idx_contracts_user` (`user_id`),
  ADD KEY `idx_contracts_employer` (`employer_id`),
  ADD KEY `idx_contracts_status` (`status`);

--
-- Indexes for table `enrollments`
--
ALTER TABLE `enrollments`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_enrollment` (`formation_id`,`user_id`),
  ADD KEY `idx_enrollments_user` (`user_id`),
  ADD KEY `idx_enrollments_formation` (`formation_id`);

--
-- Indexes for table `events`
--
ALTER TABLE `events`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_events_organizer` (`organizer_id`),
  ADD KEY `idx_events_date` (`start_date`);

--
-- Indexes for table `event_rsvps`
--
ALTER TABLE `event_rsvps`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_rsvp` (`event_id`,`user_id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `idx_event_rsvps_event` (`event_id`);

--
-- Indexes for table `exchange_rates`
--
ALTER TABLE `exchange_rates`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_rate` (`from_currency`,`to_currency`,`rate_date`),
  ADD KEY `idx_exchange_rates_currencies` (`from_currency`,`to_currency`);

--
-- Indexes for table `experiences`
--
ALTER TABLE `experiences`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_experiences_profile_id` (`profile_id`);

--
-- Indexes for table `faq_articles`
--
ALTER TABLE `faq_articles`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_faq_category` (`category`);

--
-- Indexes for table `formations`
--
ALTER TABLE `formations`
  ADD PRIMARY KEY (`id`),
  ADD KEY `created_by` (`created_by`),
  ADD KEY `idx_formations_category` (`category`),
  ADD KEY `idx_formations_status` (`status`);

--
-- Indexes for table `formation_modules`
--
ALTER TABLE `formation_modules`
  ADD PRIMARY KEY (`id`),
  ADD KEY `formation_id` (`formation_id`);

--
-- Indexes for table `group_members`
--
ALTER TABLE `group_members`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_group_member` (`group_id`,`user_id`),
  ADD KEY `idx_group_members_group` (`group_id`),
  ADD KEY `idx_group_members_user` (`user_id`);

--
-- Indexes for table `hire_offers`
--
ALTER TABLE `hire_offers`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_hire_offers_application` (`application_id`),
  ADD KEY `idx_hire_offers_status` (`status`);

--
-- Indexes for table `interviews`
--
ALTER TABLE `interviews`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_interviews_application` (`application_id`),
  ADD KEY `idx_interviews_status` (`status`),
  ADD KEY `idx_interviews_date` (`scheduled_date`);

--
-- Indexes for table `job_offers`
--
ALTER TABLE `job_offers`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_job_offers_company_id` (`company_id`),
  ADD KEY `idx_job_offers_status` (`status`);

--
-- Indexes for table `login_attempts`
--
ALTER TABLE `login_attempts`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_login_attempts_username` (`username`,`attempted_at`);

--
-- Indexes for table `mentorships`
--
ALTER TABLE `mentorships`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_mentorship` (`mentor_id`,`mentee_id`),
  ADD KEY `idx_mentorships_mentor` (`mentor_id`),
  ADD KEY `idx_mentorships_mentee` (`mentee_id`);

--
-- Indexes for table `messages`
--
ALTER TABLE `messages`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_messages_conv` (`conversation_id`),
  ADD KEY `idx_messages_sender` (`sender_id`);

--
-- Indexes for table `password_reset_tokens`
--
ALTER TABLE `password_reset_tokens`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_password_reset_user` (`user_id`,`used`);

--
-- Indexes for table `payment_transactions`
--
ALTER TABLE `payment_transactions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `from_account_id` (`from_account_id`),
  ADD KEY `to_account_id` (`to_account_id`),
  ADD KEY `idx_transactions_payslip` (`payslip_id`);

--
-- Indexes for table `payslips`
--
ALTER TABLE `payslips`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_payslip_period` (`contract_id`,`period_month`,`period_year`),
  ADD KEY `idx_payslips_contract` (`contract_id`),
  ADD KEY `idx_payslips_user` (`user_id`),
  ADD KEY `idx_payslips_period` (`period_year`,`period_month`);

--
-- Indexes for table `posts`
--
ALTER TABLE `posts`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_posts_author` (`author_id`),
  ADD KEY `idx_posts_date` (`created_date`);

--
-- Indexes for table `post_comments`
--
ALTER TABLE `post_comments`
  ADD PRIMARY KEY (`id`),
  ADD KEY `author_id` (`author_id`),
  ADD KEY `idx_post_comments_post` (`post_id`);

--
-- Indexes for table `post_likes`
--
ALTER TABLE `post_likes`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_post_like` (`post_id`,`user_id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `idx_post_likes_post` (`post_id`);

--
-- Indexes for table `profiles`
--
ALTER TABLE `profiles`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_profiles_user_id` (`user_id`);

--
-- Indexes for table `quizzes`
--
ALTER TABLE `quizzes`
  ADD PRIMARY KEY (`id`),
  ADD KEY `formation_id` (`formation_id`),
  ADD KEY `module_id` (`module_id`);

--
-- Indexes for table `quiz_questions`
--
ALTER TABLE `quiz_questions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `quiz_id` (`quiz_id`);

--
-- Indexes for table `quiz_results`
--
ALTER TABLE `quiz_results`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_quiz_results_user` (`user_id`),
  ADD KEY `idx_quiz_results_quiz` (`quiz_id`);

--
-- Indexes for table `salary_history`
--
ALTER TABLE `salary_history`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_salary_history_contract` (`contract_id`);

--
-- Indexes for table `skills`
--
ALTER TABLE `skills`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_skills_profile_id` (`profile_id`);

--
-- Indexes for table `support_tickets`
--
ALTER TABLE `support_tickets`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_tickets_user` (`user_id`),
  ADD KEY `idx_tickets_status` (`status`),
  ADD KEY `idx_tickets_priority` (`priority`),
  ADD KEY `idx_tickets_assigned` (`assigned_to`);

--
-- Indexes for table `tax_configurations`
--
ALTER TABLE `tax_configurations`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_tax_config_country` (`country`,`tax_type`);

--
-- Indexes for table `ticket_messages`
--
ALTER TABLE `ticket_messages`
  ADD PRIMARY KEY (`id`),
  ADD KEY `sender_id` (`sender_id`),
  ADD KEY `idx_ticket_messages_ticket` (`ticket_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `username` (`username`),
  ADD KEY `idx_users_email` (`email`);

--
-- Indexes for table `user_feedback`
--
ALTER TABLE `user_feedback`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_feedback_user` (`user_id`);

--
-- Indexes for table `user_preferences`
--
ALTER TABLE `user_preferences`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_user_pref` (`user_id`,`pref_key`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `achievements`
--
ALTER TABLE `achievements`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `applications`
--
ALTER TABLE `applications`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `auto_responses`
--
ALTER TABLE `auto_responses`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT for table `bank_accounts`
--
ALTER TABLE `bank_accounts`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `biometric_data`
--
ALTER TABLE `biometric_data`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `blog_articles`
--
ALTER TABLE `blog_articles`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `certificates`
--
ALTER TABLE `certificates`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `chatbot_conversations`
--
ALTER TABLE `chatbot_conversations`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `chatbot_messages`
--
ALTER TABLE `chatbot_messages`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `community_groups`
--
ALTER TABLE `community_groups`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `companies`
--
ALTER TABLE `companies`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `connections`
--
ALTER TABLE `connections`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `conversations`
--
ALTER TABLE `conversations`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `employment_contracts`
--
ALTER TABLE `employment_contracts`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `enrollments`
--
ALTER TABLE `enrollments`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `events`
--
ALTER TABLE `events`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `event_rsvps`
--
ALTER TABLE `event_rsvps`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `exchange_rates`
--
ALTER TABLE `exchange_rates`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT for table `experiences`
--
ALTER TABLE `experiences`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `faq_articles`
--
ALTER TABLE `faq_articles`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT for table `formations`
--
ALTER TABLE `formations`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `formation_modules`
--
ALTER TABLE `formation_modules`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `group_members`
--
ALTER TABLE `group_members`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `hire_offers`
--
ALTER TABLE `hire_offers`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `interviews`
--
ALTER TABLE `interviews`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `job_offers`
--
ALTER TABLE `job_offers`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT for table `login_attempts`
--
ALTER TABLE `login_attempts`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `mentorships`
--
ALTER TABLE `mentorships`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `messages`
--
ALTER TABLE `messages`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `password_reset_tokens`
--
ALTER TABLE `password_reset_tokens`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `payment_transactions`
--
ALTER TABLE `payment_transactions`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `payslips`
--
ALTER TABLE `payslips`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `posts`
--
ALTER TABLE `posts`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `post_comments`
--
ALTER TABLE `post_comments`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `post_likes`
--
ALTER TABLE `post_likes`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `profiles`
--
ALTER TABLE `profiles`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `quizzes`
--
ALTER TABLE `quizzes`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `quiz_questions`
--
ALTER TABLE `quiz_questions`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `quiz_results`
--
ALTER TABLE `quiz_results`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `salary_history`
--
ALTER TABLE `salary_history`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `skills`
--
ALTER TABLE `skills`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `support_tickets`
--
ALTER TABLE `support_tickets`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `tax_configurations`
--
ALTER TABLE `tax_configurations`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT for table `ticket_messages`
--
ALTER TABLE `ticket_messages`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT for table `user_feedback`
--
ALTER TABLE `user_feedback`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `user_preferences`
--
ALTER TABLE `user_preferences`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `achievements`
--
ALTER TABLE `achievements`
  ADD CONSTRAINT `achievements_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `applications`
--
ALTER TABLE `applications`
  ADD CONSTRAINT `applications_ibfk_1` FOREIGN KEY (`job_offer_id`) REFERENCES `job_offers` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `applications_ibfk_2` FOREIGN KEY (`candidate_profile_id`) REFERENCES `profiles` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `bank_accounts`
--
ALTER TABLE `bank_accounts`
  ADD CONSTRAINT `bank_accounts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `biometric_data`
--
ALTER TABLE `biometric_data`
  ADD CONSTRAINT `biometric_data_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `blog_articles`
--
ALTER TABLE `blog_articles`
  ADD CONSTRAINT `blog_articles_ibfk_1` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `certificates`
--
ALTER TABLE `certificates`
  ADD CONSTRAINT `certificates_ibfk_1` FOREIGN KEY (`enrollment_id`) REFERENCES `enrollments` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `chatbot_conversations`
--
ALTER TABLE `chatbot_conversations`
  ADD CONSTRAINT `chatbot_conversations_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `chatbot_conversations_ibfk_2` FOREIGN KEY (`escalated_to_ticket_id`) REFERENCES `support_tickets` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `chatbot_messages`
--
ALTER TABLE `chatbot_messages`
  ADD CONSTRAINT `chatbot_messages_ibfk_1` FOREIGN KEY (`conversation_id`) REFERENCES `chatbot_conversations` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `community_groups`
--
ALTER TABLE `community_groups`
  ADD CONSTRAINT `community_groups_ibfk_1` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `companies`
--
ALTER TABLE `companies`
  ADD CONSTRAINT `companies_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `connections`
--
ALTER TABLE `connections`
  ADD CONSTRAINT `connections_ibfk_1` FOREIGN KEY (`user_id_1`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `connections_ibfk_2` FOREIGN KEY (`user_id_2`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `conversations`
--
ALTER TABLE `conversations`
  ADD CONSTRAINT `conversations_ibfk_1` FOREIGN KEY (`participant_1`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `conversations_ibfk_2` FOREIGN KEY (`participant_2`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `employment_contracts`
--
ALTER TABLE `employment_contracts`
  ADD CONSTRAINT `employment_contracts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `employment_contracts_ibfk_2` FOREIGN KEY (`employer_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `employment_contracts_ibfk_3` FOREIGN KEY (`job_offer_id`) REFERENCES `job_offers` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `enrollments`
--
ALTER TABLE `enrollments`
  ADD CONSTRAINT `enrollments_ibfk_1` FOREIGN KEY (`formation_id`) REFERENCES `formations` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `enrollments_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `events`
--
ALTER TABLE `events`
  ADD CONSTRAINT `events_ibfk_1` FOREIGN KEY (`organizer_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `event_rsvps`
--
ALTER TABLE `event_rsvps`
  ADD CONSTRAINT `event_rsvps_ibfk_1` FOREIGN KEY (`event_id`) REFERENCES `events` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `event_rsvps_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `experiences`
--
ALTER TABLE `experiences`
  ADD CONSTRAINT `experiences_ibfk_1` FOREIGN KEY (`profile_id`) REFERENCES `profiles` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `formations`
--
ALTER TABLE `formations`
  ADD CONSTRAINT `formations_ibfk_1` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `formation_modules`
--
ALTER TABLE `formation_modules`
  ADD CONSTRAINT `formation_modules_ibfk_1` FOREIGN KEY (`formation_id`) REFERENCES `formations` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `group_members`
--
ALTER TABLE `group_members`
  ADD CONSTRAINT `group_members_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `community_groups` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `group_members_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `hire_offers`
--
ALTER TABLE `hire_offers`
  ADD CONSTRAINT `hire_offers_ibfk_1` FOREIGN KEY (`application_id`) REFERENCES `applications` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `interviews`
--
ALTER TABLE `interviews`
  ADD CONSTRAINT `interviews_ibfk_1` FOREIGN KEY (`application_id`) REFERENCES `applications` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `job_offers`
--
ALTER TABLE `job_offers`
  ADD CONSTRAINT `job_offers_ibfk_1` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `mentorships`
--
ALTER TABLE `mentorships`
  ADD CONSTRAINT `mentorships_ibfk_1` FOREIGN KEY (`mentor_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `mentorships_ibfk_2` FOREIGN KEY (`mentee_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `messages`
--
ALTER TABLE `messages`
  ADD CONSTRAINT `messages_ibfk_1` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `messages_ibfk_2` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `password_reset_tokens`
--
ALTER TABLE `password_reset_tokens`
  ADD CONSTRAINT `password_reset_tokens_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `payment_transactions`
--
ALTER TABLE `payment_transactions`
  ADD CONSTRAINT `payment_transactions_ibfk_1` FOREIGN KEY (`payslip_id`) REFERENCES `payslips` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `payment_transactions_ibfk_2` FOREIGN KEY (`from_account_id`) REFERENCES `bank_accounts` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `payment_transactions_ibfk_3` FOREIGN KEY (`to_account_id`) REFERENCES `bank_accounts` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `payslips`
--
ALTER TABLE `payslips`
  ADD CONSTRAINT `payslips_ibfk_1` FOREIGN KEY (`contract_id`) REFERENCES `employment_contracts` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `payslips_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `posts`
--
ALTER TABLE `posts`
  ADD CONSTRAINT `posts_ibfk_1` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `post_comments`
--
ALTER TABLE `post_comments`
  ADD CONSTRAINT `post_comments_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `post_comments_ibfk_2` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `post_likes`
--
ALTER TABLE `post_likes`
  ADD CONSTRAINT `post_likes_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `post_likes_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `profiles`
--
ALTER TABLE `profiles`
  ADD CONSTRAINT `profiles_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `quizzes`
--
ALTER TABLE `quizzes`
  ADD CONSTRAINT `quizzes_ibfk_1` FOREIGN KEY (`formation_id`) REFERENCES `formations` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `quizzes_ibfk_2` FOREIGN KEY (`module_id`) REFERENCES `formation_modules` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `quiz_questions`
--
ALTER TABLE `quiz_questions`
  ADD CONSTRAINT `quiz_questions_ibfk_1` FOREIGN KEY (`quiz_id`) REFERENCES `quizzes` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `quiz_results`
--
ALTER TABLE `quiz_results`
  ADD CONSTRAINT `quiz_results_ibfk_1` FOREIGN KEY (`quiz_id`) REFERENCES `quizzes` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `quiz_results_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `salary_history`
--
ALTER TABLE `salary_history`
  ADD CONSTRAINT `salary_history_ibfk_1` FOREIGN KEY (`contract_id`) REFERENCES `employment_contracts` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `skills`
--
ALTER TABLE `skills`
  ADD CONSTRAINT `skills_ibfk_1` FOREIGN KEY (`profile_id`) REFERENCES `profiles` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `support_tickets`
--
ALTER TABLE `support_tickets`
  ADD CONSTRAINT `support_tickets_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `support_tickets_ibfk_2` FOREIGN KEY (`assigned_to`) REFERENCES `users` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `ticket_messages`
--
ALTER TABLE `ticket_messages`
  ADD CONSTRAINT `ticket_messages_ibfk_1` FOREIGN KEY (`ticket_id`) REFERENCES `support_tickets` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `ticket_messages_ibfk_2` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `user_feedback`
--
ALTER TABLE `user_feedback`
  ADD CONSTRAINT `user_feedback_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `user_preferences`
--
ALTER TABLE `user_preferences`
  ADD CONSTRAINT `user_preferences_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
