package com.example.quanlydetaisinhvien.service.morong.export;

import com.example.quanlydetaisinhvien.dto.morong.DetaiFilterDTO;
import com.example.quanlydetaisinhvien.entity.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import java.util.*;

/** Presentation for exports only. Lookups are scalar and bounded, independent of report row count. */
@Component
public class ReportLabels {
    private final EntityManager em;
    public ReportLabels(EntityManager em) { this.em = em; }

    public List<String> topicFilters(DetaiFilterDTO f) {
        var labels = new ArrayList<String>();
        String keyword = f.getQ().isEmpty() ? f.getKeyword() : f.getQ();
        if (!keyword.isEmpty()) labels.add("Từ khóa: " + keyword);
        if (f.getDotdangkyid() != null) labels.add("Đợt đăng ký: " + name(
                "select d.tendot from Dotdangky d where d.id = :id", f.getDotdangkyid()));
        if (f.getLoaidot() != null) labels.add("Loại đợt: " + roundType(f.getLoaidot()));
        if (f.getBomonid() != null) labels.add("Bộ môn: " + name(
                "select b.tenbomon from Bomon b where b.id = :id", f.getBomonid()));
        if (f.getTrangthai() != null) labels.add("Trạng thái đề tài: " + topicStatus(f.getTrangthai()));
        var teacherIds = new HashSet<Long>();
        if (f.getGiangviendexuatid() != null) teacherIds.add(f.getGiangviendexuatid());
        if (f.getGiangvienhuongdanid() != null) teacherIds.add(f.getGiangvienhuongdanid());
        var teachers = new HashMap<Long, String>();
        if (!teacherIds.isEmpty()) {
            for (var row : em.createQuery("select g.id, t.hoten from Giangvien g, Taikhoan t "
                    + "where g.taikhoanid = t.id and g.id in :ids", Object[].class)
                    .setParameter("ids", teacherIds).getResultList()) {
                teachers.put((Long) row[0], validName((String) row[1]));
            }
        }
        if (f.getGiangviendexuatid() != null) labels.add("Giảng viên đề xuất: " + validName(teachers.get(f.getGiangviendexuatid())));
        if (f.getGiangvienhuongdanid() != null) labels.add("Giảng viên hướng dẫn: " + validName(teachers.get(f.getGiangvienhuongdanid())));
        // Default ordering and pagination do not add noise to the report description.
        if (!"id,desc".equals(f.getSort())) labels.add("Sắp xếp: " + ordering(f.getSort()));
        return labels;
    }

    private String name(String query, Long id) {
        var names = em.createQuery(query, String.class).setParameter("id", id).getResultList();
        return validName(names.isEmpty() ? null : names.getFirst());
    }
    private String validName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Điều kiện lọc không còn tồn tại hoặc thiếu tên hiển thị hợp lệ.");
        return name;
    }
    private String ordering(String sort) {
        var parts = sort.split(",", -1);
        String field = switch (parts[0]) {
            case "id" -> "Mã đề tài";
            case "tendetai" -> "Tên đề tài";
            case "tendot" -> "Tên đợt đăng ký";
            case "trangthai" -> "Trạng thái đề tài";
            default -> throw new IllegalArgumentException("Trường sắp xếp không hợp lệ.");
        };
        return field + (parts[1].equals("asc") ? " tăng dần" : " giảm dần");
    }
    public static String roundType(Loaidot type) {
        if (type == null) return null;
        return switch (type) {
            case MONHOC -> "Môn học";
            case NCKH -> "Nghiên cứu khoa học";
            case TLCN -> "Tiểu luận chuyên ngành";
            case KLTN -> "Khóa luận tốt nghiệp";
        };
    }
    public static String topicStatus(TrangthaiDetai status) {
        if (status == null) return null;
        return switch (status) {
            case CHODUYET -> "Chờ duyệt";
            case DADUYET -> "Đã duyệt";
            case TUCHOI -> "Từ chối";
            case CONGBO -> "Đã công bố";
        };
    }
}
