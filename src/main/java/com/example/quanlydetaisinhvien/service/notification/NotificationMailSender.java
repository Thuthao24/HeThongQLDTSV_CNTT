package com.example.quanlydetaisinhvien.service.notification;

public interface NotificationMailSender {
    void send(Message message);
    record Message(Long id, int attempt, Long studentId, Long noticeId, String to, String subject, String body) {}
    /** Transport can prove SMTP has not accepted the message. Other errors are ambiguous. */
    class DefinitelyNotSentException extends RuntimeException {
        public DefinitelyNotSentException(String message) { super(message); }
    }
}
