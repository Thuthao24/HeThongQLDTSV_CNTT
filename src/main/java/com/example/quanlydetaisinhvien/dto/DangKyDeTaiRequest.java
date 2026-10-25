package com.example.quanlydetaisinhvien.dto;

import jakarta.validation.constraints.NotNull;

public class DangKyDeTaiRequest {
    @NotNull(message = "Nhóm không được để trống")
    private Long nhomid;

    @NotNull(message = "Đề tài không được để trống")
    private Long detaiid;

    public DangKyDeTaiRequest() {}

    public DangKyDeTaiRequest(Long nhomid, Long detaiid) {
        this.nhomid = nhomid;
        this.detaiid = detaiid;
    }

    public Long getNhomid() { return nhomid; }
    public void setNhomid(Long nhomid) { this.nhomid = nhomid; }

    public Long getDetaiid() { return detaiid; }
    public void setDetaiid(Long detaiid) { this.detaiid = detaiid; }
}
