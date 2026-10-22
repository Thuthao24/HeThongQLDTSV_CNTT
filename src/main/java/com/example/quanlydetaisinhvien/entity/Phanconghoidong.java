package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Phanconghoidong", check = @CheckConstraint(name = "Chk_pchd_hinhthuc",
        constraint = "(Hinhthuc = 'COHOIDONG' and Hoidongid is not null) or "
        + "(Hinhthuc = 'KHONGHOIDONG' and Hoidongid is null)"))
public class Phanconghoidong {
    @Id
    @Column(name = "Dangkydetaiid", nullable = false)
    private Long dangkydetaiid;

    @Enumerated(EnumType.STRING)
    @Column(name = "Hinhthuc", nullable = false)
    private HinhthucDanhgia hinhthuc;

    @Column(name = "Hoidongid")
    private Long hoidongid;

    public Long getDangkydetaiid() { return dangkydetaiid; }
    public void setDangkydetaiid(Long id) { this.dangkydetaiid = id; }
    public HinhthucDanhgia getHinhthuc() { return hinhthuc; }
    public void setHinhthuc(HinhthucDanhgia hinhthuc) { this.hinhthuc = hinhthuc; }
    public Long getHoidongid() { return hoidongid; }
    public void setHoidongid(Long id) { this.hoidongid = id; }
}
