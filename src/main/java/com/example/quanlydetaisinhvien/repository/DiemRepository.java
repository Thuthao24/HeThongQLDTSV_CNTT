package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.Diem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;

public interface DiemRepository extends JpaRepository<Diem, Long> {

    List<Diem> findByDangkydetaiid(Long dangkydetaiid);
    List<Diem> findByGiangvienid(Long giangvienid);
    org.springframework.data.domain.Page<Diem> findByGiangvienid(Long giangvienid, org.springframework.data.domain.Pageable pageable);
    boolean existsByDangkydetaiidAndGiangvienid(Long dangkydetaiid, Long giangvienid);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select d from Diem d where d.dangkydetaiid = :id")
    List<Diem> findForPublication(@Param("id") Long id);
}
