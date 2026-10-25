package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Giangvienhuongdan")
@IdClass(GiangvienhuongdanId.class)
public class Giangvienhuongdan {
    @Id
    @Column(name = "Detaiid", nullable = false)
    private Long detaiid;

    @Id
    @Column(name = "Giangvienid", nullable = false)
    private Long giangvienid;

    public Giangvienhuongdan() {}

    public Long getDetaiid() { return detaiid; }
    public void setDetaiid(Long detaiid) { this.detaiid = detaiid; }

    public Long getGiangvienid() { return giangvienid; }
    public void setGiangvienid(Long giangvienid) { this.giangvienid = giangvienid; }

}
