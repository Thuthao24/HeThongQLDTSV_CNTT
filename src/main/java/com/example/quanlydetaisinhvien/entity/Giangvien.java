package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Giangvien")
public class Giangvien {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "Taikhoanid", nullable = false, unique = true)
    private Long taikhoanid;

    @Column(name = "Magiangvien", nullable = false, unique = true, length = 20)
    private String magiangvien;

    @Column(name = "Bomonid", nullable = false)
    private Long bomonid;

    public Giangvien() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTaikhoanid() { return taikhoanid; }
    public void setTaikhoanid(Long taikhoanid) { this.taikhoanid = taikhoanid; }

    public String getMagiangvien() { return magiangvien; }
    public void setMagiangvien(String magiangvien) { this.magiangvien = magiangvien; }

    public Long getBomonid() { return bomonid; }
    public void setBomonid(Long bomonid) { this.bomonid = bomonid; }

}
