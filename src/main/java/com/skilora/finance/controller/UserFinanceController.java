package com.skilora.finance.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import com.skilora.finance.model.*;
import com.skilora.finance.service.FinanceService;

import java.net.URL;
import java.util.*;

/**
 * UserFinanceController - READ-ONLY consultation for USER role.
 * 
 * The USER can see everything the ADMIN has added/modified:
 * - All contracts
 * - All bank accounts
 * - All bonuses
 * - All payslips
 * - Tax calculations (read-only)
 * 
 * NO add, update, or delete operations are available.
 */
public class UserFinanceController implements Initializable {

    // ==================== CONTRACTS TABLE ====================
    @FXML
    private TableView<ContractRow> contractTable;
    @FXML
    private TableColumn<ContractRow, String> contract_userCol;
    @FXML
    private TableColumn<ContractRow, String> contract_companyCol;
    @FXML
    private TableColumn<ContractRow, String> contract_typeCol;
    @FXML
    private TableColumn<ContractRow, String> contract_positionCol;
    @FXML
    private TableColumn<ContractRow, Double> contract_salaryCol;
    @FXML
    private TableColumn<ContractRow, String> contract_startCol;
    @FXML
    private TableColumn<ContractRow, String> contract_endCol;
    @FXML
    private TableColumn<ContractRow, String> contract_statusCol;
    @FXML
    private Label contract_countLabel;

    // ==================== BANK ACCOUNTS TABLE ====================
    @FXML
    private TableView<BankAccountRow> bankAccountTable;
    @FXML
    private TableColumn<BankAccountRow, String> bank_userCol;
    @FXML
    private TableColumn<BankAccountRow, String> bank_nameCol;
    @FXML
    private TableColumn<BankAccountRow, String> bank_ibanCol;
    @FXML
    private TableColumn<BankAccountRow, String> bank_swiftCol;
    @FXML
    private TableColumn<BankAccountRow, String> bank_currencyCol;
    @FXML
    private TableColumn<BankAccountRow, Boolean> bank_primaryCol;
    @FXML
    private TableColumn<BankAccountRow, Boolean> bank_verifiedCol;
    @FXML
    private Label bank_countLabel;

    // ==================== BONUSES TABLE ====================
    @FXML
    private TableView<BonusRow> bonusTable;
    @FXML
    private TableColumn<BonusRow, String> bonus_userCol;
    @FXML
    private TableColumn<BonusRow, Double> bonus_amountCol;
    @FXML
    private TableColumn<BonusRow, String> bonus_reasonCol;
    @FXML
    private TableColumn<BonusRow, String> bonus_dateCol;
    @FXML
    private Label bonus_countLabel;

    // ==================== PAYSLIPS TABLE ====================
    @FXML
    private TableView<PayslipRow> payslipTable;
    @FXML
    private TableColumn<PayslipRow, String> payslip_userCol;
    @FXML
    private TableColumn<PayslipRow, String> payslip_periodCol;
    @FXML
    private TableColumn<PayslipRow, Double> payslip_baseCol;
    @FXML
    private TableColumn<PayslipRow, Double> payslip_overtimeCol;
    @FXML
    private TableColumn<PayslipRow, Double> payslip_bonusCol;
    @FXML
    private TableColumn<PayslipRow, Double> payslip_grossCol;
    @FXML
    private TableColumn<PayslipRow, Double> payslip_deductCol;
    @FXML
    private TableColumn<PayslipRow, Double> payslip_netCol;
    @FXML
    private TableColumn<PayslipRow, String> payslip_statusCol;
    @FXML
    private Label payslip_countLabel;

    // ==================== SUMMARY LABELS ====================
    @FXML
    private Label summary_totalEmployees;
    @FXML
    private Label summary_totalContracts;
    @FXML
    private Label summary_totalPayslips;
    @FXML
    private Label summary_totalBonuses;

