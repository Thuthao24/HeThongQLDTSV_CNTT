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
    /** Retained for binary/source compatibility only. Ordinary notices are website-only. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(Thongbao created, String author) {
        // Intentionally no schema access, rows or snapshot mutation, even if old flags are enabled.
    }
}