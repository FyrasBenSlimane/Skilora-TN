package com.skilora.finance.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.finance.entity.PaymentTransaction;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * PaymentTransactionService
 *
 * CRUD for payment_transactions table.
 * Generates unique transaction references and tracks payment status.
 * No JavaFX imports allowed.
 */
public class PaymentTransactionService {

    private static volatile PaymentTransactionService instance;

    private PaymentTransactionService() {}

    public static PaymentTransactionService getInstance() {
        if (instance == null) {
            synchronized (PaymentTransactionService.class) {
                if (instance == null) {
                    instance = new PaymentTransactionService();
                }
            }
        }
        return instance;
    }

    // ==================== CRUD Operations ====================

    /**
     * Creates a new payment transaction with auto-generated reference.
     * Reference format: TXN-XXXXXXXX (8-char uppercase UUID prefix).
     * @return generated ID
     */
    public int create(PaymentTransaction tx) throws SQLException {
        if (tx == null) throw new IllegalArgumentException("Transaction must not be null");
        if (tx.getAmount() == null || tx.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be positive");
        if (tx.getCurrency() == null || tx.getCurrency().isBlank())
            throw new IllegalArgumentException("Currency is required");
        if (tx.getTransactionType() == null || tx.getTransactionType().isBlank())
            throw new IllegalArgumentException("Transaction type is required");
        if (tx.getFromAccountId() == null && tx.getToAccountId() == null)
            throw new IllegalArgumentException("At least one account (from/to) is required");

        // Generate unique reference
        String reference = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        tx.setReference(reference);

        String sql = "INSERT INTO payment_transactions (payslip_id, from_account_id, to_account_id, " +
                "amount, currency, transaction_type, status, reference, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (tx.getPayslipId() != null) {
                stmt.setInt(1, tx.getPayslipId());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }

            if (tx.getFromAccountId() != null) {
                stmt.setInt(2, tx.getFromAccountId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }

            if (tx.getToAccountId() != null) {
                stmt.setInt(3, tx.getToAccountId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }

            stmt.setBigDecimal(4, tx.getAmount());
            stmt.setString(5, tx.getCurrency());
            stmt.setString(6, tx.getTransactionType());
            stmt.setString(7, tx.getStatus());
            stmt.setString(8, tx.getReference());
            stmt.setString(9, tx.getNotes());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating payment transaction failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    tx.setId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Creating payment transaction failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Finds a payment transaction by ID.
     */
    public PaymentTransaction findById(int id) throws SQLException {
        String sql = "SELECT * FROM payment_transactions WHERE id = ?";

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
     * Finds all payment transactions for a payslip.
     */
    public List<PaymentTransaction> findByPayslipId(int payslipId) throws SQLException {
        String sql = "SELECT * FROM payment_transactions WHERE payslip_id = ?";
        List<PaymentTransaction> transactions = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, payslipId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSet(rs));
                }
            }
        }
        return transactions;
    }

