# MODULE 5: Finances & Rémunération — Agent Prompt

> **Goal:** Implement the complete Finances & Rémunération module from scratch. This module handles employment contracts, payslips with tax calculations, bank accounts, salary history, exchange rates, and payment transactions. Build everything: entities, services, controllers, views, DB schema, and i18n.

---

## CONTEXT — READ FIRST

### Project Architecture

- **Language:** Java 17, JavaFX 21, Maven
- **Database:** MySQL 8.0 via HikariCP connection pool
- **Pattern:** MVC (Model = entity + service, View = FXML, Controller = JavaFX controller)
- **NO DAO LAYER.** Services directly use `DatabaseConfig.getInstance().getConnection()` with PreparedStatement
- **Singleton pattern** on all services: `private static volatile XxxService instance; public static XxxService getInstance()`
- **I18n:** All UI strings use `I18n.get("key")` from `com.skilora.utils.I18n`. Resource bundles at `src/main/resources/com/skilora/i18n/messages_xx.properties`
- **Logging:** SLF4J via `LoggerFactory.getLogger(ClassName.class)`
- **UI Components:** Custom TL* components from `com.skilora.framework.components` (TLButton, TLCard, TLBadge, TLTextField, TLSelect, TLAlert, TLDialog, TLTextarea, etc.)
- **Async:** All DB calls from controllers use `javafx.concurrent.Task` with `Platform.runLater()` for UI updates
- **Currency:** Primary currencies are TND (Tunisian Dinar) and EUR (Euro). Use `BigDecimal` for all monetary calculations.

### Key File Locations

```
src/main/java/com/skilora/
├── config/DatabaseConfig.java          — DB connection singleton (HikariCP)
├── config/DatabaseInitializer.java     — Auto-creates tables on startup
├── model/entity/                       — Pure POJO entities (no JavaFX deps)
├── model/enums/                        — Enum types
├── model/service/                      — Service singletons (CRUD + business logic)
├── controller/                         — FXML controllers
├── ui/MainView.java                    — Main navigation (loads FXML views)
├── utils/I18n.java                     — Internationalization utility

src/main/resources/com/skilora/
├── i18n/messages_*.properties          — 4 resource bundles (default/fr/en/ar)
├── view/*.fxml                         — FXML view files
```

### Existing Patterns to Follow

Same entity, service, controller, FXML, and DatabaseInitializer patterns described in Modules 2-4. Refer to those for reference. Key rules:
- Entity: Pure POJO, no JavaFX imports, in `com.skilora.model.entity`
- Service: Singleton, direct JDBC, in `com.skilora.model.service`
- Controller: `Initializable`, async `Task`, `I18n.get()`, in `com.skilora.controller`
- FXML: TL* components, transparent bg VBox root, in `src/main/resources/com/skilora/view/`

### Security & Access Control

- **Employees** can:
  - View their own contracts and payslips
  - Manage their own bank accounts
  - Sign their own contracts
  - View salary history
  
- **Employers/Admins** can:
  - Create and manage all contracts
  - Generate payslips for all employees
  - Process payments
  - Configure tax rates and exchange rates
  - View reports and analytics
  
- **Implementation:** Check `SessionManager.getCurrentUser().getRole()` before allowing operations

---

## WHAT EXISTS NOW

**Nothing.** No entities, services, controllers, or views exist for the Finance module. Everything must be built from scratch.

---

## PHASE 1: Database Schema

### Step 1.1 — Add all tables to `DatabaseInitializer.java`

```sql
CREATE TABLE IF NOT EXISTS employment_contracts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    employer_id INT,
    job_offer_id INT,
    salary_base DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'TND',
    start_date DATE NOT NULL,
    end_date DATE,
    contract_type VARCHAR(20) DEFAULT 'CDI',
    status VARCHAR(20) DEFAULT 'DRAFT',
    pdf_url TEXT,
    is_signed BOOLEAN DEFAULT FALSE,
    signed_date DATETIME,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (employer_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (job_offer_id) REFERENCES job_offers(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS payslips (
    id INT AUTO_INCREMENT PRIMARY KEY,
    contract_id INT NOT NULL,
    user_id INT NOT NULL,
    period_month INT NOT NULL,
    period_year INT NOT NULL,
    gross_salary DECIMAL(12,2) NOT NULL,
    net_salary DECIMAL(12,2) NOT NULL,
    cnss_employee DECIMAL(10,2) DEFAULT 0.00,
    cnss_employer DECIMAL(10,2) DEFAULT 0.00,
    irpp DECIMAL(10,2) DEFAULT 0.00,
    other_deductions DECIMAL(10,2) DEFAULT 0.00,
    bonuses DECIMAL(10,2) DEFAULT 0.00,
    currency VARCHAR(10) DEFAULT 'TND',
    payment_status VARCHAR(20) DEFAULT 'PENDING',
    payment_date DATETIME,
    pdf_url TEXT,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_payslip_period (contract_id, period_month, period_year),
    FOREIGN KEY (contract_id) REFERENCES employment_contracts(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bank_accounts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    account_holder VARCHAR(150) NOT NULL,
    iban VARCHAR(34),
    swift_bic VARCHAR(11),
    rib VARCHAR(24),
    currency VARCHAR(10) DEFAULT 'TND',
    is_primary BOOLEAN DEFAULT FALSE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS salary_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    contract_id INT NOT NULL,
    old_salary DECIMAL(12,2),
    new_salary DECIMAL(12,2) NOT NULL,
    reason VARCHAR(255),
    effective_date DATE NOT NULL,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (contract_id) REFERENCES employment_contracts(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS exchange_rates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    from_currency VARCHAR(10) NOT NULL,
    to_currency VARCHAR(10) NOT NULL,
    rate DECIMAL(12,6) NOT NULL,
    rate_date DATE NOT NULL,
    source VARCHAR(50) DEFAULT 'BCT',
    last_updated DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_rate (from_currency, to_currency, rate_date)
);

CREATE TABLE IF NOT EXISTS payment_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    payslip_id INT,
    from_account_id INT,
    to_account_id INT,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'TND',
    transaction_type VARCHAR(30) DEFAULT 'SALARY',
    status VARCHAR(20) DEFAULT 'PENDING',
    reference VARCHAR(50),
    transaction_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    FOREIGN KEY (payslip_id) REFERENCES payslips(id) ON DELETE SET NULL,
    FOREIGN KEY (from_account_id) REFERENCES bank_accounts(id) ON DELETE SET NULL,
    FOREIGN KEY (to_account_id) REFERENCES bank_accounts(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS tax_configurations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    country VARCHAR(50) NOT NULL,
    tax_type VARCHAR(50) NOT NULL,
    rate DECIMAL(8,4) NOT NULL,
    min_bracket DECIMAL(12,2) DEFAULT 0.00,
    max_bracket DECIMAL(12,2),
    description VARCHAR(255),
    effective_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);
```

Add indexes:
```sql
CREATE INDEX idx_contracts_user ON employment_contracts(user_id);
CREATE INDEX idx_contracts_employer ON employment_contracts(employer_id);
CREATE INDEX idx_contracts_status ON employment_contracts(status);
CREATE INDEX idx_payslips_contract ON payslips(contract_id);
CREATE INDEX idx_payslips_user ON payslips(user_id);
CREATE INDEX idx_payslips_period ON payslips(period_year, period_month);
CREATE INDEX idx_bank_accounts_user ON bank_accounts(user_id);
CREATE INDEX idx_salary_history_contract ON salary_history(contract_id);
CREATE INDEX idx_exchange_rates_currencies ON exchange_rates(from_currency, to_currency);
CREATE INDEX idx_transactions_payslip ON payment_transactions(payslip_id);
CREATE INDEX idx_tax_config_country ON tax_configurations(country, tax_type);
```

---

## PHASE 2: Enums

### Step 2.1 — Create `ContractType.java`

```java
package com.skilora.model.enums;

public enum ContractType {
    CDI, CDD, FREELANCE, STAGE
}
```

### Step 2.2 — Create `ContractStatus.java`

```java
package com.skilora.model.enums;

public enum ContractStatus {
    DRAFT, PENDING_SIGNATURE, ACTIVE, EXPIRED, TERMINATED
}
```

### Step 2.3 — Create `PaymentStatus.java`

```java
package com.skilora.model.enums;

public enum PaymentStatus {
    PENDING, PROCESSING, PAID, FAILED, CANCELLED
}
```

### Step 2.4 — Create `TransactionType.java`

```java
package com.skilora.model.enums;

public enum TransactionType {
    SALARY, BONUS, REIMBURSEMENT, ADVANCE, OTHER
}
```

### Step 2.5 — Create `Currency.java`

```java
package com.skilora.model.enums;

public enum Currency {
    TND("Tunisian Dinar", "TND"),
    EUR("Euro", "€"),
    USD("US Dollar", "$"),
    GBP("British Pound", "£");

    private final String displayName;
    private final String symbol;

    Currency(String displayName, String symbol) {
        this.displayName = displayName;
        this.symbol = symbol;
    }

    public String getDisplayName() { return displayName; }
    public String getSymbol() { return symbol; }
    public String getCode() { return name(); }
}
```

---

## PHASE 3: Entities

### Step 3.1 — Create `EmploymentContract.java`

File: `src/main/java/com/skilora/model/entity/EmploymentContract.java`

