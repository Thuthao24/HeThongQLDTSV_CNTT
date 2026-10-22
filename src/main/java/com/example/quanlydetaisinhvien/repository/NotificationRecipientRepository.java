package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.entity.Vaitro;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class NotificationRecipientRepository {
    public static final int PAGE_SIZE = 500;
    private static final String ELIGIBLE = " from Sinhvien s join Taikhoan a on a.id=s.taikhoanid "
            + "where a.vaitro=:role and a.kichhoat=true";
    private final EntityManager em;
    public NotificationRecipientRepository(EntityManager em) { this.em = em; }
    public record Recipient(Long id, String masinhvien, String hoten, String email) {}
    public record Scope(Long lastId, Long count) {}
    public Scope scope() {
        return em.createQuery("select new com.example.quanlydetaisinhvien.repository.NotificationRecipientRepository$Scope"
                + "(coalesce(max(s.id),0L),count(s.id))" + ELIGIBLE, Scope.class)
                .setParameter("role", Vaitro.SINHVIEN).getSingleResult();
    }
    public List<Recipient> batchAfter(long id, long lastId) {
        return em.createQuery("select new com.example.quanlydetaisinhvien.repository.NotificationRecipientRepository$Recipient"
                + "(s.id,s.masinhvien,a.hoten,a.email)" + ELIGIBLE
                + " and s.id>:id and s.id<=:lastId order by s.id", Recipient.class)
                .setParameter("id", id).setParameter("lastId", lastId)
                .setParameter("role", Vaitro.SINHVIEN).setMaxResults(PAGE_SIZE).getResultList();
    }
}
