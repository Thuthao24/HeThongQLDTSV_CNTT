package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.Thanhviennhom;
import com.example.quanlydetaisinhvien.entity.ThanhviennhomId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ThanhviennhomRepository extends JpaRepository<Thanhviennhom, ThanhviennhomId> {

    List<Thanhviennhom> findByNhomid(Long nhomid);
    List<Thanhviennhom> findByNhomidIn(java.util.Collection<Long> nhomids);
    List<Thanhviennhom> findBySinhvienid(Long sinhvienid);
    boolean existsBySinhvienid(Long sinhvienid);
    boolean existsByNhomidAndSinhvienid(Long nhomid, Long sinhvienid);
    long countByNhomid(Long nhomid);
    void deleteByNhomidAndSinhvienid(Long nhomid, Long sinhvienid);
    void deleteByNhomid(Long nhomid);

    @Query("SELECT (COUNT(tv) > 0) FROM Thanhviennhom tv, Nhomsinhvien n " +
           "WHERE tv.nhomid = n.id AND tv.sinhvienid = :sinhvienid AND n.dotdangkyid = :dotdangkyid")
    boolean existsSinhvienInDot(@Param("sinhvienid") Long sinhvienid, @Param("dotdangkyid") Long dotdangkyid);

    @Query("SELECT (COUNT(tv) > 0) FROM Thanhviennhom tv, Nhomsinhvien n, Dotdangky d " +
           "WHERE tv.nhomid = n.id AND n.dotdangkyid = d.id AND tv.sinhvienid = :sinhvienid AND d.trangthai <> com.example.quanlydetaisinhvien.entity.TrangthaiDotdangky.DONG")
    boolean existsSinhvienInActiveDot(@Param("sinhvienid") Long sinhvienid);

    @Query("SELECT n FROM Nhomsinhvien n, Thanhviennhom tv " +
           "WHERE tv.nhomid = n.id AND tv.sinhvienid = :sinhvienid AND n.dotdangkyid = :dotdangkyid")
    Optional<com.example.quanlydetaisinhvien.entity.Nhomsinhvien> findNhomBySinhvienAndDot(
            @Param("sinhvienid") Long sinhvienid, @Param("dotdangkyid") Long dotdangkyid);
}
