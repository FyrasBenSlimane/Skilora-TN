package com.skilora.finance.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.finance.entity.BankAccount;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * BankAccountService
 *
 * CRUD for bank_accounts table.
 * Handles primary account designation with transactional safety.
 * No JavaFX imports allowed.
 */
public class BankAccountService {

    private static volatile BankAccountService instance;

    private BankAccountService() {}

    public static BankAccountService getInstance() {
        if (instance == null) {
            synchronized (BankAccountService.class) {
                if (instance == null) {
                    instance = new BankAccountService();
                }
            }
        }
        return instance;
    }

    // ==================== CRUD Operations ====================

    /**
     * Creates a new bank account.
     * @return generated ID
     */
    public int create(BankAccount account) throws SQLException {
        if (account == null) throw new IllegalArgumentException("Account must not be null");
        if (account.getUserId() <= 0) throw new IllegalArgumentException("Invalid user ID");
        if (account.getBankName() == null || account.getBankName().isBlank())
            throw new IllegalArgumentException("Bank name is required");
        if (account.getAccountHolder() == null || account.getAccountHolder().isBlank())
            throw new IllegalArgumentException("Account holder name is required");
        if (account.getIban() == null || account.getIban().isBlank())
            throw new IllegalArgumentException("IBAN is required");
        if (account.getCurrency() == null || account.getCurrency().isBlank())
            throw new IllegalArgumentException("Currency is required");

        String sql = "INSERT INTO bank_accounts (user_id, bank_name, account_holder, iban, " +
                "swift_bic, rib, currency, is_primary) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, account.getUserId());
            stmt.setString(2, account.getBankName());
            stmt.setString(3, account.getAccountHolder());
            stmt.setString(4, account.getIban());
            stmt.setString(5, account.getSwiftBic());
            stmt.setString(6, account.getRib());
            stmt.setString(7, account.getCurrency());
            stmt.setBoolean(8, account.isPrimary());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating bank account failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    account.setId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Creating bank account failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Finds all bank accounts for a user.
     */
    public List<BankAccount> findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM bank_accounts WHERE user_id = ?";
        List<BankAccount> accounts = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    accounts.add(mapResultSet(rs));
                }
            }
        }
        return accounts;
    }

    /**
     * Find a bank account by ID.
     */
    public BankAccount findById(int id) throws SQLException {
        String sql = "SELECT * FROM bank_accounts WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        }
        return null;
    }

    /**
     * Update a bank account.
     */
    public boolean update(BankAccount account) throws SQLException {
        String sql = "UPDATE bank_accounts SET bank_name = ?, account_holder = ?, iban = ?, " +
                "swift_bic = ?, rib = ?, currency = ? WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, account.getBankName());
            stmt.setString(2, account.getAccountHolder());
            stmt.setString(3, account.getIban());
            stmt.setString(4, account.getSwiftBic());
            stmt.setString(5, account.getRib());
            stmt.setString(6, account.getCurrency());
            stmt.setInt(7, account.getId());
            stmt.setInt(8, account.getUserId());
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Delete a bank account (only if not primary).
     */
    public boolean delete(int id, int userId) throws SQLException {
        String sql = "DELETE FROM bank_accounts WHERE id = ? AND user_id = ? AND is_primary = FALSE";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Set a bank account as primary (unsets all others for that user).
     */
    public boolean setPrimary(int accountId, int userId) throws SQLException {
        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                String unsetSql = "UPDATE bank_accounts SET is_primary = FALSE WHERE user_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(unsetSql)) {
                    stmt.setInt(1, userId);
                    stmt.executeUpdate();
                }
                String setSql = "UPDATE bank_accounts SET is_primary = TRUE WHERE id = ? AND user_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(setSql)) {
                    stmt.setInt(1, accountId);
                    stmt.setInt(2, userId);
                    int rows = stmt.executeUpdate();
                    if (rows > 0) {
                        conn.commit();
                        return true;
                    }
                }
                conn.rollback();
                return false;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // ==================== Private Helpers ====================

    /**
     * Maps a ResultSet row to a BankAccount entity.
     */
    private BankAccount mapResultSet(ResultSet rs) throws SQLException {
        BankAccount account = new BankAccount();
        account.setId(rs.getInt("id"));
        account.setUserId(rs.getInt("user_id"));
        account.setBankName(rs.getString("bank_name"));
        account.setAccountHolder(rs.getString("account_holder"));
        account.setIban(rs.getString("iban"));
        account.setSwiftBic(rs.getString("swift_bic"));
        account.setRib(rs.getString("rib"));
        account.setCurrency(rs.getString("currency"));
        account.setPrimary(rs.getBoolean("is_primary"));

        Timestamp createdDate = rs.getTimestamp("created_date");
        account.setCreatedDate(createdDate != null ? createdDate.toLocalDateTime() : null);

        return account;
    }
}
