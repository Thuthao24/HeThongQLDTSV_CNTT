package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.Bomon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BomonRepository extends JpaRepository<Bomon, Long> {
    boolean existsByMabomonIgnoreCaseAndIdNot(String mabomon, Long id);
    boolean existsByMabomonIgnoreCase(String mabomon);
    @org.springframework.data.jpa.repository.Query("select e from Bomon e where lower(coalesce(e.mabomon, '')) like lower(concat('%', :q, '%')) or lower(coalesce(e.tenbomon, '')) like lower(concat('%', :q, '%'))")
    org.springframework.data.domain.Page<Bomon> searchPage(@org.springframework.data.repository.query.Param("q") String q, org.springframework.data.domain.Pageable pageable);
}
