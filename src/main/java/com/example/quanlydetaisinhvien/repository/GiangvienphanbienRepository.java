package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.Giangvienphanbien;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GiangvienphanbienRepository extends JpaRepository<Giangvienphanbien, Long> {
    List<Giangvienphanbien> findByDangkydetaiid(Long id);
}
