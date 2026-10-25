package com.example.quanlydetaisinhvien.service.mail;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.util.*;

/** Exact outbox selection for local Mailjet tests; never redirects a student's snapshot address. */
@Component
public class KetquaEmailSafety {
    private final Environment environment;
    public KetquaEmailSafety(Environment environment) { this.environment=environment; }

    public boolean scopedTest() {
        return environment.matchesProfiles("email-local-test")
                || environment.getProperty("app.ketqua.email.test-mode",Boolean.class,false);
    }

    public long outboxId() {
        Long id = environment.getProperty("app.ketqua.email.outbox-id", Long.class);
        if (id != null) return id;
        return environment.getProperty("KETQUA_EMAIL_OUTBOX_ID", Long.class, 0L);
    }

    public Set<String> addresses() {
        String allowlist = environment.getProperty("app.ketqua.email.allowlist", "");
        if (allowlist == null || allowlist.isEmpty()) {
            allowlist = environment.getProperty("KETQUA_EMAIL_ALLOWLIST", "");
        }
        var addresses = new HashSet<String>();
        for (String value : allowlist.split(",")) {
            String valid = MailAddress.validOrNull(value.strip());
            if (valid != null) addresses.add(valid.toLowerCase(Locale.ROOT));
        }
        return Set.copyOf(addresses);
    }

    public boolean deliveryAllowed() {
        boolean enabled = environment.getProperty("app.ketqua.email.enabled", Boolean.class, false)
                || environment.getProperty("KETQUA_EMAIL_ENABLED", Boolean.class, false);
        return enabled
                && !environment.matchesProfiles("demo", "demo-auto", "notification-mail-test")
                && (!scopedTest() || (outboxId()>0 && addresses().size()==1));
    }

    public boolean permitsId(Long id) {
        return deliveryAllowed() && (!scopedTest() || Objects.equals(id,outboxId()));
    }

    public boolean permitted(Long id,String address) {
        String valid=MailAddress.validOrNull(address);
        if (!permitsId(id) || valid == null) return false;
        return !scopedTest() || addresses().contains(valid.toLowerCase(Locale.ROOT));
    }
}