    /**
     * Finds all payment transactions for a user via payslip JOIN.
     */
    public List<PaymentTransaction> findByUserId(int userId) throws SQLException {
        String sql = "SELECT pt.* FROM payment_transactions pt " +
                "JOIN payslips p ON pt.payslip_id = p.id " +
                "WHERE p.user_id = ? ORDER BY pt.transaction_date DESC";
        List<PaymentTransaction> transactions = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSet(rs));
                }
            }
        }
        return transactions;
    }

    /**
     * Finds all payment transactions for an employer (via payslip -> contract JOIN).
     * Useful for employer/admin payment history views.
     */
    public List<PaymentTransaction> findByEmployerId(int employerId) throws SQLException {
        String sql = "SELECT pt.* FROM payment_transactions pt " +
                "JOIN payslips p ON pt.payslip_id = p.id " +
                "JOIN employment_contracts c ON p.contract_id = c.id " +
                "WHERE c.employer_id = ? ORDER BY pt.transaction_date DESC";
        List<PaymentTransaction> transactions = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, employerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSet(rs));
                }
            }
        }
        return transactions;
    }

    /**
     * Finds most recent payment transactions (admin use).
     */
    public List<PaymentTransaction> findRecent(int limit) throws SQLException {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        String sql = "SELECT * FROM payment_transactions ORDER BY transaction_date DESC LIMIT " + safeLimit;
        List<PaymentTransaction> transactions = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                transactions.add(mapResultSet(rs));
            }
        }
        return transactions;
    }

    /**
     * Updates the status of a payment transaction.
     */
    public boolean updateStatus(int id, String status) throws SQLException {
        String sql = "UPDATE payment_transactions SET status = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Gets the total amount paid to a user (sum of PAID transactions).
     * @return total amount, or BigDecimal.ZERO if none found
     */
    public BigDecimal getTotalPaidByUser(int userId) throws SQLException {
        String sql = "SELECT SUM(pt.amount) FROM payment_transactions pt " +
                "JOIN payslips p ON pt.payslip_id = p.id " +
                "WHERE p.user_id = ? AND pt.status = 'PAID'";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal(1);
                    return total != null ? total : BigDecimal.ZERO;
                }
            }
        }
        return BigDecimal.ZERO;
    }

    // ==================== Private Helpers ====================

    /**
     * Maps a ResultSet row to a PaymentTransaction entity.
     * Handles nullable Integer fields with wasNull().
     */
    private PaymentTransaction mapResultSet(ResultSet rs) throws SQLException {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setId(rs.getInt("id"));

        int payslipId = rs.getInt("payslip_id");
        tx.setPayslipId(rs.wasNull() ? null : payslipId);

        int fromAccountId = rs.getInt("from_account_id");
        tx.setFromAccountId(rs.wasNull() ? null : fromAccountId);

        int toAccountId = rs.getInt("to_account_id");
        tx.setToAccountId(rs.wasNull() ? null : toAccountId);

        tx.setAmount(rs.getBigDecimal("amount"));
        tx.setCurrency(rs.getString("currency"));
        tx.setTransactionType(rs.getString("transaction_type"));
        tx.setStatus(rs.getString("status"));
        tx.setReference(rs.getString("reference"));

        Timestamp transactionDate = rs.getTimestamp("transaction_date");
        tx.setTransactionDate(transactionDate != null ? transactionDate.toLocalDateTime() : null);

        tx.setNotes(rs.getString("notes"));

        // Map Stripe-related fields if columns exist
        try {
            tx.setStripePaymentId(rs.getString("stripe_payment_id"));
        } catch (SQLException ignored) { /* column may not exist */ }
        try {
            tx.setReferenceProjet(rs.getString("reference_projet"));
        } catch (SQLException ignored) { /* column may not exist */ }
        try {
            tx.setNomBeneficiaire(rs.getString("nom_beneficiaire"));
        } catch (SQLException ignored) { /* column may not exist */ }

        return tx;
    }

    // ==================== Stripe Payment Methods (from PaiementService) ====================

    /**
     * Records a Stripe payment transaction.
     * Adapted from PaiementService.ajouterPaiement() for the unified PaymentTransaction model.
     *
     * @param amount          payment amount
     * @param stripePaymentId Stripe payment intent ID (pi_XXXX)
     * @param referenceProjet project reference
     * @param nomBeneficiaire beneficiary name
     * @param status          payment status (PENDING, SUCCESS, FAILED)
     * @return the created PaymentTransaction
     */
    public PaymentTransaction createStripePayment(BigDecimal amount, String stripePaymentId,
                                                   String referenceProjet, String nomBeneficiaire,
                                                   String status) throws SQLException {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setAmount(amount);
        tx.setCurrency("USD");
        tx.setTransactionType("STRIPE");
        tx.setStatus(status != null ? status : "PENDING");
        tx.setStripePaymentId(stripePaymentId);
        tx.setReferenceProjet(referenceProjet);
        tx.setNomBeneficiaire(nomBeneficiaire);
        tx.setNotes("Stripe payment - " + referenceProjet);

        // Use a dummy account for Stripe payments
        tx.setFromAccountId(null);
        tx.setToAccountId(null);

        String sql = "INSERT INTO payment_transactions (amount, currency, transaction_type, status, reference, " +
                "notes, stripe_payment_id, reference_projet, nom_beneficiaire) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String reference = "TXN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        tx.setReference(reference);

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setBigDecimal(1, tx.getAmount());
            stmt.setString(2, tx.getCurrency());
            stmt.setString(3, tx.getTransactionType());
            stmt.setString(4, tx.getStatus());
            stmt.setString(5, tx.getReference());
            stmt.setString(6, tx.getNotes());
            stmt.setString(7, tx.getStripePaymentId());
            stmt.setString(8, tx.getReferenceProjet());
            stmt.setString(9, tx.getNomBeneficiaire());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        tx.setId(keys.getInt(1));
                    }
                }
            }
        }
        return tx;
    }

    /**
     * Finds all Stripe payment transactions for a project reference.
     * Adapted from PaiementService.getPaiementsByProjet().
     */
    public List<PaymentTransaction> findByProjectReference(String referenceProjet) throws SQLException {
        String sql = "SELECT * FROM payment_transactions WHERE reference_projet = ? ORDER BY transaction_date DESC";
        List<PaymentTransaction> transactions = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, referenceProjet);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSet(rs));
                }
            }
        }
        return transactions;
    }
}
