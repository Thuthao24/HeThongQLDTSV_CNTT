package com.example.quanlydetaisinhvien.dto;

import java.util.ArrayList;
import java.util.List;

public class NhomSinhVienDto {
    private Long id;
    private Long dotdangkyid;
    private String tendot;
    private String tennhom;
    private Long nhomtruongid;
    private String tenNhomtruong;
    private String masinhvienNhomtruong;
    private int soThanhvien;
    private Long dangkydetaiid;
    private Long detaiid;
    private String tendetai;
    private String trangthaiDangky;
    private List<ThanhVienDto> danhSachThanhVien = new ArrayList<>();

    public NhomSinhVienDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDotdangkyid() { return dotdangkyid; }
    public void setDotdangkyid(Long dotdangkyid) { this.dotdangkyid = dotdangkyid; }

    public String getTendot() { return tendot; }
    public void setTendot(String tendot) { this.tendot = tendot; }

    public String getTennhom() { return tennhom; }
    public void setTennhom(String tennhom) { this.tennhom = tennhom; }

    public Long getNhomtruongid() { return nhomtruongid; }
    public void setNhomtruongid(Long nhomtruongid) { this.nhomtruongid = nhomtruongid; }

    public String getTenNhomtruong() { return tenNhomtruong; }
    public void setTenNhomtruong(String tenNhomtruong) { this.tenNhomtruong = tenNhomtruong; }

    public String getMasinhvienNhomtruong() { return masinhvienNhomtruong; }
    public void setMasinhvienNhomtruong(String masinhvienNhomtruong) { this.masinhvienNhomtruong = masinhvienNhomtruong; }

    public int getSoThanhvien() { return soThanhvien; }
    public void setSoThanhvien(int soThanhvien) { this.soThanhvien = soThanhvien; }

    public Long getDangkydetaiid() { return dangkydetaiid; }
    public void setDangkydetaiid(Long dangkydetaiid) { this.dangkydetaiid = dangkydetaiid; }

    public Long getDetaiid() { return detaiid; }
    public void setDetaiid(Long detaiid) { this.detaiid = detaiid; }

    public String getTendetai() { return tendetai; }
    public void setTendetai(String tendetai) { this.tendetai = tendetai; }

    public String getTrangthaiDangky() { return trangthaiDangky; }
    public void setTrangthaiDangky(String trangthaiDangky) { this.trangthaiDangky = trangthaiDangky; }

    public List<ThanhVienDto> getDanhSachThanhVien() { return danhSachThanhVien; }
    public void setDanhSachThanhVien(List<ThanhVienDto> danhSachThanhVien) { this.danhSachThanhVien = danhSachThanhVien; }
}
