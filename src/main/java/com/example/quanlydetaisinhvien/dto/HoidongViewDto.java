package com.example.quanlydetaisinhvien.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HoidongViewDto {
    private Long id;
    private Long dotdangkyid;
    private String tendot;
    private String tenhoidong;
    private LocalDateTime ngaybaocao;
    private String phong;
    private int sothanhvien;
    private String tenchutich;
    private String tenthuky;
    private List<String> danhsachthanhvien = new ArrayList<>();

    public HoidongViewDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDotdangkyid() {
        return dotdangkyid;
    }

    public void setDotdangkyid(Long dotdangkyid) {
        this.dotdangkyid = dotdangkyid;
    }

    public String getTendot() {
        return tendot;
    }

    public void setTendot(String tendot) {
        this.tendot = tendot;
    }

    public String getTenhoidong() {
        return tenhoidong;
    }

    public void setTenhoidong(String tenhoidong) {
        this.tenhoidong = tenhoidong;
    }

    public LocalDateTime getNgaybaocao() {
        return ngaybaocao;
    }

    public void setNgaybaocao(LocalDateTime ngaybaocao) {
        this.ngaybaocao = ngaybaocao;
    }

    public String getPhong() {
        return phong;
    }

    public void setPhong(String phong) {
        this.phong = phong;
    }

    public int getSothanhvien() {
        return sothanhvien;
    }

    public void setSothanhvien(int sothanhvien) {
        this.sothanhvien = sothanhvien;
    }

    public void setSoluongthanhvien(int soluongthanhvien) {
        this.sothanhvien = soluongthanhvien;
    }

    public int getSoluongthanhvien() {
        return this.sothanhvien;
    }

    public String getTenchutich() {
        return tenchutich;
    }

    public void setTenchutich(String tenchutich) {
        this.tenchutich = tenchutich;
    }

    public String getTenthuky() {
        return tenthuky;
    }

    public void setTenthuky(String tenthuky) {
        this.tenthuky = tenthuky;
    }

    public List<String> getDanhsachthanhvien() {
        return danhsachthanhvien;
    }

    public void setDanhsachthanhvien(List<String> danhsachthanhvien) {
        this.danhsachthanhvien = danhsachthanhvien;
    }

    public void setDanhsachThanhvien(String danhsach) {
        if (danhsach != null && !danhsach.isBlank()) {
            this.danhsachthanhvien = List.of(danhsach.split(",\\s*"));
        }
    }
}
