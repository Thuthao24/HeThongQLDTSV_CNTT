package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Emailketqua", uniqueConstraints = @UniqueConstraint(
        name = "Uk_emailketqua_recipient", columnNames = {"Ketquaid", "Sinhvienid"}),
        indexes = {@Index(name = "Ix_emailketqua_pending", columnList = "Trangthai,Id"),
                @Index(name = "Ix_emailketqua_lease", columnList = "Trangthai,Lanthu")})
public class Emailketqua {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;
    @Column(name = "Ketquaid", nullable = false) private Long ketquaid;
    @Column(name = "Sinhvienid", nullable = false) private Long sinhvienid;
    @Column(name = "Email", length = 150) private String email;
    @Column(name = "Tieude", nullable = false, length = 200) private String tieude;
    @Column(name = "Noidung", nullable = false, columnDefinition = "TEXT") private String noidung;
    @Enumerated(EnumType.STRING)
    @Column(name = "Trangthai", nullable = false) private TrangthaiEmailketqua trangthai;
    @Column(name = "Solanthu", nullable = false) private int solanthu;
    @Column(name = "Ngaytao", nullable = false) private LocalDateTime ngaytao;
    @Column(name = "Lanthu") private LocalDateTime lanthu;
    @Column(name = "Ngaygui") private LocalDateTime ngaygui;
    @Column(name = "Loi", length = 500) private String loi;

    public Long getId() { return id; }
    public Long getKetquaid() { return ketquaid; }
    public void setKetquaid(Long id) { ketquaid = id; }
    public Long getSinhvienid() { return sinhvienid; }
    public void setSinhvienid(Long id) { sinhvienid = id; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getTieude() { return tieude; }
    public void setTieude(String value) { tieude = value; }
    public String getNoidung() { return noidung; }
    public void setNoidung(String value) { noidung = value; }
    public TrangthaiEmailketqua getTrangthai() { return trangthai; }
    public void setTrangthai(TrangthaiEmailketqua value) { trangthai = value; }
    public int getSolanthu() { return solanthu; }
    public void setSolanthu(int value) { solanthu = value; }
    public LocalDateTime getNgaytao() { return ngaytao; }
    public void setNgaytao(LocalDateTime value) { ngaytao = value; }
    public LocalDateTime getLanthu() { return lanthu; }
    public void setLanthu(LocalDateTime value) { lanthu = value; }
    public LocalDateTime getNgaygui() { return ngaygui; }
    public void setNgaygui(LocalDateTime value) { ngaygui = value; }
    public String getLoi() { return loi; }
    public void setLoi(String value) { loi = value; }
}
