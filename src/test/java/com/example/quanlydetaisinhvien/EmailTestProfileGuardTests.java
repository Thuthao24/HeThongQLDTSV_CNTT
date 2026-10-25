package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.config.EmailLocalTestDatabaseConfig;
import com.example.quanlydetaisinhvien.config.EmailTestProfileGuard;
import com.example.quanlydetaisinhvien.service.mail.KetquaEmailSafety;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.Assertions.*;

class EmailTestProfileGuardTests {
    static final AtomicBoolean datasourceCreated = new AtomicBoolean();
    @TestConfiguration(proxyBeanMethods=false)
    static class ForbiddenDatasource {
        @Bean HikariDataSource dataSource() {
            datasourceCreated.set(true);
            throw new AssertionError("Datasource must not be instantiated for an unsupported profile");
        }
    }
    ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withUserConfiguration(EmailTestProfileGuard.class);
    }
    @Test void unsupportedLocalTestProfileFailsBeforeAnyDatasourceInitialization() {
        datasourceCreated.set(false);
        runner().withInitializer(context -> context.getEnvironment().setActiveProfiles("email-local-test"))
                .withUserConfiguration(ForbiddenDatasource.class).run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessageContaining("không được bật trong JAR production");
                    assertThat(datasourceCreated).isFalse();
                });
    }
    @Test void normalProductionConfigurationDoesNotRequireAnyTestFixture() {
        runner().run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(EmailTestProfileGuard.class);
            assertThat(context).doesNotHaveBean("emailLocalTestDataSource");
        });
    }
    @Test void explicitlyImportedFixtureRemainsLazyAndAvailableOnlyToTests() {
        runner().withInitializer(context -> context.getEnvironment().setActiveProfiles("email-local-test"))
                .withUserConfiguration(EmailLocalTestDatabaseConfig.class, KetquaEmailSafety.class)
                .withPropertyValues(
                        "spring.datasource.url=jdbc:mysql://127.0.0.1:1/quanlydetaisinhvien_demo",
                        "spring.datasource.username=fixture-only", "spring.datasource.password=fixture-only",
                        "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.generate-ddl=false",
                        "spring.sql.init.mode=never", "spring.mail.test-connection=false",
                        "app.ketqua.email.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(HikariDataSource.class).getHikariPoolMXBean()).isNull();
                    assertThat(context.getBean(KetquaEmailSafety.class).deliveryAllowed()).isFalse();
                });
    }
}
