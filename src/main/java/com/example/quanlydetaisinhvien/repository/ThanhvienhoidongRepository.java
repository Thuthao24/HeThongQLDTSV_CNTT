package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.Thanhvienhoidong;
import com.example.quanlydetaisinhvien.entity.ThanhvienhoidongId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ThanhvienhoidongRepository extends JpaRepository<Thanhvienhoidong, ThanhvienhoidongId> {
    List<Thanhvienhoidong> findByHoidongid(Long id);
    List<Thanhvienhoidong> findByHoidongidIn(java.util.Collection<Long> ids);
    long countByHoidongid(Long id);
    boolean existsByHoidongidAndVaitro(Long hoidongid, com.example.quanlydetaisinhvien.entity.VaitroHoidong vaitro);
    void deleteByHoidongid(Long hoidongid);
    boolean existsByHoidongidAndGiangvienid(Long hoidongid, Long giangvienid);
}