```java
package com.skilora.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmploymentContract {
    private int id;
    private int userId;
    private Integer employerId;
    private Integer jobOfferId;
    private BigDecimal salaryBase;
    private String currency;
    private LocalDate startDate;
    private LocalDate endDate;
    private String contractType;
    private String status;
    private String pdfUrl;
    private boolean isSigned;
    private LocalDateTime signedDate;
    private LocalDateTime createdDate;
    
    // Transient fields (not in DB)
    private String userName;
    private String employerName;
    private String jobTitle;
    
    // Constructors
    public EmploymentContract() {
        this.currency = "TND";
        this.contractType = "CDI";
        this.status = "DRAFT";
        this.isSigned = false;
    }
    
    public EmploymentContract(int userId, BigDecimal salaryBase, LocalDate startDate) {
        this();
        this.userId = userId;
        this.salaryBase = salaryBase;
        this.startDate = startDate;
    }
    
    // Getters and Setters for all fields
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public Integer getEmployerId() { return employerId; }
    public void setEmployerId(Integer employerId) { this.employerId = employerId; }
    
    public Integer getJobOfferId() { return jobOfferId; }
    public void setJobOfferId(Integer jobOfferId) { this.jobOfferId = jobOfferId; }
    
    public BigDecimal getSalaryBase() { return salaryBase; }
    public void setSalaryBase(BigDecimal salaryBase) { this.salaryBase = salaryBase; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
    
    public boolean isSigned() { return isSigned; }
    public void setSigned(boolean signed) { isSigned = signed; }
    
    public LocalDateTime getSignedDate() { return signedDate; }
    public void setSignedDate(LocalDateTime signedDate) { this.signedDate = signedDate; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public String getEmployerName() { return employerName; }
    public void setEmployerName(String employerName) { this.employerName = employerName; }
    
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    
    @Override
    public String toString() {
        return "EmploymentContract{" +
                "id=" + id +
                ", userId=" + userId +
                ", salaryBase=" + salaryBase +
                ", currency='" + currency + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
```

**Use `java.math.BigDecimal` for all money fields.**

### Step 3.2 — Create `Payslip.java`

File: `src/main/java/com/skilora/model/entity/Payslip.java`

```java
package com.skilora.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payslip {
    private int id;
    private int contractId;
    private int userId;
    private int periodMonth;
    private int periodYear;
    private BigDecimal grossSalary;
    private BigDecimal netSalary;
    private BigDecimal cnssEmployee;
    private BigDecimal cnssEmployer;
    private BigDecimal irpp;
    private BigDecimal otherDeductions;
    private BigDecimal bonuses;
    private String currency;
    private String paymentStatus;
    private LocalDateTime paymentDate;
    private String pdfUrl;
    private LocalDateTime createdDate;
    
    // Transient fields
    private String userName;
    private String periodLabel;
    
    // Constructors
    public Payslip() {
        this.currency = "TND";
        this.paymentStatus = "PENDING";
        this.cnssEmployee = BigDecimal.ZERO;
        this.cnssEmployer = BigDecimal.ZERO;
        this.irpp = BigDecimal.ZERO;
        this.otherDeductions = BigDecimal.ZERO;
        this.bonuses = BigDecimal.ZERO;
    }
    
    public Payslip(int contractId, int userId, int periodMonth, int periodYear, BigDecimal grossSalary) {
        this();
        this.contractId = contractId;
        this.userId = userId;
        this.periodMonth = periodMonth;
        this.periodYear = periodYear;
        this.grossSalary = grossSalary;
        this.netSalary = grossSalary;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getContractId() { return contractId; }
    public void setContractId(int contractId) { this.contractId = contractId; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public int getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(int periodMonth) { this.periodMonth = periodMonth; }
    
    public int getPeriodYear() { return periodYear; }
    public void setPeriodYear(int periodYear) { this.periodYear = periodYear; }
    
    public BigDecimal getGrossSalary() { return grossSalary; }
    public void setGrossSalary(BigDecimal grossSalary) { this.grossSalary = grossSalary; }
    
    public BigDecimal getNetSalary() { return netSalary; }
    public void setNetSalary(BigDecimal netSalary) { this.netSalary = netSalary; }
    
    public BigDecimal getCnssEmployee() { return cnssEmployee; }
    public void setCnssEmployee(BigDecimal cnssEmployee) { this.cnssEmployee = cnssEmployee; }
    
    public BigDecimal getCnssEmployer() { return cnssEmployer; }
    public void setCnssEmployer(BigDecimal cnssEmployer) { this.cnssEmployer = cnssEmployer; }
    
    public BigDecimal getIrpp() { return irpp; }
    public void setIrpp(BigDecimal irpp) { this.irpp = irpp; }
    
    public BigDecimal getOtherDeductions() { return otherDeductions; }
    public void setOtherDeductions(BigDecimal otherDeductions) { this.otherDeductions = otherDeductions; }
    
    public BigDecimal getBonuses() { return bonuses; }
    public void setBonuses(BigDecimal bonuses) { this.bonuses = bonuses; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    
    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public String getPeriodLabel() { return periodLabel; }
    public void setPeriodLabel(String periodLabel) { this.periodLabel = periodLabel; }
    
    @Override
    public String toString() {
        return "Payslip{" +
                "id=" + id +
                ", period=" + periodMonth + "/" + periodYear +
                ", gross=" + grossSalary +
                ", net=" + netSalary +
                ", status='" + paymentStatus + '\'' +
                '}';
    }
}
```

### Step 3.3 — Create `BankAccount.java`

File: `src/main/java/com/skilora/model/entity/BankAccount.java`

```java
package com.skilora.model.entity;

import java.time.LocalDateTime;

public class BankAccount {
    private int id;
    private int userId;
    private String bankName;
    private String accountHolder;
    private String iban;
    private String swiftBic;
    private String rib;
    private String currency;
    private boolean isPrimary;
    private LocalDateTime createdDate;
    
    // Constructors
    public BankAccount() {
        this.currency = "TND";
        this.isPrimary = false;
    }
    
    public BankAccount(int userId, String bankName, String accountHolder) {
        this();
        this.userId = userId;
        this.bankName = bankName;
        this.accountHolder = accountHolder;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    
    public String getAccountHolder() { return accountHolder; }
    public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }
    
    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }
    
    public String getSwiftBic() { return swiftBic; }
    public void setSwiftBic(String swiftBic) { this.swiftBic = swiftBic; }
    
    public String getRib() { return rib; }
    public void setRib(String rib) { this.rib = rib; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public boolean isPrimary() { return isPrimary; }
    public void setPrimary(boolean primary) { isPrimary = primary; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    @Override
    public String toString() {
        return "BankAccount{" +
                "id=" + id +
                ", bankName='" + bankName + '\'' +
                ", accountHolder='" + accountHolder + '\'' +
                ", isPrimary=" + isPrimary +
                '}';
    }
}
```

### Step 3.4 — Create `SalaryHistory.java`

File: `src/main/java/com/skilora/model/entity/SalaryHistory.java`

```java
package com.skilora.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class SalaryHistory {
    private int id;
    private int contractId;
    private BigDecimal oldSalary;
    private BigDecimal newSalary;
    private String reason;
    private LocalDate effectiveDate;
    private LocalDateTime createdDate;
    
    // Constructors
    public SalaryHistory() {}
    
    public SalaryHistory(int contractId, BigDecimal oldSalary, BigDecimal newSalary, 
                        LocalDate effectiveDate, String reason) {
        this.contractId = contractId;
        this.oldSalary = oldSalary;
        this.newSalary = newSalary;
        this.effectiveDate = effectiveDate;
        this.reason = reason;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getContractId() { return contractId; }
    public void setContractId(int contractId) { this.contractId = contractId; }
    
    public BigDecimal getOldSalary() { return oldSalary; }
    public void setOldSalary(BigDecimal oldSalary) { this.oldSalary = oldSalary; }
    
    public BigDecimal getNewSalary() { return newSalary; }
    public void setNewSalary(BigDecimal newSalary) { this.newSalary = newSalary; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    
    @Override
    public String toString() {
        return "SalaryHistory{" +
                "id=" + id +
                ", oldSalary=" + oldSalary +
                ", newSalary=" + newSalary +
                ", effectiveDate=" + effectiveDate +
                '}';
    }
}
```

### Step 3.5 — Create `ExchangeRate.java`

File: `src/main/java/com/skilora/model/entity/ExchangeRate.java`

```java
package com.skilora.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ExchangeRate {
    private int id;
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal rate;
    private LocalDate rateDate;
    private String source;
    private LocalDateTime lastUpdated;
    
    // Constructors
    public ExchangeRate() {
        this.source = "BCT";
    }
    
    public ExchangeRate(String fromCurrency, String toCurrency, BigDecimal rate, LocalDate rateDate) {
        this();
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.rate = rate;
        this.rateDate = rateDate;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getFromCurrency() { return fromCurrency; }
    public void setFromCurrency(String fromCurrency) { this.fromCurrency = fromCurrency; }
    
    public String getToCurrency() { return toCurrency; }
    public void setToCurrency(String toCurrency) { this.toCurrency = toCurrency; }
    
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    
    public LocalDate getRateDate() { return rateDate; }
    public void setRateDate(LocalDate rateDate) { this.rateDate = rateDate; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    
    @Override
    public String toString() {
        return "ExchangeRate{" +
                fromCurrency + " -> " + toCurrency +
                ", rate=" + rate +
                ", date=" + rateDate +
                '}';
    }
}
```

### Step 3.6 — Create `PaymentTransaction.java`

File: `src/main/java/com/skilora/model/entity/PaymentTransaction.java`

