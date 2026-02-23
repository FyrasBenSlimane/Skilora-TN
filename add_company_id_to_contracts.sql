-- =============================================
-- SKILORA - Fix: Add company_id to contracts
-- Run this in phpMyAdmin on the 'skilora' database
-- =============================================

USE skilora;

-- Étape 1: Ajouter la colonne company_id (nullable pour ne pas casser les données existantes)
ALTER TABLE `contracts`
    ADD COLUMN IF NOT EXISTS `company_id` INT DEFAULT NULL AFTER `user_id`;

-- Étape 2: Ajouter la foreign key si elle n'existe pas
-- (ignore l'erreur si elle existe déjà)
ALTER TABLE `contracts`
    ADD CONSTRAINT `contracts_ibfk_company`
    FOREIGN KEY (`company_id`) REFERENCES `companies`(`id`) ON DELETE SET NULL;

-- Vérification
SELECT 'company_id ajouté avec succès!' AS status;
DESCRIBE `contracts`;
