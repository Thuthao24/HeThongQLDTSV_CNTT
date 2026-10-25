package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Bomon")
public class Bomon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "Mabomon", nullable = false, unique = true, length = 20)
    private String mabomon;

    @Column(name = "Tenbomon", nullable = false, length = 150)
    private String tenbomon;

    public Bomon() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMabomon() { return mabomon; }
    public void setMabomon(String mabomon) { this.mabomon = mabomon; }

    public String getTenbomon() { return tenbomon; }
    public void setTenbomon(String tenbomon) { this.tenbomon = tenbomon; }

}
