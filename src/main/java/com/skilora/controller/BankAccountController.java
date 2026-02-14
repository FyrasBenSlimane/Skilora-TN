package com.skilora.controller;

import com.skilora.model.entity.BankAccount;
import com.skilora.model.service.BankAccountService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

/**
 * Bank Account Controller
 * Manages bank account display and operations.
 */
public class BankAccountController {
    @FXML private TableView<BankAccount> bankTable;
    @FXML private TableColumn<BankAccount, String> bankColumn;
    @FXML private TableColumn<BankAccount, String> ibanColumn;
    @FXML private TableColumn<BankAccount, String> currencyColumn;
    @FXML private TableColumn<BankAccount, Boolean> verifiedColumn;
    @FXML private TableColumn<BankAccount, Boolean> primaryColumn;
    @FXML private TableColumn<BankAccount, Void> actionColumn;

    @FXML private Label primaryAccountLabel;
    @FXML private Label primaryAccountHolderLabel;

    @FXML private TextField bankNameField;
    @FXML private TextField ibanField;
    @FXML private TextField swiftCodeField;
    @FXML private TextField accountHolderField;
    @FXML private ComboBox<String> currencyBox;

    private int currentUserId;
    private ObservableList<BankAccount> bankAccounts = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        setupTable();
        setupColumns();
        setupFormFields();
    }

    private void setupTable() {
        bankTable.setItems(bankAccounts);
        bankTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }

    private void setupColumns() {
        bankColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getBankName())
        );

        ibanColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getIbanMasked())
        );

        currencyColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCurrency())
        );

        verifiedColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().isVerified())
        );

        verifiedColumn.setCellFactory(column -> new TableCell<BankAccount, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(item ? "✓ Verified" : "○ Pending");
                    setStyle(item ? "-fx-text-fill: green;" : "-fx-text-fill: orange;");
                }
            }
        });

        primaryColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().isPrimaryAccount())
        );

        primaryColumn.setCellFactory(column -> new TableCell<BankAccount, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(item ? "★ Primary" : "○ Secondary");
                    setStyle(item ? "-fx-text-fill: gold;" : "-fx-text-fill: gray;");
                }
            }
        });

        actionColumn.setCellFactory(column -> new TableCell<BankAccount, Void>() {
            private final Button viewBtn = new Button("View");
            private final Button setPrimaryBtn = new Button("Set Primary");
            private final Button verifyBtn = new Button("Verify");
            private final Button deleteBtn = new Button("Delete");

            {
                viewBtn.setStyle("-fx-padding: 5; -fx-font-size: 10;");
                setPrimaryBtn.setStyle("-fx-padding: 5; -fx-font-size: 10;");
                verifyBtn.setStyle("-fx-padding: 5; -fx-font-size: 10;");
                deleteBtn.setStyle("-fx-padding: 5; -fx-font-size: 10;");

                viewBtn.setOnAction(e -> viewAccount(getTableView().getItems().get(getIndex())));
                setPrimaryBtn.setOnAction(e -> setPrimary(getTableView().getItems().get(getIndex())));
                verifyBtn.setOnAction(e -> verify(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> deleteAccount(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    VBox vbox = new VBox(3);
                    HBox hbox1 = new HBox(5);
                    HBox hbox2 = new HBox(5);
                    hbox1.getChildren().addAll(viewBtn, setPrimaryBtn);
                    hbox2.getChildren().addAll(verifyBtn, deleteBtn);
                    vbox.getChildren().addAll(hbox1, hbox2);
                    setGraphic(vbox);
                }
            }
        });
    }

    private void setupFormFields() {
        currencyBox.setItems(FXCollections.observableArrayList(
            "TND", "EUR", "USD", "GBP", "JPY", "CHF"
        ));
        currencyBox.setValue("TND");
    }

    public void setUserId(int userId) {
        this.currentUserId = userId;
        loadBankAccounts();
        loadPrimaryAccount();
    }

    private void loadBankAccounts() {
        List<BankAccount> accounts = BankAccountService.getAccountsByUserId(currentUserId);
        bankAccounts.clear();
        if (accounts != null) {
            bankAccounts.addAll(accounts);
        }
    }

    private void loadPrimaryAccount() {
        BankAccount primary = BankAccountService.getPrimaryAccount(currentUserId);
        if (primary != null) {
            primaryAccountLabel.setText(primary.getBankName());
            primaryAccountHolderLabel.setText(primary.getAccountHolderName() != null ? primary.getAccountHolderName() : "N/A");
        } else {
            primaryAccountLabel.setText("No primary account");
            primaryAccountHolderLabel.setText("N/A");
        }
    }

    @FXML
    private void addBankAccount() {
        if (ibanField.getText().isEmpty() || bankNameField.getText().isEmpty()) {
            showAlert("Validation Error", "Please fill in all required fields");
            return;
        }

        BankAccount account = new BankAccount();
        account.setUserId(currentUserId);
        account.setBankName(bankNameField.getText());
        account.setIban(ibanField.getText());
        account.setSwiftCode(swiftCodeField.getText());
        account.setCurrency(currencyBox.getValue());
        account.setAccountHolderName(accountHolderField.getText());

        BankAccount created = BankAccountService.createAccount(account);
        if (created != null) {
            showAlert("Success", "Bank account added successfully");
            clearForm();
            loadBankAccounts();
            loadPrimaryAccount();
        } else {
            showAlert("Error", "Failed to add bank account");
        }
    }

    private void viewAccount(BankAccount account) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Bank Account Details");
        alert.setHeaderText("Account Information");
        alert.setContentText(String.format(
            "Bank: %s\nIBAN: %s\nSwift: %s\nCurrency: %s\nHolder: %s\nVerified: %s\nPrimary: %s",
            account.getBankName(),
            account.getIban(),
            account.getSwiftCode() != null ? account.getSwiftCode() : "N/A",
            account.getCurrency(),
            account.getAccountHolderName() != null ? account.getAccountHolderName() : "N/A",
            account.isVerified() ? "Yes" : "No",
            account.isPrimaryAccount() ? "Yes" : "No"
        ));
        alert.showAndWait();
    }

    private void setPrimary(BankAccount account) {
        if (BankAccountService.setPrimaryAccount(account.getId(), currentUserId)) {
            showAlert("Success", "Primary account updated");
            loadBankAccounts();
            loadPrimaryAccount();
        }
    }

    private void verify(BankAccount account) {
        if (BankAccountService.verifyAccount(account.getId())) {
            showAlert("Success", "Account verified");
            loadBankAccounts();
        }
    }

    private void deleteAccount(BankAccount account) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Account");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("Delete this bank account?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            if (BankAccountService.deleteAccount(account.getId())) {
                loadBankAccounts();
                loadPrimaryAccount();
            }
        }
    }

    private void clearForm() {
        bankNameField.clear();
        ibanField.clear();
        swiftCodeField.clear();
        accountHolderField.clear();
        currencyBox.setValue("TND");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void refreshAccounts() {
        loadBankAccounts();
        loadPrimaryAccount();
    }
}
