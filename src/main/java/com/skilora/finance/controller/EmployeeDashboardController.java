package com.skilora.finance.controller;

import com.skilora.finance.model.BankAccountRow;
import com.skilora.finance.model.BonusRow;
import com.skilora.finance.model.ContractRow;
import com.skilora.finance.model.PayslipRow;
import com.skilora.finance.service.FinanceService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

/**
 * EmployeeDashboardController
 *
 * Tableau de bord avancé et personnalisé pour chaque employé (rôle USER).
 * Affiche :
 * - KPI Cards : Salaire Net, Brut, Primes, CNSS du mois courant
 * - Graphique LineChart d'évolution du salaire net sur 12 mois
 * - Détail du contrat actif
 * - Jauge de répartition du salaire (Net / CNSS / IRPP)
 * - Tableau des bulletins de paie personnels
 * - Liste des primes reçues
 * - Informations du compte bancaire principal
 * - Récapitulatif annuel
 *
 * Usage : appeler initializeWithUser(userId, fullName) après le chargement
 * FXML.
 */
public class EmployeeDashboardController implements Initializable {

    // ===================== HEADER =====================
    @FXML
    private Label greetingLabel;
    @FXML
    private Label subGreetingLabel;
    @FXML
    private Label currentDateLabel;

    // ===================== KPI CARDS =====================
    @FXML
    private Label kpi_netSalary;
    @FXML
    private Label kpi_netSalaryPeriod;
    @FXML
    private Label kpi_grossSalary;
    @FXML
    private Label kpi_grossTrend;
    @FXML
    private Label kpi_totalBonuses;
    @FXML
    private Label kpi_bonusCount;
    @FXML
    private Label kpi_totalCnss;

    // ===================== SALAIRE CHART =====================
    @FXML
    private LineChart<String, Number> salaryChart;
    @FXML
    private CategoryAxis chartXAxis;
    @FXML
    private NumberAxis chartYAxis;
    @FXML
    private Label chartPeriodLabel;

    // ===================== CONTRAT ACTIF =====================
    @FXML
    private Label contract_position;
    @FXML
    private Label contract_type;
    @FXML
    private Label contract_salary;
    @FXML
    private Label contract_startDate;
    @FXML
    private Label contract_status;

    // ===================== RÉPARTITION =====================
    @FXML
    private ProgressBar bar_net;
    @FXML
    private ProgressBar bar_cnss;
    @FXML
    private ProgressBar bar_irpp;
    @FXML
    private Label pct_net;
    @FXML
    private Label pct_cnss;
    @FXML
    private Label pct_irpp;

    // ===================== PAYSLIP TABLE =====================
    @FXML
    private TableView<PayslipRow> payslipTable;
    @FXML
    private TableColumn<PayslipRow, String> col_period;
    @FXML
    private TableColumn<PayslipRow, Double> col_base;
    @FXML
    private TableColumn<PayslipRow, Double> col_gross;
    @FXML
    private TableColumn<PayslipRow, Double> col_cnss;
    @FXML
    private TableColumn<PayslipRow, Double> col_irpp;
    @FXML
    private TableColumn<PayslipRow, Double> col_net;
    @FXML
    private TableColumn<PayslipRow, String> col_status;
    @FXML
    private Label payslip_countLabel;

    // ===================== PRIMES =====================
    @FXML
    private VBox bonusListBox;

    // ===================== BANQUE =====================
    @FXML
    private Label bank_name;
    @FXML
    private Label bank_iban;
    @FXML
    private Label bank_currency;
    @FXML
    private Label bank_verified;

    // ===================== ANNUEL =====================
    @FXML
    private Label annualYearLabel;
    @FXML
    private Label annual_gross;
    @FXML
    private Label annual_net;
    @FXML
    private Label annual_cnss;
    @FXML
    private Label annual_irpp;
    @FXML
    private Label annual_bonuses;

    // ===================== STATE =====================
    private final FinanceService financeService = FinanceService.getInstance();
    private int currentUserId = -1;
    private String currentUserName = "Employé";

