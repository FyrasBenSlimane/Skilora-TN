package com.skilora.finance.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.finance.entity.BankAccount;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(BankAccountService.class);

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
     * Finds a bank account by ID.
     */
    public BankAccount findById(int id) throws SQLException {
        String sql = "SELECT * FROM bank_accounts WHERE id = ?";

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
     * Updates an existing bank account.
     */
    public boolean update(BankAccount account) throws SQLException {
        String sql = "UPDATE bank_accounts SET bank_name = ?, account_holder = ?, iban = ?, " +
                "swift_bic = ?, rib = ?, currency = ?, is_primary = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, account.getBankName());
            stmt.setString(2, account.getAccountHolder());
            stmt.setString(3, account.getIban());
            stmt.setString(4, account.getSwiftBic());
            stmt.setString(5, account.getRib());
            stmt.setString(6, account.getCurrency());
            stmt.setBoolean(7, account.isPrimary());
            stmt.setInt(8, account.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a bank account by ID.
     */
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM bank_accounts WHERE id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
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
