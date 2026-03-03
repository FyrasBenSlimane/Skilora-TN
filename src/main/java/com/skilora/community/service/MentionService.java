package com.skilora.community.service;

import com.skilora.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MentionService — Service de gestion des mentions @utilisateur.
 *
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║  FONCTIONNALITÉ : Système de mentions (@user) dans les posts    ║
 * ║  Similaire à : Twitter, Instagram, LinkedIn                      ║
 * ║  Pattern regex : @NomPrenom ou @Nom (mot commençant par @)       ║
 * ║  Notification : L'utilisateur mentionné reçoit une notification  ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Fonctionnement :
 *   1. L'utilisateur tape @J dans le champ de texte
 *   2. searchUsers("J") cherche tous les utilisateurs commençant par "J"
 *   3. Une popup d'autocomplétion affiche les résultats (Jean Dupont, Julie...)
 *   4. L'utilisateur sélectionne → le texte est complété avec @Jean_Dupont
 *   5. Lors de la soumission, extractMentions() extrait tous les @mentions
 *   6. Pour chaque mention, on cherche l'utilisateur et on notifie
 *
 * Regex utilisé : @(\w+(?:_\w+)*)
 *   - @ : le caractère arobase (déclencheur de mention)
 *   - \w+ : un ou plusieurs caractères de mot (lettres, chiffres, underscore)
 *   - (?:_\w+)* : suivi optionnellement de _mot (pour les noms composés)
 *   - Exemples valides : @Jean, @Jean_Dupont, @admin
 *
 * Pattern : Singleton
 */
public class MentionService {

    private static final Logger logger = LoggerFactory.getLogger(MentionService.class);

