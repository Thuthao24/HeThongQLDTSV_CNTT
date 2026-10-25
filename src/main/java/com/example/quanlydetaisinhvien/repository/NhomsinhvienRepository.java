package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.Nhomsinhvien;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NhomsinhvienRepository extends JpaRepository<Nhomsinhvien, Long> {

    List<Nhomsinhvien> findByDotdangkyid(Long dotdangkyid);
    boolean existsByDotdangkyidAndTennhom(Long dotdangkyid, String tennhom);
    Optional<Nhomsinhvien> findByDotdangkyidAndNhomtruongid(Long dotdangkyid, Long nhomtruongid);
    List<Nhomsinhvien> findByNhomtruongid(Long nhomtruongid);
}
