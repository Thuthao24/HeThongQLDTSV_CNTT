package com.example.quanlydetaisinhvien;
import com.example.quanlydetaisinhvien.config.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.security.NotificationEmailPermissions;
import com.example.quanlydetaisinhvien.service.notification.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class NotificationEmailSafetyTests {
    @ParameterizedTest @ValueSource(strings={"default","demo","demo-auto","email-local-test","notification-mail-test","production"})
    void noProfileModeOrOldFlagCombinationCanReenableOrdinaryNoticeEmails(String profile){
        for(var mode:NotificationEmailSettings.Mode.values())for(int flags=0;flags<8;flags++){
            var config=new NotificationEmailSettings((flags&1)!=0,(flags&2)!=0,(flags&4)!=0,mode,Set.of("one@example.invalid"),1,1,0,1,0,1000,900,"",10000);
            var env=new MockEnvironment();env.setActiveProfiles(profile);var safe=new NotificationEmailSafety(config,env);
            assertThat(safe.deliveryAllowed()).isFalse();assertThat(safe.permitted(1L,1L,"one@example.invalid")).isFalse();
            var queue=mock(NotificationEmailQueue.class);var transport=mock(NotificationMailSender.class);var schema=mock(NotificationEmailSchema.class);
            var worker=new NotificationEmailWorker(queue,transport,safe,schema,config);worker.drain();
            var scheduler=new NotificationEmailScheduler(worker,safe,config);scheduler.start();assertThat(scheduler.isRunning()).isFalse();scheduler.stop();
            verifyNoInteractions(queue,transport,schema);
        }
    }
    @Test void directLegacySenderCannotReachTransport(){
        var transport=mock(JavaMailSender.class);
        var settings=mock(NotificationEmailSettings.class);var safe=new NotificationEmailSafety(settings,new MockEnvironment());
        var message=new NotificationMailSender.Message(1L,1,1L,1L,"one@example.invalid","Subject","Body");
        assertThatThrownBy(()->new SmtpNotificationMailSender(safe,transport,"smtp.example.invalid","sender@example.invalid").send(message)).isInstanceOf(NotificationMailSender.DefinitelyNotSentException.class);
        verifyNoInteractions(transport);
    }
    @Test void legacyEnqueueIsANoopWithoutSchemaOrRecipientAccess(){
        var config=mock(NotificationEmailSettings.class);var schema=mock(NotificationEmailSchema.class);var permissions=mock(NotificationEmailPermissions.class);
        var safe=mock(NotificationEmailSafety.class);var rows=mock(EmailthongbaoRepository.class);var recipients=mock(NotificationRecipientRepository.class);var em=mock(jakarta.persistence.EntityManager.class);
        new NotificationEmailOutbox(config,schema,permissions,safe,rows,recipients,em).enqueue(new Thongbao(),"Chief");
        verifyNoInteractions(config,schema,permissions,safe,rows,recipients,em);
    }
}
