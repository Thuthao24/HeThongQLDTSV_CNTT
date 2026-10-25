package com.example.quanlydetaisinhvien.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TaoNhomRequest {
    @NotNull(message = "Đợt đăng ký không được để trống")
    private Long dotdangkyid;

    @NotBlank(message = "Tên nhóm không được để trống")
    @Size(max = 100, message = "Tên nhóm tối đa 100 ký tự")
    private String tennhom;

    private String masinhvienNhomtruong;

    public TaoNhomRequest() {}

    public Long getDotdangkyid() { return dotdangkyid; }
    public void setDotdangkyid(Long dotdangkyid) { this.dotdangkyid = dotdangkyid; }

    public String getTennhom() { return tennhom; }
    public void setTennhom(String tennhom) { this.tennhom = tennhom; }

    public String getMasinhvienNhomtruong() { return masinhvienNhomtruong; }
    public void setMasinhvienNhomtruong(String masinhvienNhomtruong) { this.masinhvienNhomtruong = masinhvienNhomtruong; }
}
