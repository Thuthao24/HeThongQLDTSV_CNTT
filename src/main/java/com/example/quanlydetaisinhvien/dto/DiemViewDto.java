package com.example.quanlydetaisinhvien.dto;

import java.math.BigDecimal;

public class DiemViewDto {
    private Long id;
    private Long dangkydetaiid;
    private String tendetai;
    private String tennhom;
    private Long giangvienid;
    private String tengiangvien;
    private BigDecimal diemso;
    private String nhanxet;

    public DiemViewDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDangkydetaiid() {
        return dangkydetaiid;
    }

    public void setDangkydetaiid(Long dangkydetaiid) {
        this.dangkydetaiid = dangkydetaiid;
    }

    public String getTendetai() {
        return tendetai;
    }

    public void setTendetai(String tendetai) {
        this.tendetai = tendetai;
    }

    public String getTennhom() {
        return tennhom;
    }

    public void setTennhom(String tennhom) {
        this.tennhom = tennhom;
    }

    public Long getGiangvienid() {
        return giangvienid;
    }

    public void setGiangvienid(Long giangvienid) {
        this.giangvienid = giangvienid;
    }

    public String getTengiangvien() {
        return tengiangvien;
    }

    public void setTengiangvien(String tengiangvien) {
        this.tengiangvien = tengiangvien;
    }

    public BigDecimal getDiemso() {
        return diemso;
    }

    public void setDiemso(BigDecimal diemso) {
        this.diemso = diemso;
    }

    public String getNhanxet() {
        return nhanxet;
    }

    public void setNhanxet(String nhanxet) {
        this.nhanxet = nhanxet;
    }
}
