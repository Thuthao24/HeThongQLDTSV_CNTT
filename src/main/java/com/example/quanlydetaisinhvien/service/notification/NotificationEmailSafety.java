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
    /** Permanent business policy: Module 15 is website-only; legacy flags cannot re-enable mail. */
    public boolean deliveryAllowed() { return false; }
    public Set<String> addresses() {
        var result = new HashSet<String>();
        for (String value : settings.allowlist()) {
            String valid = MailAddress.validOrNull(value);
            if (valid != null) result.add(valid.toLowerCase(Locale.ROOT));
        }
        return Set.copyOf(result);
    }
    public boolean permitted(Long studentId, Long noticeId, String address) { return false; }
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
