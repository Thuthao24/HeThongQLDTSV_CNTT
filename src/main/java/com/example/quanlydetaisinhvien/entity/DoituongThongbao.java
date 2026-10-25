package com.example.quanlydetaisinhvien.entity;

/** The dean manages notices; only these three roles are recipients. */
public enum DoituongThongbao {
    SINHVIEN("Sinh viên"), GIANGVIEN("Giảng viên"), TRUONGBOMON("Trưởng bộ môn");
    private final String label;
    DoituongThongbao(String label) { this.label=label; }
    public String getLabel() { return label; }
    public static java.util.Set<DoituongThongbao> all() {
        return java.util.EnumSet.allOf(DoituongThongbao.class);
    }
}
