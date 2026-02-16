package com.skilora.finance.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for TaxCalculationService
 * Demonstrates the "Test Method" required by the workshop.
 */
class TaxCalculationServiceTest {

    // ==================== CNSS TESTS ====================

    @ParameterizedTest
    @DisplayName("Should calculate CNSS (9.18%) correctly for various salaries")
    @CsvSource({
            "1000, 91.80",
            "2000, 183.60",
            "5000, 459.00",
            "0, 0.00"
    })
    void calculateCNSS(String grossStr, String expectedCnssStr) {
        BigDecimal gross = new BigDecimal(grossStr);
        BigDecimal expectedCnss = new BigDecimal(expectedCnssStr);

        Map<String, BigDecimal> result = TaxCalculationService.calculateCompleteSalary(gross);

        assertEquals(expectedCnss, result.get("cnssEmployee"), "CNSS should be 9.18% of gross salary");
    }

    // ==================== IRPP TESTS ====================

    @Test
    @DisplayName("Should return 0 IRPP for salary below threshold (5000 TND / year equivalent approx)")
    void calculateIRPP_Zero() {
        // Very low monthly salary might result in 0 tax
        // Note: The service uses annual brackets logic applied to monthly?
        // Or assumes 'salaryInTND' is Annual?
        // Let's check TaxCalculationService logic:
        // It applies brackets directly to the input 'taxableIncome'.
        // Usually these brackets (0-5000) are ANNUAL in Tunisia.
        // If input is Monthly, this logic might be interpreting it as Annual or the
        // service handles Monthly conversion?
        // Looking at the code in TaxCalculationService.java:
        // It takes 'salaryInTND' (presumably monthly) and applies brackets {0, 5000,
        // 0}.
        // If 5000 is the ANNUAL limit, then for monthly 400 it works.
        // If 5000 is monthly limit (implied by just passing the number), then < 5000
        // monthly pays 0 tax.

        BigDecimal gross = new BigDecimal("4000"); // Below 5000 bracket
        Map<String, BigDecimal> result = TaxCalculationService.calculateCompleteSalary(gross);

        // Taxable = 4000 - (4000*0.0918) = 3632.8
        // 3632.8 < 5000 (Limit of 0% bracket)
        // So IRPP should be 0.

        assertEquals(BigDecimal.ZERO.setScale(2), result.get("irpp"));
    }

    @Test
    @DisplayName("Should calculate Net Salary correctly (Gross - CNSS - IRPP)")
    void calculateNetSalary() {
        BigDecimal gross = new BigDecimal("1000");
        Map<String, BigDecimal> result = TaxCalculationService.calculateCompleteSalary(gross);

        BigDecimal cnss = result.get("cnssEmployee");
        BigDecimal irpp = result.get("irpp");
        BigDecimal net = result.get("netSalary");

        // Net = Gross - CNSS - IRPP
        BigDecimal expectedNet = gross.subtract(cnss).subtract(irpp);

        assertEquals(expectedNet, net, "Net salary calculation mismatch");
    }

    // ==================== ROBUSTNESS TESTS ====================

    @Test
    @DisplayName("Should handle negative salary gracefully (return 0 or valid map)")
    void handleNegativeSalary() {
        BigDecimal gross = new BigDecimal("-100");
        Map<String, BigDecimal> result = TaxCalculationService.calculateCompleteSalary(gross);

        // Should logically result in negative or zero values, but not crash
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should provide optimization recommendations")
    void testOptimizationRecommendations() {
        BigDecimal gross = new BigDecimal("6000"); // Bracket 26%
        Map<String, String> recs = TaxCalculationService.getOptimizationRecommendations(gross);

        assertTrue(recs.containsKey("taxBracket"));
        assertTrue(recs.containsKey("recommendation"));
        assertNotNull(recs.get("recommendation"));
    }
}
