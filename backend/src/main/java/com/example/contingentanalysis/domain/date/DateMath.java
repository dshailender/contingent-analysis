package com.example.contingentanalysis.domain.date;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DateMath {

    public static final Map<Integer, String> BASIS_NAMES;

    static {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(0, "US 30/360");
        map.put(1, "Actual/Actual");
        map.put(2, "Actual/360");
        map.put(3, "Actual/365");
        map.put(4, "European 30/360");
        BASIS_NAMES = Collections.unmodifiableMap(map);
    }

    private DateMath() {
    }

    public static LocalDate parseDate(String d) {
        if (d == null || d.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot parse empty date");
        }
        String s = d.trim();
        if (s.contains("T")) {
            s = s.split("T")[0];
        }
        return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static boolean isLeapYear(int y) {
        return (y % 4 == 0) && (y % 100 != 0 || y % 400 == 0);
    }

    public static long daysBetween(LocalDate d1, LocalDate d2) {
        return ChronoUnit.DAYS.between(d1, d2);
    }

    public static double yearFraction(String d1Raw, String d2Raw, int basis) {
        if (d1Raw == null || d1Raw.trim().isEmpty() || d2Raw == null || d2Raw.trim().isEmpty()) {
            return 0.0;
        }

        LocalDate d1 = parseDate(d1Raw);
        LocalDate d2 = parseDate(d2Raw);

        double sign = 1.0;
        if (d2.isBefore(d1)) {
            LocalDate tmp = d1;
            d1 = d2;
            d2 = tmp;
            sign = -1.0;
        }

        long days = daysBetween(d1, d2);
        if (days == 0) {
            return 0.0;
        }

        if (basis == 2) {
            // Actual/360
            return sign * (days / 360.0);
        }

        if (basis == 3) {
            // Actual/365
            return sign * (days / 365.0);
        }

        if (basis == 0 || basis == 4) {
            // 30/360 conventions
            int y1 = d1.getYear();
            int m1 = d1.getMonthValue();
            int dd1 = d1.getDayOfMonth();

            int y2 = d2.getYear();
            int m2 = d2.getMonthValue();
            int dd2 = d2.getDayOfMonth();

            if (basis == 0) {
                // US 30/360
                if (dd1 == 31) {
                    dd1 = 30;
                }
                if (dd2 == 31 && dd1 >= 30) {
                    dd2 = 30;
                }
            } else {
                // European 30/360
                dd1 = Math.min(dd1, 30);
                dd2 = Math.min(dd2, 30);
            }

            int numDays = (y2 - y1) * 360 + (m2 - m1) * 30 + (dd2 - dd1);
            return sign * (numDays / 360.0);
        }

        // Basis 1: Actual/Actual (Excel YEARFRAC: actual days / average days in calendar years spanned)
        int y1 = d1.getYear();
        int y2 = d2.getYear();
        int yearsCount = y2 - y1 + 1;
        long totalYearDays = 0;
        for (int y = y1; y <= y2; y++) {
            totalYearDays += isLeapYear(y) ? 366 : 365;
        }
        double avgYearDays = (double) totalYearDays / (double) yearsCount;
        return sign * (days / avgYearDays);
    }
}

