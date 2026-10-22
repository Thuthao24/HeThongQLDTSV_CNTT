package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.config.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.security.NotificationEmailPermissions;
import com.example.quanlydetaisinhvien.service.notification.*;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationEmailSafetyTests {
    NotificationEmailSettings settings(boolean outbox,boolean delivery,boolean scheduling,NotificationEmailSettings.Mode mode,
            Set<String> allowlist,long student,long notice,int batch,String base) {
        return new NotificationEmailSettings(outbox,delivery,scheduling,mode,allowlist,student,notice,0L,batch,0,1000,900,base,10000);
    }
    NotificationEmailSettings enabled() { return settings(true,true,false,NotificationEmailSettings.Mode.ALLOWLIST,Set.of("one@example.invalid"),0,0,1,""); }
    NotificationMailSender.Message message=new NotificationMailSender.Message(1L,1,10L,20L,"one@example.invalid","Tiếng Việt <script>","Nội dung <b>chưa escape</b>");

    @Test void applicationAndDemoPropertiesKeepAllDeliverySwitchesOffByDefault() throws Exception {
        var normal=PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));
        var demo=PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-demo.properties"));
        for(String flag:List.of("outbox-enabled","delivery-enabled","schedule-enabled")) {
            assertThat(normal.getProperty("app.thongbao.email."+flag)).endsWith(":false}");
            assertThat(demo.getProperty("app.thongbao.email."+flag)).isEqualTo("false");
        }
        assertThat(normal.getProperty("app.thongbao.email.smtp.password")).isEqualTo("${THONGBAO_SMTP_PASSWORD:}");
        assertThat(normal.getProperty("app.ketqua.email.enabled")).isEqualTo("${KETQUA_EMAIL_ENABLED:false}");
    }

    @Test void demoRuntimeGuardPreventsDeliveryEvenWithExplicitTrueOverridesAndSystemMode() {
        var env=new MockEnvironment(); env.setActiveProfiles("demo");
        var config=settings(true,true,true,NotificationEmailSettings.Mode.SYSTEM,Set.of(),0,0,100,"");
        var safe=new NotificationEmailSafety(config,env);
        var smtp=mock(JavaMailSender.class);
        assertThatThrownBy(()->new SmtpNotificationMailSender(safe,smtp,"smtp.invalid","sender@example.invalid").send(message))
                .isInstanceOf(NotificationMailSender.DefinitelyNotSentException.class);
        var worker=mock(NotificationEmailWorker.class);
        var scheduler=new NotificationEmailScheduler(worker,safe,config);
        scheduler.start(); assertThat(scheduler.isRunning()).isFalse();
        verifyNoInteractions(smtp,worker);
    }

    @Test void disabledOutboxDoesNotReadSchemaRecipientsOrQueueEvenWhenSchemaIsMissing() {
        var config=settings(false,false,false,NotificationEmailSettings.Mode.ALLOWLIST,Set.of(),0,0,1,"");
        var schema=mock(NotificationEmailSchema.class); var permissions=mock(NotificationEmailPermissions.class);
        var safety=mock(NotificationEmailSafety.class); var emails=mock(EmailthongbaoRepository.class);
        var recipients=mock(NotificationRecipientRepository.class);
        var em=mock(jakarta.persistence.EntityManager.class);
        new NotificationEmailOutbox(config,schema,permissions,safety,emails,recipients,em).enqueue(new Thongbao(),"Chief");
        verifyNoInteractions(schema,permissions,safety,emails,recipients,em);
    }

    @Test void disabledDeliveryReturnsBeforeAnyQueueOrSmtpAccess() {
        var config=settings(true,false,false,NotificationEmailSettings.Mode.ALLOWLIST,Set.of("one@example.invalid"),0,0,1,"");
        var safe=new NotificationEmailSafety(config,new MockEnvironment());
        var queue=mock(NotificationEmailQueue.class); var sender=mock(NotificationMailSender.class); var schema=mock(NotificationEmailSchema.class);
        new NotificationEmailWorker(queue,sender,safe,schema,config).drain();
        verifyNoInteractions(queue,sender,schema);
    }

    @Test void activeRequestTransactionCannotRunTheWorkerEvenIfDeliveryIsConfigured() {
        var queue=mock(NotificationEmailQueue.class); var sender=mock(NotificationMailSender.class); var schema=mock(NotificationEmailSchema.class);
        var config=enabled(); var safe=new NotificationEmailSafety(config,new MockEnvironment());
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try { new NotificationEmailWorker(queue,sender,safe,schema,config).drain(); }
        finally { TransactionSynchronizationManager.setActualTransactionActive(false); }
        verifyNoInteractions(queue,sender,schema);
    }

    @Test void allowlistDefaultsToDenyAndNeverRedirectsRecipients() {
        var deny=new NotificationEmailSafety(settings(true,true,false,NotificationEmailSettings.Mode.ALLOWLIST,Set.of(),0,0,1,""),new MockEnvironment());
        assertThat(deny.deliveryAllowed()).isFalse();
        var safe=new NotificationEmailSafety(settings(true,true,false,NotificationEmailSettings.Mode.ALLOWLIST,
                Set.of("ONE@example.invalid","invalid","two@example.invalid\r\nBCC:x"),10,20,1,""),new MockEnvironment());
        assertThat(safe.permitted(10L,20L,"one@example.invalid")).isTrue();
        assertThat(safe.permitted(11L,20L,"one@example.invalid")).isFalse();
        assertThat(safe.permitted(10L,21L,"one@example.invalid")).isFalse();
        assertThat(safe.permitted(10L,20L,"two@example.invalid")).isFalse();
    }

    @Test void smtpUsesOneRecipientUtf8PlainTextAndNoPublicCcOrBcc() throws Exception {
        var config=enabled(); var safe=new NotificationEmailSafety(config,new MockEnvironment());
        var transport=mock(JavaMailSender.class); var mime=new MimeMessage(Session.getInstance(new Properties()));
        when(transport.createMimeMessage()).thenReturn(mime);
        new SmtpNotificationMailSender(safe,transport,"smtp.invalid","sender@example.invalid").send(message);
        mime.saveChanges();
        assertThat(mime.getRecipients(Message.RecipientType.TO)).hasSize(1);
        assertThat(mime.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("one@example.invalid");
        assertThat(mime.getRecipients(Message.RecipientType.CC)).isNull(); assertThat(mime.getRecipients(Message.RecipientType.BCC)).isNull();
        assertThat(mime.getSubject()).isEqualTo(message.subject()); assertThat(mime.getContent()).isEqualTo(message.body());
        assertThat(mime.getContentType()).containsIgnoringCase("text/plain").containsIgnoringCase("UTF-8");
        verify(transport).send(mime);
    }

    @ParameterizedTest @ValueSource(strings={"auth","ambiguous"})
    void transportDistinguishesPreAcceptanceAuthenticationRejectionFromAmbiguousSendFailure(String kind) {
        var transport=mock(JavaMailSender.class); var mime=new MimeMessage(Session.getInstance(new Properties()));
        when(transport.createMimeMessage()).thenReturn(mime);
        RuntimeException error=kind.equals("auth")?new MailAuthenticationException("private"):new MailSendException("private");
        doThrow(error).when(transport).send(mime);
        var sender=new SmtpNotificationMailSender(new NotificationEmailSafety(enabled(),new MockEnvironment()),transport,"smtp.invalid","sender@example.invalid");
        if(kind.equals("auth")) assertThatThrownBy(()->sender.send(message)).isInstanceOf(NotificationMailSender.DefinitelyNotSentException.class);
        else assertThatThrownBy(()->sender.send(message)).isSameAs(error);
    }

    @Test void missingTransportConfigurationAndOutOfScopeRecipientCannotContactSmtp() {
        var transport=mock(JavaMailSender.class); var safe=new NotificationEmailSafety(enabled(),new MockEnvironment());
        assertThatThrownBy(()->new SmtpNotificationMailSender(safe,transport,"","bad").send(message))
                .isInstanceOf(NotificationMailSender.DefinitelyNotSentException.class);
        var blocked=new NotificationMailSender.Message(1L,1,10L,20L,"other@example.invalid","Subject","Body");
        assertThatThrownBy(()->new SmtpNotificationMailSender(safe,transport,"smtp.invalid","sender@example.invalid").send(blocked))
                .isInstanceOf(NotificationMailSender.DefinitelyNotSentException.class);
        verifyNoInteractions(transport);
    }

    @Test void configuredUrlDoesNotDependOnARequestHostAndBlankBaseKeepsRelativeDetailPath() {
        assertThat(new NotificationEmailSafety(enabled(),new MockEnvironment()).detailLink(20L)).isEqualTo("/thongbao/20");
        var safe=new NotificationEmailSafety(settings(true,true,false,NotificationEmailSettings.Mode.ALLOWLIST,Set.of(),0,0,1,"https://hcmute.example.invalid/portal/"),new MockEnvironment());
        assertThat(safe.detailLink(20L)).isEqualTo("https://hcmute.example.invalid/portal/thongbao/20");
    }

    @ParameterizedTest @ValueSource(strings={"javascript:alert(1)","https://user:password@example.invalid","https://portal.invalid/?x=1","https://portal.invalid/#fragment","not-url"})
    void unsafeConfiguredBaseUrlsAreRejected(String url) {
        var safe=new NotificationEmailSafety(settings(true,false,false,NotificationEmailSettings.Mode.ALLOWLIST,Set.of(),0,0,1,url),new MockEnvironment());
        assertThatThrownBy(()->safe.detailLink(1L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void schedulerUsesADedicatedExecutorAndDoesNotEnableModule16GlobalScheduling() {
        assertThat(NotificationEmailScheduler.class.getAnnotations()).noneMatch(a->a.annotationType().getSimpleName().equals("EnableScheduling"));
        var worker=mock(NotificationEmailWorker.class);
        var config=settings(true,true,true,NotificationEmailSettings.Mode.SYSTEM,Set.of(),0,0,1,"");
        var scheduler=new NotificationEmailScheduler(worker,new NotificationEmailSafety(config,new MockEnvironment()),config);
        scheduler.start(); assertThat(scheduler.isRunning()).isTrue(); scheduler.stop(); assertThat(scheduler.isRunning()).isFalse();
        verifyNoInteractions(worker); // No immediate startup delivery.
    }

    @ParameterizedTest @ValueSource(strings={"system","student","notice","batch","correct"})
    void mailTestProfileRequiresAllowlistOneStudentOneNoticeAndSingleItemBatch(String variant) {
        var env=new MockEnvironment(); env.setActiveProfiles("notification-mail-test");
        var config=settings(true,true,true,variant.equals("system")?NotificationEmailSettings.Mode.SYSTEM:NotificationEmailSettings.Mode.ALLOWLIST,
                Set.of("one@example.invalid"),variant.equals("student")?0:10,variant.equals("notice")?0:20,variant.equals("batch")?10:1,"");
        assertThat(new NotificationEmailSafety(config,env).deliveryAllowed()).isEqualTo(variant.equals("correct"));
    }

    @Test void mailTestDatabaseGuardIsLazyAndRejectsOriginalDatabaseOrResultDelivery() throws Exception {
        var env=new MockEnvironment();
        env.getPropertySources().addLast(new org.springframework.core.env.PropertiesPropertySource("mail-test",
                PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-notification-mail-test.properties"))));
        env.withProperty("DEMO_DB_USERNAME","test-only").withProperty("DEMO_DB_PASSWORD","test-only");
        var config=new NotificationMailTestDatabaseConfig();
        try(var data=config.notificationMailTestDataSource(env)) {
            assertThat(data.getJdbcUrl()).endsWith("/quanlydetaisinhvien_demo"); assertThat(data.getHikariPoolMXBean()).isNull();
        }
        env.withProperty("spring.datasource.url","jdbc:mysql://127.0.0.1/quanlydetaisinhvien");
        assertThatThrownBy(()->config.notificationMailTestDataSource(env)).isInstanceOf(IllegalStateException.class);
        env.withProperty("spring.datasource.url","jdbc:mysql://127.0.0.1/quanlydetaisinhvien_demo");
        env.withProperty("app.ketqua.email.enabled","true");
        assertThatThrownBy(()->config.notificationMailTestDataSource(env)).hasMessageContaining("app.ketqua.email.enabled");
    }

    @Test void minNoticeIdRejectsOlderNoticesEvenWhenRecipientMatches() {
        var config = new NotificationEmailSettings(true, true, false, NotificationEmailSettings.Mode.ALLOWLIST,
                Set.of("one@example.invalid"), 10L, 0L, 6L, 1, 0, 1000, 900, "", 10000);
        var safe = new NotificationEmailSafety(config, new MockEnvironment());
        assertThat(safe.permitted(10L, 5L, "one@example.invalid")).isFalse();
        assertThat(safe.permitted(10L, 6L, "one@example.invalid")).isTrue();
        assertThat(safe.permitted(10L, 7L, "one@example.invalid")).isTrue();
    }

    @Test void demoAutoProfileAllowsDeliveryWithValidAllowlistAndProtectsDatabase() {
        var env = new MockEnvironment(); env.setActiveProfiles("demo-auto");
        var config = new NotificationEmailSettings(true, true, true, NotificationEmailSettings.Mode.ALLOWLIST,
                Set.of("hnghi.dong01@gmail.com"), 1L, 0L, 6L, 1, 0, 1000, 900, "", 10000);
        var safe = new NotificationEmailSafety(config, env);
        assertThat(safe.deliveryAllowed()).isTrue();
    }
}
