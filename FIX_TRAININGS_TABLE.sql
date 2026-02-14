-- Fix trainings table AUTO_INCREMENT issue
-- Run this SQL command if the automatic fix doesn't work

USE skilora;

-- Check current table structure
DESCRIBE trainings;

-- Fix: Modify id column to have AUTO_INCREMENT (without redefining PRIMARY KEY)
ALTER TABLE trainings MODIFY COLUMN id INT AUTO_INCREMENT;

-- Verify the fix
DESCRIBE trainings;

-- Check AUTO_INCREMENT value
SHOW TABLE STATUS WHERE Name = 'trainings';
