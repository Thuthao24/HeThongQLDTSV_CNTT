package com.example.quanlydetaisinhvien.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import com.example.quanlydetaisinhvien.entity.Dotdangky;
import com.example.quanlydetaisinhvien.entity.TrangthaiDotdangky;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DotdangkyRepository extends JpaRepository<Dotdangky, Long> {
    List<Dotdangky> findByTrangthaiIn(Collection<TrangthaiDotdangky> trangthais);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Dotdangky d where d.id = :id")
    Optional<Dotdangky> findByIdForUpdate(@Param("id") Long id);
    @org.springframework.data.jpa.repository.Query("select e from Dotdangky e where lower(coalesce(e.tendot, '')) like lower(concat('%', :q, '%'))")
    org.springframework.data.domain.Page<Dotdangky> searchPage(@org.springframework.data.repository.query.Param("q") String q, org.springframework.data.domain.Pageable pageable);
}