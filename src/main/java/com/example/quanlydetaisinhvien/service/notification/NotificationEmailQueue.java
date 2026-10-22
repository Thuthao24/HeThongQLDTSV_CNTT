package com.example.quanlydetaisinhvien.service.notification;

import com.example.quanlydetaisinhvien.config.NotificationEmailSettings;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.EmailthongbaoRepository;
import com.example.quanlydetaisinhvien.service.mail.MailAddress;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;

@Service
public class NotificationEmailQueue {
    private final EmailthongbaoRepository emails;
    private final NotificationEmailSettings settings;
    private final NotificationEmailSafety safety;
    public NotificationEmailQueue(EmailthongbaoRepository emails, NotificationEmailSettings settings, NotificationEmailSafety safety) {
        this.emails=emails; this.settings=settings; this.safety=safety;
    }
    static LocalDateTime now() { return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).withNano(0); }
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<Long> pending() {
        if (!safety.deliveryAllowed()) return List.of();
        var allowed = safety.addresses();
        return emails.pending(TrangthaiEmailthongbao.PENDING, settings.studentId(), settings.noticeId(), settings.minNoticeId(),
                settings.mode() == NotificationEmailSettings.Mode.SYSTEM, allowed.isEmpty() ? Set.of("") : allowed,
                PageRequest.of(0, settings.batchSize()));
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationMailSender.Message claim(Long id) {
        if (!safety.deliveryAllowed()) return null;
        var email = emails.lock(id).orElse(null);
        if (email == null || email.getTrangthai() != TrangthaiEmailthongbao.PENDING) return null;
        if (email.getThongbaoid() == null) { fail(email, "Thông báo đã bị xóa; không gửi email."); return null; }
        if (MailAddress.validOrNull(email.getEmail()) == null) {
            email.setTrangthai(TrangthaiEmailthongbao.MISSING_EMAIL); email.setLoi("Địa chỉ email snapshot không hợp lệ."); return null;
        }
        if (!safety.permitted(email.getSinhvienid(), email.getThongbaoSnapshotId(), email.getEmail())) return null;
        email.setTrangthai(TrangthaiEmailthongbao.SENDING); email.setSolanthu(email.getSolanthu()+1);
        email.setLanthu(now()); email.setLoi(null); emails.saveAndFlush(email);
        return new NotificationMailSender.Message(email.getId(), email.getSolanthu(), email.getSinhvienid(),
                email.getThongbaoSnapshotId(), email.getEmail(), email.getTieude(), email.getNoidung());
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(NotificationMailSender.Message claim, TrangthaiEmailthongbao status, String reason) {
        if (!Set.of(TrangthaiEmailthongbao.SENT, TrangthaiEmailthongbao.FAILED, TrangthaiEmailthongbao.UNCERTAIN).contains(status))
            throw new IllegalArgumentException("Trạng thái hoàn tất không hợp lệ.");
        var email = emails.lock(claim.id()).orElseThrow();
        if (email.getTrangthai()!=TrangthaiEmailthongbao.SENDING || email.getSolanthu()!=claim.attempt()) return;
        email.setTrangthai(status); email.setLoi(reason);
        if (status == TrangthaiEmailthongbao.SENT) email.setNgaygui(now());
        emails.saveAndFlush(email);
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recover() {
        if (!safety.deliveryAllowed()) return;
        emails.expire(TrangthaiEmailthongbao.SENDING, TrangthaiEmailthongbao.UNCERTAIN,
                now().minusSeconds(settings.claimTtlSeconds()), "Worker chưa ghi kết quả; cần đối soát SMTP, không tự gửi lại.");
        emails.cancelDeleted(TrangthaiEmailthongbao.PENDING, TrangthaiEmailthongbao.FAILED, "Thông báo đã bị xóa; không gửi email.");
    }
    private void fail(Emailthongbao email, String message) { email.setTrangthai(TrangthaiEmailthongbao.FAILED); email.setLoi(message); }
}
