package com.skilora.finance.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.finance.entity.TaxConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * TaxConfigurationService
 *
 * CRUD and progressive tax calculation for tax_configurations table.
 * Handles IRPP bracket-based calculation for Tunisia and other countries.
 * No JavaFX imports allowed.
 */
public class TaxConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(TaxConfigurationService.class);

    private static volatile TaxConfigurationService instance;

    private TaxConfigurationService() {}

    public static TaxConfigurationService getInstance() {
        if (instance == null) {
            synchronized (TaxConfigurationService.class) {
                if (instance == null) {
                    instance = new TaxConfigurationService();
                }
            }
        }
        return instance;
    }

    // ==================== CRUD Operations ====================

    /**
     * Creates a new tax configuration entry.
     * @return generated ID
     */
    public int create(TaxConfiguration tc) throws SQLException {
        String sql = "INSERT INTO tax_configurations (country, tax_type, rate, min_bracket, max_bracket, " +
                "description, effective_date, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, tc.getCountry());
            stmt.setString(2, tc.getTaxType());
            stmt.setBigDecimal(3, tc.getRate());
            stmt.setBigDecimal(4, tc.getMinBracket());

            if (tc.getMaxBracket() != null) {
                stmt.setBigDecimal(5, tc.getMaxBracket());
            } else {
                stmt.setNull(5, Types.DECIMAL);
            }

            stmt.setString(6, tc.getDescription());

            if (tc.getEffectiveDate() != null) {
                stmt.setDate(7, Date.valueOf(tc.getEffectiveDate()));
            } else {
                stmt.setNull(7, Types.DATE);
            }

            stmt.setBoolean(8, tc.isActive());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating tax configuration failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    tc.setId(generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Creating tax configuration failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Finds all active tax configurations for a country.
     */
    public List<TaxConfiguration> findByCountry(String country) throws SQLException {
        String sql = "SELECT * FROM tax_configurations WHERE country = ? AND is_active = TRUE";
        List<TaxConfiguration> configs = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, country);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    configs.add(mapResultSet(rs));
                }
            }
        }
        return configs;
    }

    /**
     * Finds active tax configurations for a country and tax type, ordered by min_bracket ASC.
     */
    public List<TaxConfiguration> findByCountryAndType(String country, String taxType) throws SQLException {
        String sql = "SELECT * FROM tax_configurations WHERE country = ? AND tax_type = ? " +
                "AND is_active = TRUE ORDER BY min_bracket ASC";
        List<TaxConfiguration> configs = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, country);
            stmt.setString(2, taxType);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    configs.add(mapResultSet(rs));
                }
            }
        }
        return configs;
    }

    /**
     * Updates an existing tax configuration.
     */
    public boolean update(TaxConfiguration tc) throws SQLException {
        String sql = "UPDATE tax_configurations SET country = ?, tax_type = ?, rate = ?, " +
                "min_bracket = ?, max_bracket = ?, description = ?, effective_date = ?, " +
                "is_active = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tc.getCountry());
            stmt.setString(2, tc.getTaxType());
            stmt.setBigDecimal(3, tc.getRate());
            stmt.setBigDecimal(4, tc.getMinBracket());

            if (tc.getMaxBracket() != null) {
                stmt.setBigDecimal(5, tc.getMaxBracket());
            } else {
                stmt.setNull(5, Types.DECIMAL);
            }

            stmt.setString(6, tc.getDescription());

            if (tc.getEffectiveDate() != null) {
                stmt.setDate(7, Date.valueOf(tc.getEffectiveDate()));
            } else {
                stmt.setNull(7, Types.DATE);
            }

            stmt.setBoolean(8, tc.isActive());
            stmt.setInt(9, tc.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a tax configuration by ID.
     */
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM tax_configurations WHERE id = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    // ==================== Tax Calculation ====================

    /**
     * Calculates progressive IRPP tax for a given annual salary.
     * Uses bracket-based progressive calculation:
     * For each bracket, tax = min(remaining salary in bracket, bracket size) * bracket rate.
     *
     * @param annualSalary the gross annual salary
     * @param country      the country code (e.g., "Tunisia")
     * @return total IRPP tax amount, rounded to 2 decimal places
     */
    public BigDecimal calculateIRPP(BigDecimal annualSalary, String country) {
        try {
            List<TaxConfiguration> brackets = findByCountryAndType(country, "IRPP");

            if (brackets.isEmpty()) {
                logger.warn("No IRPP brackets found for country: {}", country);
                return BigDecimal.ZERO;
            }

            BigDecimal totalTax = BigDecimal.ZERO;
            BigDecimal remainingSalary = annualSalary;

            for (TaxConfiguration bracket : brackets) {
                if (remainingSalary.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                BigDecimal bracketMin = bracket.getMinBracket();
                BigDecimal bracketMax = bracket.getMaxBracket();

                BigDecimal bracketSize;
                if (bracketMax != null) {
                    bracketSize = bracketMax.subtract(bracketMin);
                } else {
                    // Last bracket: no upper limit
                    bracketSize = remainingSalary;
                }

                BigDecimal taxableInBracket = remainingSalary.min(bracketSize);
                BigDecimal bracketTax = taxableInBracket.multiply(bracket.getRate())
                        .setScale(2, RoundingMode.HALF_UP);

                totalTax = totalTax.add(bracketTax);
                remainingSalary = remainingSalary.subtract(taxableInBracket);
            }

            return totalTax.setScale(2, RoundingMode.HALF_UP);

        } catch (SQLException e) {
            logger.error("Failed to calculate IRPP for country: {}", country, e);
            return BigDecimal.ZERO;
        }
    }

    // ==================== Private Helpers ====================

    /**
     * Maps a ResultSet row to a TaxConfiguration entity.
     * Handles nullable maxBracket.
     */
    private TaxConfiguration mapResultSet(ResultSet rs) throws SQLException {
        TaxConfiguration tc = new TaxConfiguration();
        tc.setId(rs.getInt("id"));
        tc.setCountry(rs.getString("country"));
        tc.setTaxType(rs.getString("tax_type"));
        tc.setRate(rs.getBigDecimal("rate"));
        tc.setMinBracket(rs.getBigDecimal("min_bracket"));

        BigDecimal maxBracket = rs.getBigDecimal("max_bracket");
        tc.setMaxBracket(maxBracket); // getBigDecimal returns null if SQL NULL

        tc.setDescription(rs.getString("description"));

        Date effectiveDate = rs.getDate("effective_date");
        tc.setEffectiveDate(effectiveDate != null ? effectiveDate.toLocalDate() : null);

        tc.setActive(rs.getBoolean("is_active"));

        return tc;
    }
}
