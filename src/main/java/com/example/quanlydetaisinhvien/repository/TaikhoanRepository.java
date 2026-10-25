package com.example.quanlydetaisinhvien.repository;


import com.example.quanlydetaisinhvien.entity.Taikhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TaikhoanRepository extends JpaRepository<Taikhoan, Long> {

    Optional<Taikhoan> findByTendangnhap(String tendangnhap);

    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    @org.springframework.data.jpa.repository.Query("select e from Taikhoan e where lower(coalesce(e.tendangnhap, '')) like lower(concat('%', :q, '%')) or lower(coalesce(e.hoten, '')) like lower(concat('%', :q, '%')) or lower(coalesce(e.email, '')) like lower(concat('%', :q, '%'))")
    org.springframework.data.domain.Page<Taikhoan> searchPage(@org.springframework.data.repository.query.Param("q") String q, org.springframework.data.domain.Pageable pageable);
    boolean existsByTendangnhapIgnoreCase(String tendangnhap);

    boolean existsByTendangnhapIgnoreCaseAndIdNot(
            String tendangnhap, Long id);
}