package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.service.mail.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResultMailSenderTests {
    private final ResultMailSender.Message message = new ResultMailSender.Message(1L, 1,
            "recipient@example.test", "Snapshot subject", "Snapshot body");

    @SuppressWarnings("unchecked")
    private ObjectProvider<JavaMailSender> provider(JavaMailSender sender) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        return provider;
    }

    @Test
    void disabledTransportCannotContactSmtpEvenWithConfigurationPresent() {
        var mail = mock(JavaMailSender.class);
        var provider = provider(mail);
        var adapter = new SmtpResultMailSender(provider, "from@example.test", "smtp.example.test", false);
        assertThatThrownBy(() -> adapter.send(message)).isInstanceOf(ResultMailSender.DefinitelyNotSentException.class);
        verifyNoInteractions(mail);
    }

    @Test
    void missingConfigurationFailsBeforeSending() {
        var mail = mock(JavaMailSender.class);
        var adapter = new SmtpResultMailSender(provider(mail), "", "", true);
        assertThatThrownBy(() -> adapter.send(message)).isInstanceOf(ResultMailSender.DefinitelyNotSentException.class);
        verifyNoInteractions(mail);
    }

    @Test
    void authenticationRejectionIsKnownNotSent() {
        var mail = mock(JavaMailSender.class);
        doThrow(new MailAuthenticationException("fake rejection")).when(mail).send(any(SimpleMailMessage.class));
        var adapter = new SmtpResultMailSender(provider(mail), "from@example.test", "smtp.example.test", true);
        assertThatThrownBy(() -> adapter.send(message)).isInstanceOf(ResultMailSender.DefinitelyNotSentException.class);
    }

    @Test
    void ambiguousSmtpFailureIsNotClassifiedAsSafeToRetry() {
        var mail = mock(JavaMailSender.class);
        doThrow(new MailSendException("fake lost ACK")).when(mail).send(any(SimpleMailMessage.class));
        var adapter = new SmtpResultMailSender(provider(mail), "from@example.test", "smtp.example.test", true);
        assertThatThrownBy(() -> adapter.send(message)).isInstanceOf(MailSendException.class)
                .isNotInstanceOf(ResultMailSender.DefinitelyNotSentException.class);
    }

    @Test
    void addressValidationRejectsMissingInvalidAndHeaderInjection() {
        assertThat(MailAddress.validOrNull(null)).isNull();
        assertThat(MailAddress.validOrNull("not an email")).isNull();
        assertThat(MailAddress.validOrNull("a@example.test\r\nBcc: b@example.test")).isNull();
        assertThat(MailAddress.validOrNull("a@example.test,b@example.test")).isNull();
        assertThat(MailAddress.validOrNull(" a@example.test ")).isEqualTo("a@example.test");
    }
}
