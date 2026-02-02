package com.skilora.community.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.community.enums.ConnectionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConnectionService {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionService.class);
    private static volatile ConnectionService instance;

    private ConnectionService() {}

    public static ConnectionService getInstance() {
        if (instance == null) {
            synchronized (ConnectionService.class) {
                if (instance == null) {
                    instance = new ConnectionService();
                }
            }
        }
        return instance;
    }

    public int sendRequest(int fromUserId, int toUserId) {
        // Ensure user1 < user2 for consistency
        int user1 = Math.min(fromUserId, toUserId);
        int user2 = Math.max(fromUserId, toUserId);
        
        String sql = "INSERT INTO connections (user_id_1, user_id_2, status, created_date) VALUES (?, ?, ?, NOW())";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, user1);
            stmt.setInt(2, user2);
            stmt.setString(3, ConnectionStatus.PENDING.name());
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                logger.info("Connection request sent from {} to {}", fromUserId, toUserId);
                return id;
            }
        } catch (SQLException e) {
            logger.error("Error sending connection request: {}", e.getMessage(), e);
        }
        return -1;
    }

    public boolean acceptRequest(int connectionId) {
        String sql = "UPDATE connections SET status = ?, last_interaction = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ConnectionStatus.ACCEPTED.name());
            stmt.setInt(2, connectionId);
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                logger.info("Connection {} accepted", connectionId);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error accepting connection: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean rejectRequest(int connectionId) {
        String sql = "UPDATE connections SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ConnectionStatus.REJECTED.name());
            stmt.setInt(2, connectionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error rejecting connection: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean removeConnection(int connectionId) {
        String sql = "DELETE FROM connections WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, connectionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error removing connection: {}", e.getMessage(), e);
        }
        return false;
    }

    public List<com.skilora.community.entity.Connection> getConnections(int userId) {
        List<com.skilora.community.entity.Connection> connections = new ArrayList<>();
        String sql = """
            SELECT c.*, 
                CASE 
                    WHEN c.user_id_1 = ? THEN u2.full_name 
                    ELSE u1.full_name 
                END as other_name,
                CASE 
                    WHEN c.user_id_1 = ? THEN u2.photo_url 
                    ELSE u1.photo_url 
                END as other_photo
            FROM connections c
            JOIN users u1 ON c.user_id_1 = u1.id
            JOIN users u2 ON c.user_id_2 = u2.id
            WHERE (c.user_id_1 = ? OR c.user_id_2 = ?) AND c.status = ?
            ORDER BY c.created_date DESC
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setInt(4, userId);
            stmt.setString(5, ConnectionStatus.ACCEPTED.name());
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                connections.add(mapConnection(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching connections: {}", e.getMessage(), e);
        }
        return connections;
    }

    public List<com.skilora.community.entity.Connection> getPendingRequests(int userId) {
        List<com.skilora.community.entity.Connection> requests = new ArrayList<>();
        String sql = """
            SELECT c.*, u1.full_name as other_name, u1.photo_url as other_photo
            FROM connections c
            JOIN users u1 ON c.user_id_1 = u1.id
            WHERE c.user_id_2 = ? AND c.status = ?
            ORDER BY c.created_date DESC
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, ConnectionStatus.PENDING.name());
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                requests.add(mapConnection(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching pending requests: {}", e.getMessage(), e);
        }
        return requests;
    }

    public boolean areConnected(int userId1, int userId2) {
        int user1 = Math.min(userId1, userId2);
        int user2 = Math.max(userId1, userId2);
        
        String sql = "SELECT 1 FROM connections WHERE user_id_1 = ? AND user_id_2 = ? AND status = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user1);
            stmt.setInt(2, user2);
            stmt.setString(3, ConnectionStatus.ACCEPTED.name());
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.error("Error checking connection: {}", e.getMessage(), e);
        }
        return false;
    }

    public int getConnectionCount(int userId) {
        String sql = "SELECT COUNT(*) FROM connections WHERE (user_id_1 = ? OR user_id_2 = ?) AND status = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setString(3, ConnectionStatus.ACCEPTED.name());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error getting connection count: {}", e.getMessage(), e);
        }
        return 0;
    }

    public List<com.skilora.community.entity.Connection> getSuggestions(int userId, int limit) {
        List<com.skilora.community.entity.Connection> suggestions = new ArrayList<>();
        String sql = """
            SELECT u.id as user_id, u.full_name, u.photo_url
            FROM users u
            WHERE u.id != ? 
            AND u.id NOT IN (
                SELECT user_id_2 FROM connections WHERE user_id_1 = ?
                UNION
                SELECT user_id_1 FROM connections WHERE user_id_2 = ?
            )
            ORDER BY RAND()
            LIMIT ?
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setInt(4, limit);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                com.skilora.community.entity.Connection suggestion = new com.skilora.community.entity.Connection();
                suggestion.setUserId1(userId);
                suggestion.setUserId2(rs.getInt("user_id"));
                suggestion.setOtherUserName(rs.getString("full_name"));
                suggestion.setOtherUserPhoto(rs.getString("photo_url"));
                suggestions.add(suggestion);
            }
        } catch (SQLException e) {
            logger.error("Error fetching suggestions: {}", e.getMessage(), e);
        }
        return suggestions;
    }

    private com.skilora.community.entity.Connection mapConnection(ResultSet rs) throws SQLException {
        com.skilora.community.entity.Connection connection = new com.skilora.community.entity.Connection();
        connection.setId(rs.getInt("id"));
        connection.setUserId1(rs.getInt("user_id_1"));
        connection.setUserId2(rs.getInt("user_id_2"));
        connection.setStatus(ConnectionStatus.valueOf(rs.getString("status")));
        connection.setConnectionType(rs.getString("connection_type"));
        connection.setStrengthScore(rs.getInt("strength_score"));
        
        Timestamp created = rs.getTimestamp("created_date");
        if (created != null) connection.setCreatedDate(created.toLocalDateTime());
        
        Timestamp lastInteraction = rs.getTimestamp("last_interaction");
        if (lastInteraction != null) connection.setLastInteraction(lastInteraction.toLocalDateTime());
        
        connection.setOtherUserName(rs.getString("other_name"));
        connection.setOtherUserPhoto(rs.getString("other_photo"));
        
        return connection;
    }
}
