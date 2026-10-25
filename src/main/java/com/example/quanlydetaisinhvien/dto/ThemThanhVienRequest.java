package com.example.quanlydetaisinhvien.dto;

import jakarta.validation.constraints.NotBlank;

public class ThemThanhVienRequest {
    @NotBlank(message = "Mã số sinh viên không được để trống")
    private String masinhvien;

    public ThemThanhVienRequest() {}

    public ThemThanhVienRequest(String masinhvien) {
        this.masinhvien = masinhvien;
    }

    public String getMasinhvien() { return masinhvien; }
    public void setMasinhvien(String masinhvien) { this.masinhvien = masinhvien; }
}