```java
package com.skilora.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentTransaction {
    private int id;
    private Integer payslipId;
    private Integer fromAccountId;
    private Integer toAccountId;
    private BigDecimal amount;
    private String currency;
    private String transactionType;
    private String status;
    private String reference;
    private LocalDateTime transactionDate;
    private String notes;
    
    // Constructors
    public PaymentTransaction() {
        this.currency = "TND";
        this.transactionType = "SALARY";
        this.status = "PENDING";
    }
    
    public PaymentTransaction(Integer payslipId, BigDecimal amount, Integer toAccountId) {
        this();
        this.payslipId = payslipId;
        this.amount = amount;
        this.toAccountId = toAccountId;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public Integer getPayslipId() { return payslipId; }
    public void setPayslipId(Integer payslipId) { this.payslipId = payslipId; }
    
    public Integer getFromAccountId() { return fromAccountId; }
    public void setFromAccountId(Integer fromAccountId) { this.fromAccountId = fromAccountId; }
    
    public Integer getToAccountId() { return toAccountId; }
    public void setToAccountId(Integer toAccountId) { this.toAccountId = toAccountId; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    @Override
    public String toString() {
        return "PaymentTransaction{" +
                "id=" + id +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
```

### Step 3.7 — Create `TaxConfiguration.java`

File: `src/main/java/com/skilora/model/entity/TaxConfiguration.java`

```java
package com.skilora.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TaxConfiguration {
    private int id;
    private String country;
    private String taxType;
    private BigDecimal rate;
    private BigDecimal minBracket;
    private BigDecimal maxBracket;
    private String description;
    private LocalDate effectiveDate;
    private boolean isActive;
    
    // Constructors
    public TaxConfiguration() {
        this.isActive = true;
        this.minBracket = BigDecimal.ZERO;
    }
    
    public TaxConfiguration(String country, String taxType, BigDecimal rate, LocalDate effectiveDate) {
        this();
        this.country = country;
        this.taxType = taxType;
        this.rate = rate;
        this.effectiveDate = effectiveDate;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    
    public String getTaxType() { return taxType; }
    public void setTaxType(String taxType) { this.taxType = taxType; }
    
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    
    public BigDecimal getMinBracket() { return minBracket; }
    public void setMinBracket(BigDecimal minBracket) { this.minBracket = minBracket; }
    
    public BigDecimal getMaxBracket() { return maxBracket; }
    public void setMaxBracket(BigDecimal maxBracket) { this.maxBracket = maxBracket; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    @Override
    public String toString() {
        return "TaxConfiguration{" +
                "country='" + country + '\'' +
                ", taxType='" + taxType + '\'' +
                ", rate=" + rate +
                ", bracket=" + minBracket + "-" + maxBracket +
                '}';
    }
}
```

---

## PHASE 4: Services

### Step 4.1 — Create `ContractService.java`

File: `src/main/java/com/skilora/model/service/ContractService.java`

```java
package com.skilora.model.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.model.entity.EmploymentContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ContractService {
    private static final Logger logger = LoggerFactory.getLogger(ContractService.class);
    private static volatile ContractService instance;
    
    private ContractService() {}
    
    public static ContractService getInstance() {
        if (instance == null) {
            synchronized (ContractService.class) {
                if (instance == null) {
                    instance = new ContractService();
                }
            }
        }
        return instance;
    }
    
    public int create(EmploymentContract contract) {
        String sql = "INSERT INTO employment_contracts (user_id, employer_id, job_offer_id, " +
                "salary_base, currency, start_date, end_date, contract_type, status, pdf_url, " +
                "is_signed, signed_date, created_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, contract.getUserId());
            if (contract.getEmployerId() != null) {
                stmt.setInt(2, contract.getEmployerId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            if (contract.getJobOfferId() != null) {
                stmt.setInt(3, contract.getJobOfferId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            stmt.setBigDecimal(4, contract.getSalaryBase());
            stmt.setString(5, contract.getCurrency());
            stmt.setDate(6, Date.valueOf(contract.getStartDate()));
            if (contract.getEndDate() != null) {
                stmt.setDate(7, Date.valueOf(contract.getEndDate()));
            } else {
                stmt.setNull(7, Types.DATE);
            }
            stmt.setString(8, contract.getContractType());
            stmt.setString(9, contract.getStatus());
            stmt.setString(10, contract.getPdfUrl());
            stmt.setBoolean(11, contract.isSigned());
            if (contract.getSignedDate() != null) {
                stmt.setTimestamp(12, Timestamp.valueOf(contract.getSignedDate()));
            } else {
                stmt.setNull(12, Types.TIMESTAMP);
            }
            
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    logger.info("Contract created with ID: {}", id);
                    return id;
                }
            }
        } catch (SQLException e) {
            logger.error("Error creating contract", e);
        }
        return -1;
    }
    
    public EmploymentContract findById(int id) {
        String sql = "SELECT c.*, " +
                "u.username as user_name, u.email as user_email, " +
                "e.username as employer_name, " +
                "j.title as job_title " +
                "FROM employment_contracts c " +
                "LEFT JOIN users u ON c.user_id = u.id " +
                "LEFT JOIN users e ON c.employer_id = e.id " +
                "LEFT JOIN job_offers j ON c.job_offer_id = j.id " +
                "WHERE c.id = ?";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding contract by ID: {}", id, e);
        }
        return null;
    }
    
    public List<EmploymentContract> findByUserId(int userId) {
        String sql = "SELECT c.*, " +
                "u.username as user_name, " +
                "e.username as employer_name, " +
                "j.title as job_title " +
                "FROM employment_contracts c " +
                "LEFT JOIN users u ON c.user_id = u.id " +
                "LEFT JOIN users e ON c.employer_id = e.id " +
                "LEFT JOIN job_offers j ON c.job_offer_id = j.id " +
                "WHERE c.user_id = ? ORDER BY c.created_date DESC";
        
        return executeQuery(sql, userId);
    }
    
    public List<EmploymentContract> findByEmployerId(int employerId) {
        String sql = "SELECT c.*, " +
                "u.username as user_name, " +
                "e.username as employer_name, " +
                "j.title as job_title " +
                "FROM employment_contracts c " +
                "LEFT JOIN users u ON c.user_id = u.id " +
                "LEFT JOIN users e ON c.employer_id = e.id " +
                "LEFT JOIN job_offers j ON c.job_offer_id = j.id " +
                "WHERE c.employer_id = ? ORDER BY c.created_date DESC";
        
        return executeQuery(sql, employerId);
    }
    
    public EmploymentContract findActiveByUserId(int userId) {
        String sql = "SELECT c.*, " +
                "u.username as user_name, " +
                "e.username as employer_name, " +
                "j.title as job_title " +
                "FROM employment_contracts c " +
                "LEFT JOIN users u ON c.user_id = u.id " +
                "LEFT JOIN users e ON c.employer_id = e.id " +
                "LEFT JOIN job_offers j ON c.job_offer_id = j.id " +
                "WHERE c.user_id = ? AND c.status = 'ACTIVE' LIMIT 1";
        
        List<EmploymentContract> contracts = executeQuery(sql, userId);
        return contracts.isEmpty() ? null : contracts.get(0);
    }
    
    public List<EmploymentContract> findByStatus(String status) {
        String sql = "SELECT c.*, " +
                "u.username as user_name, " +
                "e.username as employer_name, " +
                "j.title as job_title " +
                "FROM employment_contracts c " +
                "LEFT JOIN users u ON c.user_id = u.id " +
                "LEFT JOIN users e ON c.employer_id = e.id " +
                "LEFT JOIN job_offers j ON c.job_offer_id = j.id " +
                "WHERE c.status = ? ORDER BY c.created_date DESC";
        
        List<EmploymentContract> contracts = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    contracts.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding contracts by status: {}", status, e);
        }
        return contracts;
    }
    
    public boolean update(EmploymentContract contract) {
        String sql = "UPDATE employment_contracts SET employer_id = ?, job_offer_id = ?, " +
                "salary_base = ?, currency = ?, start_date = ?, end_date = ?, " +
                "contract_type = ?, status = ?, pdf_url = ?, is_signed = ?, signed_date = ? " +
                "WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            if (contract.getEmployerId() != null) {
                stmt.setInt(1, contract.getEmployerId());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            if (contract.getJobOfferId() != null) {
                stmt.setInt(2, contract.getJobOfferId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setBigDecimal(3, contract.getSalaryBase());
            stmt.setString(4, contract.getCurrency());
            stmt.setDate(5, Date.valueOf(contract.getStartDate()));
            if (contract.getEndDate() != null) {
                stmt.setDate(6, Date.valueOf(contract.getEndDate()));
            } else {
                stmt.setNull(6, Types.DATE);
            }
            stmt.setString(7, contract.getContractType());
            stmt.setString(8, contract.getStatus());
            stmt.setString(9, contract.getPdfUrl());
            stmt.setBoolean(10, contract.isSigned());
            if (contract.getSignedDate() != null) {
                stmt.setTimestamp(11, Timestamp.valueOf(contract.getSignedDate()));
            } else {
                stmt.setNull(11, Types.TIMESTAMP);
            }
            stmt.setInt(12, contract.getId());
            
            int rows = stmt.executeUpdate();
            logger.info("Contract updated: {}, rows affected: {}", contract.getId(), rows);
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Error updating contract: {}", contract.getId(), e);
            return false;
        }
    }
    
    public boolean sign(int id) {
        String sql = "UPDATE employment_contracts SET is_signed = TRUE, " +
                "signed_date = NOW(), status = 'ACTIVE' WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            logger.info("Contract signed: {}, rows affected: {}", id, rows);
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Error signing contract: {}", id, e);
            return false;
        }
    }
    
    public boolean terminate(int id) {
        String sql = "UPDATE employment_contracts SET status = 'TERMINATED' WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            logger.info("Contract terminated: {}, rows affected: {}", id, rows);
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Error terminating contract: {}", id, e);
            return false;
        }
    }
    
    public boolean delete(int id) {
        // Only allow deletion of DRAFT contracts
        String sql = "DELETE FROM employment_contracts WHERE id = ? AND status = 'DRAFT'";
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            logger.info("Contract deleted: {}, rows affected: {}", id, rows);
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Error deleting contract: {}", id, e);
            return false;
        }
    }
    
    private List<EmploymentContract> executeQuery(String sql, int param) {
        List<EmploymentContract> contracts = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, param);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    contracts.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing query", e);
        }
        return contracts;
    }
    
    private EmploymentContract mapResultSet(ResultSet rs) throws SQLException {
        EmploymentContract contract = new EmploymentContract();
        contract.setId(rs.getInt("id"));
        contract.setUserId(rs.getInt("user_id"));
        
        int employerId = rs.getInt("employer_id");
        contract.setEmployerId(rs.wasNull() ? null : employerId);
        
        int jobOfferId = rs.getInt("job_offer_id");
        contract.setJobOfferId(rs.wasNull() ? null : jobOfferId);
        
        contract.setSalaryBase(rs.getBigDecimal("salary_base"));
        contract.setCurrency(rs.getString("currency"));
        contract.setStartDate(rs.getDate("start_date").toLocalDate());
        
        Date endDate = rs.getDate("end_date");
        contract.setEndDate(endDate != null ? endDate.toLocalDate() : null);
        
        contract.setContractType(rs.getString("contract_type"));
        contract.setStatus(rs.getString("status"));
        contract.setPdfUrl(rs.getString("pdf_url"));
        contract.setSigned(rs.getBoolean("is_signed"));
        
        Timestamp signedDate = rs.getTimestamp("signed_date");
        contract.setSignedDate(signedDate != null ? signedDate.toLocalDateTime() : null);
        
        Timestamp createdDate = rs.getTimestamp("created_date");
        contract.setCreatedDate(createdDate != null ? createdDate.toLocalDateTime() : null);
        
        // Transient fields
        contract.setUserName(rs.getString("user_name"));
        contract.setEmployerName(rs.getString("employer_name"));
        contract.setJobTitle(rs.getString("job_title"));
        
        return contract;
    }
}
```

