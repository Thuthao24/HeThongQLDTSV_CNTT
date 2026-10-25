package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.Giangvienphanbien;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GiangvienphanbienRepository extends JpaRepository<Giangvienphanbien, Long> {
    List<Giangvienphanbien> findByDangkydetaiid(Long id);
    boolean existsByDangkydetaiidAndGiangvienid(Long dangkydetaiid, Long giangvienid);
    Optional<Giangvienphanbien> findByDangkydetaiidAndGiangvienid(Long dangkydetaiid, Long giangvienid);
    void deleteByDangkydetaiidAndGiangvienid(Long dangkydetaiid, Long giangvienid);
}
