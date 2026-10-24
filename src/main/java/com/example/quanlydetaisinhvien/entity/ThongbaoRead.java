package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;

/** Sparse per-account read receipt; creating a notice never creates these rows. */
@Entity
@Table(name = "thongbaodadoc", indexes = @Index(name = "Ix_thongbaodadoc_notice", columnList = "Thongbaoid,Taikhoanid"))
public class ThongbaoRead {
    @EmbeddedId private ThongbaoReadId id;
    @Column(name = "Ngaydoc", nullable = false, updatable = false) private LocalDateTime readAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Taikhoanid", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "Fk_thongbaodadoc_account"))
    @OnDelete(action = OnDeleteAction.CASCADE) private Taikhoan account;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Thongbaoid", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "Fk_thongbaodadoc_notice"))
    @OnDelete(action = OnDeleteAction.CASCADE) private Thongbao notice;
    protected ThongbaoRead() {}
    public ThongbaoRead(Long accountId,Long noticeId,LocalDateTime at) { id=new ThongbaoReadId(accountId,noticeId);readAt=at; }
}
