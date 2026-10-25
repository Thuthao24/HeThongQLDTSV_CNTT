package com.example.quanlydetaisinhvien.dto;

public class PhanbienViewDto {
    private Long id;
    private Long dangkydetaiid;
    private String tendetai;
    private String tennhom;
    private String tendot;
    private Long giangvienid;
    private String magiangvien;
    private String tengiangvien;
    private String email;
    private String danhsachGvhd;
    private boolean daCoDiem;
    private boolean daCongBo;

    public PhanbienViewDto() {}

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

    public String getTendot() {
        return tendot;
    }

    public void setTendot(String tendot) {
        this.tendot = tendot;
    }

    public Long getGiangvienid() {
        return giangvienid;
    }

    public void setGiangvienid(Long giangvienid) {
        this.giangvienid = giangvienid;
    }

    public String getMagiangvien() {
        return magiangvien;
    }

    public void setMagiangvien(String magiangvien) {
        this.magiangvien = magiangvien;
    }

    public String getTengiangvien() {
        return tengiangvien;
    }

    public void setTengiangvien(String tengiangvien) {
        this.tengiangvien = tengiangvien;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDanhsachGvhd() {
        return danhsachGvhd;
    }

    public void setDanhsachGvhd(String danhsachGvhd) {
        this.danhsachGvhd = danhsachGvhd;
    }

    public boolean isDaCoDiem() {
        return daCoDiem;
    }

    public void setDaCoDiem(boolean daCoDiem) {
        this.daCoDiem = daCoDiem;
    }

    public boolean isDaCongBo() {
        return daCongBo;
    }

    public void setDaCongBo(boolean daCongBo) {
        this.daCongBo = daCongBo;
    }
}
