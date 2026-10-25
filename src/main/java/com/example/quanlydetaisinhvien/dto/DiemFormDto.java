package com.example.quanlydetaisinhvien.dto;

import java.math.BigDecimal;

public class DiemFormDto {
    private Long id;
    private Long dangkydetaiid;
    private Long giangvienid;
    private BigDecimal diemso;
    private String nhanxet;

    public DiemFormDto() {}

    public DiemFormDto(Long dangkydetaiid, Long giangvienid, BigDecimal diemso, String nhanxet) {
        this.dangkydetaiid = dangkydetaiid;
        this.giangvienid = giangvienid;
        this.diemso = diemso;
        this.nhanxet = nhanxet;
    }

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

    public Long getGiangvienid() {
        return giangvienid;
    }

    public void setGiangvienid(Long giangvienid) {
        this.giangvienid = giangvienid;
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
