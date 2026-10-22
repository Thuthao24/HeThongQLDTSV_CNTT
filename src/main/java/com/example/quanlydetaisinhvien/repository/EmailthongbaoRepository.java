package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;

public interface EmailthongbaoRepository extends JpaRepository<Emailthongbao, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Emailthongbao e where e.id=:id")
    Optional<Emailthongbao> lock(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Thongbao t where t.id=:id")
    Optional<Thongbao> lockNotice(@Param("id") Long id);
    boolean existsByThongbaoSnapshotId(Long id);

    @Query("select e.id from Emailthongbao e where e.trangthai=:status and e.thongbaoid is not null "
            + "and (:studentId=0 or e.sinhvienid=:studentId) and (:noticeId=0 or e.thongbaoSnapshotId=:noticeId) "
            + "and (:minNoticeId=0 or e.thongbaoSnapshotId>=:minNoticeId) "
            + "and (:system=true or lower(e.email) in :addresses) order by e.id")
    List<Long> pending(@Param("status") TrangthaiEmailthongbao status, @Param("studentId") long studentId,
            @Param("noticeId") long noticeId, @Param("minNoticeId") long minNoticeId,
            @Param("system") boolean system, @Param("addresses") Collection<String> addresses, Pageable page);

    @Modifying
    @Query("update Emailthongbao e set e.trangthai=:uncertain,e.loi=:reason where e.trangthai=:sending "
            + "and (e.lanthu is null or e.lanthu<:cutoff)")
    int expire(@Param("sending") TrangthaiEmailthongbao sending, @Param("uncertain") TrangthaiEmailthongbao uncertain,
            @Param("cutoff") LocalDateTime cutoff, @Param("reason") String reason);
    @Modifying
    @Query("update Emailthongbao e set e.trangthai=:failed,e.loi=:reason where e.trangthai=:pending and e.thongbaoid is null")
    int cancelDeleted(@Param("pending") TrangthaiEmailthongbao pending, @Param("failed") TrangthaiEmailthongbao failed,
            @Param("reason") String reason);
}
