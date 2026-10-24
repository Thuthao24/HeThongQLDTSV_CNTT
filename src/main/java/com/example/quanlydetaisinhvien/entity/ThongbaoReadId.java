package com.example.quanlydetaisinhvien.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ThongbaoReadId implements Serializable {
    @Column(name = "Taikhoanid", nullable = false) private Long accountId;
    @Column(name = "Thongbaoid", nullable = false) private Long noticeId;
    protected ThongbaoReadId() {}
    public ThongbaoReadId(Long accountId, Long noticeId) { this.accountId=accountId;this.noticeId=noticeId; }
    @Override public boolean equals(Object other) {
        return other instanceof ThongbaoReadId that && Objects.equals(accountId,that.accountId) && Objects.equals(noticeId,that.noticeId);
    }
    @Override public int hashCode() { return Objects.hash(accountId,noticeId); }
}
