package com.skilora.finance.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Bonus entity (Finance)
 */
class BonusTest {

    private Bonus bonus;

    @BeforeEach
    void setUp() {
        bonus = new Bonus();
    }

    @Test
    void testDefaultConstructor() {
        assertNotNull(bonus);
        assertNull(bonus.getId());
    }

    @Test
    void testConstructorWithParameters() {
        Bonus b = new Bonus(7, "Performance", new BigDecimal("500.00"), "TND", "Good job");

        assertEquals(7, b.getUserId());
        assertEquals("Performance", b.getType());
        assertEquals(new BigDecimal("500.00"), b.getAmount());
        assertEquals("TND", b.getCurrency());
        assertEquals("Good job", b.getReason());
        assertEquals("PENDING", b.getStatus()); // Default
        assertNotNull(b.getDateAwarded()); // Should be LocalDate.now()
    }

    @Test
    void testGettersAndSetters() {
        LocalDate date = LocalDate.of(2023, 12, 1);
        bonus.setId(33);
        bonus.setUserId(44);
        bonus.setType("Yearly");
        bonus.setAmount(new BigDecimal("1000.00"));
        bonus.setCurrency("USD");
        bonus.setReason("Sales target met");
        bonus.setDateAwarded(date);
        bonus.setApprovedBy(1);
        bonus.setStatus("APPROVED");

        assertEquals(33, bonus.getId());
        assertEquals(44, bonus.getUserId());
        assertEquals("Yearly", bonus.getType());
        assertEquals(new BigDecimal("1000.00"), bonus.getAmount());
        assertEquals("USD", bonus.getCurrency());
        assertEquals("Sales target met", bonus.getReason());
        assertEquals(date, bonus.getDateAwarded());
        assertEquals(1, bonus.getApprovedBy());
        assertEquals("APPROVED", bonus.getStatus());
    }

    @Test
    void testToString() {
        bonus.setId(5);
        bonus.setType("Bonus");
        bonus.setStatus("PENDING");

        String result = bonus.toString();
        assertTrue(result.contains("Bonus"));
        assertTrue(result.contains("id=5"));
        assertTrue(result.contains("type='Bonus'"));
        assertTrue(result.contains("status='PENDING'"));
    }
}
