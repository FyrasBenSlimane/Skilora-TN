package com.skilora.finance.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import com.skilora.finance.model.*;
import com.skilora.finance.service.FinanceService;
import com.skilora.finance.service.TaxCalculationService;

import java.math.BigDecimal;
import java.net.URL;
import java.util.*;

/**
 * EmployeurFinanceController - READ-ONLY consultation for EMPLOYER role.
 * 
 * The EMPLOYER can see ONLY their own financial data:
 * - Their contracts
 * - Their bank accounts
 * - Their bonuses (primes)
 * - Their payslips (with CNSS/IRPP calculations)
 * - Salary summary and tax breakdown
 * 
 * NO add, update, or delete operations are available.
 * Data is filtered by the logged-in user's ID.
 */
public class EmployeurFinanceController implements Initializable {

    // Employee ID set after login
    private int employeeId = -1;
    private String employeeName = "";

    // ==================== MY CONTRACT ====================
    @FXML
    private TableView<ContractRow> myContractTable;
    @FXML
    private TableColumn<ContractRow, Integer> my_contract_idCol;
    @FXML
    private TableColumn<ContractRow, Integer> my_contract_companyCol;
    @FXML
    private TableColumn<ContractRow, String> my_contract_typeCol;
    @FXML
    private TableColumn<ContractRow, String> my_contract_positionCol;
    @FXML
    private TableColumn<ContractRow, Double> my_contract_salaryCol;
    @FXML
    private TableColumn<ContractRow, String> my_contract_startCol;
    @FXML
    private TableColumn<ContractRow, String> my_contract_endCol;
    @FXML
    private TableColumn<ContractRow, String> my_contract_statusCol;
    @FXML
    private Label my_contract_countLabel;

    // ==================== MY BANK ACCOUNTS ====================
    @FXML
    private TableView<BankAccountRow> myBankTable;
    @FXML
    private TableColumn<BankAccountRow, Integer> my_bank_idCol;
    @FXML
    private TableColumn<BankAccountRow, String> my_bank_nameCol;
    @FXML
    private TableColumn<BankAccountRow, String> my_bank_ibanCol;
    @FXML
    private TableColumn<BankAccountRow, String> my_bank_swiftCol;
    @FXML
    private TableColumn<BankAccountRow, String> my_bank_currencyCol;
    @FXML
    private TableColumn<BankAccountRow, Boolean> my_bank_primaryCol;
    @FXML
    private TableColumn<BankAccountRow, Boolean> my_bank_verifiedCol;
    @FXML
    private Label my_bank_countLabel;

    // ==================== MY BONUSES ====================
    @FXML
    private TableView<BonusRow> myBonusTable;
    @FXML
    private TableColumn<BonusRow, Integer> my_bonus_idCol;
    @FXML
    private TableColumn<BonusRow, Double> my_bonus_amountCol;
    @FXML
    private TableColumn<BonusRow, String> my_bonus_reasonCol;
    @FXML
    private TableColumn<BonusRow, String> my_bonus_dateCol;
    @FXML
    private Label my_bonus_countLabel;

    // ==================== MY PAYSLIPS ====================
    @FXML
    private TableView<PayslipRow> myPayslipTable;
    @FXML
    private TableColumn<PayslipRow, Integer> my_payslip_idCol;
    @FXML
    private TableColumn<PayslipRow, String> my_payslip_periodCol;
    @FXML
    private TableColumn<PayslipRow, Double> my_payslip_baseCol;
    @FXML
    private TableColumn<PayslipRow, Double> my_payslip_overtimeCol;
    @FXML
    private TableColumn<PayslipRow, Double> my_payslip_bonusCol;
    @FXML
    private TableColumn<PayslipRow, Double> my_payslip_grossCol;
    @FXML
    private TableColumn<PayslipRow, Double> my_payslip_deductCol;
    @FXML
    private TableColumn<PayslipRow, Double> my_payslip_netCol;
    @FXML
    private TableColumn<PayslipRow, String> my_payslip_statusCol;
    @FXML
    private Label my_payslip_countLabel;

