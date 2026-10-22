package com.example.quanlydetaisinhvien.service.notification;

import com.example.quanlydetaisinhvien.service.mail.MailAddress;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;

/** Dedicated transport: never registers/uses the Module 16 JavaMailSender or its credentials. */
@Component
public class SmtpNotificationMailSender implements NotificationMailSender {
    private final NotificationEmailSafety safety;
    private final JavaMailSender transport;
    private final String host, from;
    @Autowired
    public SmtpNotificationMailSender(NotificationEmailSafety safety, Environment env) {
        this(safety, transport(env), env.getProperty("app.thongbao.email.smtp.host", ""),
                env.getProperty("app.thongbao.email.smtp.from", ""));
    }
    public SmtpNotificationMailSender(NotificationEmailSafety safety, JavaMailSender transport, String host, String from) {
        this.safety=safety; this.transport=transport; this.host=host; this.from=from;
    }
    private static JavaMailSender transport(Environment env) {
        var sender = new JavaMailSenderImpl();
        String prefix = "app.thongbao.email.smtp.";
        sender.setHost(env.getProperty(prefix + "host", "")); sender.setPort(env.getProperty(prefix + "port", Integer.class, 587));
        sender.setUsername(env.getProperty(prefix + "username", "")); sender.setPassword(env.getProperty(prefix + "password", ""));
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        var properties = sender.getJavaMailProperties();
        properties.setProperty("mail.smtp.auth", env.getProperty(prefix + "auth", "true"));
        properties.setProperty("mail.smtp.starttls.enable", env.getProperty(prefix + "starttls", "true"));
        properties.setProperty("mail.smtp.starttls.required", env.getProperty(prefix + "starttls", "true"));
        properties.setProperty("mail.smtp.connectiontimeout", "5000"); properties.setProperty("mail.smtp.timeout", "5000");
        properties.setProperty("mail.smtp.writetimeout", "5000");
        return sender;
    }
    @Override public void send(Message snapshot) {
        if (!safety.deliveryAllowed() || !safety.permitted(snapshot.studentId(), snapshot.noticeId(), snapshot.to()))
            throw new DefinitelyNotSentException("SMTP thông báo đang tắt hoặc người nhận ngoài phạm vi cho phép.");
        if (host.isBlank() || MailAddress.validOrNull(from) == null || MailAddress.validOrNull(snapshot.to()) == null)
            throw new DefinitelyNotSentException("SMTP thông báo chưa được cấu hình hợp lệ.");
        var mime = transport.createMimeMessage();
        try {
            var message = new MimeMessageHelper(mime, false, StandardCharsets.UTF_8.name());
            message.setFrom(from); message.setTo(snapshot.to()); // Exactly one To; no CC/BCC.
            message.setSubject(snapshot.subject().replaceAll("[\\r\\n]+", " "));
            message.setText(snapshot.body(), false); // User content is plain text, never HTML.
        } catch (MessagingException ex) { throw new DefinitelyNotSentException("Không thể tạo thư hợp lệ trước khi gửi."); }
        try { transport.send(mime); }
        catch (MailAuthenticationException ex) { throw new DefinitelyNotSentException("SMTP từ chối xác thực trước khi nhận thư."); }
        // Timeout, send errors and lost replies remain ambiguous; the worker records UNCERTAIN.
    }
}
