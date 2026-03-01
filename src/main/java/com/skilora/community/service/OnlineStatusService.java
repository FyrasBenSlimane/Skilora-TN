package com.skilora.community.service;

import com.skilora.config.DatabaseConfig;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * OnlineStatusService — Gestion du statut en ligne/hors ligne des utilisateurs (temps réel).
 *
 * Fonctionnement :
 *   - Envoie un "heartbeat" (battement de cœur) toutes les 10 secondes dans la table user_online_status.
 *   - Un utilisateur est considéré "en ligne" si son dernier heartbeat date de moins de 20 secondes.
 *   - Quand l'application se ferme, le statut est mis à jour pour marquer l'utilisateur hors ligne.
 *
 * Architecture :
 *   - Table BDD : user_online_status (user_id PK, last_seen DATETIME)
 *   - Heartbeat automatique via JavaFX Timeline (cycle de 10s)
 *   - Méthode statique isUserOnline(userId) pour vérifier le statut en temps réel
 *
 * Pattern : Singleton thread-safe (Double-Checked Locking)
 */
public class OnlineStatusService {

    private static final Logger logger = LoggerFactory.getLogger(OnlineStatusService.class);

    // ── Singleton ──
    private static volatile OnlineStatusService instance;

    /** Seuil en secondes : un utilisateur est "en ligne" si last_seen < ONLINE_THRESHOLD secondes */
    private static final int ONLINE_THRESHOLD_SECONDS = 20;

    /** Intervalle du heartbeat en secondes */
    private static final int HEARTBEAT_INTERVAL_SECONDS = 10;

    private int currentUserId = -1;
    private Timeline heartbeatTimeline;

    private OnlineStatusService() {}

    /**
     * Retourne l'instance unique du OnlineStatusService.
     */
    public static OnlineStatusService getInstance() {
        if (instance == null) {
            synchronized (OnlineStatusService.class) {
                if (instance == null) {
                    instance = new OnlineStatusService();
                }
            }
        }
        return instance;
    }

    // ═══════════════════════════════════════════════════════════
    //  LIFECYCLE — Démarrage et arrêt du heartbeat
    // ═══════════════════════════════════════════════════════════

    /**
     * Démarre le heartbeat automatique pour l'utilisateur connecté.
     * Envoie immédiatement un premier heartbeat, puis un toutes les 10 secondes.
     *
     * @param userId l'ID de l'utilisateur connecté
     */
    public void startHeartbeat(int userId) {
        this.currentUserId = userId;

        // Arrêter un éventuel heartbeat précédent
        stopHeartbeat();

        // Premier heartbeat immédiat
        sendHeartbeat();

        // Timeline JavaFX qui envoie un heartbeat toutes les 10 secondes
        heartbeatTimeline = new Timeline(new KeyFrame(
                Duration.seconds(HEARTBEAT_INTERVAL_SECONDS),
                e -> sendHeartbeat()
        ));
        heartbeatTimeline.setCycleCount(Animation.INDEFINITE);
        heartbeatTimeline.play();

        logger.info("Online heartbeat started for user {}", userId);
    }

    /**
     * Arrête le heartbeat et marque l'utilisateur comme hors ligne.
     * Appelé quand l'application se ferme ou quand l'utilisateur se déconnecte.
     */
    public void stopHeartbeat() {
        if (heartbeatTimeline != null) {
            heartbeatTimeline.stop();
            heartbeatTimeline = null;
        }
        if (currentUserId > 0) {
            // Mettre last_seen dans le passé pour marquer hors ligne immédiatement
            markOffline(currentUserId);
            logger.info("User {} marked offline", currentUserId);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  HEARTBEAT — Envoi du signal de présence
    // ═══════════════════════════════════════════════════════════

    /**
     * Envoie un heartbeat (mise à jour de last_seen à NOW()).
     * Utilise INSERT ... ON DUPLICATE KEY UPDATE pour créer/mettre à jour en une seule requête.
     */
    private void sendHeartbeat() {
        if (currentUserId <= 0) return;

        new Thread(() -> {
            String sql = "INSERT INTO user_online_status (user_id, last_seen) VALUES (?, NOW()) "
                       + "ON DUPLICATE KEY UPDATE last_seen = NOW()";
            try (Connection conn = DatabaseConfig.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentUserId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                logger.debug("Heartbeat error: {}", e.getMessage());
            }
        }, "HeartbeatThread").start();
    }

    /**
     * Marque un utilisateur comme hors ligne en mettant last_seen 1 heure dans le passé.
     * Appelé lors de la fermeture de l'application.
     *
     * @param userId l'ID de l'utilisateur à marquer hors ligne
     */
    public void markOffline(int userId) {
        String sql = "INSERT INTO user_online_status (user_id, last_seen) VALUES (?, DATE_SUB(NOW(), INTERVAL 1 HOUR)) "
                   + "ON DUPLICATE KEY UPDATE last_seen = DATE_SUB(NOW(), INTERVAL 1 HOUR)";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.debug("Mark offline error: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  VÉRIFICATION — Statut en ligne d'un utilisateur
    // ═══════════════════════════════════════════════════════════

    /**
     * Vérifie si un utilisateur est actuellement en ligne.
     * Un utilisateur est considéré en ligne si son dernier heartbeat
     * date de moins de 20 secondes (ONLINE_THRESHOLD_SECONDS).
     *
     * @param userId l'ID de l'utilisateur à vérifier
     * @return true si l'utilisateur est en ligne
     */
    public boolean isUserOnline(int userId) {
        String sql = "SELECT COUNT(*) FROM user_online_status WHERE user_id = ? "
                   + "AND TIMESTAMPDIFF(SECOND, last_seen, NOW()) < ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, ONLINE_THRESHOLD_SECONDS);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.debug("Check online error: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Récupère la dernière connexion d'un utilisateur.
     * Utilisé pour afficher "Vu la dernière fois à ..." quand hors ligne.
     *
     * @param userId l'ID de l'utilisateur
     * @return le LocalDateTime de la dernière connexion, ou null si jamais connecté
     */
    public LocalDateTime getLastSeen(int userId) {
        String sql = "SELECT last_seen FROM user_online_status WHERE user_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("last_seen");
                if (ts != null) return ts.toLocalDateTime();
            }
        } catch (SQLException e) {
            logger.debug("Get last seen error: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Formate le texte du statut pour l'affichage UI.
     * - En ligne → "En ligne"
     * - Hors ligne → "Vu il y a X minutes/heures"
     *
     * @param userId l'ID de l'utilisateur
     * @return texte formaté du statut
     */
    public String getStatusText(int userId) {
        if (isUserOnline(userId)) {
            return "En ligne";
        }
        LocalDateTime lastSeen = getLastSeen(userId);
        if (lastSeen == null) {
            return "Hors ligne";
        }

        java.time.Duration diff = java.time.Duration.between(lastSeen, LocalDateTime.now());
        long minutes = diff.toMinutes();

        if (minutes < 1) return "Vu à l'instant";
        if (minutes < 60) return "Vu il y a " + minutes + " min";
        long hours = diff.toHours();
        if (hours < 24) return "Vu il y a " + hours + "h";
        long days = diff.toDays();
        return "Vu il y a " + days + "j";
    }

    /**
     * Retourne l'ID de l'utilisateur courant.
     */
    public int getCurrentUserId() {
        return currentUserId;
    }
}
