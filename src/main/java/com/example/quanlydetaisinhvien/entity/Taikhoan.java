package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "taikhoan")
public class Taikhoan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "Tendangnhap", nullable = false, unique = true, length = 50)
    private String tendangnhap;

    @Column(name = "Matkhau", nullable = false, length = 100)
    private String matkhau;

    @Column(name = "Hoten", nullable = false, length = 150)
    private String hoten;

    @Column(name = "Email", unique = true, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "Vaitro", nullable = false)
    private Vaitro vaitro;

    @Column(name = "Kichhoat", nullable = false)
    private Boolean kichhoat;

    public Taikhoan() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTendangnhap() { return tendangnhap; }
    public void setTendangnhap(String tendangnhap) { this.tendangnhap = tendangnhap; }

    public String getMatkhau() { return matkhau; }
    public void setMatkhau(String matkhau) { this.matkhau = matkhau; }

    public String getHoten() { return hoten; }
    public void setHoten(String hoten) { this.hoten = hoten; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Vaitro getVaitro() { return vaitro; }
    public void setVaitro(Vaitro vaitro) { this.vaitro = vaitro; }

    public Boolean getKichhoat() { return kichhoat; }
    public void setKichhoat(Boolean kichhoat) { this.kichhoat = kichhoat; }

}
