package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.Loimoithanhvien;
import com.example.quanlydetaisinhvien.entity.TrangthaiLoimoi;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoimoithanhvienRepository extends JpaRepository<Loimoithanhvien, Long> {

    List<Loimoithanhvien> findBySinhvienidAndTrangthai(Long sinhvienid, TrangthaiLoimoi trangthai);

    List<Loimoithanhvien> findBySinhvienidAndTrangthaiOrderByIdDesc(Long sinhvienid, TrangthaiLoimoi trangthai, Pageable pageable);

    List<Loimoithanhvien> findByNhomidAndTrangthai(Long nhomid, TrangthaiLoimoi trangthai);

    boolean existsByNhomidAndSinhvienidAndTrangthai(Long nhomid, Long sinhvienid, TrangthaiLoimoi trangthai);

    long countBySinhvienidAndTrangthai(Long sinhvienid, TrangthaiLoimoi trangthai);
}
