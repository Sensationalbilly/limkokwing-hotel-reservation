package com.hotel.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Consistent money and date formatting across the app. */
public final class Formats {

    /** Sierra Leonean Leone (new Leone). */
    public static final String CURRENCY = "SLE";

    private static final DecimalFormat MONEY =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat MONEY_COMPACT =
            new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_DATE =DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);

    private Formats() { }

    public static synchronized String money(BigDecimal amount) {
        return CURRENCY + " " + MONEY.format(amount == null ? BigDecimal.ZERO : amount);
    }

    /** Money without ".00" for whole amounts, e.g. "SLE 1,200". */
    public static synchronized String moneyCompact(BigDecimal amount) {
        return CURRENCY + " " + MONEY_COMPACT.format(amount == null ? BigDecimal.ZERO : amount);
    }

    /** e.g. "20 Nov" */
    public static String shortDate(LocalDate date) {
        return date == null ? "" : SHORT_DATE.format(date);
    }

    /** e.g. "9:01 AM" */
    public static String time(LocalTime time) {
        return time == null ? "" : TIME.format(time);
    }

    public static String date(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    public static String longDate(LocalDate date) {
        return date == null ? "" : LONG_DATE.format(date);
    }

    public static String dateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : DATE_TIME.format(dateTime);
    }
}