    /**
     * Regex pour détecter les mentions dans un texte.
     * Capture tout mot commençant par @ suivi de caractères alphanumériques et underscores.
     */
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+(?:_\\w+)*)");

    // ── Singleton ──
    private static volatile MentionService instance;

    /** Constructeur privé (Singleton) */
    private MentionService() {}

    /** Retourne l'instance unique (Double-Checked Locking) */
    public static MentionService getInstance() {
        if (instance == null) {
            synchronized (MentionService.class) {
                if (instance == null) {
                    instance = new MentionService();
                }
            }
        }
        return instance;
    }

    /**
     * Classe interne représentant un résultat de recherche d'utilisateur.
     * Contient l'ID, le nom complet et le "handle" (nom sans espaces).
     */
    public static class UserMention {
        private final int userId;       // ID de l'utilisateur en base
        private final String fullName;  // Nom complet (ex: "Jean Dupont")
        private final String handle;    // Handle pour mention (ex: "Jean_Dupont")

        public UserMention(int userId, String fullName) {
            this.userId = userId;
            this.fullName = fullName;
            // Convertir le nom en handle : remplacer les espaces par des underscores
            this.handle = fullName != null ? fullName.trim().replaceAll("\\s+", "_") : "unknown";
        }

        public int getUserId() { return userId; }
        public String getFullName() { return fullName; }
        public String getHandle() { return handle; }

        @Override
        public String toString() {
            return "@" + handle + " (" + fullName + ")";
        }
    }

    /**
     * Recherche les utilisateurs dont le nom correspond au texte saisi.
     * Utilisée pour l'autocomplétion quand l'utilisateur tape @...
     *
     * Requête SQL :
     *   SELECT id, full_name FROM users
     *   WHERE full_name LIKE '%query%' OR REPLACE(full_name, ' ', '_') LIKE '%query%'
     *   LIMIT 8
     *
     * Le REPLACE permet de chercher aussi avec les underscores (Jean_Dupont).
     *
     * @param query  le texte tapé après @ (ex: "Jea" pour chercher "Jean")
     * @param limit  nombre maximum de résultats (8 par défaut pour la popup)
     * @return liste des utilisateurs correspondants
     */
    public List<UserMention> searchUsers(String query, int limit) {
        List<UserMention> results = new ArrayList<>();
        if (query == null || query.isBlank()) return results;

        // Requête SQL : chercher par nom complet OU par handle (nom avec underscores)
        String sql = "SELECT id, full_name FROM users " +
                "WHERE full_name LIKE ? OR REPLACE(full_name, ' ', '_') LIKE ? " +
                "ORDER BY full_name ASC LIMIT ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String pattern = "%" + query + "%"; // Recherche partielle : %texte%
            stmt.setString(1, pattern);  // Chercher dans full_name
            stmt.setString(2, pattern);  // Chercher aussi dans le handle (nom_prenom)
            stmt.setInt(3, limit);       // Limiter les résultats

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(new UserMention(
                        rs.getInt("id"),
                        rs.getString("full_name")
                ));
            }
        } catch (SQLException e) {
            logger.error("Error searching users for mention: {}", e.getMessage());
        }
        return results;
    }

    /**
     * Surcharge : recherche avec limite par défaut de 8 résultats.
     */
    public List<UserMention> searchUsers(String query) {
        return searchUsers(query, 8);
    }

    /**
     * Extrait toutes les mentions (@user) d'un texte.
     * Utilise le regex MENTION_PATTERN pour trouver toutes les occurrences de @mot.
     *
     * Exemple :
     *   Input  : "Bravo @Jean_Dupont et @Marie pour le projet !"
     *   Output : ["Jean_Dupont", "Marie"]
     *
     * @param text le texte contenant potentiellement des mentions
     * @return liste des handles mentionnés (sans le @)
     */
    public List<String> extractMentions(String text) {
        List<String> mentions = new ArrayList<>();
        if (text == null || text.isBlank()) return mentions;

        // Appliquer le regex sur tout le texte
        Matcher matcher = MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            String handle = matcher.group(1); // Groupe 1 = le handle sans le @
            if (!mentions.contains(handle)) { // Éviter les doublons
                mentions.add(handle);
            }
        }
        return mentions;
    }

    /**
     * Trouve l'ID d'un utilisateur à partir de son handle (@mention).
     * Cherche dans la base par nom complet ou par handle.
     *
     * @param handle le handle de mention (ex: "Jean_Dupont")
     * @return l'ID de l'utilisateur, ou -1 si non trouvé
     */
    public int findUserIdByHandle(String handle) {
        if (handle == null || handle.isBlank()) return -1;

        // Convertir le handle en nom pour la recherche
        String nameFromHandle = handle.replace("_", " ");

        String sql = "SELECT id FROM users WHERE full_name = ? OR REPLACE(full_name, ' ', '_') = ? LIMIT 1";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nameFromHandle); // Chercher par nom complet
            stmt.setString(2, handle);          // Ou par handle exact
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            logger.error("Error finding user by handle: {}", e.getMessage());
        }
        return -1; // Utilisateur non trouvé
    }

    /**
     * Traite les mentions dans un texte de post et envoie des notifications.
     * Appelée après la création d'un post contenant des @mentions.
     *
     * Étapes :
     *   1. Extraire les mentions du texte
     *   2. Pour chaque mention, trouver l'ID de l'utilisateur
     *   3. Insérer une notification dans la table notifications
     *
     * @param text     le texte du post contenant les mentions
     * @param authorId l'ID de l'auteur du post (celui qui mentionne)
     * @param postId   l'ID du post (pour le lien depuis la notification)
     */
    public void processMentions(String text, int authorId, int postId) {
        List<String> mentions = extractMentions(text);
        if (mentions.isEmpty()) return;

        logger.info("Processing {} mentions in post {}", mentions.size(), postId);

        for (String handle : mentions) {
            int userId = findUserIdByHandle(handle);
            if (userId > 0 && userId != authorId) { // Ne pas se notifier soi-même
                // Insérer la notification dans la base de données
                createMentionNotification(userId, authorId, postId);
                logger.info("Mention notification sent to user {} (handle: @{})", userId, handle);
            }
        }
    }

    /**
     * Crée une notification de mention dans la base de données.
     * L'utilisateur mentionné verra la notification dans son centre de notifications.
     *
     * @param mentionedUserId l'ID de l'utilisateur mentionné
     * @param authorId        l'ID de l'auteur qui a mentionné
     * @param postId          l'ID du post contenant la mention
     */
    private void createMentionNotification(int mentionedUserId, int authorId, int postId) {
        String sql = "INSERT INTO notifications (user_id, type, message, reference_id, is_read, created_date) " +
                "VALUES (?, 'MENTION', ?, ?, FALSE, NOW())";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, mentionedUserId);     // Destinataire de la notification
            stmt.setString(2, "Vous avez été mentionné dans un post"); // Message
            stmt.setInt(3, postId);               // Référence vers le post
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error creating mention notification: {}", e.getMessage());
        }
    }
}
