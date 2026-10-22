package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Thongbao")
public class Thongbao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "Tieude", nullable = false, length = 255)
    private String tieude;

    @Column(name = "Noidung", nullable = false, columnDefinition = "TEXT")
    private String noidung;

    @Column(name = "Nguoidangid", nullable = false)
    private Long nguoidangid;

    @Column(name = "Ngaydang", nullable = false)
    private LocalDateTime ngaydang;

    public Thongbao() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTieude() { return tieude; }
    public void setTieude(String tieude) { this.tieude = tieude; }

    public String getNoidung() { return noidung; }
    public void setNoidung(String noidung) { this.noidung = noidung; }

    public Long getNguoidangid() { return nguoidangid; }
    public void setNguoidangid(Long nguoidangid) { this.nguoidangid = nguoidangid; }

    public LocalDateTime getNgaydang() { return ngaydang; }
    public void setNgaydang(LocalDateTime ngaydang) { this.ngaydang = ngaydang; }

}
