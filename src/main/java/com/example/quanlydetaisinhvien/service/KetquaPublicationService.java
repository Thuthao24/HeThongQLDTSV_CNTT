package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.security.KetquaPermissions;
import com.example.quanlydetaisinhvien.service.mail.KetquaEmailOutbox;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class KetquaPublicationService {
    private final KetquaRepository results;
    private final PhanconghoidongRepository assignments;
    private final HoidongRepository councils;
    private final KetquaPublicationValidator validator;
    private final KetquaSchema schema;
    private final ApplicationEventPublisher events;
    private final KetquaEmailOutbox outbox;

    public KetquaPublicationService(KetquaRepository results, PhanconghoidongRepository assignments,
            HoidongRepository councils, KetquaPublicationValidator validator, KetquaSchema schema,
            ApplicationEventPublisher events, KetquaEmailOutbox outbox) {
        this.results = results; this.assignments = assignments; this.councils = councils;
        this.validator = validator; this.schema = schema; this.events = events;
        this.outbox = outbox;
    }

    @Transactional(readOnly = true)
    public Map<Long, KetquaPublicationInfo> info(List<Ketqua> items) {
        KetquaPermissions.requireTruongKhoa();
        boolean ready = schema.hasTable("Phanconghoidong");
        boolean emailReady = schema.hasTable("Emailketqua");
        Map<Long, KetquaPublicationInfo> information = new HashMap<>();
        for (var result : items) {
            if (Boolean.TRUE.equals(result.getDacongbo())) {
                information.put(result.getId(), new KetquaPublicationInfo("Đã công bố.", false, false, null, List.of()));
            } else if (!ready) {
                information.put(result.getId(), new KetquaPublicationInfo(
                        "Chưa có bảng Phanconghoidong; cần migration được duyệt riêng.", false, false, null, List.of()));
            } else {
                Phanconghoidong assignment = assignments.findById(result.getDangkydetaiid()).orElse(null);
                List<Hoidong> choices = List.of();
                boolean canAssign = false;
                boolean allowNoCouncil = false;
                String reason = "";
                try {
                    var context = validator.context(result);
                    choices = councils.findByDotdangkyid(context.round().getId());
                    canAssign = true;
                    allowNoCouncil = context.round().getLoaidot() != Loaidot.KLTN;
                    validator.validate(result, assignment);
                    if (!emailReady) reason = "Chưa có bảng Emailketqua; cần migration được duyệt riêng.";
                } catch (NghiepvuException ex) {
                    reason = ex.getMessage();
                }
                information.put(result.getId(), new KetquaPublicationInfo(reason, canAssign, allowNoCouncil, assignment, choices));
            }
        }
        return information;
    }

    @Transactional
    public void assign(Long resultId, HinhthucDanhgia form, Long councilId) {
        KetquaPermissions.requireTruongKhoa();
        requireSchema();
        Ketqua result = unpublishedForUpdate(resultId);
        Phanconghoidong assignment = new Phanconghoidong();
        assignment.setDangkydetaiid(result.getDangkydetaiid());
        assignment.setHinhthuc(form);
        assignment.setHoidongid(councilId);
        validator.assignedGraders(validator.context(result), assignment);
        assignments.saveAndFlush(assignment);
    }

    @Transactional
    public Ketqua publish(Long resultId) {
        KetquaPermissions.requireTruongKhoa();
        // Check repeat before querying the new table; old published results remain untouched.
        Ketqua result = unpublishedForUpdate(resultId);
        requireSchema();
        validator.validate(result, assignments.findById(result.getDangkydetaiid()).orElse(null), true);
        result.setDacongbo(true);
        result.setNgaycongbo(LocalDateTime.now().withNano(0));
        results.saveAndFlush(result);
        outbox.enqueue(result);
        events.publishEvent(new KetquaPublishedEvent(result.getId()));
        return result;
    }

    private Ketqua unpublishedForUpdate(Long id) {
        if (id == null || id <= 0) throw new NghiepvuException("ID kết quả không hợp lệ.");
        var result = results.findByIdForPublication(id)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy kết quả."));
        if (Boolean.TRUE.equals(result.getDacongbo()))
            throw new NghiepvuException("Kết quả đã được công bố; giữ nguyên ngày công bố.");
        return result;
    }

    private void requireSchema() {
        if (!schema.hasTable("Phanconghoidong"))
            throw new NghiepvuException("Chưa có bảng Phanconghoidong; cần migration được duyệt riêng.");
    }
}
