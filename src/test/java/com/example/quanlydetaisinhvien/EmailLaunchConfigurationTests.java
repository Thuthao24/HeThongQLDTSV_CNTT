package com.example.quanlydetaisinhvien;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class EmailLaunchConfigurationTests {
    @Test void mailjetIsConfiguredAsExclusiveProviderAndSmtpIsRemoved() throws Exception {
        var properties=PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));
        assertThat(properties.getProperty("app.mailjet.api-key")).isEqualTo("${MAILJET_API_KEY:}");
        assertThat(properties.getProperty("app.mailjet.secret-key")).isEqualTo("${MAILJET_SECRET_KEY:}");
        assertThat(properties.getProperty("app.mailjet.from-email")).isEqualTo("${MAILJET_FROM_EMAIL:}");
        assertThat(properties.getProperty("app.mailjet.from-name")).isEqualTo("${MAILJET_FROM_NAME:HCMUTE - Khoa CNTT}");
        assertThat(properties.getProperty("app.email.base-url")).isEqualTo("${APP_PUBLIC_URL:https://hethongqldtsv-cntt.onrender.com}");

        // Xác nhận không còn bất kỳ cấu hình Gmail SMTP nào trong application.properties
        assertThat(properties.getProperty("app.ketqua.email.provider")).isNull();
        assertThat(properties.getProperty("spring.mail.host")).isNull();
        assertThat(properties.getProperty("spring.mail.port")).isNull();
        assertThat(properties.getProperty("spring.mail.username")).isNull();
        assertThat(properties.getProperty("spring.mail.password")).isNull();
    }

    @Test void resultLocalProfileUsesTheDemoCredentialsProvidedByItsLaunchScript() throws Exception {
        var env=new MockEnvironment().withProperty("DEMO_DB_USERNAME","script-test-user")
                .withProperty("DEMO_DB_PASSWORD","script-test-password");
        env.getPropertySources().addLast(new PropertiesPropertySource("local-result",
                PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-email-local-test.properties"))));
        assertThat("script-test-user".equals(env.getProperty("spring.datasource.username"))).isTrue();
        assertThat("script-test-password".equals(env.getProperty("spring.datasource.password"))).isTrue();
    }
}
