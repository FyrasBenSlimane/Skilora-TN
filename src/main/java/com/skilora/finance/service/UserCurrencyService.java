package com.skilora.finance.service;

import com.skilora.user.service.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * UserCurrencyService
 *
 * Manages user preferred currency and provides conversion/formatting utilities.
 */
public class UserCurrencyService {

    private static final Logger logger = LoggerFactory.getLogger(UserCurrencyService.class);

    public static final String KEY_PREFERRED_CURRENCY = "preferred_currency";
    public static final String DEFAULT_CURRENCY = "TND";

    private static volatile UserCurrencyService instance;

    private UserCurrencyService() {}

    public static UserCurrencyService getInstance() {
        if (instance == null) {
            synchronized (UserCurrencyService.class) {
                if (instance == null) {
                    instance = new UserCurrencyService();
                }
            }
        }
        return instance;
    }

    /**
     * Gets the user's preferred currency from user_preferences.
     *
     * @param userId the user ID
     * @return preferred currency code, or "TND" if not set
     */
    public String getPreferredCurrency(int userId) {
        return PreferencesService.getInstance()
                .getString(userId, KEY_PREFERRED_CURRENCY, DEFAULT_CURRENCY);
    }

    /**
     * Saves the user's preferred currency to user_preferences.
     *
     * @param userId   the user ID
     * @param currency the currency code (e.g. TND, EUR, USD)
     */
    public void setPreferredCurrency(int userId, String currency) {
        if (currency == null || currency.isBlank()) {
            currency = DEFAULT_CURRENCY;
        }
        PreferencesService.getInstance().set(userId, KEY_PREFERRED_CURRENCY, currency.trim().toUpperCase());
    }

    /**
     * Converts an amount from the given currency to the user's preferred currency.
     *
     * @param userId       the user ID
     * @param amount       the amount to convert
     * @param fromCurrency the source currency code
     * @return converted amount in preferred currency, or original amount if conversion fails
     */
    public double convertToPreferred(int userId, double amount, String fromCurrency) {
        String toCurrency = getPreferredCurrency(userId);
        if (fromCurrency == null || fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount;
        }

        try {
            BigDecimal result = ExchangeRateService.getInstance().convert(
                    BigDecimal.valueOf(amount), fromCurrency, toCurrency);
            return result != null ? result.doubleValue() : amount;
        } catch (Exception e) {
            logger.warn("Conversion failed {} -> {}: {}", fromCurrency, toCurrency, e.getMessage());
            return amount;
        }
    }

    /**
     * Formats an amount with the given currency for display.
     *
     * @param amount   the amount to format
     * @param currency the currency code (e.g. TND, EUR, USD)
     * @return formatted string like "3,500.00 TND"
     */
    public String formatAmount(double amount, String currency) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        nf.setGroupingUsed(true);

        String formatted = nf.format(amount);
        String curr = (currency != null && !currency.isBlank()) ? currency.trim().toUpperCase() : DEFAULT_CURRENCY;
        return formatted + " " + curr;
    }
}
