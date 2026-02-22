package com.skilora.finance.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.finance.model.ContractRow;
import com.skilora.finance.model.BankAccountRow;
import com.skilora.finance.model.BonusRow;
import com.skilora.finance.model.PayslipRow;
import com.skilora.finance.model.EmployeeSummaryRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class FinanceService {

    private static FinanceService instance;
    private ConnectionFactory connectionFactory;

    // Interface for dependency injection (Testability)
    public interface ConnectionFactory {
        Connection getConnection() throws SQLException;
    }

    // Default constructor uses the real DatabaseConfig
    private FinanceService() {
        this.connectionFactory = () -> DatabaseConfig.getInstance().getConnection();
    }

    // Constructor for Testing (Inject Mock Connection)
    public FinanceService(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public static synchronized FinanceService getInstance() {
        if (instance == null)
            instance = new FinanceService();
        return instance;
    }

    // Helper to get connection
    private Connection getConnection() throws SQLException {
        return connectionFactory.getConnection();
    }

    // --- CONTRACTS ---
    public List<ContractRow> getAllContracts() throws SQLException {
        List<ContractRow> list = new ArrayList<>();
        String sql = "SELECT c.*, u.full_name, co.name as company_name FROM contracts c JOIN users u ON c.user_id = u.id LEFT JOIN companies co ON c.company_id = co.id";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new ContractRow(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("full_name"),
                        rs.getString("company_name") != null ? rs.getString("company_name") : "N/A",
                        rs.getString("type"),
                        rs.getString("position"),
                        rs.getDouble("salary"),
                        rs.getDate("start_date").toString(),
                        rs.getDate("end_date") != null ? rs.getDate("end_date").toString() : "",
                        rs.getString("status")));
            }
        }
        return list;
    }

    public void addContract(ContractRow c) throws SQLException {
        // First, we need to find the company ID based on the name, because contracts
        // table likely stores company_id
        // But wait, the user said "il faut faire une jointure".
        // If the input is company name, we need to resolve it to an ID or maybe insert
        // it?
        // Let's assume for now we look up the ID from the name.

        int companyId = getOrCreateCompanyIdByName(c.getCompanyName());
        if (companyId == -1) {
            throw new SQLException("Failed to resolve or create company: " + c.getCompanyName());
        }

        String sql = "INSERT INTO contracts (user_id, company_id, type, position, salary, start_date, end_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, c.getUserId());
            stmt.setInt(2, companyId);
            stmt.setString(3, c.getType());
            stmt.setString(4, c.getPosition());
            stmt.setDouble(5, c.getSalary());
            stmt.setDate(6, java.sql.Date.valueOf(c.getStartDate()));
            stmt.setDate(7, (c.getEndDate() == null || c.getEndDate().isEmpty()) ? null
                    : java.sql.Date.valueOf(c.getEndDate()));
            stmt.setString(8, c.getStatus());
            stmt.executeUpdate();
        }
    }

    // Helper to resolve company name to ID, creating it if doesn't exist
    private int getOrCreateCompanyIdByName(String name) throws SQLException {
        if (name == null || name.isBlank())
            return -1;

        int existingId = getCompanyIdByName(name);
        if (existingId != -1)
            return existingId;

        String sql = "INSERT INTO companies (name) VALUES (?)";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }

    // Helper to resolve company name to ID
    private int getCompanyIdByName(String name) throws SQLException {
        String sql = "SELECT id FROM companies WHERE name = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return -1;
    }

    public void updateContract(ContractRow c) throws SQLException {
        int companyId = getOrCreateCompanyIdByName(c.getCompanyName());
        String sql = "UPDATE contracts SET user_id = ?, company_id = ?, type = ?, position = ?, salary = ?, start_date = ?, end_date = ?, status = ? WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, c.getUserId());
            stmt.setInt(2, companyId);
            stmt.setString(3, c.getType());
            stmt.setString(4, c.getPosition());
            stmt.setDouble(5, c.getSalary());
            stmt.setDate(6, java.sql.Date.valueOf(c.getStartDate()));
            stmt.setDate(7, (c.getEndDate() == null || c.getEndDate().isEmpty()) ? null
                    : java.sql.Date.valueOf(c.getEndDate()));
            stmt.setString(8, c.getStatus());
            stmt.setInt(9, c.getId());
            stmt.executeUpdate();
        }
    }

    public void deleteContract(int id) throws SQLException {
        String sql = "DELETE FROM contracts WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // --- BANK ACCOUNTS ---
    public List<BankAccountRow> getAllBankAccounts() throws SQLException {
        List<BankAccountRow> list = new ArrayList<>();
        String sql = "SELECT b.*, u.full_name FROM bank_accounts b JOIN users u ON b.user_id = u.id";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new BankAccountRow(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("full_name"),
                        rs.getString("bank_name"),
                        rs.getString("iban"),
                        rs.getString("swift"),
                        rs.getString("currency"),
                        rs.getBoolean("is_primary"),
                        rs.getBoolean("is_verified")));
            }
        }
        return list;
    }

    public void addBankAccount(BankAccountRow b) throws SQLException {
        String sql = "INSERT INTO bank_accounts (user_id, bank_name, iban, swift, currency, is_primary, is_verified) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, b.getUserId());
            stmt.setString(2, b.getBankName());
            stmt.setString(3, b.getIban());
            stmt.setString(4, b.getSwift());
            stmt.setString(5, b.getCurrency());
            stmt.setBoolean(6, b.getIsPrimary());
            stmt.setBoolean(7, b.getIsVerified());
            stmt.executeUpdate();
        }
    }

    public void updateBankAccount(BankAccountRow b) throws SQLException {
        String sql = "UPDATE bank_accounts SET user_id = ?, bank_name = ?, iban = ?, swift = ?, currency = ?, is_primary = ?, is_verified = ? WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, b.getUserId());
            stmt.setString(2, b.getBankName());
            stmt.setString(3, b.getIban());
            stmt.setString(4, b.getSwift());
            stmt.setString(5, b.getCurrency());
            stmt.setBoolean(6, b.getIsPrimary());
            stmt.setBoolean(7, b.getIsVerified());
            stmt.setInt(8, b.getId());
            stmt.executeUpdate();
        }
    }

    public void deleteBankAccount(int id) throws SQLException {
        String sql = "DELETE FROM bank_accounts WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // --- BONUSES ---
    public List<BonusRow> getAllBonuses() throws SQLException {
        List<BonusRow> list = new ArrayList<>();
        String sql = "SELECT b.*, u.full_name FROM bonuses b JOIN users u ON b.user_id = u.id";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new BonusRow(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("full_name"),
                        rs.getDouble("amount"),
                        rs.getString("reason"),
                        rs.getDate("date_awarded").toString()));
            }
        }
        return list;
    }

    public void addBonus(BonusRow b) throws SQLException {
        String sql = "INSERT INTO bonuses (user_id, amount, reason, date_awarded) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, b.getUserId());
            stmt.setDouble(2, b.getAmount());
            stmt.setString(3, b.getReason());
            stmt.setDate(4, java.sql.Date.valueOf(b.getDateAwarded()));
            stmt.executeUpdate();
        }
    }

    public void updateBonus(BonusRow b) throws SQLException {
        String sql = "UPDATE bonuses SET user_id = ?, amount = ?, reason = ?, date_awarded = ? WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, b.getUserId());
            stmt.setDouble(2, b.getAmount());
            stmt.setString(3, b.getReason());
            stmt.setDate(4, java.sql.Date.valueOf(b.getDateAwarded()));
            stmt.setInt(5, b.getId());
            stmt.executeUpdate();
        }
    }

    public void deleteBonus(int id) throws SQLException {
        String sql = "DELETE FROM bonuses WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // --- PAYSLIPS ---
    public List<PayslipRow> getAllPayslips() throws SQLException {
        List<PayslipRow> list = new ArrayList<>();
        String sql = "SELECT p.*, u.full_name FROM payslips p JOIN users u ON p.user_id = u.id";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                PayslipRow p = new PayslipRow(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("full_name"),
                        rs.getInt("month"),
                        rs.getInt("year"),
                        rs.getDouble("base_salary"),
                        rs.getDouble("overtime_hours"),
                        rs.getDouble("overtime_total"),
                        rs.getDouble("bonuses"),
                        rs.getString("currency"),
                        rs.getString("status"));
                p.setOtherDeductions(rs.getDouble("other_deductions"));
                list.add(p);
            }
        }
        return list;
    }

    public void addPayslip(PayslipRow p) throws SQLException {
        String sql = "INSERT INTO payslips (user_id, month, year, base_salary, overtime_hours, overtime_total, bonuses, other_deductions, currency, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, p.getUserId());
            stmt.setInt(2, p.getMonth());
            stmt.setInt(3, p.getYear());
            stmt.setDouble(4, p.getBaseSalary());
            stmt.setDouble(5, p.getOvertime());
            stmt.setDouble(6, p.getOvertimeTotal());
            stmt.setDouble(7, p.getBonuses());
            stmt.setDouble(8, p.getOtherDeductions());
            stmt.setString(9, p.getCurrency());
            stmt.setString(10, p.getStatus());
            stmt.executeUpdate();
        }
    }

    public void updatePayslip(PayslipRow p) throws SQLException {
        String sql = "UPDATE payslips SET user_id = ?, month = ?, year = ?, base_salary = ?, overtime_hours = ?, overtime_total = ?, bonuses = ?, other_deductions = ?, status = ? WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, p.getUserId());
            stmt.setInt(2, p.getMonth());
            stmt.setInt(3, p.getYear());
            stmt.setDouble(4, p.getBaseSalary());
            stmt.setDouble(5, p.getOvertime());
            stmt.setDouble(6, p.getOvertimeTotal());
            stmt.setDouble(7, p.getBonuses());
            stmt.setDouble(8, p.getOtherDeductions());
            stmt.setString(9, p.getStatus());
            stmt.setInt(10, p.getId());
            stmt.executeUpdate();
        }
    }

    public void deletePayslip(int id) throws SQLException {
        String sql = "DELETE FROM payslips WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // ==================== EMPLOYEUR: Per-User Queries ====================

    /**
     * Get the full name of a user from the database (more reliable than session
     * data).
     * Falls back to username if full_name is null or blank.
     */
    public String getUserFullName(int userId) {
        String sql = "SELECT full_name, username FROM users WHERE id = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String fn = rs.getString("full_name");
                if (fn != null && !fn.isBlank())
                    return fn;
                return rs.getString("username");
            }
        } catch (SQLException e) {
            System.err.println("[FinanceService] getUserFullName error: " + e.getMessage());
        }
        return "Employe";
    }

    /**
     * Get contracts for a specific employee (by user_id)
     */
    public List<ContractRow> getContractsByUserId(int userId) throws SQLException {
        List<ContractRow> list = new ArrayList<>();
        String sql = "SELECT c.id, c.user_id, c.type, c.position, c.salary, c.start_date, c.end_date, c.status, "
                + "u.full_name, COALESCE(co.name, 'N/A') AS resolved_company "
                + "FROM contracts c "
                + "JOIN users u ON c.user_id = u.id "
                + "LEFT JOIN companies co ON c.company_id = co.id "
                + "WHERE c.user_id = ? "
                + "ORDER BY c.start_date DESC";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String startDateStr = "";
                java.sql.Date startDate = rs.getDate("start_date");
                if (startDate != null)
                    startDateStr = startDate.toString();

                String endDateStr = "";
                java.sql.Date endDate = rs.getDate("end_date");
                if (endDate != null)
                    endDateStr = endDate.toString();

                list.add(new ContractRow(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("full_name"),
                        rs.getString("resolved_company"),
                        rs.getString("type"),
                        rs.getString("position"),
                        rs.getDouble("salary"),
                        startDateStr,
                        endDateStr,
                        rs.getString("status")));
            }
        }
        return list;
    }

    /**
     * Get bank accounts for a specific employee (by user_id)
     */
    public List<BankAccountRow> getBankAccountsByUserId(int userId) throws SQLException {
        List<BankAccountRow> list = new ArrayList<>();
        String sql = "SELECT b.*, u.full_name FROM bank_accounts b JOIN users u ON b.user_id = u.id WHERE b.user_id = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new BankAccountRow(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("full_name"),
                        rs.getString("bank_name"),
                        rs.getString("iban"),
                        rs.getString("swift"),
                        rs.getString("currency"),
                        rs.getBoolean("is_primary"),
                        rs.getBoolean("is_verified")));
            }
        }
        return list;
    }

    /**
     * Get bonuses for a specific employee (by user_id)
     */
    public List<BonusRow> getBonusesByUserId(int userId) throws SQLException {
        List<BonusRow> list = new ArrayList<>();
        String sql = "SELECT b.*, u.full_name FROM bonuses b JOIN users u ON b.user_id = u.id WHERE b.user_id = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new BonusRow(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("full_name"),
                        rs.getDouble("amount"),
                        rs.getString("reason"),
                        rs.getDate("date_awarded").toString()));
            }
        }
        return list;
    }

    /**
     * Get payslips for a specific employee (by user_id)
     */
    public List<PayslipRow> getPayslipsByUserId(int userId) throws SQLException {
        List<PayslipRow> list = new ArrayList<>();
        String sql = "SELECT p.*, u.full_name FROM payslips p JOIN users u ON p.user_id = u.id WHERE p.user_id = ?";
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                PayslipRow p = new PayslipRow(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("full_name"),
                        rs.getInt("month"),
                        rs.getInt("year"),
                        rs.getDouble("base_salary"),
                        rs.getDouble("overtime_hours"),
                        rs.getDouble("overtime_total"),
                        rs.getDouble("bonuses"),
                        rs.getString("currency"),
                        rs.getString("status"));
                p.setOtherDeductions(rs.getDouble("other_deductions"));
                list.add(p);
            }
        }
        return list;
    }

    public List<EmployeeSummaryRow> getEmployeeSummaries() throws SQLException {
        List<EmployeeSummaryRow> list = new ArrayList<>();

        // Utilise des sous-requêtes pour éviter les doublons causés par plusieurs
        // contrats par employé.
        // On prend le contrat ACTIVE/ACTIF en priorité, sinon le plus récent.
        String sql = "SELECT "
                + "    u.id, "
                + "    u.full_name, "
                + "    COALESCE("
                + "       (SELECT position FROM contracts WHERE user_id = u.id AND (status = 'ACTIVE' OR status = 'ACTIF') ORDER BY id DESC LIMIT 1), "
                + "       (SELECT position FROM contracts WHERE user_id = u.id ORDER BY id DESC LIMIT 1)"
                + "    ) AS position, "
                + "    COALESCE("
                + "       (SELECT salary FROM contracts WHERE user_id = u.id AND (status = 'ACTIVE' OR status = 'ACTIF') ORDER BY id DESC LIMIT 1), "
                + "       (SELECT salary FROM contracts WHERE user_id = u.id ORDER BY id DESC LIMIT 1), "
                + "       0"
                + "    ) AS active_salary, "
                + "    COALESCE((SELECT SUM(amount) FROM bonuses WHERE user_id = u.id), 0) AS total_bonuses, "
                + "    COALESCE("
                + "       (SELECT (base_salary + overtime_total + bonuses - other_deductions) "
                + "          FROM payslips WHERE user_id = u.id ORDER BY year DESC, month DESC LIMIT 1), "
                + "       0"
                + "    ) AS last_net_pay, "
                + "    COALESCE("
                + "       (SELECT CASE WHEN is_verified THEN 'Vérifié' ELSE 'En attente' END "
                + "          FROM bank_accounts WHERE user_id = u.id ORDER BY is_primary DESC, id ASC LIMIT 1), "
                + "       'Non configuré'"
                + "    ) AS bank_status "
                + "FROM users u "
                + "WHERE u.role != 'ADMIN' "
                + "ORDER BY u.full_name ASC";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String pos = rs.getString("position");
                list.add(new EmployeeSummaryRow(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        pos != null ? pos : "Aucun contrat",
                        rs.getDouble("active_salary"),
                        rs.getDouble("total_bonuses"),
                        rs.getDouble("last_net_pay"),
                        rs.getString("bank_status")));
            }
        }
        return list;
    }
}
