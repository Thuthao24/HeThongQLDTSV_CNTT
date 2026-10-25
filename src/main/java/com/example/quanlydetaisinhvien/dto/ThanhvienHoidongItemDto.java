package com.example.quanlydetaisinhvien.dto;

import com.example.quanlydetaisinhvien.entity.VaitroHoidong;

public class ThanhvienHoidongItemDto {
    private Long giangvienid;
    private VaitroHoidong vaitro;

    public ThanhvienHoidongItemDto() {}

    public ThanhvienHoidongItemDto(Long giangvienid, VaitroHoidong vaitro) {
        this.giangvienid = giangvienid;
        this.vaitro = vaitro;
    }

    public Long getGiangvienid() {
        return giangvienid;
    }

    public void setGiangvienid(Long giangvienid) {
        this.giangvienid = giangvienid;
    }

    public VaitroHoidong getVaitro() {
        return vaitro;
    }

    public void setVaitro(VaitroHoidong vaitro) {
        this.vaitro = vaitro;
    }
}