    // ==================== SALARY SUMMARY ====================
    @FXML
    private Label summary_employeeName;
    @FXML
    private Label summary_currentSalary;
    @FXML
    private Label summary_totalBonuses;
    @FXML
    private Label summary_cnssDeduction;
    @FXML
    private Label summary_irppTax;
    @FXML
    private Label summary_netSalary;
    @FXML
    private TextArea taxBreakdownArea;

    private final FinanceService financeService = FinanceService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeContractTable();
        initializeBankTable();
        initializeBonusTable();
        initializePayslipTable();
        // Data will be loaded when setEmployeeId is called
    }

    /**
     * Called by MainView after the FXML is loaded, to inject the current user ID.
     */
    public void setEmployeeId(int userId, String name) {
        this.employeeId = userId;
        this.employeeName = name;
        loadAllData();
    }

    // ==================== TABLE INITIALIZATION ====================

    private void initializeContractTable() {
        my_contract_idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        my_contract_companyCol.setCellValueFactory(new PropertyValueFactory<>("companyId"));
        my_contract_typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        my_contract_positionCol.setCellValueFactory(new PropertyValueFactory<>("position"));
        my_contract_salaryCol.setCellValueFactory(new PropertyValueFactory<>("salary"));
        my_contract_startCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        my_contract_endCol.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        my_contract_statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void initializeBankTable() {
        my_bank_idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        my_bank_nameCol.setCellValueFactory(new PropertyValueFactory<>("bankName"));
        my_bank_ibanCol.setCellValueFactory(new PropertyValueFactory<>("iban"));
        my_bank_swiftCol.setCellValueFactory(new PropertyValueFactory<>("swift"));
        my_bank_currencyCol.setCellValueFactory(new PropertyValueFactory<>("currency"));
        my_bank_primaryCol.setCellValueFactory(new PropertyValueFactory<>("isPrimary"));
        my_bank_verifiedCol.setCellValueFactory(new PropertyValueFactory<>("isVerified"));
    }

    private void initializeBonusTable() {
        my_bonus_idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        my_bonus_amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        my_bonus_reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));
        my_bonus_dateCol.setCellValueFactory(new PropertyValueFactory<>("dateAwarded"));
    }

    private void initializePayslipTable() {
        my_payslip_idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        my_payslip_periodCol.setCellValueFactory(new PropertyValueFactory<>("period"));
        my_payslip_baseCol.setCellValueFactory(new PropertyValueFactory<>("baseSalary"));
        my_payslip_overtimeCol.setCellValueFactory(new PropertyValueFactory<>("overtimeTotal"));
        my_payslip_bonusCol.setCellValueFactory(new PropertyValueFactory<>("bonuses"));
        my_payslip_grossCol.setCellValueFactory(new PropertyValueFactory<>("gross"));
        my_payslip_deductCol.setCellValueFactory(new PropertyValueFactory<>("totalDeductions"));
        my_payslip_netCol.setCellValueFactory(new PropertyValueFactory<>("net"));
        my_payslip_statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    // ==================== DATA LOADING ====================

    private void loadAllData() {
        if (employeeId <= 0)
            return;

        loadContracts();
        loadBankAccounts();
        loadBonuses();
        loadPayslips();
        updateSalarySummary();
    }

    private void loadContracts() {
        try {
            List<ContractRow> contracts = financeService.getContractsByUserId(employeeId);
            ObservableList<ContractRow> data = FXCollections.observableArrayList(contracts);
            myContractTable.setItems(data);
            my_contract_countLabel.setText("Total: " + contracts.size());
        } catch (Exception e) {
            my_contract_countLabel.setText("Erreur de chargement");
            System.err.println("Error loading contracts for user " + employeeId + ": " + e.getMessage());
        }
    }

    private void loadBankAccounts() {
        try {
            List<BankAccountRow> accounts = financeService.getBankAccountsByUserId(employeeId);
            ObservableList<BankAccountRow> data = FXCollections.observableArrayList(accounts);
            myBankTable.setItems(data);
            my_bank_countLabel.setText("Total: " + accounts.size());
        } catch (Exception e) {
            my_bank_countLabel.setText("Erreur de chargement");
            System.err.println("Error loading bank accounts for user " + employeeId + ": " + e.getMessage());
        }
    }

    private void loadBonuses() {
        try {
            List<BonusRow> bonuses = financeService.getBonusesByUserId(employeeId);
            ObservableList<BonusRow> data = FXCollections.observableArrayList(bonuses);
            myBonusTable.setItems(data);
            my_bonus_countLabel.setText("Total: " + bonuses.size());
        } catch (Exception e) {
            my_bonus_countLabel.setText("Erreur de chargement");
            System.err.println("Error loading bonuses for user " + employeeId + ": " + e.getMessage());
        }
    }

    private void loadPayslips() {
        try {
            List<PayslipRow> payslips = financeService.getPayslipsByUserId(employeeId);
            ObservableList<PayslipRow> data = FXCollections.observableArrayList(payslips);
            myPayslipTable.setItems(data);
            my_payslip_countLabel.setText("Total: " + payslips.size());
        } catch (Exception e) {
            my_payslip_countLabel.setText("Erreur de chargement");
            System.err.println("Error loading payslips for user " + employeeId + ": " + e.getMessage());
        }
    }

    /**
     * Calculates and displays a salary/tax summary for the employee.
     */
    private void updateSalarySummary() {
        try {
            summary_employeeName.setText(employeeName);

            // Get the latest contract salary
            List<ContractRow> contracts = financeService.getContractsByUserId(employeeId);
            double currentSalary = 0;
            if (!contracts.isEmpty()) {
                currentSalary = contracts.get(0).getSalary();
            }

            // If no contract, try to get salary from latest payslip
            List<PayslipRow> payslips = financeService.getPayslipsByUserId(employeeId);
            if (currentSalary == 0 && !payslips.isEmpty()) {
                currentSalary = payslips.get(0).getBaseSalary();
            }

            summary_currentSalary.setText(String.format("%.2f TND", currentSalary));

            // Calculate total bonuses from bonus table
            List<BonusRow> bonuses = financeService.getBonusesByUserId(employeeId);
            double totalBonuses = 0;
            for (BonusRow b : bonuses) {
                totalBonuses += b.getAmount();
            }
            // Also add bonuses from payslips
            for (PayslipRow ps : payslips) {
                totalBonuses += ps.getBonuses();
            }
            summary_totalBonuses.setText(String.format("%.2f TND", totalBonuses));

            // Calculate CNSS & IRPP using TaxCalculationService
            if (currentSalary > 0) {
                Map<String, BigDecimal> breakdown = TaxCalculationService.calculateCompleteSalary(
                        BigDecimal.valueOf(currentSalary));

                summary_cnssDeduction.setText(String.format("%.2f TND", breakdown.get("cnssEmployee").doubleValue()));
                summary_irppTax.setText(String.format("%.2f TND", breakdown.get("irpp").doubleValue()));
                summary_netSalary.setText(String.format("%.2f TND", breakdown.get("netSalary").doubleValue()));

                // Build detailed tax breakdown text
                StringBuilder sb = new StringBuilder();
                sb.append("═══════════════════════════════════════════\n");
                sb.append("   📊 DÉTAIL DU CALCUL DE PAIE\n");
                sb.append("   Employé: ").append(employeeName).append("\n");
                sb.append("═══════════════════════════════════════════\n\n");
                sb.append(String.format("💰 Salaire Brut:          %,.2f TND\n", currentSalary));
                sb.append(String.format("🎁 Total Primes:          %,.2f TND\n", totalBonuses));
                sb.append("\n── Déductions ──────────────────────────\n");
                sb.append(String.format("📋 CNSS (9.18%%):          %,.2f TND\n",
                        breakdown.get("cnssEmployee").doubleValue()));
                sb.append(String.format("📋 CNSS Employeur (16.5%%): %,.2f TND\n",
                        breakdown.get("cnssEmployer").doubleValue()));
                sb.append(String.format("📋 IRPP:                  %,.2f TND\n", breakdown.get("irpp").doubleValue()));
                sb.append(String.format("📋 Total Déductions:      %,.2f TND\n",
                        breakdown.get("totalDeductions").doubleValue()));
                sb.append(String.format("📊 Taux Effectif:         %.2f%%\n",
                        breakdown.get("effectiveTaxRate").doubleValue()));
                sb.append("\n── Résultat ────────────────────────────\n");
                sb.append(String.format("✅ SALAIRE NET:            %,.2f TND\n",
                        breakdown.get("netSalary").doubleValue()));
                sb.append("\n═══════════════════════════════════════════\n");

                // Add payslip history summary
                if (!payslips.isEmpty()) {
                    sb.append("\n── Historique Bulletins ─────────────────\n");
                    for (PayslipRow ps : payslips) {
                        sb.append(String.format("📄 %s | Base: %,.2f | Brut: %,.2f | Net: %,.2f | %s\n",
                                ps.getPeriod(), ps.getBaseSalary(), ps.getGross(), ps.getNet(), ps.getStatus()));
                    }
                }

                // Add optimization recommendations
                Map<String, String> recommendations = TaxCalculationService.getOptimizationRecommendations(
                        BigDecimal.valueOf(currentSalary));
                sb.append("\n💡 Tranche Fiscale: ").append(recommendations.get("taxBracket")).append("\n");
                sb.append("💡 Conseil: ").append(recommendations.get("recommendation")).append("\n");

                taxBreakdownArea.setText(sb.toString());
            } else if (!payslips.isEmpty()) {
                // No contract but has payslips - show payslip summary
                PayslipRow latest = payslips.get(0);
                summary_cnssDeduction.setText(String.format("%.2f TND", latest.getCnss()));
                summary_irppTax.setText(String.format("%.2f TND", latest.getIrpp()));
                summary_netSalary.setText(String.format("%.2f TND", latest.getNet()));

                StringBuilder sb = new StringBuilder();
                sb.append("═══════════════════════════════════════════\n");
                sb.append("   📊 RÉSUMÉ DE VOS BULLETINS DE PAIE\n");
                sb.append("   Employé: ").append(employeeName).append("\n");
                sb.append("═══════════════════════════════════════════\n\n");
                for (PayslipRow ps : payslips) {
                    sb.append(String.format("📄 Période: %s\n", ps.getPeriod()));
                    sb.append(String.format("   Base: %,.2f | Primes: %,.2f | Brut: %,.2f\n",
                            ps.getBaseSalary(), ps.getBonuses(), ps.getGross()));
                    sb.append(String.format("   CNSS: %,.2f | IRPP: %,.2f | Net: %,.2f\n",
                            ps.getCnss(), ps.getIrpp(), ps.getNet()));
                    sb.append(String.format("   Statut: %s\n\n", ps.getStatus()));
                }
                taxBreakdownArea.setText(sb.toString());
            } else {
                summary_cnssDeduction.setText("0.00 TND");
                summary_irppTax.setText("0.00 TND");
                summary_netSalary.setText("0.00 TND");
                taxBreakdownArea.setText("Aucune donnée trouvée. Contactez l'administrateur.");
            }

        } catch (Exception e) {
            System.err.println("Error updating salary summary: " + e.getMessage());
            taxBreakdownArea.setText("Erreur: " + e.getMessage());
        }
    }

    // ==================== REFRESH ACTIONS ====================

    @FXML
    private void handleRefreshAll() {
        loadAllData();
    }

    @FXML
    private void handleRefreshContracts() {
        loadContracts();
    }

    @FXML
    private void handleRefreshBankAccounts() {
        loadBankAccounts();
    }

    @FXML
    private void handleRefreshBonuses() {
        loadBonuses();
    }

    @FXML
    private void handleRefreshPayslips() {
        loadPayslips();
    }
}
