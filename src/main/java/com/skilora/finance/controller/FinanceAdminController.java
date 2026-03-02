package com.skilora.finance.controller;

import com.skilora.framework.components.*;
import com.skilora.user.entity.*;
import com.skilora.finance.entity.*;
import com.skilora.finance.service.*;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * FinanceAdminController - Admin/employer view for managing contracts, payroll, and payments.
 * Tab-based interface with full CRUD and payroll generation capabilities.
 */
public class FinanceAdminController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(FinanceAdminController.class);

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private HBox tabBox;
    @FXML private VBox contentPane;
    @FXML private TLButton headerActionBtn;

    private User currentUser;

    private final ContractService contractService = ContractService.getInstance();
    private final PayslipService payslipService = PayslipService.getInstance();
    private final ExchangeRateService exchangeRateService = ExchangeRateService.getInstance();
    private final CurrencyApiService currencyApiService = CurrencyApiService.getInstance();
    private final PaymentTransactionService paymentTransactionService = PaymentTransactionService.getInstance();
    private final BankAccountService bankAccountService = BankAccountService.getInstance();
    private final EscrowService escrowService = EscrowService.getInstance();
    private final TaxConfigurationService taxConfigurationService = TaxConfigurationService.getInstance();
    private final FinanceDataService financeDataService = FinanceDataService.getInstance();
    private final FinanceChatbotService chatbotService = FinanceChatbotService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        titleLabel.setText(I18n.get("finance.admin.title"));
        subtitleLabel.setText(I18n.get("finance.admin.subtitle"));
        createTabs();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user == null || (user.getRole() != com.skilora.user.enums.Role.ADMIN
                && user.getRole() != com.skilora.user.enums.Role.EMPLOYER)) {
            if (contentPane != null && contentPane.getScene() != null) {
                TLToast.error(contentPane.getScene(), I18n.get("errorpage.access_denied"),
                        I18n.get("errorpage.access_denied.desc"));
            }
            return;
        }
        showContractsTab();
    }

    // ==================== Tab Navigation ====================

    private void createTabs() {
        tabBox.getChildren().clear();
        TLTabs tabs = new TLTabs();
        tabs.addTab("contracts", I18n.get("finance.admin.tab.contracts"), (javafx.scene.Node) null);
        tabs.addTab("payroll", I18n.get("finance.admin.tab.payroll"), (javafx.scene.Node) null);
        tabs.addTab("payments", I18n.get("finance.admin.tab.payments"), (javafx.scene.Node) null);
        tabs.addTab("transactions", I18n.get("finance.admin.tab.transactions"), (javafx.scene.Node) null);
        tabs.addTab("escrow", I18n.get("finance.admin.tab.escrow"), (javafx.scene.Node) null);
        tabs.addTab("tax", I18n.get("finance.admin.tab.tax"), (javafx.scene.Node) null);
        tabs.addTab("rates", I18n.get("finance.admin.tab.rates"), (javafx.scene.Node) null);
        tabs.addTab("reports", I18n.get("finance.admin.tab.reports"), (javafx.scene.Node) null);
        tabs.addTab("team", "Team Overview", (javafx.scene.Node) null);
        tabs.addTab("chatbot", "Chatbot", (javafx.scene.Node) null);
        tabs.setOnTabChanged(tabId -> {
            switch (tabId) {
                case "contracts" -> showContractsTab();
                case "payroll" -> showPayrollTab();
                case "payments" -> showPaymentsTab();
                case "transactions" -> showTransactionsTab();
                case "escrow" -> showEscrowTab();
                case "tax" -> showTaxTab();
                case "rates" -> showRatesTab();
                case "reports" -> showReportsTab();
                case "team" -> showTeamTab();
                case "chatbot" -> showChatbotTab();
            }
        });
        tabBox.getChildren().add(tabs);
    }

    private void hideHeaderAction() {
        headerActionBtn.setVisible(false);
        headerActionBtn.setManaged(false);
    }

    // ==================== Contracts Management Tab ====================

    private void showContractsTab() {
        contentPane.getChildren().clear();
        contentPane.getChildren().add(createLoadingIndicator());

        Task<List<EmploymentContract>> task = new Task<>() {
            @Override
            protected List<EmploymentContract> call() throws Exception {
                return contractService.findByStatus("ACTIVE");
            }
        };

        task.setOnSucceeded(e -> {
            List<EmploymentContract> contracts = task.getValue();
            Platform.runLater(() -> displayContracts(contracts));
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load contracts", task.getException());
            Platform.runLater(() -> {
                contentPane.getChildren().clear();
                contentPane.getChildren().add(createEmptyState(I18n.get("finance.admin.contracts.error")));
            });
        });

        AppThreadPool.execute(task);
    }

    private void displayContracts(List<EmploymentContract> contracts) {
        contentPane.getChildren().clear();

        // Show Create button in header
        headerActionBtn.setText(I18n.get("finance.admin.contract.create"));
        headerActionBtn.setOnAction(e -> showCreateContractDialog());
        headerActionBtn.setVisible(true);
        headerActionBtn.setManaged(true);

        if (contracts == null || contracts.isEmpty()) {
            contentPane.getChildren().add(createEmptyState(SvgIcons.FILE_TEXT, "No contracts found", "Active employment contracts will appear here."));
            return;
        }

        VBox contractList = new VBox(12);

        for (EmploymentContract contract : contracts) {
            TLCard card = new TLCard();

            VBox content = new VBox(8);

            // Employee name header
            Label nameLabel = new Label(contract.getUserName() != null
                    ? contract.getUserName() : I18n.get("finance.admin.contract.user") + " #" + contract.getUserId());
            nameLabel.getStyleClass().addAll("text-base", "font-bold");

            TLBadge statusBadge = new TLBadge(
                    contract.getStatus() != null ? contract.getStatus() : "DRAFT",
                    getContractBadgeVariant(contract.getStatus()));

            HBox header = new HBox(12, nameLabel, statusBadge);
            header.setAlignment(Pos.CENTER_LEFT);

            content.getChildren().add(header);

            content.getChildren().add(createDetailRow(
                    I18n.get("finance.contract.type"), contract.getContractType()));

            content.getChildren().add(createDetailRow(
                    I18n.get("finance.contract.salary"),
                    contract.getSalaryBase() != null
                            ? contract.getSalaryBase().toPlainString() + " " + contract.getCurrency()
                            : "-"));

            content.getChildren().add(createDetailRow(
                    I18n.get("finance.contract.start_date"),
                    contract.getStartDate() != null
                            ? contract.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            : "-"));

            if (contract.getJobTitle() != null) {
                content.getChildren().add(createDetailRow(
                        I18n.get("finance.admin.contract.job"), contract.getJobTitle()));
            }

            card.setContent(content);
            contractList.getChildren().add(card);
        }

        ScrollPane scrollPane = new ScrollPane(contractList);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("bg-transparent");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        contentPane.getChildren().add(scrollPane);
    }

    private void showCreateContractDialog() {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("finance.admin.contract.create"));
        dialog.setDescription(I18n.get("finance.admin.contract.create.description"));

        TLTextField userIdField = new TLTextField(I18n.get("finance.admin.contract.user_id"), I18n.get("finance.admin.contract.user_id.placeholder"));
        TLTextField salaryField = new TLTextField(I18n.get("finance.contract.salary"), I18n.get("finance.contract.salary.placeholder"));

        TLSelect<String> typeSelect = new TLSelect<>(I18n.get("finance.contract.type"));
        typeSelect.getItems().addAll("CDI", "CDD", "SIVP", "STAGE");
        typeSelect.setValue("CDI");

        TLSelect<String> currencySelect = new TLSelect<>(I18n.get("finance.bank.currency"));
        currencySelect.getItems().addAll("TND", "EUR", "USD");
        currencySelect.setValue("TND");

        VBox form = new VBox(12);
        form.getChildren().add(dialog.createFormSection(I18n.get("finance.admin.contract.employee_info"), userIdField));
        form.getChildren().add(dialog.createFormSection(I18n.get("finance.admin.contract.contract_details"), salaryField, typeSelect, currencySelect));
        dialog.setContent(form);

        dialog.addButton(ButtonType.CANCEL);
        dialog.addButton(ButtonType.OK);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                boolean valid = true;
                String userIdText = userIdField.getText() != null ? userIdField.getText().trim() : "";
                String salaryText = salaryField.getText() != null ? salaryField.getText().trim() : "";

                if (userIdText.isEmpty()) {
                    userIdField.setError(I18n.get("error.validation.required", I18n.get("finance.admin.contract.user_id")));
                    valid = false;
                } else {
                    try { Integer.parseInt(userIdText); userIdField.clearValidation(); }
                    catch (NumberFormatException ex) { userIdField.setError(I18n.get("error.validation.number")); valid = false; }
                }

                if (salaryText.isEmpty()) {
                    salaryField.setError(I18n.get("error.validation.required", I18n.get("finance.contract.salary")));
                    valid = false;
                } else {
                    try {
                        BigDecimal salary = new BigDecimal(salaryText);
                        if (salary.compareTo(BigDecimal.ZERO) <= 0) {
                            salaryField.setError(I18n.get("error.validation.positive_amount"));
                            valid = false;
                        } else { salaryField.clearValidation(); }
                    } catch (NumberFormatException ex) { salaryField.setError(I18n.get("error.validation.number")); valid = false; }
                }

                if (!valid) event.consume();
            }
        );

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                int userId = Integer.parseInt(userIdField.getText().trim());
                BigDecimal salary = new BigDecimal(salaryField.getText().trim());
                String type = typeSelect.getValue();
                String currency = currencySelect.getValue();
                createContract(userId, salary, type, currency);
            }
        });
    }

    private void createContract(int userId, BigDecimal salary, String type, String currency) {
        // Disable content pane to prevent duplicate contract creation
        contentPane.setDisable(true);

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                EmploymentContract contract = new EmploymentContract(userId, salary, LocalDate.now());
                contract.setContractType(type);
                contract.setCurrency(currency);
                contract.setStatus("PENDING_SIGNATURE");
                if (currentUser != null) {
                    contract.setEmployerId(currentUser.getId());
                }
                return contractService.create(contract);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            contentPane.setDisable(false);
            showContractsTab();
        }));

        task.setOnFailed(e -> {
            logger.error("Failed to create contract", task.getException());
            Platform.runLater(() -> contentPane.setDisable(false));
        });

        AppThreadPool.execute(task);
    }

    // ==================== Payroll Generation Tab ====================

    private void showPayrollTab() {
        contentPane.getChildren().clear();
        hideHeaderAction();

        VBox payrollForm = new VBox(16);
        payrollForm.setPadding(new Insets(16));

        Label formTitle = new Label(I18n.get("finance.admin.payroll.generate"));
        formTitle.getStyleClass().addAll("text-lg", "font-bold");

        // Month selector
        TLSelect<Integer> monthSelect = new TLSelect<>(I18n.get("finance.admin.payroll.month"));
        for (int i = 1; i <= 12; i++) {
            monthSelect.getItems().add(i);
        }
        monthSelect.setValue(LocalDate.now().getMonthValue());

        // Year selector
        TLSelect<Integer> yearSelect = new TLSelect<>(I18n.get("finance.admin.payroll.year"));
        int currentYear = LocalDate.now().getYear();
        for (int y = currentYear - 2; y <= currentYear + 1; y++) {
            yearSelect.getItems().add(y);
        }
        yearSelect.setValue(currentYear);

        // Generate button
        TLButton generateBtn = new TLButton(I18n.get("finance.admin.payroll.generate_btn"), TLButton.ButtonVariant.PRIMARY);

        // Results area
        VBox resultsArea = new VBox(12);

        generateBtn.setOnAction(e -> {
            Integer month = monthSelect.getValue();
            Integer year = yearSelect.getValue();
            if (month != null && year != null) {
                generatePayroll(month, year, resultsArea);
            }
        });

        payrollForm.getChildren().addAll(formTitle, monthSelect, yearSelect, generateBtn, new TLSeparator(), resultsArea);
        contentPane.getChildren().add(payrollForm);
    }

    private void generatePayroll(int month, int year, VBox resultsArea) {
        resultsArea.getChildren().clear();
        resultsArea.getChildren().add(createLoadingIndicator());

        Task<List<EmploymentContract>> task = new Task<>() {
            @Override
            protected List<EmploymentContract> call() throws Exception {
                return contractService.findByStatus("ACTIVE");
            }
        };

        task.setOnSucceeded(e -> {
            List<EmploymentContract> contracts = task.getValue();
            Platform.runLater(() -> {
                resultsArea.getChildren().clear();

                if (contracts == null || contracts.isEmpty()) {
                    resultsArea.getChildren().add(createEmptyState(SvgIcons.CLIPBOARD, "No active contracts", "Create contracts to generate payroll."));
                    return;
                }

                Label infoLabel = new Label(I18n.get("finance.admin.payroll.generating") + " " +
                        contracts.size() + " " + I18n.get("finance.admin.payroll.employees"));
                infoLabel.getStyleClass().add("text-sm");
                resultsArea.getChildren().add(infoLabel);

                // Generate payslips for each active contract
                for (EmploymentContract contract : contracts) {
                    generatePayslipForContract(contract.getId(), month, year, resultsArea);
                }
            });
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load contracts for payroll", task.getException());
            Platform.runLater(() -> {
                resultsArea.getChildren().clear();
                resultsArea.getChildren().add(createEmptyState(I18n.get("finance.admin.payroll.error")));
            });
        });

        AppThreadPool.execute(task);
    }

    private void generatePayslipForContract(int contractId, int month, int year, VBox resultsArea) {
        Task<Payslip> task = new Task<>() {
            @Override
            protected Payslip call() {
                return payslipService.generatePayslip(contractId, month, year);
            }
        };

        task.setOnSucceeded(e -> {
            Payslip payslip = task.getValue();
            Platform.runLater(() -> {
                if (payslip != null) {
                    String msg = I18n.get("finance.admin.payroll.generated_for") +
                            " #" + contractId + " - " + I18n.get("finance.payslip.net") + ": " +
                            payslip.getNetSalary().toPlainString() + " " + payslip.getCurrency();
                    resultsArea.getChildren().add(new TLAlert(TLAlert.Variant.SUCCESS, I18n.get("message.success"), msg));
                } else {
                    String msg = I18n.get("finance.admin.payroll.failed_for") + " #" + contractId;
                    resultsArea.getChildren().add(new TLAlert(TLAlert.Variant.DESTRUCTIVE, I18n.get("message.error"), msg));
                }
            });
        });

        task.setOnFailed(e -> {
            logger.error("Failed to generate payslip for contract {}", contractId, task.getException());
        });

        AppThreadPool.execute(task);
    }

    // ==================== Payment Processing Tab ====================

    private void showPaymentsTab() {
        if (currentUser == null) return;

        contentPane.getChildren().clear();
        hideHeaderAction();
        contentPane.getChildren().add(createLoadingIndicator());

        // Load all PENDING payslips for employer's contracts
        Task<List<EmploymentContract>> contractTask = new Task<>() {
            @Override
            protected List<EmploymentContract> call() throws Exception {
                return contractService.findByEmployerId(currentUser.getId());
            }
        };

        contractTask.setOnSucceeded(e -> {
            List<EmploymentContract> contracts = contractTask.getValue();
            Platform.runLater(() -> loadPendingPayslips(contracts));
        });

        contractTask.setOnFailed(e -> {
            logger.error("Failed to load contracts for payments", contractTask.getException());
            Platform.runLater(() -> {
                contentPane.getChildren().clear();
                contentPane.getChildren().add(createEmptyState(I18n.get("finance.admin.payments.error")));
            });
        });

        AppThreadPool.execute(contractTask);
    }

    private void loadPendingPayslips(List<EmploymentContract> contracts) {
        contentPane.getChildren().clear();

        if (contracts == null || contracts.isEmpty()) {
            contentPane.getChildren().add(createEmptyState(SvgIcons.DOLLAR_SIGN, "No pending payments", "All payments have been processed."));
            return;
        }

        Label headerLabel = new Label(I18n.get("finance.admin.payments.title"));
        headerLabel.getStyleClass().addAll("text-lg", "font-bold");
        contentPane.getChildren().add(headerLabel);

        VBox payslipList = new VBox(12);

        for (EmploymentContract contract : contracts) {
            Task<List<Payslip>> task = new Task<>() {
                @Override
                protected List<Payslip> call() throws Exception {
                    return payslipService.findByContractId(contract.getId());
                }
            };

            task.setOnSucceeded(e -> {
                List<Payslip> payslips = task.getValue();
                Platform.runLater(() -> {
                    if (payslips != null) {
                        for (Payslip payslip : payslips) {
                            if ("PENDING".equals(payslip.getPaymentStatus())) {
                                payslipList.getChildren().add(createPayslipPaymentCard(payslip, contract));
                            }
                        }
                    }
                });
            });

            AppThreadPool.execute(task);
        }

        ScrollPane scrollPane = new ScrollPane(payslipList);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("bg-transparent");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        contentPane.getChildren().add(scrollPane);
    }

    private TLCard createPayslipPaymentCard(Payslip payslip, EmploymentContract contract) {
        TLCard card = new TLCard();

        VBox content = new VBox(8);

        String period = String.format("%02d/%d", payslip.getPeriodMonth(), payslip.getPeriodYear());
        Label titleLabel = new Label(
                (contract.getUserName() != null ? contract.getUserName() : "#" + payslip.getUserId())
                        + " - " + period);
        titleLabel.getStyleClass().addAll("text-base", "font-bold");

        content.getChildren().add(titleLabel);
        content.getChildren().add(createDetailRow(
                I18n.get("finance.payslip.net"),
                payslip.getNetSalary() != null
                        ? payslip.getNetSalary().toPlainString() + " " + payslip.getCurrency()
                        : "-"));

        TLButton markPaidBtn = new TLButton(I18n.get("finance.admin.payments.mark_paid"), TLButton.ButtonVariant.SUCCESS);
        markPaidBtn.setOnAction(e -> markAsPaid(payslip));

        HBox btnBox = new HBox(markPaidBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(8, 0, 0, 0));
        content.getChildren().add(btnBox);

        card.setContent(content);
        return card;
    }

    private void markAsPaid(Payslip payslip) {
        if (payslip == null) return;
        DialogUtils.showConfirmation(
            I18n.get("finance.admin.mark_paid.confirm.title"),
            I18n.get("finance.admin.mark_paid.confirm.message")
        ).ifPresent(result -> {
            if (result == ButtonType.OK) {
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() throws Exception {
                        boolean updated = payslipService.updatePaymentStatus(payslip.getId(), "PAID");
                        if (!updated) return false;

                        PaymentTransaction tx = new PaymentTransaction();
                        tx.setPayslipId(payslip.getId());
                        tx.setAmount(payslip.getNetSalary() != null ? payslip.getNetSalary() : BigDecimal.ZERO);
                        tx.setCurrency(payslip.getCurrency() != null ? payslip.getCurrency() : "TND");
                        tx.setTransactionType("SALARY");
                        tx.setStatus("PAID");

                        Integer toAccountId = null;
                        try {
                            List<BankAccount> accounts = bankAccountService.findByUserId(payslip.getUserId());
                            BankAccount primary = accounts.stream().filter(BankAccount::isPrimary).findFirst().orElse(null);
                            if (primary != null) toAccountId = primary.getId();
                        } catch (Exception e) {
                            logger.warn("Could not resolve bank account for user {}: {}", payslip.getUserId(), e.getMessage());
                        }
                        tx.setToAccountId(toAccountId);
                        tx.setNotes("Payslip " + String.format("%02d/%d", payslip.getPeriodMonth(), payslip.getPeriodYear()));

                        paymentTransactionService.create(tx);
                        return true;
                    }
                };

                task.setOnSucceeded(e -> {
                    Platform.runLater(() -> {
                        logger.info("Payslip {} marked as paid", payslip.getId());
                        showPaymentsTab();
                    });
                });

                task.setOnFailed(e -> {
                    logger.error("Failed to mark payslip {} as paid", payslip.getId(), task.getException());
                });

                AppThreadPool.execute(task);
            }
        });
    }

    // ==================== Transactions History Tab ====================

    private void showTransactionsTab() {
        if (currentUser == null) return;

        contentPane.getChildren().clear();
        hideHeaderAction();
        contentPane.getChildren().add(createLoadingIndicator());

        Task<List<PaymentTransaction>> task = new Task<>() {
            @Override
            protected List<PaymentTransaction> call() throws Exception {
                if (currentUser.getRole() == com.skilora.user.enums.Role.ADMIN) {
                    return paymentTransactionService.findRecent(200);
                }
                return paymentTransactionService.findByEmployerId(currentUser.getId());
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> displayTransactions(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> {
            logger.error("Failed to load payment transactions", task.getException());
            contentPane.getChildren().clear();
            contentPane.getChildren().add(createEmptyState(I18n.get("finance.admin.transactions.error")));
        }));

        AppThreadPool.execute(task);
    }

    private void displayTransactions(List<PaymentTransaction> txs) {
        contentPane.getChildren().clear();
        hideHeaderAction();

        if (txs == null || txs.isEmpty()) {
            contentPane.getChildren().add(createEmptyState(SvgIcons.ACTIVITY, "No transactions", "Transaction history will appear here."));
            return;
        }

        VBox list = new VBox(10);
        for (PaymentTransaction tx : txs) {
            TLCard card = new TLCard();
            VBox content = new VBox(8);

            Label ref = new Label(tx.getReference() != null ? tx.getReference() : ("#" + tx.getId()));
            ref.getStyleClass().addAll("text-base", "font-bold");

            TLBadge status = new TLBadge(
                    tx.getStatus() != null ? tx.getStatus() : "PENDING",
                    "PAID".equalsIgnoreCase(tx.getStatus()) ? TLBadge.Variant.SUCCESS : TLBadge.Variant.SECONDARY
            );

            HBox header = new HBox(12, ref, status);
            header.setAlignment(Pos.CENTER_LEFT);

            String amount = tx.getAmount() != null ? tx.getAmount().toPlainString() : "-";
            String currency = tx.getCurrency() != null ? tx.getCurrency() : "TND";
            content.getChildren().addAll(
                    header,
                    createDetailRow(I18n.get("finance.admin.transactions.amount"), amount + " " + currency),
                    createDetailRow(I18n.get("finance.admin.transactions.type"), tx.getTransactionType()),
                    createDetailRow(I18n.get("finance.admin.transactions.date"),
                            tx.getTransactionDate() != null ? tx.getTransactionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-")
            );

            if (tx.getNotes() != null && !tx.getNotes().isBlank()) {
                Label notes = new Label(tx.getNotes());
                notes.getStyleClass().addAll("text-2xs", "text-muted");
                notes.setWrapText(true);
                content.getChildren().add(notes);
            }

            card.setContent(content);
            list.getChildren().add(card);
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("bg-transparent");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        contentPane.getChildren().add(scroll);
    }

    // ==================== Escrow Admin Tab ====================

    private void showEscrowTab() {
        if (currentUser == null) return;
        contentPane.getChildren().clear();
        hideHeaderAction();
        contentPane.getChildren().add(createLoadingIndicator());

        Task<List<EscrowAccount>> task = new Task<>() {
            @Override
            protected List<EscrowAccount> call() throws Exception {
                return escrowService.findPendingForAdmin();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> displayEscrows(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> {
            logger.error("Failed to load escrows", task.getException());
            contentPane.getChildren().clear();
            contentPane.getChildren().add(createEmptyState(I18n.get("finance.admin.escrow.error")));
        }));

        AppThreadPool.execute(task);
    }

    private void displayEscrows(List<EscrowAccount> escrows) {
        contentPane.getChildren().clear();
        hideHeaderAction();

        if (escrows == null || escrows.isEmpty()) {
            contentPane.getChildren().add(createEmptyState(SvgIcons.SHIELD, "No escrow payments", "Active escrow payments will be listed here."));
            return;
        }

        VBox list = new VBox(12);
        for (EscrowAccount e : escrows) {
            TLCard card = new TLCard();
            VBox content = new VBox(10);
            content.setPadding(new Insets(14));

            Label title = new Label(I18n.get("finance.admin.escrow.hold") + " #" + e.getId());
            title.getStyleClass().addAll("text-base", "font-bold");

            String who = (e.getUserName() != null ? e.getUserName() : ("#" + e.getContractId()));
            Label whoLabel = new Label(who);
            whoLabel.getStyleClass().add("text-muted");

            content.getChildren().addAll(
                    title,
                    whoLabel,
                    createDetailRow(I18n.get("finance.admin.escrow.amount"), (e.getAmount() != null ? e.getAmount().toPlainString() : "0") + " " + (e.getCurrency() != null ? e.getCurrency() : "TND")),
                    createDetailRow(I18n.get("finance.admin.escrow.contract"), "#" + e.getContractId())
            );

            if (e.getDescription() != null && !e.getDescription().isBlank()) {
                Label desc = new Label(e.getDescription());
                desc.getStyleClass().addAll("text-2xs", "text-muted");
                desc.setWrapText(true);
                content.getChildren().add(desc);
            }

            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_LEFT);

            TLButton releaseBtn = new TLButton(I18n.get("finance.admin.escrow.release"), TLButton.ButtonVariant.SUCCESS);
            releaseBtn.setGraphic(SvgIcons.icon(SvgIcons.CHECK, 14));
            TLButton refundBtn = new TLButton(I18n.get("finance.admin.escrow.refund"), TLButton.ButtonVariant.OUTLINE);
            refundBtn.setGraphic(SvgIcons.icon(SvgIcons.REFRESH, 14));
            TLButton disputeBtn = new TLButton(I18n.get("finance.admin.escrow.dispute"), TLButton.ButtonVariant.DANGER);
            disputeBtn.setGraphic(SvgIcons.icon(SvgIcons.ALERT_TRIANGLE, 14));

            releaseBtn.setOnAction(ev -> resolveEscrow(e, "RELEASE"));
            refundBtn.setOnAction(ev -> resolveEscrow(e, "REFUND"));
            disputeBtn.setOnAction(ev -> resolveEscrow(e, "DISPUTE"));

            actions.getChildren().addAll(releaseBtn, refundBtn, disputeBtn);
            content.getChildren().add(actions);

            card.setContent(content);
            list.getChildren().add(card);
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("bg-transparent");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        contentPane.getChildren().add(scroll);
    }

    private void resolveEscrow(EscrowAccount escrow, String action) {
        if (escrow == null || currentUser == null) return;

        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("finance.admin.escrow.resolve_title"));
        dialog.setDescription(I18n.get("finance.admin.escrow.resolve_desc"));

        TLTextarea notes = new TLTextarea(I18n.get("finance.admin.escrow.notes"), I18n.get("finance.admin.escrow.notes.placeholder"));
        dialog.setContent(notes);
        dialog.addButton(ButtonType.CANCEL);
        dialog.addButton(ButtonType.OK);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            String noteText = notes.getText();
            AppThreadPool.execute(() -> {
                try {
                    boolean ok;
                    switch (action) {
                        case "RELEASE" -> ok = escrowService.release(escrow.getId(), currentUser.getId(), noteText);
                        case "REFUND" -> ok = escrowService.refund(escrow.getId(), currentUser.getId(), noteText);
                        case "DISPUTE" -> ok = escrowService.dispute(escrow.getId(), noteText);
                        default -> ok = false;
                    }
                    boolean finalOk = ok;
                    Platform.runLater(() -> {
                        if (finalOk) {
                            TLToast.success(contentPane.getScene(), I18n.get("common.success"), I18n.get("finance.admin.escrow.updated"));
                            showEscrowTab();
                        } else {
                            TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("finance.admin.escrow.update_failed"));
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("finance.admin.escrow.update_failed")));
                }
            });
        });
    }

    // ==================== Tax Configuration Tab ====================

    private void showTaxTab() {
        if (currentUser == null) return;
        contentPane.getChildren().clear();

        headerActionBtn.setText(I18n.get("finance.admin.tax.add"));
        headerActionBtn.setOnAction(e -> showAddTaxBracketDialog());
        headerActionBtn.setVisible(true);
        headerActionBtn.setManaged(true);

        contentPane.getChildren().add(createLoadingIndicator());

        Task<List<TaxConfiguration>> task = new Task<>() {
            @Override
            protected List<TaxConfiguration> call() throws Exception {
                return taxConfigurationService.findByCountryAndType("Tunisia", "IRPP");
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> displayTaxConfigs(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> {
            logger.error("Failed to load tax configurations", task.getException());
            contentPane.getChildren().clear();
            contentPane.getChildren().add(createEmptyState(I18n.get("finance.admin.tax.error")));
        }));

        AppThreadPool.execute(task);
    }

    private void displayTaxConfigs(List<TaxConfiguration> configs) {
        contentPane.getChildren().clear();

        if (configs == null || configs.isEmpty()) {
            contentPane.getChildren().add(createEmptyState(SvgIcons.FILE_TEXT, "No tax brackets", "Add tax brackets to configure payroll deductions."));
            return;
        }

        VBox list = new VBox(10);
        for (TaxConfiguration tc : configs) {
            TLCard card = new TLCard();
            VBox content = new VBox(8);

            String range = tc.getMinBracket() + " → " + (tc.getMaxBracket() != null ? tc.getMaxBracket() : "∞");
            Label title = new Label(range);
            title.getStyleClass().addAll("text-base", "font-bold");

            Label rate = new Label((tc.getRate() != null ? tc.getRate().toPlainString() : "0") + "%");
            rate.getStyleClass().add("text-muted");

            content.getChildren().addAll(title, rate);
            if (tc.getDescription() != null && !tc.getDescription().isBlank()) {
                Label d = new Label(tc.getDescription());
                d.getStyleClass().addAll("text-2xs", "text-muted");
                d.setWrapText(true);
                content.getChildren().add(d);
            }

            TLButton deleteBtn = new TLButton(I18n.get("common.delete"), TLButton.ButtonVariant.DANGER);
            deleteBtn.setGraphic(SvgIcons.icon(SvgIcons.TRASH, 14));
            deleteBtn.setOnAction(e -> deleteTaxConfig(tc.getId()));
            HBox actions = new HBox(deleteBtn);
            actions.setAlignment(Pos.CENTER_RIGHT);
            content.getChildren().add(actions);

            card.setContent(content);
            list.getChildren().add(card);
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("bg-transparent");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        contentPane.getChildren().add(scroll);
    }

    private void showAddTaxBracketDialog() {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setDialogTitle(I18n.get("finance.admin.tax.add"));
        dialog.setDescription(I18n.get("finance.admin.tax.add.desc"));

        TLTextField minField = new TLTextField(I18n.get("finance.admin.tax.min"), "0");
        TLTextField maxField = new TLTextField(I18n.get("finance.admin.tax.max"), "");
        TLTextField rateField = new TLTextField(I18n.get("finance.admin.tax.rate"), "0");
        TLTextField descField = new TLTextField(I18n.get("finance.admin.tax.desc"), "");

        VBox form = new VBox(12, minField, maxField, rateField, descField);
        dialog.setContent(form);
        dialog.addButton(ButtonType.CANCEL);
        dialog.addButton(ButtonType.OK);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                boolean valid = true;
                String minText = minField.getText() != null ? minField.getText().trim() : "";
                String rateText = rateField.getText() != null ? rateField.getText().trim() : "";

                if (minText.isEmpty()) {
                    minField.setError(I18n.get("error.validation.required", I18n.get("finance.admin.tax.min")));
                    valid = false;
                } else {
                    try { new BigDecimal(minText); minField.clearValidation(); }
                    catch (NumberFormatException ex) { minField.setError(I18n.get("finance.admin.tax.invalid")); valid = false; }
                }

                if (rateText.isEmpty()) {
                    rateField.setError(I18n.get("error.validation.required", I18n.get("finance.admin.tax.rate")));
                    valid = false;
                } else {
                    try { new BigDecimal(rateText); rateField.clearValidation(); }
                    catch (NumberFormatException ex) { rateField.setError(I18n.get("finance.admin.tax.invalid")); valid = false; }
                }

                String maxText = maxField.getText() != null ? maxField.getText().trim() : "";
                if (!maxText.isEmpty()) {
                    try { new BigDecimal(maxText); maxField.clearValidation(); }
                    catch (NumberFormatException ex) { maxField.setError(I18n.get("finance.admin.tax.invalid")); valid = false; }
                }

                if (!valid) event.consume();
            }
        );

        dialog.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            TaxConfiguration tc = new TaxConfiguration();
            tc.setCountry("Tunisia");
            tc.setTaxType("IRPP");
            tc.setMinBracket(new BigDecimal(minField.getText().trim()));
            String max = maxField.getText() != null ? maxField.getText().trim() : "";
            tc.setMaxBracket(max.isEmpty() ? null : new BigDecimal(max));
            tc.setRate(new BigDecimal(rateField.getText().trim()));
            tc.setDescription(descField.getText());
            tc.setActive(true);
            tc.setEffectiveDate(LocalDate.now());

            AppThreadPool.execute(() -> {
                try {
                    taxConfigurationService.create(tc);
                    Platform.runLater(() -> {
                        TLToast.success(contentPane.getScene(), I18n.get("common.success"), I18n.get("finance.admin.tax.saved"));
                        showTaxTab();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("finance.admin.tax.save_failed")));
                }
            });
        });
    }

    private void deleteTaxConfig(int id) {
        DialogUtils.showConfirmation(I18n.get("finance.admin.tax.delete.title"), I18n.get("finance.admin.tax.delete.desc"))
                .ifPresent(bt -> {
                    if (bt != ButtonType.OK) return;
                    AppThreadPool.execute(() -> {
                        try {
                            boolean ok = taxConfigurationService.delete(id);
                            Platform.runLater(() -> {
                                if (ok) {
                                    TLToast.success(contentPane.getScene(), I18n.get("common.success"), I18n.get("finance.admin.tax.deleted"));
                                    showTaxTab();
                                } else {
                                    TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("finance.admin.tax.delete_failed"));
                                }
                            });
                        } catch (Exception ex) {
                            Platform.runLater(() -> TLToast.error(contentPane.getScene(), I18n.get("common.error"), I18n.get("finance.admin.tax.delete_failed")));
                        }
                    });
                });
    }

    // ==================== Exchange Rates Tab ====================

    private void showRatesTab() {
        contentPane.getChildren().clear();
        hideHeaderAction();
        contentPane.getChildren().add(createLoadingIndicator());

        Task<List<ExchangeRate>> task = new Task<>() {
            @Override
            protected List<ExchangeRate> call() throws Exception {
                // Load TND to EUR and TND to USD rates for the last 30 days
                List<ExchangeRate> rates = exchangeRateService.getHistory("TND", "EUR", 30);
                List<ExchangeRate> usdRates = exchangeRateService.getHistory("TND", "USD", 30);
                rates.addAll(usdRates);
                return rates;
            }
        };

        task.setOnSucceeded(e -> {
            List<ExchangeRate> rates = task.getValue();
            Platform.runLater(() -> displayExchangeRates(rates));
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load exchange rates", task.getException());
            Platform.runLater(() -> {
                contentPane.getChildren().clear();
                contentPane.getChildren().add(createEmptyState(I18n.get("finance.admin.rates.error")));
            });
        });

        AppThreadPool.execute(task);
    }

    private void displayExchangeRates(List<ExchangeRate> rates) {
        contentPane.getChildren().clear();

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Label headerLabel = new Label(I18n.get("finance.admin.rates.title"));
        headerLabel.getStyleClass().addAll("text-lg", "font-bold");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        TLButton refreshRatesBtn = new TLButton(I18n.get("finance.admin.rates.refresh"), TLButton.ButtonVariant.OUTLINE);
        refreshRatesBtn.setGraphic(SvgIcons.icon(SvgIcons.REFRESH, 14));
        refreshRatesBtn.setOnAction(e -> {
            refreshRatesBtn.setDisable(true);
            AppThreadPool.execute(() -> {
                currencyApiService.forceUpdateExchangeRates();
                Platform.runLater(() -> {
                    refreshRatesBtn.setDisable(false);
                    showRatesTab();
                });
            });
        });
        headerRow.getChildren().addAll(headerLabel, spacer, refreshRatesBtn);
        contentPane.getChildren().add(headerRow);

        if (rates == null || rates.isEmpty()) {
            contentPane.getChildren().add(createEmptyState(SvgIcons.GLOBE, "No exchange rates", "Refresh to fetch the latest exchange rate data."));
            return;
        }

        VBox ratesList = new VBox(8);

        for (ExchangeRate rate : rates) {
            TLCard card = new TLCard();

            VBox content = new VBox(6);

            String pair = (rate.getFromCurrency() != null ? rate.getFromCurrency() : "?")
                    + " → " + (rate.getToCurrency() != null ? rate.getToCurrency() : "?");
            Label pairLabel = new Label(pair);
            pairLabel.getStyleClass().addAll("text-sm", "font-bold");

            content.getChildren().add(pairLabel);

            content.getChildren().add(createDetailRow(
                    I18n.get("finance.admin.rates.rate"),
                    rate.getRate() != null ? rate.getRate().toPlainString() : "-"));

            content.getChildren().add(createDetailRow(
                    I18n.get("finance.admin.rates.date"),
                    rate.getRateDate() != null
                            ? rate.getRateDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            : "-"));

            content.getChildren().add(createDetailRow(
                    I18n.get("finance.admin.rates.source"),
                    rate.getSource() != null ? rate.getSource() : "-"));

            card.setContent(content);
            ratesList.getChildren().add(card);
        }

        ScrollPane scrollPane = new ScrollPane(ratesList);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("bg-transparent");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        contentPane.getChildren().add(scrollPane);
    }

    // ==================== Reports Tab ====================

    private void showReportsTab() {
        contentPane.getChildren().clear();
        hideHeaderAction();
        contentPane.getChildren().add(createLoadingIndicator());

        Task<ReportData> task = new Task<>() {
            @Override
            protected ReportData call() throws Exception {
                List<EmploymentContract> activeContracts = contractService.findByStatus("ACTIVE");
                List<EmploymentContract> allContracts = contractService.findAll();

                BigDecimal totalPayroll = BigDecimal.ZERO;
                for (EmploymentContract c : activeContracts) {
                    if (c.getSalaryBase() != null) {
                        totalPayroll = totalPayroll.add(c.getSalaryBase());
                    }
                }

                return new ReportData(
                        activeContracts.size(),
                        allContracts.size(),
                        totalPayroll
                );
            }
        };

        task.setOnSucceeded(e -> {
            ReportData data = task.getValue();
            Platform.runLater(() -> displayReports(data));
        });

        task.setOnFailed(e -> {
            logger.error("Failed to load reports", task.getException());
            Platform.runLater(() -> {
                contentPane.getChildren().clear();
                contentPane.getChildren().add(createEmptyState(I18n.get("finance.admin.reports.error")));
            });
        });

        AppThreadPool.execute(task);
    }

    private void displayReports(ReportData data) {
        contentPane.getChildren().clear();

        Label headerLabel = new Label(I18n.get("finance.admin.reports.title"));
        headerLabel.getStyleClass().addAll("text-lg", "font-bold");
        contentPane.getChildren().add(headerLabel);

        FlowPane statsGrid = new FlowPane(16, 16);
        statsGrid.setPadding(new Insets(16, 0, 0, 0));

        // Active Contracts stat
        statsGrid.getChildren().add(createStatCard(
                I18n.get("finance.admin.reports.active_contracts"),
                String.valueOf(data.activeContractCount)));

        // Total Contracts stat
        statsGrid.getChildren().add(createStatCard(
                I18n.get("finance.admin.reports.total_contracts"),
                String.valueOf(data.totalContractCount)));

        // Total Payroll stat
        statsGrid.getChildren().add(createStatCard(
                I18n.get("finance.admin.reports.total_payroll"),
                data.totalPayroll.toPlainString() + " TND"));

        // Average Salary stat
        BigDecimal avgSalary = data.activeContractCount > 0
                ? data.totalPayroll.divide(BigDecimal.valueOf(data.activeContractCount), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        statsGrid.getChildren().add(createStatCard(
                I18n.get("finance.admin.reports.avg_salary"),
                avgSalary.toPlainString() + " TND"));

        contentPane.getChildren().add(statsGrid);
    }

    // ==================== Helper Methods ====================

    private HBox createDetailRow(String label, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label labelNode = new Label(label + ":");
        labelNode.getStyleClass().addAll("font-bold", "text-muted");
        labelNode.setMinWidth(Region.USE_PREF_SIZE);

        Label valueNode = new Label(value != null ? value : "-");
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

    private TLCard createStatCard(String title, String value) {
        TLCard card = new TLCard();
        card.setPrefWidth(220);

        VBox content = new VBox(8);

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().addAll("text-xs", "text-muted");

        Label valueLbl = new Label(value);
        valueLbl.getStyleClass().addAll("text-2xl", "font-bold");

        content.getChildren().addAll(titleLbl, valueLbl);
        card.setContent(content);
        return card;
    }

    private TLBadge.Variant getContractBadgeVariant(String status) {
        if (status == null) return TLBadge.Variant.DEFAULT;
        return switch (status) {
            case "ACTIVE" -> TLBadge.Variant.SUCCESS;
            case "PENDING_SIGNATURE", "DRAFT" -> TLBadge.Variant.SECONDARY;
            case "TERMINATED", "EXPIRED" -> TLBadge.Variant.DESTRUCTIVE;
            default -> TLBadge.Variant.DEFAULT;
        };
    }

    // ==================== Inner Classes ====================

    /**
     * Simple data holder for report statistics.
     */
    private record ReportData(int activeContractCount, int totalContractCount, BigDecimal totalPayroll) {}

    // ==================== Team Overview Tab (from branch) ====================

    private List<EmployeeSummaryRow> allEmployees = new ArrayList<>();

    private void showTeamTab() {
        contentPane.getChildren().clear();
        contentPane.getChildren().add(createLoadingIndicator());

        Task<List<EmployeeSummaryRow>> task = new Task<>() {
            @Override
            protected List<EmployeeSummaryRow> call() throws Exception {
                return financeDataService.getEmployeeSummaries();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            contentPane.getChildren().clear();
            allEmployees = task.getValue();
            if (allEmployees == null || allEmployees.isEmpty()) {
                contentPane.getChildren().add(createEmptyState(SvgIcons.DOLLAR_SIGN, "No employees found", "No employee data available."));
                return;
            }

            // Stats
            double totalGross = 0;
            double totalNet = 0;
            int bankIssues = 0;
            for (EmployeeSummaryRow emp : allEmployees) {
                totalGross += emp.getCurrentSalary();
                totalNet += emp.getLastNetPay();
                if (!"Verifié".equalsIgnoreCase(emp.getBankStatus())) {
                    bankIssues++;
                }
            }

            HBox statsRow = new HBox(12);
            statsRow.getChildren().addAll(
                    createStatMiniCard("Effectif Total", String.valueOf(allEmployees.size()), "👥", "#3b82f6"),
                    createStatMiniCard("Masse Salariale Brut", formatDouble(totalGross) + " T.", "💰", "#8b5cf6"),
                    createStatMiniCard("Total Déboursé Net", formatDouble(totalNet) + " T.", "💸", "#10b981"),
                    createStatMiniCard("Alertes Bancaires", String.valueOf(bankIssues), "⚠️", bankIssues > 0 ? "#ef4444" : "#22c55e")
            );

            // Employee cards
            FlowPane cardFlow = new FlowPane(12, 12);
            cardFlow.setPadding(new Insets(12));
            for (EmployeeSummaryRow emp : allEmployees) {
                cardFlow.getChildren().add(createEmployeeCard(emp));
            }

            ScrollPane scrollPane = new ScrollPane(cardFlow);
            scrollPane.setFitToWidth(true);
            scrollPane.getStyleClass().add("bg-transparent");
            VBox.setVgrow(scrollPane, Priority.ALWAYS);

            contentPane.getChildren().addAll(statsRow, scrollPane);
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            contentPane.getChildren().clear();
            contentPane.getChildren().add(createEmptyState("Failed to load team data"));
        }));

        AppThreadPool.execute(task);
    }

    private HBox createStatMiniCard(String title, String value, String icon, String color) {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefWidth(220);
        box.setStyle("-fx-background-color: -fx-background; -fx-padding: 12; " +
                "-fx-background-radius: 12; -fx-border-color: -fx-border; -fx-border-width: 1;");

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 20px; -fx-min-width: 40px; -fx-min-height: 40px; " +
                "-fx-alignment: center; -fx-background-color: " + color + "22; -fx-text-fill: " + color +
                "; -fx-background-radius: 10;");

        VBox texts = new VBox(2);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 11px;");
        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-foreground; -fx-font-size: 14px;");
        texts.getChildren().addAll(titleLbl, valueLbl);
        box.getChildren().addAll(iconLbl, texts);
        return box;
    }

    private VBox createEmployeeCard(EmployeeSummaryRow emp) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(160, 110);
        card.setMinWidth(160);

        String baseStyle = "-fx-background-color: -fx-card; -fx-background-radius: 16; " +
                "-fx-border-color: -fx-border; -fx-border-width: 1; -fx-border-radius: 16; -fx-cursor: hand; -fx-padding: 12;";

        card.setStyle(baseStyle);

        Label avatar = new Label(getInitials(emp.getFullName()));
        avatar.setStyle("-fx-background-color: " + getAvatarColor(emp.getUserId()) +
                "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; " +
                "-fx-min-width: 36px; -fx-min-height: 36px; -fx-max-width: 36px; -fx-max-height: 36px; " +
                "-fx-background-radius: 18; -fx-alignment: center;");

        Label name = new Label(emp.getFullName());
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-foreground; -fx-font-size: 13px;");
        name.setWrapText(false);
        name.setTooltip(new Tooltip(emp.getFullName()));

        Label pos = new Label(emp.getPosition());
        pos.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 11px;");
        pos.setTooltip(new Tooltip(emp.getPosition()));

        card.getChildren().addAll(avatar, name, pos);

        card.setOnMouseEntered(e -> card.setStyle(baseStyle +
                "-fx-background-color: -fx-control-inner-background; -fx-border-color: -fx-muted-foreground;"));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        return card;
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    private String getAvatarColor(int id) {
        String[] colors = {"#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#ec4899"};
        return colors[id % colors.length];
    }

    private String formatDouble(double v) {
        return String.format(Locale.FRANCE, "%,.0f", v);
    }

    // ==================== Chatbot Tab (from branch) ====================

    private VBox chatMessageContainer;
    private TextField chatQuestionField;
    private ScrollPane chatScrollPane;

    private void showChatbotTab() {
        contentPane.getChildren().clear();

        TLCard chatCard = new TLCard();
        chatCard.setHeader(chatbotService.isUsingAI() ? "Assistant Ma Paie (IA)" : "Assistant Ma Paie");

        VBox body = new VBox(12);
        body.setPrefHeight(500);

        // Chat messages area
        chatMessageContainer = new VBox(10);
        chatMessageContainer.setPadding(new Insets(10));

        chatScrollPane = new ScrollPane(chatMessageContainer);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.getStyleClass().add("bg-transparent");
        VBox.setVgrow(chatScrollPane, Priority.ALWAYS);

        // Welcome message
        String welcome = chatbotService.isUsingAI()
                ? "Bonjour ! Je suis l'assistant IA. Posez-moi vos questions sur la paie : bulletins, salaire net/brut, CNSS, IRPP, primes, contrats, comptes bancaires."
                : "Bonjour ! Je réponds uniquement aux questions sur votre paie : bulletins, salaire net/brut, CNSS, IRPP, primes, contrats, comptes bancaires.";
        appendChatMessage("Assistant", welcome);

        // Input area
        HBox inputBox = new HBox(8);
        inputBox.setAlignment(Pos.CENTER_LEFT);
        chatQuestionField = new TextField();
        chatQuestionField.setPromptText("Posez votre question...");
        chatQuestionField.setPrefWidth(400);
        HBox.setHgrow(chatQuestionField, Priority.ALWAYS);

        TLButton sendBtn = new TLButton("Envoyer", TLButton.ButtonVariant.PRIMARY);
        sendBtn.setOnAction(e -> handleChatSend());
        chatQuestionField.setOnAction(e -> handleChatSend());

        inputBox.getChildren().addAll(chatQuestionField, sendBtn);

        body.getChildren().addAll(chatScrollPane, inputBox);
        chatCard.setContent(body);

        contentPane.getChildren().add(chatCard);
    }

    private void handleChatSend() {
        if (chatQuestionField == null || chatMessageContainer == null) return;
        String question = chatQuestionField.getText();
        if (question == null || question.isBlank()) return;
        chatQuestionField.clear();

        appendChatMessage("Vous", question);

        FinanceChatbotService.FinanceChatContext context = buildChatContext();
        String answer = chatbotService.answer(question, context);
        appendChatMessage("Assistant", answer);

        if (chatScrollPane != null) {
            chatScrollPane.setVvalue(1.0);
        }
    }

    private void appendChatMessage(String who, String text) {
        if (chatMessageContainer == null || text == null) return;
        String displayText = text.replace("**", "");
        String style = "Vous".equals(who)
                ? "-fx-background-color: rgba(59,130,246,0.2); -fx-padding: 10 14; -fx-background-radius: 12; " +
                  "-fx-border-color: rgba(59,130,246,0.4); -fx-border-radius: 12; -fx-border-width: 1; " +
                  "-fx-text-fill: -fx-foreground; -fx-font-size: 13px;"
                : "-fx-background-color: rgba(0,196,167,0.12); -fx-padding: 10 14; -fx-background-radius: 12; " +
                  "-fx-border-color: rgba(0,196,167,0.3); -fx-border-radius: 12; -fx-border-width: 1; " +
                  "-fx-text-fill: -fx-foreground; -fx-font-size: 13px;";

        Label lbl = new Label(displayText);
        lbl.setWrapText(true);
        lbl.setMinWidth(200);
        lbl.setPrefWidth(600);
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setStyle(style);

        Label header = new Label(who + " :");
        header.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-muted-foreground; -fx-font-size: 11px;");

        VBox box = new VBox(4, header, lbl);
        box.setStyle("-fx-alignment: TOP_LEFT;");
        chatMessageContainer.getChildren().add(box);
    }

    private FinanceChatbotService.FinanceChatContext buildChatContext() {
        String lastPeriod = null;
        String lastNet = null;
        String employeeName = currentUser != null ? currentUser.getFullName() : null;
        int employeeId = currentUser != null ? currentUser.getId() : -1;

        try {
            List<PayslipRow> payslips = financeDataService.getPayslipsByUserId(employeeId);
            if (!payslips.isEmpty()) {
                payslips.sort(Comparator.comparingInt(PayslipRow::getYear).reversed()
                        .thenComparingInt(PayslipRow::getMonth).reversed());
                PayslipRow last = payslips.get(0);
                lastPeriod = monthFr(last.getMonth()) + " " + last.getYear();
                lastNet = formatDouble(last.getNet());
            }
        } catch (Exception ignored) {}

        Map<String, FinanceChatbotService.EmployeeSnapshot> byName = new HashMap<>();
        for (EmployeeSummaryRow emp : allEmployees) {
            String name = emp.getFullName();
            if (name == null || name.isBlank()) continue;
            String period = null;
            String net = emp.getLastNetPay() > 0 ? formatDouble(emp.getLastNetPay()) : null;
            try {
                List<PayslipRow> ps = financeDataService.getPayslipsByUserId(emp.getUserId());
                if (!ps.isEmpty()) {
                    ps.sort(Comparator.comparingInt(PayslipRow::getYear).reversed()
                            .thenComparingInt(PayslipRow::getMonth).reversed());
                    period = monthFr(ps.get(0).getMonth()) + " " + ps.get(0).getYear();
                    if (net == null) net = formatDouble(ps.get(0).getNet());
                }
            } catch (Exception ignored) {}
            String salary = emp.getCurrentSalary() > 0 ? formatDouble(emp.getCurrentSalary()) : null;
            String key = normalizeForChat(name);
            if (!key.isEmpty())
                byName.put(key, new FinanceChatbotService.EmployeeSnapshot(name, net, period, salary));
        }

        return new FinanceChatbotService.FinanceChatContext(employeeName, lastPeriod, lastNet, byName);
    }

    private static String normalizeForChat(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.FRENCH)
                .replaceAll("[àâä]", "a").replaceAll("[éèêë]", "e").replaceAll("[îï]", "i")
                .replaceAll("[ôö]", "o").replaceAll("[ùûü]", "u")
                .replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    private static final String[] MONTHS_FR = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};

    private String monthFr(int m) {
        return (m >= 1 && m <= 12) ? MONTHS_FR[m - 1] : "?";
    }
}
