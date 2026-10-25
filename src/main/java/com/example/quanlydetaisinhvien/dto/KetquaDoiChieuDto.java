package com.example.quanlydetaisinhvien.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class KetquaDoiChieuDto {
    private Long id;
    private Long dangkydetaiid;
    private String tendetai;
    private String tennhom;
    private BigDecimal diemcuoi;
    private BigDecimal diemtrungbinhthanhphan;
    private long sodiemthanhphan;
    private boolean khop;
    private Boolean dacongbo;
    private LocalDateTime ngaycongbo;

    public KetquaDoiChieuDto() {}

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

    public BigDecimal getDiemcuoi() {
        return diemcuoi;
    }

    public void setDiemcuoi(BigDecimal diemcuoi) {
        this.diemcuoi = diemcuoi;
    }

    public BigDecimal getDiemtrungbinhthanhphan() {
        return diemtrungbinhthanhphan;
    }

    public void setDiemtrungbinhthanhphan(BigDecimal diemtrungbinhthanhphan) {
        this.diemtrungbinhthanhphan = diemtrungbinhthanhphan;
    }

    public long getSodiemthanhphan() {
        return sodiemthanhphan;
    }

    public void setSodiemthanhphan(long sodiemthanhphan) {
        this.sodiemthanhphan = sodiemthanhphan;
    }

    public boolean isKhop() {
        return khop;
    }

    public void setKhop(boolean khop) {
        this.khop = khop;
    }

    public Boolean getDacongbo() {
        return dacongbo;
    }

    public void setDacongbo(Boolean dacongbo) {
        this.dacongbo = dacongbo;
    }

    public LocalDateTime getNgaycongbo() {
        return ngaycongbo;
    }

    public void setNgaycongbo(LocalDateTime ngaycongbo) {
        this.ngaycongbo = ngaycongbo;
    }
}
