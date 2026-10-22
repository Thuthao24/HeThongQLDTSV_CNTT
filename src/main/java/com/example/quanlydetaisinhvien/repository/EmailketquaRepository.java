package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailketquaRepository extends JpaRepository<Emailketqua, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Emailketqua e where e.id = :id")
    Optional<Emailketqua> findForUpdate(@Param("id") Long id);
    List<Emailketqua> findTop100ByTrangthaiOrderByIdAsc(TrangthaiEmailketqua status);
    List<Emailketqua> findByKetquaidInOrderByIdAsc(List<Long> ids);
    List<Emailketqua> findByKetquaidOrderByIdAsc(Long id);

    @Modifying
    @Query("""
            update Emailketqua e set e.trangthai = :uncertain, e.loi = :reason
            where e.trangthai = :sending and (e.lanthu is null or e.lanthu < :cutoff)
            """)
    int expireClaims(@Param("sending") TrangthaiEmailketqua sending,
            @Param("uncertain") TrangthaiEmailketqua uncertain,
            @Param("cutoff") LocalDateTime cutoff, @Param("reason") String reason);
}
