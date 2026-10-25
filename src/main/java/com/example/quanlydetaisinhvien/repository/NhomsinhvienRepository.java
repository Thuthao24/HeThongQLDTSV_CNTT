package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.Nhomsinhvien;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface NhomsinhvienRepository extends JpaRepository<Nhomsinhvien, Long> {

    List<Nhomsinhvien> findByDotdangkyid(Long dotdangkyid);
    boolean existsByDotdangkyidAndTennhom(Long dotdangkyid, String tennhom);
    Optional<Nhomsinhvien> findByDotdangkyidAndNhomtruongid(Long dotdangkyid, Long nhomtruongid);
    List<Nhomsinhvien> findByNhomtruongid(Long nhomtruongid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Nhomsinhvien n where n.id = :id")
    Optional<Nhomsinhvien> findByIdForUpdate(@Param("id") Long id);
}
