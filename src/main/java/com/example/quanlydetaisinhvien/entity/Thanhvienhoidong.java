package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Thanhvienhoidong")
@IdClass(ThanhvienhoidongId.class)
public class Thanhvienhoidong {
    @Id
    @Column(name = "Hoidongid", nullable = false)
    private Long hoidongid;

    @Id
    @Column(name = "Giangvienid", nullable = false)
    private Long giangvienid;

    @Enumerated(EnumType.STRING)
    @Column(name = "Vaitro", nullable = false)
    private VaitroHoidong vaitro;

    public Thanhvienhoidong() {}

    public Long getHoidongid() { return hoidongid; }
    public void setHoidongid(Long hoidongid) { this.hoidongid = hoidongid; }

    public Long getGiangvienid() { return giangvienid; }
    public void setGiangvienid(Long giangvienid) { this.giangvienid = giangvienid; }

    public VaitroHoidong getVaitro() { return vaitro; }
    public void setVaitro(VaitroHoidong vaitro) { this.vaitro = vaitro; }

}
