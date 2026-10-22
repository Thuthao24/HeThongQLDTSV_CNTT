package com.example.quanlydetaisinhvien.repository;

import com.example.quanlydetaisinhvien.dto.NotificationEmailFilterDTO;
import com.example.quanlydetaisinhvien.dto.NotificationEmailModels.*;
import com.example.quanlydetaisinhvien.security.NotificationEmailPermissions;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class NotificationEmailMonitorRepository {
    private final EntityManager em;
    private final NotificationEmailPermissions permissions;
    public NotificationEmailMonitorRepository(EntityManager em, NotificationEmailPermissions permissions) {
        this.em=em; this.permissions=permissions;
    }
    public List<Object[]> counts(NotificationEmailFilterDTO filter) {
        permissions.requireChief();
        return bind(em.createQuery("select e.trangthai,count(e.id) from Emailthongbao e" + where(filter)
                + " group by e.trangthai", Object[].class), filter).getResultList();
    }
    public List<Item> items(NotificationEmailFilterDTO filter) {
        permissions.requireChief();
        return bind(em.createQuery("select new com.example.quanlydetaisinhvien.dto.NotificationEmailModels$Item"
                + "(e.id,e.thongbaoid,e.thongbaoSnapshotId,e.sinhvienid,e.nguoinhan,e.email,e.tieude,e.trangthai,"
                + "e.solanthu,e.ngaytao,e.lanthu,e.ngaygui,e.loi) from Emailthongbao e" + where(filter)
                + " order by e.id desc", Item.class), filter).setFirstResult(filter.getPage()*filter.getSize())
                .setMaxResults(filter.getSize()).getResultList();
    }
    private String where(NotificationEmailFilterDTO f) {
        return " where 1=1" + (f.getThongbaoid()==null ? "" : " and e.thongbaoSnapshotId=:notice")
                + (f.getTrangthai()==null ? "" : " and e.trangthai=:status");
    }
    private <T> TypedQuery<T> bind(TypedQuery<T> q, NotificationEmailFilterDTO f) {
        if (f.getThongbaoid()!=null) q.setParameter("notice",f.getThongbaoid());
        if (f.getTrangthai()!=null) q.setParameter("status",f.getTrangthai());
        return q;
    }
}
