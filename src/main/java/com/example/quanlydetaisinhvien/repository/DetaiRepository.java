package com.example.quanlydetaisinhvien.repository;


import com.example.quanlydetaisinhvien.entity.Detai;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.example.quanlydetaisinhvien.entity.TrangthaiDetai;

public interface DetaiRepository extends JpaRepository<Detai, Long> {

    List<Detai> findByDotdangkyid(Long dotdangkyid);
    List<Detai> findByTrangthai(TrangthaiDetai trangthai);
    List<Detai> findByDotdangkyidAndTrangthai(Long dotdangkyid, TrangthaiDetai trangthai);

    boolean existsByDotdangkyid(Long dotdangkyid);
    boolean existsByDotdangkyidAndTrangthai(Long dotdangkyid, TrangthaiDetai trangthai);
    @org.springframework.data.jpa.repository.Query("select e from Detai e where lower(coalesce(e.tendetai, '')) like lower(concat('%', :q, '%')) or lower(coalesce(e.mota, '')) like lower(concat('%', :q, '%'))")
    org.springframework.data.domain.Page<Detai> searchPage(@org.springframework.data.repository.query.Param("q") String q, org.springframework.data.domain.Pageable pageable);
}