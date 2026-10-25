package com.example.quanlydetaisinhvien.dto;

import jakarta.validation.constraints.NotBlank;
import com.example.quanlydetaisinhvien.entity.DoituongThongbao;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/** Editable fields only; identity, author and creation time belong to the server. */
public class ThongbaoFormDTO {
    @NotBlank(message = "Vui lòng nhập tiêu đề.")
    @Pattern(regexp = "(?s).*[^\\s\\p{Z}\\p{Cf}\\p{Cc}].*", message = "Tiêu đề phải có ký tự hiển thị.")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự.")
    private String tieude;
    @NotBlank(message = "Vui lòng nhập nội dung.")
    @Pattern(regexp = "(?s).*[^\\s\\p{Z}\\p{Cf}\\p{Cc}].*", message = "Nội dung phải có ký tự hiển thị.")
    @Size(max = 10000, message = "Nội dung tối đa 10000 ký tự.")
    private String noidung;
    private String revision;
    @jakarta.validation.constraints.NotEmpty(message = "Vui lòng chọn ít nhất một đối tượng nhận thông báo.")
    @Size(max = 3, message = "Đối tượng nhận không hợp lệ.")
    private java.util.Set<@jakarta.validation.constraints.NotNull DoituongThongbao> recipientRoles = new java.util.LinkedHashSet<>();

    public java.util.Set<com.example.quanlydetaisinhvien.entity.DoituongThongbao> getRecipientRoles() { return recipientRoles; }
    public void setRecipientRoles(java.util.Set<com.example.quanlydetaisinhvien.entity.DoituongThongbao> value) { recipientRoles=value; }

    public String getRevision() { return revision; }
    public void setRevision(String revision) { this.revision=revision; }

    public String getTieude() { return tieude; }
    public void setTieude(String tieude) { this.tieude = tieude; }
    public String getNoidung() { return noidung; }
    public void setNoidung(String noidung) { this.noidung = noidung; }
}
