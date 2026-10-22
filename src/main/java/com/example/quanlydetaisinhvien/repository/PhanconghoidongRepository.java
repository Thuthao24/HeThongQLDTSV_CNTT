package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.Phanconghoidong;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhanconghoidongRepository extends JpaRepository<Phanconghoidong, Long> {
    java.util.List<Phanconghoidong> findByHoidongid(Long hoidongid);
}
