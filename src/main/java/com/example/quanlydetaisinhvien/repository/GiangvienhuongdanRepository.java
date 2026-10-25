package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.Giangvienhuongdan;
import com.example.quanlydetaisinhvien.entity.GiangvienhuongdanId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GiangvienhuongdanRepository extends JpaRepository<Giangvienhuongdan, GiangvienhuongdanId> {
    List<Giangvienhuongdan> findByDetaiid(Long id);
    long countByDetaiid(Long id);
    boolean existsByDetaiidAndGiangvienid(Long detaiid, Long giangvienid);
    void deleteByDetaiid(Long id);
}
