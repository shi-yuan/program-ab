package org.alicebot.util;

import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Months;
import org.joda.time.Years;
import org.joda.time.chrono.GregorianChronology;
import org.joda.time.chrono.LenientChronology;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class IntervalUtils {

    public static int getHoursBetween(final String date1, final String date2, String format) {
        final DateTimeFormatter fmt =
                DateTimeFormat
                        .forPattern(format)
                        .withChronology(
                                LenientChronology.getInstance(
                                        GregorianChronology.getInstance()));
        return Hours.hoursBetween(
                fmt.parseDateTime(date1),
                fmt.parseDateTime(date2)
        ).getHours();
    }

    public static int getYearsBetween(final String date1, final String date2, String format) {
        final DateTimeFormatter fmt =
                DateTimeFormat
                        .forPattern(format)
                        .withChronology(
                                LenientChronology.getInstance(
                                        GregorianChronology.getInstance()));
        return Years.yearsBetween(
                fmt.parseDateTime(date1),
                fmt.parseDateTime(date2)
        ).getYears();
    }

    public static int getMonthsBetween(final String date1, final String date2, String format) {
        final DateTimeFormatter fmt =
                DateTimeFormat
                        .forPattern(format)
                        .withChronology(
                                LenientChronology.getInstance(
                                        GregorianChronology.getInstance()));
        return Months.monthsBetween(
                fmt.parseDateTime(date1),
                fmt.parseDateTime(date2)
        ).getMonths();
    }

    public static int getDaysBetween(final String date1, final String date2, String format) {
        final DateTimeFormatter fmt =
                DateTimeFormat
                        .forPattern(format)
                        .withChronology(
                                LenientChronology.getInstance(
                                        GregorianChronology.getInstance()));
        return Days.daysBetween(
                fmt.parseDateTime(date1),
                fmt.parseDateTime(date2)
        ).getDays();
    }
}
