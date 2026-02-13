-- Database schema for Skilora Support Ticket System
CREATE DATABASE IF NOT EXISTS skylora;
USE skylora;

-- 1. Table for Tickets
CREATE TABLE IF NOT EXISTS ticket (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id INT NOT NULL,
    subject VARCHAR(255),
    categorie VARCHAR(50),
    priorite VARCHAR(20),
    statut VARCHAR(20),
    description TEXT,
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
    date_resolution DATETIME,
    agent_id INT
);

-- 2. Table for Messages
CREATE TABLE IF NOT EXISTS ticket_messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ticket_id INT NOT NULL,
    utilisateur_id INT NOT NULL,
    contenu TEXT,
    date_envoi DATETIME DEFAULT CURRENT_TIMESTAMP,
    is_internal BOOLEAN DEFAULT FALSE,
    attachments_json TEXT,
    FOREIGN KEY (ticket_id) REFERENCES ticket(id) ON DELETE CASCADE
);

-- 3. Insert Dummy Tickets
INSERT INTO ticket (utilisateur_id, subject, categorie, priorite, statut, description, date_creation) VALUES 
(1, 'Problème de connexion au compte', 'TECHNICAL', 'HIGH', 'OUVERT', 'Je n''arrive plus à accéder à mon dashboard depuis ce matin. J''ai essayé de réinitialiser le mot de passe mais sans succès.', NOW()),
(1, 'Erreur lors du paiement', 'PAYMENT', 'URGENT', 'EN_COURS', 'J''ai été débité deux fois pour la même formation. Merci de vérifier.', NOW()),
(2, 'Demande d''information sur la formation Java', 'FORMATION', 'MEDIUM', 'OUVERT', 'Bonjour, je voudrais savoir si la formation Java est éligible au CPF.', NOW()),
(3, 'Bug affichage profil', 'TECHNICAL', 'LOW', 'RESOLU', 'Mon image de profil ne s''affiche pas correctement sur mobile.', NOW() - INTERVAL 2 DAY);

-- 4. Insert Dummy Messages for Ticket #1
INSERT INTO ticket_messages (ticket_id, utilisateur_id, contenu, date_envoi, is_internal) VALUES 
(1, 1, 'Bonjour, je suis bloqué à la page de login.', NOW() - INTERVAL 1 HOUR, FALSE),
(1, 2, 'Bonjour, avez-vous essayé de vider votre cache ?', NOW() - INTERVAL 30 MINUTE, FALSE),
(1, 1, 'Oui, j''ai essayé mais ça ne change rien.', NOW() - INTERVAL 10 MINUTE, FALSE);

-- 5. Insert Dummy Messages for Ticket #2
INSERT INTO ticket_messages (ticket_id, utilisateur_id, contenu, date_envoi, is_internal) VALUES 
(2, 1, 'Voici la capture d''écran du double débit.', NOW() - INTERVAL 1 DAY, FALSE),
(2, 2, 'Nous avons bien reçu votre demande, nous contactons la banque.', NOW() - INTERVAL 20 HOUR, TRUE); -- Note Interne
