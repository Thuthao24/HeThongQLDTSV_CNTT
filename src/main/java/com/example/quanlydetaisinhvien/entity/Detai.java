package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Detai")
public class Detai {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "Dotdangkyid", nullable = false)
    private Long dotdangkyid;

    @Column(name = "Bomonid", nullable = false)
    private Long bomonid;

    @Column(name = "Tendetai", nullable = false, length = 255)
    private String tendetai;

    @Column(name = "Mota")
    private String mota;

    @Column(name = "Giangviendexuatid", nullable = false)
    private Long giangviendexuatid;

    @Enumerated(EnumType.STRING)
    @Column(name = "Trangthai", nullable = false)
    private TrangthaiDetai trangthai;

    public Detai() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDotdangkyid() { return dotdangkyid; }
    public void setDotdangkyid(Long dotdangkyid) { this.dotdangkyid = dotdangkyid; }

    public Long getBomonid() { return bomonid; }
    public void setBomonid(Long bomonid) { this.bomonid = bomonid; }

    public String getTendetai() { return tendetai; }
    public void setTendetai(String tendetai) { this.tendetai = tendetai; }

    public String getMota() { return mota; }
    public void setMota(String mota) { this.mota = mota; }

    public Long getGiangviendexuatid() { return giangviendexuatid; }
    public void setGiangviendexuatid(Long giangviendexuatid) { this.giangviendexuatid = giangviendexuatid; }

    public TrangthaiDetai getTrangthai() { return trangthai; }
    public void setTrangthai(TrangthaiDetai trangthai) { this.trangthai = trangthai; }

}
