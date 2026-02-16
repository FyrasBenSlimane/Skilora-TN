package com.skilora.finance.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for BankAccount entity (Finance)
 */
class BankAccountTest {

    private BankAccount account;

    @BeforeEach
    void setUp() {
        account = new BankAccount();
    }

    @Test
    void testDefaultConstructor() {
        assertNotNull(account);
        assertNull(account.getId());
    }

    @Test
    void testConstructorWithParameters() {
        BankAccount ba = new BankAccount(12, "BIAT", "TN123456", "TND");

        assertEquals(12, ba.getUserId());
        assertEquals("BIAT", ba.getBankName());
        assertEquals("TN123456", ba.getIban());
        assertEquals("TND", ba.getCurrency());
        assertFalse(ba.getVerified()); // Default false
        assertFalse(ba.getPrimaryAccount()); // Default false
    }

    @Test
    void testGettersAndSetters() {
        account.setId(99);
        account.setUserId(88);
        account.setBankName("Amen Bank");
        account.setIban("TN987654");
        account.setSwiftCode("AMENTNTT");
        account.setCurrency("EUR");
        account.setVerified(true);
        account.setPrimaryAccount(true);

        assertEquals(99, account.getId());
        assertEquals(88, account.getUserId());
        assertEquals("Amen Bank", account.getBankName());
        assertEquals("TN987654", account.getIban());
        assertEquals("AMENTNTT", account.getSwiftCode());
        assertEquals("EUR", account.getCurrency());
        assertTrue(account.getVerified());
        assertTrue(account.getPrimaryAccount());
    }

    @Test
    void testToString() {
        account.setId(1);
        account.setBankName("TestBank");
        account.setVerified(true);

        String result = account.toString();
        assertTrue(result.contains("BankAccount"));
        assertTrue(result.contains("id=1"));
        assertTrue(result.contains("bankName='TestBank'"));
        assertTrue(result.contains("verified=true"));
    }
}
