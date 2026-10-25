package com.example.quanlydetaisinhvien.exception;

public class ExportLimitException extends RuntimeException {
    public ExportLimitException(String message) { super(message); }
    public static void check(long count, int limit) {
        if (count > limit) throw new ExportLimitException("Có " + count + " dòng, vượt giới hạn xuất " + limit
                + " dòng. Vui lòng thu hẹp bộ lọc.");
    }
}
