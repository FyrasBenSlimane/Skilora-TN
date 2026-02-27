package com.skilora.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    public static String format(LocalDate date) {
        return date == null ? "" : date.format(ISO);
    }

    public static LocalDate parse(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(s.trim(), ISO);
        } catch (Exception e) {
            return null;
        }
    }
}
