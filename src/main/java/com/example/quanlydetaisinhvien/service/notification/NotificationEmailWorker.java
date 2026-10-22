package com.example.quanlydetaisinhvien.service.notification;

import com.example.quanlydetaisinhvien.config.NotificationEmailSettings;
import com.example.quanlydetaisinhvien.entity.TrangthaiEmailthongbao;
import org.slf4j.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.concurrent.TimeUnit;

/** Polls committed outbox only. Never invoked by the notice CREATE request or an AFTER_COMMIT SMTP listener. */
@Component
public class NotificationEmailWorker {
    private static final Logger log = LoggerFactory.getLogger(NotificationEmailWorker.class);
    private final NotificationEmailQueue queue;
    private final NotificationMailSender sender;
    private final NotificationEmailSafety safety;
    private final NotificationEmailSchema schema;
    private final NotificationEmailSettings settings;
    private long lastAttemptNanos;
    public NotificationEmailWorker(NotificationEmailQueue queue, NotificationMailSender sender,
            NotificationEmailSafety safety, NotificationEmailSchema schema, NotificationEmailSettings settings) {
        this.queue=queue; this.sender=sender; this.safety=safety; this.schema=schema; this.settings=settings;
    }
    public synchronized void drain() {
        if (!safety.deliveryAllowed() || TransactionSynchronizationManager.isActualTransactionActive()) return;
        try {
            if (!schema.available()) return;
            queue.recover();
            for (Long id : queue.pending()) {
                if (Thread.currentThread().isInterrupted()) break;
                long waitNanos = TimeUnit.MILLISECONDS.toNanos(settings.minIntervalMs()) - (System.nanoTime() - lastAttemptNanos);
                if (lastAttemptNanos != 0 && waitNanos > 0) {
                    try { TimeUnit.NANOSECONDS.sleep(waitNanos); }
                    catch (InterruptedException ex) { Thread.currentThread().interrupt(); break; }
                }
                var claim = queue.claim(id);
                if (claim == null) continue;
                lastAttemptNanos = System.nanoTime();
                TrangthaiEmailthongbao status;
                String reason = null;
                try { sender.send(claim); status=TrangthaiEmailthongbao.SENT; }
                catch (NotificationMailSender.DefinitelyNotSentException ex) {
                    status=TrangthaiEmailthongbao.FAILED; reason="Chưa gửi thư: kiểm tra cấu hình, địa chỉ hoặc xác thực SMTP.";
                } catch (RuntimeException ex) {
                    status=TrangthaiEmailthongbao.UNCERTAIN; reason="Không xác định SMTP đã tiếp nhận hay chưa; cần đối soát, không tự gửi lại.";
                }
                queue.complete(claim, status, reason);
            }
        } catch (RuntimeException ex) {
            // A failure after SMTP acceptance leaves SENDING; later recovery marks UNCERTAIN, never PENDING.
            log.warn("Chưa thể xử lý outbox email thông báo; không tự thử lại thư đã claim.");
        }
    }
}
