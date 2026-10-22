package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Hoidong")
public class Hoidong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "Dotdangkyid", nullable = false)
    private Long dotdangkyid;

    @Column(name = "Tenhoidong", nullable = false, length = 150)
    private String tenhoidong;

    @Column(name = "Ngaybaocao")
    private LocalDateTime ngaybaocao;

    @Column(name = "Phong", length = 50)
    private String phong;

    public Hoidong() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDotdangkyid() { return dotdangkyid; }
    public void setDotdangkyid(Long dotdangkyid) { this.dotdangkyid = dotdangkyid; }

    public String getTenhoidong() { return tenhoidong; }
    public void setTenhoidong(String tenhoidong) { this.tenhoidong = tenhoidong; }

    public LocalDateTime getNgaybaocao() { return ngaybaocao; }
    public void setNgaybaocao(LocalDateTime ngaybaocao) { this.ngaybaocao = ngaybaocao; }

    public String getPhong() { return phong; }
    public void setPhong(String phong) { this.phong = phong; }

}
