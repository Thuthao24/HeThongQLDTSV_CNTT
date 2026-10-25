package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.entity.Sinhvien;

public interface SinhvienService extends CrudService<Sinhvien, Long> {
    org.springframework.data.domain.Page<Sinhvien> searchPage(String keyword, org.springframework.data.domain.Pageable pageable);
}
