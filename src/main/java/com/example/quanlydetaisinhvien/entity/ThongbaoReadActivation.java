package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** One durable activation boundary, inserted by V005, never initialized from JVM startup. */
@Entity
@Table(name = "Thongbaodocmoc")
public class ThongbaoReadActivation {
    @Id @Column(name = "Id") private Integer id;
    @Column(name = "Thongbaoidmoc", nullable = false, updatable = false) private Long noticeBoundary;
    @Column(name = "Ngaykichhoat", nullable = false, updatable = false) private LocalDateTime activatedAt;
    protected ThongbaoReadActivation() {}
    public ThongbaoReadActivation(long boundary, LocalDateTime at) { id=1;noticeBoundary=boundary;activatedAt=at; }
    public Long getNoticeBoundary() { return noticeBoundary; }
}