    // ===================== INIT =====================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupPayslipTable();
        setupChart();
        setCurrentDate();
        // Data will be loaded when initializeWithUser() is called
    }

    /**
     * Called from the parent layout to inject the logged-in user's context.
     * Must be called after the FXML is loaded.
     */
    public void initializeWithUser(int userId, String fullName) {
        this.currentUserId = userId;
        this.currentUserName = fullName != null ? fullName : "Employé";
        setupGreeting();
        loadAllData();
    }

    // ===================== SETUP =====================

    private void setupGreeting() {
        int hour = java.time.LocalTime.now().getHour();
        String salutation;
        if (hour >= 5 && hour < 12)
            salutation = "🌅 Bonjour";
        else if (hour >= 12 && hour < 18)
            salutation = "☀️ Bon après-midi";
        else
            salutation = "🌙 Bonsoir";

        String firstName = currentUserName.split(" ")[0];
        greetingLabel.setText(salutation + ", " + firstName + " !");
        subGreetingLabel.setText("Voici votre tableau de bord financier personnel");
    }

    private void setCurrentDate() {
        LocalDate today = LocalDate.now();
        String mois = today.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        currentDateLabel.setText(today.getDayOfMonth() + " " + mois + " " + today.getYear());
        annualYearLabel.setText(String.valueOf(today.getYear()));
    }

    private void setupPayslipTable() {
        // Configure columns
        col_period.setCellValueFactory(new PropertyValueFactory<>("period"));
        col_base.setCellValueFactory(new PropertyValueFactory<>("baseSalary"));
        col_gross.setCellValueFactory(new PropertyValueFactory<>("gross"));
        col_cnss.setCellValueFactory(new PropertyValueFactory<>("cnss"));
        col_irpp.setCellValueFactory(new PropertyValueFactory<>("irpp"));
        col_net.setCellValueFactory(new PropertyValueFactory<>("net"));
        col_status.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Format number columns
        formatCurrencyColumn(col_base);
        formatCurrencyColumn(col_gross);
        formatCurrencyColumn(col_cnss);
        formatCurrencyColumn(col_irpp);
        formatCurrencyColumn(col_net);

        // Style status column
        col_status.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(status);
                    String color, bg;
                    switch (status.toUpperCase()) {
                        case "PAID":
                        case "PAYÉ":
                            color = "#22c55e";
                            bg = "rgba(34,197,94,0.15)";
                            break;
                        case "PENDING":
                        case "EN ATTENTE":
                            color = "#f59e0b";
                            bg = "rgba(245,158,11,0.15)";
                            break;
                        default:
                            color = "#6b7280";
                            bg = "rgba(107,114,128,0.15)";
                            break;
                    }
                    badge.setStyle("-fx-text-fill: " + color + "; -fx-background-color: " + bg + ";"
                            + "-fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
    }

    private void setupChart() {
        salaryChart.setCreateSymbols(true);
        salaryChart.setAnimated(true);
        salaryChart.getStylesheets().add(
                getClass().getResource("/com/skilora/ui/styles/components.css") != null
                        ? "/com/skilora/ui/styles/components.css"
                        : "");

        // Chart styling
        salaryChart.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-plot-background-color: transparent;" +
                        "-fx-padding: 0;");
        chartYAxis.setForceZeroInRange(false);
        chartYAxis.setLabel("TND");
        chartXAxis.setLabel("");
    }

    // ===================== DATA LOADING =====================

    private void loadAllData() {
        if (currentUserId <= 0) {
            showNoDataState();
            return;
        }
        loadPayslips();
        loadContract();
        loadBonuses();
        loadBankAccount();
        buildSalaryChart();
    }

    /**
     * Load payslips for the current user and update all KPI cards.
     */
    private void loadPayslips() {
        try {
            List<PayslipRow> payslips = financeService.getPayslipsByUserId(currentUserId);

            // Sort by year DESC, month DESC
            payslips.sort((a, b) -> {
                if (b.getYear() != a.getYear())
                    return Integer.compare(b.getYear(), a.getYear());
                return Integer.compare(b.getMonth(), a.getMonth());
            });

            ObservableList<PayslipRow> data = FXCollections.observableArrayList(payslips);
            payslipTable.setItems(data);
            payslip_countLabel.setText(payslips.size() + " bulletin(s)");

            // KPI from the latest payslip
            if (!payslips.isEmpty()) {
                PayslipRow latest = payslips.get(0);
                kpi_netSalary.setText(formatAmount(latest.getNet()) + " TND");
                kpi_grossSalary.setText(formatAmount(latest.getGross()) + " TND");
                kpi_netSalaryPeriod.setText(getMonthName(latest.getMonth()) + " " + latest.getYear());
                kpi_grossTrend.setText("Base: " + formatAmount(latest.getBaseSalary()) + " + H.Sup: "
                        + formatAmount(latest.getOvertimeTotal()));

                // Update salary distribution bars
                double gross = latest.getGross();
                if (gross > 0) {
                    double netPct = latest.getNet() / gross;
                    double cnssPct = latest.getCnss() / gross;
                    double irppPct = latest.getIrpp() / gross;
                    bar_net.setProgress(Math.min(1.0, netPct));
                    bar_cnss.setProgress(Math.min(1.0, cnssPct));
                    bar_irpp.setProgress(Math.min(1.0, irppPct));
                    pct_net.setText(String.format("%.1f%%", netPct * 100));
                    pct_cnss.setText(String.format("%.1f%%", cnssPct * 100));
                    pct_irpp.setText(String.format("%.1f%%", irppPct * 100));
                }
            } else {
                kpi_netSalary.setText("— TND");
                kpi_grossSalary.setText("— TND");
            }

            // Annual totals (current year)
            int currentYear = LocalDate.now().getYear();
            double totalGross = 0, totalNet = 0, totalCnss = 0, totalIrpp = 0;
            for (PayslipRow p : payslips) {
                if (p.getYear() == currentYear) {
                    totalGross += p.getGross();
                    totalNet += p.getNet();
                    totalCnss += p.getCnss();
                    totalIrpp += p.getIrpp();
                }
            }
            kpi_totalCnss.setText(formatAmount(totalCnss) + " TND");
            annual_gross.setText(formatAmount(totalGross) + " TND");
            annual_net.setText(formatAmount(totalNet) + " TND");
            annual_cnss.setText(formatAmount(totalCnss) + " TND");
            annual_irpp.setText(formatAmount(totalIrpp) + " TND");

        } catch (Exception e) {
            System.err.println("[EmployeeDashboard] Error loading payslips: " + e.getMessage());
            kpi_netSalary.setText("— TND");
        }
    }

    /**
     * Build the salary evolution chart from payslip history.
     */
    private void buildSalaryChart() {
        try {
            List<PayslipRow> payslips = financeService.getPayslipsByUserId(currentUserId);
            salaryChart.getData().clear();

            // Sort ascending for chart
            payslips.sort((a, b) -> {
                if (a.getYear() != b.getYear())
                    return Integer.compare(a.getYear(), b.getYear());
                return Integer.compare(a.getMonth(), b.getMonth());
            });

            // Take last 12 months max
            List<PayslipRow> recent = payslips.size() > 12
                    ? payslips.subList(payslips.size() - 12, payslips.size())
                    : payslips;

            XYChart.Series<String, Number> netSeries = new XYChart.Series<>();
            XYChart.Series<String, Number> grossSeries = new XYChart.Series<>();
            netSeries.setName("💰 Salaire Net");
            grossSeries.setName("📊 Salaire Brut");

            for (PayslipRow p : recent) {
                String label = getMonthShort(p.getMonth()) + "/" + String.valueOf(p.getYear()).substring(2);
                netSeries.getData().add(new XYChart.Data<>(label, p.getNet()));
                grossSeries.getData().add(new XYChart.Data<>(label, p.getGross()));
            }

            java.util.List<XYChart.Series<String, Number>> seriesList = new java.util.ArrayList<>();
            seriesList.add(grossSeries);
            seriesList.add(netSeries);
            salaryChart.getData().addAll(seriesList);

            // Style chart series
            javafx.application.Platform.runLater(() -> {
                styleChartSeries(netSeries, "#00d4ff");
                styleChartSeries(grossSeries, "#a78bfa");
            });

            chartPeriodLabel.setText(recent.size() + " derniers bulletins");

        } catch (Exception e) {
            System.err.println("[EmployeeDashboard] Error building chart: " + e.getMessage());
        }
    }

    /**
     * Load the most recent active contract.
     */
    private void loadContract() {
        try {
            List<ContractRow> contracts = financeService.getContractsByUserId(currentUserId);
            // Find active contract first, else latest
            ContractRow active = null;
            for (ContractRow c : contracts) {
                if ("ACTIVE".equalsIgnoreCase(c.getStatus())) {
                    active = c;
                    break;
                }
            }
            if (active == null && !contracts.isEmpty()) {
                active = contracts.get(0);
            }

            if (active != null) {
                contract_position.setText(active.getPosition() != null ? active.getPosition() : "—");
                contract_type.setText(active.getType() != null ? active.getType() : "—");
                contract_salary.setText(formatAmount(active.getSalary()) + " TND / mois");
                contract_startDate.setText(active.getStartDate() != null ? active.getStartDate() : "—");
                String status = active.getStatus();
                contract_status.setText(status != null ? status.toUpperCase() : "ACTIF");
                if ("ACTIVE".equalsIgnoreCase(status)) {
                    contract_status.setStyle(contract_status.getStyle()
                            .replace("#f59e0b", "#22c55e")
                            .replace("rgba(245,158,11,0.15)", "rgba(34,197,94,0.15)"));
                }
            } else {
                contract_position.setText("Aucun contrat trouvé");
                contract_type.setText("—");
                contract_salary.setText("—");
                contract_startDate.setText("—");
                contract_status.setText("INACTIF");
            }
        } catch (Exception e) {
            System.err.println("[EmployeeDashboard] Error loading contract: " + e.getMessage());
        }
    }

    /**
     * Load bonuses and display them as colored cards.
     */
    private void loadBonuses() {
        try {
            List<BonusRow> bonuses = financeService.getBonusesByUserId(currentUserId);

            // Sort by date desc
            bonuses.sort((a, b) -> b.getDateAwarded().compareTo(a.getDateAwarded()));

            // Annual bonus total
            double totalBonusAmount = bonuses.stream().mapToDouble(BonusRow::getAmount).sum();
            kpi_totalBonuses.setText(formatAmount(totalBonusAmount) + " TND");
            kpi_bonusCount.setText(bonuses.size() + " prime(s) reçue(s)");
            annual_bonuses.setText(formatAmount(totalBonusAmount) + " TND");

            bonusListBox.getChildren().clear();

            if (bonuses.isEmpty()) {
                Label emptyLabel = new Label("Aucune prime enregistrée");
                emptyLabel.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 12px;");
                bonusListBox.getChildren().add(emptyLabel);
                return;
            }

            // Show last 5 bonuses
            List<BonusRow> recent = bonuses.size() > 5 ? bonuses.subList(0, 5) : bonuses;
            String[] bonusColors = { "#22c55e", "#34d399", "#6ee7b7", "#a7f3d0", "#d1fae5" };

            for (int i = 0; i < recent.size(); i++) {
                BonusRow b = recent.get(i);
                String color = bonusColors[i % bonusColors.length];

                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 10, 8, 10));
                row.setStyle("-fx-background-color: rgba(34,197,94,0.07); -fx-background-radius: 8;"
                        + "-fx-border-color: rgba(34,197,94,0.2); -fx-border-width: 1; -fx-border-radius: 8;");

                VBox texts = new VBox(2);
                Label amountLabel = new Label(formatAmount(b.getAmount()) + " TND");
                amountLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px; -fx-font-weight: bold;");
                Label reasonLabel = new Label(b.getReason() != null ? b.getReason() : "Prime");
                reasonLabel.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 11px;");
                texts.getChildren().addAll(amountLabel, reasonLabel);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label dateLabel = new Label(b.getDateAwarded());
                dateLabel.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 10px;");

                row.getChildren().addAll(texts, spacer, dateLabel);
                bonusListBox.getChildren().add(row);
            }

        } catch (Exception e) {
            System.err.println("[EmployeeDashboard] Error loading bonuses: " + e.getMessage());
            kpi_totalBonuses.setText("— TND");
        }
    }

    /**
     * Load the primary bank account info.
     */
    private void loadBankAccount() {
        try {
            List<BankAccountRow> accounts = financeService.getBankAccountsByUserId(currentUserId);

            BankAccountRow primary = null;
            for (BankAccountRow acc : accounts) {
                if (acc.getIsPrimary()) {
                    primary = acc;
                    break;
                }
            }
            if (primary == null && !accounts.isEmpty()) {
                primary = accounts.get(0);
            }

            if (primary != null) {
                bank_name.setText(primary.getBankName() != null ? primary.getBankName() : "—");
                // Mask IBAN: show first 4 and last 4 only
                String iban = primary.getIban() != null ? primary.getIban() : "";
                bank_iban.setText(maskIban(iban));
                bank_currency.setText(primary.getCurrency() != null ? primary.getCurrency() : "TND");
                if (primary.getIsVerified()) {
                    bank_verified.setText("✓ Vérifié");
                    bank_verified.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 11px; -fx-font-weight: bold;");
                } else {
                    bank_verified.setText("⚠ En attente de vérification");
                    bank_verified.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 11px; -fx-font-weight: bold;");
                }
            } else {
                bank_name.setText("Aucun compte enregistré");
                bank_iban.setText("—");
                bank_currency.setText("—");
                bank_verified.setText("—");
            }
        } catch (Exception e) {
            System.err.println("[EmployeeDashboard] Error loading bank account: " + e.getMessage());
        }
    }

    // ===================== HELPERS =====================

    private void showNoDataState() {
        greetingLabel.setText("👋 Tableau de bord");
        subGreetingLabel.setText("Aucun utilisateur connecté — données non disponibles");
    }

    private void formatCurrencyColumn(TableColumn<PayslipRow, Double> col) {
        col.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(formatAmount(value));
                }
            }
        });
    }

    private void styleChartSeries(XYChart.Series<String, Number> series, String color) {
        try {
            if (series.getNode() != null) {
                series.getNode().setStyle("-fx-stroke: " + color + "; -fx-stroke-width: 2.5px;");
            }
            for (XYChart.Data<String, Number> data : series.getData()) {
                if (data.getNode() != null) {
                    data.getNode().setStyle(
                            "-fx-background-color: " + color + ", white;" +
                                    "-fx-background-insets: 0, 2;" +
                                    "-fx-background-radius: 5px;");
                    // Tooltip on each data point
                    Tooltip tip = new Tooltip(
                            data.getXValue() + "\n" + formatAmount(data.getYValue().doubleValue()) + " TND");
                    Tooltip.install(data.getNode(), tip);
                }
            }
        } catch (Exception e) {
            // Chart style is best-effort
        }
    }

    private String formatAmount(double amount) {
        if (amount >= 1000) {
            return String.format("%,.2f", amount).replace(",", " ").replace(".", ",");
        }
        return String.format("%.2f", amount).replace(".", ",");
    }

    private String maskIban(String iban) {
        if (iban == null || iban.length() < 8)
            return iban;
        String start = iban.substring(0, 4);
        String end = iban.substring(iban.length() - 4);
        String masked = "•".repeat(iban.length() - 8);
        return start + " " + masked + " " + end;
    }

    private String getMonthName(int month) {
        String[] months = { "Jan", "Fév", "Mar", "Avr", "Mai", "Jun",
                "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc" };
        return (month >= 1 && month <= 12) ? months[month - 1] : String.valueOf(month);
    }

    private String getMonthShort(int month) {
        String[] months = { "Jan", "Fév", "Mar", "Avr", "Mai", "Jun",
                "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc" };
        return (month >= 1 && month <= 12) ? months[month - 1] : String.valueOf(month);
    }

    // ===================== ACTIONS =====================

    @FXML
    private void handleRefresh() {
        loadAllData();
        buildSalaryChart();
    }
}
