package com.skilora.finance.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import com.skilora.finance.model.*;
import com.skilora.finance.service.FinanceChatbotService;
import com.skilora.finance.service.FinanceService;
import com.skilora.finance.service.TaxCalculationService;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;

/**
 * EmployeurFinanceController
 *
 * Dashboard financier personnel pour l'employe connecte (role EMPLOYER).
 * Toutes les donnees sont filtrees par l'ID de l'utilisateur connecte.
 * Les donnees sont inserees par l'administrateur (FinanceView / role ADMIN).
 *
 * Flux de donnees :
 * Admin ajoute un bulletin → table payslips (user_id = id employe)
 * → cet ecran affiche uniquement les lignes ou user_id = currentUser.id
 */
public class EmployeurFinanceController implements Initializable {

    // ── HEADER ────────────────────────────────────────────
    @FXML
    private Label lbl_greeting; // "Bonsoir, Mohamed Ali !"
    @FXML
    private Label lbl_subtitle; // Nom complet + poste
    @FXML
    private Label lbl_date; // Date du jour
    @FXML
    private Label lbl_userId; // Pour debug (hidden en prod)

    // ── KPI CARDS ──────────────────────────────────────────
    @FXML
    private Label kpi_net; // Salaire Net (dernier bulletin)
    @FXML
    private Label kpi_net_sub; // "Mars 2025"
    @FXML
    private Label kpi_gross; // Salaire Brut
    @FXML
    private Label kpi_gross_sub; // "Base + Heures sup."
    @FXML
    private Label kpi_bonuses; // Total primes cumulees
    @FXML
    private Label kpi_bonuses_sub; // "X prime(s)"
    @FXML
    private Label kpi_cnss; // CNSS du dernier bulletin
    @FXML
    private Label kpi_irpp; // IRPP du dernier bulletin

    // ── GRAPHIQUE EVOLUTION ────────────────────────────────
    @FXML
    private LineChart<String, Number> salaryChart;
    @FXML
    private CategoryAxis chartXAxis;
    @FXML
    private NumberAxis chartYAxis;
    @FXML
    private Label lbl_chart_info;

    // ── CARTE CONTRAT ACTIF ────────────────────────────────
    @FXML
    private Label lbl_cont_name; // Nom employe dans la carte
    @FXML
    private Label lbl_cont_position; // Poste
    @FXML
    private Label lbl_cont_type; // CDI / CDD
    @FXML
    private Label lbl_cont_salary; // Salaire contractuel
    @FXML
    private Label lbl_cont_start; // Date debut
    @FXML
    private Label lbl_cont_status; // Badge ACTIF / EXPIRE

    // ── BARRES REPARTITION ─────────────────────────────────
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

    @FXML
    private javafx.scene.layout.HBox employeeCardContainer;
    @FXML
    private javafx.scene.layout.HBox teamStatsContainer;
    @FXML
    private TextField txt_search_employee;

    // ── Assistant Ma Paie (Chatbot) ─────────────────────────────────────
    @FXML
    private javafx.scene.control.Label lblChatbotTitle;
    @FXML
    private javafx.scene.control.ScrollPane chatScrollPane;
    @FXML
    private VBox chatMessageContainer;
    @FXML
    private TextField chatQuestionField;

    private List<EmployeeSummaryRow> allEmployees = new ArrayList<>();

    // ── TABLEAU BULLETINS ──────────────────────────────────
    @FXML
    private TableView<PayslipRow> tbl_payslips;
    @FXML
    private TableColumn<PayslipRow, String> col_ps_period;
    @FXML
    private TableColumn<PayslipRow, Double> col_ps_base;
    @FXML
    private TableColumn<PayslipRow, Double> col_ps_overtime;
    @FXML
    private TableColumn<PayslipRow, Double> col_ps_bonus;
    @FXML
    private TableColumn<PayslipRow, Double> col_ps_gross;
    @FXML
    private TableColumn<PayslipRow, Double> col_ps_cnss;
    @FXML
    private TableColumn<PayslipRow, Double> col_ps_irpp;
    @FXML
    private TableColumn<PayslipRow, Double> col_ps_net;
    @FXML
    private TableColumn<PayslipRow, String> col_ps_status;
    @FXML
    private Label lbl_payslip_count;

    // ── DETAIL FISCAL ─────────────────────────────────────
    @FXML
    private Label lbl_summary_net;
    @FXML
    private Label lbl_summary_cnss;
    @FXML
    private Label lbl_summary_irpp;
    @FXML
    private TextArea area_tax_detail;

    // -- Nouveaux composants dynamiques --
    @FXML
    private PieChart pie_tax_breakdown;
    @FXML
    private Label lbl_tax_bracket;
    @FXML
    private Label lbl_tax_recommendation;
    @FXML
    private VBox vbx_tax_analysis;

    // ── TABLEAU PRIMES ─────────────────────────────────────
    @FXML
    private TableView<BonusRow> tbl_bonuses;
    @FXML
    private TableColumn<BonusRow, Double> col_bon_amount;
    @FXML
    private TableColumn<BonusRow, String> col_bon_reason;
    @FXML
    private TableColumn<BonusRow, String> col_bon_date;
    @FXML
    private Label lbl_bonus_count;

