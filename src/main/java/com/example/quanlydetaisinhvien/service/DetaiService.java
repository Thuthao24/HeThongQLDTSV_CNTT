package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.entity.Detai;

public interface DetaiService extends CrudService<Detai, Long> {
    org.springframework.data.domain.Page<Detai> searchPage(com.example.quanlydetaisinhvien.dto.morong.DetaiFilterDTO filter);
    java.util.List<Detai> layDetaiCongBo();
    java.util.List<Detai> layDetaiCongBoTheoDot(Long dotId);
    org.springframework.data.domain.Page<Detai> searchPage(String keyword, org.springframework.data.domain.Pageable pageable);
}
