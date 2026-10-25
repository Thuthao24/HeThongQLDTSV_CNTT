package com.example.quanlydetaisinhvien.dto.morong;

import com.example.quanlydetaisinhvien.entity.TrangthaiDetai;
import jakarta.validation.constraints.Positive;

public class DetaiFilterDTO extends SearchFilterDTO {
    @Positive private Long bomonid;
    private TrangthaiDetai trangthai;
    @Positive private Long giangviendexuatid;
    @Positive private Long giangvienhuongdanid;

    public Long getBomonid() { return bomonid; }
    public void setBomonid(Long id) { bomonid = id; }
    public TrangthaiDetai getTrangthai() { return trangthai; }
    public void setTrangthai(TrangthaiDetai value) { trangthai = value; }
    public Long getGiangviendexuatid() { return giangviendexuatid; }
    public void setGiangviendexuatid(Long id) { giangviendexuatid = id; }
    public Long getGiangvienhuongdanid() { return giangvienhuongdanid; }
    public void setGiangvienhuongdanid(Long id) { giangvienhuongdanid = id; }
}
