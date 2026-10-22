package com.example.quanlydetaisinhvien.dto;

public class ThanhVienDto {
    private Long sinhvienid;
    private String masinhvien;
    private String hoten;
    private String email;
    private String lop;
    private boolean nhomtruong;

    public ThanhVienDto() {}

    public ThanhVienDto(Long sinhvienid, String masinhvien, String hoten, String email, String lop, boolean nhomtruong) {
        this.sinhvienid = sinhvienid;
        this.masinhvien = masinhvien;
        this.hoten = hoten;
        this.email = email;
        this.lop = lop;
        this.nhomtruong = nhomtruong;
    }

    public Long getSinhvienid() { return sinhvienid; }
    public void setSinhvienid(Long sinhvienid) { this.sinhvienid = sinhvienid; }

    public String getMasinhvien() { return masinhvien; }
    public void setMasinhvien(String masinhvien) { this.masinhvien = masinhvien; }

    public String getHoten() { return hoten; }
    public void setHoten(String hoten) { this.hoten = hoten; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLop() { return lop; }
    public void setLop(String lop) { this.lop = lop; }

    public boolean isNhomtruong() { return nhomtruong; }
    public void setNhomtruong(boolean nhomtruong) { this.nhomtruong = nhomtruong; }
}
