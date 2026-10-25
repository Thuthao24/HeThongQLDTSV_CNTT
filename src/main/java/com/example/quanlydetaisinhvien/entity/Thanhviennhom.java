package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Thanhviennhom")
@IdClass(ThanhviennhomId.class)
public class Thanhviennhom {
    @Id
    @Column(name = "Nhomid", nullable = false)
    private Long nhomid;

    @Id
    @Column(name = "Sinhvienid", nullable = false)
    private Long sinhvienid;

    public Thanhviennhom() {}

    public Thanhviennhom(Long nhomid, Long sinhvienid) {
        this.nhomid = nhomid;
        this.sinhvienid = sinhvienid;
    }

    public Long getNhomid() { return nhomid; }
    public void setNhomid(Long nhomid) { this.nhomid = nhomid; }

    public Long getSinhvienid() { return sinhvienid; }
    public void setSinhvienid(Long sinhvienid) { this.sinhvienid = sinhvienid; }

}
