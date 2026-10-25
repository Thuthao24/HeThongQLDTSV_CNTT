package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Nopbaocao")
public class Nopbaocao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "Dangkydetaiid", nullable = false)
    private Long dangkydetaiid;

    @Column(name = "Sinhvienid", nullable = false)
    private Long sinhvienid;

    @Column(name = "Duongdanfile", nullable = false, length = 500)
    private String duongdanfile;

    @Column(name = "Ngaynop", nullable = false)
    private LocalDateTime ngaynop;

    public Nopbaocao() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDangkydetaiid() { return dangkydetaiid; }
    public void setDangkydetaiid(Long dangkydetaiid) { this.dangkydetaiid = dangkydetaiid; }

    public Long getSinhvienid() { return sinhvienid; }
    public void setSinhvienid(Long sinhvienid) { this.sinhvienid = sinhvienid; }

    public String getDuongdanfile() { return duongdanfile; }
    public void setDuongdanfile(String duongdanfile) { this.duongdanfile = duongdanfile; }

    public LocalDateTime getNgaynop() { return ngaynop; }
    public void setNgaynop(LocalDateTime ngaynop) { this.ngaynop = ngaynop; }

}
