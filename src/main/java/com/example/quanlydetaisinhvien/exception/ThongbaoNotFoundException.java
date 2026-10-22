package com.example.quanlydetaisinhvien.exception;

public class ThongbaoNotFoundException extends RuntimeException {
    public ThongbaoNotFoundException() { super("Không tìm thấy thông báo."); }
}
