package com.example.quanlydetaisinhvien.dto.GiaiDoan1;

import com.example.quanlydetaisinhvien.entity.Detai;

/** Giang vien de xuat; ID nguoi de xuat khong nhan tu form. */
public class DeTaiForm {
    private Long dotdangkyid;
    private Long bomonid;
    private String tendetai;
    private String mota;
    private Long gvhd1;
    private Long gvhd2;
    public static DeTaiForm from(Detai e, Long gvhd1, Long gvhd2){
        DeTaiForm f=new DeTaiForm(); f.dotdangkyid=e.getDotdangkyid(); f.bomonid=e.getBomonid();
        f.tendetai=e.getTendetai(); f.mota=e.getMota(); f.gvhd1=gvhd1; f.gvhd2=gvhd2; return f;
    }
    public Long getDotdangkyid(){return dotdangkyid;} public void setDotdangkyid(Long v){dotdangkyid=v;}
    public Long getBomonid(){return bomonid;} public void setBomonid(Long v){bomonid=v;}
    public String getTendetai(){return tendetai;} public void setTendetai(String v){tendetai=v;}
    public String getMota(){return mota;} public void setMota(String v){mota=v;}
    public Long getGvhd1(){return gvhd1;} public void setGvhd1(Long v){gvhd1=v;}
    public Long getGvhd2(){return gvhd2;} public void setGvhd2(Long v){gvhd2=v;}
}
