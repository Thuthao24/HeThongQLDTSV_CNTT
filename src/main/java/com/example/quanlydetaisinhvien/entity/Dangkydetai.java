package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Dangkydetai")
public class Dangkydetai {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "Nhomid", nullable = false, unique = true)
    private Long nhomid;

    @Column(name = "Detaiid", nullable = false)
    private Long detaiid;

    @Enumerated(EnumType.STRING)
    @Column(name = "Trangthai", nullable = false)
    private TrangthaiDangkydetai trangthai;

    @Column(name = "Ngaydangky", nullable = false)
    private LocalDateTime ngaydangky;

    public Dangkydetai() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getNhomid() { return nhomid; }
    public void setNhomid(Long nhomid) { this.nhomid = nhomid; }

    public Long getDetaiid() { return detaiid; }
    public void setDetaiid(Long detaiid) { this.detaiid = detaiid; }

    public TrangthaiDangkydetai getTrangthai() { return trangthai; }
    public void setTrangthai(TrangthaiDangkydetai trangthai) { this.trangthai = trangthai; }

    public LocalDateTime getNgaydangky() { return ngaydangky; }
    public void setNgaydangky(LocalDateTime ngaydangky) { this.ngaydangky = ngaydangky; }

}
