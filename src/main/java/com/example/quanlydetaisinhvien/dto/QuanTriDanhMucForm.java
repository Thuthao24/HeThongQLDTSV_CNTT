package com.example.quanlydetaisinhvien.dto;
import com.example.quanlydetaisinhvien.entity.Vaitro;
/** Form cho CRUD GV/SV; password chỉ bắt buộc khi tạo tài khoản. */
public class QuanTriDanhMucForm {
    private String ma, hoten, email, matkhau, lop;
    private Long bomonid;
    private Vaitro vaitro;
    private boolean kichhoat=true;
    public String getMa(){return ma;}public void setMa(String v){ma=v;}
    public String getHoten(){return hoten;}public void setHoten(String v){hoten=v;}
    public String getEmail(){return email;}public void setEmail(String v){email=v;}
    public String getMatkhau(){return matkhau;}public void setMatkhau(String v){matkhau=v;}
    public String getLop(){return lop;}public void setLop(String v){lop=v;}
    public Long getBomonid(){return bomonid;}public void setBomonid(Long v){bomonid=v;}
    public Vaitro getVaitro(){return vaitro;}public void setVaitro(Vaitro v){vaitro=v;}
    public boolean isKichhoat(){return kichhoat;}public void setKichhoat(boolean v){kichhoat=v;}
}
