package com.skilora.finance.controller;

import com.skilora.framework.components.*;
import com.skilora.user.entity.*;
import com.skilora.finance.entity.*;
import com.skilora.finance.service.*;
import com.skilora.finance.utils.PDFGenerator;
import com.skilora.finance.utils.ValidationHelper;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
    private final UserCurrencyService userCurrencyService = UserCurrencyService.getInstance();
    private final CurrencyApiService currencyApiService = CurrencyApiService.getInstance();
    private final PaymentTransactionService paymentTransactionService = PaymentTransactionService.getInstance();
    private final BalanceSimulationService balanceSimulationService = BalanceSimulationService.getInstance();
    private final FinanceDataService financeDataService = FinanceDataService.getInstance();

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
        tabs.addTab("transactions", I18n.get("finance.tab.transactions"), (javafx.scene.Node) null);
        tabs.addTab("simulation", I18n.get("finance.tab.simulation"), (javafx.scene.Node) null);
        tabs.addTab("reports", "Reports", (javafx.scene.Node) null);
        tabs.setOnTabChanged(tabId -> {
            switch (tabId) {
                case "contract" -> showContractTab();
                case "payslips" -> showPayslipsTab();
                case "bank" -> showBankTab();
                case "history" -> showHistoryTab();
                case "transactions" -> showTransactionsTab();
                case "simulation" -> showSimulationTab();
                case "reports" -> showReportsTab();
            }
        });
        tabBox.getChildren().add(tabs);
    }

    // ==================== Contract Tab ====================

    private void showContractTab() {
        if (currentUser == null) return;

        contentPane.getChildren().clear();
        contentPane.getChildren().addAll(createCurrencyBanner(), createLoadingIndicator());

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

        AppThreadPool.execute(task);
    }

    private void displayContract(EmploymentContract contract) {
        contentPane.getChildren().clear();

        if (contract == null) {
            contentPane.getChildren().add(createEmptyState(SvgIcons.FILE_TEXT, "No contracts found", "Your employment contracts will appear here."));
            return;
        }

        TLCard card = new TLCard();
        card.setHeader(I18n.get("finance.contract.active"));

        VBox details = new VBox(12);

        details.getChildren().add(createDetailRow(
                I18n.get("finance.contract.type"),
                contract.getContractType() != null ? contract.getContractType() : "-"));

        details.getChildren().add(createDetailRow(
                I18n.get("finance.contract.salary"),
                contract.getSalaryBase() != null
                        ? formatWithPreferred(contract.getSalaryBase(), contract.getCurrency())
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
        contentPane.getChildren().addAll(createCurrencyBanner(), createLoadingIndicator());

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

        AppThreadPool.execute(task);
    }

    private void displayPayslips(List<Payslip> payslips) {
        contentPane.getChildren().clear();

        if (payslips == null || payslips.isEmpty()) {
            contentPane.getChildren().add(createEmptyState(SvgIcons.CLIPBOARD, "No payslips yet", "Generated payslips will appear here."));
            return;
        }

        VBox payslipList = new VBox(12);

        for (Payslip payslip : payslips) {
            TLCard card = new TLCard();

            VBox content = new VBox(8);

            // Period header
            String period = String.format("%02d/%d", payslip.getPeriodMonth(), payslip.getPeriodYear());
            Label periodLabel = new Label(I18n.get("finance.payslip.period") + ": " + period);
            periodLabel.getStyleClass().addAll("text-base", "font-bold");

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
        scrollPane.getStyleClass().add("bg-transparent");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        contentPane.getChildren().add(scrollPane);
    }

    // ==================== Bank Tab ====================

    private void showBankTab() {
        if (currentUser == null) return;

        contentPane.getChildren().clear();
        contentPane.getChildren().addAll(createCurrencyBanner(), createLoadingIndicator());

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

        AppThreadPool.execute(task);
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
            contentPane.getChildren().add(createEmptyState(SvgIcons.DOLLAR_SIGN, "No bank details", "Add your bank information to receive payments."));
            return;
        }

        VBox accountList = new VBox(12);

        for (BankAccount account : accounts) {
            TLCard card = new TLCard();

            VBox content = new VBox(8);

            // Bank name header with primary badge
            Label bankLabel = new Label(account.getBankName() != null ? account.getBankName() : "-");
            bankLabel.getStyleClass().addAll("text-base", "font-bold");

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
        dialog.setContent(form);

        dialog.addButton(ButtonType.CANCEL);
        dialog.addButton(ButtonType.OK);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                boolean valid = true;
                String bankName = bankNameField.getText() != null ? bankNameField.getText().trim() : "";
                String holder = holderField.getText() != null ? holderField.getText().trim() : "";

                if (bankName.isEmpty()) {
                    bankNameField.setError(I18n.get("error.validation.required", I18n.get("finance.bank.name")));
                    valid = false;
                } else { bankNameField.clearValidation(); }

                if (holder.isEmpty()) {
                    holderField.setError(I18n.get("error.validation.required", I18n.get("finance.bank.holder")));
                    valid = false;
                } else { holderField.clearValidation(); }

                if (!valid) event.consume();
            }
        );

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                createBankAccount(
                    bankNameField.getText() != null ? bankNameField.getText().trim() : "",
                    holderField.getText() != null ? holderField.getText().trim() : "",
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

        AppThreadPool.execute(task);
    }

    // ==================== History Tab ====================

    private void showHistoryTab() {
        if (currentUser == null) return;

        contentPane.getChildren().clear();
        contentPane.getChildren().addAll(createCurrencyBanner(), createLoadingIndicator());

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

        AppThreadPool.execute(task);
    }

    private void displaySalaryHistory(List<SalaryHistory> history) {
        contentPane.getChildren().clear();

        if (history == null || history.isEmpty()) {
            contentPane.getChildren().add(createEmptyState(SvgIcons.CLOCK, "No salary history", "Salary changes will be recorded here."));
            return;
        }

        VBox historyList = new VBox(12);

        for (SalaryHistory entry : history) {
            TLCard card = new TLCard();

            VBox content = new VBox(8);

            String dateStr = entry.getEffectiveDate() != null
                    ? entry.getEffectiveDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "-";
            Label dateLabel = new Label(I18n.get("finance.history.effective_date") + ": " + dateStr);
            dateLabel.getStyleClass().addAll("text-sm", "font-bold");

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
        scrollPane.getStyleClass().add("bg-transparent");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        contentPane.getChildren().add(scrollPane);
    }

    // ==================== Transactions Tab ====================

    private void showTransactionsTab() {
        if (currentUser == null) return;

        contentPane.getChildren().clear();
        contentPane.getChildren().addAll(createCurrencyBanner(), createLoadingIndicator());

        Task<List<PaymentTransaction>> task = new Task<>() {
            @Override
            protected List<PaymentTransaction> call() throws Exception {
                return paymentTransactionService.findByUserId(currentUser.getId());
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> displayTransactions(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> {
            logger.error("Failed to load transactions", task.getException());
            contentPane.getChildren().clear();
            contentPane.getChildren().addAll(createCurrencyBanner(), createEmptyState(I18n.get("finance.transactions.error")));
        }));

        AppThreadPool.execute(task);
    }

    private void displayTransactions(List<PaymentTransaction> transactions) {
        contentPane.getChildren().clear();
        contentPane.getChildren().add(createCurrencyBanner());

        if (transactions == null || transactions.isEmpty()) {
            contentPane.getChildren().add(createEmptyState(SvgIcons.ACTIVITY, "No transactions", "Transaction history will appear here."));
            return;
        }

        VBox list = new VBox(12);
        for (PaymentTransaction tx : transactions) {
            TLCard card = new TLCard();
            VBox content = new VBox(8);

            String title = tx.getReference() != null ? tx.getReference() : ("#" + tx.getId());
            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().addAll("text-base", "font-bold");

            TLBadge status = new TLBadge(tx.getStatus() != null ? tx.getStatus() : "PENDING", getPaymentBadgeVariant(tx.getStatus()));

            HBox header = new HBox(12, titleLabel, status);
            header.setAlignment(Pos.CENTER_LEFT);
            content.getChildren().add(header);

            content.getChildren().add(createDetailRow(I18n.get("finance.transactions.amount"),
                    formatWithPreferred(tx.getAmount(), tx.getCurrency())));
            content.getChildren().add(createDetailRow(I18n.get("finance.transactions.type"),
                    tx.getTransactionType() != null ? tx.getTransactionType() : "-"));
            content.getChildren().add(createDetailRow(I18n.get("finance.transactions.date"),
                    tx.getTransactionDate() != null ? tx.getTransactionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-"));

            if (tx.getNotes() != null && !tx.getNotes().isBlank()) {
                Label notes = new Label(tx.getNotes());
                notes.getStyleClass().addAll("text-2xs", "text-muted");
                notes.setWrapText(true);
                content.getChildren().add(notes);
            }

            card.setContent(content);
            list.getChildren().add(card);
        }

        ScrollPane scrollPane = new ScrollPane(list);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("bg-transparent");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        contentPane.getChildren().add(scrollPane);
    }

    // ==================== Balance Simulation Tab ====================

    private void showSimulationTab() {
        if (currentUser == null) return;

        contentPane.getChildren().clear();
        contentPane.getChildren().add(createCurrencyBanner());

        TLCard card = new TLCard();
        card.setHeader(I18n.get("finance.simulation.title"));

        VBox body = new VBox(12);

        TLSelect<Integer> monthsSelect = new TLSelect<>(I18n.get("finance.simulation.months"));
        monthsSelect.getItems().addAll(3, 6, 12, 24);
        monthsSelect.setValue(6);

        TLButton runBtn = new TLButton(I18n.get("finance.simulation.run"), TLButton.ButtonVariant.PRIMARY);

        VBox results = new VBox(10);
        results.getChildren().add(new TLLoadingState(I18n.get("common.loading")));

        runBtn.setOnAction(e -> loadSimulation(monthsSelect.getValue(), results));

        body.getChildren().addAll(monthsSelect, runBtn, new TLSeparator(), results);
        card.setContent(body);

        contentPane.getChildren().add(card);
        loadSimulation(monthsSelect.getValue(), results);
    }

    private void loadSimulation(Integer months, VBox results) {
        if (currentUser == null || results == null) return;
        int m = months != null ? months : 6;
        results.getChildren().clear();
        results.getChildren().add(new TLLoadingState(I18n.get("common.loading")));

        Task<List<BalanceSimulationService.BalanceProjection>> task = new Task<>() {
            @Override
            protected List<BalanceSimulationService.BalanceProjection> call() {
                // Ensure rates are reasonably up to date for conversions
                currencyApiService.updateExchangeRates();
                return balanceSimulationService.projectBalance(currentUser.getId(), m);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<BalanceSimulationService.BalanceProjection> projections = task.getValue();
            results.getChildren().clear();
            if (projections == null || projections.isEmpty()) {
                results.getChildren().add(createEmptyState(SvgIcons.TRENDING_UP, "No simulation data", "Run a simulation to see projected balances."));
                return;
            }

            for (BalanceSimulationService.BalanceProjection p : projections) {
                TLCard rowCard = new TLCard();
                VBox c = new VBox(6);

                Label month = new Label(p.getMonth().format(DateTimeFormatter.ofPattern("MMM yyyy")));
                month.getStyleClass().addAll("text-sm", "font-bold");

                c.getChildren().addAll(
                        month,
                        createDetailRow(I18n.get("finance.simulation.income"), userCurrencyService.formatAmount(p.getProjectedIncome(), userCurrencyService.getPreferredCurrency(currentUser.getId()))),
                        createDetailRow(I18n.get("finance.simulation.deductions"), userCurrencyService.formatAmount(p.getProjectedDeductions(), userCurrencyService.getPreferredCurrency(currentUser.getId()))),
                        createDetailRow(I18n.get("finance.simulation.balance"), userCurrencyService.formatAmount(p.getProjectedBalance(), userCurrencyService.getPreferredCurrency(currentUser.getId())))
                );

                rowCard.setContent(c);
                results.getChildren().add(rowCard);
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            logger.error("Failed to simulate balance", task.getException());
            results.getChildren().clear();
            results.getChildren().add(createEmptyState(I18n.get("finance.simulation.error")));
        }));

        AppThreadPool.execute(task);
    }

    // ==================== Currency Banner ====================

    private Node createCurrencyBanner() {
        if (currentUser == null) return new Region();

        String preferred = userCurrencyService.getPreferredCurrency(currentUser.getId());

        TLCard card = new TLCard();
        VBox content = new VBox(8);

        Label title = new Label(I18n.get("finance.currency.title"));
        title.getStyleClass().addAll("text-sm", "font-bold");
        Label value = new Label(I18n.get("finance.currency.current", preferred));
        value.getStyleClass().add("text-muted");

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        TLButton changeBtn = new TLButton(I18n.get("finance.currency.change"), TLButton.ButtonVariant.OUTLINE);
        TLButton refreshBtn = new TLButton(I18n.get("finance.currency.refresh_rates"), TLButton.ButtonVariant.OUTLINE);
        refreshBtn.setOnAction(e -> AppThreadPool.execute(() -> currencyApiService.forceUpdateExchangeRates()));
        changeBtn.setOnAction(e -> showCurrencyPicker());

        actions.getChildren().addAll(changeBtn, refreshBtn);
        content.getChildren().addAll(title, value, actions);
        card.setContent(content);
        return card;
    }

    private void showCurrencyPicker() {
        if (currentUser == null) return;
        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("finance.currency.change"));
        dialog.setDescription(I18n.get("finance.currency.change_desc"));

        TLSelect<String> currency = new TLSelect<>(I18n.get("finance.currency.label"));
        currency.getItems().addAll("TND", "EUR", "USD", "GBP");
        currency.setValue(userCurrencyService.getPreferredCurrency(currentUser.getId()));

        dialog.setContent(currency);
        dialog.addButton(ButtonType.CANCEL);
        dialog.addButton(ButtonType.OK);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            String cur = currency.getValue();
            userCurrencyService.setPreferredCurrency(currentUser.getId(), cur);
            TLToast.success(contentPane.getScene(), I18n.get("common.success"), I18n.get("finance.currency.saved"));
            showContractTab();
        });
    }

    private String formatWithPreferred(BigDecimal amount, String fromCurrency) {
        if (amount == null) return "-";
        String from = fromCurrency != null ? fromCurrency : UserCurrencyService.DEFAULT_CURRENCY;
        String preferred = currentUser != null ? userCurrencyService.getPreferredCurrency(currentUser.getId()) : from;
        String base = amount.toPlainString() + " " + from;
        if (preferred.equalsIgnoreCase(from) || currentUser == null) return base;
        double converted = userCurrencyService.convertToPreferred(currentUser.getId(), amount.doubleValue(), from);
        return base + " (≈ " + userCurrencyService.formatAmount(converted, preferred) + ")";
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

                AppThreadPool.execute(task);
            }
        });
    }

    // ==================== Helper Methods ====================

    private HBox createDetailRow(String label, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label labelNode = new Label(label + ":");
        labelNode.getStyleClass().addAll("font-bold", "text-muted");
        labelNode.setMinWidth(Region.USE_PREF_SIZE);

        Label valueNode = new Label(value);
        valueNode.setWrapText(true);

        row.getChildren().addAll(labelNode, valueNode);
        return row;
    }

    private TLEmptyState createEmptyState(String icon, String title, String description) {
        TLEmptyState empty = new TLEmptyState(icon, title, description);
        VBox.setVgrow(empty, Priority.ALWAYS);
        return empty;
    }

    private TLEmptyState createEmptyState(String message) {
        return createEmptyState(SvgIcons.DOLLAR_SIGN, message, "");
    }

    private TLLoadingState createLoadingIndicator() {
        return new TLLoadingState();
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

    // ==================== Reports Tab (from branch) ====================

    private void showReportsTab() {
        if (currentUser == null) return;

        contentPane.getChildren().clear();

        TLCard reportCard = new TLCard();
        reportCard.setHeader("Employee Financial Report");

        VBox body = new VBox(16);

        Label descLabel = new Label("Generate a comprehensive PDF report for your financial data including contracts, payslips, bonuses, and bank accounts.");
        descLabel.setWrapText(true);
        descLabel.getStyleClass().add("text-muted");

        TLButton generateBtn = new TLButton("Generate My Report", TLButton.ButtonVariant.PRIMARY);
        generateBtn.setOnAction(e -> generateCurrentUserReport());

        body.getChildren().addAll(descLabel, generateBtn);
        reportCard.setContent(body);

        // Tax Calculator Card
        TLCard taxCard = new TLCard();
        taxCard.setHeader("Tax Calculator");

        VBox taxBody = new VBox(12);
        Label taxDesc = new Label("Calculate CNSS, IRPP, and net salary from gross amount (Tunisian tax system).");
        taxDesc.setWrapText(true);
        taxDesc.getStyleClass().add("text-muted");

        TLTextField grossField = new TLTextField("Gross Salary (TND)", "Enter gross salary");
        Label taxResult = new Label();
        taxResult.setWrapText(true);

        TLButton calcBtn = new TLButton("Calculate", TLButton.ButtonVariant.OUTLINE);
        calcBtn.setOnAction(e -> {
            String grossText = grossField.getText();
            String error = ValidationHelper.validatePositiveNumber(grossText, "Gross salary");
            if (error != null) {
                taxResult.setText("❌ " + error);
                taxResult.setStyle("-fx-text-fill: #ef4444;");
                return;
            }
            double gross = Double.parseDouble(grossText.trim());
            double cnss = gross * 0.0918;
            double irpp = (gross - cnss) * 0.26;
            double totalDeduct = cnss + irpp;
            double net = gross - totalDeduct;
            double rate = (totalDeduct / gross) * 100;

            taxResult.setText(String.format(
                    "Brut: %,.2f TND  |  CNSS (9.18%%): %,.2f TND  |  IRPP: %,.2f TND\n" +
                    "Total retenu: %,.2f TND  |  Net à percevoir: %,.2f TND  |  Taux: %.1f%%",
                    gross, cnss, irpp, totalDeduct, net, rate));
            taxResult.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
        });

        taxBody.getChildren().addAll(taxDesc, grossField, calcBtn, taxResult);
        taxCard.setContent(taxBody);

        contentPane.getChildren().addAll(reportCard, taxCard);
    }

    /**
     * Generates a PDF report for the current user using FinanceDataService + PDFGenerator.
     */
    private void generateCurrentUserReport() {
        if (currentUser == null) return;

        int userId = currentUser.getId();
        String userName = currentUser.getFullName();

        // FileChooser MUST be shown on the JavaFX Application Thread
        Stage stage = (Stage) contentPane.getScene().getWindow();
        String safeName = (userName != null ? userName : "Employe")
                .replaceAll("[^a-zA-Z0-9_-]", "_");
        String defaultName = "Rapport_Financier_" + safeName + "_" + java.time.LocalDate.now() + ".pdf";

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Enregistrer le rapport financier");
        fileChooser.setInitialFileName(defaultName);
        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Rapports PDF (*.pdf)", "*.pdf"),
                new javafx.stage.FileChooser.ExtensionFilter("Fichiers HTML (*.html)", "*.html")
        );

        File chosenFile = fileChooser.showSaveDialog(stage);
        if (chosenFile == null) return;

        javafx.stage.FileChooser.ExtensionFilter selectedFilter = fileChooser.getSelectedExtensionFilter();
        final File targetFile = PDFGenerator.ensureExtension(chosenFile, selectedFilter);

        Task<File> task = new Task<>() {
            @Override
            protected File call() {
                String contractInfo = buildContractInfoHtml(userId);
                String bankInfo = buildBankInfoHtml(userId);
                String bonusInfo = buildBonusInfoHtml(userId);
                String payslipInfo = buildPayslipInfoHtml(userId);
                String summary = buildProfessionalSummaryWithAI(userId, userName);

                return PDFGenerator.generateToFile(targetFile, userId, userName,
                        contractInfo, bankInfo, bonusInfo, payslipInfo, summary);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            File pdf = task.getValue();
            if (pdf != null) {
                TLToast.success(contentPane.getScene(), "Report Generated",
                        "Saved to: " + pdf.getAbsolutePath());
            }
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            logger.error("Failed to generate report", task.getException());
            TLToast.error(contentPane.getScene(), "Error", "Failed to generate report.");
        }));

        AppThreadPool.execute(task);
    }

    // ==================== Report HTML Builders (from branch) ====================

    private static final String[] MONTHS_FR = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};

    private String buildContractInfoHtml(int empId) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"salary-grid\">");
        java.text.NumberFormat nf = java.text.NumberFormat.getIntegerInstance(Locale.FRANCE);
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
        StringBuilder sb = new StringBuilder();
        try {
            List<BonusRow> list = financeDataService.getBonusesByUserId(empId);
            if (list.isEmpty()) {
                return "<p class=\"report-field-value\" style=\"color:#6a7f96;\">Aucune prime.</p>";
            }
            double total = list.stream().mapToDouble(BonusRow::getAmount).sum();
            String label = list.size() == 1 && list.get(0).getReason() != null && !list.get(0).getReason().isEmpty()
                    ? capitalizeWords(escapeHtml(list.get(0).getReason()))
                    : (list.size() > 1 ? "Total des primes" : "Prime mensuelle");
            sb.append("<div class=\"bonus-badge\">");
            sb.append("<div class=\"report-field\"><span class=\"report-field-label\">Montant total :</span> <span class=\"b-amount\">")
                    .append(String.format(Locale.FRANCE, "%.0f", total))
                    .append(" <span class=\"b-currency\">TND</span></span></div>");
            sb.append("<div class=\"report-field\"><span class=\"report-field-label\">Désignation :</span> <span class=\"b-label\">")
                    .append(label).append("</span></div>");
            sb.append("</div>");
        } catch (Exception e) {
            sb.append("<p class=\"report-field-value\" style=\"color:#6a7f96;\">Erreur chargement primes.</p>");
        }
        return sb.toString();
    }

    private String buildPayslipInfoHtml(int empId) {
        StringBuilder sb = new StringBuilder();
        try {
            List<PayslipRow> userPayslips = financeDataService.getPayslipsByUserId(empId);
            if (userPayslips.isEmpty())
                return "<p class=\"report-field-value\" style=\"color:#6a7f96;\">Aucun bulletin de paie récent.</p>";

            java.text.NumberFormat nfDecimal = java.text.NumberFormat.getNumberInstance(Locale.FRANCE);
            nfDecimal.setMinimumFractionDigits(2);
            nfDecimal.setMaximumFractionDigits(2);

            for (PayslipRow p : userPayslips) {
                int m = p.getMonth(), y = p.getYear();
                String periodLabel = (m >= 1 && m <= 12) ? (MONTHS_FR[m - 1] + " " + y)
                        : (p.getPeriod() != null ? p.getPeriod() : "—");
                boolean paid = p.getStatus() != null
                        && (p.getStatus().equalsIgnoreCase("PAID") || p.getStatus().equalsIgnoreCase("PAYÉ"));
                String tag = paid ? "Payé" : "En attente";

                sb.append("<div class=\"payslip-row\"><table><tr>");
                sb.append("<td style=\"width:38%;\"><div class=\"payslip-col-label\">Période :</div><div class=\"payslip-col-value\">")
                        .append(escapeHtml(periodLabel)).append("</div></td>");
                sb.append("<td style=\"width:42%;\"><div class=\"payslip-col-label\">Net à payer :</div><div class=\"payslip-net\">")
                        .append(nfDecimal.format(p.getNet())).append(" <span class=\"payslip-net-currency\">TND</span></div></td>");
                sb.append("<td style=\"text-align:right;\"><div class=\"payslip-col-label\">Statut :</div><span class=\"tag\">")
                        .append(escapeHtml(tag)).append("</span></td>");
                sb.append("</tr></table></div>");
            }
        } catch (Exception e) {
            sb.append("<p class=\"report-field-value\" style=\"color:#6a7f96;\">Erreur chargement bulletins.</p>");
        }
        return sb.toString();
    }

    /**
     * Builds the professional summary: AI-generated if configured, else rule-based fallback.
     */
    private String buildProfessionalSummaryWithAI(int empId, String employeeName) {
        try {
            String rawData = buildRawDataForAISummary(empId, employeeName);
            FinanceChatbotAIService aiService = FinanceChatbotAIService.getInstance();
            if (aiService.isConfigured() && rawData != null && !rawData.isBlank()) {
                String aiSummary = aiService.generateReportSummary(rawData);
                if (aiSummary != null && !aiSummary.isBlank())
                    return aiSummary;
            }
        } catch (Exception e) {
            logger.warn("AI summary generation failed, falling back to rule-based", e);
        }
        return buildProfessionalSummary(empId, employeeName);
    }

    private String buildRawDataForAISummary(int empId, String employeeName) {
        String name = (employeeName != null && !employeeName.isBlank()) ? employeeName : "L'employé";
        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(Locale.FRANCE);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        try {
            List<ContractRow> contracts = financeDataService.getContractsByUserId(empId);
            List<PayslipRow> payslips = new java.util.ArrayList<>(financeDataService.getPayslipsByUserId(empId));
            payslips.sort(Comparator.comparingInt(PayslipRow::getYear).reversed()
                    .thenComparingInt(PayslipRow::getMonth).reversed());
            List<BonusRow> bonuses = financeDataService.getBonusesByUserId(empId);
            List<BankAccountRow> banks = financeDataService.getBankAccountsByUserId(empId);

            StringBuilder sb = new StringBuilder();
            sb.append("Données employé : ").append(name).append(".\n");

            ContractRow active = contracts.stream()
                    .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()) || "ACTIF".equalsIgnoreCase(c.getStatus()))
                    .findFirst().orElse(contracts.isEmpty() ? null : contracts.get(0));
            if (active != null) {
                sb.append("Contrat : poste ").append(active.getPosition() != null ? active.getPosition() : "—")
                        .append(", type ").append(active.getType() != null ? active.getType().toUpperCase() : "—")
                        .append(", salaire de base ").append(nf.format(active.getSalary())).append(" TND.\n");
            } else {
                sb.append("Contrat : aucun contrat actif.\n");
            }

            if (!payslips.isEmpty()) {
                sb.append("Bulletins de paie : ").append(payslips.size()).append(" émis.");
                PayslipRow last = payslips.get(0);
                String period = (last.getMonth() >= 1 && last.getMonth() <= 12)
                        ? (MONTHS_FR[last.getMonth() - 1] + " " + last.getYear()) : "";
                if (!period.isEmpty())
                    sb.append(" Dernier : ").append(period).append(", net à payer ").append(nf.format(last.getNet())).append(" TND.");
                sb.append("\n");
            } else {
                sb.append("Bulletins de paie : aucun.\n");
            }

            if (!bonuses.isEmpty()) {
                double totalBonus = bonuses.stream().mapToDouble(BonusRow::getAmount).sum();
                sb.append("Primes : ").append(bonuses.size()).append(" prime(s), total ").append(nf.format(totalBonus)).append(" TND.\n");
            } else {
                sb.append("Primes : aucune.\n");
            }
            sb.append("Comptes bancaires : ").append(banks.size()).append(" enregistré(s).");
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String buildProfessionalSummary(int empId, String employeeName) {
        String name = (employeeName != null && !employeeName.isBlank()) ? employeeName : "L'employé";
        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(Locale.FRANCE);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        try {
            List<ContractRow> contracts = financeDataService.getContractsByUserId(empId);
            List<PayslipRow> payslips = new java.util.ArrayList<>(financeDataService.getPayslipsByUserId(empId));
            payslips.sort(Comparator.comparingInt(PayslipRow::getYear).reversed()
                    .thenComparingInt(PayslipRow::getMonth).reversed());
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
                sb.append("Sur la période couverte, ").append(payslips.size()).append(" bulletin(s) de paie ont été émis");
                PayslipRow last = payslips.get(0);
                String period = (last.getMonth() >= 1 && last.getMonth() <= 12)
                        ? (MONTHS_FR[last.getMonth() - 1] + " " + last.getYear()) : "";
                if (!period.isEmpty())
                    sb.append(" ; le dernier (").append(period).append(") s'élève à ").append(nf.format(last.getNet())).append(" TND net");
                sb.append(". ");
            }

            if (!bonuses.isEmpty()) {
                double totalBonus = bonuses.stream().mapToDouble(BonusRow::getAmount).sum();
                sb.append(bonuses.size()).append(" prime(s) versée(s) (total : ").append(nf.format(totalBonus)).append(" TND). ");
            }

            if (banks.size() > 0)
                sb.append(banks.size()).append(" compte(s) bancaire(s) enregistré(s) pour le virement du salaire.");
            return sb.toString().trim();
        } catch (Exception e) {
            return name + " — données financières indisponibles.";
        }
    }

    // ==================== HTML Utility Methods ====================

    private static String capitalizeWords(String s) {
        if (s == null || s.isBlank()) return s == null ? "" : s.trim();
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
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
