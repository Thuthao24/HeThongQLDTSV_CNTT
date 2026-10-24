package com.example.quanlydetaisinhvien.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HoidongFormDto {
    private Long id;
    private Long dotdangkyid;
    private String tenhoidong;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime ngaybaocao;

    private String phong;
    private List<ThanhvienHoidongItemDto> thanhviens = new ArrayList<>();

    public HoidongFormDto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDotdangkyid() {
        return dotdangkyid;
    }

    public void setDotdangkyid(Long dotdangkyid) {
        this.dotdangkyid = dotdangkyid;
    }

    public String getTenhoidong() {
        return tenhoidong;
    }

    public void setTenhoidong(String tenhoidong) {
        this.tenhoidong = tenhoidong;
    }

    public LocalDateTime getNgaybaocao() {
        return ngaybaocao;
    }

    public void setNgaybaocao(LocalDateTime ngaybaocao) {
        this.ngaybaocao = ngaybaocao;
    }

    public String getPhong() {
        return phong;
    }

    public void setPhong(String phong) {
        this.phong = phong;
    }

    public List<ThanhvienHoidongItemDto> getThanhviens() {
        return thanhviens;
    }

    public void setThanhviens(List<ThanhvienHoidongItemDto> thanhviens) {
        this.thanhviens = thanhviens;
    }
}
