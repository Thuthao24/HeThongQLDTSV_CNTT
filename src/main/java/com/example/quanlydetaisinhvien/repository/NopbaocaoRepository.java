package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.Nopbaocao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NopbaocaoRepository extends JpaRepository<Nopbaocao, Long> {

    List<Nopbaocao> findByDangkydetaiid(Long dangkydetaiid);
    List<Nopbaocao> findByDangkydetaiidIn(java.util.Collection<Long> dangkyids);
    java.util.Optional<Nopbaocao> findFirstByDangkydetaiidOrderByIdDesc(Long dangkydetaiid);
    List<Nopbaocao> findBySinhvienid(Long sinhvienid);
    boolean existsByDangkydetaiid(Long dangkydetaiid);
    void deleteByDangkydetaiid(Long dangkydetaiid);

    boolean existsByDuongdanfileAndDangkydetaiidNot(String duongdanfile, Long dangkydetaiid);
    long countByDuongdanfile(String duongdanfile);
}
