package com.skilora.finance.enums;

public enum Currency {
    TND("Tunisian Dinar", "TND"),
    EUR("Euro", "€"),
    USD("US Dollar", "$"),
    GBP("British Pound", "£");

    private final String displayName;
    private final String symbol;

    Currency(String displayName, String symbol) {
        this.displayName = displayName;
        this.symbol = symbol;
    }

    public String getDisplayName() { return displayName; }
    public String getSymbol() { return symbol; }
    public String getCode() { return name(); }
}
