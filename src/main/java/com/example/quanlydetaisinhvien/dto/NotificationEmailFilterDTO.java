package com.example.quanlydetaisinhvien.dto;

import com.example.quanlydetaisinhvien.entity.TrangthaiEmailthongbao;
import jakarta.validation.constraints.*;

public class NotificationEmailFilterDTO {
    @Positive private Long thongbaoid;
    private TrangthaiEmailthongbao trangthai;
    @Min(0) @Max(1000000) private int page;
    @Min(1) @Max(100) private int size=20;
    public Long getThongbaoid() { return thongbaoid; }
    public void setThongbaoid(Long value) { thongbaoid=value; }
    public TrangthaiEmailthongbao getTrangthai() { return trangthai; }
    public void setTrangthai(TrangthaiEmailthongbao value) { trangthai=value; }
    public int getPage() { return page; }
    public void setPage(int value) { page=value; }
    public int getSize() { return size; }
    public void setSize(int value) { size=value; }
}
