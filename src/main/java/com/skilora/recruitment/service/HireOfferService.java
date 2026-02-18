package com.skilora.recruitment.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.recruitment.entity.HireOffer;
import com.skilora.recruitment.enums.HireOfferStatus;
import com.skilora.utils.ResultSetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HireOfferService {

    private static final Logger logger = LoggerFactory.getLogger(HireOfferService.class);
    private static volatile HireOfferService instance;

    private HireOfferService() {}

    public static HireOfferService getInstance() {
        if (instance == null) {
            synchronized (HireOfferService.class) {
                if (instance == null) {
                    instance = new HireOfferService();
                }
            }
        }
        return instance;
    }

    public int create(HireOffer offer) {
        if (offer == null) throw new IllegalArgumentException("Hire offer cannot be null");
        if (offer.getApplicationId() <= 0) throw new IllegalArgumentException("Valid application ID is required");
        if (offer.getSalaryOffered() <= 0) throw new IllegalArgumentException("Salary must be positive");
        String sql = """
            INSERT INTO hire_offers (application_id, salary_offered, currency, start_date,
                contract_type, benefits, status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, offer.getApplicationId());
            stmt.setDouble(2, offer.getSalaryOffered());
            stmt.setString(3, offer.getCurrency());
            stmt.setDate(4, offer.getStartDate() != null ? Date.valueOf(offer.getStartDate()) : null);
            stmt.setString(5, offer.getContractType());
            stmt.setString(6, offer.getBenefits());
            stmt.setString(7, offer.getStatus() != null ? offer.getStatus() : HireOfferStatus.PENDING.name());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                logger.info("Hire offer created: id={}", id);
                return id;
            }
        } catch (SQLException e) {
            logger.error("Error creating hire offer: {}", e.getMessage(), e);
        }
        return -1;
    }

    public HireOffer findById(int id) {
        String sql = """
            SELECT h.*, u.full_name AS candidate_name, j.title AS job_title, c.name AS company_name
            FROM hire_offers h
            JOIN applications a ON h.application_id = a.id
            JOIN users u ON a.candidate_profile_id = u.id
            JOIN job_offers j ON a.job_offer_id = j.id
            LEFT JOIN companies c ON j.company_id = c.id
            WHERE h.id = ?
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapHireOffer(rs);
        } catch (SQLException e) {
            logger.error("Error finding hire offer {}: {}", id, e.getMessage(), e);
        }
        return null;
    }

    public List<HireOffer> findByApplication(int applicationId) {
        return findByQuery("WHERE h.application_id = ?", applicationId);
    }

    public List<HireOffer> findByEmployer(int employerId) {
        String sql = """
            SELECT h.*, u.full_name AS candidate_name, j.title AS job_title, c.name AS company_name
            FROM hire_offers h
            JOIN applications a ON h.application_id = a.id
            JOIN users u ON a.candidate_profile_id = u.id
            JOIN job_offers j ON a.job_offer_id = j.id
            LEFT JOIN companies c ON j.company_id = c.id
            WHERE j.employer_id = ?
            ORDER BY h.created_date DESC
            """;
        List<HireOffer> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employerId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapHireOffer(rs));
        } catch (SQLException e) {
            logger.error("Error finding employer offers: {}", e.getMessage(), e);
        }
        return list;
    }

    public List<HireOffer> findPendingForCandidate(int userId) {
        String sql = """
            SELECT h.*, u.full_name AS candidate_name, j.title AS job_title, c.name AS company_name
            FROM hire_offers h
            JOIN applications a ON h.application_id = a.id
            JOIN users u ON a.candidate_profile_id = u.id
            JOIN job_offers j ON a.job_offer_id = j.id
            LEFT JOIN companies c ON j.company_id = c.id
            WHERE a.candidate_profile_id = ? AND h.status = '" + HireOfferStatus.PENDING.name() + "'
            ORDER BY h.created_date DESC
            """;
        List<HireOffer> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapHireOffer(rs));
        } catch (SQLException e) {
            logger.error("Error finding pending offers for candidate {}: {}", userId, e.getMessage(), e);
        }
        return list;
    }

    public boolean respond(int id, String status) {
        String sql = "UPDATE hire_offers SET status = ?, responded_date = NOW() WHERE id = ? AND status = '" + HireOfferStatus.PENDING.name() + "'";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            boolean updated = stmt.executeUpdate() > 0;
            if (updated) logger.info("Hire offer {} responded: {}", id, status);
            return updated;
        } catch (SQLException e) {
            logger.error("Error responding to offer: {}", e.getMessage(), e);
        }
        return false;
    }

    public boolean accept(int id) {
        return respond(id, HireOfferStatus.ACCEPTED.name());
    }

    public boolean reject(int id) {
        return respond(id, HireOfferStatus.REJECTED.name());
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM hire_offers WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting hire offer: {}", e.getMessage(), e);
        }
        return false;
    }

    // ── Private helpers ──

    private List<HireOffer> findByQuery(String whereClause, int param) {
        String sql = """
            SELECT h.*, u.full_name AS candidate_name, j.title AS job_title, c.name AS company_name
            FROM hire_offers h
            JOIN applications a ON h.application_id = a.id
            JOIN users u ON a.candidate_profile_id = u.id
            JOIN job_offers j ON a.job_offer_id = j.id
            LEFT JOIN companies c ON j.company_id = c.id
            """ + whereClause + " ORDER BY h.created_date DESC";
        List<HireOffer> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, param);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(mapHireOffer(rs));
        } catch (SQLException e) {
            logger.error("Error querying hire offers: {}", e.getMessage(), e);
        }
        return list;
    }

    private HireOffer mapHireOffer(ResultSet rs) throws SQLException {
        HireOffer h = new HireOffer();
        h.setId(rs.getInt("id"));
        h.setApplicationId(rs.getInt("application_id"));
        h.setSalaryOffered(rs.getDouble("salary_offered"));
        h.setCurrency(rs.getString("currency"));
        Date startDate = rs.getDate("start_date");
        if (startDate != null) h.setStartDate(startDate.toLocalDate());
        h.setContractType(rs.getString("contract_type"));
        h.setBenefits(rs.getString("benefits"));
        h.setStatus(rs.getString("status"));
        Timestamp created = rs.getTimestamp("created_date");
        if (created != null) h.setCreatedDate(created.toLocalDateTime());
        Timestamp responded = rs.getTimestamp("responded_date");
        if (responded != null) h.setRespondedDate(responded.toLocalDateTime());
        h.setCandidateName(ResultSetUtils.getOptionalString(rs, "candidate_name"));
        h.setJobTitle(ResultSetUtils.getOptionalString(rs, "job_title"));
        h.setCompanyName(ResultSetUtils.getOptionalString(rs, "company_name"));
        return h;
    }
}
