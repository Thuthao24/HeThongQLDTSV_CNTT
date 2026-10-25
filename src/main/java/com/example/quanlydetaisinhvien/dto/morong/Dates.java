package com.example.quanlydetaisinhvien.dto.morong;

import java.time.LocalDate;

final class Dates {
    private Dates() {}
    static boolean valid(LocalDate from, LocalDate to) {
        return validYear(from) && validYear(to) && (from == null || to == null || !from.isAfter(to));
    }
    private static boolean validYear(LocalDate date) {
        return date == null || date.getYear() >= 1900 && date.getYear() <= 9998;
    }
}
