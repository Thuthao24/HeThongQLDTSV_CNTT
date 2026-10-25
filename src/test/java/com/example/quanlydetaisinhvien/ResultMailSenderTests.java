package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.service.mail.*;
import com.example.quanlydetaisinhvien.service.mailjet.MailjetClient;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResultMailSenderTests {
    private final ResultMailSender.Message message=new ResultMailSender.Message(1L,1,"recipient@example.test","Tiêu đề","Nội dung snapshot");
    KetquaEmailSafety safety(boolean enabled) {
        return new KetquaEmailSafety(new MockEnvironment().withProperty("app.ketqua.email.enabled",Boolean.toString(enabled)));
    }
    @Test void scopedTransportRejectsAnotherOutboxOrAnotherRecipientBeforeContactingMailjet() {
        var env=new MockEnvironment().withProperty("app.ketqua.email.enabled","true")
                .withProperty("app.ketqua.email.test-mode","true").withProperty("app.ketqua.email.outbox-id","1")
                .withProperty("app.ketqua.email.allowlist",message.to());
        var client=mock(MailjetClient.class); var sender=new MailjetResultMailSender(client,new KetquaEmailSafety(env));
        assertThatThrownBy(()->sender.send(new ResultMailSender.Message(2L,1,message.to(),message.subject(),message.body())))
                .isInstanceOf(ResultMailSender.DefinitelyNotSentException.class);
        assertThatThrownBy(()->sender.send(new ResultMailSender.Message(1L,1,"other@example.test",message.subject(),message.body())))
                .isInstanceOf(ResultMailSender.DefinitelyNotSentException.class);
        verifyNoInteractions(client);
    }
    @Test void resultTransportPreservesIndividualSnapshot() {
        var client=mock(MailjetClient.class);
        new MailjetResultMailSender(client,safety(true)).send(message);
        verify(client).send(MailjetClient.Flow.RESULT,1L,1,message.to(),message.subject(),message.body());
    }
    @Test void disabledTransportCannotContactMailjetEvenWithConfigurationPresent() {
        var client=mock(MailjetClient.class);
        assertThatThrownBy(()->new MailjetResultMailSender(client,safety(false)).send(message))
                .isInstanceOf(ResultMailSender.DefinitelyNotSentException.class);
        verifyNoInteractions(client);
    }
    @Test void authenticationOrValidationRejectionIsKnownNotSent() {
        var client=mock(MailjetClient.class);
        doThrow(new MailjetClient.Rejected("Mailjet rejected")).when(client).send(any(),any(),anyInt(),anyString(),anyString(),anyString());
        assertThatThrownBy(()->new MailjetResultMailSender(client,safety(true)).send(message))
                .isInstanceOf(ResultMailSender.DefinitelyNotSentException.class);
    }
    @Test void ambiguousApiFailureIsNotClassifiedAsSafeToRetry() {
        var client=mock(MailjetClient.class);
        doThrow(new MailjetClient.Uncertain("Unknown outcome")).when(client).send(any(),any(),anyInt(),anyString(),anyString(),anyString());
        assertThatThrownBy(()->new MailjetResultMailSender(client,safety(true)).send(message))
                .isInstanceOf(MailjetClient.Uncertain.class).isNotInstanceOf(ResultMailSender.DefinitelyNotSentException.class);
    }
    @Test void addressValidationRejectsMissingInvalidAndHeaderInjection() {
        assertThat(MailAddress.validOrNull(null)).isNull();
        assertThat(MailAddress.validOrNull("not an email")).isNull();
        assertThat(MailAddress.validOrNull("a@example.test\r\nBcc: b@example.test")).isNull();
        assertThat(MailAddress.validOrNull("a@example.test,b@example.test")).isNull();
        assertThat(MailAddress.validOrNull(" a@example.test ")).isEqualTo("a@example.test");
    }

    @Test void productionAlwaysUsesSnapshotEvenWhenObsoleteTestFlagsRemainInEnvironment() {
        var env=new MockEnvironment().withProperty("app.ketqua.email.enabled","true")
                .withProperty("app.ketqua.email.test-mode","false").withProperty("app.ketqua.email.outbox-id","1")
                .withProperty("app.ketqua.email.allowlist","fixture@example.invalid")
                .withProperty("app.ketqua.email.recipient-override","fixture@example.invalid");
        var client=mock(MailjetClient.class);
        var snapshot=new ResultMailSender.Message(42L,1,"student@example.test","Subject","Private snapshot");
        new MailjetResultMailSender(client,new KetquaEmailSafety(env)).send(snapshot);
        verify(client).send(MailjetClient.Flow.RESULT,42L,1,snapshot.to(),snapshot.subject(),snapshot.body());
    }
    @Test void retiredOverrideCannotBypassLocalSnapshotAllowlist() {
        var env=new MockEnvironment().withProperty("app.ketqua.email.enabled","true")
                .withProperty("app.ketqua.email.test-mode","true").withProperty("app.ketqua.email.outbox-id","1")
                .withProperty("app.ketqua.email.allowlist","fixture@example.invalid")
                .withProperty("app.ketqua.email.recipient-override","fixture@example.invalid");
        var client=mock(MailjetClient.class);
        assertThatThrownBy(()->new MailjetResultMailSender(client,new KetquaEmailSafety(env)).send(message))
                .isInstanceOf(ResultMailSender.DefinitelyNotSentException.class);
        verifyNoInteractions(client);
    }
}
