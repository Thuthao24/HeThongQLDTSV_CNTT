package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "Diem")
public class Diem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "Dangkydetaiid", nullable = false)
    private Long dangkydetaiid;

    @Column(name = "Giangvienid", nullable = false)
    private Long giangvienid;

    @Column(name = "Diemso", nullable = false, precision = 4, scale = 2)
    private BigDecimal diemso;

    @Column(name = "Nhanxet", length = 500)
    private String nhanxet;

    public Diem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDangkydetaiid() { return dangkydetaiid; }
    public void setDangkydetaiid(Long dangkydetaiid) { this.dangkydetaiid = dangkydetaiid; }

    public Long getGiangvienid() { return giangvienid; }
    public void setGiangvienid(Long giangvienid) { this.giangvienid = giangvienid; }

    public BigDecimal getDiemso() { return diemso; }
    public void setDiemso(BigDecimal diemso) { this.diemso = diemso; }

    public String getNhanxet() { return nhanxet; }
    public void setNhanxet(String nhanxet) { this.nhanxet = nhanxet; }

}
