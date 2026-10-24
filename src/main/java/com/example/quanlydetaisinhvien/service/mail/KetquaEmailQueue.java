package com.example.quanlydetaisinhvien.service.mail;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.EmailketquaRepository;
import com.example.quanlydetaisinhvien.repository.KetquaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class KetquaEmailQueue {
    private final EmailketquaRepository emails;
    private final KetquaRepository results;
    private final KetquaEmailSafety safety;
    public KetquaEmailQueue(EmailketquaRepository emails, KetquaRepository results, KetquaEmailSafety safety) {
        this.emails = emails; this.results = results; this.safety=safety;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<Long> pending() {
        if (!safety.deliveryAllowed()) return List.of();
        if (safety.scopedTest()) {
            return emails.findById(safety.outboxId())
                    .filter(e->e.getTrangthai()==TrangthaiEmailketqua.PENDING && safety.permitted(e.getId(),e.getEmail()))
                    .map(e->List.of(e.getId())).orElseGet(List::of);
        }
        return emails.findTop100ByTrangthaiOrderByIdAsc(TrangthaiEmailketqua.PENDING)
                .stream().map(Emailketqua::getId).toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResultMailSender.Message claim(Long id) {
        if (!safety.permitsId(id)) return null;
        var email = emails.findForUpdate(id).orElse(null);
        if (email == null || email.getTrangthai() != TrangthaiEmailketqua.PENDING) return null;
        var result = results.findById(email.getKetquaid()).orElse(null);
        if (result == null || !Boolean.TRUE.equals(result.getDacongbo())) {
            email.setTrangthai(TrangthaiEmailketqua.FAILED);
            email.setLoi("Kết quả chưa được công bố; không gửi email.");
            return null;
        }
        if (MailAddress.validOrNull(email.getEmail()) == null) {
            email.setTrangthai(TrangthaiEmailketqua.MISSING_EMAIL);
            email.setLoi("Địa chỉ email không hợp lệ.");
            return null;
        }
        if (!safety.permitted(id,email.getEmail())) return null;
        email.setTrangthai(TrangthaiEmailketqua.SENDING);
        email.setSolanthu(email.getSolanthu() + 1);
        email.setLanthu(LocalDateTime.now().withNano(0));
        emails.saveAndFlush(email);
        return new ResultMailSender.Message(email.getId(), email.getSolanthu(), email.getEmail(),
                email.getTieude(), email.getNoidung());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(ResultMailSender.Message claim, TrangthaiEmailketqua status, String reason) {
        var email = emails.findForUpdate(claim.id()).orElseThrow();
        if (email.getTrangthai() != TrangthaiEmailketqua.SENDING || email.getSolanthu() != claim.attempt()) return;
        email.setTrangthai(status); email.setLoi(reason);
        if (status == TrangthaiEmailketqua.SENT) email.setNgaygui(LocalDateTime.now().withNano(0));
        emails.saveAndFlush(email);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int expireClaims(LocalDateTime cutoff) {
        if (!safety.deliveryAllowed()) return 0;
        if (safety.scopedTest()) {
            var email=emails.findForUpdate(safety.outboxId()).orElse(null);
            if (email==null || email.getTrangthai()!=TrangthaiEmailketqua.SENDING
                    || !safety.permitted(email.getId(),email.getEmail())
                    || (email.getLanthu()!=null && !email.getLanthu().isBefore(cutoff))) return 0;
            email.setTrangthai(TrangthaiEmailketqua.UNCERTAIN);
            email.setLoi("Worker chưa ghi nhận kết quả gửi; cần đối soát trước khi gửi lại.");
            emails.saveAndFlush(email);
            return 1;
        }
        return emails.expireClaims(TrangthaiEmailketqua.SENDING, TrangthaiEmailketqua.UNCERTAIN,
                cutoff, "Worker chưa ghi nhận kết quả gửi; cần đối soát trước khi gửi lại.");
    }
}
