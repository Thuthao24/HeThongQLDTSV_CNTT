package com.example.quanlydetaisinhvien.dto.GiaiDoan1;

import com.example.quanlydetaisinhvien.entity.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

/** Nhap lieu dot dang ky; trang thai luon do server quan ly. */
public class DotForm {
    private String tendot;
    private Loaidot loaidot;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime batdaugiangvien;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime ketthucgiangvien;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime batdausinhvien;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime ketthucsinhvien;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime hanchamphanbien;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime ngaybaocaohoidong;

    public static DotForm from(Dotdangky dot) {
        DotForm f = new DotForm();
        f.tendot=dot.getTendot(); f.loaidot=dot.getLoaidot();
        f.batdaugiangvien=dot.getBatdaugiangvien(); f.ketthucgiangvien=dot.getKetthucgiangvien();
        f.batdausinhvien=dot.getBatdausinhvien(); f.ketthucsinhvien=dot.getKetthucsinhvien();
        f.hanchamphanbien=dot.getHanchamphanbien(); f.ngaybaocaohoidong=dot.getNgaybaocaohoidong();
        return f;
    }
    public String getTendot(){return tendot;} public void setTendot(String v){tendot=v;}
    public Loaidot getLoaidot(){return loaidot;} public void setLoaidot(Loaidot v){loaidot=v;}
    public LocalDateTime getBatdaugiangvien(){return batdaugiangvien;} public void setBatdaugiangvien(LocalDateTime v){batdaugiangvien=v;}
    public LocalDateTime getKetthucgiangvien(){return ketthucgiangvien;} public void setKetthucgiangvien(LocalDateTime v){ketthucgiangvien=v;}
    public LocalDateTime getBatdausinhvien(){return batdausinhvien;} public void setBatdausinhvien(LocalDateTime v){batdausinhvien=v;}
    public LocalDateTime getKetthucsinhvien(){return ketthucsinhvien;} public void setKetthucsinhvien(LocalDateTime v){ketthucsinhvien=v;}
    public LocalDateTime getHanchamphanbien(){return hanchamphanbien;} public void setHanchamphanbien(LocalDateTime v){hanchamphanbien=v;}
    public LocalDateTime getNgaybaocaohoidong(){return ngaybaocaohoidong;} public void setNgaybaocaohoidong(LocalDateTime v){ngaybaocaohoidong=v;}
}
