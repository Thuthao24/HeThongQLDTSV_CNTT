package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ThongbaoReadRepository extends JpaRepository<ThongbaoRead,ThongbaoReadId> {
    String UNREAD = " from Thongbao t where t.id>:boundary "
            + "and (t.recipientRoles is empty or :role member of t.recipientRoles) "
            + "and not exists (select r.id from ThongbaoRead r where r.id.accountId=:accountId and r.id.noticeId=t.id)";
    @Query("select count(t.id)"+UNREAD)
    long countUnread(@Param("accountId") Long accountId,@Param("role") DoituongThongbao role,@Param("boundary") long boundary);
    @Query("select t.id"+UNREAD+" and t.id in :ids")
    Set<Long> unreadAmong(@Param("accountId") Long accountId,@Param("role") DoituongThongbao role,
            @Param("boundary") long boundary,@Param("ids") Collection<Long> ids);
    // Serialize receipt inserts for the same account before checking the composite key.
    // All mark-read calls acquire account -> notice in that order; notice edits/deletes use the same notice lock.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Taikhoan a where a.tendangnhap=:username")
    Optional<Taikhoan> lockReader(@Param("username") String username);
}
