package com.example.quanlydetaisinhvien.entity;

import java.io.Serializable;
import java.util.Objects;

public class ThanhvienhoidongId implements Serializable {
    public ThanhvienhoidongId(Long hoidongid, Long giangvienid) {
        this.hoidongid = hoidongid;
        this.giangvienid = giangvienid;
    }

    private Long hoidongid;
    private Long giangvienid;

    public ThanhvienhoidongId() {}

    public Long getHoidongid() { return hoidongid; }
    public void setHoidongid(Long hoidongid) { this.hoidongid = hoidongid; }
    public Long getGiangvienid() { return giangvienid; }
    public void setGiangvienid(Long giangvienid) { this.giangvienid = giangvienid; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ThanhvienhoidongId that)) return false;
        return Objects.equals(hoidongid, that.hoidongid) && Objects.equals(giangvienid, that.giangvienid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hoidongid, giangvienid);
    }
}
