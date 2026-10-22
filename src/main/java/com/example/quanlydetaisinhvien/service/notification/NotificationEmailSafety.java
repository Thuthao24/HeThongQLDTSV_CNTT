package com.example.quanlydetaisinhvien.service.notification;

import com.example.quanlydetaisinhvien.config.NotificationEmailSettings;
import com.example.quanlydetaisinhvien.service.mail.MailAddress;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.util.*;

@Component
public class NotificationEmailSafety {
    private final NotificationEmailSettings settings;
    private final Environment environment;
    public NotificationEmailSafety(NotificationEmailSettings settings, Environment environment) {
        this.settings = settings; this.environment = environment;
    }
    public boolean deliveryAllowed() {
        if (environment.matchesProfiles("notification-mail-test") && (settings.mode()!=NotificationEmailSettings.Mode.ALLOWLIST
                || settings.studentId()<=0 || settings.noticeId()<=0 || settings.batchSize()!=1)) return false;
        return settings.outboxEnabled() && settings.deliveryEnabled() && !environment.matchesProfiles("demo")
                && (settings.mode() == NotificationEmailSettings.Mode.SYSTEM || !addresses().isEmpty());
    }
    public Set<String> addresses() {
        var result = new HashSet<String>();
        for (String value : settings.allowlist()) {
            String valid = MailAddress.validOrNull(value);
            if (valid != null) result.add(valid.toLowerCase(Locale.ROOT));
        }
        return Set.copyOf(result);
    }
    public boolean permitted(Long studentId, Long noticeId, String address) {
        String valid = MailAddress.validOrNull(address);
        return valid != null && (settings.studentId() == 0 || Objects.equals(studentId, settings.studentId()))
                && (settings.noticeId() == 0 || Objects.equals(noticeId, settings.noticeId()))
                && (settings.minNoticeId() == 0 || (noticeId != null && noticeId >= settings.minNoticeId()))
                && (settings.mode() == NotificationEmailSettings.Mode.SYSTEM || addresses().contains(valid.toLowerCase(Locale.ROOT)));
    }
    public String detailLink(Long id) {
        String configured = settings.baseUrl().strip();
        if (configured.isEmpty()) return "/thongbao/" + id;
        URI uri;
        try { uri = URI.create(configured); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Base URL email không hợp lệ."); }
        if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("Base URL email phải là HTTP(S) tin cậy, không có credentials/query/fragment.");
        }
        return configured.replaceAll("/+$", "") + "/thongbao/" + id;
    }
}
