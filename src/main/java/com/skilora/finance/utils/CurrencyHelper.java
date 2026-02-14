package com.skilora.finance.utils;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Currency Helper - Provides list of world currencies
 */
public class CurrencyHelper {

    public static ObservableList<String> getWorldCurrencies() {
        return FXCollections.observableArrayList(
                "TND - Tunisian Dinar",
                "EUR - Euro",
                "USD - US Dollar",
                "GBP - British Pound",
                "CHF - Swiss Franc",
                "CAD - Canadian Dollar",
                "AUD - Australian Dollar",
                "JPY - Japanese Yen",
                "CNY - Chinese Yuan",
                "AED - UAE Dirham",
                "SAR - Saudi Riyal",
                "QAR - Qatari Riyal",
                "KWD - Kuwaiti Dinar",
                "BHD - Bahraini Dinar",
                "OMR - Omani Rial",
                "JOD - Jordanian Dinar",
                "EGP - Egyptian Pound",
                "MAD - Moroccan Dirham",
                "DZD - Algerian Dinar",
                "LYD - Libyan Dinar",
                "MRU - Mauritanian Ouguiya",
                "SDG - Sudanese Pound",
                "INR - Indian Rupee",
                "PKR - Pakistani Rupee",
                "BDT - Bangladeshi Taka",
                "LKR - Sri Lankan Rupee",
                "THB - Thai Baht",
                "MYR - Malaysian Ringgit",
                "SGD - Singapore Dollar",
                "IDR - Indonesian Rupiah",
                "PHP - Philippine Peso",
                "VND - Vietnamese Dong",
                "KRW - South Korean Won",
                "TWD - New Taiwan Dollar",
                "HKD - Hong Kong Dollar",
                "RUB - Russian Ruble",
                "TRY - Turkish Lira",
                "PLN - Polish Zloty",
                "CZK - Czech Koruna",
                "HUF - Hungarian Forint",
                "RON - Romanian Leu",
                "SEK - Swedish Krona",
                "NOK - Norwegian Krone",
                "DKK - Danish Krone",
                "ISK - Icelandic Króna",
                "BRL - Brazilian Real",
                "MXN - Mexican Peso",
                "ARS - Argentine Peso",
                "CLP - Chilean Peso",
                "COP - Colombian Peso",
                "PEN - Peruvian Sol",
                "ZAR - South African Rand",
                "NGN - Nigerian Naira",
                "KES - Kenyan Shilling",
                "GHS - Ghanaian Cedi",
                "XOF - West African CFA Franc",
                "XAF - Central African CFA Franc");
    }

    public static String getCurrencyCode(String fullCurrency) {
        if (fullCurrency == null || !fullCurrency.contains(" - ")) {
            return fullCurrency;
        }
        return fullCurrency.split(" - ")[0];
    }

    public static String getFullCurrencyName(String code) {
        ObservableList<String> currencies = getWorldCurrencies();
        for (String currency : currencies) {
            if (currency.startsWith(code + " - ")) {
                return currency;
            }
        }
        return code;
    }
}
