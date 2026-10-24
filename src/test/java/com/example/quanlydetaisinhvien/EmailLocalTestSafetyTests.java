package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.config.*;
import com.example.quanlydetaisinhvien.service.mail.KetquaEmailSafety;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class EmailLocalTestSafetyTests {
    MockEnvironment environment() throws Exception {
        var env=new MockEnvironment(); env.setActiveProfiles("email-local-test");
        env.withProperty("DEMO_DB_USERNAME","test-only").withProperty("DEMO_DB_PASSWORD","test-only");
        env.getPropertySources().addLast(new PropertiesPropertySource("local-profile",
                PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-email-local-test.properties"))));
        env.getPropertySources().addLast(new PropertiesPropertySource("base",
                PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"))));
        return env;
    }
    HikariDataSource source(MockEnvironment env) {
        return new EmailLocalTestDatabaseConfig().emailLocalTestDataSource(env,new KetquaEmailSafety(env));
    }
    void resultScope(MockEnvironment env) {
        env.withProperty("KETQUA_EMAIL_ENABLED","true").withProperty("KETQUA_EMAIL_OUTBOX_ID","77")
                .withProperty("KETQUA_EMAIL_ALLOWLIST","approved@example.invalid");
    }

    @Test void newProfileDefaultsToNoDeliveryAndDoesNotOpenADatabaseConnection() throws Exception {
        var env=environment();
        assertThat(new KetquaEmailSafety(env).deliveryAllowed()).isFalse();
        try(var data=source(env)) {
            assertThat(data.getJdbcUrl()).endsWith("/quanlydetaisinhvien_demo");
            assertThat(data.getHikariPoolMXBean()).isNull();
        }
    }


    @Test void exactResultScopeWorksAndProfileCannotDisableItsScopeGuard() throws Exception {
        var env=environment(); resultScope(env); env.withProperty("app.ketqua.email.test-mode","false");
        var safe=new KetquaEmailSafety(env);
        assertThat(safe.scopedTest()).isTrue(); assertThat(safe.deliveryAllowed()).isTrue();
        assertThat(safe.permitted(77L,"approved@example.invalid")).isTrue();
        assertThat(safe.permitted(76L,"approved@example.invalid")).isFalse();
        assertThat(safe.permitted(77L,"other@example.invalid")).isFalse();
        try(var data=source(env)) { assertThat(data.getHikariPoolMXBean()).isNull(); }
    }
    @ParameterizedTest @ValueSource(strings={"outbox","addresses"})
    void resultScopeMustHaveAnExactRowAndOneAddress(String missing) throws Exception {
        var env=environment(); resultScope(env);
        env.withProperty(missing.equals("outbox")?"KETQUA_EMAIL_OUTBOX_ID":"KETQUA_EMAIL_ALLOWLIST",missing.equals("outbox")?"0":"");
        assertThat(new KetquaEmailSafety(env).deliveryAllowed()).isFalse();
        assertThatThrownBy(()->source(env)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest @ValueSource(strings={"jdbc:mysql://127.0.0.1/quanlydetaisinhvien", "jdbc:mysql://127.0.0.1/quanlydetaisinhvien_demo?x=1", "jdbc:h2:mem:forbidden"})
    void localTestGuardRejectsTheOriginalOrAmbiguousDatabase(String url) throws Exception {
        var env=environment(); env.withProperty("spring.datasource.url",url);
        assertThatThrownBy(()->source(env)).isInstanceOf(IllegalStateException.class);
    }
    @ParameterizedTest @ValueSource(strings={"spring.jpa.hibernate.ddl-auto", "spring.jpa.generate-ddl", "spring.sql.init.mode", "spring.mail.test-connection"})
    void unsafeInitializationOverridesFailBeforeOpeningAConnection(String property) throws Exception {
        var env=environment(); env.withProperty(property,"unsafe");
        assertThatThrownBy(()->source(env)).isInstanceOf(IllegalStateException.class).hasMessageContaining(property);
    }
    @ParameterizedTest @ValueSource(strings={"demo","demo-auto","notification-mail-test"})
    void oldDemoProfilesRemainUnableToSendResultMail(String profile) throws Exception {
        var env=environment(); resultScope(env); env.setActiveProfiles(profile);
        assertThat(new KetquaEmailSafety(env).deliveryAllowed()).isFalse();
    }
}
