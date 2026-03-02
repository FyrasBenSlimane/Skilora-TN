package com.skilora.finance.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.finance.entity.EscrowAccount;
import com.skilora.utils.ResultSetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * EscrowService
 *
 * Singleton service for escrow_accounts table.
 * Handles hold creation, release, refund, and dispute operations.
 * No JavaFX imports allowed.
 */
public class EscrowService {

    private static final Logger logger = LoggerFactory.getLogger(EscrowService.class);
    private static volatile EscrowService instance;

    /** Base SELECT with JOINs for user_name, employer_name from contract. */
    private static final String BASE_SELECT =
            "SELECT e.*, " +
            "u.full_name AS user_name, " +
            "emp.full_name AS employer_name " +
            "FROM escrow_accounts e " +
            "JOIN employment_contracts c ON e.contract_id = c.id " +
            "LEFT JOIN users u ON c.user_id = u.id " +
            "LEFT JOIN users emp ON c.employer_id = emp.id";

    private EscrowService() {}

    public static EscrowService getInstance() {
        if (instance == null) {
            synchronized (EscrowService.class) {
                if (instance == null) {
                    instance = new EscrowService();
                }
            }
        }
        return instance;
    }

    /**
     * Creates a new escrow hold.
     * @return generated ID
     */
    public int createHold(EscrowAccount escrow) throws SQLException {
        String sql = "INSERT INTO escrow_accounts (contract_id, admin_id, amount, currency, status, description) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, escrow.getContractId());
            stmt.setInt(2, escrow.getAdminId());
            stmt.setBigDecimal(3, escrow.getAmount());
            stmt.setString(4, escrow.getCurrency() != null ? escrow.getCurrency() : "TND");
            stmt.setString(5, escrow.getStatus() != null ? escrow.getStatus() : "HOLDING");
            stmt.setString(6, escrow.getDescription());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating escrow hold failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    escrow.setId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Creating escrow hold failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Finds an escrow by ID with JOINs for contract user_name, employer_name.
     */
    public EscrowAccount findById(int id) throws SQLException {
        String sql = BASE_SELECT + " WHERE e.id = ?";

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
     * Finds all escrow accounts for a contract.
     */
    public List<EscrowAccount> findByContract(int contractId) throws SQLException {
        String sql = BASE_SELECT + " WHERE e.contract_id = ? ORDER BY e.created_date DESC";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, contractId);

            try (ResultSet rs = stmt.executeQuery()) {
                List<EscrowAccount> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
                return list;
            }
        }
    }

    /**
     * Finds all escrow accounts with HOLDING status, ordered by created_date.
     */
    public List<EscrowAccount> findPendingForAdmin() throws SQLException {
        String sql = BASE_SELECT + " WHERE e.status = 'HOLDING' ORDER BY e.created_date ASC";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            List<EscrowAccount> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
            return list;
        }
    }

    /**
     * Releases an escrow hold. Only if current status is HOLDING.
     * Sets status=RELEASED, released_date=NOW(), admin_id, release_notes.
     */
    public boolean release(int id, int adminId, String notes) throws SQLException {
        String sql = "UPDATE escrow_accounts SET status = 'RELEASED', released_date = NOW(), " +
                "admin_id = ?, release_notes = ? WHERE id = ? AND status = 'HOLDING'";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, adminId);
            stmt.setString(2, notes);
            stmt.setInt(3, id);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                logger.info("Escrow {} released by admin {}", id, adminId);
            }
            return rows > 0;
        }
    }

    /**
     * Refunds an escrow hold. Only if current status is HOLDING.
     * Sets status=REFUNDED, released_date=NOW(), admin_id, release_notes.
     */
    public boolean refund(int id, int adminId, String notes) throws SQLException {
        String sql = "UPDATE escrow_accounts SET status = 'REFUNDED', released_date = NOW(), " +
                "admin_id = ?, release_notes = ? WHERE id = ? AND status = 'HOLDING'";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, adminId);
            stmt.setString(2, notes);
            stmt.setInt(3, id);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                logger.info("Escrow {} refunded by admin {}", id, adminId);
            }
            return rows > 0;
        }
    }

    /**
     * Marks an escrow as disputed.
     * Sets status=DISPUTED, stores reason in release_notes.
     */
    public boolean dispute(int id, String reason) throws SQLException {
        String sql = "UPDATE escrow_accounts SET status = 'DISPUTED', release_notes = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reason);
            stmt.setInt(2, id);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                logger.info("Escrow {} disputed: {}", id, reason);
            }
            return rows > 0;
        }
    }

    /**
     * Finds all escrow accounts with the given status.
     */
    public List<EscrowAccount> findByStatus(String status) throws SQLException {
        String sql = BASE_SELECT + " WHERE e.status = ? ORDER BY e.created_date DESC";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);

            try (ResultSet rs = stmt.executeQuery()) {
                List<EscrowAccount> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
                return list;
            }
        }
    }

    private EscrowAccount mapResultSet(ResultSet rs) throws SQLException {
        EscrowAccount e = new EscrowAccount();
        e.setId(rs.getInt("id"));
        e.setContractId(rs.getInt("contract_id"));
        e.setAdminId(rs.getInt("admin_id"));
        e.setAmount(rs.getBigDecimal("amount"));
        e.setCurrency(rs.getString("currency"));
        e.setStatus(rs.getString("status"));
        e.setDescription(rs.getString("description"));

        Timestamp created = rs.getTimestamp("created_date");
        e.setCreatedDate(created != null ? created.toLocalDateTime() : null);

        Timestamp released = rs.getTimestamp("released_date");
        e.setReleasedDate(released != null ? released.toLocalDateTime() : null);

        e.setReleaseNotes(rs.getString("release_notes"));

        e.setUserName(ResultSetUtils.getOptionalString(rs, "user_name"));
        e.setEmployerName(ResultSetUtils.getOptionalString(rs, "employer_name"));

        return e;
    }
}
