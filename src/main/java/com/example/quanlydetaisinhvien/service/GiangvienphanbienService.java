package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.entity.Giangvienphanbien;
import java.util.List;

public interface GiangvienphanbienService extends CrudService<Giangvienphanbien, Long> {
    Giangvienphanbien phanCong(Long dangkydetaiid, Long giangvienid, String currentUsername);
    void xoaPhanCong(Long dangkydetaiid, Long giangvienid, String currentUsername);
    List<Giangvienphanbien> findByDangkydetaiid(Long dangkydetaiid);
}
