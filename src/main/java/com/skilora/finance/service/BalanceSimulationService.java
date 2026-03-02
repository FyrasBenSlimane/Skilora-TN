package com.skilora.finance.service;

import com.skilora.finance.entity.EmploymentContract;
import com.skilora.finance.entity.Payslip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * BalanceSimulationService
 *
 * Simulates bank account balance projections based on contracts, payslips, and payments.
 */
public class BalanceSimulationService {

    private static final Logger logger = LoggerFactory.getLogger(BalanceSimulationService.class);

    private static final BigDecimal CNSS_EMPLOYEE_RATE = new BigDecimal("0.0918");
    private static final BigDecimal TWELVE = new BigDecimal("12");

    private static volatile BalanceSimulationService instance;

    private BalanceSimulationService() {}

    public static BalanceSimulationService getInstance() {
        if (instance == null) {
            synchronized (BalanceSimulationService.class) {
                if (instance == null) {
                    instance = new BalanceSimulationService();
                }
            }
        }
        return instance;
    }

    /**
     * Inner class representing a monthly balance projection.
     */
    public static class BalanceProjection {
        private final LocalDate month;
        private final double projectedIncome;
        private final double projectedDeductions;
        private final double projectedBalance;

        public BalanceProjection(LocalDate month, double projectedIncome, double projectedDeductions, double projectedBalance) {
            this.month = month;
            this.projectedIncome = projectedIncome;
            this.projectedDeductions = projectedDeductions;
            this.projectedBalance = projectedBalance;
        }

        public LocalDate getMonth() { return month; }
        public double getProjectedIncome() { return projectedIncome; }
        public double getProjectedDeductions() { return projectedDeductions; }
        public double getProjectedBalance() { return projectedBalance; }
    }

    /**
     * Gets the current balance for a user (sum of all PAID payment transactions).
     *
     * @param userId the user ID
     * @return current balance
     */
    public double getCurrentBalance(int userId) {
        try {
            BigDecimal total = PaymentTransactionService.getInstance().getTotalPaidByUser(userId);
            return total != null ? total.doubleValue() : 0.0;
        } catch (Exception e) {
            logger.error("Failed to get current balance for user {}: {}", userId, e.getMessage());
            return 0.0;
        }
    }

    /**
     * Projects monthly balance for the next N months based on active contract salary,
     * known deductions (CNSS, IRPP), and pending escrow (placeholder: 0).
     *
     * @param userId the user ID
     * @param months number of months to project
     * @return list of BalanceProjection for each month
     */
    public List<BalanceProjection> projectBalance(int userId, int months) {
        List<BalanceProjection> projections = new ArrayList<>();

        try {
            double runningBalance = getCurrentBalance(userId);
            EmploymentContract contract = ContractService.getInstance().findActiveByUserId(userId);

            BigDecimal monthlyGross = BigDecimal.ZERO;
            BigDecimal monthlyDeductions = BigDecimal.ZERO;

            if (contract != null && contract.getSalaryBase() != null && contract.getSalaryBase().compareTo(BigDecimal.ZERO) > 0) {
                monthlyGross = contract.getSalaryBase();
                BigDecimal cnssEmployee = monthlyGross.multiply(CNSS_EMPLOYEE_RATE).setScale(2, RoundingMode.HALF_UP);
                BigDecimal annualGross = monthlyGross.multiply(TWELVE);
                BigDecimal annualIrpp = TaxConfigurationService.getInstance()
                        .calculateIRPP(annualGross, "Tunisia");
                BigDecimal monthlyIrpp = annualIrpp.divide(TWELVE, 2, RoundingMode.HALF_UP);
                monthlyDeductions = cnssEmployee.add(monthlyIrpp);
            }

            LocalDate start = LocalDate.now().withDayOfMonth(1);
            BigDecimal pendingEscrow = BigDecimal.ZERO; // No escrow table in codebase; placeholder

            for (int i = 0; i < months; i++) {
                LocalDate month = start.plusMonths(i);
                double income = monthlyGross.doubleValue();
                double deductions = monthlyDeductions.doubleValue();
                double escrow = pendingEscrow.doubleValue();

                runningBalance = runningBalance + income - deductions - escrow;

                projections.add(new BalanceProjection(month, income, deductions + escrow, runningBalance));
            }

        } catch (Exception e) {
            logger.error("Failed to project balance for user {}: {}", userId, e.getMessage());
        }

        return projections;
    }

    /**
     * Validates that all expected payslips exist for the contract duration.
     *
     * @param contractId the contract ID
     * @return list of missing months (year-month) that have no payslip
     */
    public List<LocalDate> validatePayroll(int contractId) {
        List<LocalDate> missing = new ArrayList<>();

        try {
            EmploymentContract contract = ContractService.getInstance().findById(contractId);
            if (contract == null) {
                logger.warn("Contract {} not found for payroll validation", contractId);
                return missing;
            }

            LocalDate start = contract.getStartDate();
            LocalDate end = contract.getEndDate() != null ? contract.getEndDate() : LocalDate.now();

            if (start == null) {
                return missing;
            }

            List<Payslip> existing = PayslipService.getInstance().findByContractId(contractId);

            LocalDate cursor = start.withDayOfMonth(1);
            while (!cursor.isAfter(end)) {
                final int m = cursor.getMonthValue();
                final int y = cursor.getYear();

                boolean found = existing.stream()
                        .anyMatch(p -> p.getPeriodMonth() == m && p.getPeriodYear() == y);

                if (!found) {
                    missing.add(cursor);
                }
                cursor = cursor.plusMonths(1);
            }

        } catch (Exception e) {
            logger.error("Failed to validate payroll for contract {}: {}", contractId, e.getMessage());
        }

        return missing;
    }
}
