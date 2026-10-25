
package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.Dangkydetai;
import com.example.quanlydetaisinhvien.entity.TrangthaiDangkydetai;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DangkydetaiRepository
        extends JpaRepository<Dangkydetai, Long> {

    Optional<Dangkydetai> findByNhomid(Long nhomid);

    List<Dangkydetai> findByNhomidIn(
            java.util.Collection<Long> nhomids);

    boolean existsByNhomid(Long nhomid);

    boolean existsByDetaiid(Long detaiid);

    boolean existsByDetaiidAndTrangthai(
            Long detaiid,
            TrangthaiDangkydetai trangthai);

    boolean existsByDetaiidAndTrangthaiAndIdNot(
            Long detaiid,
            TrangthaiDangkydetai trangthai,
            Long id);

    List<Dangkydetai> findByDetaiid(Long detaiid);

    void deleteByNhomid(Long nhomid);

    // Danh sách đăng ký có thể chấm điểm
    @org.springframework.data.jpa.repository.Query("""
        select d.id, r.id, r.tendot, t.tendetai, n.tennhom
        from Dangkydetai d
        join Detai t on t.id = d.detaiid
        join Dotdangky r on r.id = t.dotdangkyid
        join Nhomsinhvien n on n.id = d.nhomid
        where d.trangthai = :approved
          and not exists (
             select k.id from Ketqua k
             where k.dangkydetaiid = d.id
               and k.dacongbo = true
          )
          and (
             exists (
                select pb.id from Giangvienphanbien pb
                where pb.dangkydetaiid = d.id
                  and pb.giangvienid = :lecturerId
             )
             or exists (
                select p.dangkydetaiid from Phanconghoidong p, Thanhvienhoidong tv
                where p.dangkydetaiid = d.id
                  and tv.hoidongid = p.hoidongid
                  and tv.giangvienid = :lecturerId
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
    List<Object[]> findAvailableGradesForLecturer(
            @Param("lecturerId") Long lecturerId,
            @Param("approved")
            TrangthaiDangkydetai approved);

    // Lấy ID đề tài từ đăng ký
    @Query("""
        select d.detaiid
        from Dangkydetai d
        where d.id = :id
        """)
    Optional<Long> findDetaiIdByRegistrationId(
            @Param("id") Long id);

    // Khóa đăng ký khi xử lý
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select d
        from Dangkydetai d
        where d.id = :id
        """)
    Optional<Dangkydetai> findByIdForUpdate(
            @Param("id") Long id);

    // Đọc các đăng ký đã duyệt có khóa
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select d
        from Dangkydetai d
        where d.detaiid = :detaiId
          and d.trangthai = :trangthai
        """)
    List<Dangkydetai> findApprovedForUpdate(
            @Param("detaiId") Long detaiId,
            @Param("trangthai")
            TrangthaiDangkydetai trangthai);
}
