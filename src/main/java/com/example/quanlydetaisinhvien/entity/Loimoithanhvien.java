package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "loimoithanhvien")
public class Loimoithanhvien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @Column(name = "Nhomid", nullable = false)
    private Long nhomid;

    @Column(name = "Sinhvienid", nullable = false)
    private Long sinhvienid;

    @Column(name = "Nguoimoiid", nullable = false)
    private Long nguoimoiid;

    @Enumerated(EnumType.STRING)
    @Column(name = "Trangthai", nullable = false, length = 30)
    private TrangthaiLoimoi trangthai = TrangthaiLoimoi.CHO_XAC_NHAN;

    @Column(name = "Ngaytao", nullable = false)
    private LocalDateTime ngaytao = LocalDateTime.now();

    @Column(name = "Ngayphanhoi")
    private LocalDateTime ngayphanhoi;

    public Loimoithanhvien() {}

    public Loimoithanhvien(Long nhomid, Long sinhvienid, Long nguoimoiid) {
        this.nhomid = nhomid;
        this.sinhvienid = sinhvienid;
        this.nguoimoiid = nguoimoiid;
        this.trangthai = TrangthaiLoimoi.CHO_XAC_NHAN;
        this.ngaytao = LocalDateTime.now();
    }

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

    public Long getSinhvienid() {
        return sinhvienid;
    }

    public void setSinhvienid(Long sinhvienid) {
        this.sinhvienid = sinhvienid;
    }

    public Long getNguoimoiid() {
        return nguoimoiid;
    }

    public void setNguoimoiid(Long nguoimoiid) {
        this.nguoimoiid = nguoimoiid;
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
