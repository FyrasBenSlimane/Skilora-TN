package com.skilora.finance.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Payslip entity (Finance)
 */
class PayslipTest {

    private Payslip payslip;

    @BeforeEach
    void setUp() {
        payslip = new Payslip();
    }

    @Test
    void testDefaultConstructor() {
        assertNotNull(payslip);
        assertNull(payslip.getId());
    }

    @Test
    void testConstructorWithParameters() {
        Payslip p = new Payslip(101, 5, 2024, new BigDecimal("2500.00"), "TND");

        assertEquals(101, p.getUserId());
        assertEquals(5, p.getMonth());
        assertEquals(2024, p.getYear());
        assertEquals(new BigDecimal("2500.00"), p.getGrossSalary());
        assertEquals("TND", p.getCurrency());
        assertEquals("DRAFT", p.getStatus()); // Default set by constructor
    }

    @Test
    void testGettersAndSetters() {
        LocalDate payDate = LocalDate.of(2024, 5, 30);

        payslip.setId(55);
        payslip.setUserId(22);
        payslip.setMonth(6);
        payslip.setYear(2025);
        payslip.setGrossSalary(new BigDecimal("3000.00"));
        payslip.setNetSalary(new BigDecimal("2400.00"));
        payslip.setCurrency("EUR");
        payslip.setStatus("PAID");
        payslip.setPdfUrl("http://payslips/jan.pdf");
        payslip.setPaymentDate(payDate);
        payslip.setDeductionsJson("{cnss: 200}");
        payslip.setBonusesJson("{bonus: 100}");

        assertEquals(55, payslip.getId());
        assertEquals(22, payslip.getUserId());
        assertEquals(6, payslip.getMonth());
        assertEquals(2025, payslip.getYear());
        assertEquals(new BigDecimal("3000.00"), payslip.getGrossSalary());
        assertEquals(new BigDecimal("2400.00"), payslip.getNetSalary());
        assertEquals("EUR", payslip.getCurrency());
        assertEquals("PAID", payslip.getStatus());
        assertEquals("http://payslips/jan.pdf", payslip.getPdfUrl());
        assertEquals(payDate, payslip.getPaymentDate());
        assertEquals("{cnss: 200}", payslip.getDeductionsJson());
        assertEquals("{bonus: 100}", payslip.getBonusesJson());
    }

    @Test
    void testToString() {
        payslip.setId(7);
        payslip.setMonth(1);
        payslip.setYear(2024);
        payslip.setStatus("SENT");

        String result = payslip.toString();
        assertTrue(result.contains("Payslip"));
        assertTrue(result.contains("id=7"));
        assertTrue(result.contains("month=1"));
        assertTrue(result.contains("status='SENT'"));
    }
}
