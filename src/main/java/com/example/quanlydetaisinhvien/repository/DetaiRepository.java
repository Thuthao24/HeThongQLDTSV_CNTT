package com.example.quanlydetaisinhvien.repository;


import com.example.quanlydetaisinhvien.entity.Detai;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.example.quanlydetaisinhvien.entity.TrangthaiDetai;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface DetaiRepository extends JpaRepository<Detai, Long> {

    List<Detai> findByDotdangkyid(Long dotdangkyid);
    List<Detai> findByTrangthai(TrangthaiDetai trangthai);
    List<Detai> findByDotdangkyidAndTrangthai(Long dotdangkyid, TrangthaiDetai trangthai);

    boolean existsByDotdangkyid(Long dotdangkyid);
    boolean existsByDotdangkyidAndTrangthai(Long dotdangkyid, TrangthaiDetai trangthai);
    @org.springframework.data.jpa.repository.Query("select e from Detai e where lower(coalesce(e.tendetai, '')) like lower(concat('%', :q, '%')) or lower(coalesce(e.mota, '')) like lower(concat('%', :q, '%'))")
    org.springframework.data.domain.Page<Detai> searchPage(@org.springframework.data.repository.query.Param("q") String q, org.springframework.data.domain.Pageable pageable);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Detai d where d.id = :id")
    Optional<Detai> findByIdForUpdate(@Param("id") Long id);

}
