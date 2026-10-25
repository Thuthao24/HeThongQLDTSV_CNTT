package com.example.quanlydetaisinhvien.dto.morong;

import jakarta.validation.constraints.Positive;

public class ThongkeFilterDTO {
    @Positive(message = "ID đợt đăng ký phải là số nguyên dương.")
    private Long dotdangkyid;

    public Long getDotdangkyid() { return dotdangkyid; }
    public void setDotdangkyid(Long dotdangkyid) { this.dotdangkyid = dotdangkyid; }
}
