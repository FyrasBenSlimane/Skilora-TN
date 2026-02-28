package com.skilora.finance.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import com.skilora.framework.components.*;
import com.skilora.finance.utils.CurrencyHelper;
import com.skilora.finance.utils.PDFGenerator;
import com.skilora.finance.utils.ValidationHelper;
import com.skilora.finance.model.ContractRow;
import com.skilora.finance.model.BankAccountRow;
import com.skilora.finance.model.BonusRow;
import com.skilora.finance.model.PayslipRow;
import com.skilora.finance.service.FinanceService;
// Using specific Finance model
import com.skilora.model.service.UserService;
import com.skilora.model.entity.User;
import javafx.collections.transformation.FilteredList;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import com.skilora.utils.DialogUtils;

public class FinanceController implements Initializable {

    @FXML
    private TLTextField mainSearchField;
    @FXML
    private TLButton themeToggleBtn;
    private boolean isDarkMode = true;

    // Employee Management
    // Contracts
    @FXML
    private TLComboBox<String> contract_userIdCombo; // Changed back to String
    @FXML
    private TLTextField contract_companyIdField; // Keeps FXML ID but stores Name now
    @FXML
    private TLComboBox<String> contract_typeCombo;
    @FXML
    private TLTextField contract_positionField;
    @FXML
    private TLTextField contract_salaryField;
    @FXML
    private TLDatePicker contract_startDatePicker;
    @FXML
    private TLDatePicker contract_endDatePicker;
    @FXML
    private TLComboBox<String> contract_statusCombo;
    @FXML
    private Label contract_errorLabel;
    @FXML
    private Label contract_seniorityLabel;
    @FXML
    private TableView<ContractRow> contractTable;
    @FXML
    private TableColumn<ContractRow, Integer> contract_idCol;
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
    private ObservableList<ContractRow> contractData = FXCollections.observableArrayList();
    private FilteredList<ContractRow> filteredContracts;
    private ContractRow selectedContract = null;

    // Bank Accounts
    @FXML
    private TLComboBox<String> bank_userIdCombo; // Changed back to String
    @FXML
    private TLTextField bank_nameField;
    @FXML
    private TLTextField bank_ibanField;
    @FXML
    private TLTextField bank_swiftField;
    @FXML
    private TLComboBox<String> bank_currencyCombo;
    @FXML
    private TLComboBox<String> bank_primaryCombo;
    @FXML
    private TLComboBox<String> bank_verifiedCombo;
    @FXML
    private Label bank_errorLabel;
    @FXML
    private TableView<BankAccountRow> bankAccountTable;
    @FXML
    private TableColumn<BankAccountRow, Integer> bank_idCol;
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
    private ObservableList<BankAccountRow> bankData = FXCollections.observableArrayList();
    private FilteredList<BankAccountRow> filteredBanks;
    private BankAccountRow selectedBank = null;

    // Bonuses
    @FXML
    private TLComboBox<String> bonus_userIdCombo; // Changed back to String
    @FXML
    private TLTextField bonus_amountField;
    @FXML
    private TLTextField bonus_reasonField;
    @FXML
    private Label bonus_errorLabel;
    @FXML
    private TableView<BonusRow> bonusTable;
    @FXML
    private TableColumn<BonusRow, Integer> bonus_idCol;
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
    private ObservableList<BonusRow> bonusData = FXCollections.observableArrayList();
    private FilteredList<BonusRow> filteredBonuses;
    private BonusRow selectedBonus = null;

    // Payslips (Creative!)
    @FXML
    private TLComboBox<String> payslip_userIdCombo; // Changed back to String
    @FXML
    private TLComboBox<Integer> payslip_monthCombo;
    @FXML
    private TLComboBox<Integer> payslip_yearCombo;
    @FXML
    private TLComboBox<String> payslip_currencyCombo;
    @FXML
    private TLTextField payslip_baseSalaryField;
    @FXML
    private TLTextField payslip_overtimeField;
    @FXML
    private TLTextField payslip_overtimeRateField;
    @FXML
    private TLTextField payslip_bonusesField;
    @FXML
    private TLTextField payslip_cnssField;
    @FXML
    private TLTextField payslip_irppField;
    @FXML
    private TLTextField payslip_otherDeductionsField;
    @FXML
    private TLComboBox<String> payslip_statusCombo;
    @FXML
    private Label payslip_grossLabel;
    @FXML
    private Label payslip_deductionsLabel;
    @FXML
    private Label payslip_netLabel;
    @FXML
    private Label payslip_errorLabel;
    @FXML
    private TableView<PayslipRow> payslipTable;
    @FXML
    private TableColumn<PayslipRow, Integer> payslip_idCol;
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
    private ObservableList<PayslipRow> payslipData = FXCollections.observableArrayList();
    private FilteredList<PayslipRow> filteredPayslips;
    private PayslipRow selectedPayslip = null;

    // Reports
    @FXML
    private TLComboBox<String> report_employeeCombo;
    @FXML
    private TLTextField tax_grossField;
    @FXML
    private TLComboBox<String> tax_currencyCombo;
    @FXML
    private TextArea tax_resultArea; // conservé caché pour compatibilité

