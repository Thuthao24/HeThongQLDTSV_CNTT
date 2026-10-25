package com.example.quanlydetaisinhvien.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Reject a test-only launch before any datasource, worker or web server is created. */
@Configuration(proxyBeanMethods = false)
@Profile("email-local-test")
public class EmailTestProfileGuard {
    @Bean
    static BeanFactoryPostProcessor requireExplicitTestFixture() {
        return factory -> {
            if (!factory.containsBeanDefinition("emailLocalTestDataSource")) {
                throw new IllegalStateException(
                        "Profile email-local-test chỉ dành cho test source với fixture được import rõ ràng; không được bật trong JAR production.");
            }
        };
    }
}