### Step 4.2 — Create `PayslipService.java`

Singleton CRUD + calculation:
- `int create(Payslip p)` — INSERT
- `Payslip findById(int id)`
- `List<Payslip> findByContractId(int contractId)` — ORDER BY period_year DESC, period_month DESC
- `List<Payslip> findByUserId(int userId)` — all payslips for user
- `Payslip findByPeriod(int contractId, int month, int year)` — single payslip
- `boolean updatePaymentStatus(int id, String status)` — mark as paid
- `boolean delete(int id)` — only if PENDING
- `Payslip generatePayslip(int contractId, int month, int year)` — **BUSINESS LOGIC:**
  1. Get contract → get salaryBase (gross)
  2. Calculate CNSS employee (9.18% of gross)
  3. Calculate CNSS employer (16.57% of gross)
  4. Calculate IRPP using progressive brackets from TaxConfiguration:
     - Annual gross = monthly gross × 12
     - Apply progressive rates per bracket (0%, 26%, 28%, 32%, 35%)
     - Monthly IRPP = Annual IRPP ÷ 12
  5. Net = Gross - CNSS employee - IRPP - other deductions + bonuses
  6. INSERT and return the generated Payslip
  
**IRPP Calculation Example:**
```
Annual salary = 30,000 TND
- 0% on first 5,000 = 0
- 26% on next 15,000 (5,001-20,000) = 3,900
- 28% on remaining 10,000 (20,001-30,000) = 2,800
Total annual IRPP = 6,700 TND
Monthly IRPP = 558.33 TND
```

### Step 4.3 — Create `BankAccountService.java`

File: `src/main/java/com/skilora/model/service/BankAccountService.java`

Singleton CRUD:
- `int create(BankAccount account)` — INSERT, validate IBAN/RIB with ValidationUtils
- `BankAccount findById(int id)`
- `List<BankAccount> findByUserId(int userId)`
- `BankAccount findPrimaryByUserId(int userId)` — WHERE is_primary = TRUE
- `boolean update(BankAccount account)`
- `boolean setPrimary(int id, int userId)` — unset all others for user, set this one
- `boolean delete(int id)`

**Implementation Note:** Use `ValidationUtils.isValidIban()` and `isValidRib()` before inserting.

### Step 4.4 — Create `SalaryHistoryService.java`

File: `src/main/java/com/skilora/model/service/SalaryHistoryService.java`

Singleton CRUD:
- `int log(SalaryHistory entry)` — INSERT
- `List<SalaryHistory> findByContractId(int contractId)` — ORDER BY effective_date DESC

**Auto-logging:** When ContractService.update() changes salaryBase, automatically log to salary_history.

### Step 4.5 — Create `ExchangeRateService.java`

File: `src/main/java/com/skilora/model/service/ExchangeRateService.java`

Singleton CRUD + conversion:
- `int save(ExchangeRate rate)` — INSERT ON DUPLICATE KEY UPDATE (upsert by from+to+date)
- `ExchangeRate getRate(String from, String to)` — latest rate
- `ExchangeRate getRate(String from, String to, LocalDate date)` — rate for specific date
- `BigDecimal convert(BigDecimal amount, String from, String to)` — amount × rate
- `List<ExchangeRate> getHistory(String from, String to, int days)` — last N days

**Seed default rates** in DatabaseInitializer if table is empty:
  - EUR → TND: 3.40
  - USD → TND: 3.15
  - GBP → TND: 3.95
  - TND → EUR: 0.2941
  - TND → USD: 0.3175

### Step 4.6 — Create `PaymentTransactionService.java`

File: `src/main/java/com/skilora/model/service/PaymentTransactionService.java`

Singleton CRUD:
- `int create(PaymentTransaction tx)` — INSERT
- `PaymentTransaction findById(int id)`
- `List<PaymentTransaction> findByPayslipId(int payslipId)`
- `List<PaymentTransaction> findByUserId(int userId)` — via payslip → contract → user
- `boolean updateStatus(int id, String status)`
- `BigDecimal getTotalPaidByUser(int userId)` — SUM of PAID transactions

### Step 4.7 — Create `TaxConfigurationService.java`

File: `src/main/java/com/skilora/model/service/TaxConfigurationService.java`

Singleton CRUD + tax calculation:
- `int create(TaxConfiguration tc)` — INSERT
- `List<TaxConfiguration> findByCountry(String country)` — active configs only
- `List<TaxConfiguration> findByCountryAndType(String country, String taxType)` — for IRPP, CNSS
- `boolean update(TaxConfiguration tc)`
- `boolean delete(int id)`
- `BigDecimal calculateIRPP(BigDecimal annualSalary, String country)` — **Progressive calculation:**
  ```
  1. Get all IRPP tax brackets for country ordered by minBracket ASC
  2. For each bracket, calculate tax on the portion of salary in that bracket
  3. Sum all bracket taxes
  ```

**Seed Tunisian tax brackets** in DatabaseInitializer if empty:
  - IRPP 0% for 0-5000 TND
  - IRPP 26% for 5001-20000 TND
  - IRPP 28% for 20001-30000 TND
  - IRPP 32% for 30001-50000 TND
  - IRPP 35% for 50001+ TND
  - CNSS Employee: 9.18%
  - CNSS Employer: 16.57%

---

## PHASE 5: Business Rules & Validation

### Step 5.1 — Contract Business Rules

1. **Status Transitions:**
   - DRAFT → PENDING_SIGNATURE → ACTIVE
   - ACTIVE → TERMINATED or EXPIRED
   - Cannot go back from ACTIVE to DRAFT
   
2. **Validation:**
   - salaryBase must be > 0
   - startDate cannot be in the past (more than 30 days)
   - endDate must be after startDate (for CDD)
   - CDI contracts should have NULL endDate
   - User cannot have multiple ACTIVE contracts

3. **Auto-expiration:**
   - Check CDD contracts daily: if endDate < TODAY, set status = 'EXPIRED'

### Step 5.2 — Payslip Business Rules

1. **Uniqueness:**
   - One payslip per contract per month/year (enforced by UNIQUE constraint)
   
2. **Calculation Rules:**
   - CNSS employee = 9.18% of gross (Tunisia)
   - CNSS employer = 16.57% of gross (Tunisia)
   - IRPP = progressive based on annual salary
   - Net = Gross - CNSS_employee - IRPP - otherDeductions + bonuses
   
3. **Payment Status Flow:**
   - PENDING → PROCESSING → PAID
   - PENDING → FAILED → PENDING (retry)
   - Cannot delete if status = PAID

### Step 5.3 — Bank Account Business Rules

1. **Primary Account:**
   - User must have at least one primary account
   - Only one primary account per user
   - Setting new primary auto-unsets others
   
2. **Validation:**
   - IBAN: alphanumeric, starts with 2-letter country code
   - RIB: 20 digits for Tunisia
   - Account holder name required

### Step 5.4 — Error Handling Pattern

All services should handle exceptions consistently:

```java
try {
    // JDBC operations
} catch (SQLException e) {
    logger.error("Error description with context", e);
    return null; // or false, or -1 depending on method return type
}
```

Controllers should catch service exceptions and show user-friendly alerts:

```java
Task<Void> task = new Task<>() {
    @Override
    protected Void call() {
        try {
            service.operation();
        } catch (Exception e) {
            Platform.runLater(() -> 
                TLAlert.error(I18n.get("error.db.operation"), e.getMessage())
            );
        }
        return null;
    }
};
```

---

## PHASE 6: Controllers & Views

### Step 6.1 — Create `FinanceController.java`

File: `src/main/java/com/skilora/controller/FinanceController.java`

