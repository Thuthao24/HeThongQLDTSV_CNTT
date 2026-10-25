package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.entity.Dotdangky;

public interface DotdangkyService extends CrudService<Dotdangky, Long> {
    java.util.List<Dotdangky> layDotMoiNhat(int limit);
    java.util.List<Dotdangky> layDotDangKyNhom();
    org.springframework.data.domain.Page<Dotdangky> searchPage(String keyword, org.springframework.data.domain.Pageable pageable);
}
