package com.skilora.finance.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.finance.entity.ContractRow;
import com.skilora.finance.entity.BankAccountRow;
import com.skilora.finance.entity.BonusRow;
import com.skilora.finance.entity.PayslipRow;
import com.skilora.finance.entity.EmployeeSummaryRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralised data-access service for finance Row view-models.
 * Provides CRUD operations on contracts, bank accounts, bonuses, payslips,
 * and aggregated employee summaries used by admin / employer dashboards.
 *
 * Singleton – obtain via {@link #getInstance()}.
 */
public class FinanceDataService {

    private static volatile FinanceDataService instance;

    private FinanceDataService() { }

    public static FinanceDataService getInstance() {
        if (instance == null) {
            synchronized (FinanceDataService.class) {
                if (instance == null) {
                    instance = new FinanceDataService();
                }
            }
        }
        return instance;
    }

    private Connection getConnection() throws SQLException {
        return DatabaseConfig.getInstance().getConnection();
    }

    // ==================== CONTRACTS ====================

    public List<ContractRow> getAllContracts() throws SQLException {
        List<ContractRow> list = new ArrayList<>();
        String sql = "SELECT c.id, c.user_id, c.type, c.position, c.salary, c.start_date, c.end_date, c.status, "
                + "COALESCE(u.full_name, 'Inconnu') AS full_name, "
                + "COALESCE(c.company_Name, '') AS company_name "
                + "FROM contracts c "
                + "LEFT JOIN users u ON c.user_id = u.id "
                + "ORDER BY c.id DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(buildContractRow(rs));
            }
        }
        return list;
    }

    public List<ContractRow> getContractsByUserId(int userId) throws SQLException {
        List<ContractRow> list = new ArrayList<>();
        String sql = "SELECT c.id, c.user_id, c.type, c.position, c.salary, c.start_date, c.end_date, c.status, "
                + "COALESCE(u.full_name, 'Inconnu') AS full_name, "
                + "COALESCE(c.company_Name, '') AS company_name "
                + "FROM contracts c "
                + "LEFT JOIN users u ON c.user_id = u.id "
                + "WHERE c.user_id = ? "
                + "ORDER BY c.id DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(buildContractRow(rs));
            }
        }
        return list;
    }

    private ContractRow buildContractRow(ResultSet rs) throws SQLException {
        String startDateStr = "";
        java.sql.Date sd = rs.getDate("start_date");
        if (sd != null) startDateStr = sd.toString();

        String endDateStr = "";
        java.sql.Date ed = rs.getDate("end_date");
        if (ed != null) endDateStr = ed.toString();

        String companyName = rs.getString("company_name");
        if (companyName == null) companyName = "";

        return new ContractRow(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("full_name"),
                companyName,
                rs.getString("type"),
                rs.getString("position"),
                rs.getDouble("salary"),
                startDateStr,
                endDateStr,
                rs.getString("status"));
    }

    public void addContract(ContractRow c) throws SQLException {
        String sql = "INSERT INTO contracts (user_id, company_Name, type, position, salary, start_date, end_date, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, c.getUserId());
            stmt.setString(2, c.getCompanyName() != null ? c.getCompanyName() : "");
            stmt.setString(3, c.getType());
            stmt.setString(4, c.getPosition() != null ? c.getPosition() : "");
            stmt.setDouble(5, c.getSalary());
            stmt.setDate(6, java.sql.Date.valueOf(c.getStartDate()));
            stmt.setDate(7, (c.getEndDate() == null || c.getEndDate().isEmpty())
                    ? null : java.sql.Date.valueOf(c.getEndDate()));
            stmt.setString(8, c.getStatus() != null ? c.getStatus() : "ACTIVE");
            stmt.executeUpdate();
        }
    }

    public void updateContract(ContractRow c) throws SQLException {
        String sql = "UPDATE contracts SET user_id = ?, company_Name = ?, type = ?, position = ?, salary = ?, "
                + "start_date = ?, end_date = ?, status = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, c.getUserId());
            stmt.setString(2, c.getCompanyName() != null ? c.getCompanyName() : "");
            stmt.setString(3, c.getType());
            stmt.setString(4, c.getPosition() != null ? c.getPosition() : "");
            stmt.setDouble(5, c.getSalary());
            stmt.setDate(6, java.sql.Date.valueOf(c.getStartDate()));
            stmt.setDate(7, (c.getEndDate() == null || c.getEndDate().isEmpty())
                    ? null : java.sql.Date.valueOf(c.getEndDate()));
            stmt.setString(8, c.getStatus() != null ? c.getStatus() : "ACTIVE");
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

    // ==================== BANK ACCOUNTS ====================

    public List<BankAccountRow> getAllBankAccounts() throws SQLException {
        List<BankAccountRow> list = new ArrayList<>();
        String sql = "SELECT b.id, b.user_id, b.bank_name, b.iban, b.swift, b.currency, b.is_primary, b.is_verified, "
                + "COALESCE(u.full_name, 'Inconnu') AS full_name "
                + "FROM bank_accounts b "
                + "LEFT JOIN users u ON b.user_id = u.id "
                + "ORDER BY b.id ASC";
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

    public List<BankAccountRow> getBankAccountsByUserId(int userId) throws SQLException {
        List<BankAccountRow> list = new ArrayList<>();
        String sql = "SELECT b.*, u.full_name FROM bank_accounts b "
                + "JOIN users u ON b.user_id = u.id WHERE b.user_id = ?";
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

    public void addBankAccount(BankAccountRow b) throws SQLException {
        String accountHolder = b.getAccountHolder();
        if (accountHolder == null || accountHolder.isBlank()) {
            accountHolder = getUserFullName(b.getUserId());
            if (accountHolder == null || accountHolder.isBlank()) accountHolder = "Account Holder";
        }
        String sql = "INSERT INTO bank_accounts (user_id, bank_name, account_holder, iban, swift_bic, currency, is_primary, is_verified) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, b.getUserId());
            stmt.setString(2, b.getBankName());
            stmt.setString(3, accountHolder);
            stmt.setString(4, b.getIban());
            stmt.setString(5, b.getSwift());
            stmt.setString(6, b.getCurrency());
            stmt.setBoolean(7, b.getIsPrimary());
            stmt.setBoolean(8, b.getIsVerified());
            stmt.executeUpdate();
        }
    }

    public void updateBankAccount(BankAccountRow b) throws SQLException {
        String sql = "UPDATE bank_accounts SET user_id = ?, bank_name = ?, iban = ?, swift = ?, "
                + "currency = ?, is_primary = ?, is_verified = ? WHERE id = ?";
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

    // ==================== BONUSES ====================

    public List<BonusRow> getAllBonuses() throws SQLException {
        List<BonusRow> list = new ArrayList<>();
        String sql = "SELECT b.id, b.user_id, b.amount, b.reason, b.date_awarded, "
                + "COALESCE(u.full_name, 'Inconnu') AS full_name "
                + "FROM bonuses b "
                + "LEFT JOIN users u ON b.user_id = u.id "
                + "ORDER BY b.date_awarded DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                java.sql.Date awarded = rs.getDate("date_awarded");
                String dateStr = (awarded != null) ? awarded.toString() : "";
                list.add(new BonusRow(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("full_name"),
                        rs.getDouble("amount"),
                        rs.getString("reason"),
                        dateStr));
            }
        }
        return list;
    }

    public List<BonusRow> getBonusesByUserId(int userId) throws SQLException {
        List<BonusRow> list = new ArrayList<>();
        String sql = "SELECT b.*, u.full_name FROM bonuses b "
                + "JOIN users u ON b.user_id = u.id WHERE b.user_id = ?";
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

    // ==================== PAYSLIPS ====================

    public List<PayslipRow> getAllPayslips() throws SQLException {
        List<PayslipRow> list = new ArrayList<>();
        String sql = "SELECT p.id, p.user_id, p.month, p.year, p.base_salary, p.overtime_hours, "
                + "p.overtime_total, p.bonuses, p.other_deductions, p.currency, p.status, "
                + "COALESCE(u.full_name, 'Inconnu') AS full_name "
                + "FROM payslips p "
                + "LEFT JOIN users u ON p.user_id = u.id "
                + "ORDER BY p.year DESC, p.month DESC";
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

    public List<PayslipRow> getPayslipsByUserId(int userId) throws SQLException {
        List<PayslipRow> list = new ArrayList<>();
        String sql = "SELECT p.*, u.full_name FROM payslips p "
                + "JOIN users u ON p.user_id = u.id WHERE p.user_id = ?";
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

    public void addPayslip(PayslipRow p) throws SQLException {
        int contractId = p.getContractId();
        if (contractId <= 0) {
            contractId = getFirstEmploymentContractIdForUser(p.getUserId());
            if (contractId <= 0) {
                throw new SQLException("No employment contract found for this employee. Create a contract first.");
            }
        }
        double gross = p.getBaseSalary() + p.getOvertimeTotal() + p.getBonuses();
        double cnss = gross * 0.0918;
        double irpp = (gross - cnss) * 0.26;
        double net = gross - cnss - irpp - p.getOtherDeductions();

        String sql = "INSERT INTO payslips (contract_id, user_id, period_month, period_year, "
                + "gross_salary, net_salary, cnss_employee, cnss_employer, irpp, other_deductions, "
                + "bonuses, currency, payment_status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, contractId);
            stmt.setInt(2, p.getUserId());
            stmt.setInt(3, p.getMonth());
            stmt.setInt(4, p.getYear());
            stmt.setDouble(5, gross);
            stmt.setDouble(6, net);
            stmt.setDouble(7, cnss);
            stmt.setDouble(8, gross * 0.1657);
            stmt.setDouble(9, irpp);
            stmt.setDouble(10, p.getOtherDeductions());
            stmt.setDouble(11, p.getBonuses());
            stmt.setString(12, p.getCurrency());
            stmt.executeUpdate();
        }
    }

    /**
     * Returns the first employment contract id for the user (from employment_contracts table).
     * If none exists, creates one from the finance "contracts" table so payslips can be added.
     */
    public int getFirstEmploymentContractIdForUser(int userId) throws SQLException {
        String sql = "SELECT id FROM employment_contracts WHERE user_id = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        }
        // No employment_contract: try to create one from the "contracts" table (Finance Admin tab)
        String sel = "SELECT id, user_id, company_Name, type, position, salary, start_date, end_date, status FROM contracts WHERE user_id = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement selStmt = conn.prepareStatement(sel)) {
            selStmt.setInt(1, userId);
            ResultSet rs = selStmt.executeQuery();
            if (!rs.next()) return 0;
            double salary = rs.getDouble("salary");
            if (rs.wasNull()) salary = 0;
            java.sql.Date startDate = rs.getDate("start_date");
            if (startDate == null) startDate = java.sql.Date.valueOf(java.time.LocalDate.now());
            java.sql.Date endDate = rs.getDate("end_date");
            String contractType = rs.getString("type");
            if (contractType == null || contractType.isBlank()) contractType = "CDI";
            String status = rs.getString("status");
            if (status == null || status.isBlank()) status = "ACTIVE";
            String companyName = rs.getString("company_Name");
            if (companyName == null) companyName = "";
            String position = rs.getString("position");
            if (position == null) position = "";

            String ins = "INSERT INTO employment_contracts (user_id, salary_base, currency, start_date, end_date, contract_type, status, company_name, position) VALUES (?, ?, 'TND', ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insStmt = conn.prepareStatement(ins, PreparedStatement.RETURN_GENERATED_KEYS)) {
                insStmt.setInt(1, userId);
                insStmt.setDouble(2, salary);
                insStmt.setDate(3, startDate);
                insStmt.setDate(4, endDate);
                insStmt.setString(5, contractType);
                insStmt.setString(6, status);
                insStmt.setString(7, companyName);
                insStmt.setString(8, position);
                insStmt.executeUpdate();
                try (ResultSet keys = insStmt.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : 0;
                }
            }
        }
    }

    public void updatePayslip(PayslipRow p) throws SQLException {
        String sql = "UPDATE payslips SET user_id = ?, month = ?, year = ?, base_salary = ?, "
                + "overtime_hours = ?, overtime_total = ?, bonuses = ?, other_deductions = ?, "
                + "status = ? WHERE id = ?";
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

    // ==================== USER HELPERS ====================

    /**
     * Get the full name of a user. Falls back to username if full_name is absent.
     */
    public String getUserFullName(int userId) {
        String sql = "SELECT full_name, username FROM users WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String fn = rs.getString("full_name");
                if (fn != null && !fn.isBlank()) return fn;
                return rs.getString("username");
            }
        } catch (SQLException e) {
            System.err.println("[FinanceDataService] getUserFullName error: " + e.getMessage());
        }
        return "Employe";
    }

    // ==================== EMPLOYEE SUMMARIES ====================

    /**
     * Aggregated summary per employee for the admin dashboard.
     */
    public List<EmployeeSummaryRow> getEmployeeSummaries() throws SQLException {
        List<EmployeeSummaryRow> list = new ArrayList<>();

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
