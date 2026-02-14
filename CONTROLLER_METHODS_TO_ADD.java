// BANK ACCOUNTS - Methods to be added at line 471
    private void initializeBankTab() {
        bank_currencyCombo.setItems(CurrencyHelper.getWorldCurrencies());
        bank_primaryCombo.setItems(FXCollections.observableArrayList("Yes", "No"));
        bank_verifiedCombo.setItems(FXCollections.observableArrayList("Yes", "No"));
        
        bank_idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        bank_userCol.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        bank_nameCol.setCellValueFactory(new PropertyValueFactory<>("bankName"));
        bank_ibanCol.setCellValueFactory(new PropertyValueFactory<>("iban"));
        bank_swiftCol.setCellValueFactory(new PropertyValueFactory<>("swift"));
        bank_currencyCol.setCellValueFactory(new PropertyValueFactory<>("currency"));
        bank_primaryCol.setCellValueFactory(new PropertyValueFactory<>("isPrimary"));
        bank_verifiedCol.setCellValueFactory(new PropertyValueFactory<>("isVerified"));
        bankAccountTable.setItems(bankData);
        bankAccountTable.setOnMouseClicked(e -> onBankSelected());
    }

    @FXML private void handleAddBankAccount() {
        clearFieldError(bank_errorLabel);
        if (bank_userIdCombo.getValue() == null) { showFieldError(bank_errorLabel, "Select employee!"); return; }
        if (bank_nameField.getText().isEmpty()) { showFieldError(bank_errorLabel, "Bank name required!"); return; }
        if (bank_ibanField.getText().isEmpty()) { showFieldError(bank_errorLabel, "IBAN required!"); return; }
        
        EmployeeRow emp = bank_userIdCombo.getValue();
        BankAccountRow bank = new BankAccountRow(bankData.size() + 1, emp.getId(), emp.getFullName(),
            bank_nameField.getText(), bank_ibanField.getText(), bank_swiftField.getText(),
            CurrencyHelper.getCurrencyCode(bank_currencyCombo.getValue()),
            "Yes".equals(bank_primaryCombo.getValue()), "Yes".equals(bank_verifiedCombo.getValue()));
        bankData.add(bank);
        updateBankCount();
        handleClearBankForm();
        showSuccess("Bank account added!");
    }

    @FXML private void handleUpdateBankAccount() {
        if (selectedBank != null) {
            selectedBank.setBankName(bank_nameField.getText());
            selectedBank.setIban(bank_ibanField.getText());
            bankAccountTable.refresh();
            showSuccess("Bank account updated!");
        }
    }

    @FXML private void handleDeleteBankAccount() {
        if (selectedBank != null) {
            bankData.remove(selectedBank);
            updateBankCount();
            handleClearBankForm();
            showSuccess("Bank account deleted!");
        }
    }

    @FXML private void handleClearBankForm() {
        bank_userIdCombo.setValue(null);
        bank_nameField.setText("");
        bank_ibanField.setText("");
        bank_swiftField.setText("");
        bank_currencyCombo.setValue(null);
        bank_primaryCombo.setValue("No");
        bank_verifiedCombo.setValue("No");
        selectedBank = null;
    }

    @FXML private void handleRefreshBankAccounts() { bankAccountTable.refresh(); }

    private void onBankSelected() {
        selectedBank = bankAccountTable.getSelectionModel().getSelectedItem();
        if (selectedBank != null) {
            bank_userIdCombo.setValue(findEmployeeById(selectedBank.getUserId()));
            bank_nameField.setText(selectedBank.getBankName());
            bank_ibanField.setText(selectedBank.getIban());
            bank_swiftField.setText(selectedBank.getSwift());
            bank_currencyCombo.setValue(CurrencyHelper.getFullCurrencyName(selectedBank.getCurrency()));
            bank_primaryCombo.setValue(selectedBank.getIsPrimary() ? "Yes" : "No");
            bank_verifiedCombo.setValue(selectedBank.getIsVerified() ? "Yes" : "No");
        }
    }

    private void updateBankCount() { bank_countLabel.setText("Total: " + bankData.size()); }

    // BONUSES
    private void initializeBonusTab() {
        bonus_idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        bonus_userCol.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        bonus_amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        bonus_reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));
        bonus_dateCol.setCellValueFactory(new PropertyValueFactory<>("dateAwarded"));
        bonusTable.setItems(bonusData);
        bonusTable.setOnMouseClicked(e -> onBonusSelected());
    }

    @FXML private void handleAddBonus() {
        clearFieldError(bonus_errorLabel);
        if (bonus_userIdCombo.getValue() == null) { showFieldError(bonus_errorLabel, "Select employee!"); return; }
        if (!isValidDouble(bonus_amountField.getText())) { showFieldError(bonus_errorLabel, "Invalid amount!"); return; }
        
        EmployeeRow emp = bonus_userIdCombo.getValue();
        BonusRow bonus = new BonusRow(bonusData.size() + 1, emp.getId(), emp.getFullName(),
            Double.parseDouble(bonus_amountField.getText()), bonus_reasonField.getText(), LocalDate.now().toString());
        bonusData.add(bonus);
        updateBonusCount();
        handleClearBonusForm();
        showSuccess("Bonus added!");
    }

    @FXML private void handleUpdateBonus() {
        if (selectedBonus != null) {
            selectedBonus.setAmount(Double.parseDouble(bonus_amountField.getText()));
            selectedBonus.setReason(bonus_reasonField.getText());
            bonusTable.refresh();
            showSuccess("Bonus updated!");
        }
    }

    @FXML private void handleDeleteBonus() {
        if (selectedBonus != null) {
            bonusData.remove(selectedBonus);
            updateBonusCount();
            handleClearBonusForm();
            showSuccess("Bonus deleted!");
        }
    }

    @FXML private void handleClearBonusForm() {
        bonus_userIdCombo.setValue(null);
        bonus_amountField.setText("");
        bonus_reasonField.setText("");
        selectedBonus = null;
    }

    @FXML private void handleRefreshBonuses() { bonusTable.refresh(); }

    private void onBonusSelected() {
        selectedBonus = bonusTable.getSelectionModel().getSelectedItem();
        if (selectedBonus != null) {
            bonus_userIdCombo.setValue(findEmployeeById(selectedBonus.getUserId()));
            bonus_amountField.setText(String.valueOf(selectedBonus.getAmount()));
            bonus_reasonField.setText(selectedBonus.getReason());
        }
    }

    private void updateBonusCount() { bonus_countLabel.setText("Total: " + bonusData.size()); }

    // PAYSLIPS (CREATIVE!)
    private void initializePayslipTab() {
        payslip_monthCombo.setItems(FXCollections.observableArrayList(1,2,3,4,5,6,7,8,9,10,11,12));
        payslip_yearCombo.setItems(FXCollections.observableArrayList(2023,2024,2025,2026));
        payslip_currencyCombo.setItems(CurrencyHelper.getWorldCurrencies());
        payslip_statusCombo.setItems(FXCollections.observableArrayList("DRAFT","PENDING","APPROVED","PAID"));
        
        payslip_idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        payslip_userCol.setCellValueFactory(new PropertyValueFactory<>("employeeName"));
        payslip_periodCol.setCellValueFactory(new PropertyValueFactory<>("period"));
        payslip_baseCol.setCellValueFactory(new PropertyValueFactory<>("baseSalary"));
        payslip_overtimeCol.setCellValueFactory(new PropertyValueFactory<>("overtimeTotal"));
        payslip_bonusCol.setCellValueFactory(new PropertyValueFactory<>("bonuses"));
        payslip_grossCol.setCellValueFactory(new PropertyValueFactory<>("gross"));
        payslip_deductCol.setCellValueFactory(new PropertyValueFactory<>("totalDeductions"));
        payslip_netCol.setCellValueFactory(new PropertyValueFactory<>("net"));
        payslip_statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        payslipTable.setItems(payslipData);
        payslipTable.setOnMouseClicked(e -> onPayslipSelected());
    }

    @FXML private void handleCalculatePayslip() {
        try {
            double base = parseDouble(payslip_baseSalaryField.getText(), 0);
            double overtimeHours = parseDouble(payslip_overtimeField.getText(), 0);
            double overtimeRate = parseDouble(payslip_overtimeRateField.getText(), 0);
            double bonuses = parseDouble(payslip_bonusesField.getText(), 0);
            double otherDeduct = parseDouble(payslip_otherDeductionsField.getText(), 0);
            
            double overtimeTotal = overtimeHours * overtimeRate;
            double gross = base + overtimeTotal + bonuses;
            double cnss = gross * 0.0918;
            double irpp = (gross - cnss) * 0.26;
            double totalDeduct = cnss + irpp + otherDeduct;
            double net = gross - totalDeduct;
            
            payslip_cnssField.setText(String.format("%.2f", cnss));
            payslip_irppField.setText(String.format("%.2f", irpp));
            payslip_grossLabel.setText(String.format("%.2f TND", gross));
            payslip_deductionsLabel.setText(String.format("%.2f TND", totalDeduct));
            payslip_netLabel.setText(String.format("%.2f TND", net));
        } catch(Exception e) {
            showFieldError(payslip_errorLabel, "Invalid numbers!");
        }
    }

    @FXML private void handleAddPayslip() {
        clearFieldError(payslip_errorLabel);
        if (payslip_userIdCombo.getValue() == null) { showFieldError(payslip_errorLabel, "Select employee!"); return; }
        handleCalculatePayslip();
        
        EmployeeRow emp = payslip_userIdCombo.getValue();
        double base = parseDouble(payslip_baseSalaryField.getText(), 0);
        double overtimeHours = parseDouble(payslip_overtimeField.getText(), 0);
        double overtimeRate = parseDouble(payslip_overtimeRateField.getText(), 0);
        double bonuses = parseDouble(payslip_bonusesField.getText(), 0);
        
        PayslipRow payslip = new PayslipRow(payslipData.size() + 1, emp.getId(), emp.getFullName(),
            payslip_monthCombo.getValue(), payslip_yearCombo.getValue(), base,
            overtimeHours, overtimeHours * overtimeRate, bonuses,
            CurrencyHelper.getCurrencyCode(payslip_currencyCombo.getValue()), payslip_statusCombo.getValue());
        payslip.setOtherDeductions(parseDouble(payslip_otherDeductionsField.getText(), 0));
        payslipData.add(payslip);
        updatePayslipCount();
        handleClearPayslipForm();
        showSuccess("Payslip saved!");
    }

    @FXML private void handleUpdatePayslip() {
        if (selectedPayslip != null) {
            handleCalculatePayslip();
            payslipTable.refresh();
            showSuccess("Payslip updated!");
        }
    }

    @FXML private void handleDeletePayslip() {
        if (selectedPayslip != null) {
            payslipData.remove(selectedPayslip);
            updatePayslipCount();
            handleClearPayslipForm();
            showSuccess("Payslip deleted!");
        }
    }

    @FXML private void handleClearPayslipForm() {
        payslip_userIdCombo.setValue(null);
        payslip_baseSalaryField.setText("");
        payslip_overtimeField.setText("0");
        payslip_overtimeRateField.setText("0");
        payslip_bonusesField.setText("0");
        payslip_cnssField.setText("");
        payslip_irppField.setText("");
        payslip_otherDeductionsField.setText("0");
        payslip_grossLabel.setText("0.00 TND");
        payslip_deductionsLabel.setText("0.00 TND");
        payslip_netLabel.setText("0.00 TND");
        selectedPayslip = null;
    }

    @FXML private void handleRefreshPayslips() { payslipTable.refresh(); }
    
    @FXML private void handleExportPayslipPDF() {
        if (selectedPayslip != null) {
            // Generate single payslip PDF
            showSuccess("PDF export feature coming soon!");
        } else {
            showFieldError(payslip_errorLabel, "Select a payslip first!");
        }
    }

    @FXML private void handleExportSelectedPayslip() { handleExportPayslipPDF(); }

    private void onPayslipSelected() {
        selectedPayslip = payslipTable.getSelectionModel().getSelectedItem();
        if (selectedPayslip != null) {
            payslip_userIdCombo.setValue(findEmployeeById(selectedPayslip.getUserId()));
            payslip_monthCombo.setValue(selectedPayslip.getMonth());
            payslip_yearCombo.setValue(selectedPayslip.getYear());
            payslip_baseSalaryField.setText(String.valueOf(selectedPayslip.getBaseSalary()));
            payslip_overtimeField.setText(String.valueOf(selectedPayslip.getOvertime()));
            payslip_bonusesField.setText(String.valueOf(selectedPayslip.getBonuses()));
            payslip_statusCombo.setValue(selectedPayslip.getStatus());
            handleCalculatePayslip();
        }
    }

    private void updatePayslipCount() { payslip_countLabel.setText("Total: " + payslipData.size()); }

    // REPORTS
    private void initializeReportsTab() {
        tax_currencyCombo.setItems(FXCollections.observableArrayList("TND","EUR","USD"));
    }

    @FXML private void handleGenerateEmployeeReport() {
        if (report_employeeCombo.getValue() == null) {
            showSuccess("Please select an employee!");
            return;
        }
        
        EmployeeRow emp = report_employeeCombo.getValue();
        String contractInfo = buildContractInfo(emp.getId());
        String bankInfo = buildBankInfo(emp.getId());
        String bonusInfo = buildBonusInfo(emp.getId());
        String payslipInfo = buildPayslipInfo(emp.getId());
        
        File pdf = PDFGenerator.generateEmployeeReport(emp.getId(), emp.getFullName(),
            contractInfo, bankInfo, bonusInfo, payslipInfo, (Stage) report_employeeCombo.getScene().getWindow());
        
        if (pdf != null) {
            showSuccess("PDF generated: " + pdf.getName());
        }
    }

    @FXML private void handleCalculateTax() {
        try {
            double gross = Double.parseDouble(tax_grossField.getText());
            double cnss = gross * 0.0918;
            double irpp = (gross - cnss) * 0.26;
            double net = gross - cnss - irpp;
            
            String result = String.format(
                "=== TAX BREAKDOWN ===\n\n" +
                "Gross Salary: %.2f %s\n" +
                "CNSS (9.18%%): %.2f %s\n" +
                "IRPP (26%%): %.2f %s\n" +
                "Total Deductions: %.2f %s\n\n" +
                "NET SALARY: %.2f %s",
                gross, tax_currencyCombo.getValue(),
                cnss, tax_currencyCombo.getValue(),
                irpp, tax_currencyCombo.getValue(),
                (cnss + irpp), tax_currencyCombo.getValue(),
                net, tax_currencyCombo.getValue()
            );
            tax_resultArea.setText(result);
        } catch (Exception e) {
            tax_resultArea.setText("Error: Invalid amount!");
        }
    }

    @FXML private void toggleTheme() {
        isDarkMode = !isDarkMode;
        themeToggleBtn.setText(isDarkMode ? "🌙 Dark" : "☀️ Light");
        showSuccess("Theme toggled!");
    }

    // UTILITY METHODS
    private void loadSampleData() {
        // Sample employees
        employeeData.add(new EmployeeRow(101, "Ahmed", "Ben Ali", "ahmed@skilora.tn", "+216 20123456", "Developer"));
        employeeData.add(new EmployeeRow(102, "Fatima", "Mansouri", "fatima@skilora.tn", "+216 20234567", "Manager"));
        employeeData.add(new EmployeeRow(103, "Mohamed", "Trabelsi", "mohamed@skilora.tn", "+216 20345678", "Designer"));
        refreshEmployeeComboBoxes();
        updateEmployeeCount();
    }

    private EmployeeRow findEmployeeById(int id) {
        return employeeData.stream().filter(e -> e.getId() == id).findFirst().orElse(null);
    }

    private String buildContractInfo(int empId) {
        StringBuilder sb = new StringBuilder();
        contractData.stream().filter(c -> c.getUserId() == empId).forEach(c ->
            sb.append("<div class='info-row'><div class='info-label'>Position:</div><div class='info-value'>")
              .append(c.getPosition()).append("</div></div>")
              .append("<div class='info-row'><div class='info-label'>Salary:</div><div class='info-value'>")
              .append(c.getSalary()).append(" TND</div></div>"));
        return sb.toString();
    }

    private String buildBankInfo(int empId) {
        StringBuilder sb = new StringBuilder();
        bankData.stream().filter(b -> b.getUserId() == empId).forEach(b ->
            sb.append("<div class='info-row'><div class='info-label'>Bank:</div><div class='info-value'>")
              .append(b.getBankName()).append("</div></div>")
              .append("<div class='info-row'><div class='info-label'>IBAN:</div><div class='info-value'>")
              .append(b.getIban()).append("</div></div>"));
        return sb.toString();
    }

    private String buildBonusInfo(int empId) {
        StringBuilder sb = new StringBuilder();
        bonusData.stream().filter(b -> b.getUserId() == empId).forEach(b ->
            sb.append("<div class='info-row'><div class='info-label'>Amount:</div><div class='info-value'>")
              .append(b.getAmount()).append(" TND</div></div>"));
        return sb.toString();
    }

    private String buildPayslipInfo(int empId) {
        StringBuilder sb = new StringBuilder();
        payslipData.stream().filter(p -> p.getUserId() == empId).forEach(p ->
            sb.append("<div class='info-row'><div class='info-label'>Period:</div><div class='info-value'>")
              .append(p.getPeriod()).append ("</div></div>")
              .append("<div class='info-row'><div class='info-label'>Net:</div><div class='info-value'>")
              .append(p.getNet()).append(" TND</div></div>"));
        return sb.toString();
    }

    private void showFieldError(Label label, String message) {
        label.setText("⚠️ " + message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void clearFieldError(Label label) {
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    private void showSuccess(String message) {
        System.out.println("✅ " + message);
    }

    private boolean isValidInteger(String text) {
        try { Integer.parseInt(text); return true; } catch(Exception e) { return false; }
    }

    private boolean isValidDouble(String text) {
        try { Double.parseDouble(text); return true; } catch(Exception e) { return false; }
    }

    private double parseDouble(String text, double defaultValue) {
        try { return Double.parseDouble(text); } catch(Exception e) { return defaultValue; }
    }
}
