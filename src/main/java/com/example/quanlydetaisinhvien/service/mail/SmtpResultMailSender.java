package com.example.quanlydetaisinhvien.service.mail;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpResultMailSender implements ResultMailSender {
    private final ObjectProvider<JavaMailSender> sender;
    private final String from;
    private final String host;
    private final boolean enabled;

    public SmtpResultMailSender(ObjectProvider<JavaMailSender> sender,
            @Value("${app.ketqua.email.from:}") String from,
            @Value("${spring.mail.host:}") String host,
            @Value("${app.ketqua.email.enabled:false}") boolean enabled) {
        this.sender = sender; this.from = from; this.host = host;
        this.enabled = enabled;
    }

    @Override
    public void send(Message snapshot) {
        if (!enabled) throw new DefinitelyNotSentException("Gửi email thật đang tắt.");
        JavaMailSender transport = sender.getIfAvailable();
        if (transport == null || host.isBlank() || MailAddress.validOrNull(from) == null)
            throw new DefinitelyNotSentException("SMTP chưa được cấu hình hợp lệ.");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(snapshot.to());
        message.setSubject(snapshot.subject()); message.setText(snapshot.body());
        try {
            transport.send(message);
        } catch (MailAuthenticationException ex) {
            throw new DefinitelyNotSentException("SMTP từ chối xác thực trước khi nhận thư.");
        }
        // All other exceptions are ambiguous; worker records UNCERTAIN, never auto-retries.
    }
}
