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

    // An empty collection denotes an unchanged historical notice: visible to all three recipient roles.
    @ElementCollection
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    @CollectionTable(name = "Thongbaodoituong", joinColumns = @JoinColumn(name = "Thongbaoid"),
            uniqueConstraints = @UniqueConstraint(name = "Uk_thongbaodoituong_role", columnNames = {"Thongbaoid", "Vaitro"}),
            indexes = @Index(name = "Ix_thongbaodoituong_role", columnList = "Vaitro,Thongbaoid"))
    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "Vaitro", nullable = false, length = 20)
    private java.util.Set<DoituongThongbao> recipientRoles = new java.util.HashSet<>();

    public java.util.Set<DoituongThongbao> getRecipientRoles() { return recipientRoles; }
    public void setRecipientRoles(java.util.Set<DoituongThongbao> roles) {
        recipientRoles.clear(); recipientRoles.addAll(roles);
    }

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
