package com.example.quanlydetaisinhvien.config;

import com.example.quanlydetaisinhvien.service.mail.KetquaEmailSafety;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import java.net.URI;

/** Test-only guard; imported explicitly, never packaged or auto-scanned in production. */
@TestConfiguration(proxyBeanMethods=false)
@Profile("email-local-test")
public class EmailLocalTestDatabaseConfig {
    @Bean public HikariDataSource emailLocalTestDataSource(Environment env,
            KetquaEmailSafety results) {
        if (env.matchesProfiles("demo", "demo-auto", "notification-mail-test"))
            throw new IllegalStateException("email-local-test không chạy chung với các profile demo hoặc notification-mail-test.");
        String url=env.getRequiredProperty("spring.datasource.url");
        try {
            if (!url.startsWith("jdbc:mysql://")) throw new IllegalArgumentException();
            URI uri=URI.create(url.substring("jdbc:".length()));
            if (uri.getHost()==null || !"/quanlydetaisinhvien_demo".equals(uri.getRawPath())
                    || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null)
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("email-local-test chỉ cho phép database quanlydetaisinhvien_demo, không có query hoặc credentials trong URL.");
        }
        require(env,"spring.jpa.hibernate.ddl-auto","none");
        require(env,"spring.jpa.generate-ddl","false");
        require(env,"spring.sql.init.mode","never");
        require(env,"spring.mail.test-connection","false");
        boolean resultDelivery=env.getProperty("app.ketqua.email.enabled",Boolean.class,false)
                || env.getProperty("KETQUA_EMAIL_ENABLED",Boolean.class,false);
        if (resultDelivery && !results.deliveryAllowed())
            throw new IllegalStateException("Gửi thử kết quả cần một outbox ID và ALLOWLIST đúng một địa chỉ.");
        String username=env.getRequiredProperty("spring.datasource.username");
        String password=env.getRequiredProperty("spring.datasource.password");
        if (username.isBlank() || password.isBlank())
            throw new IllegalStateException("email-local-test cần DEMO_DB_USERNAME và DEMO_DB_PASSWORD.");
        var source=new HikariDataSource();
        source.setJdbcUrl(url); source.setUsername(username); source.setPassword(password);
        source.setDriverClassName("com.mysql.cj.jdbc.Driver");
        return source;
    }
    private static void require(Environment env,String key,String expected) {
        if (!expected.equals(env.getProperty(key))) throw new IllegalStateException("email-local-test yêu cầu "+key+"="+expected);
    }
}
