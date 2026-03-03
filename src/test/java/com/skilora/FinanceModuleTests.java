package com.skilora;

// === Finance Entities ===
import com.skilora.finance.entity.*;
import com.skilora.finance.enums.ContractStatus;
import com.skilora.finance.enums.ContractType;
import com.skilora.finance.enums.PaymentStatus;
import com.skilora.finance.enums.TransactionType;
import com.skilora.finance.service.*;

// === Config ===
import com.skilora.config.DatabaseConfig;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 *   SKILORA - Finance Module Test Suite
 * ──────────────────────────────────────────────────────────────────────
 *   Comprehensive tests covering:
 *   • 14 Entity classes (BankAccount, Bonus, Contract, Payslip, etc.)
 *   • 5 Enum types (ContractStatus/Type, Currency, PaymentStatus, etc.)
 *   • 16 Service classes (singletons, CRUD, edge cases)
 *   • Database schema validation (all finance tables)
 *   • Row view-models (BankAccountRow, PayslipRow, etc.)
 *   • Tax calculation, salary simulation, exchange rates
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@TestMethodOrder(OrderAnnotation.class)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@DisplayName("Finance Module Tests")
class FinanceModuleTests {

    // ═══════════════════════════════════════════════════════════════
    //  Section 1: Enum Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(1)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("1. Finance Enums")
    class FinanceEnumTests {

        @Test @Order(1)
        @DisplayName("ContractStatus has 5 values")
        void contractStatusValues() {
            ContractStatus[] values = ContractStatus.values();
            assertEquals(5, values.length);
            assertNotNull(ContractStatus.DRAFT);
            assertNotNull(ContractStatus.PENDING_SIGNATURE);
            assertNotNull(ContractStatus.ACTIVE);
            assertNotNull(ContractStatus.EXPIRED);
            assertNotNull(ContractStatus.TERMINATED);
        }

        @Test @Order(2)
        @DisplayName("ContractType has 4 values")
        void contractTypeValues() {
            ContractType[] values = ContractType.values();
            assertEquals(4, values.length);
            assertNotNull(ContractType.CDI);
            assertNotNull(ContractType.CDD);
            assertNotNull(ContractType.FREELANCE);
            assertNotNull(ContractType.STAGE);
        }

        @Test @Order(3)
        @DisplayName("Currency has 4 values with symbols")
        void currencyValues() {
            com.skilora.finance.enums.Currency[] values = com.skilora.finance.enums.Currency.values();
            assertEquals(4, values.length);

            assertEquals("TND", com.skilora.finance.enums.Currency.TND.getCode());
            assertEquals("Tunisian Dinar", com.skilora.finance.enums.Currency.TND.getDisplayName());
            assertEquals("TND", com.skilora.finance.enums.Currency.TND.getSymbol());

            assertEquals("€", com.skilora.finance.enums.Currency.EUR.getSymbol());
            assertEquals("$", com.skilora.finance.enums.Currency.USD.getSymbol());
            assertEquals("£", com.skilora.finance.enums.Currency.GBP.getSymbol());
        }

        @Test @Order(4)
        @DisplayName("PaymentStatus has 5 values")
        void paymentStatusValues() {
            PaymentStatus[] values = PaymentStatus.values();
            assertEquals(5, values.length);
            assertNotNull(PaymentStatus.PENDING);
            assertNotNull(PaymentStatus.PROCESSING);
            assertNotNull(PaymentStatus.PAID);
            assertNotNull(PaymentStatus.FAILED);
            assertNotNull(PaymentStatus.CANCELLED);
        }

        @Test @Order(5)
        @DisplayName("TransactionType has 5 values")
        void transactionTypeValues() {
            TransactionType[] values = TransactionType.values();
            assertEquals(5, values.length);
            assertNotNull(TransactionType.SALARY);
            assertNotNull(TransactionType.BONUS);
            assertNotNull(TransactionType.REIMBURSEMENT);
            assertNotNull(TransactionType.ADVANCE);
            assertNotNull(TransactionType.OTHER);
        }

        @Test
        @Order(6)
        @DisplayName("All Currency values have non-empty display names")
        void allCurrenciesHaveNames() {
            for (com.skilora.finance.enums.Currency cur : com.skilora.finance.enums.Currency.values()) {
                assertNotNull(cur.getDisplayName());
                assertFalse(cur.getDisplayName().isEmpty());
                assertNotNull(cur.getSymbol());
            }
        }

