package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Giangvienphanbien")
public class Giangvienphanbien {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "Dangkydetaiid", nullable = false)
    private Long dangkydetaiid;

    @Column(name = "Giangvienid", nullable = false)
    private Long giangvienid;

    public Giangvienphanbien() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDangkydetaiid() { return dangkydetaiid; }
    public void setDangkydetaiid(Long dangkydetaiid) { this.dangkydetaiid = dangkydetaiid; }

    public Long getGiangvienid() { return giangvienid; }
    public void setGiangvienid(Long giangvienid) { this.giangvienid = giangvienid; }

}
