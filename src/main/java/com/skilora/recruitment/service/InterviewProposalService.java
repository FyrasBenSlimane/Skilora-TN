package com.skilora.recruitment.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.recruitment.entity.InterviewProposal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * InterviewProposalService — Manages candidate-initiated interview time proposals.
 *
 * Flow:
 * 1. Candidate submits a proposal with preferred date/time + type via {@link #create}
 * 2. Employer sees pending proposals via {@link #getPendingForEmployer}
 * 3. Employer accepts (creates Interview) or rejects via {@link #accept} / {@link #reject}
 * 4. Accepted proposals auto-create an Interview record in the interviews table.
 */
public class InterviewProposalService {

    private static final Logger logger = LoggerFactory.getLogger(InterviewProposalService.class);
    private static volatile InterviewProposalService instance;

    private InterviewProposalService() {}

    public static InterviewProposalService getInstance() {
        if (instance == null) {
            synchronized (InterviewProposalService.class) {
                if (instance == null) instance = new InterviewProposalService();
            }
        }
        return instance;
    }

    /**
     * Create a new interview time proposal from a candidate.
     *
     * @return generated ID, or -1 on failure
     */
    public int create(InterviewProposal proposal) {
        String sql = "INSERT INTO interview_proposals " +
                     "(application_id, proposed_by, proposed_date, duration_minutes, type, message, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 'PENDING')";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, proposal.getApplicationId());
            ps.setInt(2, proposal.getProposedBy());
            ps.setTimestamp(3, Timestamp.valueOf(proposal.getProposedDate()));
            ps.setInt(4, proposal.getDurationMinutes());
            ps.setString(5, proposal.getType());
            ps.setString(6, proposal.getMessage());
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                logger.info("Interview proposal created: id={}, app={}, date={}",
                        id, proposal.getApplicationId(), proposal.getProposedDate());
                return id;
            }
        } catch (SQLException e) {
            logger.error("Failed to create interview proposal", e);
        }
        return -1;
    }

    /**
     * Get all pending proposals for an employer, enriched with candidate name and job title.
     */
    public List<InterviewProposal> getPendingForEmployer(int employerUserId) {
        String sql = """
            SELECT ip.*, u.full_name AS candidate_name, jo.title AS job_title
            FROM interview_proposals ip
            JOIN applications a ON ip.application_id = a.id
            JOIN job_offers jo ON a.job_offer_id = jo.id
            JOIN companies c ON jo.company_id = c.id
            LEFT JOIN profiles p ON a.candidate_profile_id = p.id
            LEFT JOIN users u ON p.user_id = u.id
            WHERE c.owner_id = ? AND ip.status = 'PENDING'
            ORDER BY ip.proposed_date ASC
            """;
        return queryProposals(sql, employerUserId);
    }

    /**
     * Get proposals made by a candidate for a specific application.
     */
    public List<InterviewProposal> getByApplication(int applicationId) {
        String sql = """
            SELECT ip.*, u.full_name AS candidate_name, jo.title AS job_title
            FROM interview_proposals ip
            JOIN applications a ON ip.application_id = a.id
            JOIN job_offers jo ON a.job_offer_id = jo.id
            LEFT JOIN profiles p ON a.candidate_profile_id = p.id
            LEFT JOIN users u ON p.user_id = u.id
            WHERE ip.application_id = ?
            ORDER BY ip.created_at DESC
            """;
        return queryProposals(sql, applicationId);
    }

    /**
     * Accept a proposal — creates an Interview record from the proposed details.
     *
     * @return the new Interview ID, or -1 on failure
     */
    public int accept(int proposalId) {
        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Load proposal
                InterviewProposal proposal = findById(proposalId);
                if (proposal == null || !"PENDING".equals(proposal.getStatus())) {
                    conn.rollback();
                    return -1;
                }

                // Mark accepted
                String updateSql = "UPDATE interview_proposals SET status = 'ACCEPTED', " +
                                   "responded_at = NOW() WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setInt(1, proposalId);
                    ps.executeUpdate();
                }

                // Create Interview
                String insertSql = "INSERT INTO interviews " +
                                   "(application_id, scheduled_date, duration_minutes, type, status, notes) " +
                                   "VALUES (?, ?, ?, ?, 'SCHEDULED', ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, proposal.getApplicationId());
                    ps.setTimestamp(2, Timestamp.valueOf(proposal.getProposedDate()));
                    ps.setInt(3, proposal.getDurationMinutes());
                    ps.setString(4, proposal.getType());
                    ps.setString(5, "Proposed by candidate: " +
                            (proposal.getMessage() != null ? proposal.getMessage() : ""));
                    ps.executeUpdate();

                    ResultSet keys = ps.getGeneratedKeys();
                    if (keys.next()) {
                        int interviewId = keys.getInt(1);
                        conn.commit();
                        logger.info("Proposal {} accepted → Interview {} created", proposalId, interviewId);
                        return interviewId;
                    }
                }

                conn.rollback();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Failed to accept proposal {}", proposalId, e);
        }
        return -1;
    }

    /**
     * Reject a proposal.
     */
    public boolean reject(int proposalId) {
        String sql = "UPDATE interview_proposals SET status = 'REJECTED', responded_at = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, proposalId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to reject proposal {}", proposalId, e);
            return false;
        }
    }

    /**
     * Find a single proposal by ID.
     */
    public InterviewProposal findById(int id) {
        String sql = "SELECT * FROM interview_proposals WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapProposal(rs);
            }
        } catch (SQLException e) {
            logger.error("Failed to find proposal {}", id, e);
        }
        return null;
    }

    /**
     * Count pending proposals for an employer.
     */
    public int getPendingCountForEmployer(int employerUserId) {
        String sql = """
            SELECT COUNT(*) FROM interview_proposals ip
            JOIN applications a ON ip.application_id = a.id
            JOIN job_offers jo ON a.job_offer_id = jo.id
            JOIN companies c ON jo.company_id = c.id
            WHERE c.owner_id = ? AND ip.status = 'PENDING'
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to count pending proposals", e);
        }
        return 0;
    }

    // ── Helpers ──

    private List<InterviewProposal> queryProposals(String sql, int param) {
        List<InterviewProposal> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapProposal(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to query proposals", e);
        }
        return list;
    }

    private InterviewProposal mapProposal(ResultSet rs) throws SQLException {
        InterviewProposal p = new InterviewProposal();
        p.setId(rs.getInt("id"));
        p.setApplicationId(rs.getInt("application_id"));
        p.setProposedBy(rs.getInt("proposed_by"));

        Timestamp pd = rs.getTimestamp("proposed_date");
        if (pd != null) p.setProposedDate(pd.toLocalDateTime());

        p.setDurationMinutes(rs.getInt("duration_minutes"));
        p.setType(rs.getString("type"));
        p.setMessage(rs.getString("message"));
        p.setStatus(rs.getString("status"));

        Timestamp ra = rs.getTimestamp("responded_at");
        if (ra != null) p.setRespondedAt(ra.toLocalDateTime());

        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) p.setCreatedAt(ca.toLocalDateTime());

        // Transient fields (may or may not be in ResultSet)
        try {
            p.setCandidateName(rs.getString("candidate_name"));
        } catch (SQLException ignored) {}
        try {
            p.setJobTitle(rs.getString("job_title"));
        } catch (SQLException ignored) {}

        return p;
    }
}
