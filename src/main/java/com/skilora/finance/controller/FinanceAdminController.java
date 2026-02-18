package com.skilora.finance.controller;

import com.skilora.framework.components.*;
import com.skilora.user.entity.*;
import com.skilora.finance.entity.*;
import com.skilora.finance.service.*;
import com.skilora.utils.AppThreadPool;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

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

    private User currentUser;

    private final ContractService contractService = ContractService.getInstance();
    private final PayslipService payslipService = PayslipService.getInstance();
    private final ExchangeRateService exchangeRateService = ExchangeRateService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        titleLabel.setText(I18n.get("finance.admin.title"));
        subtitleLabel.setText(I18n.get("finance.admin.subtitle"));
        createTabs();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        showContractsTab();
    }

    // ==================== Tab Navigation ====================

    private void createTabs() {
        tabBox.getChildren().clear();
        TLTabs tabs = new TLTabs();
        tabs.addTab("contracts", I18n.get("finance.admin.tab.contracts"), (javafx.scene.Node) null);
        tabs.addTab("payroll", I18n.get("finance.admin.tab.payroll"), (javafx.scene.Node) null);
        tabs.addTab("payments", I18n.get("finance.admin.tab.payments"), (javafx.scene.Node) null);
        tabs.addTab("rates", I18n.get("finance.admin.tab.rates"), (javafx.scene.Node) null);
        tabs.addTab("reports", I18n.get("finance.admin.tab.reports"), (javafx.scene.Node) null);
        tabs.setOnTabChanged(tabId -> {
            switch (tabId) {
                case "contracts" -> showContractsTab();
                case "payroll" -> showPayrollTab();
                case "payments" -> showPaymentsTab();
                case "rates" -> showRatesTab();
                case "reports" -> showReportsTab();
            }
        });
        tabBox.getChildren().add(tabs);
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

        // Header with Create button
        TLButton createBtn = new TLButton(I18n.get("finance.admin.contract.create"), TLButton.ButtonVariant.PRIMARY);
        createBtn.setOnAction(e -> showCreateContractDialog());
        HBox headerBox = new HBox(createBtn);
        headerBox.setAlignment(Pos.CENTER_RIGHT);
        contentPane.getChildren().add(headerBox);

        if (contracts == null || contracts.isEmpty()) {
            contentPane.getChildren().add(createEmptyState(I18n.get("finance.admin.contracts.empty")));
            return;
        }

        VBox contractList = new VBox(12);

        for (EmploymentContract contract : contracts) {
            TLCard card = new TLCard();

            VBox content = new VBox(8);
            content.setPadding(new Insets(16));

            // Employee name header
            Label nameLabel = new Label(contract.getUserName() != null
                    ? contract.getUserName() : I18n.get("finance.admin.contract.user") + " #" + contract.getUserId());
            nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

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
        scrollPane.setStyle("-fx-background-color: transparent;");
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
        form.setPadding(new Insets(16));
        form.getChildren().add(dialog.createFormSection(I18n.get("finance.admin.contract.employee_info"), userIdField));
        form.getChildren().add(dialog.createFormSection(I18n.get("finance.admin.contract.contract_details"), salaryField, typeSelect, currencySelect));
        dialog.setContent(form);

        dialog.addButton(ButtonType.CANCEL);
        dialog.addButton(ButtonType.OK);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                String userIdText = userIdField.getText() != null ? userIdField.getText().trim() : "";
                String salaryText = salaryField.getText() != null ? salaryField.getText().trim() : "";

                if (userIdText.isEmpty()) {
                    DialogUtils.showError(I18n.get("message.error"),
                        I18n.get("error.validation.required", I18n.get("finance.admin.contract.user_id")));
                    return;
                }

                if (salaryText.isEmpty()) {
                    DialogUtils.showError(I18n.get("message.error"),
                        I18n.get("error.validation.required", I18n.get("finance.contract.salary")));
                    return;
                }

                try {
                    int userId = Integer.parseInt(userIdText);
                    BigDecimal salary = new BigDecimal(salaryText);

                    if (salary.compareTo(BigDecimal.ZERO) <= 0) {
                        DialogUtils.showError(I18n.get("message.error"),
                            I18n.get("error.validation.positive_amount"));
                        return;
                    }

                    String type = typeSelect.getValue();
                    String currency = currencySelect.getValue();

                    createContract(userId, salary, type, currency);
                } catch (NumberFormatException ex) {
                    DialogUtils.showError(I18n.get("message.error"),
                        I18n.get("error.validation.number"));
                    logger.warn("Invalid input for contract creation: {}", ex.getMessage());
                }
            }
        });
    }

    private void createContract(int userId, BigDecimal salary, String type, String currency) {
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

        task.setOnSucceeded(e -> Platform.runLater(this::showContractsTab));

        task.setOnFailed(e -> {
            logger.error("Failed to create contract", task.getException());
        });

        AppThreadPool.execute(task);
    }

    // ==================== Payroll Generation Tab ====================

    private void showPayrollTab() {
        contentPane.getChildren().clear();

        VBox payrollForm = new VBox(16);
        payrollForm.setPadding(new Insets(16));

        Label formTitle = new Label(I18n.get("finance.admin.payroll.generate"));
        formTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

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
                    resultsArea.getChildren().add(createEmptyState(I18n.get("finance.admin.payroll.no_contracts")));
                    return;
                }

                Label infoLabel = new Label(I18n.get("finance.admin.payroll.generating") + " " +
                        contracts.size() + " " + I18n.get("finance.admin.payroll.employees"));
                infoLabel.setStyle("-fx-font-size: 14px;");
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
            contentPane.getChildren().add(createEmptyState(I18n.get("finance.admin.payments.no_contracts")));
            return;
        }

        Label headerLabel = new Label(I18n.get("finance.admin.payments.title"));
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
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
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        contentPane.getChildren().add(scrollPane);
    }

    private TLCard createPayslipPaymentCard(Payslip payslip, EmploymentContract contract) {
        TLCard card = new TLCard();

        VBox content = new VBox(8);
        content.setPadding(new Insets(16));

        String period = String.format("%02d/%d", payslip.getPeriodMonth(), payslip.getPeriodYear());
        Label titleLabel = new Label(
                (contract.getUserName() != null ? contract.getUserName() : "#" + payslip.getUserId())
                        + " - " + period);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        content.getChildren().add(titleLabel);
        content.getChildren().add(createDetailRow(
                I18n.get("finance.payslip.net"),
                payslip.getNetSalary() != null
                        ? payslip.getNetSalary().toPlainString() + " " + payslip.getCurrency()
                        : "-"));

        TLButton markPaidBtn = new TLButton(I18n.get("finance.admin.payments.mark_paid"), TLButton.ButtonVariant.SUCCESS);
        markPaidBtn.setOnAction(e -> markAsPaid(payslip.getId()));

        HBox btnBox = new HBox(markPaidBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(8, 0, 0, 0));
        content.getChildren().add(btnBox);

        card.setContent(content);
        return card;
    }

    private void markAsPaid(int payslipId) {
        DialogUtils.showConfirmation(
            I18n.get("finance.admin.mark_paid.confirm.title"),
            I18n.get("finance.admin.mark_paid.confirm.message")
        ).ifPresent(result -> {
            if (result == ButtonType.OK) {
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() throws Exception {
                        return payslipService.updatePaymentStatus(payslipId, "PAID");
                    }
                };

                task.setOnSucceeded(e -> {
                    Platform.runLater(() -> {
                        logger.info("Payslip {} marked as paid", payslipId);
                        showPaymentsTab();
                    });
                });

                task.setOnFailed(e -> {
                    logger.error("Failed to mark payslip {} as paid", payslipId, task.getException());
                });

                AppThreadPool.execute(task);
            }
        });
    }

    // ==================== Exchange Rates Tab ====================

    private void showRatesTab() {
        contentPane.getChildren().clear();
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

        Label headerLabel = new Label(I18n.get("finance.admin.rates.title"));
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        contentPane.getChildren().add(headerLabel);

        if (rates == null || rates.isEmpty()) {
            contentPane.getChildren().add(createEmptyState(I18n.get("finance.admin.rates.empty")));
            return;
        }

        VBox ratesList = new VBox(8);

        for (ExchangeRate rate : rates) {
            TLCard card = new TLCard();

            VBox content = new VBox(6);
            content.setPadding(new Insets(12));

            String pair = (rate.getFromCurrency() != null ? rate.getFromCurrency() : "?")
                    + " → " + (rate.getToCurrency() != null ? rate.getToCurrency() : "?");
            Label pairLabel = new Label(pair);
            pairLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

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
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        contentPane.getChildren().add(scrollPane);
    }

    // ==================== Reports Tab ====================

    private void showReportsTab() {
        contentPane.getChildren().clear();
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
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
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
        labelNode.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-muted-foreground;");
        labelNode.setMinWidth(Region.USE_PREF_SIZE);

        Label valueNode = new Label(value != null ? value : "-");
        valueNode.setWrapText(true);

        row.getChildren().addAll(labelNode, valueNode);
        return row;
    }

    private VBox createEmptyState(String message) {
        VBox emptyState = new VBox(12);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(48));

        Label label = new Label(message);
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-muted-foreground;");

        emptyState.getChildren().add(label);
        return emptyState;
    }

    private StackPane createLoadingIndicator() {
        return TLSpinner.createCentered(TLSpinner.Size.LG);
    }

    private TLCard createStatCard(String title, String value) {
        TLCard card = new TLCard();
        card.setPrefWidth(220);

        VBox content = new VBox(8);
        content.setPadding(new Insets(16));

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-muted-foreground;");

        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

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
}
