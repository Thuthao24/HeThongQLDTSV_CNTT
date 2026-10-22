package com.example.quanlydetaisinhvien.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.net.URI;

/** Fail before opening a connection if higher-priority settings make the demo unsafe. */
@Configuration(proxyBeanMethods = false)
@Profile({"demo", "demo-auto"})
public class DemoDatabaseConfig {
    @Bean
    public HikariDataSource dataSource(Environment environment) {
        String url = environment.getRequiredProperty("spring.datasource.url");
        try {
            if (!url.startsWith("jdbc:mysql://")) throw new IllegalArgumentException();
            URI uri = URI.create(url.substring("jdbc:".length()));
            if (uri.getHost() == null || !"/quanlydetaisinhvien_demo".equals(uri.getRawPath())
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Profile demo chỉ cho phép URL MySQL tới quanlydetaisinhvien_demo, không có query.");
        }
        require(environment, "spring.jpa.hibernate.ddl-auto", "none");
        require(environment, "spring.jpa.generate-ddl", "false");
        require(environment, "spring.sql.init.mode", "never");
        require(environment, "app.ketqua.email.enabled", "false");
        require(environment, "app.ketqua.email.scheduling-enabled", "false");
        require(environment, "spring.mail.test-connection", "false");
        String username = environment.getRequiredProperty("spring.datasource.username");
        String password = environment.getRequiredProperty("spring.datasource.password");
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Profile demo cần tài khoản database riêng qua DEMO_DB_USERNAME/DEMO_DB_PASSWORD.");
        }
        // Default constructor is lazy: validation above does not open a MySQL connection.
        HikariDataSource source = new HikariDataSource();
        source.setJdbcUrl(url);
        source.setUsername(username);
        source.setPassword(password);
        source.setDriverClassName("com.mysql.cj.jdbc.Driver");
        return source;
    }

    private static void require(Environment environment, String key, String expected) {
        if (!expected.equals(environment.getProperty(key))) {
            throw new IllegalStateException("Profile demo yêu cầu " + key + "=" + expected);
        }
    }
}
