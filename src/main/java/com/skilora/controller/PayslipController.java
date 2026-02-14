package com.skilora.controller;

import com.skilora.model.entity.Payslip;
import com.skilora.model.service.PayslipService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Payslip Controller
 * Manages payslip display, generation, and management.
 */
public class PayslipController {
    private static final Logger logger = LoggerFactory.getLogger(PayslipController.class);

    @FXML private TableView<Payslip> payslipTable;
    @FXML private TableColumn<Payslip, String> monthColumn;
    @FXML private TableColumn<Payslip, Double> grossColumn;
    @FXML private TableColumn<Payslip, Double> netColumn;
    @FXML private TableColumn<Payslip, Double> deductionsColumn;
    @FXML private TableColumn<Payslip, String> statusColumn;
    @FXML private TableColumn<Payslip, Void> actionColumn;

    @FXML private Label lastPayslipLabel;
    @FXML private Label averageSalaryLabel;
    @FXML private Label totalYearEarningsLabel;

    @FXML private ComboBox<Integer> yearFilter;
    @FXML private ComboBox<String> statusFilter;

    private int currentUserId;
    private ObservableList<Payslip> payslips = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        setupTable();
        setupColumns();
        setupFilters();
    }

    private void setupTable() {
        payslipTable.setItems(payslips);
        payslipTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }

    private void setupColumns() {
        monthColumn.setCellValueFactory(cellData -> {
            Payslip p = cellData.getValue();
            String monthName = java.time.YearMonth.of(p.getYear(), p.getMonth()).toString();
            return new javafx.beans.property.SimpleStringProperty(monthName);
        });

        grossColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getGrossSalary())
        );

        netColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getNetSalary())
        );

        deductionsColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getDeductions())
        );

        statusColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus())
        );

        actionColumn.setCellFactory(column -> new TableCell<Payslip, Void>() {
            private final Button downloadBtn = new Button("Download");
            private final Button viewBtn = new Button("View");

            {
                downloadBtn.setStyle("-fx-padding: 5; -fx-font-size: 11;");
                viewBtn.setStyle("-fx-padding: 5; -fx-font-size: 11;");
                downloadBtn.setOnAction(e -> downloadPayslip(getTableView().getItems().get(getIndex())));
                viewBtn.setOnAction(e -> viewPayslip(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(5);
                    hbox.setAlignment(Pos.CENTER);
                    hbox.getChildren().addAll(viewBtn, downloadBtn);
                    setGraphic(hbox);
                }
            }
        });
    }

    private void setupFilters() {
        ObservableList<Integer> years = FXCollections.observableArrayList();
        for (int i = 2020; i <= java.time.LocalDate.now().getYear(); i++) {
            years.add(i);
        }
        yearFilter.setItems(years);
        yearFilter.setValue(java.time.LocalDate.now().getYear());
        yearFilter.setOnAction(e -> applyFilters());

        statusFilter.setItems(FXCollections.observableArrayList("ALL", "PENDING", "GENERATED", "PAID"));
        statusFilter.setValue("ALL");
        statusFilter.setOnAction(e -> applyFilters());
    }

    public void setUserId(int userId) {
        this.currentUserId = userId;
        loadPayslips();
        loadStatistics();
    }

    private void loadPayslips() {
        List<Payslip> payslipList = PayslipService.getPayslipsByUserId(currentUserId);
        payslips.clear();
        if (payslipList != null) {
            payslips.addAll(payslipList);
        }
    }

    private void loadStatistics() {
        List<Payslip> userPayslips = PayslipService.getPayslipsByUserId(currentUserId);
        
        if (userPayslips != null && !userPayslips.isEmpty()) {
            // Last payslip
            Payslip lastPayslip = userPayslips.get(0);
            lastPayslipLabel.setText(String.format("%.2f %s", lastPayslip.getNetSalary(), lastPayslip.getCurrency()));

            // Average salary
            double avgSalary = userPayslips.stream()
                .mapToDouble(Payslip::getGrossSalary)
                .average()
                .orElse(0.0);
            averageSalaryLabel.setText(String.format("%.2f %s", avgSalary, userPayslips.get(0).getCurrency()));

            // Total earnings
            double totalEarnings = userPayslips.stream()
                .mapToDouble(Payslip::getGrossSalary)
                .sum();
            totalYearEarningsLabel.setText(String.format("%.2f %s", totalEarnings, userPayslips.get(0).getCurrency()));
        }
    }

    private void applyFilters() {
        Integer selectedYear = yearFilter.getValue();
        String selectedStatus = statusFilter.getValue();

        payslips.clear();
        List<Payslip> allPayslips = PayslipService.getPayslipsByUserId(currentUserId);

        if (allPayslips != null) {
            allPayslips.stream()
                .filter(p -> p.getYear() == selectedYear)
                .filter(p -> selectedStatus.equals("ALL") || p.getStatus().equals(selectedStatus))
                .forEach(payslips::add);
        }
    }

    private void viewPayslip(Payslip payslip) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payslip Details");
        alert.setHeaderText(String.format("Payslip %d/%d", payslip.getMonth(), payslip.getYear()));
        alert.setContentText(String.format(
            "Gross Salary: %.2f %s\nNet Salary: %.2f %s\nDeductions: %.2f\nStatus: %s",
            payslip.getGrossSalary(), payslip.getCurrency(),
            payslip.getNetSalary(), payslip.getCurrency(),
            payslip.getDeductions(),
            payslip.getStatus()
        ));
        alert.showAndWait();
        logger.info("Viewed payslip: {}", payslip.getId());
    }

    private void downloadPayslip(Payslip payslip) {
        if (payslip.getPdfUrl() != null && !payslip.getPdfUrl().isEmpty()) {
            // TODO: Implement PDF download logic
            logger.info("Downloaded payslip: {}", payslip.getId());
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Download Unavailable");
            alert.setHeaderText("PDF not available");
            alert.setContentText("This payslip PDF is not yet available.");
            alert.showAndWait();
        }
    }

    @FXML
    private void refreshPayslips() {
        loadPayslips();
        loadStatistics();
    }
}
