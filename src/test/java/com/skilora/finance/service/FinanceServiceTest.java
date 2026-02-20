package com.skilora.finance.service;

import com.skilora.finance.model.PayslipRow;
import com.skilora.finance.model.ContractRow;
import com.skilora.finance.model.BankAccountRow;
import com.skilora.finance.model.BonusRow;
import com.skilora.model.entity.User;
import com.skilora.model.service.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FinanceServiceTest {

    private final FinanceService service = FinanceService.getInstance();
    private final UserService userService = UserService.getInstance();
    private User testUser;

    @BeforeAll
    void setupUser() {
        // On récupère un utilisateur existant pour lier nos données de test
        List<User> users = userService.getAllUsers();
        if (users.isEmpty()) {
            fail("IMPOSSIBLE DE TESTER : Aucun utilisateur trouvé. Ajoutez-en un via l'application ou SQL.");
        }
        this.testUser = users.get(0);
        System.out.println("--- DÉBUT DES TESTS AVEC L'UTILISATEUR ID: " + testUser.getId() + " ---");
    }

    // ===================================================================================
    // TEST 1: PAYSLIPS (BULLETINS DE PAIE)
    // ===================================================================================
    @Test
    @DisplayName("CRUD Réel : Bulletins de Paie (Payslip)")
    void testPayslipCrud() throws SQLException {
        System.out.println("\n[TEST] Démarrage CRUD Payslip...");

        // 1. CREATE
        PayslipRow p = new PayslipRow(0, testUser.getId(), testUser.getFullName(), 13, 2099, 2000.0, 0, 0, 0, "TND",
                "TEST_P");
        service.addPayslip(p);

        // 2. READ
        List<PayslipRow> all = service.getAllPayslips();
        PayslipRow inserted = all.stream()
                .filter(x -> "TEST_P".equals(x.getStatus()) && x.getYear() == 2099)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Le bulletin n'a pas été inséré"));

        System.out.println("✅ Ajout OK (ID: " + inserted.getId() + ")");

        // 3. UPDATE
        inserted.setStatus("TEST_UPDATED");
        inserted.setBaseSalary(5000.0);
        service.updatePayslip(inserted);

        // Verify update
        assertNotNull(service.getAllPayslips().stream()
                .filter(x -> x.getId() == inserted.getId() && x.getBaseSalary() == 5000.0)
                .findFirst()
                .orElse(null), "La modification a échoué");
        System.out.println("✅ Modification OK");

        // 4. DELETE
        service.deletePayslip(inserted.getId());

        // Verify delete
        boolean exists = service.getAllPayslips().stream().anyMatch(x -> x.getId() == inserted.getId());
        assertFalse(exists, "La suppression a échoué");
        System.out.println("✅ Suppression OK");
    }

    // ===================================================================================
    // TEST 2: CONTRACTS (CONTRATS)
    // ===================================================================================
    @Test
    @DisplayName("CRUD Réel : Contrats de Travail (Contract)")
    void testContractCrud() throws SQLException {
        System.out.println("\n[TEST] Démarrage CRUD Contrat...");

        // 1. CREATE
        ContractRow c = new ContractRow(0, testUser.getId(), testUser.getFullName(), 0, "CDI", "TEST_DEV", 3500.0,
                "2099-01-01", "", "Active");
        service.addContract(c);

        // 2. READ
        List<ContractRow> all = service.getAllContracts();
        ContractRow inserted = all.stream()
                .filter(x -> "TEST_DEV".equals(x.getPosition()) && x.getSalary() == 3500.0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Le contrat n'a pas été inséré"));

        System.out.println("✅ Ajout OK (ID: " + inserted.getId() + ")");

        // 3. UPDATE
        inserted.setPosition("TEST_LEAD");
        inserted.setSalary(4500.0);
        service.updateContract(inserted);

        // Verify update
        assertNotNull(service.getAllContracts().stream()
                .filter(x -> x.getId() == inserted.getId() && x.getSalary() == 4500.0)
                .findFirst()
                .orElse(null), "La modification a échoué");
        System.out.println("✅ Modification OK");

        // 4. DELETE
        service.deleteContract(inserted.getId());

        // Verify delete
        boolean exists = service.getAllContracts().stream().anyMatch(x -> x.getId() == inserted.getId());
        assertFalse(exists, "La suppression a échoué");
        System.out.println("✅ Suppression OK");
    }

    // ===================================================================================
    // TEST 3: BANK ACCOUNTS (COMPTES BANCAIRES)
    // ===================================================================================
    @Test
    @DisplayName("CRUD Réel : Comptes Bancaires (BankAccount)")
    void testBankAccountCrud() throws SQLException {
        System.out.println("\n[TEST] Démarrage CRUD Compte Bancaire...");

        // 1. CREATE
        BankAccountRow b = new BankAccountRow(0, testUser.getId(), testUser.getFullName(), "TEST_BANK",
                "TN99000000001234567890", "TEST_SWIFT", "TND", true, false);
        service.addBankAccount(b);

        // 2. READ
        List<BankAccountRow> all = service.getAllBankAccounts();
        BankAccountRow inserted = all.stream()
                .filter(x -> "TEST_SWIFT".equals(x.getSwift()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Le compte n'a pas été inséré"));

        System.out.println("✅ Ajout OK (ID: " + inserted.getId() + ")");

        // 3. UPDATE
        // Note: Dans FinanceService, update modifies BankName and IBAN.
        inserted.setBankName("TEST_BANK_UPDATED");
        service.updateBankAccount(inserted);

        // Verify update
        assertNotNull(service.getAllBankAccounts().stream()
                .filter(x -> x.getId() == inserted.getId() && "TEST_BANK_UPDATED".equals(x.getBankName()))
                .findFirst()
                .orElse(null), "La modification a échoué");
        System.out.println("✅ Modification OK");

        // 4. DELETE
        service.deleteBankAccount(inserted.getId());

        // Verify delete
        boolean exists = service.getAllBankAccounts().stream().anyMatch(x -> x.getId() == inserted.getId());
        assertFalse(exists, "La suppression a échoué");
        System.out.println("✅ Suppression OK");
    }

    // ===================================================================================
    // TEST 4: BONUSES (PRIMES)
    // ===================================================================================
    @Test
    @DisplayName("CRUD Réel : Primes (Bonus)")
    void testBonusCrud() throws SQLException {
        System.out.println("\n[TEST] Démarrage CRUD Primes...");

        // 1. CREATE
        BonusRow bo = new BonusRow(0, testUser.getId(), testUser.getFullName(), 500.0, "TEST_REASON", "2099-12-31");
        service.addBonus(bo);

        // 2. READ
        List<BonusRow> all = service.getAllBonuses();
        BonusRow inserted = all.stream()
                .filter(x -> "TEST_REASON".equals(x.getReason()) && x.getAmount() == 500.0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("La prime n'a pas été insérée"));

        System.out.println("✅ Ajout OK (ID: " + inserted.getId() + ")");

        // 3. UPDATE
        inserted.setAmount(1000.0);
        inserted.setReason("TEST_REASON_UPDATED");
        service.updateBonus(inserted);

        // Verify update
        assertNotNull(service.getAllBonuses().stream()
                .filter(x -> x.getId() == inserted.getId() && x.getAmount() == 1000.0)
                .findFirst()
                .orElse(null), "La modification a échoué");
        System.out.println("✅ Modification OK");

        // 4. DELETE
        service.deleteBonus(inserted.getId());

        // Verify delete
        boolean exists = service.getAllBonuses().stream().anyMatch(x -> x.getId() == inserted.getId());
        assertFalse(exists, "La suppression a échoué");
        System.out.println("✅ Suppression OK");
    }
}
