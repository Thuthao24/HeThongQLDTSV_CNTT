package com.example.quanlydetaisinhvien.dto;

import com.example.quanlydetaisinhvien.entity.TrangthaiDangkydetai;
import java.time.LocalDateTime;

public class DangKyDeTaiDto {
    private Long id;
    private Long nhomid;
    private String tennhom;
    private Long dotdangkyid;
    private String tendot;
    private Long nhomtruongid;
    private String tenNhomtruong;
    private String masinhvienNhomtruong;
    private Long detaiid;
    private String tendetai;
    private TrangthaiDangkydetai trangthai;
    private LocalDateTime ngaydangky;

    public DangKyDeTaiDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getNhomid() { return nhomid; }
    public void setNhomid(Long nhomid) { this.nhomid = nhomid; }

    public String getTennhom() { return tennhom; }
    public void setTennhom(String tennhom) { this.tennhom = tennhom; }

    public Long getDotdangkyid() { return dotdangkyid; }
    public void setDotdangkyid(Long dotdangkyid) { this.dotdangkyid = dotdangkyid; }

    public String getTendot() { return tendot; }
    public void setTendot(String tendot) { this.tendot = tendot; }

    public Long getNhomtruongid() { return nhomtruongid; }
    public void setNhomtruongid(Long nhomtruongid) { this.nhomtruongid = nhomtruongid; }

    public String getTenNhomtruong() { return tenNhomtruong; }
    public void setTenNhomtruong(String tenNhomtruong) { this.tenNhomtruong = tenNhomtruong; }

    public String getMasinhvienNhomtruong() { return masinhvienNhomtruong; }
    public void setMasinhvienNhomtruong(String masinhvienNhomtruong) { this.masinhvienNhomtruong = masinhvienNhomtruong; }

    public Long getDetaiid() { return detaiid; }
    public void setDetaiid(Long detaiid) { this.detaiid = detaiid; }

    public String getTendetai() { return tendetai; }
    public void setTendetai(String tendetai) { this.tendetai = tendetai; }

    public TrangthaiDangkydetai getTrangthai() { return trangthai; }
    public void setTrangthai(TrangthaiDangkydetai trangthai) { this.trangthai = trangthai; }

    public LocalDateTime getNgaydangky() { return ngaydangky; }
    public void setNgaydangky(LocalDateTime ngaydangky) { this.ngaydangky = ngaydangky; }

    private LocalDateTime hanNop;
    public LocalDateTime getHanNop() { return hanNop; }
    public void setHanNop(LocalDateTime hanNop) { this.hanNop = hanNop; }
}
