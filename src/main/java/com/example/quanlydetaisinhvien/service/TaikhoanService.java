package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.entity.Taikhoan;

public interface TaikhoanService extends CrudService<Taikhoan, Long> {
    java.util.Optional<Taikhoan> layTheoTenDangNhap(String username);
    org.springframework.data.domain.Page<Taikhoan> searchPage(String keyword, org.springframework.data.domain.Pageable pageable);
}
