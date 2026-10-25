package com.example.quanlydetaisinhvien.repository;


import com.example.quanlydetaisinhvien.entity.Sinhvien;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SinhvienRepository extends JpaRepository<Sinhvien, Long> {
    Optional<Sinhvien> findByMasinhvien(String masinhvien);
    Optional<Sinhvien> findByTaikhoanid(Long taikhoanid);
    boolean existsByMasinhvien(String masinhvien);

    boolean existsByBomonid(Long bomonid);
    boolean existsByMasinhvienIgnoreCase(String masinhvien);
    @org.springframework.data.jpa.repository.Query("select e from Sinhvien e where lower(coalesce(e.masinhvien, '')) like lower(concat('%', :q, '%')) or lower(coalesce(e.lop, '')) like lower(concat('%', :q, '%'))")
    org.springframework.data.domain.Page<Sinhvien> searchPage(@org.springframework.data.repository.query.Param("q") String q, org.springframework.data.domain.Pageable pageable);
}