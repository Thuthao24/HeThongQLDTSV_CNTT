package com.example.quanlydetaisinhvien.service.mail;

import com.example.quanlydetaisinhvien.service.mailjet.MailjetClient;
import org.springframework.stereotype.Component;

/**
 * Nhà cung cấp gửi email kết quả duy nhất của Module 16 sử dụng Mailjet Send API v3.1 qua HTTPS.
 * - Hoạt động độc lập qua HTTPS (port 443), tương thích hoàn toàn với Render Free.
 * - Không phụ thuộc JavaMailSender hay giao thức SMTP (port 587 bị Render Free chặn).
 */
@Component
public class MailjetResultMailSender implements ResultMailSender {
    private static volatile MailjetClient.Accepted lastAccepted;

    public static MailjetClient.Accepted getLastAccepted() {
        return lastAccepted;
    }

    private final MailjetClient client;
    private final KetquaEmailSafety safety;

    public MailjetResultMailSender(MailjetClient client, KetquaEmailSafety safety) {
        this.client = client;
        this.safety = safety;
    }

    @Override
    public void send(Message snapshot) {
        if (!safety.permitted(snapshot.id(), snapshot.to())) {
            throw new DefinitelyNotSentException("Email kết quả đang tắt hoặc ngoài phạm vi thử nghiệm.");
        }
        try {
            lastAccepted = client.send(MailjetClient.Flow.RESULT, snapshot.id(), snapshot.attempt(), snapshot.to(), snapshot.subject(), snapshot.body());
        } catch (MailjetClient.Rejected ex) {
            throw new DefinitelyNotSentException(ex.getMessage());
        }
    }
}
