package com.example.quanlydetaisinhvien.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

/** Reuses the existing verified demo DB guard, without changing its rules or Module 16. */
@Configuration(proxyBeanMethods=false)
@Profile("notification-mail-test & !demo")
public class NotificationMailTestDatabaseConfig {
    @Bean public HikariDataSource notificationMailTestDataSource(Environment environment) {
        return new DemoDatabaseConfig().dataSource(environment);
    }
}