Main finance view for employees:
- **Tab-based interface:** "Contrat" | "Bulletins de Paie" | "Comptes Bancaires" | "Historique"
- **Contract tab:**
  - Display active contract details (salary, type, dates, status)
  - Sign button if unsigned
  - Download PDF button
- **Payslips tab:**
  - List of payslips by period (month/year)
  - Click to expand details (gross, deductions, net breakdown)
  - Download PDF button per payslip
  - Currency converter: show salary in TND and EUR side by side
- **Bank Accounts tab:**
  - List accounts
  - Add new account form (bank name, IBAN, SWIFT, RIB)
  - Set primary account
  - Delete account
- **History tab:**
  - Salary change history
  - Payment transaction history

**Implementation Template:**

```java
package com.skilora.controller;

import com.skilora.framework.components.*;
import com.skilora.model.entity.*;
import com.skilora.model.service.*;
import com.skilora.utils.I18n;
import com.skilora.utils.SessionManager;
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

import java.net.URL;
import java.util.*;

public class FinanceController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(FinanceController.class);
    
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private HBox tabBox;
    @FXML private VBox contentPane;
    
    private int currentUserId;
    private String currentTab = "contract";
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentUserId = SessionManager.getCurrentUser().getId();
        
        titleLabel.setText(I18n.get("finance.title"));
        subtitleLabel.setText(I18n.get("finance.subtitle"));
        
        // Create tab buttons
        createTabs();
        
        // Load default tab
        showContractTab();
    }
    
    private void createTabs() {
        TLButton contractBtn = new TLButton(I18n.get("finance.tab.contract"));
        TLButton payslipsBtn = new TLButton(I18n.get("finance.tab.payslips"));
        TLButton bankBtn = new TLButton(I18n.get("finance.tab.bank"));
        TLButton historyBtn = new TLButton(I18n.get("finance.tab.history"));
        
        contractBtn.setOnAction(e -> showContractTab());
        payslipsBtn.setOnAction(e -> showPayslipsTab());
        bankBtn.setOnAction(e -> showBankTab());
        historyBtn.setOnAction(e -> showHistoryTab());
        
        tabBox.getChildren().addAll(contractBtn, payslipsBtn, bankBtn, historyBtn);
    }
    
    private void showContractTab() {
        currentTab = "contract";
        contentPane.getChildren().clear();
        
        Task<EmploymentContract> task = new Task<>() {
            @Override
            protected EmploymentContract call() {
                return ContractService.getInstance().findActiveByUserId(currentUserId);
            }
        };
        
        task.setOnSucceeded(e -> {
            EmploymentContract contract = task.getValue();
            Platform.runLater(() -> displayContract(contract));
        });
        
        task.setOnFailed(e -> {
            logger.error("Failed to load contract", task.getException());
            Platform.runLater(() -> 
                TLAlert.error(I18n.get("error.db.connection"), task.getException().getMessage())
            );
        });
        
        new Thread(task).start();
    }
    
    private void displayContract(EmploymentContract contract) {
        if (contract == null) {
            Label emptyLabel = new Label(I18n.get("contract.empty"));
            contentPane.getChildren().add(emptyLabel);
            return;
        }
        
        TLCard card = new TLCard();
        VBox cardContent = new VBox(16);
        cardContent.setPadding(new Insets(24));
        
        // Contract details
        cardContent.getChildren().addAll(
            createDetailRow(I18n.get("contract.type"), I18n.get("contract.type." + contract.getContractType().toLowerCase())),
            createDetailRow(I18n.get("contract.salary"), contract.getSalaryBase() + " " + contract.getCurrency()),
            createDetailRow(I18n.get("contract.start_date"), contract.getStartDate().toString()),
            createDetailRow(I18n.get("contract.status"), I18n.get("contract.status." + contract.getStatus().toLowerCase()))
        );
        
        // Sign button if not signed
        if (!contract.isSigned() && "PENDING_SIGNATURE".equals(contract.getStatus())) {
            TLButton signBtn = new TLButton(I18n.get("contract.sign"));
            signBtn.setOnAction(e -> signContract(contract.getId()));
            cardContent.getChildren().add(signBtn);
        }
        
        card.setContent(cardContent);
        contentPane.getChildren().add(card);
    }
    
    private HBox createDetailRow(String label, String value) {
        Label labelNode = new Label(label + ":");
        labelNode.getStyleClass().add("text-muted");
        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("text-bold");
        
        HBox row = new HBox(8, labelNode, valueNode);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
    
    private void signContract(int contractId) {
        TLDialog.confirm(
            I18n.get("contract.confirm.sign"),
            () -> {
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() {
                        return ContractService.getInstance().sign(contractId);
                    }
                };
                
                task.setOnSucceeded(e -> {
                    if (task.getValue()) {
                        Platform.runLater(() -> {
                            TLAlert.success(I18n.get("contract.success.signed"));
                            showContractTab(); // Refresh
                        });
                    }
                });
                
                new Thread(task).start();
            }
        );
    }
    
    private void showPayslipsTab() {
        currentTab = "payslips";
        contentPane.getChildren().clear();
        
        Task<List<Payslip>> task = new Task<>() {
            @Override
            protected List<Payslip> call() {
                return PayslipService.getInstance().findByUserId(currentUserId);
            }
        };
        
        task.setOnSucceeded(e -> {
            List<Payslip> payslips = task.getValue();
            Platform.runLater(() -> displayPayslips(payslips));
        });
        
        new Thread(task).start();
    }
    
    private void displayPayslips(List<Payslip> payslips) {
        if (payslips.isEmpty()) {
            Label emptyLabel = new Label(I18n.get("payslip.empty"));
            contentPane.getChildren().add(emptyLabel);
            return;
        }
        
        for (Payslip payslip : payslips) {
            TLCard card = createPayslipCard(payslip);
            contentPane.getChildren().add(card);
        }
    }
    
    private TLCard createPayslipCard(Payslip payslip) {
        TLCard card = new TLCard();
        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        
        // Period header
        Label periodLabel = new Label(payslip.getPeriodMonth() + "/" + payslip.getPeriodYear());
        periodLabel.getStyleClass().add("h3");
        
        // Salary details
        content.getChildren().addAll(
            periodLabel,
            createDetailRow(I18n.get("payslip.gross"), payslip.getGrossSalary() + " " + payslip.getCurrency()),
            createDetailRow(I18n.get("payslip.cnss_employee"), "-" + payslip.getCnssEmployee() + " " + payslip.getCurrency()),
            createDetailRow(I18n.get("payslip.irpp"), "-" + payslip.getIrpp() + " " + payslip.getCurrency()),
            createDetailRow(I18n.get("payslip.net"), payslip.getNetSalary() + " " + payslip.getCurrency())
        );
        
        TLBadge statusBadge = new TLBadge(I18n.get("payslip.status." + payslip.getPaymentStatus().toLowerCase()));
        content.getChildren().add(statusBadge);
        
        card.setContent(content);
        return card;
    }
    
    private void showBankTab() {
        currentTab = "bank";
        contentPane.getChildren().clear();
        // TODO: Implement bank accounts tab
    }
    
    private void showHistoryTab() {
        currentTab = "history";
        contentPane.getChildren().clear();
        // TODO: Implement history tab
    }
}
```

### Step 6.2 — Create `FinanceView.fxml`

```xml
<VBox spacing="24" style="-fx-background-color: transparent;">
    <padding><Insets top="32" right="32" bottom="32" left="32"/></padding>

    <HBox spacing="16" alignment="CENTER_LEFT">
        <VBox spacing="4">
            <Label fx:id="titleLabel" text="Finances" styleClass="h2"/>
            <Label fx:id="subtitleLabel" styleClass="text-muted"/>
        </VBox>
        <Region HBox.hgrow="ALWAYS"/>
    </HBox>

    <HBox fx:id="tabBox" spacing="8" alignment="CENTER_LEFT"/>
    <VBox fx:id="contentPane" VBox.vgrow="ALWAYS"/>
</VBox>
```

### Step 6.3 — Create `FinanceAdminController.java`

Admin/employer view:
- **Contracts Management:** Create, view, sign contracts for employees
- **Payroll Generation:** Generate payslips for all employees for a given month
- **Payment Processing:** Mark payslips as paid, create payment transactions
- **Exchange Rates:** View and update exchange rates
- **Tax Configuration:** View and edit tax brackets (admin only)
- **Reports:** Total payroll, average salary, department breakdowns

### Step 6.4 — Create `FinanceAdminView.fxml`

Similar tabbed layout for admin features.

---

## PHASE 7: Navigation Integration

### Step 7.1 — Add to `MainView.java`

1. Add sidebar nav button: `createNavButton(I18n.get("nav.finance"), SVG_FINANCE_ICON, this::showFinanceView)`
2. Implement `showFinanceView()` — load FinanceView for employees, FinanceAdminView for admin/employer
3. Add cached view field

SVG icon for finance:
```
M12 1v22M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6
```

---

## PHASE 8: I18n Keys

### Step 8.1 — Add to all 4 resource bundles

**Default & French (messages.properties AND messages_fr.properties):**

*Note: The default bundle (messages.properties) should contain the same keys as French. Copy all French keys to both files.*

