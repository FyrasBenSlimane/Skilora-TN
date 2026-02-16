package com.skilora.finance.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for EmploymentContract entity (Finance)
 */
class EmploymentContractTest {

    private EmploymentContract contract;

    @BeforeEach
    void setUp() {
        contract = new EmploymentContract();
    }

    @Test
    void testDefaultConstructor() {
        assertNotNull(contract);
        assertNull(contract.getId());
        assertNull(contract.getStatus()); // Default is null unless set by parameterized constructor
    }

    @Test
    void testConstructorWithParameters() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        EmploymentContract c = new EmploymentContract(1, 2, new BigDecimal("3500.00"), "TND", start, "CDI");

        assertEquals(1, c.getUserId());
        assertEquals(2, c.getEmployerId());
        assertEquals(new BigDecimal("3500.00"), c.getSalaryBase());
        assertEquals("TND", c.getCurrency());
        assertEquals(start, c.getStartDate());
        assertEquals("CDI", c.getContractType());
        assertEquals("DRAFT", c.getStatus()); // Set by constructor logic
        assertFalse(c.getSigned()); // Set by constructor logic
    }

    @Test
    void testGettersAndSetters() {
        LocalDate end = LocalDate.of(2025, 1, 1);
        contract.setId(10);
        contract.setUserId(5);
        contract.setEmployerId(99);
        contract.setSalaryBase(new BigDecimal("4000.50"));
        contract.setCurrency("EUR");
        contract.setContractType("CDD");
        contract.setStatus("Active");
        contract.setPdfUrl("http://docs/contract.pdf");
        contract.setSigned(true);
        contract.setSignatureDate(end);
        contract.setEndDate(end);

        assertEquals(10, contract.getId());
        assertEquals(5, contract.getUserId());
        assertEquals(99, contract.getEmployerId());
        assertEquals(new BigDecimal("4000.50"), contract.getSalaryBase());
        assertEquals("EUR", contract.getCurrency());
        assertEquals("CDD", contract.getContractType());
        assertEquals("Active", contract.getStatus());
        assertEquals("http://docs/contract.pdf", contract.getPdfUrl());
        assertTrue(contract.getSigned());
        assertEquals(end, contract.getSignatureDate());
        assertEquals(end, contract.getEndDate());
    }

    @Test
    void testToString() {
        contract.setId(1);
        contract.setSalaryBase(new BigDecimal("1000"));
        contract.setCurrency("USD");
        contract.setStatus("DRAFT");

        String result = contract.toString();
        assertTrue(result.contains("EmploymentContract"));
        assertTrue(result.contains("id=1"));
        assertTrue(result.contains("salaryBase=1000"));
        assertTrue(result.contains("currency='USD'"));
        assertTrue(result.contains("status='DRAFT'"));
    }
}
