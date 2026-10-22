package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Sinhvien")
public class Sinhvien {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "Taikhoanid", nullable = false, unique = true)
    private Long taikhoanid;

    @Column(name = "Masinhvien", nullable = false, unique = true, length = 20)
    private String masinhvien;

    @Column(name = "Bomonid", nullable = false)
    private Long bomonid;

    @Column(name = "Lop", length = 50)
    private String lop;

    public Sinhvien() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTaikhoanid() { return taikhoanid; }
    public void setTaikhoanid(Long taikhoanid) { this.taikhoanid = taikhoanid; }

    public String getMasinhvien() { return masinhvien; }
    public void setMasinhvien(String masinhvien) { this.masinhvien = masinhvien; }

    public Long getBomonid() { return bomonid; }
    public void setBomonid(Long bomonid) { this.bomonid = bomonid; }

    public String getLop() { return lop; }
    public void setLop(String lop) { this.lop = lop; }

}
