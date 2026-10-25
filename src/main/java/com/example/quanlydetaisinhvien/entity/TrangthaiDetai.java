
package com.example.quanlydetaisinhvien.entity;

public enum TrangthaiDetai {
    CHODUYET("Chờ duyệt"),
    DADUYET("Đã duyệt"),
    CONGBO("Đã công bố"),
    TUCHOI("Từ chối");

    private final String hienthi;

    TrangthaiDetai(String hienthi) {
        this.hienthi = hienthi;
    }

    public String getHienthi() {
        return hienthi;
    }
}
