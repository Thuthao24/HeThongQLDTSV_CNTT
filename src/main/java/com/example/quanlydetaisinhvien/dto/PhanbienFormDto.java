package com.example.quanlydetaisinhvien.dto;

public class PhanbienFormDto {
    private Long id;
    private Long dangkydetaiid;
    private Long giangvienid;

    public PhanbienFormDto() {}

    public PhanbienFormDto(Long dangkydetaiid, Long giangvienid) {
        this.dangkydetaiid = dangkydetaiid;
        this.giangvienid = giangvienid;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDangkydetaiid() {
        return dangkydetaiid;
    }

    public void setDangkydetaiid(Long dangkydetaiid) {
        this.dangkydetaiid = dangkydetaiid;
    }

    public Long getGiangvienid() {
        return giangvienid;
    }

    public void setGiangvienid(Long giangvienid) {
        this.giangvienid = giangvienid;
    }
}
