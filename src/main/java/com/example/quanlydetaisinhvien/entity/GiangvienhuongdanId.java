package com.example.quanlydetaisinhvien.entity;

import java.io.Serializable;
import java.util.Objects;

public class GiangvienhuongdanId implements Serializable {
    private Long detaiid;
    private Long giangvienid;

    public GiangvienhuongdanId() {}

    public Long getDetaiid() { return detaiid; }
    public void setDetaiid(Long detaiid) { this.detaiid = detaiid; }
    public Long getGiangvienid() { return giangvienid; }
    public void setGiangvienid(Long giangvienid) { this.giangvienid = giangvienid; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GiangvienhuongdanId that)) return false;
        return Objects.equals(detaiid, that.detaiid) && Objects.equals(giangvienid, that.giangvienid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(detaiid, giangvienid);
    }
}