    private final FinanceService financeService = FinanceService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeContractTable();
        initializeBankTable();
        initializeBonusTable();
        initializePayslipTable();
        loadAllData();
    }

    // ==================== TABLE INITIALIZATION ====================

    private void initializeContractTable() {
        contract_userCol.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        contract_companyCol.setCellValueFactory(new PropertyValueFactory<>("companyName"));
        contract_typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        contract_positionCol.setCellValueFactory(new PropertyValueFactory<>("position"));
        contract_salaryCol.setCellValueFactory(new PropertyValueFactory<>("salary"));
        contract_startCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        contract_endCol.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        contract_statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void initializeBankTable() {
        bank_userCol.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        bank_nameCol.setCellValueFactory(new PropertyValueFactory<>("bankName"));
        bank_ibanCol.setCellValueFactory(new PropertyValueFactory<>("iban"));
        bank_swiftCol.setCellValueFactory(new PropertyValueFactory<>("swift"));
        bank_currencyCol.setCellValueFactory(new PropertyValueFactory<>("currency"));
        bank_primaryCol.setCellValueFactory(new PropertyValueFactory<>("primary"));
        bank_verifiedCol.setCellValueFactory(new PropertyValueFactory<>("verified"));
    }

    private void initializeBonusTable() {
        bonus_userCol.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        bonus_amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        bonus_reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));
        bonus_dateCol.setCellValueFactory(new PropertyValueFactory<>("dateAwarded"));
    }

    private void initializePayslipTable() {
        payslip_userCol.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        payslip_periodCol.setCellValueFactory(new PropertyValueFactory<>("period"));
        payslip_baseCol.setCellValueFactory(new PropertyValueFactory<>("baseSalary"));
        payslip_overtimeCol.setCellValueFactory(new PropertyValueFactory<>("overtimeTotal"));
        payslip_bonusCol.setCellValueFactory(new PropertyValueFactory<>("bonuses"));
        payslip_grossCol.setCellValueFactory(new PropertyValueFactory<>("gross"));
        payslip_deductCol.setCellValueFactory(new PropertyValueFactory<>("totalDeductions"));
        payslip_netCol.setCellValueFactory(new PropertyValueFactory<>("net"));
        payslip_statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    // ==================== DATA LOADING ====================

    private void loadAllData() {
        loadContracts();
        loadBankAccounts();
        loadBonuses();
        loadPayslips();
        updateSummary();
    }

    private void loadContracts() {
        try {
            List<ContractRow> contracts = financeService.getAllContracts();
            ObservableList<ContractRow> data = FXCollections.observableArrayList(contracts);
            contractTable.setItems(data);
            contract_countLabel.setText("Total: " + contracts.size());
        } catch (Exception e) {
            contract_countLabel.setText("Error loading contracts");
            System.err.println("Error loading contracts: " + e.getMessage());
        }
    }

    private void loadBankAccounts() {
        try {
            List<BankAccountRow> accounts = financeService.getAllBankAccounts();
            ObservableList<BankAccountRow> data = FXCollections.observableArrayList(accounts);
            bankAccountTable.setItems(data);
            bank_countLabel.setText("Total: " + accounts.size());
        } catch (Exception e) {
            bank_countLabel.setText("Error loading bank accounts");
            System.err.println("Error loading bank accounts: " + e.getMessage());
        }
    }

    private void loadBonuses() {
        try {
            List<BonusRow> bonuses = financeService.getAllBonuses();
            ObservableList<BonusRow> data = FXCollections.observableArrayList(bonuses);
            bonusTable.setItems(data);
            bonus_countLabel.setText("Total: " + bonuses.size());
        } catch (Exception e) {
            bonus_countLabel.setText("Error loading bonuses");
            System.err.println("Error loading bonuses: " + e.getMessage());
        }
    }

    private void loadPayslips() {
        try {
            List<PayslipRow> payslips = financeService.getAllPayslips();
            ObservableList<PayslipRow> data = FXCollections.observableArrayList(payslips);
            payslipTable.setItems(data);
            payslip_countLabel.setText("Total: " + payslips.size());
        } catch (Exception e) {
            payslip_countLabel.setText("Error loading payslips");
            System.err.println("Error loading payslips: " + e.getMessage());
        }
    }

    private void updateSummary() {
        try {
            int contracts = contractTable.getItems() != null ? contractTable.getItems().size() : 0;
            int payslips = payslipTable.getItems() != null ? payslipTable.getItems().size() : 0;
            int bonuses = bonusTable.getItems() != null ? bonusTable.getItems().size() : 0;

            // Count unique employees from contracts
            Set<String> employees = new HashSet<>();
            if (contractTable.getItems() != null) {
                for (ContractRow c : contractTable.getItems()) {
                    employees.add(c.getEmployeeName());
                }
            }

            summary_totalEmployees.setText(String.valueOf(employees.size()));
            summary_totalContracts.setText(String.valueOf(contracts));
            summary_totalPayslips.setText(String.valueOf(payslips));
            summary_totalBonuses.setText(String.valueOf(bonuses));
        } catch (Exception e) {
            System.err.println("Error updating summary: " + e.getMessage());
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
