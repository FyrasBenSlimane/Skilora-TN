package com.skilora.finance.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.finance.entity.EmploymentContract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ContractService
 *
 * Full CRUD for employment_contracts table.
 * Handles contract lifecycle: creation, signing, termination.
 * No JavaFX imports allowed.
 */
public class ContractService {

    private static final Logger logger = LoggerFactory.getLogger(ContractService.class);

    private static volatile ContractService instance;

    private ContractService() {}

    public static ContractService getInstance() {
        if (instance == null) {
            synchronized (ContractService.class) {
                if (instance == null) {
                    instance = new ContractService();
                }
            }
        }
        return instance;
    }

    // ==================== CRUD Operations ====================

    /**
     * Creates a new employment contract.
     * @return generated ID
     */
    public int create(EmploymentContract contract) throws SQLException {
        String sql = "INSERT INTO employment_contracts (user_id, employer_id, job_offer_id, salary_base, " +
                "currency, start_date, end_date, contract_type, status, pdf_url, is_signed) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, contract.getUserId());

            if (contract.getEmployerId() != null) {
                stmt.setInt(2, contract.getEmployerId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }

            if (contract.getJobOfferId() != null) {
                stmt.setInt(3, contract.getJobOfferId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }

            stmt.setBigDecimal(4, contract.getSalaryBase());
            stmt.setString(5, contract.getCurrency());
            stmt.setDate(6, Date.valueOf(contract.getStartDate()));

            if (contract.getEndDate() != null) {
                stmt.setDate(7, Date.valueOf(contract.getEndDate()));
            } else {
                stmt.setNull(7, Types.DATE);
            }

            stmt.setString(8, contract.getContractType());
            stmt.setString(9, contract.getStatus());
            stmt.setString(10, contract.getPdfUrl());
            stmt.setBoolean(11, contract.isSigned());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating contract failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    contract.setId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Creating contract failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Finds a contract by ID with JOINs for user_name, employer_name, job_title.
     */
    public EmploymentContract findById(int id) throws SQLException {
        String sql = "SELECT c.*, " +
                "u.full_name AS user_name, " +
                "e.full_name AS employer_name, " +
                "j.title AS job_title " +
                "FROM employment_contracts c " +
                "LEFT JOIN users u ON c.user_id = u.id " +
                "LEFT JOIN users e ON c.employer_id = e.id " +
                "LEFT JOIN job_offers j ON c.job_offer_id = j.id " +
                "WHERE c.id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Finds all contracts for a user, ordered by created_date DESC.
     */
    public List<EmploymentContract> findByUserId(int userId) throws SQLException {
        String sql = "SELECT c.*, " +
                "u.full_name AS user_name, " +
                "e.full_name AS employer_name, " +
                "j.title AS job_title " +
                "FROM employment_contracts c " +
                "LEFT JOIN users u ON c.user_id = u.id " +
                "LEFT JOIN users e ON c.employer_id = e.id " +
                "LEFT JOIN job_offers j ON c.job_offer_id = j.id " +
                "WHERE c.user_id = ? ORDER BY c.created_date DESC";

        return executeQuery(sql, userId);
    }

    /**
     * Finds all contracts for an employer.
     */
    public List<EmploymentContract> findByEmployerId(int employerId) throws SQLException {
        String sql = "SELECT c.*, " +
                "u.full_name AS user_name, " +
                "e.full_name AS employer_name, " +
                "j.title AS job_title " +
                "FROM employment_contracts c " +
                "LEFT JOIN users u ON c.user_id = u.id " +
                "LEFT JOIN users e ON c.employer_id = e.id " +
                "LEFT JOIN job_offers j ON c.job_offer_id = j.id " +
                "WHERE c.employer_id = ?";

        return executeQuery(sql, employerId);
    }

    /**
     * Finds the active contract for a user (LIMIT 1).
     */
    public EmploymentContract findActiveByUserId(int userId) throws SQLException {
        String sql = "SELECT c.*, " +
                "u.full_name AS user_name, " +
                "e.full_name AS employer_name, " +
                "j.title AS job_title " +
                "FROM employment_contracts c " +
                "LEFT JOIN users u ON c.user_id = u.id " +
                "LEFT JOIN users e ON c.employer_id = e.id " +
                "LEFT JOIN job_offers j ON c.job_offer_id = j.id " +
                "WHERE c.user_id = ? AND c.status = 'ACTIVE' LIMIT 1";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Finds all contracts with a given status.
     */
    public List<EmploymentContract> findByStatus(String status) throws SQLException {
        String sql = "SELECT c.*, " +
                "u.full_name AS user_name, " +
                "e.full_name AS employer_name, " +
                "j.title AS job_title " +
                "FROM employment_contracts c " +
                "LEFT JOIN users u ON c.user_id = u.id " +
                "LEFT JOIN users e ON c.employer_id = e.id " +
                "LEFT JOIN job_offers j ON c.job_offer_id = j.id " +
                "WHERE c.status = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);

            try (ResultSet rs = stmt.executeQuery()) {
                List<EmploymentContract> contracts = new ArrayList<>();
                while (rs.next()) {
                    contracts.add(mapResultSet(rs));
                }
                return contracts;
            }
        }
    }

    /**
     * Updates an existing contract.
     */
    public boolean update(EmploymentContract contract) throws SQLException {
        String sql = "UPDATE employment_contracts SET user_id = ?, employer_id = ?, job_offer_id = ?, " +
                "salary_base = ?, currency = ?, start_date = ?, end_date = ?, contract_type = ?, " +
                "status = ?, pdf_url = ?, is_signed = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, contract.getUserId());

            if (contract.getEmployerId() != null) {
                stmt.setInt(2, contract.getEmployerId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }

            if (contract.getJobOfferId() != null) {
                stmt.setInt(3, contract.getJobOfferId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }

            stmt.setBigDecimal(4, contract.getSalaryBase());
            stmt.setString(5, contract.getCurrency());
            stmt.setDate(6, Date.valueOf(contract.getStartDate()));

            if (contract.getEndDate() != null) {
                stmt.setDate(7, Date.valueOf(contract.getEndDate()));
            } else {
                stmt.setNull(7, Types.DATE);
            }

            stmt.setString(8, contract.getContractType());
            stmt.setString(9, contract.getStatus());
            stmt.setString(10, contract.getPdfUrl());
            stmt.setBoolean(11, contract.isSigned());
            stmt.setInt(12, contract.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Signs a contract: sets is_signed=TRUE, signed_date=NOW(), status='ACTIVE'.
     */
    public boolean sign(int id) throws SQLException {
        String sql = "UPDATE employment_contracts SET is_signed = TRUE, signed_date = NOW(), " +
                "status = 'ACTIVE' WHERE id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Terminates a contract: sets status='TERMINATED'.
     */
    public boolean terminate(int id) throws SQLException {
        String sql = "UPDATE employment_contracts SET status = 'TERMINATED' WHERE id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a contract only if status is 'DRAFT'.
     */
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM employment_contracts WHERE id = ? AND status = 'DRAFT'";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    // ==================== Private Helpers ====================

    /**
     * Executes a query with a single int parameter and returns a list of contracts.
     */
    private List<EmploymentContract> executeQuery(String sql, int param) throws SQLException {
        List<EmploymentContract> contracts = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, param);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    contracts.add(mapResultSet(rs));
                }
            }
        }
        return contracts;
    }

    /**
     * Maps a ResultSet row to an EmploymentContract entity.
     * Handles nullable Integer fields with rs.wasNull() and nullable dates.
     */
    private EmploymentContract mapResultSet(ResultSet rs) throws SQLException {
        EmploymentContract contract = new EmploymentContract();
        contract.setId(rs.getInt("id"));
        contract.setUserId(rs.getInt("user_id"));

        int employerId = rs.getInt("employer_id");
        contract.setEmployerId(rs.wasNull() ? null : employerId);

        int jobOfferId = rs.getInt("job_offer_id");
        contract.setJobOfferId(rs.wasNull() ? null : jobOfferId);

        contract.setSalaryBase(rs.getBigDecimal("salary_base"));
        contract.setCurrency(rs.getString("currency"));
        contract.setStartDate(rs.getDate("start_date").toLocalDate());

        Date endDate = rs.getDate("end_date");
        contract.setEndDate(endDate != null ? endDate.toLocalDate() : null);

        contract.setContractType(rs.getString("contract_type"));
        contract.setStatus(rs.getString("status"));
        contract.setPdfUrl(rs.getString("pdf_url"));
        contract.setSigned(rs.getBoolean("is_signed"));

        Timestamp signedDate = rs.getTimestamp("signed_date");
        contract.setSignedDate(signedDate != null ? signedDate.toLocalDateTime() : null);

        Timestamp createdDate = rs.getTimestamp("created_date");
        contract.setCreatedDate(createdDate != null ? createdDate.toLocalDateTime() : null);

        // Transient fields from JOINs
        try {
            contract.setUserName(rs.getString("user_name"));
        } catch (SQLException ignored) {}
        try {
            contract.setEmployerName(rs.getString("employer_name"));
        } catch (SQLException ignored) {}
        try {
            contract.setJobTitle(rs.getString("job_title"));
        } catch (SQLException ignored) {}

        return contract;
    }
}
