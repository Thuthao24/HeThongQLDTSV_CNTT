package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Ketqua")
public class Ketqua {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "Dangkydetaiid", nullable = false, unique = true)
    private Long dangkydetaiid;

    @Column(name = "Diemcuoi", precision = 4, scale = 2)
    private BigDecimal diemcuoi;

    @Column(name = "Dacongbo", nullable = false)
    private Boolean dacongbo;

    @Column(name = "Ngaycongbo")
    private LocalDateTime ngaycongbo;

    public Ketqua() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDangkydetaiid() { return dangkydetaiid; }
    public void setDangkydetaiid(Long dangkydetaiid) { this.dangkydetaiid = dangkydetaiid; }

    public BigDecimal getDiemcuoi() { return diemcuoi; }
    public void setDiemcuoi(BigDecimal diemcuoi) { this.diemcuoi = diemcuoi; }

    public Boolean getDacongbo() { return dacongbo; }
    public void setDacongbo(Boolean dacongbo) { this.dacongbo = dacongbo; }

    public LocalDateTime getNgaycongbo() { return ngaycongbo; }
    public void setNgaycongbo(LocalDateTime ngaycongbo) { this.ngaycongbo = ngaycongbo; }

}
