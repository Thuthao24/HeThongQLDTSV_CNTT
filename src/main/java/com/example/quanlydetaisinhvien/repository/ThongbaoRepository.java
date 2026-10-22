package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.Thongbao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ThongbaoRepository extends JpaRepository<Thongbao, Long> {

    Page<Thongbao> findByTieudeContainingIgnoreCaseOrNoidungContainingIgnoreCase(
            String tieude, String noidung, Pageable pageable);
    // Spring Data applies the limit in SQL, including a stable tie-breaker.
    List<Thongbao> findTop5ByOrderByNgaydangDescIdDesc();

    List<Thongbao> findByTieudeContainingIgnoreCaseOrNoidungContainingIgnoreCase(
            String tieude, String noidung, Sort sort);
}
