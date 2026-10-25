package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.entity.Giangvien;

public interface GiangvienService extends CrudService<Giangvien, Long> {
    org.springframework.data.domain.Page<Giangvien> searchPage(String keyword, org.springframework.data.domain.Pageable pageable);
}
