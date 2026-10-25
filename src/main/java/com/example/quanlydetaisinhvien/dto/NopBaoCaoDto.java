package com.example.quanlydetaisinhvien.dto;

import java.time.LocalDateTime;

public class NopBaoCaoDto {
    private Long id;
    private Long dangkydetaiid;
    private Long nhomid;
    private String tennhom;
    private Long detaiid;
    private String tendetai;
    private Long sinhvienid;
    private String masinhvien;
    private String hoten;
    private String duongdanfile;
    private LocalDateTime ngaynop;
    private boolean daKhoa;

    public NopBaoCaoDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDangkydetaiid() { return dangkydetaiid; }
    public void setDangkydetaiid(Long dangkydetaiid) { this.dangkydetaiid = dangkydetaiid; }

    public Long getNhomid() { return nhomid; }
    public void setNhomid(Long nhomid) { this.nhomid = nhomid; }

    public String getTennhom() { return tennhom; }
    public void setTennhom(String tennhom) { this.tennhom = tennhom; }

    public Long getDetaiid() { return detaiid; }
    public void setDetaiid(Long detaiid) { this.detaiid = detaiid; }

    public String getTendetai() { return tendetai; }
    public void setTendetai(String tendetai) { this.tendetai = tendetai; }

    public Long getSinhvienid() { return sinhvienid; }
    public void setSinhvienid(Long sinhvienid) { this.sinhvienid = sinhvienid; }

    public String getMasinhvien() { return masinhvien; }
    public void setMasinhvien(String masinhvien) { this.masinhvien = masinhvien; }

    public String getHoten() { return hoten; }
    public void setHoten(String hoten) { this.hoten = hoten; }

    public String getDuongdanfile() { return duongdanfile; }
    public void setDuongdanfile(String duongdanfile) { this.duongdanfile = duongdanfile; }

    public LocalDateTime getNgaynop() { return ngaynop; }
    public void setNgaynop(LocalDateTime ngaynop) { this.ngaynop = ngaynop; }

    public boolean isDaKhoa() { return daKhoa; }
    public void setDaKhoa(boolean daKhoa) { this.daKhoa = daKhoa; }

    private LocalDateTime hanNop;
    public LocalDateTime getHanNop() { return hanNop; }
    public void setHanNop(LocalDateTime hanNop) { this.hanNop = hanNop; }

    public String getTenFileHienThi() {
        if (duongdanfile == null || duongdanfile.isBlank()) return "Tệp báo cáo";
        String path = duongdanfile.trim();
        int lastSlash = path.lastIndexOf('/');
        String name = (lastSlash >= 0 && lastSlash < path.length() - 1) ? path.substring(lastSlash + 1) : path;
        // Nếu file được lưu dạng timestamp_uuid_tenfile thì tách lấy tên file gốc cho gọn
        String[] parts = name.split("_", 3);
        if (parts.length == 3 && parts[0].matches("\\d+")) {
            return parts[2];
        }
        return name;
    }

    public String getDinhDangFile() {
        if (duongdanfile == null) return "FILE";
        String lower = duongdanfile.toLowerCase();
        if (lower.endsWith(".pdf")) return "PDF";
        if (lower.endsWith(".docx") || lower.endsWith(".doc")) return "DOCX";
        if (lower.endsWith(".zip") || lower.endsWith(".rar")) return "ZIP";
        if (lower.startsWith("http://") || lower.startsWith("https://")) return "LINK";
        return "FILE";
    }
}