    // ── TABLEAU COMPTES BANCAIRES ──────────────────────────
    @FXML
    private TableView<BankAccountRow> tbl_banks;
    @FXML
    private TableColumn<BankAccountRow, String> col_bank_name;
    @FXML
    private TableColumn<BankAccountRow, String> col_bank_iban;
    @FXML
    private TableColumn<BankAccountRow, String> col_bank_swift;
    @FXML
    private TableColumn<BankAccountRow, String> col_bank_currency;
    @FXML
    private TableColumn<BankAccountRow, Boolean> col_bank_primary;
    @FXML
    private TableColumn<BankAccountRow, Boolean> col_bank_verified;
    @FXML
    private Label lbl_bank_count;

    // ── TABLEAU CONTRATS ───────────────────────────────────
    @FXML
    private TableView<ContractRow> tbl_contracts;
    @FXML
    private TableColumn<ContractRow, String> col_cnt_company;
    @FXML
    private TableColumn<ContractRow, String> col_cnt_type;
    @FXML
    private TableColumn<ContractRow, String> col_cnt_position;
    @FXML
    private TableColumn<ContractRow, Double> col_cnt_salary;
    @FXML
    private TableColumn<ContractRow, String> col_cnt_start;
    @FXML
    private TableColumn<ContractRow, String> col_cnt_end;
    @FXML
    private TableColumn<ContractRow, String> col_cnt_status;
    @FXML
    private Label lbl_contract_count;

    // ── ETAT ───────────────────────────────────────────────
    private int employeeId = -1;
    private String employeeName = "Employe"; // vrai nom depuis la DB
    private final FinanceService financeService = FinanceService.getInstance();
    private final FinanceChatbotService chatbotService = FinanceChatbotService.getInstance();