    // Nouveaux labels résultats Tax Calculator (cartes KPI)
    @FXML
    private javafx.scene.layout.VBox tax_resultsBox;
    @FXML
    private javafx.scene.layout.VBox tax_hintBox;
    @FXML
    private Label tax_grossResult;
    @FXML
    private Label tax_cnssResult;
    @FXML
    private Label tax_irppResult;
    @FXML
    private Label tax_totalDeductResult;
    @FXML
    private Label tax_netResult;
    @FXML
    private Label tax_rateResult;
    @FXML
    private Label tax_summaryLabel;
    @FXML
    private Label tax_employeeLabelResult;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        try {
            if (contract_userIdCombo == null)
                throw new RuntimeException("contract_userIdCombo is null - FXML Injection failed");

            initializeContractTab();
            initializeBankTab();
            initializeBonusTab();
            initializePayslipTab();
            initializeReportsTab();
            setupSearchFilter();
            loadSampleData();
        } catch (Throwable e) {
            e.printStackTrace();
            System.err.println("Error initializing Finance Controller: " + e.getMessage());
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Finance Initialization Error");
                alert.setHeaderText("Critical Error initializing Finance Controller");
                String msg = e.getClass().getName() + ": " + e.getMessage();
                if (e.getCause() != null)
                    msg += "\nCaused by: " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage();
                if (msg.length() > 500)
                    msg = msg.substring(0, 500) + "...";
                alert.setContentText(msg);
                alert.getDialogPane().setMinWidth(600);
                alert.showAndWait();
            });
        }
    }

    // Continue dans le prochain fichier...

    @FXML
    private void setupContractsTable() {
        // Just for compatibility if called elsewhere, but let's use
        // initializeContractTab()
        initializeContractTab();
    }

    // CONTRACTS
    private void initializeContractTab() {
        contract_typeCombo
                .setItems(FXCollections.observableArrayList("PERMANENT", "TEMPORARY", "INTERNSHIP", "FREELANCE"));
        contract_statusCombo.setItems(FXCollections.observableArrayList("ACTIVE", "ENDED", "SUSPENDED", "TERMINATED"));

        contract_idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        contract_userCol.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        contract_companyCol.setCellValueFactory(new PropertyValueFactory<>("companyName"));
        contract_typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        contract_positionCol.setCellValueFactory(new PropertyValueFactory<>("position"));
        contract_salaryCol.setCellValueFactory(new PropertyValueFactory<>("salary"));
        contract_startCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        contract_endCol.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        contract_statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        filteredContracts = new FilteredList<>(contractData, p -> true);
        contractTable.setItems(filteredContracts);
        contractTable.setOnMouseClicked(e -> onContractSelected());

        // ── Ancienneté automatique : mise à jour dès qu'une date change ──
        if (contract_startDatePicker != null) {
            contract_startDatePicker.valueProperty().addListener((obs, oldV, newV) -> updateContractSeniority());
        }
        if (contract_endDatePicker != null) {
            contract_endDatePicker.valueProperty().addListener((obs, oldV, newV) -> updateContractSeniority());
        }
        updateContractSeniority();
    }

    @FXML
    private void handleAddContract() {
        clearFieldError(contract_errorLabel);
        if (contract_userIdCombo.getValue() == null) {
            showFieldError(contract_errorLabel, "Sélectionnez un employé!");
            return;
        }
        if (contract_salaryField.getText().isEmpty() || !isValidDouble(contract_salaryField.getText())) {
            showFieldError(contract_errorLabel, "Salaire invalide!");
            return;
        }
        if (contract_startDatePicker.getValue() == null) {
            showFieldError(contract_errorLabel, "Date de début requise!");
            return;
        }
        if (contract_typeCombo.getValue() == null || contract_typeCombo.getValue().isBlank()) {
            showFieldError(contract_errorLabel, "Type de contrat requis!");
            return;
        }

        String emp = contract_userIdCombo.getValue();
        String companyName = contract_companyIdField.getText().isBlank() ? "" : contract_companyIdField.getText();
        String statusVal = (contract_statusCombo.getValue() == null || contract_statusCombo.getValue().isBlank())
                ? "ACTIVE"
                : contract_statusCombo.getValue();

        ContractRow contract = new ContractRow(
                0,
                extractUserId(emp),
                extractUserName(emp),
                companyName,
                contract_typeCombo.getValue(),
                contract_positionField.getText(),
                Double.parseDouble(contract_salaryField.getText()),
                contract_startDatePicker.getValue().toString(),
                contract_endDatePicker.getValue() != null ? contract_endDatePicker.getValue().toString() : "",
                statusVal);
        try {
            FinanceService.getInstance().addContract(contract);
            loadSampleData();
            handleClearContractForm();
            updateContractCount();
            showSuccess("Contrat ajouté!");
        } catch (Exception e) {
            showFieldError(contract_errorLabel, "Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUpdateContract() {
        if (selectedContract == null) {
            showFieldError(contract_errorLabel, "Sélectionnez un contrat dans la liste d'abord!");
            return;
        }
        try {
            String emp = contract_userIdCombo.getValue();
            if (emp == null)
                throw new Exception("Sélectionnez un employé!");
            if (contract_startDatePicker.getValue() == null)
                throw new Exception("Date de début requise!");
            if (contract_salaryField.getText().isEmpty() || !isValidDouble(contract_salaryField.getText()))
                throw new Exception("Salaire invalide!");

            selectedContract.setUserId(extractUserId(emp));
            selectedContract.setEmployeeName(extractUserName(emp));
            selectedContract.setCompanyName(contract_companyIdField.getText());
            selectedContract.setType(
                    contract_typeCombo.getValue() != null ? contract_typeCombo.getValue() : selectedContract.getType());
            selectedContract.setPosition(contract_positionField.getText());
            selectedContract.setSalary(Double.parseDouble(contract_salaryField.getText()));
            selectedContract.setStartDate(contract_startDatePicker.getValue().toString());
            selectedContract.setEndDate(
                    contract_endDatePicker.getValue() != null ? contract_endDatePicker.getValue().toString() : "");
            selectedContract.setStatus(contract_statusCombo.getValue() != null ? contract_statusCombo.getValue()
                    : selectedContract.getStatus());

            FinanceService.getInstance().updateContract(selectedContract);
            loadSampleData();
            handleClearContractForm();
            showSuccess("Contrat mis à jour!");
        } catch (Exception e) {
            showFieldError(contract_errorLabel, "Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteContract() {
        if (selectedContract != null) {
            Optional<ButtonType> result = DialogUtils.showConfirmation("Delete Contract",
                    "Are you sure you want to delete the contract for " + selectedContract.getEmployeeName() + "?");
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    FinanceService.getInstance().deleteContract(selectedContract.getId());
                    loadSampleData();
                    updateContractCount();
                    handleClearContractForm();
                    showSuccess("Contract deleted!");
                } catch (Exception e) {
                    showFieldError(contract_errorLabel, e.getMessage());
                }
            }
        }
    }

    @FXML
    private void handleClearContractForm() {
        contract_userIdCombo.setValue(null);
        contract_companyIdField.setText("");
        contract_positionField.setText("");
        contract_salaryField.setText("");
        contract_startDatePicker.setValue(null);
        contract_endDatePicker.setValue(null);
        selectedContract = null;
        updateContractSeniority();
    }

    @FXML
    private void handleRefreshContracts() {
        // Rechargement réel depuis la base de données
        try {
            contractData.setAll(FinanceService.getInstance().getAllContracts());
            updateContractCount();
        } catch (Exception e) {
            System.err.println("[Finance] Refresh contrats: " + e.getMessage());
            e.printStackTrace();
            showFieldError(contract_errorLabel, "Erreur refresh: " + e.getMessage());
        }
    }

    private void onContractSelected() {
        selectedContract = contractTable.getSelectionModel().getSelectedItem();
        if (selectedContract != null) {
            String emp = getEmployeeStringById(selectedContract.getUserId());
            contract_userIdCombo.setValue(emp);
            contract_companyIdField
                    .setText(selectedContract.getCompanyName() != null ? selectedContract.getCompanyName() : "");
            contract_positionField
                    .setText(selectedContract.getPosition() != null ? selectedContract.getPosition() : "");
            contract_salaryField.setText(String.valueOf(selectedContract.getSalary()));
            // Remplir Type et Status (corrige bug Update)
            if (selectedContract.getType() != null)
                contract_typeCombo.setValue(selectedContract.getType());
            if (selectedContract.getStatus() != null)
                contract_statusCombo.setValue(selectedContract.getStatus());
            // Dates
            if (selectedContract.getStartDate() != null && !selectedContract.getStartDate().isEmpty()) {
                try {
                    contract_startDatePicker.setValue(LocalDate.parse(selectedContract.getStartDate()));
                } catch (Exception ignored) {
                }
            } else {
                contract_startDatePicker.setValue(null);
            }
            if (selectedContract.getEndDate() != null && !selectedContract.getEndDate().isEmpty()) {
                try {
                    contract_endDatePicker.setValue(LocalDate.parse(selectedContract.getEndDate()));
                } catch (Exception ignored) {
                }
            } else {
                contract_endDatePicker.setValue(null);
            }
        }
        updateContractSeniority();
    }

    /**
     * Met à jour le label d'ancienneté (format: "X ans Y mois") selon Date Début et
     * Date Fin.
     * Mise à jour automatique via listeners sur les DatePickers.
     */
    private void updateContractSeniority() {
        if (contract_seniorityLabel == null)
            return;
        LocalDate start = contract_startDatePicker != null ? contract_startDatePicker.getValue() : null;
        LocalDate end = contract_endDatePicker != null ? contract_endDatePicker.getValue() : null;

        if (start == null || end == null) {
            contract_seniorityLabel.setText("Ancienneté : —");
            contract_seniorityLabel
                    .setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 12px; -fx-font-weight: 600;");
            return;
        }
        if (end.isBefore(start)) {
            contract_seniorityLabel.setText("Ancienneté : dates invalides (Fin < Début)");
            contract_seniorityLabel
                    .setStyle("-fx-text-fill: -fx-destructive; -fx-font-size: 12px; -fx-font-weight: 700;");
            return;
        }

        String anciennete = formatAnciennete(start, end);
        contract_seniorityLabel.setText("Ancienneté : " + anciennete);
        contract_seniorityLabel.setStyle("-fx-text-fill: -fx-foreground; -fx-font-size: 12px; -fx-font-weight: 700;");
    }

    /**
     * Calcule et formate l'ancienneté en années/mois (ex: "3 ans et 4 mois").
     * Les jours sont ignorés (on garde années + mois).
     */
    private static String formatAnciennete(LocalDate start, LocalDate end) {
        Period p = Period.between(start, end);
        int years = Math.max(0, p.getYears());
        int months = Math.max(0, p.getMonths());

        if (years == 0 && months == 0)
            return "0 mois";

        StringBuilder sb = new StringBuilder();
        if (years > 0) {
            sb.append(years).append(years == 1 ? " an" : " ans");
        }
        if (months > 0) {
            if (sb.length() > 0)
                sb.append(" et ");
            sb.append(months).append(" mois");
        }
        return sb.toString();
    }

    private void updateContractCount() {
        if (filteredContracts != null)
            contract_countLabel.setText("Total: " + filteredContracts.size());
        else
            contract_countLabel.setText("Total: " + contractData.size());
    }

    // BANK ACCOUNTS - Methods to be added at line 471
    private void initializeBankTab() {
        bank_currencyCombo.setItems(CurrencyHelper.getWorldCurrencies());
        bank_primaryCombo.setItems(FXCollections.observableArrayList("Yes", "No"));
        bank_verifiedCombo.setItems(FXCollections.observableArrayList("Yes", "No"));

        bank_idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        bank_userCol.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        bank_nameCol.setCellValueFactory(new PropertyValueFactory<>("bankName"));
        bank_ibanCol.setCellValueFactory(new PropertyValueFactory<>("iban"));
        bank_swiftCol.setCellValueFactory(new PropertyValueFactory<>("swift"));
        bank_currencyCol.setCellValueFactory(new PropertyValueFactory<>("currency"));
        bank_primaryCol.setCellValueFactory(new PropertyValueFactory<>("isPrimary"));
        bank_verifiedCol.setCellValueFactory(new PropertyValueFactory<>("isVerified"));

        filteredBanks = new FilteredList<>(bankData, p -> true);
        bankAccountTable.setItems(filteredBanks);
        bankAccountTable.setOnMouseClicked(e -> onBankSelected());
    }

    @FXML
    private void handleAddBankAccount() {
        clearFieldError(bank_errorLabel);

        // STRICT VALIDATION
        if (bank_userIdCombo.getValue() == null) {
            showFieldError(bank_errorLabel, "Select employee first!");
            return;
        }

        String error;
        if ((error = ValidationHelper.validateRequired(bank_nameField.getText(), "Bank name")) != null) {
            showFieldError(bank_errorLabel, error);
            bank_nameField.requestFocus();
            return;
        }
        if ((error = ValidationHelper.validateIBAN(bank_ibanField.getText())) != null) {
            showFieldError(bank_errorLabel, error);
            bank_ibanField.requestFocus();
            return;
        }
        if ((error = ValidationHelper.validateSWIFT(bank_swiftField.getText())) != null) {
            showFieldError(bank_errorLabel, error);
            bank_swiftField.requestFocus();
            return;
        }
        if (bank_currencyCombo.getValue() == null) {
            showFieldError(bank_errorLabel, "Select currency!");
            return;
        }

        // All valid, create bank account
        String emp = bank_userIdCombo.getValue();
        BankAccountRow bank = new BankAccountRow(
                bankData.size() + 1,
                extractUserId(emp),
                extractUserName(emp),
                bank_nameField.getText().trim(),
                ValidationHelper.formatIBAN(bank_ibanField.getText()),
                bank_swiftField.getText().trim().toUpperCase(),
                CurrencyHelper.getCurrencyCode(bank_currencyCombo.getValue()),
                "Yes".equals(bank_primaryCombo.getValue()),
                "Yes".equals(bank_verifiedCombo.getValue()));

        try {
            FinanceService.getInstance().addBankAccount(bank);
            loadSampleData();
        } catch (Exception e) {
            showFieldError(bank_errorLabel, e.getMessage());
        }
        bankAccountTable.refresh(); // Force refresh
        updateBankCount();
        handleClearBankForm();
        showSuccess("✅ Bank account added successfully!");
    }

    @FXML
    private void handleUpdateBankAccount() {
        if (selectedBank != null) {
            try {
                String emp = bank_userIdCombo.getValue();
                if (emp == null)
                    throw new Exception("Please select an employee!");

                selectedBank.setUserId(extractUserId(emp));
                selectedBank.setBankName(bank_nameField.getText());
                selectedBank.setIban(bank_ibanField.getText());
                selectedBank.setSwift(bank_swiftField.getText());
                selectedBank.setCurrency(CurrencyHelper.getCurrencyCode(bank_currencyCombo.getValue()));
                selectedBank.setIsPrimary("Yes".equals(bank_primaryCombo.getValue()));
                selectedBank.setIsVerified("Yes".equals(bank_verifiedCombo.getValue()));

                FinanceService.getInstance().updateBankAccount(selectedBank);
                loadSampleData();
                showSuccess("Bank account updated!");
            } catch (Exception e) {
                showFieldError(bank_errorLabel, e.getMessage());
            }
        }
    }

    @FXML
    private void handleDeleteBankAccount() {
        if (selectedBank != null) {
            Optional<ButtonType> result = DialogUtils.showConfirmation("Delete Bank Account",
                    "Are you sure you want to delete the bank account " + selectedBank.getIban() + "?");
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    FinanceService.getInstance().deleteBankAccount(selectedBank.getId());
                    loadSampleData();
                    updateBankCount();
                    handleClearBankForm();
                    showSuccess("Bank account deleted!");
                } catch (Exception e) {
                    showFieldError(bank_errorLabel, e.getMessage());
                }
            }
        }
    }

    @FXML
    private void handleClearBankForm() {
        bank_userIdCombo.setValue(null);
        bank_nameField.setText("");
        bank_ibanField.setText("");
        bank_swiftField.setText("");
        bank_currencyCombo.setValue(null);
        bank_primaryCombo.setValue("No");
        bank_verifiedCombo.setValue("No");
        selectedBank = null;
    }

    @FXML
    private void handleRefreshBankAccounts() {
        bankAccountTable.refresh();
    }

    private void onBankSelected() {
        selectedBank = bankAccountTable.getSelectionModel().getSelectedItem();
        if (selectedBank != null) {
            bank_userIdCombo.setValue(getEmployeeStringById(selectedBank.getUserId()));
            bank_nameField.setText(selectedBank.getBankName());
            bank_ibanField.setText(selectedBank.getIban());
            bank_swiftField.setText(selectedBank.getSwift());
            bank_currencyCombo.setValue(CurrencyHelper.getFullCurrencyName(selectedBank.getCurrency()));
            bank_primaryCombo.setValue(selectedBank.getIsPrimary() ? "Yes" : "No");
            bank_verifiedCombo.setValue(selectedBank.getIsVerified() ? "Yes" : "No");
        }
    }

    private void updateBankCount() {
        if (filteredBanks != null)
            bank_countLabel.setText("Total: " + filteredBanks.size());
        else
            bank_countLabel.setText("Total: " + bankData.size());
    }

    // BONUSES
    private void initializeBonusTab() {
        bonus_idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        bonus_userCol.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        bonus_amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        bonus_reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));
        bonus_dateCol.setCellValueFactory(new PropertyValueFactory<>("dateAwarded"));

        filteredBonuses = new FilteredList<>(bonusData, p -> true);
        bonusTable.setItems(filteredBonuses);
        bonusTable.setOnMouseClicked(e -> onBonusSelected());
    }

    @FXML
    private void handleAddBonus() {
        clearFieldError(bonus_errorLabel);

        // STRICT VALIDATION
        if (bonus_userIdCombo.getValue() == null) {
            showFieldError(bonus_errorLabel, "Select employee first!");
            return;
        }

        String error;
        if ((error = ValidationHelper.validatePositiveNumber(bonus_amountField.getText(), "Bonus amount")) != null) {
            showFieldError(bonus_errorLabel, error);
            bonus_amountField.requestFocus();
            return;
        }
        if ((error = ValidationHelper.validateRequired(bonus_reasonField.getText(), "Reason")) != null) {
            showFieldError(bonus_errorLabel, error);
            bonus_reasonField.requestFocus();
            return;
        }

        // All valid, create bonus
        String emp = bonus_userIdCombo.getValue();
        BonusRow bonus = new BonusRow(
                bonusData.size() + 1,
                extractUserId(emp),
                extractUserName(emp),
                Double.parseDouble(bonus_amountField.getText().trim()),
                bonus_reasonField.getText().trim(),
                LocalDate.now().toString());

        try {
            FinanceService.getInstance().addBonus(bonus);
            loadSampleData();
        } catch (Exception e) {
            showFieldError(bonus_errorLabel, e.getMessage());
        }
        bonusTable.refresh(); // Force refresh
        updateBonusCount();
        handleClearBonusForm();
        showSuccess("✅ Bonus added successfully!");
    }

    @FXML
    private void handleUpdateBonus() {
        if (selectedBonus != null) {
            try {
                String emp = bonus_userIdCombo.getValue();
                if (emp == null)
                    throw new Exception("Please select an employee!");

                selectedBonus.setUserId(extractUserId(emp));
                selectedBonus.setEmployeeName(extractUserName(emp));
                selectedBonus.setAmount(Double.parseDouble(bonus_amountField.getText()));
                selectedBonus.setReason(bonus_reasonField.getText());

                FinanceService.getInstance().updateBonus(selectedBonus);
                loadSampleData();
                showSuccess("Bonus updated!");
            } catch (Exception e) {
                showFieldError(bonus_errorLabel, e.getMessage());
            }
        }
    }

    @FXML
    private void handleDeleteBonus() {
        if (selectedBonus != null) {
            Optional<ButtonType> result = DialogUtils.showConfirmation("Delete Bonus",
                    "Are you sure you want to delete this bonus (" + selectedBonus.getAmount() + ")?");
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    FinanceService.getInstance().deleteBonus(selectedBonus.getId());
                    loadSampleData();
                    updateBonusCount();
                    handleClearBonusForm();
                    showSuccess("Bonus deleted!");
                } catch (Exception e) {
                    showFieldError(bonus_errorLabel, e.getMessage());
                }
            }
        }
    }

    @FXML
    private void handleClearBonusForm() {
        bonus_userIdCombo.setValue(null);
        bonus_amountField.setText("");
        bonus_reasonField.setText("");
        selectedBonus = null;
    }

    @FXML
    private void handleRefreshBonuses() {
        bonusTable.refresh();
    }

    private void onBonusSelected() {
        selectedBonus = bonusTable.getSelectionModel().getSelectedItem();
        if (selectedBonus != null) {
            bonus_userIdCombo.setValue(getEmployeeStringById(selectedBonus.getUserId()));
            bonus_amountField.setText(String.valueOf(selectedBonus.getAmount()));
            bonus_reasonField.setText(selectedBonus.getReason());
        }
    }

    private void updateBonusCount() {
        if (filteredBonuses != null)
            bonus_countLabel.setText("Total: " + filteredBonuses.size());
        else
            bonus_countLabel.setText("Total: " + bonusData.size());
    }

    // PAYSLIPS (CREATIVE!)
    private void initializePayslipTab() {
        payslip_monthCombo.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        payslip_yearCombo.setItems(FXCollections.observableArrayList(2023, 2024, 2025, 2026));
        payslip_currencyCombo.setItems(CurrencyHelper.getWorldCurrencies());
        payslip_statusCombo.setItems(FXCollections.observableArrayList("DRAFT", "PENDING", "APPROVED", "PAID"));

        payslip_idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        payslip_userCol.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        payslip_periodCol.setCellValueFactory(new PropertyValueFactory<>("period"));
        payslip_baseCol.setCellValueFactory(new PropertyValueFactory<>("baseSalary"));
        payslip_overtimeCol.setCellValueFactory(new PropertyValueFactory<>("overtimeTotal"));
        payslip_bonusCol.setCellValueFactory(new PropertyValueFactory<>("bonuses"));
        payslip_grossCol.setCellValueFactory(new PropertyValueFactory<>("gross"));
        payslip_deductCol.setCellValueFactory(new PropertyValueFactory<>("totalDeductions"));
        payslip_netCol.setCellValueFactory(new PropertyValueFactory<>("net"));
        payslip_statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        filteredPayslips = new FilteredList<>(payslipData, p -> true);
        payslipTable.setItems(filteredPayslips);
        payslipTable.setOnMouseClicked(e -> onPayslipSelected());
    }

    @FXML
    private void handleCalculatePayslip() {
        try {
            double base = parseDouble(payslip_baseSalaryField.getText(), 0);
            double overtimeHours = parseDouble(payslip_overtimeField.getText(), 0);
            double overtimeRate = parseDouble(payslip_overtimeRateField.getText(), 0);
            double bonuses = parseDouble(payslip_bonusesField.getText(), 0);
            double otherDeduct = parseDouble(payslip_otherDeductionsField.getText(), 0);

            double overtimeTotal = overtimeHours * overtimeRate;
            double gross = base + overtimeTotal + bonuses;
            double cnss = gross * 0.0918;
            double irpp = (gross - cnss) * 0.26;
            double totalDeduct = cnss + irpp + otherDeduct;
            double net = gross - totalDeduct;

            payslip_cnssField.setText(String.format("%.2f", cnss));
            payslip_irppField.setText(String.format("%.2f", irpp));
            payslip_grossLabel.setText(String.format("%.2f TND", gross));
            payslip_deductionsLabel.setText(String.format("%.2f TND", totalDeduct));
            payslip_netLabel.setText(String.format("%.2f TND", net));
        } catch (Exception e) {
            showFieldError(payslip_errorLabel, "Invalid numbers!");
        }
    }

    @FXML
    private void handleAddPayslip() {
        clearFieldError(payslip_errorLabel);

        // STRICT VALIDATION
        if (payslip_userIdCombo.getValue() == null) {
            showFieldError(payslip_errorLabel, "Select employee first!");
            return;
        }
        if (payslip_monthCombo.getValue() == null || payslip_yearCombo.getValue() == null) {
            showFieldError(payslip_errorLabel, "Select period (month and year)!");
            return;
        }
        if (payslip_currencyCombo.getValue() == null) {
            showFieldError(payslip_errorLabel, "Select currency!");
            return;
        }
        if (payslip_statusCombo.getValue() == null) {
            showFieldError(payslip_errorLabel, "Select status!");
            return;
        }

        String error;
        if ((error = ValidationHelper.validatePositiveNumber(payslip_baseSalaryField.getText(),
                "Base salary")) != null) {
            showFieldError(payslip_errorLabel, error);
            payslip_baseSalaryField.requestFocus();
            return;
        }

        // Calculate automatically
        handleCalculatePayslip();

        // All valid, create payslip
        String emp = payslip_userIdCombo.getValue();
        double base = parseDouble(payslip_baseSalaryField.getText(), 0);
        double overtimeHours = parseDouble(payslip_overtimeField.getText(), 0);
        double overtimeRate = parseDouble(payslip_overtimeRateField.getText(), 0);
        double bonuses = parseDouble(payslip_bonusesField.getText(), 0);

        // Parse month and year safely (handles Integer or String from ComboBox)
        int month = Integer.parseInt(String.valueOf(payslip_monthCombo.getValue()));
        int year = Integer.parseInt(String.valueOf(payslip_yearCombo.getValue()));

        PayslipRow payslip = new PayslipRow(
                payslipData.size() + 1,
                extractUserId(emp),
                extractUserName(emp),
                month,
                year,
                base,
                overtimeHours,
                overtimeHours * overtimeRate,
                bonuses,
                CurrencyHelper.getCurrencyCode(payslip_currencyCombo.getValue()),
                payslip_statusCombo.getValue());

        payslip.setOtherDeductions(parseDouble(payslip_otherDeductionsField.getText(), 0));
        try {
            FinanceService.getInstance().addPayslip(payslip);
            loadSampleData();
            payslipTable.refresh();
            updatePayslipCount();
            handleClearPayslipForm();
            showSuccess("✅ Payslip saved successfully!");
        } catch (Exception e) {
            showFieldError(payslip_errorLabel, "Error saving payslip: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUpdatePayslip() {
        if (selectedPayslip != null) {
            try {
                String emp = payslip_userIdCombo.getValue();
                if (emp == null)
                    throw new Exception("Please select an employee!");

                // Recalculate based on current fields
                handleCalculatePayslip();

                selectedPayslip.setUserId(extractUserId(emp));
                selectedPayslip.setEmployeeName(extractUserName(emp));
                selectedPayslip.setMonth(Integer.parseInt(String.valueOf(payslip_monthCombo.getValue())));
                selectedPayslip.setYear(Integer.parseInt(String.valueOf(payslip_yearCombo.getValue())));
                selectedPayslip.setBaseSalary(parseDouble(payslip_baseSalaryField.getText(), 0));
                selectedPayslip.setOvertime(parseDouble(payslip_overtimeField.getText(), 0));
                selectedPayslip.setBonuses(parseDouble(payslip_bonusesField.getText(), 0));
                selectedPayslip.setOtherDeductions(parseDouble(payslip_otherDeductionsField.getText(), 0));
                selectedPayslip.setCurrency(CurrencyHelper.getCurrencyCode(payslip_currencyCombo.getValue()));
                selectedPayslip.setStatus(payslip_statusCombo.getValue());

                FinanceService.getInstance().updatePayslip(selectedPayslip);
                loadSampleData();
                showSuccess("Payslip updated!");
            } catch (Exception e) {
                showFieldError(payslip_errorLabel, e.getMessage());
            }
        }
    }

    @FXML
    private void handleDeletePayslip() {
        if (selectedPayslip != null) {
            Optional<ButtonType> result = DialogUtils.showConfirmation("Delete Payslip",
                    "Are you sure you want to delete the payslip for " + selectedPayslip.getPeriod() + "?");
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    FinanceService.getInstance().deletePayslip(selectedPayslip.getId());
                    loadSampleData();
                    updatePayslipCount();
                    handleClearPayslipForm();
                    showSuccess("Payslip deleted!");
                } catch (Exception e) {
                    showFieldError(payslip_errorLabel, e.getMessage());
                }
            }
        }
    }

    @FXML
    private void handleClearPayslipForm() {
        payslip_userIdCombo.setValue(null);
        payslip_baseSalaryField.setText("");
        payslip_overtimeField.setText("0");
        payslip_overtimeRateField.setText("0");
        payslip_bonusesField.setText("0");
        payslip_cnssField.setText("");
        payslip_irppField.setText("");
        payslip_otherDeductionsField.setText("0");
        payslip_grossLabel.setText("0.00 TND");
        payslip_deductionsLabel.setText("0.00 TND");
        payslip_netLabel.setText("0.00 TND");
        selectedPayslip = null;
    }

    @FXML
    private void handleRefreshPayslips() {
        payslipTable.refresh();
    }

    @FXML
    private void handleExportPayslipPDF() {
        if (selectedPayslip != null) {
            // Generate single payslip PDF
            showSuccess("PDF export feature coming soon!");
        } else {
            showFieldError(payslip_errorLabel, "Select a payslip first!");
        }
    }

    private void onPayslipSelected() {
        selectedPayslip = payslipTable.getSelectionModel().getSelectedItem();
        if (selectedPayslip != null) {
            payslip_userIdCombo.setValue(getEmployeeStringById(selectedPayslip.getUserId()));
            payslip_monthCombo.setValue(selectedPayslip.getMonth());
            payslip_yearCombo.setValue(selectedPayslip.getYear());
            payslip_baseSalaryField.setText(String.valueOf(selectedPayslip.getBaseSalary()));
            payslip_overtimeField.setText(String.valueOf(selectedPayslip.getOvertime()));
            payslip_bonusesField.setText(String.valueOf(selectedPayslip.getBonuses()));
            payslip_statusCombo.setValue(selectedPayslip.getStatus());
            handleCalculatePayslip();
        }
    }

    private void updatePayslipCount() {
        if (filteredPayslips != null)
            payslip_countLabel.setText("Total: " + filteredPayslips.size());
        else
            payslip_countLabel.setText("Total: " + payslipData.size());
    }

    // REPORTS
    private void initializeReportsTab() {
        tax_currencyCombo.setItems(FXCollections.observableArrayList("TND", "EUR", "USD"));
    }

    @FXML
    private void handleGenerateEmployeeReport() {
        if (report_employeeCombo.getValue() == null) {
            showSuccess("Please select an employee!");
            return;
        }

        String emp = report_employeeCombo.getValue();
        int empId = extractUserId(emp);
        String empName = extractUserName(emp);
        String contractInfo = buildContractInfo(empId);
        String bankInfo = buildBankInfo(empId);
        String bonusInfo = buildBonusInfo(empId);
        String payslipInfo = buildPayslipInfo(empId);
        String professionalSummary = buildProfessionalSummary(empId, empName);

        File pdf = PDFGenerator.generateEmployeeReport(empId, empName,
                contractInfo, bankInfo, bonusInfo, payslipInfo, (Stage) report_employeeCombo.getScene().getWindow(),
                professionalSummary);

        if (pdf != null) {
            showSuccess("Rapport enregistré : " + pdf.getAbsolutePath());
        }
    }

    @FXML
    private void handleCalculateTax() {
        try {
            // VALIDATION
            String error;
            if ((error = ValidationHelper.validatePositiveNumber(tax_grossField.getText(), "Gross salary")) != null) {
                // Afficher erreur dans le hint box
                if (tax_hintBox != null) {
                    tax_hintBox.setVisible(true);
                    tax_hintBox.setManaged(true);
                }
                if (tax_resultsBox != null) {
                    tax_resultsBox.setVisible(false);
                    tax_resultsBox.setManaged(false);
                }
                if (tax_resultArea != null)
                    tax_resultArea.setText("❌ " + error);
                return;
            }

            double gross = Double.parseDouble(tax_grossField.getText().trim());
            double cnss = gross * 0.0918;
            double irpp = (gross - cnss) * 0.26;
            double totalDeduct = cnss + irpp;
            double net = gross - totalDeduct;
            double taux = (totalDeduct / gross) * 100;
            String currency = tax_currencyCombo.getValue() != null ? tax_currencyCombo.getValue() : "TND";

            // ── Mise à jour des cartes KPI ──
            if (tax_grossResult != null)
                tax_grossResult.setText(String.format("%,.2f %s", gross, currency));
            if (tax_cnssResult != null)
                tax_cnssResult.setText(String.format("- %,.2f %s", cnss, currency));
            if (tax_irppResult != null)
                tax_irppResult.setText(String.format("- %,.2f %s", irpp, currency));
            if (tax_totalDeductResult != null)
                tax_totalDeductResult.setText(String.format("- %,.2f %s", totalDeduct, currency));
            if (tax_netResult != null)
                tax_netResult.setText(String.format("%,.2f %s", net, currency));
            if (tax_rateResult != null)
                tax_rateResult.setText(String.format("%.1f %%", taux));
            if (tax_summaryLabel != null)
                tax_summaryLabel.setText(String.format(
                        "Brut: %,.2f %s  |  CNSS: %,.2f %s  |  IRPP: %,.2f %s\nTotal retenu: %,.2f %s  |  Net à percevoir: %,.2f %s",
                        gross, currency, cnss, currency, irpp, currency, totalDeduct, currency, net, currency));
            if (tax_employeeLabelResult != null)
                tax_employeeLabelResult.setText("Calculé le "
                        + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            // ── Afficher les résultats, cacher le hint ──
            if (tax_resultsBox != null) {
                tax_resultsBox.setVisible(true);
                tax_resultsBox.setManaged(true);
            }
            if (tax_hintBox != null) {
                tax_hintBox.setVisible(false);
                tax_hintBox.setManaged(false);
            }

            // Compatibilité: garder aussi le TextArea caché renseigné
            if (tax_resultArea != null) {
                tax_resultArea.setText(String.format(
                        "Brut: %,.2f %s | CNSS: %,.2f | IRPP: %,.2f | Net: %,.2f %s",
                        gross, currency, cnss, irpp, net, currency));
            }

        } catch (Exception e) {
            if (tax_resultArea != null)
                tax_resultArea.setText("❌ ERROR: Please enter a valid number!");
        }
    }

    private void setupSearchFilter() {
        if (mainSearchField != null) {
            mainSearchField.getControl().textProperty().addListener((obs, oldV, newV) -> {
                applyGlobalFilter(newV);
            });
        }
    }

    private void applyGlobalFilter(String searchText) {
        if (searchText == null)
            searchText = "";
        final String lowerCaseFilter = searchText.toLowerCase();

        if (filteredContracts != null) {
            filteredContracts.setPredicate(row -> {
                if (lowerCaseFilter.isEmpty())
                    return true;
                return row.getEmployeeName().toLowerCase().contains(lowerCaseFilter);
            });
        }

        if (filteredBanks != null) {
            filteredBanks.setPredicate(row -> {
                if (lowerCaseFilter.isEmpty())
                    return true;
                return row.getEmployeeName().toLowerCase().contains(lowerCaseFilter);
            });
        }

        if (filteredBonuses != null) {
            filteredBonuses.setPredicate(row -> {
                if (lowerCaseFilter.isEmpty())
                    return true;
                return row.getEmployeeName().toLowerCase().contains(lowerCaseFilter);
            });
        }

        if (filteredPayslips != null) {
            filteredPayslips.setPredicate(row -> {
                if (lowerCaseFilter.isEmpty())
                    return true;
                return row.getEmployeeName().toLowerCase().contains(lowerCaseFilter);
            });
        }

        updateContractCount();
        updateBankCount();
        updateBonusCount();
        updatePayslipCount();
    }

    @FXML
    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        themeToggleBtn.setText(isDarkMode ? "🌙 Dark" : "☀️ Light");
        showSuccess("Theme toggled!");
    }

    // UTILITY METHODS
    private void loadSampleData() {
        FinanceService service = FinanceService.getInstance();

        // === USERS (pour les combos) ===
        try {
            UserService userService = UserService.getInstance();
            List<User> users = userService.getAllUsers();
            ObservableList<String> userList = FXCollections.observableArrayList();
            for (User u : users) {
                userList.add(u.getId() + " - " + u.getFullName());
            }
            if (contract_userIdCombo != null)
                contract_userIdCombo.setItems(userList);
            if (bank_userIdCombo != null)
                bank_userIdCombo.setItems(userList);
            if (bonus_userIdCombo != null)
                bonus_userIdCombo.setItems(userList);
            if (payslip_userIdCombo != null)
                payslip_userIdCombo.setItems(userList);
            if (report_employeeCombo != null)
                report_employeeCombo.setItems(userList);
        } catch (Exception e) {
            System.err.println("[Finance] Erreur chargement utilisateurs: " + e.getMessage());
            e.printStackTrace();
        }

        // === CONTRATS ===
        try {
            contractData.setAll(service.getAllContracts());
            updateContractCount();
        } catch (Exception e) {
            System.err.println("[Finance] Erreur chargement contrats: " + e.getMessage());
            e.printStackTrace();
            showFieldError(contract_errorLabel, "Erreur chargement: " + e.getMessage());
        }

        // === COMPTES BANCAIRES ===
        try {
            bankData.setAll(service.getAllBankAccounts());
            updateBankCount();
        } catch (Exception e) {
            System.err.println("[Finance] Erreur chargement comptes bancaires: " + e.getMessage());
            e.printStackTrace();
            showFieldError(bank_errorLabel, "Erreur chargement: " + e.getMessage());
        }

        // === PRIMES / BONUS ===
        try {
            bonusData.setAll(service.getAllBonuses());
            updateBonusCount();
        } catch (Exception e) {
            System.err.println("[Finance] Erreur chargement primes: " + e.getMessage());
            e.printStackTrace();
            showFieldError(bonus_errorLabel, "Erreur chargement: " + e.getMessage());
        }

        // === BULLETINS DE PAIE ===
        try {
            payslipData.setAll(service.getAllPayslips());
            updatePayslipCount();
        } catch (Exception e) {
            System.err.println("[Finance] Erreur chargement bulletins: " + e.getMessage());
            e.printStackTrace();
            showFieldError(payslip_errorLabel, "Erreur chargement: " + e.getMessage());
        }
    }

    private String getEmployeeStringById(int id) {
        if (contract_userIdCombo != null && contract_userIdCombo.getItems() != null) {
            String prefix = id + " - ";
            for (String s : contract_userIdCombo.getItems()) {
                if (s.startsWith(prefix))
                    return s;
            }
        }
        return null;
    }

    private int extractUserId(String selection) {
        if (selection == null || !selection.contains("-"))
            return -1;
        try {
            return Integer.parseInt(selection.split(" - ")[0].trim());
        } catch (Exception e) {
            return -1;
        }
    }

    private String extractUserName(String selection) {
        if (selection == null || !selection.contains("-"))
            return "";
        try {
            return selection.split(" - ", 2)[1].trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static final String[] MONTHS_FR = { "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre" };

    private String buildContractInfo(int empId) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"salary-grid\">");
        java.text.NumberFormat nf = java.text.NumberFormat.getIntegerInstance(Locale.FRANCE);
        List<ContractRow> userContracts = contractData.stream().filter(c -> c.getUserId() == empId).toList();
        if (userContracts.isEmpty()) {
            sb.append("<p class=\"report-field-value\" style=\"color:#6a7f96;\">Aucun contrat trouv\u00e9.</p>");
            sb.append("</div>");
            return sb.toString();
        }
        for (ContractRow c : userContracts) {
            String pos = escapeHtml(c.getPosition() != null ? c.getPosition() : "\u2014");
            String posCap = capitalizeWords(pos);
            double sal = c.getSalary();
            String typeStr = escapeHtml(c.getType() != null ? c.getType().toUpperCase(Locale.FRENCH) : "\u2014");
            String startStr = escapeHtml(c.getStartDate() != null ? c.getStartDate() : "\u2014");
            String endStr = escapeHtml(
                    (c.getEndDate() != null && !c.getEndDate().isBlank()) ? c.getEndDate() : "\u2014");
            LocalDate start = null, end = null;
            try {
                if (c.getStartDate() != null && !c.getStartDate().isBlank())
                    start = LocalDate.parse(c.getStartDate());
            } catch (Exception ignored) {}
            try {
                if (c.getEndDate() != null && !c.getEndDate().isBlank())
                    end = LocalDate.parse(c.getEndDate());
            } catch (Exception ignored) {}
            String anciennete = "\u2014";
            if (start != null && end != null && !end.isBefore(start))
                anciennete = formatAnciennete(start, end);
            else if (start != null && end != null && end.isBefore(start))
                anciennete = "Dates invalides";

            sb.append("<div class=\"salary-card\">");
            sb.append("<div class=\"report-field\"><span class=\"report-field-label\">Position dans notre soci\u00e9t\u00e9 :</span> <span class=\"report-field-value\">").append(posCap).append("</span></div>");
            sb.append("<div class=\"report-field\"><span class=\"report-field-label\">Type de contrat :</span> <span class=\"report-field-value\">").append(typeStr).append("</span></div>");
            sb.append("<div class=\"report-field\"><span class=\"report-field-label\">Salaire mensuel (salaire de base) :</span> <span class=\"report-field-value\">").append(nf.format((long) sal)).append(" <span class=\"currency\">TND</span></span></div>");
            sb.append("<div class=\"report-field\"><span class=\"report-field-label\">Date de d\u00e9but du travail :</span> <span class=\"report-field-value\">").append(startStr).append("</span></div>");
            sb.append("<div class=\"report-field\"><span class=\"report-field-label\">Date de fin du contrat :</span> <span class=\"report-field-value\">").append(endStr).append("</span></div>");
            sb.append("<div class=\"report-field\"><span class=\"report-field-label\">Anciennet\u00e9 :</span> <span class=\"report-field-value\">").append(escapeHtml(anciennete)).append("</span></div>");
            sb.append("</div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String buildBankInfo(int empId) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"bank-grid\">");
        List<BankAccountRow> userBanks = bankData.stream().filter(b -> b.getUserId() == empId).toList();
        if (userBanks.isEmpty()) {
            sb.append("<p class=\"report-field-value\" style=\"color:#6a7f96;\">Aucun compte bancaire.</p>");
            sb.append("</div>");
            return sb.toString();
        }
        for (BankAccountRow b : userBanks) {
            String bankName = escapeHtml(b.getBankName() != null ? b.getBankName() : "\u2014");
            String iban = escapeHtml(b.getIban() != null ? b.getIban() : "\u2014");
            sb.append("<div class=\"bank-card\">");
            sb.append("<div class=\"report-field\"><span class=\"report-field-label\">Nom de la banque :</span> <span class=\"report-field-value\">").append(capitalizeWords(bankName)).append("</span></div>");
            sb.append("<div class=\"report-field\"><span class=\"report-field-label\">IBAN (num\u00e9ro de compte) :</span> <span class=\"report-field-value\">").append(iban).append("</span></div>");
            sb.append("</div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String buildBonusInfo(int empId) {
        StringBuilder sb = new StringBuilder();
        List<BonusRow> list = bonusData.stream().filter(b -> b.getUserId() == empId).toList();
        if (list.isEmpty()) {
            return "<p class=\"report-field-value\" style=\"color:#6a7f96;\">Aucune prime.</p>";
        }
        double total = list.stream().mapToDouble(BonusRow::getAmount).sum();
        String label = list.size() == 1 && list.get(0).getReason() != null && !list.get(0).getReason().isEmpty()
                ? capitalizeWords(escapeHtml(list.get(0).getReason()))
                : (list.size() > 1 ? "Total des primes" : "Prime mensuelle");
        sb.append("<div class=\"bonus-badge\">");
        sb.append("<div class=\"report-field\"><span class=\"report-field-label\">Montant total des primes :</span> <span class=\"b-amount\">").append(String.format(Locale.FRANCE, "%.0f", total)).append(" <span class=\"b-currency\">TND</span></span></div>");
        sb.append("<div class=\"report-field\"><span class=\"report-field-label\">D\u00e9signation :</span> <span class=\"b-label\">").append(label).append("</span></div>");
        sb.append("</div>");
        return sb.toString();
    }

    /**
     * Construit un résumé professionnel unique par employé pour le rapport PDF
     * (poste, type de contrat, salaire, bulletins, primes, comptes bancaires).
     */
    private String buildProfessionalSummary(int empId, String employeeName) {
        String name = (employeeName != null && !employeeName.isBlank()) ? employeeName : "L'employé";
        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(Locale.FRANCE);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        List<ContractRow> contracts = contractData.stream().filter(c -> c.getUserId() == empId).toList();
        List<PayslipRow> payslips = new java.util.ArrayList<>(
                payslipData.stream().filter(p -> p.getUserId() == empId).toList());
        payslips.sort(Comparator.comparingInt(PayslipRow::getYear).reversed().thenComparingInt(PayslipRow::getMonth)
                .reversed());
        List<BonusRow> bonuses = bonusData.stream().filter(b -> b.getUserId() == empId).toList();
        long bankCount = bankData.stream().filter(b -> b.getUserId() == empId).count();

        StringBuilder sb = new StringBuilder();
        ContractRow active = contracts.stream()
                .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()) || "ACTIF".equalsIgnoreCase(c.getStatus()))
                .findFirst().orElse(contracts.isEmpty() ? null : contracts.get(0));
        if (active != null) {
            String poste = active.getPosition() != null ? active.getPosition() : "—";
            String type = active.getType() != null ? active.getType().toUpperCase() : "—";
            double sal = active.getSalary();
            sb.append(name).append(" occupe le poste de ").append(poste).append(" en ").append(type);
            sb.append(" (salaire de base : ").append(nf.format(sal)).append(" TND). ");
        } else {
            sb.append(name).append(" n'a pas de contrat actif enregistré. ");
        }
        if (!payslips.isEmpty()) {
            sb.append("Sur la période couverte, ").append(payslips.size()).append(" bulletin(s) de paie ont été émis");
            PayslipRow last = payslips.get(0);
            String period = (last.getMonth() >= 1 && last.getMonth() <= 12)
                    ? (MONTHS_FR[last.getMonth() - 1] + " " + last.getYear())
                    : "";
            if (!period.isEmpty())
                sb.append(" ; le dernier (").append(period).append(") s'élève à ").append(nf.format(last.getNet()))
                        .append(" TND net");
            sb.append(". ");
        }
        if (!bonuses.isEmpty()) {
            double totalBonus = bonuses.stream().mapToDouble(BonusRow::getAmount).sum();
            sb.append(bonuses.size()).append(" prime(s) versée(s) (total : ").append(nf.format(totalBonus))
                    .append(" TND). ");
        }
        if (bankCount > 0)
            sb.append(bankCount).append(" compte(s) bancaire(s) enregistré(s) pour le virement du salaire.");
        return sb.toString().trim();
    }

    private String buildPayslipInfo(int empId) {
        StringBuilder sb = new StringBuilder();
        List<PayslipRow> userPayslips = payslipData.stream().filter(p -> p.getUserId() == empId).toList();
        if (userPayslips.isEmpty())
            return "<p class=\"report-field-value\" style=\"color:#6a7f96;\">Aucun bulletin de paie r\u00e9cent.</p>";

        java.text.NumberFormat nfDecimal = java.text.NumberFormat.getNumberInstance(Locale.FRANCE);
        nfDecimal.setMinimumFractionDigits(2);
        nfDecimal.setMaximumFractionDigits(2);

        for (PayslipRow p : userPayslips) {
            int m = p.getMonth(), y = p.getYear();
            String periodLabel = (m >= 1 && m <= 12) ? (MONTHS_FR[m - 1] + " " + y)
                    : (p.getPeriod() != null ? p.getPeriod() : "\u2014");
            boolean paid = p.getStatus() != null
                    && (p.getStatus().equalsIgnoreCase("PAID") || p.getStatus().equalsIgnoreCase("PAY\u00c9"));
            String tag = paid ? "Pay\u00e9" : "En attente";

            sb.append("<div class=\"payslip-row\"><table><tr>");
            sb.append("<td style=\"width:38%;\"><div class=\"payslip-col-label\">P\u00e9riode :</div><div class=\"payslip-col-value\">").append(escapeHtml(periodLabel)).append("</div></td>");
            sb.append("<td style=\"width:42%;\"><div class=\"payslip-col-label\">Net \u00e0 payer :</div><div class=\"payslip-net\">").append(nfDecimal.format(p.getNet())).append(" <span class=\"payslip-net-currency\">TND</span></div></td>");
            sb.append("<td style=\"text-align:right;\"><div class=\"payslip-col-label\">Statut :</div><span class=\"tag\">").append(escapeHtml(tag)).append("</span></td>");
            sb.append("</tr></table></div>");
        }
        return sb.toString();
    }

    /** Met la première lettre de chaque mot en majuscule (ex. "formateur java" → "Formateur Java"). */
    private static String capitalizeWords(String s) {
        if (s == null || s.isBlank())
            return s == null ? "" : s.trim();
        String t = s.trim();
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (Character.isWhitespace(c)) {
                cap = true;
                sb.append(c);
            } else if (cap) {
                sb.append(Character.toUpperCase(c));
                cap = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    private static String escapeHtml(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private void showFieldError(Label label, String message) {
        label.setText("⚠️ " + message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void clearFieldError(Label label) {
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    private void showSuccess(String message) {
        System.out.println("✅ " + message);
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
        });
    }

    private boolean isValidDouble(String s) {
        if (s == null || s.isEmpty())
            return false;
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private double parseDouble(String text, double defaultValue) {
        try {
            return Double.parseDouble(text);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
