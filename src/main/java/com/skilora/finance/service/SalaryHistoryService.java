package com.skilora.finance.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.finance.entity.SalaryHistory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SalaryHistoryService
 *
 * Simple CRUD for salary_history table.
 * Records salary changes for employment contracts.
 * No JavaFX imports allowed.
 */
public class SalaryHistoryService {

    private static volatile SalaryHistoryService instance;

    private SalaryHistoryService() {}

    public static SalaryHistoryService getInstance() {
        if (instance == null) {
            synchronized (SalaryHistoryService.class) {
                if (instance == null) {
                    instance = new SalaryHistoryService();
                }
            }
        }
        return instance;
    }

    // ==================== CRUD Operations ====================

    /**
     * Logs a new salary history entry.
     * @return generated ID
     */
    public int log(SalaryHistory entry) throws SQLException {
        String sql = "INSERT INTO salary_history (contract_id, old_salary, new_salary, reason, " +
                "effective_date) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, entry.getContractId());
            stmt.setBigDecimal(2, entry.getOldSalary());
            stmt.setBigDecimal(3, entry.getNewSalary());
            stmt.setString(4, entry.getReason());

            if (entry.getEffectiveDate() != null) {
                stmt.setDate(5, Date.valueOf(entry.getEffectiveDate()));
            } else {
                stmt.setNull(5, Types.DATE);
            }

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Logging salary history failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    entry.setId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Logging salary history failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Finds all salary history entries for a contract, ordered by effective_date DESC.
     */
    public List<SalaryHistory> findByContractId(int contractId) throws SQLException {
        String sql = "SELECT * FROM salary_history WHERE contract_id = ? ORDER BY effective_date DESC";
        List<SalaryHistory> entries = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, contractId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(mapResultSet(rs));
                }
            }
        }
        return entries;
    }

    // ==================== Private Helpers ====================

    /**
     * Maps a ResultSet row to a SalaryHistory entity.
     * Uses getBigDecimal for salary fields.
     */
    private SalaryHistory mapResultSet(ResultSet rs) throws SQLException {
        SalaryHistory entry = new SalaryHistory();
        entry.setId(rs.getInt("id"));
        entry.setContractId(rs.getInt("contract_id"));
        entry.setOldSalary(rs.getBigDecimal("old_salary"));
        entry.setNewSalary(rs.getBigDecimal("new_salary"));
        entry.setReason(rs.getString("reason"));

        Date effectiveDate = rs.getDate("effective_date");
        entry.setEffectiveDate(effectiveDate != null ? effectiveDate.toLocalDate() : null);

        Timestamp createdDate = rs.getTimestamp("created_date");
        entry.setCreatedDate(createdDate != null ? createdDate.toLocalDateTime() : null);

        return entry;
    }
}
