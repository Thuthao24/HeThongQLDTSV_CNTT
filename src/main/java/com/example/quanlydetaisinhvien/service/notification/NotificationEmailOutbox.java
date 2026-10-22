package com.example.quanlydetaisinhvien.service.notification;

import com.example.quanlydetaisinhvien.config.NotificationEmailSettings;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.security.NotificationEmailPermissions;
import com.example.quanlydetaisinhvien.service.mail.MailAddress;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

@Service
public class NotificationEmailOutbox {
    private final NotificationEmailSettings settings;
    private final NotificationEmailSchema schema;
    private final NotificationEmailPermissions permissions;
    private final NotificationEmailSafety safety;
    private final EmailthongbaoRepository emails;
    private final NotificationRecipientRepository recipients;
    private final EntityManager em;
    public NotificationEmailOutbox(NotificationEmailSettings settings, NotificationEmailSchema schema,
            NotificationEmailPermissions permissions, NotificationEmailSafety safety,
            EmailthongbaoRepository emails, NotificationRecipientRepository recipients, EntityManager em) {
        this.settings=settings; this.schema=schema; this.permissions=permissions; this.safety=safety;
        this.emails=emails; this.recipients=recipients;
        this.em=em;
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(Thongbao created, String author) {
        if (!settings.outboxEnabled()) return; // No schema access when feature has not been deployed.
        permissions.requireChief(); schema.requireAvailable();
        var notice = emails.lockNotice(created.getId()).orElseThrow();
        if (emails.existsByThongbaoSnapshotId(notice.getId())) return;
        var scope = recipients.scope();
        if (scope.count() > settings.maxRecipientsPerNotice())
            throw new DataAccessResourceFailureException("Số người nhận vượt giới hạn xử lý; không tạo hàng đợi một phần.");
        long after = 0;
        long enqueued = 0;
        while (true) {
            var batch = recipients.batchAfter(after, scope.lastId());
            if (batch.isEmpty()) break;
            if (enqueued + batch.size() > settings.maxRecipientsPerNotice())
                throw new DataAccessResourceFailureException("Phạm vi người nhận đã thay đổi; cần tạo lại thông báo.");
            var managedBatch = new ArrayList<Emailthongbao>(batch.size());
            for (var recipient : batch) {
                String address = MailAddress.validOrNull(recipient.email());
                var email = new Emailthongbao();
                email.setThongbaoid(notice.getId()); email.setThongbaoSnapshotId(notice.getId());
                email.setSinhvienid(recipient.id()); email.setNguoinhan(recipient.masinhvien() + " · " + recipient.hoten());
                email.setEmail(address);
                email.setTieude("[HCMUTE] Thông báo mới: " + notice.getTieude().replaceAll("[\\r\\n]+", " "));
                email.setNoidung("Xin chào " + recipient.hoten() + ",\n\nThông báo mới: " + notice.getTieude()
                        + "\n\n" + notice.getNoidung() + "\n\nNgười đăng: " + author
                        + "\nThời gian đăng: " + notice.getNgaydang().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        + "\nChi tiết (yêu cầu đăng nhập): " + safety.detailLink(notice.getId()));
                email.setNgaytao(notice.getNgaydang());
                email.setTrangthai(address == null ? TrangthaiEmailthongbao.MISSING_EMAIL : TrangthaiEmailthongbao.PENDING);
                if (address == null) email.setLoi("Sinh viên chưa có địa chỉ email hợp lệ tại thời điểm tạo thông báo.");
                managedBatch.add(emails.save(email));
            }
            emails.flush();
            // Detach ONLY our flushed journal rows, never clear the caller's persistence context.
            managedBatch.forEach(em::detach);
            enqueued += batch.size(); after = batch.getLast().id();
        }
        if (enqueued != scope.count())
            throw new DataAccessResourceFailureException("Phạm vi người nhận đã thay đổi; không lưu hàng đợi thiếu.");
    }
}
