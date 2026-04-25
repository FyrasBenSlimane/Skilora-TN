-- =============================================================================
-- ERREUR #1834 : vous ne devez PAS exécuter seule la ligne ALTER sur applications.
-- phpMyAdmin "Copier" ne copie souvent qu'UNE ligne : c'est pour cela que ça échoue.
--
-- ERREUR #1062 « duplicate entry '1' for key 'PRIMARY' » lors du MODIFY AUTO_INCREMENT :
-- Cause fréquente : une ligne avec id = 0 (ou id < 0). MySQL tente de renuméroter vers 1 alors
-- qu’une ligne id = 1 existe déjà. La procédure ci-dessous (SOLUTION 3) corrige id<=0 puis fixe
-- AUTO_INCREMENT = MAX(id)+1. Sinon, vérifiez les doublons : SELECT id, COUNT(*) c FROM
-- applications GROUP BY id HAVING c > 1;
-- =============================================================================

-- -----------------------------------------------------------------------------
-- SOLUTION 1 (à essayer EN PREMIER) — TOUT sur UNE SEULE ligne, sans retour à la ligne
-- Collez la ligne entière dans SQL, puis Exécuter UNE fois.
-- -----------------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS=0;ALTER TABLE applications MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT;SET FOREIGN_KEY_CHECKS=1;

-- Si vous voulez aussi corriger profiles dans la même ligne :
-- SET FOREIGN_KEY_CHECKS=0;ALTER TABLE applications MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT;ALTER TABLE profiles MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT;SET FOREIGN_KEY_CHECKS=1;

-- -----------------------------------------------------------------------------
-- SOLUTION 2 — si la solution 1 est refusée (client SQL sans multi-instructions)
-- Exécutez CHAQUE bloc ci-dessous séparément, dans l’ordre (1 puis 2 puis 3 puis 4).
-- Le nom de votre contrainte actuel est : fk_interview_candidates_application
-- -----------------------------------------------------------------------------

-- (1) Supprimer la FK vers applications (nom vu dans votre message d’erreur MySQL)
ALTER TABLE interview_candidates DROP FOREIGN KEY fk_interview_candidates_application;

-- (1bis) Si MySQL dit "Unknown key", essayez à la place l’ancien nom automatique :
-- ALTER TABLE interview_candidates DROP FOREIGN KEY interview_candidates_ibfk_1;

-- (2) Si vous avez une table interviews liée à applications, listez puis supprimez sa FK :
-- SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'interviews'
--    AND REFERENCED_TABLE_NAME = 'applications';
-- puis par exemple :
-- ALTER TABLE interviews DROP FOREIGN KEY interviews_ibfk_1;

-- (3) Modifier applications (seulement après que les DROP ci-dessus ont réussi)
ALTER TABLE applications MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT;

-- (4) Recréer la clé étrangère (obligatoire après le DROP de l’étape 1)
ALTER TABLE interview_candidates
  ADD CONSTRAINT fk_interview_candidates_application
  FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE;

-- =============================================================================
-- Voir toutes les FK qui pointent encore vers applications.id (diagnostic)
-- =============================================================================
SELECT kcu.TABLE_NAME AS table_enfant, kcu.CONSTRAINT_NAME AS nom_a_supprimer
FROM information_schema.KEY_COLUMN_USAGE kcu
WHERE kcu.TABLE_SCHEMA = DATABASE()
  AND kcu.REFERENCED_TABLE_NAME = 'applications'
  AND kcu.REFERENCED_COLUMN_NAME = 'id';

-- -----------------------------------------------------------------------------
-- SOLUTION 3 — UNE exécution dans phpMyAdmin (tout le bloc, y compris CALL)
-- Supprime dynamiquement TOUTES les FK qui pointent vers applications.id,
-- modifie applications, puis recrée interview_candidates + interviews.
-- Si "ADD CONSTRAINT" dit que la contrainte existe déjà, ignorez (migration déjà OK).
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS fix_applications_autoincrement;
DELIMITER $$
CREATE PROCEDURE fix_applications_autoincrement()
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE v_table VARCHAR(64);
  DECLARE v_cstr VARCHAR(64);
  DECLARE cur CURSOR FOR
    SELECT DISTINCT kcu.TABLE_NAME, kcu.CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = DATABASE()
      AND kcu.REFERENCED_TABLE_NAME = 'applications'
      AND kcu.REFERENCED_COLUMN_NAME = 'id';
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

  SET FOREIGN_KEY_CHECKS = 0;

  -- #1062 : id <= 0 provoque un renumérotation vers 1 en conflit avec une ligne id = 1
  fix_ids: WHILE (SELECT COUNT(*) FROM applications WHERE id <= 0) > 0 DO
    SET @bad := (SELECT MIN(id) FROM applications WHERE id <= 0);
    SET @mx := IFNULL((SELECT MAX(id) FROM applications WHERE id > 0), 0);
    SET @nw := @mx + 1;
    SELECT COUNT(*) INTO @has_ic FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'interview_candidates';
    IF @has_ic > 0 THEN
      UPDATE interview_candidates SET application_id = @nw WHERE application_id = @bad;
    END IF;
    SELECT COUNT(*) INTO @has_iv FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'interviews';
    IF @has_iv > 0 THEN
      UPDATE interviews SET application_id = @nw WHERE application_id = @bad;
    END IF;
    UPDATE applications SET id = @nw WHERE id = @bad LIMIT 1;
  END WHILE fix_ids;

  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO v_table, v_cstr;
    IF done THEN
      LEAVE read_loop;
    END IF;
    SET @sql = CONCAT('ALTER TABLE `', v_table, '` DROP FOREIGN KEY `', v_cstr, '`');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END LOOP;
  CLOSE cur;

  SELECT COALESCE(MAX(id), 0) + 1 INTO @ainc FROM applications;
  SET @modsql := CONCAT(
    'ALTER TABLE applications MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=',
    @ainc);
  PREPARE am FROM @modsql;
  EXECUTE am;
  DEALLOCATE PREPARE am;

  SELECT COUNT(*) INTO @has_ic FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'interview_candidates';
  IF @has_ic > 0 THEN
    ALTER TABLE interview_candidates
      ADD CONSTRAINT fk_interview_candidates_application
      FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE;
  END IF;

  SELECT COUNT(*) INTO @has_iv FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'interviews';
  IF @has_iv > 0 THEN
    ALTER TABLE interviews
      ADD CONSTRAINT fk_interviews_application
      FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE;
  END IF;

  SET FOREIGN_KEY_CHECKS = 1;
END$$
DELIMITER ;

CALL fix_applications_autoincrement();
