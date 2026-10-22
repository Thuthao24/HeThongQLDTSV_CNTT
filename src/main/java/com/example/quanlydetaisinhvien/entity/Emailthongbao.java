package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;

/** Private delivery journal. Never exposed as a public notice DTO. */
@Entity
@Table(name = "Emailthongbao", uniqueConstraints = @UniqueConstraint(name = "Uk_emailthongbao_recipient",
        columnNames = {"Thongbaosnapshotid", "Sinhvienid"}), indexes = {
        @Index(name = "Ix_emailthongbao_pending", columnList = "Trangthai,Id"),
        @Index(name = "Ix_emailthongbao_lease", columnList = "Trangthai,Lanthu")})
public class Emailthongbao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "Thongbaoid") private Long thongbaoid;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Thongbaoid", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "Fk_emailthongbao_notice"))
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Thongbao notice;
    @Column(name = "Thongbaosnapshotid", nullable = false) private Long thongbaoSnapshotId;
    // Historical recipient ID, not a live association. Retain audit without blocking student deletion.
    @Column(name = "Sinhvienid", nullable = false, updatable = false) private Long sinhvienid;
    @Column(name = "Nguoinhan", nullable = false, length = 200) private String nguoinhan;
    @Column(name = "Email", length = 150) private String email;
    @Column(name = "Tieude", nullable = false, length = 320) private String tieude;
    @Column(name = "Noidung", nullable = false, columnDefinition = "TEXT") private String noidung;
    @Enumerated(EnumType.STRING) @Column(name = "Trangthai", nullable = false) private TrangthaiEmailthongbao trangthai;
    @Column(name = "Solanthu", nullable = false) private int solanthu;
    @Column(name = "Ngaytao", nullable = false) private LocalDateTime ngaytao;
    @Column(name = "Lanthu") private LocalDateTime lanthu;
    @Column(name = "Ngaygui") private LocalDateTime ngaygui;
    @Column(name = "Loi", length = 500) private String loi;
    public Long getId() { return id; }
    public Long getThongbaoid() { return thongbaoid; }
    public void setThongbaoid(Long value) { thongbaoid = value; }
    public Long getThongbaoSnapshotId() { return thongbaoSnapshotId; }
    public void setThongbaoSnapshotId(Long value) { thongbaoSnapshotId = value; }
    public Long getSinhvienid() { return sinhvienid; }
    public void setSinhvienid(Long value) { sinhvienid = value; }
    public String getNguoinhan() { return nguoinhan; }
    public void setNguoinhan(String value) { nguoinhan = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getTieude() { return tieude; }
    public void setTieude(String value) { tieude = value; }
    public String getNoidung() { return noidung; }
    public void setNoidung(String value) { noidung = value; }
    public TrangthaiEmailthongbao getTrangthai() { return trangthai; }
    public void setTrangthai(TrangthaiEmailthongbao value) { trangthai = value; }
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
