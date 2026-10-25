package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.entity.Bomon;

public interface BomonService extends CrudService<Bomon, Long> {
    org.springframework.data.domain.Page<Bomon> searchPage(String keyword, org.springframework.data.domain.Pageable pageable);
}
