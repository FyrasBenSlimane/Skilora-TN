package com.skilora.controller;

import com.skilora.model.entity.Bonus;
import com.skilora.model.service.BonusService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

/**
 * Bonus Controller
 * Manages bonus display and operations.
 */
public class BonusController {
    private static final Logger logger = LoggerFactory.getLogger(BonusController.class);

    @FXML private TableView<Bonus> bonusTable;
    @FXML private TableColumn<Bonus, LocalDate> dateColumn;
    @FXML private TableColumn<Bonus, Double> amountColumn;
    @FXML private TableColumn<Bonus, String> reasonColumn;
    @FXML private TableColumn<Bonus, String> currencyColumn;
    @FXML private TableColumn<Bonus, Void> actionColumn;

    @FXML private Label totalBonusesLabel;
    @FXML private Label averageBonusLabel;
    @FXML private Label lastBonusLabel;

    @FXML private ComboBox<Integer> yearFilter;

    private int currentUserId;
    private ObservableList<Bonus> bonuses = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        setupTable();
        setupColumns();
        setupFilters();
    }

    private void setupTable() {
        bonusTable.setItems(bonuses);
        bonusTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }

    private void setupColumns() {
        dateColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getDateAwarded())
        );

        amountColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getAmount())
        );

        reasonColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getReason())
        );

        currencyColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCurrency())
        );

        actionColumn.setCellFactory(column -> new TableCell<Bonus, Void>() {
            private final Button viewBtn = new Button("View");
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");

            {
                viewBtn.setStyle("-fx-padding: 5; -fx-font-size: 11;");
                editBtn.setStyle("-fx-padding: 5; -fx-font-size: 11;");
                deleteBtn.setStyle("-fx-padding: 5; -fx-font-size: 11;");

                viewBtn.setOnAction(e -> viewBonus(getTableView().getItems().get(getIndex())));
                editBtn.setOnAction(e -> editBonus(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> deleteBonus(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(5);
                    hbox.setAlignment(Pos.CENTER);
                    hbox.getChildren().addAll(viewBtn, editBtn, deleteBtn);
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
    }

    public void setUserId(int userId) {
        this.currentUserId = userId;
        loadBonuses();
        loadStatistics();
    }

    private void loadBonuses() {
        List<Bonus> bonusList = BonusService.getBonusesByUserId(currentUserId);
        bonuses.clear();
        if (bonusList != null) {
            bonuses.addAll(bonusList);
        }
    }

    private void loadStatistics() {
        int currentYear = java.time.LocalDate.now().getYear();
        
        // Total bonuses for current year
        double totalBonuses = BonusService.getTotalBonusesByYear(currentUserId, currentYear);
        totalBonusesLabel.setText(String.format("%.2f TND", totalBonuses));

        // Average bonus
        List<Bonus> bonusList = BonusService.getBonusesByUserId(currentUserId);
        if (bonusList != null && !bonusList.isEmpty()) {
            double avgBonus = bonusList.stream()
                .mapToDouble(Bonus::getAmount)
                .average()
                .orElse(0.0);
            averageBonusLabel.setText(String.format("%.2f TND", avgBonus));

            // Last bonus
            Bonus lastBonus = bonusList.get(0);
            lastBonusLabel.setText(String.format("%.2f TND on %s", lastBonus.getAmount(), lastBonus.getDateAwarded()));
        }
    }

    private void applyFilters() {
        Integer selectedYear = yearFilter.getValue();
        
        bonuses.clear();
        LocalDate startDate = LocalDate.of(selectedYear, 1, 1);
        LocalDate endDate = LocalDate.of(selectedYear, 12, 31);
        
        List<Bonus> filteredBonuses = BonusService.getBonusesByDateRange(currentUserId, startDate, endDate);
        if (filteredBonuses != null) {
            bonuses.addAll(filteredBonuses);
        }
    }

    private void viewBonus(Bonus bonus) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Bonus Details");
        alert.setHeaderText("Bonus Information");
        alert.setContentText(String.format(
            "Amount: %.2f %s\nReason: %s\nDate Awarded: %s",
            bonus.getAmount(),
            bonus.getCurrency(),
            bonus.getReason(),
            bonus.getDateAwarded()
        ));
        alert.showAndWait();
    }

    private void editBonus(Bonus bonus) {
        // TODO: Open edit dialog
        logger.info("Edit bonus: {}", bonus.getId());
    }

    private void deleteBonus(Bonus bonus) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Bonus");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("Delete this bonus record?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            if (BonusService.deleteBonus(bonus.getId())) {
                bonuses.remove(bonus);
                loadStatistics();
                logger.info("Deleted bonus: {}", bonus.getId());
            }
        }
    }

    @FXML
    private void refreshBonuses() {
        loadBonuses();
        loadStatistics();
    }
}
