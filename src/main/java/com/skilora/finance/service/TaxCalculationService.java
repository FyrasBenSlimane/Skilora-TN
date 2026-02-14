package com.skilora.finance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * AI-Powered Tax Calculation Service
 * Implements intelligent tax calculations for Tunisia (IRPP + CNSS)
 */
public class TaxCalculationService {

    // CNSS rates for Tunisia
    private static final BigDecimal CNSS_EMPLOYEE_RATE = new BigDecimal("0.0918"); // 9.18%
    private static final BigDecimal CNSS_EMPLOYER_RATE = new BigDecimal("0.165"); // 16.5%

    // IRPP Tunisia progressive tax brackets (2025)
    private static final BigDecimal[][] IRPP_BRACKETS = {
            { new BigDecimal("0"), new BigDecimal("5000"), new BigDecimal("0") }, // 0%
            { new BigDecimal("5000"), new BigDecimal("20000"), new BigDecimal("0.26") }, // 26%
            { new BigDecimal("20000"), new BigDecimal("30000"), new BigDecimal("0.28") }, // 28%
            { new BigDecimal("30000"), new BigDecimal("50000"), new BigDecimal("0.32") }, // 32%
            { new BigDecimal("50000"), new BigDecimal("999999999"), new BigDecimal("0.35") } // 35%
    };

    /**
     * Complete salary calculation for TND
     * Returns a detailed breakdown of all components
     */
    public static Map<String, BigDecimal> calculateCompleteSalary(BigDecimal salaryInTND) {
        Map<String, BigDecimal> breakdown = new HashMap<>();

        // Calculate CNSS
        BigDecimal cnssEmployee = salaryInTND.multiply(CNSS_EMPLOYEE_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal cnssEmployer = salaryInTND.multiply(CNSS_EMPLOYER_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        // Calculate taxable income (gross - CNSS)
        BigDecimal taxableIncome = salaryInTND.subtract(cnssEmployee);

        // Calculate IRPP (progressive)
        BigDecimal irpp = calculateProgressiveIRPP(taxableIncome);

        // Calculate net salary
        BigDecimal totalDeductions = cnssEmployee.add(irpp);
        BigDecimal netSalary = salaryInTND.subtract(totalDeductions);

        // Build comprehensive breakdown
        breakdown.put("grossSalaryTND", salaryInTND);
        breakdown.put("cnssEmployee", cnssEmployee);
        breakdown.put("cnssEmployer", cnssEmployer);
        breakdown.put("taxableIncome", taxableIncome);
        breakdown.put("irpp", irpp);
        breakdown.put("totalDeductions", totalDeductions);
        breakdown.put("netSalary", netSalary);
        breakdown.put("effectiveTaxRate",
                salaryInTND.compareTo(BigDecimal.ZERO) > 0
                        ? totalDeductions.divide(salaryInTND, 4, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"))
                        : BigDecimal.ZERO);

        return breakdown;
    }

    /**
     * AI-Powered progressive IRPP calculation
     * Calculates tax based on Tunisian progressive brackets
     */
    private static BigDecimal calculateProgressiveIRPP(BigDecimal taxableIncome) {
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal remainingIncome = taxableIncome;

        for (BigDecimal[] bracket : IRPP_BRACKETS) {
            BigDecimal lowerBound = bracket[0];
            BigDecimal upperBound = bracket[1];
            BigDecimal taxRate = bracket[2];

            if (remainingIncome.compareTo(lowerBound) <= 0) {
                break;
            }

            BigDecimal bracketSize = upperBound.subtract(lowerBound);
            BigDecimal taxableInBracket = remainingIncome.min(bracketSize);
            BigDecimal taxForBracket = taxableInBracket.multiply(taxRate);

            totalTax = totalTax.add(taxForBracket);
            remainingIncome = remainingIncome.subtract(taxableInBracket);

            if (remainingIncome.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
        }

        return totalTax.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * AI Recommendation: Optimal salary structure
     * Suggests best salary breakdown for tax optimization
     */
    public static Map<String, String> getOptimizationRecommendations(BigDecimal grossSalary) {
        Map<String, String> recommendations = new HashMap<>();

        if (grossSalary.compareTo(new BigDecimal("5000")) < 0) {
            recommendations.put("taxBracket", "0% - Optimal bracket");
            recommendations.put("recommendation", "✅ No IRPP tax. Consider asking for bonuses instead of raises.");
        } else if (grossSalary.compareTo(new BigDecimal("20000")) < 0) {
            recommendations.put("taxBracket", "26% - Low bracket");
            recommendations.put("recommendation",
                    "💡 Consider negotiating benefits (transport, meal vouchers) to reduce taxable income.");
        } else if (grossSalary.compareTo(new BigDecimal("30000")) < 0) {
            recommendations.put("taxBracket", "28% - Medium bracket");
            recommendations.put("recommendation",
                    "💡 You're close to 32% bracket. Consider salary + stock options combination.");
        } else if (grossSalary.compareTo(new BigDecimal("50000")) < 0) {
            recommendations.put("taxBracket", "32% - High bracket");
            recommendations.put("recommendation",
                    "⚠️ High tax rate. Explore retirement savings (CNAM) for deductions.");
        } else {
            recommendations.put("taxBracket", "35% - Maximum bracket");
            recommendations.put("recommendation",
                    "⚠️ Maximum tax rate. Strongly consider tax optimization strategies.");
        }

        return recommendations;
    }

    /**
     * AI Market Comparison
     * Compares salary against market average
     */
    public static String compareToMarket(BigDecimal salary, String position) {
        // Simulated AI market data (in real app, this would query a database)
        Map<String, BigDecimal> marketAverages = new HashMap<>();
        marketAverages.put("Developer", new BigDecimal("3000"));
        marketAverages.put("Senior Developer", new BigDecimal("5000"));
        marketAverages.put("Manager", new BigDecimal("7000"));
        marketAverages.put("Director", new BigDecimal("12000"));

        BigDecimal marketAvg = marketAverages.getOrDefault(position, new BigDecimal("3000"));
        BigDecimal difference = salary.subtract(marketAvg);
        BigDecimal percentDiff = difference.divide(marketAvg, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        if (percentDiff.compareTo(BigDecimal.ZERO) > 0) {
            return String.format("✅ You earn %.1f%% above market average for %s",
                    percentDiff.doubleValue(), position);
        } else {
            return String.format("📊 You earn %.1f%% below market average for %s. Consider negotiating.",
                    Math.abs(percentDiff.doubleValue()), position);
        }
    }
}
