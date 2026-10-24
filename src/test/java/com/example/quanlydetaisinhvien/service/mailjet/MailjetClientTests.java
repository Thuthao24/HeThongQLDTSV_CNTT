package com.example.quanlydetaisinhvien.service.mailjet;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.boot.test.system.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import static org.assertj.core.api.Assertions.*;

@org.junit.jupiter.api.extension.ExtendWith(OutputCaptureExtension.class)
class MailjetClientTests {
    MockMailjetServer server;
    @BeforeEach void start() { server=new MockMailjetServer(); }
    @AfterEach void stop() { if(server!=null) server.close(); }
    MailjetClient.Accepted send() { return server.client(Duration.ofSeconds(2)).send(MailjetClient.Flow.RESULT,77L,1,"one@example.invalid","Tiếng Việt\r\nBcc: ignored","Nội dung <b>văn bản thuần</b>"); }

    @Test void usesOfficialHttpsEndpointAndOnePrivateUtf8Message(CapturedOutput output) {
        assertThat(MailjetClient.SEND_ENDPOINT.toString()).isEqualTo("https://api.mailjet.com/v3.1/send");
        assertThat(send().messageId()).isEqualTo("123456");
        assertThat(server.requests).hasSize(1);
        var request=server.requests.getFirst();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.contentType()).contains("application/json").contains("UTF-8");
        assertThat(request.authorization()).isEqualTo("Basic "+Base64.getEncoder().encodeToString("fake-public-key:fake-secret-key".getBytes(StandardCharsets.UTF_8)));
        var messages=request.body().path("Messages"); assertThat(messages.size()).isEqualTo(1);
        var message=messages.get(0);
        assertThat(message.path("To").size()).isEqualTo(1);
        assertThat(message.path("To").get(0).path("Email").asString()).isEqualTo("one@example.invalid");
        assertThat(message.has("Cc")).isFalse(); assertThat(message.has("Bcc")).isFalse(); assertThat(message.has("HTMLPart")).isFalse();
        assertThat(message.path("TextPart").asString()).contains("Nội dung <b>");
        assertThat(message.path("Subject").asString()).doesNotContain("\r","\n");
        assertThat(message.path("CustomID").asString()).isEqualTo("hcmute-result-77-1");
        assertThat(output.getAll()).contains("Mailjet accepted").doesNotContain("fake-secret-key","fake-public-key","one@example.invalid","Nội dung");
    }
    @ParameterizedTest @ValueSource(ints={400,401,403,404,422,429})
    void explicitRequestRejectionIsFailedWithoutAutomaticRetry(int status,CapturedOutput output) {
        server.status=status; server.reply="{\"ErrorMessage\":\"PRIVATE_PROVIDER_TEXT fake-secret-key\"}";
        assertThatThrownBy(this::send).isInstanceOf(MailjetClient.Rejected.class).hasMessageContaining("HTTP "+status)
                .hasMessageNotContaining("PRIVATE_PROVIDER_TEXT").hasMessageNotContaining("fake-secret-key");
        assertThat(server.requests).hasSize(1);
        assertThat(output.getAll()).doesNotContain("PRIVATE_PROVIDER_TEXT","fake-secret-key");
    }
    @ParameterizedTest @ValueSource(ints={408,500,502,503,504,302})
    void serverOrAmbiguousStatusNeverAutomaticallyRetries(int status) {
        server.status=status;
        assertThatThrownBy(this::send).isInstanceOf(MailjetClient.Uncertain.class);
        assertThat(server.requests).hasSize(1);
    }
    @ParameterizedTest @ValueSource(strings={"mj-0015","send-0008"})
    void http200WithPerMessageAuthenticationOrSenderErrorIsNotSent(String code) {
        server.reply="{\"Messages\":[{\"Status\":\"error\",\"Errors\":[{\"StatusCode\":403,\"ErrorCode\":\""+code+"\",\"ErrorMessage\":\"PRIVATE_PROVIDER_TEXT\"}]}]}";
        assertThatThrownBy(this::send).isInstanceOf(MailjetClient.Rejected.class).hasMessageNotContaining("PRIVATE_PROVIDER_TEXT");
        assertThat(server.requests).hasSize(1);
    }
    @ParameterizedTest @ValueSource(strings={"not JSON","{}","{\"Messages\":[]}","{\"Messages\":[{},{}]}",
            "{\"Messages\":[{\"Status\":\"success\"}]}",
            "{\"Messages\":[{\"Status\":\"error\",\"Errors\":[{\"StatusCode\":500}]}]}",
            "{\"Messages\":[{\"Status\":\"error\"}]}"})
    void incompleteOrAmbiguousResponseCannotBecomeSent(String reply) {
        server.reply=reply;
        assertThatThrownBy(this::send).isInstanceOf(MailjetClient.Uncertain.class);
        assertThat(server.requests).hasSize(1);
    }
    @Test void timeoutAfterRequestRemainsUncertainWithoutResubmitting() {
        server.delayMs=500;
        assertThatThrownBy(()->server.client(Duration.ofMillis(150)).send(MailjetClient.Flow.RESULT,4L,1,"one@example.invalid","Test","Body"))
                .isInstanceOf(MailjetClient.Uncertain.class);
        assertThat(server.requests).hasSize(1);
    }
    @ParameterizedTest @CsvSource({"'',secret,sender@example.invalid","key,'',sender@example.invalid","key,secret,invalid"})
    void missingCredentialsOrInvalidSenderStopsBeforeHttp(String key,String secret,String from) {
        assertThatThrownBy(()->server.client(Duration.ofSeconds(1),key,secret,from)
                .send(MailjetClient.Flow.RESULT,1L,1,"one@example.invalid","Test","Body"))
                .isInstanceOf(MailjetClient.Rejected.class);
        assertThat(server.requests).isEmpty();
    }
    @Test void invalidRecipientStopsBeforeHttp() {
        assertThatThrownBy(()->server.client(Duration.ofSeconds(1)).send(MailjetClient.Flow.RESULT,1L,1,"one@example.invalid,two@example.invalid","Test","Body"))
                .isInstanceOf(MailjetClient.Rejected.class);
        assertThat(server.requests).isEmpty();
    }
    @ParameterizedTest @ValueSource(strings={"wrong-recipient","extra-recipient","success-with-errors","missing-id","cc"})
    void contradictorySuccessResponseIsUncertain(String scenario) {
        String recipient=scenario.equals("wrong-recipient")?"other@example.invalid":"one@example.invalid";
        String id=scenario.equals("missing-id")?"":"1234";
        String to="{\"Email\":\""+recipient+"\",\"MessageID\":\""+id+"\",\"MessageUUID\":\"test-uuid\"}";
        if(scenario.equals("extra-recipient")) to+=","+to;
        String extra=scenario.equals("success-with-errors")?",\"Errors\":[{\"StatusCode\":400}]":scenario.equals("cc")?",\"Cc\":[{}]":"";
        server.reply="{\"Messages\":[{\"Status\":\"success\",\"To\":["+to+"]"+extra+"}]}";
        assertThatThrownBy(this::send).isInstanceOf(MailjetClient.Uncertain.class);
        assertThat(server.requests).hasSize(1);
    }
}
