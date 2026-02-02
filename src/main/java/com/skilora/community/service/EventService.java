package com.skilora.community.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.community.entity.Event;
import com.skilora.community.entity.EventRsvp;
import com.skilora.community.enums.EventStatus;
import com.skilora.community.enums.EventType;
import com.skilora.formation.service.AchievementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);
    private static volatile EventService instance;

    private EventService() {}

    public static EventService getInstance() {
        if (instance == null) {
            synchronized (EventService.class) {
                if (instance == null) {
                    instance = new EventService();
                }
            }
        }
        return instance;
    }

    public int create(Event event) {
        String sql = "INSERT INTO events (organizer_id, title, description, event_type, location, is_online, online_link, " +
                    "start_date, end_date, max_attendees, image_url, status, created_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, event.getOrganizerId());
            stmt.setString(2, event.getTitle());
            stmt.setString(3, event.getDescription());
            stmt.setString(4, event.getEventType().name());
            stmt.setString(5, event.getLocation());
            stmt.setBoolean(6, event.isOnline());
            stmt.setString(7, event.getOnlineLink());
            stmt.setTimestamp(8, Timestamp.valueOf(event.getStartDate()));
            stmt.setTimestamp(9, event.getEndDate() != null ? Timestamp.valueOf(event.getEndDate()) : null);
            stmt.setInt(10, event.getMaxAttendees());
            stmt.setString(11, event.getImageUrl());
            stmt.setString(12, event.getStatus().name());
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                logger.info("Event created with id {}", id);
                
                // Award achievement
                AchievementService.getInstance().checkAndAward(event.getOrganizerId());
                return id;
            }
        } catch (SQLException e) {
            logger.error("Error creating event: {}", e.getMessage(), e);
        }
        return -1;
    }

    public Event findById(int id) {
        String sql = """
            SELECT e.*, u.full_name as organizer_name
            FROM events e
            JOIN users u ON e.organizer_id = u.id
            WHERE e.id = ?
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapEvent(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding event: {}", e.getMessage(), e);
        }
        return null;
    }

    public List<Event> findUpcoming() {
        List<Event> events = new ArrayList<>();
        String sql = """
            SELECT e.*, u.full_name as organizer_name
            FROM events e
            JOIN users u ON e.organizer_id = u.id
            WHERE e.start_date > NOW() AND e.status = ?
            ORDER BY e.start_date ASC
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, EventStatus.UPCOMING.name());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                events.add(mapEvent(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching upcoming events: {}", e.getMessage(), e);
        }
        return events;
    }

    public List<Event> findByOrganizer(int organizerId) {
        List<Event> events = new ArrayList<>();
        String sql = """
            SELECT e.*, u.full_name as organizer_name
            FROM events e
            JOIN users u ON e.organizer_id = u.id
            WHERE e.organizer_id = ?
            ORDER BY e.start_date DESC
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, organizerId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                events.add(mapEvent(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching organizer events: {}", e.getMessage(), e);
        }
        return events;
    }

    public List<Event> findByAttendee(int userId) {
        List<Event> events = new ArrayList<>();
        String sql = """
            SELECT e.*, u.full_name as organizer_name
            FROM events e
            JOIN users u ON e.organizer_id = u.id
            JOIN event_rsvps r ON e.id = r.event_id
            WHERE r.user_id = ? AND r.status = 'GOING'
            ORDER BY e.start_date ASC
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                events.add(mapEvent(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching attendee events: {}", e.getMessage(), e);
        }
        return events;
    }

    public boolean update(Event event) {
        String sql = "UPDATE events SET title = ?, description = ?, event_type = ?, location = ?, is_online = ?, " +
                    "online_link = ?, start_date = ?, end_date = ?, max_attendees = ?, image_url = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, event.getTitle());
            stmt.setString(2, event.getDescription());
            stmt.setString(3, event.getEventType().name());
            stmt.setString(4, event.getLocation());
            stmt.setBoolean(5, event.isOnline());
            stmt.setString(6, event.getOnlineLink());
            stmt.setTimestamp(7, Timestamp.valueOf(event.getStartDate()));
            stmt.setTimestamp(8, event.getEndDate() != null ? Timestamp.valueOf(event.getEndDate()) : null);
            stmt.setInt(9, event.getMaxAttendees());
            stmt.setString(10, event.getImageUrl());
            stmt.setString(11, event.getStatus().name());
            stmt.setInt(12, event.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating event: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean cancel(int id) {
        String sql = "UPDATE events SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, EventStatus.CANCELLED.name());
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error cancelling event: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM events WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting event: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean rsvp(int eventId, int userId, String status) {
        String sql = "INSERT INTO event_rsvps (event_id, user_id, status, rsvp_date) VALUES (?, ?, ?, NOW()) " +
                    "ON DUPLICATE KEY UPDATE status = ?, rsvp_date = NOW()";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.setInt(2, userId);
            stmt.setString(3, status);
            stmt.setString(4, status);
            
            if (stmt.executeUpdate() > 0) {
                // Update attendee count if status is GOING
                if ("GOING".equals(status)) {
                    String updateSql = "UPDATE events SET current_attendees = (SELECT COUNT(*) FROM event_rsvps WHERE event_id = ? AND status = 'GOING') WHERE id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, eventId);
                        updateStmt.setInt(2, eventId);
                        updateStmt.executeUpdate();
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error RSVP to event: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean cancelRsvp(int eventId, int userId) {
        String sql = "DELETE FROM event_rsvps WHERE event_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.setInt(2, userId);
            
            if (stmt.executeUpdate() > 0) {
                // Update count
                String updateSql = "UPDATE events SET current_attendees = (SELECT COUNT(*) FROM event_rsvps WHERE event_id = ? AND status = 'GOING') WHERE id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, eventId);
                    updateStmt.setInt(2, eventId);
                    updateStmt.executeUpdate();
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error cancelling RSVP: {}", e.getMessage(), e);
        }
        return false;
    }

    public List<EventRsvp> getAttendees(int eventId) {
        List<EventRsvp> attendees = new ArrayList<>();
        String sql = "SELECT * FROM event_rsvps WHERE event_id = ? ORDER BY rsvp_date DESC";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                attendees.add(mapRsvp(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching attendees: {}", e.getMessage(), e);
        }
        return attendees;
    }

    public boolean isAttending(int eventId, int userId) {
        String sql = "SELECT 1 FROM event_rsvps WHERE event_id = ? AND user_id = ? AND status = 'GOING'";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.error("Error checking attendance: {}", e.getMessage(), e);
        }
        return false;
    }

    private Event mapEvent(ResultSet rs) throws SQLException {
        Event event = new Event();
        event.setId(rs.getInt("id"));
        event.setOrganizerId(rs.getInt("organizer_id"));
        event.setTitle(rs.getString("title"));
        event.setDescription(rs.getString("description"));
        event.setEventType(EventType.valueOf(rs.getString("event_type")));
        event.setLocation(rs.getString("location"));
        event.setOnline(rs.getBoolean("is_online"));
        event.setOnlineLink(rs.getString("online_link"));
        event.setMaxAttendees(rs.getInt("max_attendees"));
        event.setCurrentAttendees(rs.getInt("current_attendees"));
        event.setImageUrl(rs.getString("image_url"));
        event.setStatus(EventStatus.valueOf(rs.getString("status")));
        
        Timestamp start = rs.getTimestamp("start_date");
        if (start != null) event.setStartDate(start.toLocalDateTime());
        
        Timestamp end = rs.getTimestamp("end_date");
        if (end != null) event.setEndDate(end.toLocalDateTime());
        
        Timestamp created = rs.getTimestamp("created_date");
        if (created != null) event.setCreatedDate(created.toLocalDateTime());
        
        event.setOrganizerName(rs.getString("organizer_name"));
        
        return event;
    }

    private EventRsvp mapRsvp(ResultSet rs) throws SQLException {
        EventRsvp rsvp = new EventRsvp();
        rsvp.setId(rs.getInt("id"));
        rsvp.setEventId(rs.getInt("event_id"));
        rsvp.setUserId(rs.getInt("user_id"));
        rsvp.setStatus(rs.getString("status"));
        
        Timestamp rsvpDate = rs.getTimestamp("rsvp_date");
        if (rsvpDate != null) rsvp.setRsvpDate(rsvpDate.toLocalDateTime());
        
        return rsvp;
    }
}
