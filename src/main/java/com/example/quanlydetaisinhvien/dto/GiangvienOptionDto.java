package com.example.quanlydetaisinhvien.dto;

public class GiangvienOptionDto {
    private Long id;
    private String magiangvien;
    private String hoten;

    public GiangvienOptionDto() {}

    public GiangvienOptionDto(Long id, String magiangvien, String hoten) {
        this.id = id;
        this.magiangvien = magiangvien;
        this.hoten = hoten;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMagiangvien() {
        return magiangvien;
    }

    public void setMagiangvien(String magiangvien) {
        this.magiangvien = magiangvien;
    }

    public String getHoten() {
        return hoten;
    }

    public void setHoten(String hoten) {
        this.hoten = hoten;
    }

    public String getHienthi() {
        return magiangvien + (hoten != null && !hoten.isBlank() ? " - " + hoten : "");
    }
}