```properties
# Finance Module
nav.finance=Finances
finance.title=Finances & Rémunération
finance.subtitle=Gérez vos contrats et bulletins de paie

# Contract
finance.tab.contract=Contrat
finance.tab.payslips=Bulletins de Paie
finance.tab.bank=Comptes Bancaires
finance.tab.history=Historique
contract.title=Contrat d'emploi
contract.type=Type de contrat
contract.type.cdi=CDI - Contrat à durée indéterminée
contract.type.cdd=CDD - Contrat à durée déterminée
contract.type.freelance=Freelance
contract.type.stage=Stage
contract.salary=Salaire de base
contract.start_date=Date de début
contract.end_date=Date de fin
contract.status=Statut
contract.status.draft=Brouillon
contract.status.pending=En attente de signature
contract.status.active=Actif
contract.status.expired=Expiré
contract.status.terminated=Résilié
contract.sign=Signer le contrat
contract.download=Télécharger PDF
contract.new=Nouveau contrat
contract.create=Créer un contrat
contract.confirm.sign=Êtes-vous sûr de vouloir signer ce contrat ?
contract.success.signed=Contrat signé avec succès
contract.success.created=Contrat créé avec succès
contract.empty=Aucun contrat

# Payslip
payslip.title=Bulletin de paie
payslip.period=Période
payslip.gross=Salaire brut
payslip.net=Salaire net
payslip.cnss_employee=CNSS (employé)
payslip.cnss_employer=CNSS (employeur)
payslip.irpp=IRPP
payslip.deductions=Déductions
payslip.bonuses=Primes
payslip.status=Statut paiement
payslip.status.pending=En attente
payslip.status.processing=En cours
payslip.status.paid=Payé
payslip.status.failed=Échoué
payslip.generate=Générer bulletin
payslip.download=Télécharger
payslip.empty=Aucun bulletin de paie
payslip.mark_paid=Marquer comme payé

# Bank Account
bank.title=Comptes bancaires
bank.name=Nom de la banque
bank.holder=Titulaire
bank.iban=IBAN
bank.swift=SWIFT/BIC
bank.rib=RIB
bank.primary=Compte principal
bank.set_primary=Définir comme principal
bank.add=Ajouter un compte
bank.delete=Supprimer
bank.confirm.delete=Êtes-vous sûr de vouloir supprimer ce compte ?
bank.success.added=Compte ajouté avec succès
bank.success.deleted=Compte supprimé
bank.empty=Aucun compte bancaire

# Exchange
exchange.title=Taux de change
exchange.from=De
exchange.to=Vers
exchange.rate=Taux
exchange.date=Date
exchange.convert=Convertir
exchange.result={0} {1} = {2} {3}

# Tax
tax.title=Configuration fiscale
tax.country=Pays
tax.type=Type
tax.rate=Taux
tax.bracket=Tranche
tax.min=Minimum
tax.max=Maximum

# History
history.salary=Historique des salaires
history.payments=Historique des paiements
history.old_salary=Ancien salaire
history.new_salary=Nouveau salaire
history.reason=Raison
history.date=Date

# Admin
finance.admin.title=Administration Financière
finance.admin.subtitle=Gérer les salaires et les contrats
finance.admin.tab.contracts=Gestion des Contrats
finance.admin.tab.payroll=Génération de Paie
finance.admin.tab.payments=Traitement des Paiements
finance.admin.tab.rates=Taux de Change
finance.admin.tab.taxes=Configuration Fiscale
finance.admin.tab.reports=Rapports

# Admin - Contracts
contract.admin.create=Créer un Contrat
contract.admin.employee=Employé
contract.admin.select_employee=Sélectionner un Employé
contract.admin.job_offer=Offre d'Emploi
contract.admin.list=Tous les Contrats

# Admin - Payroll
payroll.generate_for=Générer pour
payroll.month=Mois
payroll.year=Année
payroll.generate_all=Générer pour Tous les Employés
payroll.confirm.generate=Générer les bulletins pour {0} {1} ?
payroll.success.generated=Bulletins générés : {0}
payroll.total_amount=Montant Total

# Admin - Reports
reports.total_payroll=Masse Salariale Totale
reports.average_salary=Salaire Moyen
reports.employee_count=Employés Actifs
reports.period=Période
reports.export=Exporter le Rapport

# Validation Errors
error.validation.required={0} est requis
error.validation.invalid_email=Format d'email invalide
error.validation.invalid_iban=Format IBAN invalide
error.validation.invalid_date=Date invalide
error.validation.date_range=La date de fin doit être après la date de début
error.validation.positive_amount=Le montant doit être positif
error.validation.currency=Devise invalide

# Database Errors
error.db.connection=Échec de la connexion à la base de données
error.db.create=Échec de la création de {0}
error.db.update=Échec de la mise à jour de {0}
error.db.delete=Échec de la suppression de {0}
error.db.notfound={0} introuvable

# General Messages
message.confirm.action=Êtes-vous sûr ?
message.success=Opération réussie
message.error=Opération échouée
message.loading=Chargement...
message.no_data=Aucune donnée disponible
```

**English (messages_en.properties):**
```properties
nav.finance=Finance
finance.title=Finance & Payroll
finance.subtitle=Manage your contracts and payslips

finance.tab.contract=Contract
finance.tab.payslips=Payslips
finance.tab.bank=Bank Accounts
finance.tab.history=History
contract.title=Employment Contract
contract.type=Contract Type
contract.type.cdi=Permanent Contract
contract.type.cdd=Fixed-term Contract
contract.type.freelance=Freelance
contract.type.stage=Internship
contract.salary=Base Salary
contract.start_date=Start Date
contract.end_date=End Date
contract.status=Status
contract.status.draft=Draft
contract.status.pending=Pending Signature
contract.status.active=Active
contract.status.expired=Expired
contract.status.terminated=Terminated
contract.sign=Sign Contract
contract.download=Download PDF
contract.new=New Contract
contract.create=Create Contract
contract.confirm.sign=Are you sure you want to sign this contract?
contract.success.signed=Contract signed successfully
contract.success.created=Contract created successfully
contract.empty=No contracts

payslip.title=Payslip
payslip.period=Period
payslip.gross=Gross Salary
payslip.net=Net Salary
payslip.cnss_employee=CNSS (employee)
payslip.cnss_employer=CNSS (employer)
payslip.irpp=Income Tax
payslip.deductions=Deductions
payslip.bonuses=Bonuses
payslip.status=Payment Status
payslip.status.pending=Pending
payslip.status.processing=Processing
payslip.status.paid=Paid
payslip.status.failed=Failed
payslip.generate=Generate Payslip
payslip.download=Download
payslip.empty=No payslips
payslip.mark_paid=Mark as Paid

bank.title=Bank Accounts
bank.name=Bank Name
bank.holder=Account Holder
bank.iban=IBAN
bank.swift=SWIFT/BIC
bank.rib=RIB
bank.primary=Primary Account
bank.set_primary=Set as Primary
bank.add=Add Account
bank.delete=Delete
bank.confirm.delete=Are you sure you want to delete this account?
bank.success.added=Account added successfully
bank.success.deleted=Account deleted
bank.empty=No bank accounts

exchange.title=Exchange Rates
exchange.from=From
exchange.to=To
exchange.rate=Rate
exchange.date=Date
exchange.convert=Convert
exchange.result={0} {1} = {2} {3}

tax.title=Tax Configuration
tax.country=Country
tax.type=Type
tax.rate=Rate
tax.bracket=Bracket
tax.min=Minimum
tax.max=Maximum

history.salary=Salary History
history.payments=Payment History
history.old_salary=Old Salary
history.new_salary=New Salary
history.reason=Reason
history.date=Date

# Admin
finance.admin.title=Finance Administration
finance.admin.subtitle=Manage payroll and contracts
finance.admin.tab.contracts=Contract Management
finance.admin.tab.payroll=Payroll Generation
finance.admin.tab.payments=Payment Processing
finance.admin.tab.rates=Exchange Rates
finance.admin.tab.taxes=Tax Configuration
finance.admin.tab.reports=Reports

# Admin - Contracts
contract.admin.create=Create Contract
contract.admin.employee=Employee
contract.admin.select_employee=Select Employee
contract.admin.job_offer=Job Offer
contract.admin.list=All Contracts

# Admin - Payroll
payroll.generate_for=Generate for
payroll.month=Month
payroll.year=Year
payroll.generate_all=Generate for All Employees
payroll.confirm.generate=Generate payslips for {0} {1}?
payroll.success.generated=Payslips generated: {0}
payroll.total_amount=Total Amount

# Admin - Reports
reports.total_payroll=Total Payroll
reports.average_salary=Average Salary
reports.employee_count=Active Employees
reports.period=Period
reports.export=Export Report

# Validation Errors
error.validation.required={0} is required
error.validation.invalid_email=Invalid email format
error.validation.invalid_iban=Invalid IBAN format
error.validation.invalid_date=Invalid date
error.validation.date_range=End date must be after start date
error.validation.positive_amount=Amount must be positive
error.validation.currency=Invalid currency

# Database Errors
error.db.connection=Database connection failed
error.db.create=Failed to create {0}
error.db.update=Failed to update {0}
error.db.delete=Failed to delete {0}
error.db.notfound={0} not found

# General Messages
message.confirm.action=Are you sure?
message.success=Operation successful
message.error=Operation failed
message.loading=Loading...
message.no_data=No data available
```

