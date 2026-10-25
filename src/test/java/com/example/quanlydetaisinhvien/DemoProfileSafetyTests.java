package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.config.DemoDatabaseConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

class DemoProfileSafetyTests {
    MockEnvironment environment() throws IOException {
        var env = new MockEnvironment();
        env.getPropertySources().addLast(new PropertiesPropertySource("demo",
                PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-demo.properties"))));
        return env.withProperty("DEMO_DB_USERNAME", "test-only")
                .withProperty("DEMO_DB_PASSWORD", "test-only");
    }

    @Test void demoIsLazyAndIgnoresNormalEmailSwitch() throws Exception {
        var env = environment().withProperty("KETQUA_EMAIL_ENABLED", "true");
        try (var source = new DemoDatabaseConfig().dataSource(env)) {
            assertThat(source.getJdbcUrl()).isEqualTo("jdbc:mysql://127.0.0.1:3306/quanlydetaisinhvien_demo");
            assertThat(source.getHikariPoolMXBean()).isNull(); // No connection/pool has been started.
            assertThat(env.getProperty("app.ketqua.email.enabled")).isEqualTo("false");
            assertThat(env.getProperty("server.port")).isEqualTo("8081");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"jdbc:mysql://127.0.0.1:3306/quanlydetaisinhvien",
            "jdbc:mysql://127.0.0.1:3306/quanlydetaisinhvien_demo_other",
            "jdbc:mysql://127.0.0.1:3306/quanlydetaisinhvien_demo?sessionVariables=x",
            "jdbc:h2:mem:wrong", "jdbc:mysql://user@127.0.0.1/quanlydetaisinhvien_demo"})
    void rejectsUnsafeTargetsBeforeConnecting(String url) throws Exception {
        var env = environment().withProperty("spring.datasource.url", url);
        assertThatThrownBy(() -> new DemoDatabaseConfig().dataSource(env)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"app.ketqua.email.enabled", "app.ketqua.email.scheduling-enabled",
            "spring.mail.test-connection", "spring.jpa.generate-ddl"})
    void rejectsUnsafeBooleanOverrides(String key) throws Exception {
        var env = environment().withProperty(key, "true");
        assertThatThrownBy(() -> new DemoDatabaseConfig().dataSource(env)).hasMessageContaining(key);
    }

    @Test void rejectsAutomaticSchemaOrDataChanges() throws Exception {
        var ddl = environment().withProperty("spring.jpa.hibernate.ddl-auto", "update");
        var init = environment().withProperty("spring.sql.init.mode", "always");
        assertThatThrownBy(() -> new DemoDatabaseConfig().dataSource(ddl)).hasMessageContaining("ddl-auto");
        assertThatThrownBy(() -> new DemoDatabaseConfig().dataSource(init)).hasMessageContaining("init.mode");
    }

    @Test void requiresSeparateDatabaseCredentials() throws Exception {
        var env = environment().withProperty("DEMO_DB_PASSWORD", "");
        assertThatThrownBy(() -> new DemoDatabaseConfig().dataSource(env)).hasMessageContaining("DEMO_DB_PASSWORD");
    }
}
