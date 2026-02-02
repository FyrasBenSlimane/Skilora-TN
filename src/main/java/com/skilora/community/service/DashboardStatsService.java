package com.skilora.community.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DashboardStatsService - Provides real-time statistics from the database
 * for all three dashboard roles (ADMIN, EMPLOYER, USER).
 */
public class DashboardStatsService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardStatsService.class);
    private static DashboardStatsService instance;

    private DashboardStatsService() {}

    public static synchronized DashboardStatsService getInstance() {
        if (instance == null) instance = new DashboardStatsService();
        return instance;
    }

    // ── Admin Stats ──

    public int getTotalUsers() {
        return countQuery("SELECT COUNT(*) FROM users WHERE is_active = 1");
    }

    public int getNewUsersThisMonth() {
        return countQuery("SELECT COUNT(*) FROM users WHERE created_at >= DATE_FORMAT(NOW(), '%Y-%m-01')");
    }

    public int getTotalActiveOffers() {
        return countQuery("SELECT COUNT(*) FROM job_offers WHERE status IN ('OPEN', 'ACTIVE')");
    }

    public int getOpenTickets() {
        return countQuery("SELECT COUNT(*) FROM support_tickets WHERE status IN ('OPEN', 'IN_PROGRESS')");
    }

    public double getTotalPayrollThisMonth() {
        String sql = "SELECT COALESCE(SUM(gross_salary), 0) FROM payslips " +
                     "WHERE period_month = MONTH(NOW()) AND period_year = YEAR(NOW())";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            logger.error("Failed to get total payroll", e);
        }
        return 0;
    }

    // ── Employer Stats ──

    public int getOfferViewsForEmployer(int userId) {
        // Count total applications across employer's offers as a proxy for "views"
        return countQuery("SELECT COUNT(*) FROM applications a " +
                         "JOIN job_offers jo ON a.job_offer_id = jo.id " +
                         "JOIN companies c ON jo.company_id = c.id " +
                         "WHERE c.owner_id = ?", userId);
    }

    public int getApplicationsForEmployer(int userId) {
        return countQuery("SELECT COUNT(*) FROM applications a " +
                         "JOIN job_offers jo ON a.job_offer_id = jo.id " +
                         "JOIN companies c ON jo.company_id = c.id " +
                         "WHERE c.owner_id = ? AND a.status = 'PENDING'", userId);
    }

    public int getInterviewsThisWeek(int userId) {
        return countQuery("SELECT COUNT(*) FROM interviews i " +
                         "JOIN applications a ON i.application_id = a.id " +
                         "JOIN job_offers jo ON a.job_offer_id = jo.id " +
                         "JOIN companies c ON jo.company_id = c.id " +
                         "WHERE c.owner_id = ? AND i.scheduled_date >= NOW() " +
                         "AND i.scheduled_date <= DATE_ADD(NOW(), INTERVAL 7 DAY)", userId);
    }

    // ── User Stats ──

    public int getUserApplicationCount(int userId) {
        return countQuery("SELECT COUNT(*) FROM applications a " +
                         "JOIN profiles p ON a.candidate_profile_id = p.id " +
                         "WHERE p.user_id = ?", userId);
    }

    public int getUserActiveApplications(int userId) {
        return countQuery("SELECT COUNT(*) FROM applications a " +
                         "JOIN profiles p ON a.candidate_profile_id = p.id " +
                         "WHERE p.user_id = ? AND a.status NOT IN ('REJECTED', 'ACCEPTED')", userId);
    }

    public int getUserEnrollmentCount(int userId) {
        return countQuery("SELECT COUNT(*) FROM enrollments WHERE user_id = ?", userId);
    }

    public int getUserConnectionCount(int userId) {
        return countQuery("SELECT COUNT(*) FROM connections WHERE (user_id_1 = ? OR user_id_2 = ?) " +
                         "AND status = 'ACCEPTED'", userId, userId);
    }

    // ── Recent Activity from DB ──

    public List<ActivityItem> getRecentActivity(User user, int limit) {
        List<ActivityItem> items = new ArrayList<>();

        switch (user.getRole()) {
            case ADMIN:
                items.addAll(getAdminActivity(limit));
                break;
            case EMPLOYER:
                items.addAll(getEmployerActivity(user.getId(), limit));
                break;
            default:
                items.addAll(getUserActivity(user.getId(), limit));
                break;
        }

        return items;
    }

    private List<ActivityItem> getAdminActivity(int limit) {
        List<ActivityItem> items = new ArrayList<>();

        // Recent login attempts
        String sql = "SELECT username, attempted_at, success FROM login_attempts " +
                     "ORDER BY attempted_at DESC LIMIT ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String user = rs.getString("username");
                    boolean success = rs.getBoolean("success");
                    LocalDateTime time = rs.getTimestamp("attempted_at").toLocalDateTime();
                    String type = success ? "login" : "security";
                    String desc = success
                            ? user + " logged in successfully"
                            : "Failed login attempt for " + user;
                    items.add(new ActivityItem(type, desc, time));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get admin activity", e);
        }

        // Recent tickets
        String ticketSql = "SELECT st.subject, st.created_date, u.full_name FROM support_tickets st " +
                          "JOIN users u ON st.user_id = u.id ORDER BY st.created_date DESC LIMIT ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(ticketSql)) {
            ps.setInt(1, 3);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String subject = rs.getString("subject");
                    String name = rs.getString("full_name");
                    LocalDateTime time = rs.getTimestamp("created_date").toLocalDateTime();
                    items.add(new ActivityItem("support", name + ": " + subject, time));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get ticket activity", e);
        }

        // Sort by time descending and limit
        items.sort((a, b) -> b.getTime().compareTo(a.getTime()));
        return items.size() > limit ? items.subList(0, limit) : items;
    }

    private List<ActivityItem> getEmployerActivity(int userId, int limit) {
        List<ActivityItem> items = new ArrayList<>();

        String sql = "SELECT a.applied_date, a.status, jo.title, " +
                     "COALESCE(u.full_name, CONCAT('Candidate #', a.candidate_profile_id)) as candidate_name " +
                     "FROM applications a " +
                     "JOIN job_offers jo ON a.job_offer_id = jo.id " +
                     "JOIN companies c ON jo.company_id = c.id " +
                     "LEFT JOIN profiles p ON a.candidate_profile_id = p.id " +
                     "LEFT JOIN users u ON p.user_id = u.id " +
                     "WHERE c.owner_id = ? ORDER BY a.applied_date DESC LIMIT ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String candidate = rs.getString("candidate_name");
                    String jobTitle = rs.getString("title");
                    LocalDateTime time = rs.getTimestamp("applied_date").toLocalDateTime();
                    items.add(new ActivityItem("application",
                            candidate + " applied for \"" + jobTitle + "\"", time));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get employer activity", e);
        }

        return items;
    }

    private List<ActivityItem> getUserActivity(int userId, int limit) {
        List<ActivityItem> items = new ArrayList<>();

        // Recent applications
        String sql = "SELECT a.applied_date, a.status, jo.title FROM applications a " +
                     "JOIN job_offers jo ON a.job_offer_id = jo.id " +
                     "JOIN profiles p ON a.candidate_profile_id = p.id " +
                     "WHERE p.user_id = ? ORDER BY a.applied_date DESC LIMIT ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String jobTitle = rs.getString("title");
                    String status = rs.getString("status");
                    LocalDateTime time = rs.getTimestamp("applied_date").toLocalDateTime();
                    items.add(new ActivityItem("application",
                            "Applied for \"" + jobTitle + "\" - " + status, time));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get user activity", e);
        }

        // Recent enrollments
        String enrollSql = "SELECT e.enrolled_date, f.title FROM enrollments e " +
                          "JOIN formations f ON e.formation_id = f.id WHERE e.user_id = ? " +
                          "ORDER BY e.enrolled_date DESC LIMIT ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(enrollSql)) {
            ps.setInt(1, userId);
            ps.setInt(2, 3);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String title = rs.getString("title");
                    LocalDateTime time = rs.getTimestamp("enrolled_date").toLocalDateTime();
                    items.add(new ActivityItem("formation", "Enrolled in \"" + title + "\"", time));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get enrollment activity", e);
        }

        items.sort((a, b) -> b.getTime().compareTo(a.getTime()));
        return items.size() > limit ? items.subList(0, limit) : items;
    }

    // ── Helpers ──

    private int countQuery(String sql, int... params) {
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setInt(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Count query failed: {}", sql, e);
        }
        return 0;
    }

    // ── Activity Item ──

    public static class ActivityItem {
        private final String type;
        private final String description;
        private final LocalDateTime time;

        public ActivityItem(String type, String description, LocalDateTime time) {
            this.type = type;
            this.description = description;
            this.time = time;
        }

        public String getType() { return type; }
        public String getDescription() { return description; }
        public LocalDateTime getTime() { return time; }
    }
}
