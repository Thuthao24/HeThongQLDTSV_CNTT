package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.Ketqua;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface KetquaRepository extends JpaRepository<Ketqua, Long> {
    boolean existsByDangkydetaiid(Long dangkydetaiid);
    boolean existsByDangkydetaiidAndDacongboTrue(Long dangkydetaiid);

    @Query("SELECT k.dangkydetaiid FROM Ketqua k WHERE k.dangkydetaiid IN :ids AND k.dacongbo = true")
    java.util.Set<Long> findDangkydetaiidsWithKetquaDaCongBo(@Param("ids") java.util.Collection<Long> ids);
    Optional<Ketqua> findByDangkydetaiid(Long dangkydetaiid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select k from Ketqua k where k.id = :id")
    Optional<Ketqua> findByIdForPublication(@Param("id") Long id);

    // Entities store foreign-key IDs rather than mapped JPA associations.
    @Query("""
            select k from Ketqua k
            where k.dacongbo = true and exists (
                select d.id from Dangkydetai d, Nhomsinhvien n,
                    Thanhviennhom tv, Sinhvien sv, Taikhoan tk
                where d.id = k.dangkydetaiid and n.id = d.nhomid
                    and tv.nhomid = n.id and tv.sinhvienid = sv.id
                    and sv.taikhoanid = tk.id and tk.tendangnhap = :username
            )
            """)
    List<Ketqua> findPublishedForStudent(@Param("username") String username);

    @Query("""
            select k from Ketqua k
            where k.dacongbo = true and exists (
                select d.id from Dangkydetai d, Nhomsinhvien n,
                    Thanhviennhom tv, Sinhvien sv, Taikhoan tk
                where d.id = k.dangkydetaiid
                    and n.id = d.nhomid
                    and tv.nhomid = n.id
                    and tv.sinhvienid = sv.id
                    and sv.taikhoanid = tk.id
                    and tk.tendangnhap = :username
            )
            """)
    Page<Ketqua> findPublishedPageForStudent(
            @Param("username") String username,
            Pageable pageable);

    @Query("""
            select k from Ketqua k
            where k.id = :id and k.dacongbo = true and exists (
                select d.id from Dangkydetai d, Nhomsinhvien n,
                    Thanhviennhom tv, Sinhvien sv, Taikhoan tk
                where d.id = k.dangkydetaiid and n.id = d.nhomid
                    and tv.nhomid = n.id and tv.sinhvienid = sv.id
                    and sv.taikhoanid = tk.id and tk.tendangnhap = :username
            )
            """)
    Optional<Ketqua> findPublishedByIdForStudent(@Param("id") Long id,
                                                @Param("username") String username);
}
