package com.example.quanlydetaisinhvien.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class NopBaoCaoRequest {
    @NotNull(message = "Mã đăng ký đề tài không được để trống")
    private Long dangkydetaiid;

    @NotBlank(message = "Đường dẫn file báo cáo không được để trống")
    private String duongdanfile;

    public NopBaoCaoRequest() {}

    public NopBaoCaoRequest(Long dangkydetaiid, String duongdanfile) {
        this.dangkydetaiid = dangkydetaiid;
        this.duongdanfile = duongdanfile;
    }

    public Long getDangkydetaiid() { return dangkydetaiid; }
    public void setDangkydetaiid(Long dangkydetaiid) { this.dangkydetaiid = dangkydetaiid; }

    public String getDuongdanfile() { return duongdanfile; }
    public void setDuongdanfile(String duongdanfile) { this.duongdanfile = duongdanfile; }
}