        @ParameterizedTest
        @EnumSource(ContractStatus.class)
        @Order(7)
        @DisplayName("All ContractStatus valueOf round-trips")
        void contractStatusValueOf(ContractStatus status) {
            assertEquals(status, ContractStatus.valueOf(status.name()));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 2: BankAccount Entity Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(10)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("10. BankAccount Entity")
    class BankAccountEntityTests {

        @Test @Order(1)
        @DisplayName("BankAccount default constructor")
        void defaults() {
            BankAccount ba = new BankAccount();
            assertEquals("TND", ba.getCurrency());
            assertFalse(ba.isPrimary());
            assertFalse(ba.isVerified());
        }

        @Test @Order(2)
        @DisplayName("BankAccount 3-arg constructor")
        void paramConstructor() {
            BankAccount ba = new BankAccount(5, "STB", "Ahmed Ben Salah");
            assertEquals(5, ba.getUserId());
            assertEquals("STB", ba.getBankName());
            assertEquals("Ahmed Ben Salah", ba.getAccountHolder());
            assertEquals("TND", ba.getCurrency());
        }

        @Test @Order(3)
        @DisplayName("BankAccount full setters/getters")
        void settersGetters() {
            BankAccount ba = new BankAccount();
            ba.setId(1);
            ba.setUserId(5);
            ba.setBankName("BIAT");
            ba.setAccountHolder("Test User");
            ba.setIban("TN59 1000 1234 5678 9012 3456");
            ba.setSwiftBic("BIATTNTT");
            ba.setRib("10001234567890123456");
            ba.setCurrency("EUR");
            ba.setPrimary(true);
            ba.setVerified(true);

            assertEquals("BIAT", ba.getBankName());
            assertEquals("TN59 1000 1234 5678 9012 3456", ba.getIban());
            assertEquals("BIATTNTT", ba.getSwiftBic());
            assertTrue(ba.isPrimary());
            assertTrue(ba.isVerified());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 3: Bonus Entity Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(11)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("11. Bonus Entity")
    class BonusEntityTests {

        @Test @Order(1)
        @DisplayName("Bonus default constructor")
        void defaults() {
            Bonus b = new Bonus();
            assertEquals("TND", b.getCurrency());
            assertEquals("PENDING", b.getStatus());
        }

        @Test @Order(2)
        @DisplayName("Bonus parameterized constructor")
        void paramConstructor() {
            Bonus b = new Bonus(5, "PERFORMANCE", new BigDecimal("500.00"), "TND", "Q4 bonus");
            assertEquals(5, b.getUserId());
            assertEquals("PERFORMANCE", b.getType());
            assertEquals(new BigDecimal("500.00"), b.getAmount());
            assertEquals(LocalDate.now(), b.getDateAwarded());
            assertEquals("PENDING", b.getStatus());
        }

        @Test @Order(3)
        @DisplayName("Bonus setters/getters")
        void settersGetters() {
            Bonus b = new Bonus();
            b.setId(1);
            b.setApprovedBy(100);
            b.setStatus("APPROVED");
            assertEquals(1, b.getId());
            assertEquals(100, b.getApprovedBy());
            assertEquals("APPROVED", b.getStatus());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 4: EmploymentContract Entity Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(12)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("12. EmploymentContract Entity")
    class ContractEntityTests {

        @Test @Order(1)
        @DisplayName("Contract default constructor")
        void defaults() {
            EmploymentContract c = new EmploymentContract();
            assertEquals("TND", c.getCurrency());
            assertEquals("CDI", c.getContractType());
            assertEquals("DRAFT", c.getStatus());
            assertFalse(c.isSigned());
        }

        @Test @Order(2)
        @DisplayName("Contract parameterized constructor")
        void paramConstructor() {
            EmploymentContract c = new EmploymentContract(5, new BigDecimal("2500.00"), LocalDate.of(2025, 1, 1));
            assertEquals(5, c.getUserId());
            assertEquals(new BigDecimal("2500.00"), c.getSalaryBase());
            assertEquals(LocalDate.of(2025, 1, 1), c.getStartDate());
        }

        @Test @Order(3)
        @DisplayName("Contract full fields")
        void fullFields() {
            EmploymentContract c = new EmploymentContract();
            c.setId(1);
            c.setEmployerId(10);
            c.setJobOfferId(20);
            c.setEndDate(LocalDate.of(2026, 1, 1));
            c.setContractType("CDD");
            c.setStatus("ACTIVE");
            c.setPdfUrl("contract.pdf");
            c.setSigned(true);
            c.setUserName("Employee");
            c.setEmployerName("Company");
            c.setJobTitle("Developer");

            assertEquals(10, c.getEmployerId());
            assertEquals("CDD", c.getContractType());
            assertEquals("ACTIVE", c.getStatus());
            assertTrue(c.isSigned());
            assertEquals("Developer", c.getJobTitle());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 5: Payslip Entity Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(13)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("13. Payslip Entity")
    class PayslipEntityTests {

        @Test @Order(1)
        @DisplayName("Payslip default constructor sets BigDecimal to ZERO")
        void defaults() {
            Payslip p = new Payslip();
            assertEquals(BigDecimal.ZERO, p.getCnssEmployee());
            assertEquals(BigDecimal.ZERO, p.getCnssEmployer());
            assertEquals(BigDecimal.ZERO, p.getIrpp());
            assertEquals(BigDecimal.ZERO, p.getOtherDeductions());
            assertEquals(BigDecimal.ZERO, p.getBonuses());
            assertEquals("TND", p.getCurrency());
            assertEquals("PENDING", p.getPaymentStatus());
        }

        @Test @Order(2)
        @DisplayName("Payslip parameterized constructor sets net=gross")
        void paramConstructor() {
            Payslip p = new Payslip(1, 5, 6, 2025, new BigDecimal("3000.00"));
            assertEquals(1, p.getContractId());
            assertEquals(5, p.getUserId());
            assertEquals(6, p.getPeriodMonth());
            assertEquals(2025, p.getPeriodYear());
            assertEquals(new BigDecimal("3000.00"), p.getGrossSalary());
            assertEquals(new BigDecimal("3000.00"), p.getNetSalary());
        }

        @Test @Order(3)
        @DisplayName("Payslip transient fields")
        void transientFields() {
            Payslip p = new Payslip();
            p.setUserName("Employee");
            p.setPeriodLabel("June 2025");
            assertEquals("Employee", p.getUserName());
            assertEquals("June 2025", p.getPeriodLabel());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 6: Other Entities (Escrow, ExchangeRate, etc.)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(14)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("14. Other Finance Entities")
    class OtherFinanceEntities {

        @Test @Order(1)
        @DisplayName("EscrowAccount defaults")
        void escrowDefaults() {
            EscrowAccount ea = new EscrowAccount();
            assertEquals("TND", ea.getCurrency());
            assertEquals("HOLDING", ea.getStatus());
        }

        @Test @Order(2)
        @DisplayName("EscrowAccount parameterized constructor")
        void escrowParam() {
            EscrowAccount ea = new EscrowAccount(1, new BigDecimal("1000.00"));
            assertEquals(1, ea.getContractId());
            assertEquals(new BigDecimal("1000.00"), ea.getAmount());
        }

        @Test @Order(3)
        @DisplayName("ExchangeRate defaults")
        void exchangeRateDefaults() {
            ExchangeRate er = new ExchangeRate();
            assertEquals("BCT", er.getSource());
        }

        @Test @Order(4)
        @DisplayName("ExchangeRate parameterized constructor")
        void exchangeRateParam() {
            ExchangeRate er = new ExchangeRate("TND", "EUR", new BigDecimal("0.30"), LocalDate.now());
            assertEquals("TND", er.getFromCurrency());
            assertEquals("EUR", er.getToCurrency());
            assertEquals(new BigDecimal("0.30"), er.getRate());
        }

        @Test @Order(5)
        @DisplayName("PaymentTransaction defaults")
        void txDefaults() {
            PaymentTransaction tx = new PaymentTransaction();
            assertEquals("TND", tx.getCurrency());
            assertEquals("SALARY", tx.getTransactionType());
            assertEquals("PENDING", tx.getStatus());
        }

        @Test @Order(6)
        @DisplayName("PaymentTransaction parameterized constructor")
        void txParam() {
            PaymentTransaction tx = new PaymentTransaction(1, new BigDecimal("2500.00"), 5);
            assertEquals(1, tx.getPayslipId());
            assertEquals(new BigDecimal("2500.00"), tx.getAmount());
            assertEquals(5, tx.getToAccountId());
        }

        @Test @Order(7)
        @DisplayName("SalaryHistory constructor")
        void salaryHistoryConstructor() {
            SalaryHistory sh = new SalaryHistory(1,
                    new BigDecimal("2000.00"), new BigDecimal("2500.00"),
                    LocalDate.of(2025, 7, 1), "Annual Raise");
            assertEquals(1, sh.getContractId());
            assertEquals(new BigDecimal("2000.00"), sh.getOldSalary());
            assertEquals(new BigDecimal("2500.00"), sh.getNewSalary());
            assertEquals("Annual Raise", sh.getReason());
        }

        @Test @Order(8)
        @DisplayName("TaxConfiguration defaults")
        void taxDefaults() {
            TaxConfiguration tc = new TaxConfiguration();
            assertTrue(tc.isActive());
            assertEquals(BigDecimal.ZERO, tc.getMinBracket());
        }

        @Test @Order(9)
        @DisplayName("TaxConfiguration parameterized constructor")
        void taxParam() {
            TaxConfiguration tc = new TaxConfiguration("TN", "IRPP", new BigDecimal("26"), LocalDate.now());
            assertEquals("TN", tc.getCountry());
            assertEquals("IRPP", tc.getTaxType());
            assertEquals(new BigDecimal("26"), tc.getRate());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 7: Row View-Model Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(15)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("15. Row View-Models")
    class RowViewModelTests {

        @Test @Order(1)
        @DisplayName("BankAccountRow fields")
        void bankAccountRow() {
            BankAccountRow row = new BankAccountRow(1, 5, "Ahmed", "BIAT", "TN59...", "BIATTNTT", "TND", true, true);
            assertEquals(1, row.getId());
            assertEquals(5, row.getUserId());
            assertEquals("Ahmed", row.getEmployeeName());
            assertEquals("BIAT", row.getBankName());
            assertTrue(row.getIsPrimary());
            assertTrue(row.getIsVerified());
        }

        @Test @Order(2)
        @DisplayName("BonusRow fields")
        void bonusRow() {
            BonusRow row = new BonusRow(1, 5, "Ahmed", 500.0, "Performance", "2025-06-01");
            assertEquals(1, row.getId());
            assertEquals(500.0, row.getAmount(), 0.01);
            assertEquals("Performance", row.getReason());
        }

        @Test @Order(3)
        @DisplayName("ContractRow fields")
        void contractRow() {
            ContractRow row = new ContractRow(1, 5, "Ahmed", "Skilora Inc.", "CDI", "Developer",
                    2500.0, "2025-01-01", "2026-01-01", "ACTIVE");
            assertEquals(1, row.getId());
            assertEquals("CDI", row.getType());
            assertEquals("ACTIVE", row.getStatus());
            assertEquals(2500.0, row.getSalary(), 0.01);
        }

        @Test @Order(4)
        @DisplayName("PayslipRow calculateTotals")
        void payslipRowCalculateTotals() {
            PayslipRow row = new PayslipRow(1, 5, "Ahmed", 6, 2025,
                    2500.0, 0, 0, 200.0, "TND", "PENDING");
            row.calculateTotals();
            // gross = base + overtime + bonuses = 2500 + 0 + 200 = 2700
            assertEquals(2700.0, row.getGross(), 0.01);
            // cnss = 9.18% of gross
            assertEquals(2700.0 * 0.0918, row.getCnss(), 0.01);
            // irpp = 26% of (gross - cnss)
            double cnss = 2700.0 * 0.0918;
            assertEquals((2700.0 - cnss) * 0.26, row.getIrpp(), 0.01);
            // net > 0
            assertTrue(row.getNet() > 0);
        }

        @Test @Order(5)
        @DisplayName("EmployeeSummaryRow fields")
        void employeeSummaryRow() {
            EmployeeSummaryRow row = new EmployeeSummaryRow(5, "Ahmed Ben Salah", "Developer",
                    2500.0, 500.0, 2100.0, "Verified");
            assertEquals(5, row.getUserId());
            assertEquals("Ahmed Ben Salah", row.getFullName());
            assertEquals("Developer", row.getPosition());
            assertEquals(2500.0, row.getCurrentSalary(), 0.01);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 8: Service Singleton Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(30)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("30. Service Singletons")
    class ServiceSingletonTests {

        @Test @Order(1) @DisplayName("BankAccountService singleton")
        void bankAccountService() { assertSame(BankAccountService.getInstance(), BankAccountService.getInstance()); }

        @Test @Order(2) @DisplayName("ContractService singleton")
        void contractService() { assertSame(ContractService.getInstance(), ContractService.getInstance()); }

        @Test @Order(3) @DisplayName("PayslipService singleton")
        void payslipService() { assertSame(PayslipService.getInstance(), PayslipService.getInstance()); }

        @Test @Order(4) @DisplayName("PaymentTransactionService singleton")
        void paymentTxService() { assertSame(PaymentTransactionService.getInstance(), PaymentTransactionService.getInstance()); }

        @Test @Order(5) @DisplayName("ExchangeRateService singleton")
        void exchangeRateService() { assertSame(ExchangeRateService.getInstance(), ExchangeRateService.getInstance()); }

        @Test @Order(6) @DisplayName("TaxConfigurationService singleton")
        void taxConfigService() { assertSame(TaxConfigurationService.getInstance(), TaxConfigurationService.getInstance()); }

        @Test @Order(7) @DisplayName("EscrowService singleton")
        void escrowService() { assertSame(EscrowService.getInstance(), EscrowService.getInstance()); }

        @Test @Order(8) @DisplayName("SalaryHistoryService singleton")
        void salaryHistoryService() { assertSame(SalaryHistoryService.getInstance(), SalaryHistoryService.getInstance()); }

        @Test @Order(9) @DisplayName("FinanceDataService singleton")
        void financeDataService() { assertSame(FinanceDataService.getInstance(), FinanceDataService.getInstance()); }

        @Test @Order(10) @DisplayName("BalanceSimulationService singleton")
        void balanceSimService() { assertSame(BalanceSimulationService.getInstance(), BalanceSimulationService.getInstance()); }

        @Test @Order(11) @DisplayName("UserCurrencyService singleton")
        void userCurrencyService() { assertSame(UserCurrencyService.getInstance(), UserCurrencyService.getInstance()); }

        @Test @Order(12) @DisplayName("CurrencyApiService singleton")
        void currencyApiService() { assertSame(CurrencyApiService.getInstance(), CurrencyApiService.getInstance()); }

        @Test @Order(13) @DisplayName("StripePaymentService singleton")
        void stripeService() { assertSame(StripePaymentService.getInstance(), StripePaymentService.getInstance()); }

        @Test @Order(14) @DisplayName("FinanceChatbotService singleton")
        void chatbotService() { assertSame(FinanceChatbotService.getInstance(), FinanceChatbotService.getInstance()); }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 9: Database Schema Validation
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(40)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("40. Finance DB Schema")
    class FinanceDbSchemaTests {

        private boolean tableExists(String table) throws SQLException {
            try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
                ResultSet rs = conn.getMetaData().getTables(null, null, table, null);
                return rs.next();
            }
        }

        private boolean columnExists(String table, String column) throws SQLException {
            try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
                ResultSet rs = conn.getMetaData().getColumns(null, null, table, column);
                return rs.next();
            }
        }

        @Test @Order(1)  @DisplayName("'bank_accounts' table exists")
        void bankAccountsTable() throws SQLException { assertTrue(tableExists("bank_accounts")); }

        @Test @Order(2)  @DisplayName("'bonuses' table exists")
        void bonusesTable() throws SQLException { assertTrue(tableExists("bonuses")); }

        @Test @Order(3)  @DisplayName("'employment_contracts' table exists")
        void contractsTable() throws SQLException { assertTrue(tableExists("employment_contracts")); }

        @Test @Order(4)  @DisplayName("'escrow_accounts' table exists")
        void escrowTable() throws SQLException { assertTrue(tableExists("escrow_accounts")); }

        @Test @Order(5)  @DisplayName("'exchange_rates' table exists")
        void exchangeRatesTable() throws SQLException { assertTrue(tableExists("exchange_rates")); }

        @Test @Order(6)  @DisplayName("'payment_transactions' table exists")
        void paymentTxTable() throws SQLException { assertTrue(tableExists("payment_transactions")); }

        @Test @Order(7)  @DisplayName("'payslips' table exists")
        void payslipsTable() throws SQLException { assertTrue(tableExists("payslips")); }

        @Test @Order(8)  @DisplayName("'salary_history' table exists")
        void salaryHistoryTable() throws SQLException { assertTrue(tableExists("salary_history")); }

        @Test @Order(9)  @DisplayName("'tax_configurations' table exists")
        void taxConfigTable() throws SQLException { assertTrue(tableExists("tax_configurations")); }

        // Column checks
        @Test @Order(20) @DisplayName("bank_accounts has IBAN column")
        void bankAccountColumns() throws SQLException {
            assertTrue(columnExists("bank_accounts", "iban"));
            assertTrue(columnExists("bank_accounts", "user_id"));
            assertTrue(columnExists("bank_accounts", "bank_name"));
            assertTrue(columnExists("bank_accounts", "is_primary"));
        }

        @Test @Order(21) @DisplayName("employment_contracts has salary_base column")
        void contractColumns() throws SQLException {
            assertTrue(columnExists("employment_contracts", "user_id"));
            assertTrue(columnExists("employment_contracts", "salary_base"));
            assertTrue(columnExists("employment_contracts", "contract_type"));
            assertTrue(columnExists("employment_contracts", "status"));
        }

        @Test @Order(22) @DisplayName("payslips has net_salary column")
        void payslipColumns() throws SQLException {
            assertTrue(columnExists("payslips", "contract_id"));
            assertTrue(columnExists("payslips", "user_id"));
            assertTrue(columnExists("payslips", "gross_salary"));
            assertTrue(columnExists("payslips", "net_salary"));
            assertTrue(columnExists("payslips", "cnss_employee"));
        }

        @Test @Order(23) @DisplayName("payment_transactions has stripe_payment_id")
        void txColumns() throws SQLException {
            assertTrue(columnExists("payment_transactions", "amount"));
            assertTrue(columnExists("payment_transactions", "status"));
            assertTrue(columnExists("payment_transactions", "transaction_type"));
        }

        @Test @Order(24) @DisplayName("exchange_rates has rate column")
        void exchangeRateColumns() throws SQLException {
            assertTrue(columnExists("exchange_rates", "from_currency"));
            assertTrue(columnExists("exchange_rates", "to_currency"));
            assertTrue(columnExists("exchange_rates", "rate"));
        }

        @Test @Order(25) @DisplayName("escrow_accounts has status column")
        void escrowColumns() throws SQLException {
            assertTrue(columnExists("escrow_accounts", "contract_id"));
            assertTrue(columnExists("escrow_accounts", "amount"));
            assertTrue(columnExists("escrow_accounts", "status"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 10: BankAccountService DB Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(50)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("50. BankAccountService CRUD")
    class BankAccountServiceTests {

        private static final BankAccountService service = BankAccountService.getInstance();

        @Test @Order(1)
        @DisplayName("findByUserId returns list for non-existent user")
        void findByUserIdEmpty() throws SQLException {
            List<BankAccount> list = service.findByUserId(999999);
            assertNotNull(list);
            assertTrue(list.isEmpty());
        }

        @Test @Order(2)
        @DisplayName("findById returns null for non-existent")
        void findByIdNull() throws SQLException {
            assertNull(service.findById(999999));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 11: ContractService DB Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(55)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("55. ContractService Operations")
    class ContractServiceTests {

        private static final ContractService service = ContractService.getInstance();

        @Test @Order(1)
        @DisplayName("findAll returns list")
        void findAll() throws SQLException {
            assertNotNull(service.findAll());
        }

        @Test @Order(2)
        @DisplayName("findById returns null for non-existent")
        void findByIdNull() throws SQLException {
            assertNull(service.findById(999999));
        }

        @Test @Order(3)
        @DisplayName("findByUserId returns list")
        void findByUserId() throws SQLException {
            List<EmploymentContract> contracts = service.findByUserId(999999);
            assertNotNull(contracts);
            assertTrue(contracts.isEmpty());
        }

        @Test @Order(4)
        @DisplayName("findActiveByUserId returns null for non-existent")
        void findActiveByUserIdNull() throws SQLException {
            assertNull(service.findActiveByUserId(999999));
        }

        @Test @Order(5)
        @DisplayName("findByStatus returns list")
        void findByStatus() throws SQLException {
            List<EmploymentContract> contracts = service.findByStatus("ACTIVE");
            assertNotNull(contracts);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 12: PayslipService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(60)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("60. PayslipService Operations")
    class PayslipServiceTests {

        private static final PayslipService service = PayslipService.getInstance();

        @Test @Order(1)
        @DisplayName("findById returns null for non-existent")
        void findByIdNull() throws SQLException {
            assertNull(service.findById(999999));
        }

        @Test @Order(2)
        @DisplayName("findByUserId returns empty for non-existent")
        void findByUserIdEmpty() throws SQLException {
            List<Payslip> list = service.findByUserId(999999);
            assertNotNull(list);
            assertTrue(list.isEmpty());
        }

        @Test @Order(3)
        @DisplayName("findByContractId returns empty for non-existent")
        void findByContractIdEmpty() throws SQLException {
            List<Payslip> list = service.findByContractId(999999);
            assertNotNull(list);
            assertTrue(list.isEmpty());
        }

        @Test @Order(4)
        @DisplayName("generatePayslip returns null for non-existent contract")
        void generatePayslipNull() {
            Payslip p = service.generatePayslip(999999, 6, 2025);
            assertNull(p);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 13: PaymentTransactionService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(65)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("65. PaymentTransactionService Operations")
    class PaymentTxServiceTests {

        private static final PaymentTransactionService service = PaymentTransactionService.getInstance();

        @Test @Order(1)
        @DisplayName("findById returns null for non-existent")
        void findByIdNull() throws SQLException {
            assertNull(service.findById(999999));
        }

        @Test @Order(2)
        @DisplayName("findByUserId returns empty for non-existent")
        void findByUserIdEmpty() throws SQLException {
            List<PaymentTransaction> list = service.findByUserId(999999);
            assertNotNull(list);
            assertTrue(list.isEmpty());
        }

        @Test @Order(3)
        @DisplayName("findRecent returns list")
        void findRecent() throws SQLException {
            List<PaymentTransaction> recent = service.findRecent(5);
            assertNotNull(recent);
        }

        @Test @Order(4)
        @DisplayName("getTotalPaidByUser returns 0 for non-existent")
        void totalPaidZero() throws SQLException {
            BigDecimal total = service.getTotalPaidByUser(999999);
            assertNotNull(total);
            assertEquals(0, total.compareTo(BigDecimal.ZERO));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 14: ExchangeRateService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(70)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("70. ExchangeRateService Operations")
    class ExchangeRateServiceTests {

        private static final ExchangeRateService service = ExchangeRateService.getInstance();

        @Test @Order(1)
        @DisplayName("getRate returns null for non-existent pair")
        void getRateNull() throws SQLException {
            ExchangeRate rate = service.getRate("XXX", "YYY");
            assertNull(rate);
        }

        @Test @Order(2)
        @DisplayName("convert returns null for non-existent pair")
        void convertNull() throws SQLException {
            BigDecimal result = service.convert(BigDecimal.TEN, "XXX", "YYY");
            assertNull(result);
        }

        @Test @Order(3)
        @DisplayName("getHistory returns list")
        void getHistory() throws SQLException {
            List<ExchangeRate> history = service.getHistory("TND", "EUR", 30);
            assertNotNull(history);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 15: TaxConfigurationService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(75)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("75. TaxConfigurationService Operations")
    class TaxConfigServiceTests {

        private static final TaxConfigurationService service = TaxConfigurationService.getInstance();

        @Test @Order(1)
        @DisplayName("findByCountry returns list")
        void findByCountry() throws SQLException {
            List<TaxConfiguration> list = service.findByCountry("TN");
            assertNotNull(list);
        }

        @Test @Order(2)
        @DisplayName("calculateCompleteSalary returns non-null map")
        void calculateCompleteSalary() {
            Map<String, BigDecimal> result = TaxConfigurationService.calculateCompleteSalary(new BigDecimal("2500"));
            assertNotNull(result);
            assertTrue(result.containsKey("gross") || result.containsKey("grossSalary") || !result.isEmpty());
        }

        @Test @Order(3)
        @DisplayName("getOptimizationRecommendations returns map")
        void getOptimizationRecommendations() {
            Map<String, String> recs = TaxConfigurationService.getOptimizationRecommendations(new BigDecimal("3000"));
            assertNotNull(recs);
        }

        @Test @Order(4)
        @DisplayName("compareToMarket returns non-null string")
        void compareToMarket() {
            String result = TaxConfigurationService.compareToMarket(new BigDecimal("2500"), "Developer");
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 16: EscrowService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(80)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("80. EscrowService Operations")
    class EscrowServiceTests {

        private static final EscrowService service = EscrowService.getInstance();

        @Test @Order(1)
        @DisplayName("findById returns null for non-existent")
        void findByIdNull() throws SQLException {
            assertNull(service.findById(999999));
        }

        @Test @Order(2)
        @DisplayName("findByContract returns empty for non-existent")
        void findByContractEmpty() throws SQLException {
            List<EscrowAccount> list = service.findByContract(999999);
            assertNotNull(list);
            assertTrue(list.isEmpty());
        }

        @Test @Order(3)
        @DisplayName("findByStatus returns list")
        void findByStatus() throws SQLException {
            List<EscrowAccount> list = service.findByStatus("HOLDING");
            assertNotNull(list);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 17: FinanceDataService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(85)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("85. FinanceDataService Operations")
    class FinanceDataServiceTests {

        private static final FinanceDataService service = FinanceDataService.getInstance();

        @Test @Order(1)
        @DisplayName("getAllContracts returns list")
        void getAllContracts() throws SQLException {
            assertNotNull(service.getAllContracts());
        }

        @Test @Order(2)
        @DisplayName("getAllBankAccounts returns list")
        void getAllBankAccounts() throws SQLException {
            assertNotNull(service.getAllBankAccounts());
        }

        @Test @Order(3)
        @DisplayName("getAllBonuses returns list")
        void getAllBonuses() throws SQLException {
            assertNotNull(service.getAllBonuses());
        }

        @Test @Order(4)
        @DisplayName("getAllPayslips returns list")
        void getAllPayslips() throws SQLException {
            assertNotNull(service.getAllPayslips());
        }

        @Test @Order(5)
        @DisplayName("getEmployeeSummaries returns list")
        void getEmployeeSummaries() throws SQLException {
            assertNotNull(service.getEmployeeSummaries());
        }

        @Test @Order(6)
        @DisplayName("getContractsByUserId returns list for non-existent")
        void getContractsByUserId() throws SQLException {
            List<ContractRow> list = service.getContractsByUserId(999999);
            assertNotNull(list);
            assertTrue(list.isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 18: BalanceSimulationService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(90)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("90. BalanceSimulationService")
    class BalanceSimTests {

        private static final BalanceSimulationService service = BalanceSimulationService.getInstance();

        @Test @Order(1)
        @DisplayName("getCurrentBalance returns 0 for non-existent user")
        void currentBalanceZero() {
            assertEquals(0.0, service.getCurrentBalance(999999), 0.01);
        }

        @Test @Order(2)
        @DisplayName("projectBalance returns list")
        void projectBalance() {
            assertNotNull(service.projectBalance(999999, 6));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 19: UserCurrencyService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(92)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("92. UserCurrencyService")
    class UserCurrencyTests {

        private static final UserCurrencyService service = UserCurrencyService.getInstance();

        @Test @Order(1)
        @DisplayName("getPreferredCurrency returns default for non-existent")
        void getPreferredDefault() {
            String currency = service.getPreferredCurrency(999999);
            assertNotNull(currency);
        }

        @Test @Order(2)
        @DisplayName("formatAmount returns formatted string")
        void formatAmount() {
            String formatted = service.formatAmount(1500.50, "TND");
            assertNotNull(formatted);
            assertFalse(formatted.isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 20: FinanceChatbotService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(95)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("95. FinanceChatbotService")
    class FinanceChatbotTests {

        private static final FinanceChatbotService service = FinanceChatbotService.getInstance();

        @Test @Order(1)
        @DisplayName("isFinanceRelated detects finance questions")
        void financeRelated() {
            assertTrue(service.isFinanceRelated("Quel est mon salaire net?"));
            assertTrue(service.isFinanceRelated("Combien de CNSS je paie?"));
        }

        @Test @Order(2)
        @DisplayName("isFinanceRelated rejects non-finance questions")
        void nonFinanceRelated() {
            assertFalse(service.isFinanceRelated("What is the weather today?"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 21: Edge Cases & Validation
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(98)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("98. Edge Cases & Validation")
    class EdgeCaseTests {

        @Test @Order(1)
        @DisplayName("BankAccount with empty IBAN is allowed by entity")
        void emptyIban() {
            BankAccount ba = new BankAccount();
            ba.setIban("");
            assertEquals("", ba.getIban());
        }

        @Test @Order(2)
        @DisplayName("Payslip with negative gross is allowed by entity")
        void negativeGross() {
            Payslip p = new Payslip();
            p.setGrossSalary(new BigDecimal("-100"));
            assertEquals(new BigDecimal("-100"), p.getGrossSalary());
        }

        @Test @Order(3)
        @DisplayName("EmploymentContract status transitions")
        void contractStatusTransitions() {
            String[] statuses = {"DRAFT", "PENDING_SIGNATURE", "ACTIVE", "EXPIRED", "TERMINATED"};
            EmploymentContract c = new EmploymentContract();
            for (String s : statuses) {
                c.setStatus(s);
                assertEquals(s, c.getStatus());
            }
        }

        @Test @Order(4)
        @DisplayName("PaymentTransaction status transitions")
        void txStatusTransitions() {
            String[] statuses = {"PENDING", "PROCESSING", "PAID", "FAILED", "CANCELLED"};
            PaymentTransaction tx = new PaymentTransaction();
            for (String s : statuses) {
                tx.setStatus(s);
                assertEquals(s, tx.getStatus());
            }
        }

        @Test @Order(5)
        @DisplayName("EscrowAccount status transitions")
        void escrowStatusTransitions() {
            String[] statuses = {"HOLDING", "RELEASED", "REFUNDED", "DISPUTED"};
            EscrowAccount ea = new EscrowAccount();
            for (String s : statuses) {
                ea.setStatus(s);
                assertEquals(s, ea.getStatus());
            }
        }

        @Test @Order(6)
        @DisplayName("Currency valueOf round-trips")
        void currencyValueOf() {
            for (com.skilora.finance.enums.Currency c : com.skilora.finance.enums.Currency.values()) {
                assertEquals(c, com.skilora.finance.enums.Currency.valueOf(c.name()));
            }
        }

        @Test @Order(7)
        @DisplayName("ExchangeRate same currency pair")
        void sameCurrencyPair() {
            ExchangeRate er = new ExchangeRate("TND", "TND", BigDecimal.ONE, LocalDate.now());
            assertEquals("TND", er.getFromCurrency());
            assertEquals("TND", er.getToCurrency());
            assertEquals(BigDecimal.ONE, er.getRate());
        }
    }
}
