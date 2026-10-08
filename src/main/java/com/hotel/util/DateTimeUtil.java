package com.hotel.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utility helper class for date and time manipulations.
 * Demonstrates static utility methods and OOP helper design.
 */
public final class DateTimeUtil {

    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[] {
        DateTimeFormatter.ISO_LOCAL_DATE, // yyyy-MM-dd
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        new java.time.format.DateTimeFormatterBuilder().appendPattern("dd/MM/").appendValueReduced(java.time.temporal.ChronoField.YEAR, 2, 2, 2000).toFormatter(),
        new java.time.format.DateTimeFormatterBuilder().appendPattern("d/M/").appendValueReduced(java.time.temporal.ChronoField.YEAR, 2, 2, 2000).toFormatter(),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("d-M-yyyy"),
        new java.time.format.DateTimeFormatterBuilder().appendPattern("dd-MM-").appendValueReduced(java.time.temporal.ChronoField.YEAR, 2, 2, 2000).toFormatter(),
        new java.time.format.DateTimeFormatterBuilder().appendPattern("d-M-").appendValueReduced(java.time.temporal.ChronoField.YEAR, 2, 2, 2000).toFormatter(),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.ENGLISH)
    };

    // Private constructor prevents instantiation (OOP Best Practice for utility classes)
    private DateTimeUtil() {
        throw novelUnsupportedOperationException();
    }

    private static UnsupportedOperationException novelUnsupportedOperationException() {
        return new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String formatLocalDate(LocalDate date) {
        if (date == null) return "N/A";
        return date.format(DISPLAY_DATE_FORMAT);
    }

    /**
     * Flexibly parses date strings into a LocalDate.
     * Supports ISO (yyyy-MM-dd), common slash formats (dd/MM/yy, dd/MM/yyyy),
     * dash formats, and textual month formats without throwing unexpected conversion errors.
     * Returns null if text is null or blank.
     */
    public static LocalDate parseFlexibleLocalDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (java.time.format.DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException("Unable to parse date: " + text);
    }

    public static long calculateNights(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            return 0;
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        return Math.max(nights, 1);
    }
}
