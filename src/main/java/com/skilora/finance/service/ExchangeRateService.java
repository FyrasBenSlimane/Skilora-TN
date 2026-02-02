package com.skilora.finance.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.finance.entity.ExchangeRate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ExchangeRateService
 *
 * CRUD and currency conversion for exchange_rates table.
 * Supports upsert, latest/dated rate lookup, and conversion.
 * No JavaFX imports allowed.
 */
public class ExchangeRateService {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateService.class);

    private static volatile ExchangeRateService instance;

    private ExchangeRateService() {}

    public static ExchangeRateService getInstance() {
        if (instance == null) {
            synchronized (ExchangeRateService.class) {
                if (instance == null) {
                    instance = new ExchangeRateService();
                }
            }
        }
        return instance;
    }

    // ==================== CRUD Operations ====================

    /**
     * Saves an exchange rate (upsert on from_currency, to_currency, rate_date).
     * Uses INSERT ON DUPLICATE KEY UPDATE.
     * @return generated or existing ID
     */
    public int save(ExchangeRate rate) throws SQLException {
        String sql = "INSERT INTO exchange_rates (from_currency, to_currency, rate, rate_date, source) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE rate = VALUES(rate), source = VALUES(source), " +
                "last_updated = NOW()";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, rate.getFromCurrency());
            stmt.setString(2, rate.getToCurrency());
            stmt.setBigDecimal(3, rate.getRate());

            if (rate.getRateDate() != null) {
                stmt.setDate(4, Date.valueOf(rate.getRateDate()));
            } else {
                stmt.setDate(4, Date.valueOf(LocalDate.now()));
            }

            stmt.setString(5, rate.getSource());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    rate.setId(generatedId);
                    return generatedId;
                }
            }
        }
        return -1;
    }

    /**
     * Gets the latest exchange rate between two currencies.
     */
    public ExchangeRate getRate(String from, String to) throws SQLException {
        String sql = "SELECT * FROM exchange_rates WHERE from_currency = ? AND to_currency = ? " +
                "ORDER BY rate_date DESC LIMIT 1";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, from);
            stmt.setString(2, to);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Gets the exchange rate between two currencies for a specific date.
     */
    public ExchangeRate getRate(String from, String to, LocalDate date) throws SQLException {
        String sql = "SELECT * FROM exchange_rates WHERE from_currency = ? AND to_currency = ? " +
                "AND rate_date = ?";

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, from);
            stmt.setString(2, to);
            stmt.setDate(3, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    /**
     * Converts an amount from one currency to another using the latest rate.
     * @return converted amount, or null if no rate found
     */
    public BigDecimal convert(BigDecimal amount, String from, String to) throws SQLException {
        if (from.equalsIgnoreCase(to)) {
            return amount;
        }

        ExchangeRate rate = getRate(from, to);
        if (rate == null || rate.getRate() == null) {
            logger.warn("No exchange rate found for {} -> {}", from, to);
            return null;
        }

        return amount.multiply(rate.getRate()).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Gets exchange rate history for a currency pair over the last N days.
     */
    public List<ExchangeRate> getHistory(String from, String to, int days) throws SQLException {
        String sql = "SELECT * FROM exchange_rates WHERE from_currency = ? AND to_currency = ? " +
                "AND rate_date >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
                "ORDER BY rate_date ASC";
        List<ExchangeRate> rates = new ArrayList<>();

        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, from);
            stmt.setString(2, to);
            stmt.setInt(3, days);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rates.add(mapResultSet(rs));
                }
            }
        }
        return rates;
    }

    // ==================== Private Helpers ====================

    /**
     * Maps a ResultSet row to an ExchangeRate entity.
     */
    private ExchangeRate mapResultSet(ResultSet rs) throws SQLException {
        ExchangeRate rate = new ExchangeRate();
        rate.setId(rs.getInt("id"));
        rate.setFromCurrency(rs.getString("from_currency"));
        rate.setToCurrency(rs.getString("to_currency"));
        rate.setRate(rs.getBigDecimal("rate"));

        Date rateDate = rs.getDate("rate_date");
        rate.setRateDate(rateDate != null ? rateDate.toLocalDate() : null);

        rate.setSource(rs.getString("source"));

        Timestamp lastUpdated = rs.getTimestamp("last_updated");
        rate.setLastUpdated(lastUpdated != null ? lastUpdated.toLocalDateTime() : null);

        return rate;
    }
}
