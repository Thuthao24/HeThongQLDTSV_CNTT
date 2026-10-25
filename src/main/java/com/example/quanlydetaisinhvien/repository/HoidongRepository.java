package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.Hoidong;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HoidongRepository extends JpaRepository<Hoidong, Long> {
    List<Hoidong> findByDotdangkyid(Long id);

    @org.springframework.data.jpa.repository.Query("""
        select h from Hoidong h
        where exists (
            select m.hoidongid
            from Thanhvienhoidong m
            where m.hoidongid = h.id
              and m.giangvienid = :lecturerId
        )
        """)
    org.springframework.data.domain.Page<Hoidong> findVisibleForLecturer(
        @org.springframework.data.repository.query.Param("lecturerId") Long lecturerId,
        org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
        select h from Hoidong h
        where exists (
            select m.hoidongid
            from Thanhvienhoidong m
            where m.hoidongid = h.id
              and m.giangvienid = :lecturerId
        )
        """)
    List<Hoidong> findAllVisibleForLecturer(
        @org.springframework.data.repository.query.Param("lecturerId") Long lecturerId);

    @org.springframework.data.jpa.repository.Query("""
        select h from Hoidong h
        where h.id = :councilId
          and exists (
              select m.hoidongid
              from Thanhvienhoidong m
              where m.hoidongid = h.id
                and m.giangvienid = :lecturerId
          )
        """)
    java.util.Optional<Hoidong> findVisibleById(
        @org.springframework.data.repository.query.Param("councilId") Long councilId,
        @org.springframework.data.repository.query.Param("lecturerId") Long lecturerId);
}
