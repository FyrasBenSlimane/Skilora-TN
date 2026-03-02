package com.skilora.finance.service;

import com.skilora.finance.entity.ExchangeRate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * CurrencyApiService
 *
 * Fetches live exchange rates from a free API and persists them to the database.
 * Singleton service with periodic update throttling (6 hours).
 */
public class CurrencyApiService {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyApiService.class);

    private static final String API_BASE = "https://api.exchangerate-api.com/v4/latest/";
    private static final long UPDATE_THRESHOLD_HOURS = 6;

    private static volatile CurrencyApiService instance;
    private volatile LocalDateTime lastUpdateTime;

    private final HttpClient httpClient;

    private CurrencyApiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.lastUpdateTime = null;
    }

    public static CurrencyApiService getInstance() {
        if (instance == null) {
            synchronized (CurrencyApiService.class) {
                if (instance == null) {
                    instance = new CurrencyApiService();
                }
            }
        }
        return instance;
    }

    /**
     * Fetches latest exchange rates from the API for the given base currency.
     *
     * @param baseCurrency the base currency code (e.g. TND, USD, EUR)
     * @return map of target currency -> rate (1 base = rate target), or empty map on failure
     */
    public Map<String, BigDecimal> fetchLatestRates(String baseCurrency) {
        Map<String, BigDecimal> rates = new HashMap<>();
        String url = API_BASE + baseCurrency;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.warn("Currency API returned status {} for base {}", response.statusCode(), baseCurrency);
                return rates;
            }

            String body = response.body();
            parseRatesFromJson(body, rates);

        } catch (Exception e) {
            logger.error("Failed to fetch exchange rates for base {}: {}", baseCurrency, e.getMessage());
        }

        return rates;
    }

    /**
     * Parses JSON response using manual String operations (no external JSON library).
     * Expects format: {"rates":{"TND":1,"EUR":0.296,"USD":0.348,...}}
     */
    private void parseRatesFromJson(String json, Map<String, BigDecimal> outRates) {
        if (json == null || json.isEmpty()) {
            return;
        }

        int ratesStart = json.indexOf("\"rates\"");
        if (ratesStart < 0) {
            return;
        }

        int braceStart = json.indexOf('{', ratesStart);
        if (braceStart < 0) {
            return;
        }

        int depth = 1;
        int i = braceStart + 1;

        while (i < json.length() && depth > 0) {
            char c = json.charAt(i);

            if (c == '{') {
                depth++;
                i++;
                continue;
            }
            if (c == '}') {
                depth--;
                i++;
                continue;
            }

            if (c == '"' && depth == 1) {
                int keyEnd = json.indexOf('"', i + 1);
                if (keyEnd < 0) break;

                String key = json.substring(i + 1, keyEnd).trim();
                i = keyEnd + 1;

                int colon = json.indexOf(':', i);
                if (colon < 0) break;

                i = colon + 1;
                while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;

                int numEnd = i;
                while (numEnd < json.length()) {
                    char nc = json.charAt(numEnd);
                    if (nc == ',' || nc == '}' || nc == ']' || Character.isWhitespace(nc)) break;
                    if (nc == '.' || nc == '-' || (nc >= '0' && nc <= '9')) {
                        numEnd++;
                    } else {
                        break;
                    }
                }

                String numStr = json.substring(i, numEnd).trim();
                try {
                    BigDecimal rate = new BigDecimal(numStr);
                    outRates.put(key, rate);
                } catch (NumberFormatException ignored) {
                    // skip invalid values
                }

                i = numEnd;
            } else {
                i++;
            }
        }
    }

    /**
     * Fetches rates for TND, EUR, USD, GBP and saves to exchange_rates table.
     * Only performs fetch if last update was more than 6 hours ago.
     */
    public void updateExchangeRates() {
        if (lastUpdateTime != null) {
            long hoursSince = java.time.Duration.between(lastUpdateTime, LocalDateTime.now()).toHours();
            if (hoursSince < UPDATE_THRESHOLD_HOURS) {
                logger.debug("Skipping exchange rate update: last update {} hours ago", hoursSince);
                return;
            }
        }

        LocalDate today = LocalDate.now();
        String source = "exchangerate-api.com";
        ExchangeRateService ers = ExchangeRateService.getInstance();

        String[] bases = {"TND", "EUR", "USD", "GBP"};
        int saved = 0;

        for (String base : bases) {
            Map<String, BigDecimal> rates = fetchLatestRates(base);
            if (rates.isEmpty()) {
                logger.warn("No rates received for base {}", base);
                continue;
            }

            for (Map.Entry<String, BigDecimal> e : rates.entrySet()) {
                String toCurrency = e.getKey();
                BigDecimal rate = e.getValue();

                ExchangeRate er = new ExchangeRate(base, toCurrency, rate, today);
                er.setSource(source);
                er.setLastUpdated(LocalDateTime.now());

                try {
                    ers.save(er);
                    saved++;
                } catch (Exception ex) {
                    logger.error("Failed to save rate {}->{}: {}", base, toCurrency, ex.getMessage());
                }
            }
        }

        if (saved > 0) {
            lastUpdateTime = LocalDateTime.now();
            logger.info("Updated {} exchange rates from API", saved);
        }
    }

    /**
     * Forces an update regardless of last update time (e.g. for manual refresh).
     */
    public void forceUpdateExchangeRates() {
        lastUpdateTime = null;
        updateExchangeRates();
    }
}
