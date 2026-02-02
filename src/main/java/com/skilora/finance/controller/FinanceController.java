package com.skilora.finance.controller;

import com.skilora.framework.components.*;
import com.skilora.user.entity.*;
import com.skilora.finance.entity.*;
import com.skilora.finance.service.*;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * FinanceController - Employee-facing finance view.
 * Tab-based interface for viewing contracts, payslips, bank accounts, and salary history.
 */
public class FinanceController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(FinanceController.class);

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private HBox tabBox;
    @FXML private VBox contentPane;

    private User currentUser;

    private final ContractService contractService = ContractService.getInstance();
    private final PayslipService payslipService = PayslipService.getInstance();
    private final BankAccountService bankAccountService = BankAccountService.getInstance();
    private final SalaryHistoryService salaryHistoryService = SalaryHistoryService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        titleLabel.setText(I18n.get("finance.title"));
        subtitleLabel.setText(I18n.get("finance.subtitle"));
        createTabs();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        showContractTab();
    }

    // ==================== Tab Navigation ====================

    private void createTabs() {
        tabBox.getChildren().clear();
        TLTabs tabs = new TLTabs();
        tabs.addTab("contract", I18n.get("finance.tab.contract"), (javafx.scene.Node) null);
        tabs.addTab("payslips", I18n.get("finance.tab.payslips"), (javafx.scene.Node) null);
        tabs.addTab("bank", I18n.get("finance.tab.bank"), (javafx.scene.Node) null);
        tabs.addTab("history", I18n.get("finance.tab.history"), (javafx.scene.Node) null);
        tabs.setOnTabChanged(tabId -> {
            switch (tabId) {
                case "contract" -> showContractTab();
                case "payslips" -> showPayslipsTab();
                case "bank" -> showBankTab();
                case "history" -> showHistoryTab();
            }
        });
        tabBox.getChildren().add(tabs);
    }

    // ==================== Contract Tab ====================

    private void showContractTab() {
        if (currentUser == null) return;

        contentPane.getChildren().clear();
        contentPane.getChildren().add(createLoadingIndicator());

        Task<EmploymentContract> task = new Task<>() {
            @Override
            protected EmploymentContract call() throws Exception {
                return contractService.findActiveByUserId(currentUser.getId());
            }
        };

        task.setOnSucceeded(e -> {
            EmploymentContract contract = task.getValue();
            Platform.runLater(() -> displayContract(contract));
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load contract", task.getException());
            Platform.runLater(() -> {
                contentPane.getChildren().clear();
                contentPane.getChildren().add(createEmptyState(I18n.get("finance.contract.error")));
            });
        });

        new Thread(task).start();
    }

    private void displayContract(EmploymentContract contract) {
        contentPane.getChildren().clear();

        if (contract == null) {
            contentPane.getChildren().add(createEmptyState(I18n.get("finance.contract.empty")));
            return;
        }

        TLCard card = new TLCard();
        card.setHeader(I18n.get("finance.contract.active"));

        VBox details = new VBox(12);
        details.setPadding(new Insets(16));

        details.getChildren().add(createDetailRow(
                I18n.get("finance.contract.type"),
                contract.getContractType() != null ? contract.getContractType() : "-"));

        details.getChildren().add(createDetailRow(
                I18n.get("finance.contract.salary"),
                contract.getSalaryBase() != null
                        ? contract.getSalaryBase().toPlainString() + " " + contract.getCurrency()
                        : "-"));

        details.getChildren().add(createDetailRow(
                I18n.get("finance.contract.start_date"),
                contract.getStartDate() != null
                        ? contract.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        : "-"));

        details.getChildren().add(createDetailRow(
                I18n.get("finance.contract.end_date"),
                contract.getEndDate() != null
                        ? contract.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        : I18n.get("finance.contract.indefinite")));

        details.getChildren().add(createDetailRow(
                I18n.get("finance.contract.status"),
                contract.getStatus() != null ? contract.getStatus() : "-"));

        details.getChildren().add(createDetailRow(
                I18n.get("finance.contract.employer"),
                contract.getEmployerName() != null ? contract.getEmployerName() : "-"));

        // Add sign button if status is PENDING_SIGNATURE
        if ("PENDING_SIGNATURE".equals(contract.getStatus())) {
            TLButton signBtn = new TLButton(I18n.get("finance.contract.sign"), TLButton.ButtonVariant.SUCCESS);
            signBtn.setOnAction(e -> signContract(contract.getId()));
            HBox btnBox = new HBox(signBtn);
            btnBox.setAlignment(Pos.CENTER_RIGHT);
            btnBox.setPadding(new Insets(8, 0, 0, 0));
            details.getChildren().add(btnBox);
        }

        card.setContent(details);
        contentPane.getChildren().add(card);
    }

    // ==================== Payslips Tab ====================

    private void showPayslipsTab() {
        if (currentUser == null) return;

        contentPane.getChildren().clear();
        contentPane.getChildren().add(createLoadingIndicator());

        Task<List<Payslip>> task = new Task<>() {
            @Override
            protected List<Payslip> call() throws Exception {
                return payslipService.findByUserId(currentUser.getId());
            }
        };

        task.setOnSucceeded(e -> {
            List<Payslip> payslips = task.getValue();
            Platform.runLater(() -> displayPayslips(payslips));
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load payslips", task.getException());
            Platform.runLater(() -> {
                contentPane.getChildren().clear();
                contentPane.getChildren().add(createEmptyState(I18n.get("finance.payslips.error")));
            });
        });

        new Thread(task).start();
    }

    private void displayPayslips(List<Payslip> payslips) {
        contentPane.getChildren().clear();

        if (payslips == null || payslips.isEmpty()) {
            contentPane.getChildren().add(createEmptyState(I18n.get("finance.payslips.empty")));
            return;
        }

        VBox payslipList = new VBox(12);

        for (Payslip payslip : payslips) {
            TLCard card = new TLCard();

            VBox content = new VBox(8);
            content.setPadding(new Insets(16));

            // Period header
            String period = String.format("%02d/%d", payslip.getPeriodMonth(), payslip.getPeriodYear());
            Label periodLabel = new Label(I18n.get("finance.payslip.period") + ": " + period);
            periodLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            // Status badge
            TLBadge statusBadge = new TLBadge(
                    payslip.getPaymentStatus() != null ? payslip.getPaymentStatus() : "PENDING",
                    getPaymentBadgeVariant(payslip.getPaymentStatus()));

            HBox header = new HBox(12, periodLabel, statusBadge);
            header.setAlignment(Pos.CENTER_LEFT);

            content.getChildren().add(header);

            content.getChildren().add(createDetailRow(
                    I18n.get("finance.payslip.gross"),
                    formatAmount(payslip.getGrossSalary(), payslip.getCurrency())));

            content.getChildren().add(createDetailRow(
                    I18n.get("finance.payslip.cnss"),
                    formatAmount(payslip.getCnssEmployee(), payslip.getCurrency())));

            content.getChildren().add(createDetailRow(
                    I18n.get("finance.payslip.irpp"),
                    formatAmount(payslip.getIrpp(), payslip.getCurrency())));

            content.getChildren().add(createDetailRow(
                    I18n.get("finance.payslip.net"),
                    formatAmount(payslip.getNetSalary(), payslip.getCurrency())));

            card.setContent(content);
            payslipList.getChildren().add(card);
        }

        ScrollPane scrollPane = new ScrollPane(payslipList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        contentPane.getChildren().add(scrollPane);
    }

    // ==================== Bank Tab ====================

    private void showBankTab() {
        if (currentUser == null) return;

        contentPane.getChildren().clear();
        contentPane.getChildren().add(createLoadingIndicator());

        Task<List<BankAccount>> task = new Task<>() {
            @Override
            protected List<BankAccount> call() throws Exception {
                return bankAccountService.findByUserId(currentUser.getId());
            }
        };

        task.setOnSucceeded(e -> {
            List<BankAccount> accounts = task.getValue();
            Platform.runLater(() -> displayBankAccounts(accounts));
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load bank accounts", task.getException());
            Platform.runLater(() -> {
                contentPane.getChildren().clear();
                contentPane.getChildren().add(createEmptyState(I18n.get("finance.bank.error")));
            });
        });

        new Thread(task).start();
    }

    private void displayBankAccounts(List<BankAccount> accounts) {
        contentPane.getChildren().clear();

        // Add Account button
        TLButton addBtn = new TLButton(I18n.get("finance.bank.add"), TLButton.ButtonVariant.PRIMARY);
        addBtn.setOnAction(e -> showAddBankAccountDialog());
        HBox btnBox = new HBox(addBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        contentPane.getChildren().add(btnBox);

        if (accounts == null || accounts.isEmpty()) {
            contentPane.getChildren().add(createEmptyState(I18n.get("finance.bank.empty")));
            return;
        }

        VBox accountList = new VBox(12);

        for (BankAccount account : accounts) {
            TLCard card = new TLCard();

            VBox content = new VBox(8);
            content.setPadding(new Insets(16));

            // Bank name header with primary badge
            Label bankLabel = new Label(account.getBankName() != null ? account.getBankName() : "-");
            bankLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            HBox header = new HBox(12, bankLabel);
            header.setAlignment(Pos.CENTER_LEFT);

            if (account.isPrimary()) {
                TLBadge primaryBadge = new TLBadge(
                        I18n.get("finance.bank.primary"), TLBadge.Variant.SUCCESS);
                header.getChildren().add(primaryBadge);
            }

            content.getChildren().add(header);

            content.getChildren().add(createDetailRow(
                    I18n.get("finance.bank.holder"),
                    account.getAccountHolder() != null ? account.getAccountHolder() : "-"));

            content.getChildren().add(createDetailRow(
                    I18n.get("finance.bank.iban"),
                    account.getIban() != null ? account.getIban() : "-"));

            content.getChildren().add(createDetailRow(
                    I18n.get("finance.bank.rib"),
                    account.getRib() != null ? account.getRib() : "-"));

            content.getChildren().add(createDetailRow(
                    I18n.get("finance.bank.currency"),
                    account.getCurrency() != null ? account.getCurrency() : "TND"));

            card.setContent(content);
            accountList.getChildren().add(card);
        }

        contentPane.getChildren().add(accountList);
    }

    private void showAddBankAccountDialog() {
        if (currentUser == null) return;

        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("finance.bank.add"));
        dialog.setDescription(I18n.get("finance.bank.add.description"));

        TLTextField bankNameField = new TLTextField(I18n.get("finance.bank.name"), I18n.get("finance.bank.name.placeholder"));
        TLTextField holderField = new TLTextField(I18n.get("finance.bank.holder"), I18n.get("finance.bank.holder.placeholder"));
        TLTextField ibanField = new TLTextField(I18n.get("finance.bank.iban"), I18n.get("finance.bank.iban.placeholder"));
        TLTextField ribField = new TLTextField(I18n.get("finance.bank.rib"), I18n.get("finance.bank.rib.placeholder"));

        VBox form = new VBox(12, bankNameField, holderField, ibanField, ribField);
        form.setPadding(new Insets(16));
        dialog.setContent(form);

        dialog.addButton(ButtonType.CANCEL);
        dialog.addButton(ButtonType.OK);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                String bankName = bankNameField.getText() != null ? bankNameField.getText().trim() : "";
                String holder = holderField.getText() != null ? holderField.getText().trim() : "";

                if (bankName.isEmpty()) {
                    DialogUtils.showError(I18n.get("message.error"),
                        I18n.get("error.validation.required", I18n.get("finance.bank.name")));
                    return;
                }

                if (holder.isEmpty()) {
                    DialogUtils.showError(I18n.get("message.error"),
                        I18n.get("error.validation.required", I18n.get("finance.bank.holder")));
                    return;
                }

                createBankAccount(
                        bankName,
                        holder,
                        ibanField.getText() != null ? ibanField.getText().trim() : "",
                        ribField.getText() != null ? ribField.getText().trim() : "");
            }
        });
    }

    private void createBankAccount(String bankName, String holder, String iban, String rib) {
        if (currentUser == null) return;

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                BankAccount account = new BankAccount(currentUser.getId(), bankName, holder);
                account.setIban(iban);
                account.setRib(rib);
                return bankAccountService.create(account);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(this::showBankTab));

        task.setOnFailed(e -> {
            logger.error("Failed to create bank account", task.getException());
        });

        new Thread(task).start();
    }

    // ==================== History Tab ====================

    private void showHistoryTab() {
        if (currentUser == null) return;

        contentPane.getChildren().clear();
        contentPane.getChildren().add(createLoadingIndicator());

        Task<List<SalaryHistory>> task = new Task<>() {
            @Override
            protected List<SalaryHistory> call() throws Exception {
                // First find active contract, then load its salary history
                EmploymentContract contract = contractService.findActiveByUserId(currentUser.getId());
                if (contract == null) {
                    // Try loading all contracts and use the most recent one
                    List<EmploymentContract> contracts = contractService.findByUserId(currentUser.getId());
                    if (contracts != null && !contracts.isEmpty()) {
                        contract = contracts.get(0);
                    }
                }
                if (contract != null) {
                    return salaryHistoryService.findByContractId(contract.getId());
                }
                return List.of();
            }
        };

        task.setOnSucceeded(e -> {
            List<SalaryHistory> history = task.getValue();
            Platform.runLater(() -> displaySalaryHistory(history));
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load salary history", task.getException());
            Platform.runLater(() -> {
                contentPane.getChildren().clear();
                contentPane.getChildren().add(createEmptyState(I18n.get("finance.history.error")));
            });
        });

        new Thread(task).start();
    }

    private void displaySalaryHistory(List<SalaryHistory> history) {
        contentPane.getChildren().clear();

        if (history == null || history.isEmpty()) {
            contentPane.getChildren().add(createEmptyState(I18n.get("finance.history.empty")));
            return;
        }

        VBox historyList = new VBox(12);

        for (SalaryHistory entry : history) {
            TLCard card = new TLCard();

            VBox content = new VBox(8);
            content.setPadding(new Insets(16));

            String dateStr = entry.getEffectiveDate() != null
                    ? entry.getEffectiveDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "-";
            Label dateLabel = new Label(I18n.get("finance.history.effective_date") + ": " + dateStr);
            dateLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            content.getChildren().add(dateLabel);

            content.getChildren().add(createDetailRow(
                    I18n.get("finance.history.old_salary"),
                    entry.getOldSalary() != null ? entry.getOldSalary().toPlainString() : "-"));

            content.getChildren().add(createDetailRow(
                    I18n.get("finance.history.new_salary"),
                    entry.getNewSalary() != null ? entry.getNewSalary().toPlainString() : "-"));

            content.getChildren().add(createDetailRow(
                    I18n.get("finance.history.reason"),
                    entry.getReason() != null ? entry.getReason() : "-"));

            card.setContent(content);
            historyList.getChildren().add(card);
        }

        ScrollPane scrollPane = new ScrollPane(historyList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        contentPane.getChildren().add(scrollPane);
    }

    // ==================== Contract Signing ====================

    private void signContract(int contractId) {
        DialogUtils.showConfirmation(
            I18n.get("finance.contract.sign.title"),
            I18n.get("finance.contract.sign.confirm") + "\n" + I18n.get("finance.contract.sign.message")
        ).ifPresent(result -> {
            if (result == ButtonType.OK) {
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() throws Exception {
                        return contractService.sign(contractId);
                    }
                };

                task.setOnSucceeded(e -> {
                    Platform.runLater(() -> {
                        logger.info("Contract {} signed successfully", contractId);
                        showContractTab();
                    });
                });

                task.setOnFailed(e -> {
                    logger.error("Failed to sign contract {}", contractId, task.getException());
                });

                new Thread(task).start();
            }
        });
    }

    // ==================== Helper Methods ====================

    private HBox createDetailRow(String label, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label labelNode = new Label(label + ":");
        labelNode.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-muted-foreground;");
        labelNode.setMinWidth(Region.USE_PREF_SIZE);

        Label valueNode = new Label(value);
        valueNode.setWrapText(true);

        row.getChildren().addAll(labelNode, valueNode);
        return row;
    }

    private VBox createEmptyState(String message) {
        VBox emptyState = new VBox(12);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(48));

        Label label = new Label(message);
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        emptyState.getChildren().add(label);
        return emptyState;
    }

    private StackPane createLoadingIndicator() {
        return TLSpinner.createCentered(TLSpinner.Size.LG);
    }

    private String formatAmount(BigDecimal amount, String currency) {
        if (amount == null) return "-";
        return amount.toPlainString() + " " + (currency != null ? currency : "TND");
    }

    private TLBadge.Variant getPaymentBadgeVariant(String status) {
        if (status == null) return TLBadge.Variant.DEFAULT;
        return switch (status) {
            case "PAID" -> TLBadge.Variant.SUCCESS;
            case "PENDING" -> TLBadge.Variant.SECONDARY;
            case "FAILED" -> TLBadge.Variant.DESTRUCTIVE;
            default -> TLBadge.Variant.DEFAULT;
        };
    }
}
