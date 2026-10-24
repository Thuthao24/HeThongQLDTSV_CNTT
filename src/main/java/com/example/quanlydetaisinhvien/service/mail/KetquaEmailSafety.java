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
    public long outboxId() { return environment.getProperty("app.ketqua.email.outbox-id",Long.class,0L); }
    public Set<String> addresses() {
        var addresses=new HashSet<String>();
        for (String value:environment.getProperty("app.ketqua.email.allowlist","").split(",")) {
            String valid=MailAddress.validOrNull(value.strip());
            if (valid!=null) addresses.add(valid.toLowerCase(Locale.ROOT));
        }
        return Set.copyOf(addresses);
    }
    public boolean deliveryAllowed() {
        return environment.getProperty("app.ketqua.email.enabled",Boolean.class,false)
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
