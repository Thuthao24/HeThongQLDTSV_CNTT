package com.example.quanlydetaisinhvien.dto;

public class DangkydetaiOptionDto {
    private Long id;
    private String hienthi;
    private String danhsachGvhd;

    public DangkydetaiOptionDto() {}

    public DangkydetaiOptionDto(Long id, String hienthi, String danhsachGvhd) {
        this.id = id;
        this.hienthi = hienthi;
        this.danhsachGvhd = danhsachGvhd;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHienthi() {
        return hienthi;
    }

    public void setHienthi(String hienthi) {
        this.hienthi = hienthi;
    }

    public String getDanhsachGvhd() {
        return danhsachGvhd;
    }

    public void setDanhsachGvhd(String danhsachGvhd) {
        this.danhsachGvhd = danhsachGvhd;
    }
}
