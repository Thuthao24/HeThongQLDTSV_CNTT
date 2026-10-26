package com.example.quanlydetaisinhvien.dto;

import com.example.quanlydetaisinhvien.entity.TrangthaiLoimoi;
import java.time.LocalDateTime;

public class LoiMoiThanhVienDto {
    private Long id;
    private Long nhomid;
    private String tennhom;
    private Long dotdangkyid;
    private String tendot;
    private String tendetai;

    private Long sinhvienid;
    private String masinhvien;
    private String hotenSinhvien;

    private Long nguoimoiid;
    private String masinhvienNhomtruong;
    private String tenNhomtruong;

    private TrangthaiLoimoi trangthai;
    private LocalDateTime ngaytao;
    private LocalDateTime ngayphanhoi;

    public LoiMoiThanhVienDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNhomid() {
        return nhomid;
    }

    public void setNhomid(Long nhomid) {
        this.nhomid = nhomid;
    }

    public String getTennhom() {
        return tennhom;
    }

    public void setTennhom(String tennhom) {
        this.tennhom = tennhom;
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

    public String getTendetai() {
        return tendetai;
    }

    public void setTendetai(String tendetai) {
        this.tendetai = tendetai;
    }

    public Long getSinhvienid() {
        return sinhvienid;
    }

    public void setSinhvienid(Long sinhvienid) {
        this.sinhvienid = sinhvienid;
    }

    public String getMasinhvien() {
        return masinhvien;
    }

    public void setMasinhvien(String masinhvien) {
        this.masinhvien = masinhvien;
    }

    public String getHotenSinhvien() {
        return hotenSinhvien;
    }

    public void setHotenSinhvien(String hotenSinhvien) {
        this.hotenSinhvien = hotenSinhvien;
    }

    public Long getNguoimoiid() {
        return nguoimoiid;
    }

    public void setNguoimoiid(Long nguoimoiid) {
        this.nguoimoiid = nguoimoiid;
    }

    public String getMasinhvienNhomtruong() {
        return masinhvienNhomtruong;
    }

    public void setMasinhvienNhomtruong(String masinhvienNhomtruong) {
        this.masinhvienNhomtruong = masinhvienNhomtruong;
    }

    public String getTenNhomtruong() {
        return tenNhomtruong;
    }

    public void setTenNhomtruong(String tenNhomtruong) {
        this.tenNhomtruong = tenNhomtruong;
    }

    public TrangthaiLoimoi getTrangthai() {
        return trangthai;
    }

    public void setTrangthai(TrangthaiLoimoi trangthai) {
        this.trangthai = trangthai;
    }

    public LocalDateTime getNgaytao() {
        return ngaytao;
    }

    public void setNgaytao(LocalDateTime ngaytao) {
        this.ngaytao = ngaytao;
    }

    public LocalDateTime getNgayphanhoi() {
        return ngayphanhoi;
    }

    public void setNgayphanhoi(LocalDateTime ngayphanhoi) {
        this.ngayphanhoi = ngayphanhoi;
    }
}
