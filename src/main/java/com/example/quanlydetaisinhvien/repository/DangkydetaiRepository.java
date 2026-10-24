package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.Dangkydetai;
import com.example.quanlydetaisinhvien.entity.TrangthaiDangkydetai;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface DangkydetaiRepository extends JpaRepository<Dangkydetai, Long> {

    Optional<Dangkydetai> findByNhomid(Long nhomid);
    List<Dangkydetai> findByNhomidIn(java.util.Collection<Long> nhomids);
    boolean existsByNhomid(Long nhomid);
    boolean existsByDetaiid(Long detaiid);
    boolean existsByDetaiidAndTrangthai(Long detaiid, TrangthaiDangkydetai trangthai);
    boolean existsByDetaiidAndTrangthaiAndIdNot(Long detaiid, TrangthaiDangkydetai trangthai, Long id);
    List<Dangkydetai> findByDetaiid(Long detaiid);
    void deleteByNhomid(Long nhomid);
    @org.springframework.data.jpa.repository.Query("""
        select d.id, r.id, r.tendot, t.tendetai, n.tennhom
        from Dangkydetai d, Detai t, Dotdangky r,
             Nhomsinhvien n, Phanconghoidong p, Ketqua k
        where t.id = d.detaiid
          and r.id = t.dotdangkyid
          and n.id = d.nhomid
          and p.dangkydetaiid = d.id
          and k.dangkydetaiid = d.id
          and d.trangthai = :approved
          and k.dacongbo = false
          and (
             exists (
                select tv.hoidongid from Thanhvienhoidong tv
                where tv.hoidongid = p.hoidongid
                  and tv.giangvienid = :lecturerId
             )
             or exists (
                select pb.id from Giangvienphanbien pb
                where pb.dangkydetaiid = d.id
                  and pb.giangvienid = :lecturerId
             )
          )
          and not exists (
             select s.id from Giangvienhuongdan s
             where s.detaiid = t.id
               and s.giangvienid = :lecturerId
          )
          and not exists (
             select sc.id from Diem sc
             where sc.dangkydetaiid = d.id
               and sc.giangvienid = :lecturerId
          )
        order by r.id desc, d.id desc
        """)
    java.util.List<Object[]> findAvailableGradesForLecturer(
        @org.springframework.data.repository.query.Param("lecturerId")
        Long lecturerId,
        @org.springframework.data.repository.query.Param("approved")
        TrangthaiDangkydetai approved);
        
    @Query("""select d.detaiid from Dangkydetai d  where d.id = :id """)
    Optional<Long> findDetaiIdByRegistrationId( @Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""select d from Dangkydetai d where d.id = :id """)
    Optional<Dangkydetai> findByIdForUpdate(
        @Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""select d from Dangkydetai d  where d.detaiid = :detaiId and d.trangthai = :trangthai""")
    List<Dangkydetai> findApprovedForUpdate(
        @Param("detaiId") Long detaiId,
        @Param("trangthai") TrangthaiDangkydetai trangthai);
}
