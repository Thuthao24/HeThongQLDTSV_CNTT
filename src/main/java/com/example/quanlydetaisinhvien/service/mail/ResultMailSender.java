package com.example.quanlydetaisinhvien.service.mail;

public interface ResultMailSender {
    void send(Message message);
    record Message(Long id, int attempt, String to, String subject, String body) {}

    /** Only use when the transport can prove that SMTP has NOT accepted this message. */
    class DefinitelyNotSentException extends RuntimeException {
        public DefinitelyNotSentException(String message) { super(message); }
    }
}
