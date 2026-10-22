package com.example.quanlydetaisinhvien.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Editable fields only; identity, author and creation time belong to the server. */
public class ThongbaoFormDTO {
    @NotBlank(message = "Vui lòng nhập tiêu đề.")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự.")
    private String tieude;
    @NotBlank(message = "Vui lòng nhập nội dung.")
    @Size(max = 10000, message = "Nội dung tối đa 10000 ký tự.")
    private String noidung;

    public String getTieude() { return tieude; }
    public void setTieude(String tieude) { this.tieude = tieude; }
    public String getNoidung() { return noidung; }
    public void setNoidung(String noidung) { this.noidung = noidung; }
}
