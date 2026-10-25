package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.Thongbao;
import com.example.quanlydetaisinhvien.entity.DoituongThongbao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ThongbaoRepository extends JpaRepository<Thongbao, Long> {
    String VISIBLE = "select t from Thongbao t where (:manager=true or t.recipientRoles is empty or :role member of t.recipientRoles) "
            + "and (lower(t.tieude) like :pattern escape '!' or lower(t.noidung) like :pattern escape '!')";
    @Query(VISIBLE)
    Page<Thongbao> visible(@Param("manager") boolean manager,@Param("role") DoituongThongbao role,
            @Param("pattern") String pattern,Pageable pageable);
    @Query(VISIBLE)
    List<Thongbao> visibleLatest(@Param("manager") boolean manager,@Param("role") DoituongThongbao role,
            @Param("pattern") String pattern,Pageable pageable);
    @Query(VISIBLE)
    List<Thongbao> visible(@Param("manager") boolean manager,@Param("role") DoituongThongbao role,
            @Param("pattern") String pattern,Sort sort);
    @Query("select distinct t from Thongbao t left join fetch t.recipientRoles where t.id in :ids")
    List<Thongbao> withAudience(@Param("ids") java.util.Collection<Long> ids);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Thongbao t where t.id=:id")
    Optional<Thongbao> findForUpdate(@Param("id") Long id);

    Page<Thongbao> findByTieudeContainingIgnoreCaseOrNoidungContainingIgnoreCase(
            String tieude, String noidung, Pageable pageable);
    // Spring Data applies the limit in SQL, including a stable tie-breaker.
    List<Thongbao> findTop5ByOrderByNgaydangDescIdDesc();

    List<Thongbao> findByTieudeContainingIgnoreCaseOrNoidungContainingIgnoreCase(
            String tieude, String noidung, Sort sort);
}
