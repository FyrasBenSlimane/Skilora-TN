package com.skilora.finance.controller;

import com.skilora.framework.components.*;
import com.skilora.user.entity.*;
import com.skilora.finance.entity.*;
import com.skilora.finance.service.*;
import com.skilora.user.service.UserService;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.skilora.finance.utils.PDFGenerator;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * FinanceAdminController - Admin finance view (branch layout: TabPane + forms/tables in FXML).
 * Pixel-perfect copy from GestionFinance branch: Contracts, Bank Accounts, Bonuses, Payslips, Reports.
 */
public class FinanceAdminController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(FinanceAdminController.class);

    @FXML private TLTextField mainSearchField;

    // Contract tab (FXML-injected)
    @FXML private TLComboBox<String> contract_userIdCombo;
    @FXML private TLTextField contract_companyIdField;
    @FXML private TLComboBox<String> contract_typeCombo;
    @FXML private TLTextField contract_positionField;
    @FXML private TLTextField contract_salaryField;
    @FXML private TLDatePicker contract_startDatePicker;
    @FXML private TLDatePicker contract_endDatePicker;
    @FXML private TLComboBox<String> contract_statusCombo;
    @FXML private Label contract_seniorityLabel;
    @FXML private Label contract_errorLabel;
    @FXML private TableView<EmploymentContract> contractTable;
    @FXML private TableColumn<EmploymentContract, String> contract_userCol;
    @FXML private TableColumn<EmploymentContract, String> contract_companyCol;
    @FXML private TableColumn<EmploymentContract, String> contract_typeCol;
    @FXML private TableColumn<EmploymentContract, String> contract_positionCol;
    @FXML private TableColumn<EmploymentContract, String> contract_salaryCol;
    @FXML private TableColumn<EmploymentContract, String> contract_startCol;
    @FXML private TableColumn<EmploymentContract, String> contract_endCol;
    @FXML private TableColumn<EmploymentContract, String> contract_statusCol;
    @FXML private Label contract_countLabel;
    @FXML private TLButton contract_addBtn;
    @FXML private TLButton contract_updateBtn;
    @FXML private TLButton contract_clearBtn;
    @FXML private TLButton contract_deleteBtn;
    @FXML private TableColumn<EmploymentContract, ?> contract_idCol;

    // Bank tab
    @FXML private TLComboBox<String> bank_userIdCombo;
    @FXML private TLTextField bank_nameField;
    @FXML private TLTextField bank_ibanField;
    @FXML private TLTextField bank_swiftField;
    @FXML private TLComboBox<String> bank_currencyCombo;
    @FXML private TLComboBox<String> bank_primaryCombo;
    @FXML private TLComboBox<String> bank_verifiedCombo;
    @FXML private Label bank_errorLabel;
    @FXML private TableView<com.skilora.finance.entity.BankAccountRow> bankAccountTable;
    @FXML private TableColumn<com.skilora.finance.entity.BankAccountRow, String> bank_userCol;
    @FXML private TableColumn<com.skilora.finance.entity.BankAccountRow, String> bank_nameCol;
    @FXML private TableColumn<com.skilora.finance.entity.BankAccountRow, String> bank_ibanCol;
    @FXML private TableColumn<com.skilora.finance.entity.BankAccountRow, String> bank_swiftCol;
    @FXML private TableColumn<com.skilora.finance.entity.BankAccountRow, String> bank_currencyCol;
    @FXML private TableColumn<com.skilora.finance.entity.BankAccountRow, String> bank_primaryCol;
    @FXML private TableColumn<com.skilora.finance.entity.BankAccountRow, String> bank_verifiedCol;
    @FXML private Label bank_countLabel;
    @FXML private TLButton bank_addBtn;
    @FXML private TLButton bank_updateBtn;
    @FXML private TLButton bank_clearBtn;
    @FXML private TLButton bank_deleteBtn;
    @FXML private TableColumn<com.skilora.finance.entity.BankAccountRow, ?> bank_idCol;

    // Bonus tab
    @FXML private TLComboBox<String> bonus_userIdCombo;
    @FXML private TLTextField bonus_amountField;
    @FXML private TLTextField bonus_reasonField;
    @FXML private Label bonus_errorLabel;
    @FXML private TableView<com.skilora.finance.entity.BonusRow> bonusTable;
    @FXML private TableColumn<com.skilora.finance.entity.BonusRow, String> bonus_userCol;
    @FXML private TableColumn<com.skilora.finance.entity.BonusRow, String> bonus_amountCol;
    @FXML private TableColumn<com.skilora.finance.entity.BonusRow, String> bonus_reasonCol;
    @FXML private TableColumn<com.skilora.finance.entity.BonusRow, String> bonus_dateCol;
    @FXML private Label bonus_countLabel;
    @FXML private TLButton bonus_addBtn;
    @FXML private TLButton bonus_updateBtn;
    @FXML private TLButton bonus_clearBtn;
    @FXML private TLButton bonus_deleteBtn;
    @FXML private TableColumn<com.skilora.finance.entity.BonusRow, ?> bonus_idCol;

    // Payslip tab
    @FXML private TLComboBox<String> payslip_userIdCombo;
    @FXML private TLComboBox<Integer> payslip_monthCombo;
    @FXML private TLComboBox<Integer> payslip_yearCombo;
    @FXML private TLComboBox<String> payslip_currencyCombo;
    @FXML private TLTextField payslip_baseSalaryField;
    @FXML private TLTextField payslip_overtimeField;
    @FXML private TLTextField payslip_overtimeRateField;
    @FXML private TLTextField payslip_bonusesField;
    @FXML private TLTextField payslip_cnssField;
    @FXML private TLTextField payslip_irppField;
    @FXML private TLTextField payslip_otherDeductionsField;
    @FXML private TLComboBox<String> payslip_statusCombo;
    @FXML private Label payslip_grossLabel;
    @FXML private Label payslip_cnssLabel;
    @FXML private Label payslip_irppLabel;
    @FXML private Label payslip_totalDeductionsLabel;
    @FXML private Label payslip_netLabel;
    @FXML private Label payslip_errorLabel;
    @FXML private TableView<com.skilora.finance.entity.PayslipRow> payslipTable;
    @FXML private TableColumn<com.skilora.finance.entity.PayslipRow, String> payslip_userCol;
    @FXML private TableColumn<com.skilora.finance.entity.PayslipRow, String> payslip_periodCol;
    @FXML private TableColumn<com.skilora.finance.entity.PayslipRow, String> payslip_baseCol;
    @FXML private TableColumn<com.skilora.finance.entity.PayslipRow, String> payslip_overtimeCol;
    @FXML private TableColumn<com.skilora.finance.entity.PayslipRow, String> payslip_bonusCol;
    @FXML private TableColumn<com.skilora.finance.entity.PayslipRow, String> payslip_grossCol;
    @FXML private TableColumn<com.skilora.finance.entity.PayslipRow, String> payslip_deductCol;
    @FXML private TableColumn<com.skilora.finance.entity.PayslipRow, String> payslip_netCol;
    @FXML private TableColumn<com.skilora.finance.entity.PayslipRow, String> payslip_statusCol;
    @FXML private Label payslip_countLabel;
    @FXML private TLButton payslip_calculateBtn;
    @FXML private TLButton payslip_addBtn;
    @FXML private TLButton payslip_updateBtn;
    @FXML private TLButton payslip_pdfBtn;
    @FXML private TLButton payslip_clearBtn;
    @FXML private TLButton payslip_deleteBtn;
    @FXML private TableColumn<com.skilora.finance.entity.PayslipRow, ?> payslip_idCol;

    // Reports tab
    @FXML private TLComboBox<String> report_employeeCombo;
    @FXML private TLTextField tax_grossField;
    @FXML private TLComboBox<String> tax_currencyCombo;
    @FXML private javafx.scene.layout.VBox tax_resultsBox;
    @FXML private javafx.scene.layout.VBox tax_hintBox;
    @FXML private Label tax_grossResult;
    @FXML private Label tax_cnssResult;
    @FXML private Label tax_irppResult;
    @FXML private Label tax_totalDeductResult;
    @FXML private Label tax_netResult;
    @FXML private Label tax_rateResult;
    @FXML private Label tax_summaryLabel;
    @FXML private Label tax_employeeLabelResult;
    @FXML private TLButton report_generateBtn;
    @FXML private TLButton tax_calculateBtn;
    @FXML private TextArea tax_resultArea;

    private User currentUser;
    private EmploymentContract selectedContract;
    private final ObservableList<EmploymentContract> contractData = FXCollections.observableArrayList();

    private final ContractService contractService = ContractService.getInstance();
    private final FinanceDataService financeDataService = FinanceDataService.getInstance();
    private final UserService userService = UserService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (contractTable != null) {
            initializeContractTab();
            initializeBankTab();
            initializeBonusTab();
            initializePayslipTab();
            initializeReportsTab();
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user == null || (user.getRole() != com.skilora.user.enums.Role.ADMIN
                && user.getRole() != com.skilora.user.enums.Role.EMPLOYER)) {
            javafx.scene.Node node = mainSearchField != null ? mainSearchField : (contractTable != null ? contractTable : null);
            if (node != null && node.getScene() != null) {
                TLToast.error(node.getScene(), I18n.get("errorpage.access_denied"),
                        I18n.get("errorpage.access_denied.desc"));
            }
            return;
        }
    }

    private javafx.scene.Scene getScene() {
        if (mainSearchField != null && mainSearchField.getScene() != null) return mainSearchField.getScene();
        if (contractTable != null && contractTable.getScene() != null) return contractTable.getScene();
        return null;
    }

    // ==================== Contracts tab (FXML layout) ====================

    private void initializeContractTab() {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        contract_userCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getUserName() != null ? c.getValue().getUserName() : "#" + c.getValue().getUserId()));
        contract_companyCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getCompanyName() != null ? c.getValue().getCompanyName() : (c.getValue().getEmployerName() != null ? c.getValue().getEmployerName() : "")));
        contract_typeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getContractType() != null ? c.getValue().getContractType() : ""));
        contract_positionCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getPosition() != null ? c.getValue().getPosition() : (c.getValue().getJobTitle() != null ? c.getValue().getJobTitle() : "")));
        contract_salaryCol.setCellValueFactory(c -> {
            EmploymentContract x = c.getValue();
            if (x.getSalaryBase() == null) return new SimpleStringProperty("-");
            String curr = x.getCurrency() != null ? x.getCurrency() : "TND";
            return new SimpleStringProperty(String.format("%.2f", x.getSalaryBase().doubleValue()) + " " + curr);
        });
        contract_startCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getStartDate() != null ? c.getValue().getStartDate().format(df) : ""));
        contract_endCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getEndDate() != null ? c.getValue().getEndDate().format(df) : ""));
        contract_statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus() != null ? c.getValue().getStatus() : ""));

        contract_typeCombo.getItems().setAll("CDI", "CDD", "SIVP", "STAGE");
        contract_typeCombo.setValue("CDI");
        contract_statusCombo.getItems().setAll("DRAFT", "PENDING_SIGNATURE", "ACTIVE", "TERMINATED", "EXPIRED");
        contract_statusCombo.setValue("ACTIVE");

        if (contract_startDatePicker != null) contract_startDatePicker.valueProperty().addListener((o, a, b) -> updateContractSeniorityLabel());
        if (contract_endDatePicker != null) contract_endDatePicker.valueProperty().addListener((o, a, b) -> updateContractSeniorityLabel());

        contractTable.setItems(contractData);
        contractTable.getSelectionModel().selectedItemProperty().addListener((o, a, c) -> onContractSelected());

        loadContractEmployeeCombo();
        handleRefreshContracts();
    }

    private void loadContractEmployeeCombo() {
        Task<List<User>> task = new Task<>() {
            @Override
            protected List<User> call() {
                return userService.findAll();
            }
        };
        task.setOnSucceeded(e -> {
            List<User> users = task.getValue();
            if (users != null && contract_userIdCombo != null) {
                ObservableList<String> items = FXCollections.observableArrayList();
                for (User u : users) {
                    String name = u.getFullName() != null ? u.getFullName() : ("User #" + u.getId());
                    items.add(u.getId() + " - " + name);
                }
                contract_userIdCombo.getItems().setAll(items);
            }
        });
        AppThreadPool.execute(task);
    }

    private void updateContractSeniorityLabel() {
        if (contract_seniorityLabel == null) return;
        LocalDate start = contract_startDatePicker != null ? contract_startDatePicker.getValue() : null;
        LocalDate end = contract_endDatePicker != null ? contract_endDatePicker.getValue() : null;
        if (start == null) {
            contract_seniorityLabel.setText(I18n.get("finance.admin.contract.seniority") + " : —");
            return;
        }
        if (end == null) end = LocalDate.now();
        Period p = Period.between(start, end);
        String s = p.getYears() + " " + I18n.get("finance.admin.contract.years") + ", " +
            p.getMonths() + " " + I18n.get("finance.admin.contract.months");
        contract_seniorityLabel.setText(I18n.get("finance.admin.contract.seniority") + " : " + s);
    }

    private void showContractError(String message) {
        if (contract_errorLabel != null) {
            contract_errorLabel.setText(message);
            contract_errorLabel.setVisible(true);
            contract_errorLabel.setManaged(true);
        }
    }

    private void clearContractError() {
        if (contract_errorLabel != null) {
            contract_errorLabel.setText("");
            contract_errorLabel.setVisible(false);
            contract_errorLabel.setManaged(false);
        }
    }

    private int extractContractUserId(String comboValue) {
        if (comboValue == null || !comboValue.contains(" - ")) return -1;
        try {
            return Integer.parseInt(comboValue.split(" - ")[0].trim());
        } catch (NumberFormatException e) { return -1; }
    }

    @FXML
    private void handleAddContract() {
        clearContractError();
        if (contract_userIdCombo == null || contract_userIdCombo.getValue() == null) {
            showContractError(I18n.get("finance.admin.contract.select_employee"));
            return;
        }
        String salaryText = contract_salaryField != null ? contract_salaryField.getText() : "";
        if (salaryText == null || salaryText.trim().isEmpty() || !isValidContractSalary(salaryText.trim())) {
            showContractError(I18n.get("finance.admin.contract.invalid_salary"));
            return;
        }
        if (contract_startDatePicker == null || contract_startDatePicker.getValue() == null) {
            showContractError(I18n.get("finance.admin.contract.start_required"));
            return;
        }
        if (contract_typeCombo == null || contract_typeCombo.getValue() == null) {
            showContractError(I18n.get("finance.admin.contract.type_required"));
            return;
        }
        int userId = extractContractUserId(contract_userIdCombo.getValue());
        if (userId <= 0) {
            showContractError(I18n.get("finance.admin.contract.select_employee"));
            return;
        }
        EmploymentContract contract = new EmploymentContract(userId, new BigDecimal(salaryText.trim()), contract_startDatePicker.getValue());
        contract.setContractType(contract_typeCombo.getValue());
        contract.setCurrency("TND");
        contract.setStatus(contract_statusCombo != null && contract_statusCombo.getValue() != null ? contract_statusCombo.getValue() : "ACTIVE");
        contract.setCompanyName(contract_companyIdField != null && contract_companyIdField.getText() != null ? contract_companyIdField.getText().trim() : null);
        contract.setPosition(contract_positionField != null && contract_positionField.getText() != null ? contract_positionField.getText().trim() : null);
        contract.setEndDate(contract_endDatePicker != null ? contract_endDatePicker.getValue() : null);
        if (currentUser != null) contract.setEmployerId(currentUser.getId());

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                return contractService.create(contract);
            }
        };
        task.setOnSucceeded(e -> {
            handleClearContractForm();
            handleRefreshContracts();
            if (getScene() != null) TLToast.success(getScene(), I18n.get("finance.admin.contract.added"), "");
        });
        task.setOnFailed(e -> showContractError(I18n.get("finance.admin.contracts.error") + ": " + (task.getException() != null ? task.getException().getMessage() : "")));
        AppThreadPool.execute(task);
    }

    @FXML
    private void handleUpdateContract() {
        clearContractError();
        if (selectedContract == null) {
            showContractError(I18n.get("finance.admin.contract.select_to_update"));
            return;
        }
        if (contract_userIdCombo == null || contract_userIdCombo.getValue() == null || contract_startDatePicker == null || contract_startDatePicker.getValue() == null) {
            showContractError(I18n.get("finance.admin.contract.start_required"));
            return;
        }
        String salaryText = contract_salaryField != null ? contract_salaryField.getText() : "";
        if (salaryText == null || salaryText.trim().isEmpty() || !isValidContractSalary(salaryText.trim())) {
            showContractError(I18n.get("finance.admin.contract.invalid_salary"));
            return;
        }
        int userId = extractContractUserId(contract_userIdCombo.getValue());
        if (userId <= 0) return;
        selectedContract.setUserId(userId);
        selectedContract.setSalaryBase(new BigDecimal(salaryText.trim()));
        selectedContract.setStartDate(contract_startDatePicker.getValue());
        selectedContract.setEndDate(contract_endDatePicker != null ? contract_endDatePicker.getValue() : null);
        selectedContract.setContractType(contract_typeCombo != null && contract_typeCombo.getValue() != null ? contract_typeCombo.getValue() : selectedContract.getContractType());
        selectedContract.setStatus(contract_statusCombo != null && contract_statusCombo.getValue() != null ? contract_statusCombo.getValue() : selectedContract.getStatus());
        selectedContract.setCompanyName(contract_companyIdField != null ? contract_companyIdField.getText() : null);
        selectedContract.setPosition(contract_positionField != null ? contract_positionField.getText() : null);

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return contractService.update(selectedContract);
            }
        };
        task.setOnSucceeded(e -> {
            handleClearContractForm();
            handleRefreshContracts();
            if (getScene() != null) TLToast.success(getScene(), I18n.get("finance.admin.contract.updated"), "");
        });
        task.setOnFailed(e -> showContractError(I18n.get("finance.admin.contracts.error") + ": " + (task.getException() != null ? task.getException().getMessage() : "")));
        AppThreadPool.execute(task);
    }

    @FXML
    private void handleDeleteContract() {
        clearContractError();
        if (selectedContract == null) return;
        Optional<ButtonType> result = DialogUtils.showConfirmation(
            I18n.get("finance.admin.contract.delete_confirm_title"),
            I18n.get("finance.admin.contract.delete_confirm_msg", selectedContract.getUserName() != null ? selectedContract.getUserName() : "#" + selectedContract.getUserId()));
        if (result.isPresent() && result.get() == ButtonType.OK) {
            int id = selectedContract.getId();
            Task<Boolean> task = new Task<>() {
            @Override
                protected Boolean call() throws Exception {
                    return contractService.delete(id);
            }
        };
        task.setOnSucceeded(e -> {
                handleClearContractForm();
                handleRefreshContracts();
                if (getScene() != null) TLToast.success(getScene(), I18n.get("finance.admin.contract.deleted"), "");
            });
            task.setOnFailed(e -> showContractError(I18n.get("finance.admin.contracts.error") + ": " + (task.getException() != null ? task.getException().getMessage() : "")));
        AppThreadPool.execute(task);
        }
    }

    @FXML
    private void handleClearContractForm() {
        selectedContract = null;
        if (contract_userIdCombo != null) contract_userIdCombo.setValue(null);
        if (contract_companyIdField != null) contract_companyIdField.setText("");
        if (contract_positionField != null) contract_positionField.setText("");
        if (contract_salaryField != null) contract_salaryField.setText("");
        if (contract_startDatePicker != null) contract_startDatePicker.setValue(null);
        if (contract_endDatePicker != null) contract_endDatePicker.setValue(null);
        if (contract_typeCombo != null) contract_typeCombo.setValue("CDI");
        if (contract_statusCombo != null) contract_statusCombo.setValue("ACTIVE");
        clearContractError();
        updateContractSeniorityLabel();
        if (contractTable != null) contractTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleRefreshContracts() {
        Task<List<EmploymentContract>> task = new Task<>() {
            @Override
            protected List<EmploymentContract> call() throws Exception {
                return contractService.findAll();
            }
        };
        task.setOnSucceeded(e -> {
            List<EmploymentContract> list = task.getValue();
            contractData.setAll(list != null ? list : List.of());
            updateContractCountLabel();
        });
        task.setOnFailed(e -> {
            logger.error("Failed to load contracts", task.getException());
            showContractError(I18n.get("finance.admin.contracts.error"));
        });
        AppThreadPool.execute(task);
    }

    private void updateContractCountLabel() {
        if (contract_countLabel != null) {
            contract_countLabel.setText(I18n.get("finance.admin.contract.total") + ": " + contractData.size());
        }
    }

    private void onContractSelected() {
        selectedContract = contractTable != null ? contractTable.getSelectionModel().getSelectedItem() : null;
        if (selectedContract == null) return;
        contract_userIdCombo.setValue(selectedContract.getUserId() + " - " + (selectedContract.getUserName() != null ? selectedContract.getUserName() : "User #" + selectedContract.getUserId()));
        contract_companyIdField.setText(selectedContract.getCompanyName() != null ? selectedContract.getCompanyName() : "");
        contract_positionField.setText(selectedContract.getPosition() != null ? selectedContract.getPosition() : "");
        contract_salaryField.setText(selectedContract.getSalaryBase() != null ? selectedContract.getSalaryBase().toPlainString() : "");
        contract_startDatePicker.setValue(selectedContract.getStartDate());
        if (contract_endDatePicker != null) contract_endDatePicker.setValue(selectedContract.getEndDate());
        contract_typeCombo.setValue(selectedContract.getContractType() != null ? selectedContract.getContractType() : "CDI");
        contract_statusCombo.setValue(selectedContract.getStatus() != null ? selectedContract.getStatus() : "ACTIVE");
        updateContractSeniorityLabel();
    }

    private boolean isValidContractSalary(String s) {
        try {
            BigDecimal b = new BigDecimal(s);
            return b.compareTo(BigDecimal.ZERO) > 0;
        } catch (NumberFormatException e) { return false; }
    }


    // ==================== Bank, Bonus, Payslip, Reports tabs ====================

    private void initializeBankTab() {
        if (bankAccountTable == null) return;
        bank_userCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmployeeName()));
        bank_nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBankName()));
        bank_ibanCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getIban()));
        bank_swiftCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSwift()));
        bank_currencyCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCurrency()));
        bank_primaryCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getIsPrimary() ? "Yes" : "No"));
        bank_verifiedCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getIsVerified() ? "Yes" : "No"));
        ObservableList<BankAccountRow> bankData = FXCollections.observableArrayList();
        bankAccountTable.setItems(bankData);
        try {
            bankData.setAll(financeDataService.getAllBankAccounts());
            if (bank_countLabel != null) bank_countLabel.setText("Total: " + bankData.size());
        } catch (Exception e) { logger.error("Load bank accounts", e); }
        if (bank_currencyCombo != null) bank_currencyCombo.getItems().setAll("TND", "EUR", "USD");
        if (bank_primaryCombo != null) bank_primaryCombo.getItems().setAll("Yes", "No");
        if (bank_verifiedCombo != null) bank_verifiedCombo.getItems().setAll("Verified", "Unverified");
        loadEmployeeComboInto(bank_userIdCombo);

        // Selection listener to populate form
        bankAccountTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (bank_userIdCombo != null) bank_userIdCombo.setValue(newVal.getUserId() + " - " + newVal.getEmployeeName());
                if (bank_nameField != null) bank_nameField.setText(newVal.getBankName());
                if (bank_ibanField != null) bank_ibanField.setText(newVal.getIban());
                if (bank_swiftField != null) bank_swiftField.setText(newVal.getSwift());
                if (bank_currencyCombo != null) bank_currencyCombo.setValue(newVal.getCurrency());
                if (bank_primaryCombo != null) bank_primaryCombo.setValue(newVal.getIsPrimary() ? "Yes" : "No");
                if (bank_verifiedCombo != null) bank_verifiedCombo.setValue(newVal.getIsVerified() ? "Verified" : "Unverified");
            }
        });
    }

    private void initializeBonusTab() {
        if (bonusTable == null) return;
        bonus_userCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmployeeName()));
        bonus_amountCol.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f", c.getValue().getAmount())));
        bonus_reasonCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReason()));
        bonus_dateCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateAwarded()));
        ObservableList<BonusRow> bonusData = FXCollections.observableArrayList();
        bonusTable.setItems(bonusData);
        try {
            bonusData.setAll(financeDataService.getAllBonuses());
            if (bonus_countLabel != null) bonus_countLabel.setText("Total: " + bonusData.size());
        } catch (Exception e) { logger.error("Load bonuses", e); }
        loadEmployeeComboInto(bonus_userIdCombo);

        // Selection listener to populate form
        bonusTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (bonus_userIdCombo != null) bonus_userIdCombo.setValue(newVal.getUserId() + " - " + newVal.getEmployeeName());
                if (bonus_amountField != null) bonus_amountField.setText(String.format("%.2f", newVal.getAmount()));
                if (bonus_reasonField != null) bonus_reasonField.setText(newVal.getReason());
            }
        });
    }

    private void initializePayslipTab() {
        if (payslipTable == null) return;
        payslip_userCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmployeeName()));
        payslip_periodCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPeriod()));
        payslip_baseCol.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f", c.getValue().getBaseSalary())));
        payslip_overtimeCol.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f", c.getValue().getOvertimeTotal())));
        payslip_bonusCol.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f", c.getValue().getBonuses())));
        payslip_grossCol.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f", c.getValue().getGross())));
        payslip_deductCol.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f", c.getValue().getTotalDeductions())));
        payslip_netCol.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f", c.getValue().getNet())));
        payslip_statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        ObservableList<PayslipRow> payslipData = FXCollections.observableArrayList();
        payslipTable.setItems(payslipData);
        try {
            payslipData.setAll(financeDataService.getAllPayslips());
            if (payslip_countLabel != null) payslip_countLabel.setText("Total: " + payslipData.size());
        } catch (Exception e) { logger.error("Load payslips", e); }
        if (payslip_monthCombo != null) { payslip_monthCombo.getItems().clear(); for (int i = 1; i <= 12; i++) payslip_monthCombo.getItems().add(i); payslip_monthCombo.setValue(LocalDate.now().getMonthValue()); }
        if (payslip_yearCombo != null) { payslip_yearCombo.getItems().clear(); int y = LocalDate.now().getYear(); for (int i = y - 2; i <= y + 1; i++) payslip_yearCombo.getItems().add(i); payslip_yearCombo.setValue(y); }
        if (payslip_currencyCombo != null) payslip_currencyCombo.getItems().setAll("TND", "EUR", "USD");
        if (payslip_statusCombo != null) payslip_statusCombo.getItems().setAll("DRAFT", "PENDING", "PAID", "CANCELLED");
        loadEmployeeComboInto(payslip_userIdCombo);

        // Selection listener to populate form
        payslipTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (payslip_userIdCombo != null) payslip_userIdCombo.setValue(newVal.getUserId() + " - " + newVal.getEmployeeName());
                if (payslip_monthCombo != null) payslip_monthCombo.setValue(newVal.getMonth());
                if (payslip_yearCombo != null) payslip_yearCombo.setValue(newVal.getYear());
                if (payslip_currencyCombo != null) payslip_currencyCombo.setValue(newVal.getCurrency());
                if (payslip_statusCombo != null) payslip_statusCombo.setValue(newVal.getStatus());
                if (payslip_baseSalaryField != null) payslip_baseSalaryField.setText(String.format("%.2f", newVal.getBaseSalary()));
                if (payslip_overtimeField != null) payslip_overtimeField.setText(String.format("%.2f", newVal.getOvertime()));
                if (payslip_bonusesField != null) payslip_bonusesField.setText(String.format("%.2f", newVal.getBonuses()));
                if (payslip_otherDeductionsField != null) payslip_otherDeductionsField.setText(String.format("%.2f", newVal.getOtherDeductions()));
                handleCalculatePayslip();
            }
        });
    }

    private void initializeReportsTab() {
        loadEmployeeComboInto(report_employeeCombo);
        if (tax_currencyCombo != null) tax_currencyCombo.getItems().setAll("TND", "EUR", "USD");
    }

    private void loadEmployeeComboInto(TLComboBox<String> combo) {
        if (combo == null) return;
        Task<List<User>> task = new Task<>() {
            @Override
            protected List<User> call() { return userService.findAll(); }
        };
                task.setOnSucceeded(e -> {
            List<User> users = task.getValue();
            if (users != null) {
                ObservableList<String> items = FXCollections.observableArrayList();
                for (User u : users) {
                    String name = u.getFullName() != null ? u.getFullName() : ("User #" + u.getId());
                    items.add(u.getId() + " - " + name);
                }
                combo.getItems().setAll(items);
            }
        });
                AppThreadPool.execute(task);
    }

    // ==================== Bank Account CRUD ====================

    @FXML
    private void handleAddBankAccount() {
        if (bank_userIdCombo == null || bank_nameField == null || bank_ibanField == null) return;
        String selected = bank_userIdCombo.getValue();
        if (selected == null || !selected.contains(" - ")) {
            if (getScene() != null) TLToast.warning(getScene(), "Validation", "Select an employee");
            return;
        }
        int userId = Integer.parseInt(selected.substring(0, selected.indexOf(" - ")).trim());
        String employeeName = selected.substring(selected.indexOf(" - ") + 3).trim();
        String bankName = bank_nameField.getText() != null ? bank_nameField.getText().trim() : "";
        String iban = bank_ibanField.getText() != null ? bank_ibanField.getText().trim() : "";
        String swift = bank_swiftField != null && bank_swiftField.getText() != null ? bank_swiftField.getText().trim() : "";
        String currency = bank_currencyCombo != null ? bank_currencyCombo.getValue() : "TND";
        if (currency == null) currency = "TND";
        boolean isPrimary = bank_primaryCombo != null && "Yes".equals(bank_primaryCombo.getValue());
        boolean isVerified = bank_verifiedCombo != null && "Verified".equals(bank_verifiedCombo.getValue());

        if (bankName.isEmpty() || iban.isEmpty()) {
            if (getScene() != null) TLToast.warning(getScene(), "Validation", "Bank name and IBAN are required");
            return;
        }

        BankAccountRow account = new BankAccountRow(0, userId, employeeName, bankName, iban, swift, currency, isPrimary, isVerified);
        account.setAccountHolder(employeeName);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                financeDataService.addBankAccount(account);
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (getScene() != null) TLToast.success(getScene(), "Success", "Bank account added");
            handleClearBankForm();
            handleRefreshBankAccounts();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable ex = task.getException();
            logger.error("Failed to add bank account", ex);
            String msg = "Failed to add bank account";
            if (ex != null && ex.getMessage() != null) {
                if (ex.getMessage().contains("account_holder")) {
                    msg = "Account holder name is required";
                } else if (ex.getMessage().contains("Duplicate entry")) {
                    msg = "This bank account already exists";
                }
            }
            if (getScene() != null) TLToast.error(getScene(), "Error", msg);
        }));
        AppThreadPool.execute(task);
    }

    @FXML
    private void handleUpdateBankAccount() {
        BankAccountRow selected = bankAccountTable != null ? bankAccountTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            if (getScene() != null) TLToast.warning(getScene(), "Selection", "Select a bank account to update");
            return;
        }
        if (bank_nameField == null || bank_ibanField == null) return;

        String bankName = bank_nameField.getText() != null ? bank_nameField.getText().trim() : "";
        String iban = bank_ibanField.getText() != null ? bank_ibanField.getText().trim() : "";
        if (bankName.isEmpty() || iban.isEmpty()) {
            if (getScene() != null) TLToast.warning(getScene(), "Validation", "Bank name and IBAN are required");
            return;
        }

        selected.setBankName(bankName);
        selected.setIban(iban);
        selected.setSwift(bank_swiftField != null && bank_swiftField.getText() != null ? bank_swiftField.getText().trim() : "");
        selected.setCurrency(bank_currencyCombo != null ? bank_currencyCombo.getValue() : "TND");
        selected.setIsPrimary(bank_primaryCombo != null && "Yes".equals(bank_primaryCombo.getValue()));
        selected.setIsVerified(bank_verifiedCombo != null && "Verified".equals(bank_verifiedCombo.getValue()));

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                financeDataService.updateBankAccount(selected);
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (getScene() != null) TLToast.success(getScene(), "Success", "Bank account updated");
            handleRefreshBankAccounts();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            logger.error("Failed to update bank account", task.getException());
            if (getScene() != null) TLToast.error(getScene(), "Error", "Failed to update bank account");
        }));
                AppThreadPool.execute(task);
    }

    @FXML
    private void handleDeleteBankAccount() {
        BankAccountRow selected = bankAccountTable != null ? bankAccountTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            if (getScene() != null) TLToast.warning(getScene(), "Selection", "Select a bank account to delete");
            return;
        }
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                financeDataService.deleteBankAccount(selected.getId());
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (getScene() != null) TLToast.success(getScene(), "Success", "Bank account deleted");
            handleRefreshBankAccounts();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            logger.error("Failed to delete bank account", task.getException());
            if (getScene() != null) TLToast.error(getScene(), "Error", "Failed to delete bank account");
        }));
        AppThreadPool.execute(task);
    }

    @FXML
    private void handleClearBankForm() {
        if (bank_userIdCombo != null) bank_userIdCombo.setValue(null);
        if (bank_nameField != null) bank_nameField.setText("");
        if (bank_ibanField != null) bank_ibanField.setText("");
        if (bank_swiftField != null) bank_swiftField.setText("");
        if (bank_currencyCombo != null) bank_currencyCombo.setValue("TND");
        if (bank_primaryCombo != null) bank_primaryCombo.setValue("No");
        if (bank_verifiedCombo != null) bank_verifiedCombo.setValue("Unverified");
        if (bankAccountTable != null) bankAccountTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleRefreshBankAccounts() {
        if (bankAccountTable != null && bankAccountTable.getItems() != null) {
            try {
                bankAccountTable.getItems().setAll(financeDataService.getAllBankAccounts());
                if (bank_countLabel != null) bank_countLabel.setText("Total: " + bankAccountTable.getItems().size());
            } catch (Exception e) { logger.error("Refresh bank", e); }
        }
    }

    // ==================== Bonus CRUD ====================

    @FXML
    private void handleAddBonus() {
        if (bonus_userIdCombo == null || bonus_amountField == null) return;
        String selected = bonus_userIdCombo.getValue();
        if (selected == null || !selected.contains(" - ")) {
            if (getScene() != null) TLToast.warning(getScene(), "Validation", "Select an employee");
            return;
        }
        int userId = Integer.parseInt(selected.substring(0, selected.indexOf(" - ")).trim());
        String amountText = bonus_amountField.getText() != null ? bonus_amountField.getText().trim() : "";
        String reason = bonus_reasonField != null && bonus_reasonField.getText() != null ? bonus_reasonField.getText().trim() : "";

        if (amountText.isEmpty()) {
            if (getScene() != null) TLToast.warning(getScene(), "Validation", "Amount is required");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            if (getScene() != null) TLToast.warning(getScene(), "Validation", "Invalid amount");
            return;
        }

        String dateAwarded = LocalDate.now().toString();
        BonusRow bonus = new BonusRow(0, userId, "", amount, reason, dateAwarded);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                financeDataService.addBonus(bonus);
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (getScene() != null) TLToast.success(getScene(), "Success", "Bonus added");
            handleClearBonusForm();
            handleRefreshBonuses();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            logger.error("Failed to add bonus", task.getException());
            if (getScene() != null) TLToast.error(getScene(), "Error", "Failed to add bonus");
        }));
        AppThreadPool.execute(task);
    }

    @FXML
    private void handleUpdateBonus() {
        BonusRow selected = bonusTable != null ? bonusTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            if (getScene() != null) TLToast.warning(getScene(), "Selection", "Select a bonus to update");
            return;
        }
        if (bonus_amountField == null) return;

        String amountText = bonus_amountField.getText() != null ? bonus_amountField.getText().trim() : "";
        if (amountText.isEmpty()) {
            if (getScene() != null) TLToast.warning(getScene(), "Validation", "Amount is required");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            if (getScene() != null) TLToast.warning(getScene(), "Validation", "Invalid amount");
            return;
        }

        selected.setAmount(amount);
        selected.setReason(bonus_reasonField != null && bonus_reasonField.getText() != null ? bonus_reasonField.getText().trim() : "");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                financeDataService.updateBonus(selected);
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (getScene() != null) TLToast.success(getScene(), "Success", "Bonus updated");
            handleRefreshBonuses();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            logger.error("Failed to update bonus", task.getException());
            if (getScene() != null) TLToast.error(getScene(), "Error", "Failed to update bonus");
        }));
        AppThreadPool.execute(task);
    }

    @FXML
    private void handleDeleteBonus() {
        BonusRow selected = bonusTable != null ? bonusTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            if (getScene() != null) TLToast.warning(getScene(), "Selection", "Select a bonus to delete");
            return;
        }
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                financeDataService.deleteBonus(selected.getId());
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (getScene() != null) TLToast.success(getScene(), "Success", "Bonus deleted");
            handleRefreshBonuses();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            logger.error("Failed to delete bonus", task.getException());
            if (getScene() != null) TLToast.error(getScene(), "Error", "Failed to delete bonus");
        }));
        AppThreadPool.execute(task);
    }

    @FXML
    private void handleClearBonusForm() {
        if (bonus_userIdCombo != null) bonus_userIdCombo.setValue(null);
        if (bonus_amountField != null) bonus_amountField.setText("");
        if (bonus_reasonField != null) bonus_reasonField.setText("");
        if (bonusTable != null) bonusTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleRefreshBonuses() {
        if (bonusTable != null && bonusTable.getItems() != null) {
            try {
                bonusTable.getItems().setAll(financeDataService.getAllBonuses());
                if (bonus_countLabel != null) bonus_countLabel.setText("Total: " + bonusTable.getItems().size());
            } catch (Exception e) { logger.error("Refresh bonuses", e); }
        }
    }

    // ==================== Payslip CRUD ====================

    @FXML
    private void handleCalculatePayslip() {
        if (payslip_baseSalaryField == null) return;
        try {
            double baseSalary = parseDoubleOrZero(payslip_baseSalaryField.getText());
            double overtime = parseDoubleOrZero(payslip_overtimeField != null ? payslip_overtimeField.getText() : "0");
            double overtimeRate = parseDoubleOrZero(payslip_overtimeRateField != null ? payslip_overtimeRateField.getText() : "0");
            double bonuses = parseDoubleOrZero(payslip_bonusesField != null ? payslip_bonusesField.getText() : "0");
            double otherDeductions = parseDoubleOrZero(payslip_otherDeductionsField != null ? payslip_otherDeductionsField.getText() : "0");

            double overtimeTotal = overtime * overtimeRate;
            double gross = baseSalary + overtimeTotal + bonuses;
            double cnss = gross * 0.0918;
            double irpp = (gross - cnss) * 0.26;
            double totalDeductions = cnss + irpp + otherDeductions;
            double net = gross - totalDeductions;

            if (payslip_grossLabel != null) payslip_grossLabel.setText(String.format("%.2f TND", gross));
            if (payslip_cnssLabel != null) payslip_cnssLabel.setText(String.format("%.2f TND", cnss));
            if (payslip_irppLabel != null) payslip_irppLabel.setText(String.format("%.2f TND", irpp));
            if (payslip_totalDeductionsLabel != null) payslip_totalDeductionsLabel.setText(String.format("%.2f TND", totalDeductions));
            if (payslip_netLabel != null) payslip_netLabel.setText(String.format("%.2f TND", net));
        } catch (Exception e) {
            logger.warn("Failed to calculate payslip", e);
        }
    }

    private double parseDoubleOrZero(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        try { return Double.parseDouble(text.trim()); } catch (NumberFormatException e) { return 0; }
    }

    @FXML
    private void handleAddPayslip() {
        if (payslip_userIdCombo == null || payslip_monthCombo == null || payslip_yearCombo == null) return;
        String selected = payslip_userIdCombo.getValue();
        if (selected == null || !selected.contains(" - ")) {
            if (getScene() != null) TLToast.warning(getScene(), "Validation", "Select an employee");
            return;
        }
        int userId = Integer.parseInt(selected.substring(0, selected.indexOf(" - ")).trim());
        Integer month = payslip_monthCombo.getValue();
        Integer year = payslip_yearCombo.getValue();
        if (month == null || year == null) {
            if (getScene() != null) TLToast.warning(getScene(), "Validation", "Select month and year");
            return;
        }

        double baseSalary = parseDoubleOrZero(payslip_baseSalaryField != null ? payslip_baseSalaryField.getText() : "0");
        double overtime = parseDoubleOrZero(payslip_overtimeField != null ? payslip_overtimeField.getText() : "0");
        double overtimeRate = parseDoubleOrZero(payslip_overtimeRateField != null ? payslip_overtimeRateField.getText() : "0");
        double bonuses = parseDoubleOrZero(payslip_bonusesField != null ? payslip_bonusesField.getText() : "0");
        double otherDeductions = parseDoubleOrZero(payslip_otherDeductionsField != null ? payslip_otherDeductionsField.getText() : "0");
        String currency = payslip_currencyCombo != null ? payslip_currencyCombo.getValue() : "TND";
        String status = payslip_statusCombo != null ? payslip_statusCombo.getValue() : "DRAFT";

        double overtimeTotal = overtime * overtimeRate;
        PayslipRow payslip = new PayslipRow(0, userId, "", month, year, baseSalary, overtime, overtimeTotal, bonuses, currency, status);
        payslip.setOtherDeductions(otherDeductions);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                financeDataService.addPayslip(payslip);
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (getScene() != null) TLToast.success(getScene(), "Success", "Payslip added");
            handleClearPayslipForm();
            handleRefreshPayslips();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            Throwable ex = task.getException();
            logger.error("Failed to add payslip", ex);
            String msg = "Failed to add payslip";
            String exMsg = getExceptionMessage(ex);
            if (exMsg != null && (exMsg.contains("Duplicate entry") || exMsg.contains("uq_payslip_period"))) {
                msg = "A payslip already exists for this employee, month and year. Select it and use Update or choose another period.";
            }
            if (getScene() != null) TLToast.error(getScene(), "Error", msg);
        }));
        AppThreadPool.execute(task);
    }

    /** Unwrap exception message for user-facing errors. */
    private static String getExceptionMessage(Throwable t) {
        while (t != null) {
            String m = t.getMessage();
            if (m != null && !m.isBlank()) return m;
            t = t.getCause();
        }
        return null;
    }

    @FXML
    private void handleUpdatePayslip() {
        PayslipRow selected = payslipTable != null ? payslipTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            if (getScene() != null) TLToast.warning(getScene(), "Selection", "Select a payslip to update");
            return;
        }

        selected.setMonth(payslip_monthCombo != null ? payslip_monthCombo.getValue() : selected.getMonth());
        selected.setYear(payslip_yearCombo != null ? payslip_yearCombo.getValue() : selected.getYear());
        selected.setBaseSalary(parseDoubleOrZero(payslip_baseSalaryField != null ? payslip_baseSalaryField.getText() : "0"));
        selected.setOvertime(parseDoubleOrZero(payslip_overtimeField != null ? payslip_overtimeField.getText() : "0"));
        double overtimeRate = parseDoubleOrZero(payslip_overtimeRateField != null ? payslip_overtimeRateField.getText() : "0");
        selected.setOvertimeTotal(selected.getOvertime() * overtimeRate);
        selected.setBonuses(parseDoubleOrZero(payslip_bonusesField != null ? payslip_bonusesField.getText() : "0"));
        selected.setOtherDeductions(parseDoubleOrZero(payslip_otherDeductionsField != null ? payslip_otherDeductionsField.getText() : "0"));
        selected.setCurrency(payslip_currencyCombo != null ? payslip_currencyCombo.getValue() : selected.getCurrency());
        selected.setStatus(payslip_statusCombo != null ? payslip_statusCombo.getValue() : selected.getStatus());
        selected.calculateTotals();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                financeDataService.updatePayslip(selected);
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (getScene() != null) TLToast.success(getScene(), "Success", "Payslip updated");
            handleRefreshPayslips();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            logger.error("Failed to update payslip", task.getException());
            if (getScene() != null) TLToast.error(getScene(), "Error", "Failed to update payslip");
        }));
        AppThreadPool.execute(task);
    }

    @FXML
    private void handleDeletePayslip() {
        PayslipRow selected = payslipTable != null ? payslipTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            if (getScene() != null) TLToast.warning(getScene(), "Selection", "Select a payslip to delete");
            return;
        }
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                financeDataService.deletePayslip(selected.getId());
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (getScene() != null) TLToast.success(getScene(), "Success", "Payslip deleted");
            handleRefreshPayslips();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            logger.error("Failed to delete payslip", task.getException());
            if (getScene() != null) TLToast.error(getScene(), "Error", "Failed to delete payslip");
        }));
        AppThreadPool.execute(task);
    }

    @FXML
    private void handleClearPayslipForm() {
        if (payslip_userIdCombo != null) payslip_userIdCombo.setValue(null);
        if (payslip_monthCombo != null) payslip_monthCombo.setValue(LocalDate.now().getMonthValue());
        if (payslip_yearCombo != null) payslip_yearCombo.setValue(LocalDate.now().getYear());
        if (payslip_currencyCombo != null) payslip_currencyCombo.setValue("TND");
        if (payslip_statusCombo != null) payslip_statusCombo.setValue("DRAFT");
        if (payslip_baseSalaryField != null) payslip_baseSalaryField.setText("");
        if (payslip_overtimeField != null) payslip_overtimeField.setText("");
        if (payslip_overtimeRateField != null) payslip_overtimeRateField.setText("");
        if (payslip_bonusesField != null) payslip_bonusesField.setText("");
        if (payslip_otherDeductionsField != null) payslip_otherDeductionsField.setText("");
        if (payslip_grossLabel != null) payslip_grossLabel.setText("0.00 TND");
        if (payslip_cnssLabel != null) payslip_cnssLabel.setText("0.00 TND");
        if (payslip_irppLabel != null) payslip_irppLabel.setText("0.00 TND");
        if (payslip_totalDeductionsLabel != null) payslip_totalDeductionsLabel.setText("0.00 TND");
        if (payslip_netLabel != null) payslip_netLabel.setText("0.00 TND");
        if (payslipTable != null) payslipTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleRefreshPayslips() {
        if (payslipTable != null && payslipTable.getItems() != null) {
            try {
                payslipTable.getItems().setAll(financeDataService.getAllPayslips());
                if (payslip_countLabel != null) payslip_countLabel.setText("Total: " + payslipTable.getItems().size());
            } catch (Exception e) { logger.error("Refresh payslips", e); }
        }
    }

    @FXML
    private void handleExportPayslipPDF() {
        PayslipRow selected = payslipTable != null ? payslipTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            if (getScene() != null) TLToast.warning(getScene(), "Selection", "Select a payslip to export");
            return;
        }
        Stage stage = null;
        if (payslip_pdfBtn != null && payslip_pdfBtn.getScene() != null && payslip_pdfBtn.getScene().getWindow() instanceof Stage) {
            stage = (Stage) payslip_pdfBtn.getScene().getWindow();
        } else if (getScene() != null && getScene().getWindow() instanceof Stage) {
            stage = (Stage) getScene().getWindow();
        }
        if (stage == null) {
            if (getScene() != null) TLToast.error(getScene(), "Error", "Cannot show save dialog.");
            return;
        }
        String empName = selected.getEmployeeName() != null ? selected.getEmployeeName() : "Employé";
        File result = PDFGenerator.generatePayslipPDF(selected, empName, stage);
        if (result != null && getScene() != null) {
            TLToast.success(getScene(), "Export", "Bulletin enregistré : " + result.getName());
        }
    }

    @FXML
    private void handleGenerateEmployeeReport() {
        if (report_employeeCombo == null) return;
        String selected = report_employeeCombo.getValue();
        if (selected == null || selected.isBlank()) {
            if (getScene() != null) TLToast.warning(getScene(), I18n.get("common.select") != null ? I18n.get("common.select") : "Select", "Select an employee to generate the report.");
                return;
            }
        int employeeId;
        String employeeName;
        int dash = selected.indexOf(" - ");
        if (dash >= 0) {
            try {
                employeeId = Integer.parseInt(selected.substring(0, dash).trim());
                employeeName = selected.substring(dash + 3).trim();
            } catch (NumberFormatException e) {
                if (getScene() != null) TLToast.error(getScene(), "Error", "Invalid employee selection.");
                return;
            }
        } else {
            if (getScene() != null) TLToast.warning(getScene(), "Select", "Select an employee from the list.");
                return;
            }
        Stage stage = null;
        if (report_generateBtn != null && report_generateBtn.getScene() != null && report_generateBtn.getScene().getWindow() instanceof Stage) {
            stage = (Stage) report_generateBtn.getScene().getWindow();
        } else if (getScene() != null && getScene().getWindow() instanceof Stage) {
            stage = (Stage) getScene().getWindow();
        }
        if (stage == null) {
            if (getScene() != null) TLToast.error(getScene(), "Error", "Cannot show save dialog.");
            return;
        }
        String safeName = (employeeName != null ? employeeName : "Employe").replaceAll("[^a-zA-Z0-9_-]", "_");
        String defaultName = "Rapport_Financier_" + safeName + "_" + LocalDate.now() + ".pdf";
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport financier");
        fileChooser.setInitialFileName(defaultName);
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Rapports PDF (*.pdf)", "*.pdf"),
            new FileChooser.ExtensionFilter("Fichiers HTML (*.html)", "*.html")
        );
        File chosenFile = fileChooser.showSaveDialog(stage);
        if (chosenFile == null) return;
        FileChooser.ExtensionFilter selectedFilter = fileChooser.getSelectedExtensionFilter();
        final File targetFile = PDFGenerator.ensureExtension(chosenFile, selectedFilter);
        final int empId = employeeId;
        final String empName = employeeName != null ? employeeName : "Employé";
        Task<File> task = new Task<>() {
            @Override
            protected File call() throws Exception {
                String contractInfo = buildContractInfoHtml(empId);
                String bankInfo = buildBankInfoHtml(empId);
                String bonusInfo = buildBonusInfoHtml(empId);
                String payslipInfo = buildPayslipInfoHtml(empId);
                String summary = buildReportSummary(empId, empName);
                return PDFGenerator.generateToFile(targetFile, empId, empName,
                    contractInfo, bankInfo, bonusInfo, payslipInfo, summary);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            File pdf = task.getValue();
            if (pdf != null && getScene() != null) {
                TLToast.success(getScene(), "Rapport généré", "Enregistré : " + pdf.getAbsolutePath());
            }
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            logger.error("Failed to generate report", task.getException());
            if (getScene() != null) TLToast.error(getScene(), "Erreur", "Échec de la génération du rapport.");
        }));
        AppThreadPool.execute(task);
    }
    @FXML private void handleCalculateTax() {
        if (tax_grossField == null || tax_grossField.getText() == null || tax_grossField.getText().trim().isEmpty()) return;
        try {
            double gross = Double.parseDouble(tax_grossField.getText().trim());
            double cnss = gross * 0.0918;
            double irpp = (gross - cnss) * 0.26;
            double totalDeduct = cnss + irpp;
            double net = gross - totalDeduct;
            if (tax_grossResult != null) tax_grossResult.setText(String.format("%.2f TND", gross));
            if (tax_cnssResult != null) tax_cnssResult.setText(String.format("%.2f TND", cnss));
            if (tax_irppResult != null) tax_irppResult.setText(String.format("%.2f TND", irpp));
            if (tax_totalDeductResult != null) tax_totalDeductResult.setText(String.format("%.2f TND", totalDeduct));
            if (tax_netResult != null) tax_netResult.setText(String.format("%.2f TND", net));
            if (tax_rateResult != null) tax_rateResult.setText(String.format("%.1f%%", gross > 0 ? (totalDeduct / gross * 100) : 0));
            if (tax_resultsBox != null) { tax_resultsBox.setVisible(true); tax_resultsBox.setManaged(true); }
            if (tax_hintBox != null) { tax_hintBox.setVisible(false); tax_hintBox.setManaged(false); }
        } catch (NumberFormatException e) { logger.warn("Invalid tax input"); }
    }

    // ─── Report PDF: HTML builders (for generate employee report) ───────────

    private static final String[] MONTHS_FR = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
        "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};

    private String buildContractInfoHtml(int empId) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"salary-grid\">");
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.FRANCE);
        try {
            List<ContractRow> userContracts = financeDataService.getContractsByUserId(empId);
            if (userContracts.isEmpty()) {
                sb.append("<p class=\"report-field-value\" style=\"color:#6a7f96;\">Aucun contrat trouvé.</p>");
                sb.append("</div>");
                return sb.toString();
            }
            for (ContractRow c : userContracts) {
                String pos = escapeHtml(c.getPosition() != null ? c.getPosition() : "—");
                String posCap = capitalizeWords(pos);
                double sal = c.getSalary();
                String typeStr = escapeHtml(c.getType() != null ? c.getType().toUpperCase(Locale.FRENCH) : "—");
                String startStr = escapeHtml(c.getStartDate() != null ? c.getStartDate() : "—");
                String endStr = escapeHtml((c.getEndDate() != null && !c.getEndDate().isBlank()) ? c.getEndDate() : "—");
                sb.append("<div class=\"salary-card\">");
                sb.append("<div class=\"report-field\"><span class=\"report-field-label\">Position :</span> <span class=\"report-field-value\">").append(posCap).append("</span></div>");
                sb.append("<div class=\"report-field\"><span class=\"report-field-label\">Type :</span> <span class=\"report-field-value\">").append(typeStr).append("</span></div>");
                sb.append("<div class=\"report-field\"><span class=\"report-field-label\">Salaire mensuel :</span> <span class=\"report-field-value\">").append(nf.format((long) sal)).append(" <span class=\"currency\">TND</span></span></div>");
                sb.append("<div class=\"report-field\"><span class=\"report-field-label\">Début :</span> <span class=\"report-field-value\">").append(startStr).append("</span></div>");
                sb.append("<div class=\"report-field\"><span class=\"report-field-label\">Fin :</span> <span class=\"report-field-value\">").append(endStr).append("</span></div>");
                sb.append("</div>");
            }
        } catch (Exception e) {
            sb.append("<p class=\"report-field-value\" style=\"color:#6a7f96;\">Erreur chargement contrats.</p>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String buildBankInfoHtml(int empId) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"bank-grid\">");
        try {
            List<BankAccountRow> userBanks = financeDataService.getBankAccountsByUserId(empId);
            if (userBanks.isEmpty()) {
                sb.append("<p class=\"report-field-value\" style=\"color:#6a7f96;\">Aucun compte bancaire.</p>");
                sb.append("</div>");
                return sb.toString();
            }
            for (BankAccountRow b : userBanks) {
                String bankName = escapeHtml(b.getBankName() != null ? b.getBankName() : "—");
                String iban = escapeHtml(b.getIban() != null ? b.getIban() : "—");
                sb.append("<div class=\"bank-card\">");
                sb.append("<div class=\"report-field\"><span class=\"report-field-label\">Banque :</span> <span class=\"report-field-value\">").append(capitalizeWords(bankName)).append("</span></div>");
                sb.append("<div class=\"report-field\"><span class=\"report-field-label\">IBAN :</span> <span class=\"report-field-value\">").append(iban).append("</span></div>");
                sb.append("</div>");
            }
        } catch (Exception e) {
            sb.append("<p class=\"report-field-value\" style=\"color:#6a7f96;\">Erreur chargement comptes.</p>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String buildBonusInfoHtml(int empId) {
        try {
            List<BonusRow> list = financeDataService.getBonusesByUserId(empId);
            if (list.isEmpty()) return "<p class=\"report-field-value\" style=\"color:#6a7f96;\">Aucune prime.</p>";
            double total = list.stream().mapToDouble(BonusRow::getAmount).sum();
            String label = list.size() == 1 && list.get(0).getReason() != null && !list.get(0).getReason().isEmpty()
                ? capitalizeWords(escapeHtml(list.get(0).getReason()))
                : (list.size() > 1 ? "Total des primes" : "Prime mensuelle");
            StringBuilder sb = new StringBuilder();
            sb.append("<div class=\"bonus-badge\">");
            sb.append("<div class=\"report-field\"><span class=\"report-field-label\">Montant total :</span> <span class=\"b-amount\">")
                .append(String.format(Locale.FRANCE, "%.0f", total))
                .append(" <span class=\"b-currency\">TND</span></span></div>");
            sb.append("<div class=\"report-field\"><span class=\"report-field-label\">Désignation :</span> <span class=\"b-label\">")
                .append(label).append("</span></div>");
            sb.append("</div>");
            return sb.toString();
        } catch (Exception e) {
            return "<p class=\"report-field-value\" style=\"color:#6a7f96;\">Erreur chargement primes.</p>";
        }
    }

    private String buildPayslipInfoHtml(int empId) {
        StringBuilder sb = new StringBuilder();
        try {
            List<PayslipRow> userPayslips = financeDataService.getPayslipsByUserId(empId);
            if (userPayslips.isEmpty()) return "<p class=\"report-field-value\" style=\"color:#6a7f96;\">Aucun bulletin de paie récent.</p>";
            NumberFormat nfDecimal = NumberFormat.getNumberInstance(Locale.FRANCE);
            nfDecimal.setMinimumFractionDigits(2);
            nfDecimal.setMaximumFractionDigits(2);
            for (PayslipRow p : userPayslips) {
                int m = p.getMonth(), y = p.getYear();
                String periodLabel = (m >= 1 && m <= 12) ? (MONTHS_FR[m - 1] + " " + y) : (p.getPeriod() != null ? p.getPeriod() : "—");
                boolean paid = p.getStatus() != null && (p.getStatus().equalsIgnoreCase("PAID") || p.getStatus().equalsIgnoreCase("PAYÉ"));
                String tag = paid ? "Payé" : "En attente";
                sb.append("<div class=\"payslip-row\"><table><tr>");
                sb.append("<td style=\"width:38%;\"><div class=\"payslip-col-label\">Période :</div><div class=\"payslip-col-value\">").append(escapeHtml(periodLabel)).append("</div></td>");
                sb.append("<td style=\"width:42%;\"><div class=\"payslip-col-label\">Net à payer :</div><div class=\"payslip-net\">").append(nfDecimal.format(p.getNet())).append(" <span class=\"payslip-net-currency\">TND</span></div></td>");
                sb.append("<td style=\"text-align:right;\"><div class=\"payslip-col-label\">Statut :</div><span class=\"tag\">").append(escapeHtml(tag)).append("</span></td>");
                sb.append("</tr></table></div>");
            }
        } catch (Exception e) {
            sb.append("<p class=\"report-field-value\" style=\"color:#6a7f96;\">Erreur chargement bulletins.</p>");
        }
        return sb.toString();
    }

    private String buildReportSummary(int empId, String employeeName) {
        String name = (employeeName != null && !employeeName.isBlank()) ? employeeName : "L'employé";
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRANCE);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        try {
            List<ContractRow> contracts = financeDataService.getContractsByUserId(empId);
            List<PayslipRow> payslips = new ArrayList<>(financeDataService.getPayslipsByUserId(empId));
            payslips.sort(Comparator.comparingInt(PayslipRow::getYear).reversed().thenComparingInt(PayslipRow::getMonth).reversed());
            List<BonusRow> bonuses = financeDataService.getBonusesByUserId(empId);
            List<BankAccountRow> banks = financeDataService.getBankAccountsByUserId(empId);
            StringBuilder sb = new StringBuilder();
            ContractRow active = contracts.stream()
                .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()) || "ACTIF".equalsIgnoreCase(c.getStatus()))
                .findFirst().orElse(contracts.isEmpty() ? null : contracts.get(0));
            if (active != null) {
                sb.append(name).append(" occupe le poste de ").append(active.getPosition() != null ? active.getPosition() : "—")
                    .append(" en ").append(active.getType() != null ? active.getType().toUpperCase() : "—");
                sb.append(" (salaire de base : ").append(nf.format(active.getSalary())).append(" TND). ");
            } else {
                sb.append(name).append(" n'a pas de contrat actif enregistré. ");
            }
            if (!payslips.isEmpty()) {
                PayslipRow last = payslips.get(0);
                String period = (last.getMonth() >= 1 && last.getMonth() <= 12) ? (MONTHS_FR[last.getMonth() - 1] + " " + last.getYear()) : "";
                if (!period.isEmpty()) sb.append(" ; le dernier (").append(period).append(") s'élève à ").append(nf.format(last.getNet())).append(" TND net");
                sb.append(". ");
            }
            if (!bonuses.isEmpty()) {
                double totalBonus = bonuses.stream().mapToDouble(BonusRow::getAmount).sum();
                sb.append(bonuses.size()).append(" prime(s) versée(s) (total : ").append(nf.format(totalBonus)).append(" TND). ");
            }
            if (!banks.isEmpty()) sb.append(banks.size()).append(" compte(s) bancaire(s) enregistré(s) pour le virement du salaire.");
            return sb.toString().trim();
        } catch (Exception e) {
            return name + " — données financières indisponibles.";
        }
    }

    private static String capitalizeWords(String s) {
        if (s == null || s.isBlank()) return s == null ? "" : s.trim();
        String t = s.trim();
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (Character.isWhitespace(c)) { cap = true; sb.append(c); }
            else if (cap) { sb.append(Character.toUpperCase(c)); cap = false; }
            else sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
