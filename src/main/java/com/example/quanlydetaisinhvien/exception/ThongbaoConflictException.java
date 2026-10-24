package com.example.quanlydetaisinhvien.exception;

public class ThongbaoConflictException extends RuntimeException {
    public ThongbaoConflictException() { super("Thông báo đã thay đổi. Vui lòng mở lại bản mới nhất trước khi sửa."); }
}