**Arabic (messages_ar.properties):**
```properties
nav.finance=المالية
finance.title=المالية والرواتب
finance.subtitle=إدارة عقودك وكشوف الرواتب

finance.tab.contract=العقد
finance.tab.payslips=كشوف الرواتب
finance.tab.bank=الحسابات البنكية
finance.tab.history=السجل
contract.title=عقد العمل
contract.type=نوع العقد
contract.type.cdi=عقد دائم
contract.type.cdd=عقد محدد المدة
contract.type.freelance=عمل حر
contract.type.stage=تربّص
contract.salary=الراتب الأساسي
contract.start_date=تاريخ البدء
contract.end_date=تاريخ الانتهاء
contract.status=الحالة
contract.status.draft=مسودة
contract.status.pending=في انتظار التوقيع
contract.status.active=نشط
contract.status.expired=منتهي
contract.status.terminated=مُنهى
contract.sign=توقيع العقد
contract.download=تحميل PDF
contract.new=عقد جديد
contract.create=إنشاء عقد
contract.confirm.sign=هل أنت متأكد من توقيع هذا العقد؟
contract.success.signed=تم توقيع العقد بنجاح
contract.success.created=تم إنشاء العقد بنجاح
contract.empty=لا توجد عقود

payslip.title=كشف الراتب
payslip.period=الفترة
payslip.gross=الراتب الإجمالي
payslip.net=الراتب الصافي
payslip.cnss_employee=الضمان الاجتماعي (الموظف)
payslip.cnss_employer=الضمان الاجتماعي (صاحب العمل)
payslip.irpp=ضريبة الدخل
payslip.deductions=الاقتطاعات
payslip.bonuses=المنح
payslip.status=حالة الدفع
payslip.status.pending=في الانتظار
payslip.status.processing=قيد المعالجة
payslip.status.paid=مدفوع
payslip.status.failed=فشل
payslip.generate=إنشاء كشف الراتب
payslip.download=تحميل
payslip.empty=لا توجد كشوف رواتب
payslip.mark_paid=تعيين كمدفوع

bank.title=الحسابات البنكية
bank.name=اسم البنك
bank.holder=صاحب الحساب
bank.iban=IBAN
bank.swift=SWIFT/BIC
bank.rib=RIB
bank.primary=الحساب الرئيسي
bank.set_primary=تعيين كرئيسي
bank.add=إضافة حساب
bank.delete=حذف
bank.confirm.delete=هل أنت متأكد من حذف هذا الحساب؟
bank.success.added=تمت إضافة الحساب بنجاح
bank.success.deleted=تم حذف الحساب
bank.empty=لا توجد حسابات بنكية

exchange.title=أسعار الصرف
exchange.from=من
exchange.to=إلى
exchange.rate=السعر
exchange.date=التاريخ
exchange.convert=تحويل
exchange.result={0} {1} = {2} {3}

tax.title=الإعداد الضريبي
tax.country=البلد
tax.type=النوع
tax.rate=النسبة
tax.bracket=الشريحة
tax.min=الحد الأدنى
tax.max=الحد الأقصى

history.salary=سجل الرواتب
history.payments=سجل المدفوعات
history.old_salary=الراتب القديم
history.new_salary=الراتب الجديد
history.reason=السبب
history.date=التاريخ

# Admin
finance.admin.title=إدارة المالية
finance.admin.subtitle=إدارة الرواتب والعقود
finance.admin.tab.contracts=إدارة العقود
finance.admin.tab.payroll=إنشاء الرواتب
finance.admin.tab.payments=معالجة المدفوعات
finance.admin.tab.rates=أسعار الصرف
finance.admin.tab.taxes=الإعداد الضريبي
finance.admin.tab.reports=التقارير

# Admin - Contracts
contract.admin.create=إنشاء عقد
contract.admin.employee=الموظف
contract.admin.select_employee=اختر موظف
contract.admin.job_offer=عرض العمل
contract.admin.list=جميع العقود

# Admin - Payroll
payroll.generate_for=إنشاء لـ
payroll.month=الشهر
payroll.year=السنة
payroll.generate_all=إنشاء لجميع الموظفين
payroll.confirm.generate=إنشاء كشوف رواتب لـ {0} {1}؟
payroll.success.generated=تم إنشاء كشوف الرواتب: {0}
payroll.total_amount=المبلغ الإجمالي

# Admin - Reports
reports.total_payroll=إجمالي الرواتب
reports.average_salary=متوسط الراتب
reports.employee_count=الموظفون النشطون
reports.period=الفترة
reports.export=تصدير التقرير

# Validation Errors
error.validation.required={0} مطلوب
error.validation.invalid_email=صيغة البريد الإلكتروني غير صالحة
error.validation.invalid_iban=صيغة IBAN غير صالحة
error.validation.invalid_date=تاريخ غير صالح
error.validation.date_range=تاريخ الانتهاء يجب أن يكون بعد تاريخ البدء
error.validation.positive_amount=المبلغ يجب أن يكون موجباً
error.validation.currency=عملة غير صالحة

# Database Errors
error.db.connection=فشل الاتصال بقاعدة البيانات
error.db.create=فشل في إنشاء {0}
error.db.update=فشل في تحديث {0}
error.db.delete=فشل في حذف {0}
error.db.notfound={0} غير موجود

# General Messages
message.confirm.action=هل أنت متأكد؟
message.success=نجحت العملية
message.error=فشلت العملية
message.loading=جاري التحميل...
message.no_data=لا توجد بيانات
```

---

## PHASE 9: Seed Data

### Step 9.1 — Seed tax configurations

In `DatabaseInitializer`, after creating `tax_configurations`, if table is empty:

```sql
INSERT INTO tax_configurations (country, tax_type, rate, min_bracket, max_bracket, description, effective_date, is_active) VALUES
('Tunisia', 'IRPP', 0.00, 0.00, 5000.00, 'Tranche exonérée', '2025-01-01', TRUE),
('Tunisia', 'IRPP', 0.26, 5000.01, 20000.00, 'Tranche 26%', '2025-01-01', TRUE),
('Tunisia', 'IRPP', 0.28, 20000.01, 30000.00, 'Tranche 28%', '2025-01-01', TRUE),
('Tunisia', 'IRPP', 0.32, 30000.01, 50000.00, 'Tranche 32%', '2025-01-01', TRUE),
('Tunisia', 'IRPP', 0.35, 50000.01, NULL, 'Tranche 35%', '2025-01-01', TRUE),
('Tunisia', 'CNSS_EMPLOYEE', 0.0918, 0.00, NULL, 'CNSS part salariale 9.18%', '2025-01-01', TRUE),
('Tunisia', 'CNSS_EMPLOYER', 0.1657, 0.00, NULL, 'CNSS part patronale 16.57%', '2025-01-01', TRUE);
```

### Step 9.2 — Seed default exchange rates

```sql
INSERT INTO exchange_rates (from_currency, to_currency, rate, rate_date, source) VALUES
('EUR', 'TND', 3.4000, CURDATE(), 'BCT'),
('USD', 'TND', 3.1500, CURDATE(), 'BCT'),
('GBP', 'TND', 3.9500, CURDATE(), 'BCT'),
('TND', 'EUR', 0.2941, CURDATE(), 'BCT'),
('TND', 'USD', 0.3175, CURDATE(), 'BCT');
```

---

## PHASE 10: Entity Validation & PDF Generation

### Step 10.1 — Add Validation Utility

Create `src/main/java/com/skilora/utils/ValidationUtils.java`:

```java
package com.skilora.utils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;

public class ValidationUtils {
    private static final Pattern IBAN_PATTERN = Pattern.compile("^[A-Z]{2}\\d{2}[A-Z0-9]{1,30}$");
    private static final Pattern RIB_PATTERN = Pattern.compile("^\\d{20}$");
    
    public static boolean isValidIban(String iban) {
        return iban != null && IBAN_PATTERN.matcher(iban.replaceAll("\\s", "")).matches();
    }
    
    public static boolean isValidRib(String rib) {
        return rib != null && RIB_PATTERN.matcher(rib).matches();
    }
    
    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public static boolean isValidDateRange(LocalDate start, LocalDate end) {
        return start != null && end != null && !end.isBefore(start);
    }
}
```

### Step 10.2 — PDF Generation (Optional for MVP)

For PDF generation of contracts and payslips:
- Use Apache PDFBox or iText library
- Create `PdfGeneratorService` singleton
- Methods: `generateContractPdf(EmploymentContract)`, `generatePayslipPdf(Payslip)`
- Store PDFs in `data/finance/pdfs/` directory
- Store relative path in `pdf_url` field

**Note:** PDF generation can be deferred to post-MVP. For initial implementation, `pdf_url` can remain NULL.

---

## PHASE 11: Verification

### Step 11.1 — Compile: `mvn compile -q`
### Step 11.2 — Run & Test all CRUD operations
### Step 11.3 — Verify tax calculations with sample data
### Step 11.4 — Test currency conversion
### Step 11.5 — Verify i18n keys in all 3 languages

---

## PHASE 12: Performance Optimization

### Step 12.1 — Database Indexing

All required indexes are in Phase 1. Verify they exist:

```sql
-- Check indexes
SHOW INDEX FROM employment_contracts;
SHOW INDEX FROM payslips;
SHOW INDEX FROM bank_accounts;
SHOW INDEX FROM salary_history;
SHOW INDEX FROM exchange_rates;
SHOW INDEX FROM payment_transactions;
SHOW INDEX FROM tax_configurations;
```

### Step 12.2 — Query Optimization

1. **Use PreparedStatements** for all queries (prevents SQL injection + performance)
2. **Limit result sets** with `LIMIT` clause where appropriate
3. **Use JOINs** instead of N+1 queries for related data
4. **Cache tax configurations** in memory (load once at startup, rarely changes)
5. **Cache exchange rates** for current day (refresh daily)

### Step 12.3 — Connection Pooling

HikariCP is already configured in `DatabaseConfig`. Verify pool settings:
- minimumIdle: 5
- maximumPoolSize: 10
- connectionTimeout: 30000ms

### Step 12.4 — Async Operations

All database operations from controllers MUST use `javafx.concurrent.Task`:
```java
Task<List<Payslip>> task = new Task<>() {
    @Override
    protected List<Payslip> call() {
        return PayslipService.getInstance().findByUserId(userId);
    }
};
task.setOnSucceeded(e -> {
    List<Payslip> payslips = task.getValue();
    Platform.runLater(() -> updateUI(payslips));
});
new Thread(task).start();
```

