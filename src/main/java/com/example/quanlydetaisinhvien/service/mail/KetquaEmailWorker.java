package com.example.quanlydetaisinhvien.service.mail;

import com.example.quanlydetaisinhvien.entity.TrangthaiEmailketqua;
import com.example.quanlydetaisinhvien.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;

@Component
public class KetquaEmailWorker {
    private static final Logger log = LoggerFactory.getLogger(KetquaEmailWorker.class);
    private final KetquaEmailQueue queue;
    private final ResultMailSender sender;
    private final KetquaSchema schema;
    private final boolean enabled;

    public KetquaEmailWorker(KetquaEmailQueue queue, ResultMailSender sender, KetquaSchema schema,
            @Value("${app.ketqua.email.enabled:false}") boolean enabled) {
        this.queue = queue; this.sender = sender; this.schema = schema; this.enabled = enabled;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterPublication(KetquaPublishedEvent event) { drain(); }

    @EventListener(ApplicationReadyEvent.class)
    public void afterStartup() { drain(); }

    @Scheduled(fixedDelayString = "${app.ketqua.email.poll-ms:60000}",
            initialDelayString = "${app.ketqua.email.poll-ms:60000}")
    public void drain() {
        if (!enabled || !schema.hasTable("Emailketqua")) return;
        try {
            // Unknown outcome of an old SENDING claim must never become an automatic retry.
            queue.expireClaims(LocalDateTime.now().minusMinutes(15));
            for (Long id : queue.pending()) process(id);
        } catch (RuntimeException ex) {
            // After-commit failure must not be reported as a failed publication.
            log.warn("Chưa thể xử lý hàng đợi email kết quả; sẽ kiểm tra lại ở lần quét sau.");
        }
    }

    public void process(Long id) {
        if (!enabled) return;
        var claim = queue.claim(id);
        if (claim == null) return;
        TrangthaiEmailketqua status;
        String reason = null;
        try {
            sender.send(claim);
            status = TrangthaiEmailketqua.SENT;
        } catch (ResultMailSender.DefinitelyNotSentException ex) {
            status = TrangthaiEmailketqua.FAILED;
            reason = "Chưa gửi thư: kiểm tra cấu hình hoặc kết nối Mailjet trước khi thử lại.";
        } catch (RuntimeException ex) {
            status = TrangthaiEmailketqua.UNCERTAIN;
            reason = "Không xác định Mailjet đã nhận thư hay chưa; cần đối soát, không tự gửi lại.";
        }
        queue.complete(claim, status, reason);
    }
}