    // ══════════════════════════════════════════════════════
    // INITIALISATION
    // ══════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupChart();
        setCurrentDate();
        loadEmployeeCards();
        setupSearch();
        try {
            showChatWelcome();
        } catch (Exception e) {
            System.err.println("[EmployeurFinance] showChatWelcome: " + e.getMessage());
        }
    }

    /**
     * Appele par MainView apres le chargement du FXML.
     * userId = ID de l'utilisateur connecte (currentUser.getId()).
     * nameHint = nom passe depuis la session (peut etre null/username).
     * On recupere toujours le vrai fullName depuis la DB.
     */
    public void setEmployeeId(int userId, String nameHint) {
        this.employeeId = userId;
        // Recupere le vrai nom depuis la base de donnees
        String dbName = financeService.getUserFullName(userId);
        this.employeeName = (dbName != null && !dbName.isBlank()) ? dbName : nameHint;
        setupHeader();
        loadAllData();
        loadEmployeeCards(); // Update highlights
    }

    // ══════════════════════════════════════════════════════
    // SETUP
    // ══════════════════════════════════════════════════════

    private void setupHeader() {
        // Salutation selon l'heure
        int hour = LocalTime.now().getHour();
        String salut = (hour >= 5 && hour < 12) ? "Bonjour"
                : (hour >= 12 && hour < 18) ? "Bon apres-midi"
                        : "Bonsoir";

        if (lbl_greeting != null)
            lbl_greeting.setText(salut + ", " + employeeName + " !");
        if (lbl_subtitle != null)
            lbl_subtitle.setText("Tableau de bord financier personnel — " + employeeName);
        if (lbl_userId != null)
            lbl_userId.setVisible(false); // cache l'ID en production
    }

    private void setCurrentDate() {
        LocalDate today = LocalDate.now();
        String mois = today.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        if (lbl_date != null)
            lbl_date.setText(today.getDayOfMonth() + " " + capitalize(mois) + " " + today.getYear());
    }

    private void setupSearch() {
        if (txt_search_employee != null) {
            txt_search_employee.textProperty().addListener((obs, oldVal, newVal) -> {
                filterEmployeeCards(newVal);
            });
        }
    }

    private void filterEmployeeCards(String filter) {
        if (employeeCardContainer == null)
            return;
        employeeCardContainer.getChildren().clear();
        String lowerFilter = filter.toLowerCase();

        for (EmployeeSummaryRow emp : allEmployees) {
            if (emp.getFullName().toLowerCase().contains(lowerFilter) ||
                    emp.getPosition().toLowerCase().contains(lowerFilter)) {
                employeeCardContainer.getChildren().add(createEmployeeCard(emp));
            }
        }
    }

    private void loadEmployeeCards() {
        if (employeeCardContainer == null)
            return;
        try {
            allEmployees = financeService.getEmployeeSummaries();
            employeeCardContainer.getChildren().clear();

            // Calculate Global Stats
            double totalGross = 0;
            double totalNet = 0;
            int bankIssues = 0;

            for (EmployeeSummaryRow emp : allEmployees) {
                VBox card = createEmployeeCard(emp);
                employeeCardContainer.getChildren().add(card);

                totalGross += emp.getCurrentSalary();
                totalNet += emp.getLastNetPay();
                if (!"Verifié".equalsIgnoreCase(emp.getBankStatus())) {
                    bankIssues++;
                }
            }

            updateTeamStats(allEmployees.size(), totalGross, totalNet, bankIssues);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateTeamStats(int count, double gross, double net, int alerts) {
        if (teamStatsContainer == null)
            return;
        teamStatsContainer.getChildren().clear();

        teamStatsContainer.getChildren().addAll(
                createStatMiniCard("Effectif Total", String.valueOf(count), "👥", "#3b82f6"),
                createStatMiniCard("Masse Salariale Brut", fmt(gross) + " T.", "💰", "#8b5cf6"),
                createStatMiniCard("Total Déboursé Net", fmt(net) + " T.", "💸", "#10b981"),
                createStatMiniCard("Alertes Bancaires", String.valueOf(alerts), "⚠️",
                        alerts > 0 ? "#ef4444" : "#22c55e"));
    }

    private HBox createStatMiniCard(String title, String value, String icon, String color) {
        HBox box = new HBox(12);
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        box.setPrefWidth(220);
        box.setStyle(
                "-fx-background-color: -fx-background; -fx-padding: 12; -fx-background-radius: 12; -fx-border-color: -fx-border; -fx-border-width: 1;");

        Label iconLbl = new Label(icon);
        iconLbl.setStyle(
                "-fx-font-size: 20px; -fx-min-width: 40px; -fx-min-height: 40px; -fx-alignment: center; -fx-background-color: "
                        + color + "22; -fx-text-fill: " + color + "; -fx-background-radius: 10;");

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
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setPrefSize(160, 110);
        card.setMinWidth(160);

        // Creative styling
        String baseStyle = "-fx-background-color: -fx-card; -fx-background-radius: 16; -fx-border-color: -fx-border; -fx-border-width: 1; -fx-border-radius: 16; -fx-cursor: hand; -fx-padding: 12;";
        String selectedStyle = "-fx-background-color: -fx-card; -fx-background-radius: 16; -fx-border-color: #3b82f6; -fx-border-width: 2; -fx-border-radius: 16; -fx-cursor: hand; -fx-padding: 12; -fx-effect: dropshadow(gaussian, rgba(59,130,246,0.15), 10, 0, 0, 4);";

        if (emp.getUserId() == this.employeeId) {
            card.setStyle(selectedStyle);
        } else {
            card.setStyle(baseStyle);
        }

        Label avatar = new Label(getInitials(emp.getFullName()));
        avatar.setStyle("-fx-background-color: " + getAvatarColor(emp.getUserId())
                + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-min-width: 36px; -fx-min-height: 36px; -fx-max-width: 36px; -fx-max-height: 36px; -fx-background-radius: 18; -fx-alignment: center;");

        Label name = new Label(emp.getFullName());
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-foreground; -fx-font-size: 13px;");
        name.setWrapText(false);
        name.setTooltip(new Tooltip(emp.getFullName()));

        Label pos = new Label(emp.getPosition());
        pos.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 11px;");
        pos.setTooltip(new Tooltip(emp.getPosition()));

        card.getChildren().addAll(avatar, name, pos);

        card.setOnMouseClicked(e -> {
            setEmployeeId(emp.getUserId(), emp.getFullName());
        });

        // Hover effects
        card.setOnMouseEntered(e -> {
            if (emp.getUserId() != this.employeeId) {
                card.setStyle(baseStyle
                        + "-fx-background-color: -fx-control-inner-background; -fx-border-color: -fx-muted-foreground;");
            }
        });
        card.setOnMouseExited(e -> {
            if (emp.getUserId() != this.employeeId) {
                card.setStyle(baseStyle);
            } else {
                card.setStyle(selectedStyle);
            }
        });

        return card;
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank())
            return "?";
        String[] parts = name.split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    private String getAvatarColor(int id) {
        String[] colors = { "#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#ec4899" };
        return colors[id % colors.length];
    }

    private void setupChart() {
        if (salaryChart == null)
            return;
        salaryChart.setAnimated(true);
        salaryChart.setCreateSymbols(true);
        salaryChart.setStyle("-fx-background-color: transparent;");
        if (chartYAxis != null) {
            chartYAxis.setForceZeroInRange(false);
            chartYAxis.setLabel("TND");
        }
        if (chartXAxis != null)
            chartXAxis.setLabel("");
    }

    private void setupTableColumns() {

        // ── Bulletins de paie (ID colonne cachee)
        bind(col_ps_period, "period");
        bindMoney(col_ps_base, "baseSalary");
        bindMoney(col_ps_overtime, "overtimeTotal");
        bindMoney(col_ps_bonus, "bonuses");
        bindMoneyBold(col_ps_gross, "gross", "#c4b5fd");
        bindMoney(col_ps_cnss, "cnss");
        bindMoney(col_ps_irpp, "irpp");
        bindMoneyBold(col_ps_net, "net", "#6ee7b7");
        bindStatusPayslip(col_ps_status);

        // ── Primes (ID cache)
        bindMoneyBoldBonus(col_bon_amount, "amount", "#fbbf24");
        bind(col_bon_reason, "reason");
        bind(col_bon_date, "dateAwarded");

        // ── Comptes bancaires (ID cache)
        bind(col_bank_name, "bankName");
        bindIban(col_bank_iban);
        bind(col_bank_swift, "swift");
        bind(col_bank_currency, "currency");
        bindBool(col_bank_primary, "isPrimary", "Principal", "#22c55e");
        bindBool(col_bank_verified, "isVerified", "Verifie", "#7dd3fc");

        // ── Contrats (ID cache)
        bind(col_cnt_company, "companyName");
        bindContractType(col_cnt_type);
        bind(col_cnt_position, "position");
        bindMoneyTND(col_cnt_salary, "salary");
        bind(col_cnt_start, "startDate");
        bind(col_cnt_end, "endDate");
        bindStatusContract(col_cnt_status);
    }

    // ══════════════════════════════════════════════════════
    // CHARGEMENT DES DONNEES
    // ══════════════════════════════════════════════════════

    private void loadAllData() {
        if (employeeId <= 0) {
            if (lbl_greeting != null)
                lbl_greeting.setText("Aucun utilisateur connecte.");
            return;
        }
        loadPayslips();
        loadContracts();
        loadBonuses();
        loadBankAccounts();
        buildSalaryChart();
        buildTaxDetail();
    }

    // ── Bulletins ─────────────────────────────────────────
    private void loadPayslips() {
        try {
            List<PayslipRow> list = financeService.getPayslipsByUserId(employeeId);
            // Plus recent en premier
            list.sort(Comparator.comparingInt(PayslipRow::getYear).reversed()
                    .thenComparingInt(PayslipRow::getMonth).reversed());

            if (tbl_payslips != null)
                tbl_payslips.setItems(FXCollections.observableArrayList(list));
            if (lbl_payslip_count != null)
                lbl_payslip_count.setText(list.size() + " bulletin(s) de paie");

            if (!list.isEmpty()) {
                PayslipRow last = list.get(0);
                double gross = last.getGross();
                double net = last.getNet();
                double cnss = last.getCnss();
                double irpp = last.getIrpp();

                set(kpi_net, fmt(net) + " TND");
                set(kpi_net_sub, monthFr(last.getMonth()) + " " + last.getYear() + " (dernier)");
                set(kpi_gross, fmt(gross) + " TND");
                set(kpi_gross_sub, "Base: " + fmt(last.getBaseSalary()) + " + H.sup: " + fmt(last.getOvertimeTotal()));
                set(kpi_cnss, fmt(cnss) + " TND");
                set(kpi_irpp, fmt(irpp) + " TND");

                // Barres de repartition
                if (gross > 0.01) {
                    setBar(bar_net, pct_net, net / gross);
                    setBar(bar_cnss, pct_cnss, cnss / gross);
                    setBar(bar_irpp, pct_irpp, irpp / gross);
                }
            } else {
                set(kpi_net, "Aucun bulletin");
                set(kpi_gross, "Aucun bulletin");
                set(kpi_cnss, "--");
                set(kpi_irpp, "--");
            }
        } catch (Exception e) {
            System.err.println("[Dashboard] loadPayslips: " + e.getMessage());
        }
    }

    // ── Contrats ──────────────────────────────────────────
    private void loadContracts() {
        try {
            List<ContractRow> list = financeService.getContractsByUserId(employeeId);
            if (tbl_contracts != null)
                tbl_contracts.setItems(FXCollections.observableArrayList(list));
            if (lbl_contract_count != null)
                lbl_contract_count.setText(list.size() + " contrat(s)");

            // Carte contrat actif
            ContractRow active = list.stream()
                    .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()) || "ACTIF".equalsIgnoreCase(c.getStatus()))
                    .findFirst().orElse(list.isEmpty() ? null : list.get(0));

            if (active != null) {
                set(lbl_cont_name, employeeName);
                set(lbl_cont_position, nvl(active.getPosition()));
                set(lbl_cont_type, nvl(active.getType()));
                set(lbl_cont_salary, fmt(active.getSalary()) + " TND / mois");
                set(lbl_cont_start, nvl(active.getStartDate()));
                if (lbl_cont_status != null) {
                    String st = active.getStatus() != null ? active.getStatus().toUpperCase() : "ACTIF";
                    lbl_cont_status.setText(st);
                    boolean ok = st.contains("ACTIV") || st.contains("ACTIF");
                    lbl_cont_status.setStyle(ok
                            ? "-fx-text-fill:#22c55e;-fx-background-color:rgba(34,197,94,0.15);"
                                    + "-fx-padding:3 10;-fx-background-radius:10;-fx-font-weight:bold;"
                            : "-fx-text-fill:#f59e0b;-fx-background-color:rgba(245,158,11,0.15);"
                                    + "-fx-padding:3 10;-fx-background-radius:10;-fx-font-weight:bold;");
                }
            } else {
                set(lbl_cont_name, employeeName);
                set(lbl_cont_position, "Aucun contrat enregistre");
                set(lbl_cont_salary, "--");
                set(lbl_cont_start, "--");
                set(lbl_cont_type, "--");
            }
        } catch (Exception e) {
            System.err.println("[Dashboard] loadContracts: " + e.getMessage());
        }
    }

    // ── Primes ────────────────────────────────────────────
    private void loadBonuses() {
        try {
            List<BonusRow> list = financeService.getBonusesByUserId(employeeId);
            list.sort((a, b) -> b.getDateAwarded().compareTo(a.getDateAwarded()));
            if (tbl_bonuses != null)
                tbl_bonuses.setItems(FXCollections.observableArrayList(list));

            double total = list.stream().mapToDouble(BonusRow::getAmount).sum();
            set(kpi_bonuses, fmt(total) + " TND");
            set(kpi_bonuses_sub, list.size() + " prime(s) recue(s)");
            if (lbl_bonus_count != null)
                lbl_bonus_count.setText(list.size() + " prime(s)");
        } catch (Exception e) {
            System.err.println("[Dashboard] loadBonuses: " + e.getMessage());
        }
    }

    // ── Comptes bancaires ─────────────────────────────────
    private void loadBankAccounts() {
        try {
            List<BankAccountRow> list = financeService.getBankAccountsByUserId(employeeId);
            if (tbl_banks != null)
                tbl_banks.setItems(FXCollections.observableArrayList(list));
            if (lbl_bank_count != null)
                lbl_bank_count.setText(list.size() + " compte(s) enregistre(s)");
        } catch (Exception e) {
            System.err.println("[Dashboard] loadBankAccounts: " + e.getMessage());
        }
    }

    // ── Graphique ─────────────────────────────────────────
    private void buildSalaryChart() {
        if (salaryChart == null)
            return;
        try {
            List<PayslipRow> list = financeService.getPayslipsByUserId(employeeId);
            salaryChart.getData().clear();

            list.sort(Comparator.comparingInt(PayslipRow::getYear)
                    .thenComparingInt(PayslipRow::getMonth));

            List<PayslipRow> recent = list.size() > 12
                    ? list.subList(list.size() - 12, list.size())
                    : list;

            XYChart.Series<String, Number> seriesNet = new XYChart.Series<>();
            XYChart.Series<String, Number> seriesGross = new XYChart.Series<>();
            seriesNet.setName("Salaire Net");
            seriesGross.setName("Salaire Brut");

            for (PayslipRow p : recent) {
                String label = monthShort(p.getMonth()) + "/" + String.valueOf(p.getYear()).substring(2);
                seriesNet.getData().add(new XYChart.Data<>(label, p.getNet()));
                seriesGross.getData().add(new XYChart.Data<>(label, p.getGross()));
            }

            List<XYChart.Series<String, Number>> series = new ArrayList<>();
            series.add(seriesGross);
            series.add(seriesNet);
            salaryChart.getData().addAll(series);

            if (lbl_chart_info != null)
                lbl_chart_info.setText(recent.size() + " mois affiches");

            Platform.runLater(() -> {
                styleSeries(seriesNet, "#6ee7b7");
                styleSeries(seriesGross, "#c4b5fd");
            });

        } catch (Exception e) {
            System.err.println("[Dashboard] buildSalaryChart: " + e.getMessage());
        }
    }

    // ── Detail fiscal (TextArea) ──────────────────────────
    private void buildTaxDetail() {
        try {
            // Salaire de reference : contrat actif ou dernier bulletin
            List<ContractRow> contracts = financeService.getContractsByUserId(employeeId);
            double refSalary = contracts.stream()
                    .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()) || "ACTIF".equalsIgnoreCase(c.getStatus()))
                    .mapToDouble(ContractRow::getSalary).findFirst()
                    .orElse(contracts.isEmpty() ? 0 : contracts.get(0).getSalary());

            List<PayslipRow> payslips = financeService.getPayslipsByUserId(employeeId);
            payslips.sort(Comparator.comparingInt(PayslipRow::getYear).reversed()
                    .thenComparingInt(PayslipRow::getMonth).reversed());

            if (refSalary == 0 && !payslips.isEmpty())
                refSalary = payslips.get(0).getBaseSalary();

            List<BonusRow> bonuses = financeService.getBonusesByUserId(employeeId);
            double totalBonuses = bonuses.stream().mapToDouble(BonusRow::getAmount).sum();

            StringBuilder sb = new StringBuilder();
            sb.append("╔══════════════════════════════════════════════════════╗\n");
            sb.append("║         DETAIL DU CALCUL DE REMUNERATION             ║\n");
            sb.append("╠══════════════════════════════════════════════════════╣\n");
            sb.append("║  Employe  : ").append(padRight(employeeName, 40)).append("║\n");
            sb.append("║  Calcule  : ").append(padRight(LocalDate.now().toString(), 40)).append("║\n");
            sb.append("╚══════════════════════════════════════════════════════╝\n\n");

            if (refSalary > 0) {
                Map<String, BigDecimal> bd = TaxCalculationService.calculateCompleteSalary(
                        BigDecimal.valueOf(refSalary));

                double cnssEmp = bd.get("cnssEmployee").doubleValue();
                double cnssEmpl = bd.get("cnssEmployer").doubleValue();
                double irpp = bd.get("irpp").doubleValue();
                double totDed = bd.get("totalDeductions").doubleValue();
                double net = bd.get("netSalary").doubleValue();
                double rate = bd.get("effectiveTaxRate").doubleValue();

                set(lbl_summary_net, fmt(net) + " TND");
                set(lbl_summary_cnss, fmt(cnssEmp) + " TND");
                set(lbl_summary_irpp, fmt(irpp) + " TND");

                updatePieChart(net, cnssEmp, irpp);

                sb.append("REVENUS\n");
                sb.append("  Salaire brut de base     : ").append(fmtR(refSalary)).append(" TND\n");
                sb.append("  Total primes cumulees     : ").append(fmtR(totalBonuses)).append(" TND\n\n");

                sb.append("DEDUCTIONS (retenues sur le salaire)\n");
                sb.append("  CNSS part salarie (9.18%) : ").append(fmtR(cnssEmp)).append(" TND\n");
                sb.append("  CNSS part patronale (16.57%): ").append(fmtR(cnssEmpl))
                        .append(" TND  [a la charge de l'employeur]\n");
                sb.append("  IRPP (impot sur le revenu): ").append(fmtR(irpp)).append(" TND\n");
                sb.append("  Total deductions nettes   : ").append(fmtR(totDed)).append(" TND\n");
                sb.append("  Taux d'imposition effectif: ").append(String.format("%.2f", rate)).append(" %\n\n");

                Map<String, String> recs = TaxCalculationService.getOptimizationRecommendations(
                        BigDecimal.valueOf(refSalary));

                set(lbl_tax_bracket, recs.get("taxBracket"));
                set(lbl_tax_recommendation, recs.get("recommendation"));
                sb.append("ANALYSE FISCALE\n");
                sb.append("  Tranche IRPP    : ").append(recs.get("taxBracket")).append("\n");
                sb.append("  Recommandation  : ").append(recs.get("recommendation")).append("\n\n");
            } else {
                sb.append("  Aucun contrat actif trouve.\n");
                sb.append("  Contactez l'administrateur RH pour enregistrer votre contrat.\n\n");
            }

            if (!payslips.isEmpty()) {
                sb.append("HISTORIQUE DES BULLETINS DE PAIE\n");
                sb.append("  ┌────────────────┬──────────────┬──────────────┬──────────────┬────────────┐\n");
                sb.append("  │ Periode        │ Brut (TND)   │ CNSS (TND)   │ Net  (TND)   │ Statut     │\n");
                sb.append("  ├────────────────┼──────────────┼──────────────┼──────────────┼────────────┤\n");
                for (PayslipRow p : payslips) {
                    sb.append(String.format("  │ %-14s │ %12s │ %12s │ %12s │ %-10s │%n",
                            p.getPeriod(),
                            fmtR(p.getGross()),
                            fmtR(p.getCnss()),
                            fmtR(p.getNet()),
                            p.getStatus()));
                }
                sb.append("  └────────────────┴──────────────┴──────────────┴──────────────┴────────────┘\n");
            }

            if (area_tax_detail != null)
                area_tax_detail.setText(sb.toString());

        } catch (Exception e) {
            System.err.println("[Dashboard] buildTaxDetail: " + e.getMessage());
            if (area_tax_detail != null)
                area_tax_detail.setText("Erreur lors du calcul : " + e.getMessage());
        }
    }

    private void updatePieChart(double net, double cnss, double irpp) {
        if (pie_tax_breakdown == null)
            return;

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("Salaire Net", net),
                new PieChart.Data("CNSS (9.18%)", cnss),
                new PieChart.Data("IRPP", irpp));

        pie_tax_breakdown.setData(pieData);
        pie_tax_breakdown.setLabelsVisible(false);
        pie_tax_breakdown.setLegendVisible(true);
        pie_tax_breakdown.setStartAngle(90);

        // Styling slices
        Platform.runLater(() -> {
            for (PieChart.Data data : pieData) {
                String color = "#7dd3fc"; // Default Net
                if (data.getName().contains("CNSS"))
                    color = "#fbbf24";
                if (data.getName().contains("IRPP"))
                    color = "#f87171";
                data.getNode().setStyle("-fx-pie-color: " + color + ";");
            }
        });
    }

    // ══════════════════════════════════════════════════════
    // CELLULES DE TABLEAU (noms uniques — evite erasure clash)
    // ══════════════════════════════════════════════════════

    /**
     * Lie une colonne String generique (PayslipRow, BonusRow, ContractRow,
     * BankAccountRow)
     */
    @SuppressWarnings("unchecked")
    private <T> void bind(TableColumn<T, String> col, String prop) {
        if (col == null)
            return;
        col.setCellValueFactory(new PropertyValueFactory<>(prop));
    }

    /** Colonne Double formatee en TND pour PayslipRow */
    private void bindMoney(TableColumn<PayslipRow, Double> col, String prop) {
        if (col == null)
            return;
        col.setCellValueFactory(new PropertyValueFactory<>(prop));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : fmt(v) + " TND");
            }
        });
    }

    /** Colonne Double coloree en gras pour PayslipRow */
    private void bindMoneyBold(TableColumn<PayslipRow, Double> col, String prop, String color) {
        if (col == null)
            return;
        col.setCellValueFactory(new PropertyValueFactory<>(prop));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(fmt(v) + " TND");
                setStyle("-fx-text-fill:" + color + ";-fx-font-weight:bold;");
            }
        });
    }

    /**
     * Colonne Double coloree en gras pour BonusRow (nom distinct pour eviter
     * erasure)
     */
    private void bindMoneyBoldBonus(TableColumn<BonusRow, Double> col, String prop, String color) {
        if (col == null)
            return;
        col.setCellValueFactory(new PropertyValueFactory<>(prop));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(fmt(v) + " TND");
                setStyle("-fx-text-fill:" + color + ";-fx-font-weight:bold;");
            }
        });
    }

    /** Colonne Double + TND pour ContractRow */
    private void bindMoneyTND(TableColumn<ContractRow, Double> col, String prop) {
        if (col == null)
            return;
        col.setCellValueFactory(new PropertyValueFactory<>(prop));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setText("--");
                    return;
                }
                setText(fmt(v) + " TND");
                setStyle("-fx-text-fill:#6ee7b7;-fx-font-weight:bold;");
            }
        });
    }

    /** Badge de statut pour PayslipRow */
    private void bindStatusPayslip(TableColumn<PayslipRow, String> col) {
        if (col == null)
            return;
        col.setCellValueFactory(new PropertyValueFactory<>("status"));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                setGraphic(statusBadge(s));
                setText(null);
            }
        });
    }

    /** Badge de statut pour ContractRow */
    private void bindStatusContract(TableColumn<ContractRow, String> col) {
        if (col == null)
            return;
        col.setCellValueFactory(new PropertyValueFactory<>("status"));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                setGraphic(statusBadge(s));
                setText(null);
            }
        });
    }

    /** Badge de type contrat pour ContractRow */
    private void bindContractType(TableColumn<ContractRow, String> col) {
        if (col == null)
            return;
        col.setCellValueFactory(new PropertyValueFactory<>("type"));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) {
                    setText("--");
                    setStyle("");
                    return;
                }
                setText(t.toUpperCase());
                boolean cdi = t.toUpperCase().contains("CDI");
                setStyle("-fx-text-fill:" + (cdi ? "#6ee7b7" : "#fbbf24") + ";-fx-font-weight:bold;");
            }
        });
    }

    /** IBAN masque */
    private void bindIban(TableColumn<BankAccountRow, String> col) {
        if (col == null)
            return;
        col.setCellValueFactory(new PropertyValueFactory<>("iban"));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String iban, boolean empty) {
                super.updateItem(iban, empty);
                if (empty || iban == null || iban.isBlank()) {
                    setText("--");
                    return;
                }
                setText(iban.length() > 8
                        ? iban.substring(0, 4) + " **** " + iban.substring(iban.length() - 4)
                        : iban);
            }
        });
    }

    /** Boolean badge (Principal / Verifie) pour BankAccountRow */
    private void bindBool(TableColumn<BankAccountRow, Boolean> col,
            String prop, String trueLabel, String color) {
        if (col == null)
            return;
        col.setCellValueFactory(new PropertyValueFactory<>(prop));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                boolean yes = Boolean.TRUE.equals(v);
                Label lbl = new Label(yes ? "\u2713 " + trueLabel : "--");
                lbl.setStyle(yes
                        ? "-fx-text-fill:" + color + ";-fx-font-weight:bold;"
                        : "-fx-text-fill:gray;");
                setGraphic(lbl);
                setText(null);
            }
        });
    }

    // ══════════════════════════════════════════════════════
    // UTILITAIRES
    // ══════════════════════════════════════════════════════

    private Label statusBadge(String status) {
        String up = status.toUpperCase();
        String color, bg;
        if (up.contains("PAID") || up.contains("PAYE") || up.contains("ACTIF") || up.contains("ACTIVE")) {
            color = "#22c55e";
            bg = "rgba(34,197,94,0.15)";
        } else if (up.contains("PENDING") || up.contains("ATTENTE")) {
            color = "#f59e0b";
            bg = "rgba(245,158,11,0.15)";
        } else if (up.contains("EXPIR") || up.contains("CANCEL") || up.contains("ANNUL")) {
            color = "#ef4444";
            bg = "rgba(239,68,68,0.12)";
        } else {
            color = "#6b7280";
            bg = "rgba(107,114,128,0.12)";
        }
        Label badge = new Label(up);
        badge.setStyle("-fx-text-fill:" + color + ";-fx-background-color:" + bg
                + ";-fx-padding:3 10;-fx-background-radius:10;-fx-font-size:11px;-fx-font-weight:bold;");
        return badge;
    }

    private void styleSeries(XYChart.Series<String, Number> series, String color) {
        try {
            if (series.getNode() != null)
                series.getNode().setStyle("-fx-stroke:" + color + ";-fx-stroke-width:2.5px;");
            for (XYChart.Data<String, Number> d : series.getData()) {
                if (d.getNode() != null) {
                    d.getNode().setStyle("-fx-background-color:" + color + ",white;"
                            + "-fx-background-insets:0,2;-fx-background-radius:5px;");
                    Tooltip tt = new Tooltip(d.getXValue() + " : " + fmt(d.getYValue().doubleValue()) + " TND");
                    Tooltip.install(d.getNode(), tt);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void setBar(ProgressBar bar, Label pctLbl, double ratio) {
        double v = Math.max(0.0, Math.min(1.0, ratio));
        if (bar != null)
            bar.setProgress(v);
        if (pctLbl != null)
            pctLbl.setText(String.format("%.1f%%", v * 100));
    }

    private void set(Label lbl, String text) {
        if (lbl != null)
            lbl.setText(text);
    }

    private String fmt(double v) {
        // Force US locale to get fixed separators, then replace for French/Tunisian
        // style
        return String.format(java.util.Locale.US, "%,.2f", v)
                .replace(",", " ") // comma (thousands) -> space
                .replace(".", ","); // dot (decimal) -> comma
    }

    private String fmtR(double v) {
        return String.format("%12.2f", v);
    }

    private String nvl(String s) {
        return (s != null && !s.isBlank()) ? s : "--";
    }

    private String monthFr(int m) {
        String[] names = { "Janvier", "Fevrier", "Mars", "Avril", "Mai", "Juin",
                "Juillet", "Aout", "Septembre", "Octobre", "Novembre", "Decembre" };
        return (m >= 1 && m <= 12) ? names[m - 1] : String.valueOf(m);
    }

    private String monthShort(int m) {
        String[] s = { "Jan", "Fev", "Mar", "Avr", "Mai", "Jun",
                "Jul", "Aou", "Sep", "Oct", "Nov", "Dec" };
        return (m >= 1 && m <= 12) ? s[m - 1] : String.valueOf(m);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty())
            return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String padRight(String s, int len) {
        if (s == null)
            s = "";
        if (s.length() >= len)
            return s.substring(0, len);
        return s + " ".repeat(len - s.length());
    }

    // ── Assistant Ma Paie (Chatbot) ─────────────────────────────────────
    private FinanceChatbotService.FinanceChatContext buildChatContext() {
        String lastPeriod = null;
        String lastNet = null;
        try {
            List<PayslipRow> payslips = financeService.getPayslipsByUserId(employeeId);
            if (!payslips.isEmpty()) {
                payslips.sort(Comparator.comparingInt(PayslipRow::getYear).reversed()
                        .thenComparingInt(PayslipRow::getMonth).reversed());
                PayslipRow last = payslips.get(0);
                lastPeriod = monthFr(last.getMonth()) + " " + last.getYear();
                lastNet = fmt(last.getNet());
            }
        } catch (Exception ignored) { }

        Map<String, FinanceChatbotService.EmployeeSnapshot> byName = new HashMap<>();
        for (EmployeeSummaryRow emp : allEmployees) {
            String name = emp.getFullName();
            if (name == null || name.isBlank()) continue;
            String period = null;
            String net = emp.getLastNetPay() > 0 ? fmt(emp.getLastNetPay()) : null;
            try {
                List<PayslipRow> ps = financeService.getPayslipsByUserId(emp.getUserId());
                if (!ps.isEmpty()) {
                    ps.sort(Comparator.comparingInt(PayslipRow::getYear).reversed()
                            .thenComparingInt(PayslipRow::getMonth).reversed());
                    period = monthFr(ps.get(0).getMonth()) + " " + ps.get(0).getYear();
                    if (net == null) net = fmt(ps.get(0).getNet());
                }
            } catch (Exception ignored) { }
            String salary = emp.getCurrentSalary() > 0 ? fmt(emp.getCurrentSalary()) : null;
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

    private void showChatWelcome() {
        if (lblChatbotTitle != null && chatbotService.isUsingAI())
            lblChatbotTitle.setText("Assistant Ma Paie (IA)");
        if (chatMessageContainer != null) {
            String welcome = chatbotService.isUsingAI()
                    ? "Bonjour ! Je suis l'assistant IA. Posez-moi vos questions sur la paie : bulletins, salaire net/brut, CNSS, IRPP, primes, contrats, comptes bancaires."
                    : "Bonjour ! Je réponds uniquement aux questions sur votre paie : bulletins, salaire net/brut, CNSS, IRPP, primes, contrats, comptes bancaires. Posez votre question ci-dessous.";
            appendChatMessage("Assistant", welcome);
        }
    }

    @FXML
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
        try {
            String displayText = text.replace("**", "");
            String style = "Vous".equals(who)
                    ? "-fx-background-color: rgba(59,130,246,0.2); -fx-padding: 10 14; -fx-background-radius: 12; -fx-border-color: rgba(59,130,246,0.4); -fx-border-radius: 12; -fx-border-width: 1; -fx-text-fill: -fx-foreground; -fx-font-size: 13px;"
                    : "-fx-background-color: rgba(0,196,167,0.12); -fx-padding: 10 14; -fx-background-radius: 12; -fx-border-color: rgba(0,196,167,0.3); -fx-border-radius: 12; -fx-border-width: 1; -fx-text-fill: -fx-foreground; -fx-font-size: 13px;";
            Label lbl = new Label(displayText);
            lbl.setWrapText(true);
            lbl.setMinWidth(200);
            lbl.setPrefWidth(600);
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setStyle(style);
            if (chatScrollPane != null && chatScrollPane.getWidth() > 0)
                lbl.maxWidthProperty().bind(chatScrollPane.widthProperty().subtract(50));
            Label header = new Label(who + " :");
            header.setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-muted-foreground; -fx-font-size: 11px;");
            VBox box = new VBox(4, header, lbl);
            box.setStyle("-fx-alignment: TOP_LEFT;");
            chatMessageContainer.getChildren().add(box);
        } catch (Exception e) {
            System.err.println("[EmployeurFinance] appendChatMessage: " + e.getMessage());
        }
    }

    // ── Actions de rafraichissement ───────────────────────
    @FXML
    private void handleRefreshAll() {
        loadAllData();
    }

    @FXML
    private void handleRefreshPayslips() {
        loadPayslips();
        buildSalaryChart();
        buildTaxDetail();
    }

    @FXML
    private void handleRefreshBonuses() {
        loadBonuses();
    }

    @FXML
    private void handleRefreshBanks() {
        loadBankAccounts();
    }

    @FXML
    private void handleRefreshContracts() {
        loadContracts();
    }
}
