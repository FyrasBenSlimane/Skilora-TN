package com.skilora.finance.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.finance.entity.EmploymentContract;
import com.skilora.finance.entity.Payslip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * PayslipService
 *
 * CRUD and payslip generation for the payslips table.
 * Handles salary breakdown calculation (CNSS, IRPP, net).
 * No JavaFX imports allowed.
 */
public class PayslipService {

    private static final Logger logger = LoggerFactory.getLogger(PayslipService.class);

    private static final BigDecimal CNSS_EMPLOYEE_RATE = new BigDecimal("0.0918");
    private static final BigDecimal CNSS_EMPLOYER_RATE = new BigDecimal("0.1657");
    private static final BigDecimal TWELVE = new BigDecimal("12");

    private static volatile PayslipService instance;

    private PayslipService() {}

    public static PayslipService getInstance() {
        if (instance == null) {
            synchronized (PayslipService.class) {
                if (instance == null) {
                    instance = new PayslipService();
                }
            }
        }
        return instance;
    }

    // ==================== CRUD Operations ====================

    /**
     * Creates a new payslip.
     * @return generated ID
     */
    public int create(Payslip p) throws SQLException {
        String sql = "INSERT INTO payslips (contract_id, user_id, period_month, period_year, " +
                "gross_salary, net_salary, cnss_employee, cnss_employer, irpp, other_deductions, " +
                "bonuses, currency, payment_status, pdf_url) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, p.getContractId());
            stmt.setInt(2, p.getUserId());
            stmt.setInt(3, p.getPeriodMonth());
            stmt.setInt(4, p.getPeriodYear());
            stmt.setBigDecimal(5, p.getGrossSalary());
            stmt.setBigDecimal(6, p.getNetSalary());
            stmt.setBigDecimal(7, p.getCnssEmployee());
            stmt.setBigDecimal(8, p.getCnssEmployer());
            stmt.setBigDecimal(9, p.getIrpp());
            stmt.setBigDecimal(10, p.getOtherDeductions());
            stmt.setBigDecimal(11, p.getBonuses());
            stmt.setString(12, p.getCurrency());
            stmt.setString(13, p.getPaymentStatus());
            stmt.setString(14, p.getPdfUrl());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating payslip failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    p.setId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Creating payslip failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Finds a payslip by ID with JOIN for userName.
     */
    public Payslip findById(int id) throws SQLException {
        String sql = "SELECT p.*, u.full_name AS user_name FROM payslips p " +
                "LEFT JOIN users u ON p.user_id = u.id WHERE p.id = ?";

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
     * Finds all payslips for a contract, ordered by period DESC.
     */
    public List<Payslip> findByContractId(int contractId) throws SQLException {
        String sql = "SELECT p.*, u.full_name AS user_name FROM payslips p " +
                "LEFT JOIN users u ON p.user_id = u.id " +
                "WHERE p.contract_id = ? ORDER BY p.period_year DESC, p.period_month DESC";
        List<Payslip> payslips = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, contractId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    payslips.add(mapResultSet(rs));
                }
            }
        }
        return payslips;
    }

    /**
     * Finds all payslips for a user.
     */
    public List<Payslip> findByUserId(int userId) throws SQLException {
        String sql = "SELECT p.*, u.full_name AS user_name FROM payslips p " +
                "LEFT JOIN users u ON p.user_id = u.id " +
                "WHERE p.user_id = ? ORDER BY p.period_year DESC, p.period_month DESC";
        List<Payslip> payslips = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    payslips.add(mapResultSet(rs));
                }
            }
        }
        return payslips;
    }

    /**
     * Updates payment status. Sets payment_date=NOW() if status is 'PAID'.
     */
    public boolean updatePaymentStatus(int id, String status) throws SQLException {
        String sql;
        if ("PAID".equalsIgnoreCase(status)) {
            sql = "UPDATE payslips SET payment_status = ?, payment_date = NOW() WHERE id = ?";
        } else {
            sql = "UPDATE payslips SET payment_status = ? WHERE id = ?";
        }

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a payslip only if paymentStatus is 'PENDING'.
     */
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM payslips WHERE id = ? AND payment_status = 'PENDING'";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    // ==================== Business Logic ====================

    /**
     * Generates a payslip for a given contract and period.
     * Calculates CNSS employee/employer contributions, IRPP, and net salary.
     *
     * @param contractId the employment contract ID
     * @param month      the pay period month (1-12)
     * @param year       the pay period year
     * @return the generated Payslip with all calculations, or null on failure
     */
    public Payslip generatePayslip(int contractId, int month, int year) {
        try {
            // 1. Get contract
            EmploymentContract contract = ContractService.getInstance().findById(contractId);
            if (contract == null) {
                logger.error("Cannot generate payslip: contract {} not found", contractId);
                return null;
            }

            // 2. Get gross salary
            BigDecimal gross = contract.getSalaryBase();
            if (gross == null || gross.compareTo(BigDecimal.ZERO) <= 0) {
                logger.error("Cannot generate payslip: invalid salary base for contract {}", contractId);
                return null;
            }

            // 3. CNSS employee = gross * 9.18%
            BigDecimal cnssEmployee = gross.multiply(CNSS_EMPLOYEE_RATE)
                    .setScale(2, RoundingMode.HALF_UP);

            // 4. CNSS employer = gross * 16.57%
            BigDecimal cnssEmployer = gross.multiply(CNSS_EMPLOYER_RATE)
                    .setScale(2, RoundingMode.HALF_UP);

            // 5. IRPP = annual IRPP / 12
            BigDecimal annualGross = gross.multiply(TWELVE);
            BigDecimal annualIrpp = TaxConfigurationService.getInstance()
                    .calculateIRPP(annualGross, "Tunisia");
            BigDecimal monthlyIrpp = annualIrpp.divide(TWELVE, 2, RoundingMode.HALF_UP);

            // 6. Net = gross - cnssEmployee - irpp - otherDeductions + bonuses
            BigDecimal otherDeductions = BigDecimal.ZERO;
            BigDecimal bonuses = BigDecimal.ZERO;
            BigDecimal net = gross.subtract(cnssEmployee)
                    .subtract(monthlyIrpp)
                    .subtract(otherDeductions)
                    .add(bonuses)
                    .setScale(2, RoundingMode.HALF_UP);

            // 7. Create and persist payslip
            Payslip payslip = new Payslip(contractId, contract.getUserId(), month, year, gross);
            payslip.setNetSalary(net);
            payslip.setCnssEmployee(cnssEmployee);
            payslip.setCnssEmployer(cnssEmployer);
            payslip.setIrpp(monthlyIrpp);
            payslip.setOtherDeductions(otherDeductions);
            payslip.setBonuses(bonuses);
            payslip.setCurrency(contract.getCurrency());

            int generatedId = create(payslip);
            payslip.setId(generatedId);

            logger.info("Generated payslip #{} for contract {} period {}/{}: gross={}, net={}",
                    generatedId, contractId, month, year, gross, net);

            return payslip;

        } catch (SQLException e) {
            logger.error("Failed to generate payslip for contract {} period {}/{}",
                    contractId, month, year, e);
            return null;
        }
    }

    // ==================== Private Helpers ====================

    /**
     * Maps a ResultSet row to a Payslip entity.
     * Uses getBigDecimal for all monetary fields.
     */
    private Payslip mapResultSet(ResultSet rs) throws SQLException {
        Payslip p = new Payslip();
        p.setId(rs.getInt("id"));
        p.setContractId(rs.getInt("contract_id"));
        p.setUserId(rs.getInt("user_id"));
        p.setPeriodMonth(rs.getInt("period_month"));
        p.setPeriodYear(rs.getInt("period_year"));
        p.setGrossSalary(rs.getBigDecimal("gross_salary"));
        p.setNetSalary(rs.getBigDecimal("net_salary"));
        p.setCnssEmployee(rs.getBigDecimal("cnss_employee"));
        p.setCnssEmployer(rs.getBigDecimal("cnss_employer"));
        p.setIrpp(rs.getBigDecimal("irpp"));
        p.setOtherDeductions(rs.getBigDecimal("other_deductions"));
        p.setBonuses(rs.getBigDecimal("bonuses"));
        p.setCurrency(rs.getString("currency"));
        p.setPaymentStatus(rs.getString("payment_status"));

        Timestamp paymentDate = rs.getTimestamp("payment_date");
        p.setPaymentDate(paymentDate != null ? paymentDate.toLocalDateTime() : null);

        p.setPdfUrl(rs.getString("pdf_url"));

        Timestamp createdDate = rs.getTimestamp("created_date");
        p.setCreatedDate(createdDate != null ? createdDate.toLocalDateTime() : null);

        // Transient fields
        try {
            p.setUserName(rs.getString("user_name"));
        } catch (SQLException ignored) {}

        return p;
    }
}
