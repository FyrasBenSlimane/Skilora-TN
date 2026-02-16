package com.skilora.community.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.community.entity.Conversation;
import com.skilora.community.entity.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MessagingService — Service CRUD pour la messagerie entre utilisateurs.
 *
 * Responsabilités :
 *   - CREATE : Créer/récupérer une conversation (getOrCreateConversation),
 *             envoyer un message (sendMessage)
 *   - READ   : Lire les conversations (getConversations), lire les messages (getMessages),
 *             compter les non lus (getUnreadCount)
 *   - UPDATE : Modifier un message (updateMessage), marquer comme lu (markAsRead)
 *   - DELETE : Supprimer un message (deleteMessage)
 *
 * Sécurité : Les opérations UPDATE et DELETE vérifient le sender_id
 *           (seul l'expéditeur peut modifier/supprimer ses propres messages).
 *
 * Pattern : Singleton thread-safe (Double-Checked Locking)
 */
public class MessagingService {

    private static final Logger logger = LoggerFactory.getLogger(MessagingService.class);

    // ── Instance unique (pattern Singleton) ──
    private static volatile MessagingService instance;

    // Constructeur privé : empêche l'instanciation directe (Singleton)
    private MessagingService() {}

    /**
     * Retourne l'instance unique du MessagingService.
     * Utilise le pattern Double-Checked Locking pour la sécurité multi-thread.
     * @return l'instance singleton de MessagingService
     */
    public static MessagingService getInstance() {
        if (instance == null) {                          // 1ère vérification (sans verrou)
            synchronized (MessagingService.class) {      // Verrou pour thread-safety
                if (instance == null) {                  // 2ème vérification (avec verrou)
                    instance = new MessagingService();
                }
            }
        }
        return instance;
    }

    // ═══════════════════════════════════════════════════════════
    //  GESTION DES CONVERSATIONS — Create/Read
    // ═══════════════════════════════════════════════════════════

    /**
     * Récupère ou crée une conversation entre deux utilisateurs.
     * Utilise Math.min/Math.max pour normaliser l'ordre des participants
     * et éviter les doublons (A→B == B→A).
     *
     * @param userId1 premier participant
     * @param userId2 deuxième participant
     * @return l'ID de la conversation existante ou nouvellement créée
     */
    public int getOrCreateConversation(int userId1, int userId2) {
        // Normalisation : participant_1 = le plus petit ID, participant_2 = le plus grand
        // Cela garantit qu'une seule conversation existe entre deux utilisateurs
        int p1 = Math.min(userId1, userId2);
        int p2 = Math.max(userId1, userId2);
        
        // Étape 1 : Vérifier si la conversation existe déjà
        String checkSql = "SELECT id FROM conversations WHERE participant_1 = ? AND participant_2 = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkSql)) {
            stmt.setInt(1, p1);
            stmt.setInt(2, p2);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id"); // Conversation existante trouvée
            }
        } catch (SQLException e) {
            logger.error("Error checking conversation: {}", e.getMessage(), e);
        }
        
        // Étape 2 : Créer une nouvelle conversation si elle n'existe pas
        String insertSql = "INSERT INTO conversations (participant_1, participant_2, created_date) VALUES (?, ?, NOW())";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, p1);
            stmt.setInt(2, p2);
            stmt.executeUpdate();
            
            // Récupérer l'ID auto-généré
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error creating conversation: {}", e.getMessage(), e);
        }
        return -1; // Échec
    }

    /**
     * Récupère la liste des conversations d'un utilisateur.
     * Requête complexe avec :
     *   - CASE WHEN pour déterminer le nom de l'autre participant
     *   - Sous-requête pour le dernier message (aperçu)
     *   - Sous-requête pour le compteur de messages non lus
     *   - Trié par date du dernier message (plus récent en premier)
     *
     * @param userId l'ID de l'utilisateur connecté
     * @return liste des conversations avec les infos transitoires (otherUserName, lastMessagePreview, unreadCount)
     */
    public List<Conversation> getConversations(int userId) {
        List<Conversation> conversations = new ArrayList<>();
        // Requête SQL complexe avec CASE WHEN, sous-requêtes et double JOIN
        String sql = """
            SELECT c.*,
                CASE 
                    WHEN c.participant_1 = ? THEN u2.full_name 
                    ELSE u1.full_name 
                END as other_name,
                CASE 
                    WHEN c.participant_1 = ? THEN u2.photo_url 
                    ELSE u1.photo_url 
                END as other_photo,
                (SELECT content FROM messages WHERE conversation_id = c.id ORDER BY created_date DESC LIMIT 1) as last_msg,
                (SELECT COUNT(*) FROM messages WHERE conversation_id = c.id AND sender_id != ? AND is_read = FALSE) as unread
            FROM conversations c
            JOIN users u1 ON c.participant_1 = u1.id
            JOIN users u2 ON c.participant_2 = u2.id
            WHERE (c.participant_1 = ? OR c.participant_2 = ?)
            AND ((c.participant_1 = ? AND c.is_archived_1 = FALSE) OR (c.participant_2 = ? AND c.is_archived_2 = FALSE))
            ORDER BY CASE WHEN c.last_message_date IS NULL THEN 1 ELSE 0 END, c.last_message_date DESC
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setInt(4, userId);
            stmt.setInt(5, userId);
            stmt.setInt(6, userId);
            stmt.setInt(7, userId);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                conversations.add(mapConversation(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching conversations: {}", e.getMessage(), e);
        }
        return conversations;
    }

    // ═══════════════════════════════════════════════════════════
    //  OPÉRATION CREATE — Envoyer un message
    // ═══════════════════════════════════════════════════════════

    /**
     * Envoie un nouveau message dans une conversation.
     * Insère le message et met à jour la date du dernier message
     * dans la table conversations (pour le tri).
     *
     * @param conversationId l'ID de la conversation
     * @param senderId       l'ID de l'expéditeur
     * @param content        le contenu textuel du message
     * @return l'ID du message envoyé, ou -1 en cas d'échec
     */
    public int sendMessage(int conversationId, int senderId, String content) {
        String sql = "INSERT INTO messages (conversation_id, sender_id, content, created_date) VALUES (?, ?, ?, NOW())";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, conversationId);    // Conversation cible
            stmt.setInt(2, senderId);          // Expéditeur
            stmt.setString(3, content);        // Contenu du message
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                // Mettre à jour la date du dernier message dans la conversation (pour le tri)
                String updateSql = "UPDATE conversations SET last_message_date = NOW() WHERE id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, conversationId);
                    updateStmt.executeUpdate();
                }
                return id; // Retourne l'ID du message créé
            }
        } catch (SQLException e) {
            logger.error("Error sending message: {}", e.getMessage(), e);
        }
        return -1; // Échec de l'envoi
    }

    // ═══════════════════════════════════════════════════════════
    //  OPÉRATION READ — Lire les messages
    // ═══════════════════════════════════════════════════════════

    /**
     * Récupère les messages d'une conversation avec pagination.
     * Les messages sont triés par date croissante (ASC) pour l'affichage en chat.
     * JOIN avec users pour récupérer le nom de l'expéditeur.
     *
     * @param conversationId l'ID de la conversation
     * @param page           numéro de page (commence à 1)
     * @param pageSize       nombre de messages par page
     * @return liste de messages triés chronologiquement
     */
    public List<Message> getMessages(int conversationId, int page, int pageSize) {
        List<Message> messages = new ArrayList<>();
        int offset = (page - 1) * pageSize; // Calcul de l'offset pour la pagination
        
        String sql = """
            SELECT m.*, u.full_name as sender_name
            FROM messages m
            JOIN users u ON m.sender_id = u.id
            WHERE m.conversation_id = ?
            ORDER BY m.created_date ASC
            LIMIT ? OFFSET ?
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, conversationId);
            stmt.setInt(2, pageSize);
            stmt.setInt(3, offset);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                messages.add(mapMessage(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching messages: {}", e.getMessage(), e);
        }
        return messages;
    }

    /**
     * Marque tous les messages reçus comme lus dans une conversation.
     * Met is_read = TRUE pour les messages où sender_id != userId
     * (on ne marque pas ses propres messages comme lus).
     *
     * @param conversationId l'ID de la conversation
     * @param userId         l'ID de l'utilisateur qui lit la conversation
     * @return true si l'opération a réussi
     */
    public boolean markAsRead(int conversationId, int userId) {
        // WHERE sender_id != ? : ne marquer que les messages des AUTRES utilisateurs
        String sql = "UPDATE messages SET is_read = TRUE WHERE conversation_id = ? AND sender_id != ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, conversationId);
            stmt.setInt(2, userId);       // Exclure ses propres messages
            return stmt.executeUpdate() >= 0; // >= 0 car peut être 0 si déjà tout lu
        } catch (SQLException e) {
            logger.error("Error marking as read: {}", e.getMessage(), e);
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════
    //  OPÉRATION UPDATE — Modifier un message
    // ═══════════════════════════════════════════════════════════

    /**
     * Modifie le contenu d'un message existant.
     * CONTRÔLE DE SÉCURITÉ : WHERE sender_id = ? garantit que seul
     * l'expéditeur original peut modifier son propre message.
     * Si un autre utilisateur essaie, executeUpdate() retourne 0 (aucune ligne modifiée).
     *
     * @param messageId  l'ID du message à modifier
     * @param senderId   l'ID de l'utilisateur qui demande la modification (contrôle d'accès)
     * @param newContent le nouveau contenu textuel
     * @return true si la modification a réussi, false si refusé ou échec
     */
    public boolean updateMessage(int messageId, int senderId, String newContent) {
        // WHERE sender_id = ? : contrôle de saisie — seul l'expéditeur peut modifier
        String sql = "UPDATE messages SET content = ? WHERE id = ? AND sender_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newContent);    // Nouveau contenu
            stmt.setInt(2, messageId);        // ID du message
            stmt.setInt(3, senderId);         // Vérification : est-ce bien l'expéditeur ?
            return stmt.executeUpdate() > 0;  // true si 1 ligne modifiée (autorisé)
        } catch (SQLException e) {
            logger.error("Error updating message: {}", e.getMessage(), e);
        }
        return false; // Modification refusée ou échec
    }

    // ═══════════════════════════════════════════════════════════
    //  OPÉRATION DELETE — Supprimer un message
    // ═══════════════════════════════════════════════════════════

    /**
     * Supprime un message de la base de données.
     * CONTRÔLE DE SÉCURITÉ : WHERE sender_id = ? garantit que seul
     * l'expéditeur original peut supprimer son propre message.
     *
     * @param messageId l'ID du message à supprimer
     * @param senderId  l'ID de l'utilisateur qui demande la suppression (contrôle d'accès)
     * @return true si la suppression a réussi, false si refusé ou échec
     */
    public boolean deleteMessage(int messageId, int senderId) {
        // WHERE sender_id = ? : contrôle de saisie — seul l'expéditeur peut supprimer
        String sql = "DELETE FROM messages WHERE id = ? AND sender_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, messageId);        // ID du message à supprimer
            stmt.setInt(2, senderId);         // Vérification : est-ce bien l'expéditeur ?
            return stmt.executeUpdate() > 0;  // true si 1 ligne supprimée (autorisé)
        } catch (SQLException e) {
            logger.error("Error deleting message: {}", e.getMessage(), e);
        }
        return false; // Suppression refusée ou échec
    }

    /**
     * Compte le nombre total de messages non lus pour un utilisateur.
     * Cherche dans toutes ses conversations les messages où :
     *   - sender_id != userId (messages des autres)
     *   - is_read = FALSE (pas encore lu)
     *
     * @param userId l'ID de l'utilisateur
     * @return le nombre de messages non lus
     */
    public int getUnreadCount(int userId) {
        // Requête avec JOIN pour filtrer uniquement les conversations de l'utilisateur
        String sql = """
            SELECT COUNT(*) FROM messages m
            JOIN conversations c ON m.conversation_id = c.id
            WHERE (c.participant_1 = ? OR c.participant_2 = ?)
            AND m.sender_id != ?
            AND m.is_read = FALSE
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error getting unread count: {}", e.getMessage(), e);
        }
        return 0;
    }

    // ═══════════════════════════════════════════════════════════
    //  MAPPING — Conversion ResultSet → Objet Java
    // ═══════════════════════════════════════════════════════════

    /**
     * Convertit une ligne du ResultSet en objet Conversation.
     * Mappe les colonnes de la table conversations + les champs transitoires
     * (other_name, other_photo, last_msg, unread) issus des JOINs et sous-requêtes.
     */
    private Conversation mapConversation(ResultSet rs) throws SQLException {
        Conversation conv = new Conversation();
        conv.setId(rs.getInt("id"));
        conv.setParticipant1(rs.getInt("participant_1"));
        conv.setParticipant2(rs.getInt("participant_2"));
        conv.setArchived1(rs.getBoolean("is_archived_1"));
        conv.setArchived2(rs.getBoolean("is_archived_2"));
        
        // Conversion Timestamp SQL → LocalDateTime Java
        Timestamp lastMsg = rs.getTimestamp("last_message_date");
        if (lastMsg != null) conv.setLastMessageDate(lastMsg.toLocalDateTime());
        
        Timestamp created = rs.getTimestamp("created_date");
        if (created != null) conv.setCreatedDate(created.toLocalDateTime());
        
        // Champs transitoires (non stockés en base, calculés par la requête)
        conv.setOtherUserName(rs.getString("other_name"));       // Nom de l'autre participant
        conv.setOtherUserPhoto(rs.getString("other_photo"));     // Photo de l'autre participant
        conv.setLastMessagePreview(rs.getString("last_msg"));    // Aperçu du dernier message
        conv.setUnreadCount(rs.getInt("unread"));                // Nombre de messages non lus
        
        return conv;
    }

    /**
     * Convertit une ligne du ResultSet en objet Message.
     * Mappe les colonnes de la table messages + le sender_name du JOIN avec users.
     */
    private Message mapMessage(ResultSet rs) throws SQLException {
        Message msg = new Message();
        msg.setId(rs.getInt("id"));
        msg.setConversationId(rs.getInt("conversation_id"));
        msg.setSenderId(rs.getInt("sender_id"));
        msg.setContent(rs.getString("content"));
        msg.setRead(rs.getBoolean("is_read"));
        
        // Conversion Timestamp SQL → LocalDateTime Java
        Timestamp created = rs.getTimestamp("created_date");
        if (created != null) msg.setCreatedDate(created.toLocalDateTime());
        
        msg.setSenderName(rs.getString("sender_name")); // Nom de l'expéditeur (JOIN)
        
        return msg;
    }
}
