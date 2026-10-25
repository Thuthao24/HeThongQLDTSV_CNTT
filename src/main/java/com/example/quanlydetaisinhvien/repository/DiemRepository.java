package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.Diem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface DiemRepository extends JpaRepository<Diem, Long> {

    List<Diem> findByDangkydetaiid(Long dangkydetaiid);
    List<Diem> findByGiangvienid(Long giangvienid);
    org.springframework.data.domain.Page<Diem> findByGiangvienid(Long giangvienid, org.springframework.data.domain.Pageable pageable);
    boolean existsByDangkydetaiidAndGiangvienid(Long dangkydetaiid, Long giangvienid);
    Optional<Diem> findByDangkydetaiidAndGiangvienid(Long dangkydetaiid, Long giangvienid);

    @Query("SELECT AVG(d.diemso) FROM Diem d WHERE d.dangkydetaiid = :dangkydetaiid")
    BigDecimal tinhDiemTrungBinhTheoDangky(@Param("dangkydetaiid") Long dangkydetaiid);

    @Query("SELECT COUNT(d) FROM Diem d WHERE d.dangkydetaiid = :dangkydetaiid")
    long demSoLuongDiemTheoDangky(@Param("dangkydetaiid") Long dangkydetaiid);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select d from Diem d where d.dangkydetaiid = :id")
    List<Diem> findForPublication(@Param("id") Long id);
}
