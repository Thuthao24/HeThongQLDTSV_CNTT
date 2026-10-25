package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.Giangvien;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GiangvienRepository extends JpaRepository<Giangvien, Long> {
    Optional<Giangvien> findByTaikhoanid(Long taikhoanid);
    boolean existsByBomonid(Long bomonid);
    boolean existsByMagiangvienIgnoreCase(String magiangvien);
    @org.springframework.data.jpa.repository.Query("select e from Giangvien e where lower(coalesce(e.magiangvien, '')) like lower(concat('%', :q, '%'))")
    org.springframework.data.domain.Page<Giangvien> searchPage(@org.springframework.data.repository.query.Param("q") String q, org.springframework.data.domain.Pageable pageable);
}