---

## PHASE 13: Testing Guidelines

### Step 13.1 — Unit Test Services

Create tests for critical business logic:

1. **TaxConfigurationService.calculateIRPP()**
   - Test with various salary amounts
   - Verify progressive bracket calculations
   - Test edge cases (0, negative, very large amounts)

2. **PayslipService.generatePayslip()**
   - Verify CNSS calculations (9.18% and 16.57%)
   - Verify IRPP calculation matches TaxConfiguration
   - Verify net = gross - deductions + bonuses

3. **ExchangeRateService.convert()**
   - Test conversion accuracy
   - Test with missing rates
   - Test reverse conversion (TND→EUR→TND should be close to original)

### Step 13.2 — Integration Tests

Test complete workflows:

1. **Contract Creation Flow:**
   - Create contract (status = DRAFT)
   - Sign contract (status → ACTIVE)
   - Terminate contract (status → TERMINATED)

2. **Payroll Flow:**
   - Generate payslip for active contract
   - Verify uniqueness constraint (cannot generate duplicate)
   - Mark as paid
   - Create payment transaction

3. **Bank Account Flow:**
   - Add multiple accounts
   - Set primary (others become non-primary)
   - Delete non-primary account (should succeed)
   - Delete primary account (should fail or auto-set another as primary)

### Step 13.3 — Manual Testing Checklist

- [ ] Create contract and verify all fields save correctly
- [ ] Sign contract and verify status changes to ACTIVE
- [ ] Generate payslip and verify calculations are correct
- [ ] View payslip details and verify all deductions shown
- [ ] Add bank account with valid IBAN and verify validation
- [ ] Try invalid IBAN and verify error message
- [ ] Set primary account and verify other becomes non-primary
- [ ] View salary history after contract update
- [ ] Test currency conversion display
- [ ] Test all i18n languages (FR, EN, AR)
- [ ] Test admin view (create contracts for employees)
- [ ] Test bulk payroll generation
- [ ] Test payment transaction creation
- [ ] Export/download PDF (if implemented)

---

## PHASE 14: Implementation Checklist

### Database & Schema
- [ ] Add all 8 tables to DatabaseInitializer
- [ ] Add all indexes
- [ ] Seed tax configurations for Tunisia
- [ ] Seed default exchange rates
- [ ] Test database connection and table creation

### Enums
- [ ] ContractType.java
- [ ] ContractStatus.java
- [ ] PaymentStatus.java
- [ ] TransactionType.java
- [ ] Currency.java

### Entities (7 POJOs)
- [ ] EmploymentContract.java (with all getters/setters)
- [ ] Payslip.java
- [ ] BankAccount.java
- [ ] SalaryHistory.java
- [ ] ExchangeRate.java
- [ ] PaymentTransaction.java
- [ ] TaxConfiguration.java

### Services (7 Singletons)
- [ ] ContractService.java (full CRUD + sign/terminate)
- [ ] PayslipService.java (CRUD + generatePayslip with tax calc)
- [ ] BankAccountService.java (CRUD + setPrimary)
- [ ] SalaryHistoryService.java
- [ ] ExchangeRateService.java (CRUD + convert)
- [ ] PaymentTransactionService.java
- [ ] TaxConfigurationService.java (CRUD + calculateIRPP)

### Utilities
- [ ] ValidationUtils.java (IBAN, RIB, amounts, dates)
- [ ] PdfGeneratorService.java (optional for MVP)

### Controllers & Views
- [ ] FinanceController.java (employee view)
- [ ] FinanceView.fxml (tabbed interface)
- [ ] FinanceAdminController.java (admin/employer view)
- [ ] FinanceAdminView.fxml (admin tabbed interface)

### Navigation
- [ ] Add finance nav button to MainView.java
- [ ] Implement showFinanceView() with role-based routing
- [ ] Add finance SVG icon

### Internationalization
- [ ] Add all keys to messages.properties (default)
- [ ] Add all keys to messages_fr.properties (French)
- [ ] Add all keys to messages_en.properties (English)
- [ ] Add all keys to messages_ar.properties (Arabic)
- [ ] Test language switching

### Business Logic Implementation
- [ ] IRPP progressive tax calculation
- [ ] CNSS calculations (9.18% + 16.57%)
- [ ] Net salary formula
- [ ] Currency conversion
- [ ] Contract status transitions
- [ ] Primary bank account logic
- [ ] Salary history auto-logging

### Testing & Validation
- [ ] Test all CRUD operations
- [ ] Verify tax calculations with known values
- [ ] Test currency conversion accuracy
- [ ] Validate IBAN/RIB formats
- [ ] Test unique constraints (payslip per month/year)
- [ ] Test foreign key cascades
- [ ] Test with multiple users
- [ ] Test admin vs employee permissions

### Compile & Run
- [ ] `mvn clean compile` - no errors
- [ ] Run application
- [ ] Navigate to Finance module
- [ ] Create test data
- [ ] Verify all features work end-to-end

---

## RULES

1. **NO DAO classes.** Services contain JDBC code directly.
2. **NO JavaFX imports in entity or service classes.**
3. **All UI text via `I18n.get("key")`.**
4. **Singleton pattern** on all services.
5. **Use `BigDecimal` for ALL monetary values** — never `double` for money.
6. **Async DB calls** via `javafx.concurrent.Task`.
7. **Follow existing code patterns exactly.**
8. **Table creation in `DatabaseInitializer.java`.**
9. **Use TL* components** for UI.
10. **Handle null safely.**
11. **Do NOT break existing functionality.**

---

## TROUBLESHOOTING COMMON ISSUES

### Issue 1: "Duplicate entry for key 'uq_payslip_period'"
**Solution:** Check if payslip already exists for that contract/month/year before generating. Use `PayslipService.findByPeriod()` first.

### Issue 2: IRPP calculation doesn't match expected result
**Solution:** 
- Verify tax brackets are loaded correctly in database
- Ensure annual salary calculation (monthly × 12) is correct
- Check progressive bracket logic (each bracket applied only to portion of salary in that bracket)
- Use `BigDecimal` with proper scale and rounding mode

### Issue 3: Currency conversion returns null
**Solution:**
- Verify exchange rates are seeded in database
- Check if rate exists for the specific currency pair
- Add fallback to latest rate if specific date rate doesn't exist

### Issue 4: Bank account validation always fails
**Solution:**
- Check IBAN regex pattern matches your format
- Remove spaces before validating IBAN
- RIB should be exactly 20 digits for Tunisia

### Issue 5: Contract sign button doesn't change status
**Solution:**
- Verify the `sign()` method updates both `is_signed`, `signed_date`, AND `status` fields
- Check database transaction commits successfully
- Refresh the view after successful sign operation

### Issue 6: User can have multiple ACTIVE contracts
**Solution:**
- Add business logic check in `ContractService.create()` to verify no other ACTIVE contract exists
- Set previous contract to TERMINATED before activating new one

### Issue 7: Payslip shows incorrect net salary
**Solution:**
- Verify calculation formula: `net = gross - cnssEmployee - irpp - otherDeductions + bonuses`
- Check each deduction is calculated correctly
- Ensure `BigDecimal` operations use proper methods (`.subtract()`, `.add()`, `.multiply()`)

### Issue 8: i18n keys not found in Arabic
**Solution:**
- Verify file encoding is UTF-8 with BOM for Arabic characters
- Check file is named exactly `messages_ar.properties`
- Ensure all keys exist in all 4 property files

### Issue 9: Primary bank account doesn't unset others
**Solution:**
- `setPrimary()` must use transaction to:
  1. UPDATE all accounts for user SET is_primary = FALSE
  2. UPDATE specific account SET is_primary = TRUE
- Consider using a single UPDATE with CASE statement

### Issue 10: Foreign key constraint violations
**Solution:**
- Check deletion order: Delete payslips before deleting contracts
- Use `ON DELETE CASCADE` for automatic cleanup (already in schema)
- Verify referenced IDs exist before INSERT

---

## APPENDIX: SQL Query Examples

### Get employee's total earnings this year
```sql
SELECT SUM(net_salary) as total_earnings
FROM payslips p
JOIN employment_contracts c ON p.contract_id = c.id
WHERE c.user_id = ? 
AND p.period_year = YEAR(CURDATE())
AND p.payment_status = 'PAID';
```

### Get average salary by contract type
```sql
SELECT contract_type, AVG(salary_base) as avg_salary, COUNT(*) as count
FROM employment_contracts
WHERE status = 'ACTIVE'
GROUP BY contract_type;
```

### Find contracts expiring in next 30 days
```sql
SELECT c.*, u.username, u.email
FROM employment_contracts c
JOIN users u ON c.user_id = u.id
WHERE c.contract_type = 'CDD'
AND c.end_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)
AND c.status = 'ACTIVE';
```

### Get payroll for specific month
```sql
SELECT u.username, p.gross_salary, p.net_salary, p.payment_status
FROM payslips p
JOIN users u ON p.user_id = u.id
WHERE p.period_month = ? AND p.period_year = ?
ORDER BY u.username;
```

### Currency conversion with rates
```sql
SELECT 
    p.net_salary as amount_tnd,
    p.net_salary * er.rate as amount_eur,
    er.rate as exchange_rate
FROM payslips p
LEFT JOIN exchange_rates er ON er.from_currency = 'TND' AND er.to_currency = 'EUR'
WHERE p.id = ?
AND er.rate_date = (SELECT MAX(rate_date) FROM exchange_rates WHERE from_currency = 'TND' AND to_currency = 'EUR');
```

---

## END OF MODULE 5 SPECIFICATION
