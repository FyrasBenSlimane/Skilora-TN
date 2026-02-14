package com.skilora.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Date Utilities
 * 
 * Static utility methods for date operations.
 * Used across all gestions for date formatting and manipulation.
 * 
 * METHODS:
 * - formatDate(): Format LocalDate to string
 * - formatDateTime(): Format LocalDateTime to string
 * - parseDate(): Parse string to LocalDate
 * - getCurrentDate(): Get current date
 * - getCurrentDateTime(): Get current date/time
 * - addDays(): Add days to date
 * - daysBetween(): Calculate days between dates
 */
public class DateUtils {
    
    // Standard date formats
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DISPLAY_DATE_FORMAT = "dd/MM/yyyy";
    public static final String DISPLAY_DATETIME_FORMAT = "dd/MM/yyyy HH:mm";
    
    /**
     * Formats a LocalDate to string using default format (yyyy-MM-dd).
     * 
     * @param date Date to format
     * @return Formatted date string
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
    }
    
    /**
     * Formats a LocalDate to string using custom format.
     * 
     * @param date Date to format
     * @param pattern Date format pattern
     * @return Formatted date string
     */
    public static String formatDate(LocalDate date, String pattern) {
        if (date == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }
    
    /**
     * Formats a LocalDate to display format (dd/MM/yyyy).
     * 
     * @param date Date to format
     * @return Formatted date string for display
     */
    public static String formatDateForDisplay(LocalDate date) {
        return formatDate(date, DISPLAY_DATE_FORMAT);
    }
    
    /**
     * Formats a LocalDateTime to string using default format.
     * 
     * @param dateTime DateTime to format
     * @return Formatted datetime string
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern(DATETIME_FORMAT));
    }
    
    /**
     * Formats a LocalDateTime to display format.
     * 
     * @param dateTime DateTime to format
     * @return Formatted datetime string for display
     */
    public static String formatDateTimeForDisplay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern(DISPLAY_DATETIME_FORMAT));
    }
    
    /**
     * Parses a string to LocalDate using default format (yyyy-MM-dd).
     * 
     * @param dateString Date string to parse
     * @return LocalDate object
     * @throws java.time.format.DateTimeParseException if parsing fails
     */
    public static LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(DATE_FORMAT));
    }
    
    /**
     * Parses a string to LocalDate using custom format.
     * 
     * @param dateString Date string to parse
     * @param pattern Date format pattern
     * @return LocalDate object
     * @throws java.time.format.DateTimeParseException if parsing fails
     */
    public static LocalDate parseDate(String dateString, String pattern) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(pattern));
    }
    
    /**
     * Gets the current date.
     * 
     * @return Current LocalDate
     */
    public static LocalDate getCurrentDate() {
        return LocalDate.now();
    }
    
    /**
     * Gets the current date and time.
     * 
     * @return Current LocalDateTime
     */
    public static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now();
    }
    
    /**
     * Adds days to a date.
     * 
     * @param date Base date
     * @param days Number of days to add (can be negative)
     * @return New date with days added
     */
    public static LocalDate addDays(LocalDate date, int days) {
        if (date == null) {
            return null;
        }
        return date.plusDays(days);
    }
    
    /**
     * Calculates days between two dates.
     * 
     * @param startDate Start date
     * @param endDate End date
     * @return Number of days between dates
     */
    public static long daysBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(startDate, endDate);
    }
    
    /**
     * Checks if a date is in the past.
     * 
     * @param date Date to check
     * @return true if date is before today
     */
    public static boolean isPast(LocalDate date) {
        if (date == null) {
            return false;
        }
        return date.isBefore(LocalDate.now());
    }
    
    /**
     * Checks if a date is in the future.
     * 
     * @param date Date to check
     * @return true if date is after today
     */
    public static boolean isFuture(LocalDate date) {
        if (date == null) {
            return false;
        }
        return date.isAfter(LocalDate.now());
    }
}
