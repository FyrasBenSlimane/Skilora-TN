package com.skilora.controller;

import com.skilora.model.entity.EmploymentContract;
import com.skilora.model.service.EmploymentContractService;
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
 * Employment Contract Controller
 * Manages employment contract display and operations.
 */
public class EmploymentContractController {
    private static final Logger logger = LoggerFactory.getLogger(EmploymentContractController.class);

    @FXML private TableView<EmploymentContract> contractTable;
    @FXML private TableColumn<EmploymentContract, String> companyColumn;
    @FXML private TableColumn<EmploymentContract, String> typeColumn;
    @FXML private TableColumn<EmploymentContract, Double> salaryColumn;
    @FXML private TableColumn<EmploymentContract, LocalDate> startDateColumn;
    @FXML private TableColumn<EmploymentContract, String> statusColumn;
    @FXML private TableColumn<EmploymentContract, Void> actionColumn;

    @FXML private Label currentContractLabel;
    @FXML private Label salaryLabel;
    @FXML private Label statusLabel;

    private int currentUserId;
    private ObservableList<EmploymentContract> contracts = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        setupTable();
        setupColumns();
    }

    private void setupTable() {
        contractTable.setItems(contracts);
        contractTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }

    private void setupColumns() {
        companyColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getCompanyName() != null ? cellData.getValue().getCompanyName() : "N/A"
            )
        );

        typeColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getType())
        );

        salaryColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getBaseSalary())
        );

        startDateColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getStartDate())
        );

        statusColumn.setCellValueFactory(cellData -> {
            EmploymentContract contract = cellData.getValue();
            String status = contract.isSigned() ? "SIGNED" : "PENDING";
            return new javafx.beans.property.SimpleStringProperty(status);
        });

        actionColumn.setCellFactory(column -> new TableCell<EmploymentContract, Void>() {
            private final Button viewBtn = new Button("View");
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");

            {
                viewBtn.setStyle("-fx-padding: 5; -fx-font-size: 11;");
                editBtn.setStyle("-fx-padding: 5; -fx-font-size: 11;");
                deleteBtn.setStyle("-fx-padding: 5; -fx-font-size: 11;");

                viewBtn.setOnAction(e -> viewContract(getTableView().getItems().get(getIndex())));
                editBtn.setOnAction(e -> editContract(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> deleteContract(getTableView().getItems().get(getIndex())));
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

    public void setUserId(int userId) {
        this.currentUserId = userId;
        loadContracts();
        loadCurrentContract();
    }

    private void loadContracts() {
        List<EmploymentContract> contractList = EmploymentContractService.getContractsByUserId(currentUserId);
        contracts.clear();
        if (contractList != null) {
            contracts.addAll(contractList);
        }
    }

    private void loadCurrentContract() {
        EmploymentContract activeContract = EmploymentContractService.getActiveContract(currentUserId);
        if (activeContract != null) {
            currentContractLabel.setText(activeContract.getCompanyName() != null ? activeContract.getCompanyName() : "N/A");
            salaryLabel.setText(String.format("%.2f %s", activeContract.getBaseSalary(), activeContract.getCurrency()));
            statusLabel.setText(activeContract.isSigned() ? "SIGNED" : "PENDING");
        } else {
            currentContractLabel.setText("No active contract");
            salaryLabel.setText("N/A");
            statusLabel.setText("INACTIVE");
        }
    }

    private void viewContract(EmploymentContract contract) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Contract Details");
        alert.setHeaderText("Employment Contract");
        alert.setContentText(contract.toString());
        alert.showAndWait();
        logger.info("Viewed contract: {}", contract.getId());
    }

    private void editContract(EmploymentContract contract) {
        // TODO: Open edit dialog
        logger.info("Edit contract: {}", contract.getId());
    }

    private void deleteContract(EmploymentContract contract) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Contract");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("Delete this employment contract?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            if (EmploymentContractService.deleteContract(contract.getId())) {
                contracts.remove(contract);
                logger.info("Deleted contract: {}", contract.getId());
            }
        }
    }

    @FXML
    private void refreshContracts() {
        loadContracts();
        loadCurrentContract();
    }
}
