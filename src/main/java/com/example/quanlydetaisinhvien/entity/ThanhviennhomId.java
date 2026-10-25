package com.example.quanlydetaisinhvien.entity;

import java.io.Serializable;
import java.util.Objects;

public class ThanhviennhomId implements Serializable {
    private Long nhomid;
    private Long sinhvienid;

    public ThanhviennhomId() {}

    public Long getNhomid() { return nhomid; }
    public void setNhomid(Long nhomid) { this.nhomid = nhomid; }
    public Long getSinhvienid() { return sinhvienid; }
    public void setSinhvienid(Long sinhvienid) { this.sinhvienid = sinhvienid; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ThanhviennhomId that)) return false;
        return Objects.equals(nhomid, that.nhomid) && Objects.equals(sinhvienid, that.sinhvienid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nhomid, sinhvienid);
    }
}
