package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Dotdangky")
public class Dotdangky {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "Tendot", nullable = false, length = 200)
    private String tendot;

    @Enumerated(EnumType.STRING)
    @Column(name = "Loaidot", nullable = false)
    private Loaidot loaidot;

    @Column(name = "Batdaugiangvien", nullable = false)
    private LocalDateTime batdaugiangvien;

    @Column(name = "Ketthucgiangvien", nullable = false)
    private LocalDateTime ketthucgiangvien;

    @Column(name = "Batdausinhvien", nullable = false)
    private LocalDateTime batdausinhvien;

    @Column(name = "Ketthucsinhvien", nullable = false)
    private LocalDateTime ketthucsinhvien;

    @Column(name = "Hanchamphanbien")
    private LocalDateTime hanchamphanbien;

    @Column(name = "Ngaybaocaohoidong")
    private LocalDateTime ngaybaocaohoidong;

    @Enumerated(EnumType.STRING)
    @Column(name = "Trangthai", nullable = false)
    private TrangthaiDotdangky trangthai;

    public Dotdangky() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTendot() { return tendot; }
    public void setTendot(String tendot) { this.tendot = tendot; }

    public Loaidot getLoaidot() { return loaidot; }
    public void setLoaidot(Loaidot loaidot) { this.loaidot = loaidot; }

    public LocalDateTime getBatdaugiangvien() { return batdaugiangvien; }
    public void setBatdaugiangvien(LocalDateTime batdaugiangvien) { this.batdaugiangvien = batdaugiangvien; }

    public LocalDateTime getKetthucgiangvien() { return ketthucgiangvien; }
    public void setKetthucgiangvien(LocalDateTime ketthucgiangvien) { this.ketthucgiangvien = ketthucgiangvien; }

    public LocalDateTime getBatdausinhvien() { return batdausinhvien; }
    public void setBatdausinhvien(LocalDateTime batdausinhvien) { this.batdausinhvien = batdausinhvien; }

    public LocalDateTime getKetthucsinhvien() { return ketthucsinhvien; }
    public void setKetthucsinhvien(LocalDateTime ketthucsinhvien) { this.ketthucsinhvien = ketthucsinhvien; }

    public LocalDateTime getHanchamphanbien() { return hanchamphanbien; }
    public void setHanchamphanbien(LocalDateTime hanchamphanbien) { this.hanchamphanbien = hanchamphanbien; }

    public LocalDateTime getNgaybaocaohoidong() { return ngaybaocaohoidong; }
    public void setNgaybaocaohoidong(LocalDateTime ngaybaocaohoidong) { this.ngaybaocaohoidong = ngaybaocaohoidong; }

    public TrangthaiDotdangky getTrangthai() { return trangthai; }
    public void setTrangthai(TrangthaiDotdangky trangthai) { this.trangthai = trangthai; }

}
