package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Nhomsinhvien")
public class Nhomsinhvien {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "Dotdangkyid", nullable = false)
    private Long dotdangkyid;

    @Column(name = "Tennhom", nullable = false, length = 100)
    private String tennhom;

    @Column(name = "Nhomtruongid", nullable = false)
    private Long nhomtruongid;

    public Nhomsinhvien() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDotdangkyid() { return dotdangkyid; }
    public void setDotdangkyid(Long dotdangkyid) { this.dotdangkyid = dotdangkyid; }

    public String getTennhom() { return tennhom; }
    public void setTennhom(String tennhom) { this.tennhom = tennhom; }

    public Long getNhomtruongid() { return nhomtruongid; }
    public void setNhomtruongid(Long nhomtruongid) { this.nhomtruongid = nhomtruongid; }

}
