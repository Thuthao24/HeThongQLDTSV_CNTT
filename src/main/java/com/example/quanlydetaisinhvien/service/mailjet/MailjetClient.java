package com.example.quanlydetaisinhvien.service.mailjet;

import com.example.quanlydetaisinhvien.service.mail.MailAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/** One POST, one outbox claim, one private recipient. Never retries or follows redirects. */
@Component
public class MailjetClient {
    public static final URI SEND_ENDPOINT = URI.create("https://api.mailjet.com/v3.1/send");
    private static final Logger log = LoggerFactory.getLogger(MailjetClient.class);
    public enum Flow { RESULT }
    public record Accepted(String messageId, String messageUuid) {}
    /** Contains only application-authored diagnostics, never provider text, headers or credentials. */
    public static class Rejected extends RuntimeException {
        public Rejected(String reason) { super(reason); }
    }
    public static class Uncertain extends RuntimeException {
        public Uncertain(String reason) { super(reason); }
    }
    private final HttpClient http;
    private final URI endpoint;
    private final Duration timeout;
    private final String key, secret, from, name;
    private final JsonMapper json = JsonMapper.builder().build();

    @Autowired
    public MailjetClient(Environment environment) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                        .followRedirects(HttpClient.Redirect.NEVER).build(), SEND_ENDPOINT, Duration.ofSeconds(15),
                environment.getProperty("app.mailjet.api-key", ""),
                environment.getProperty("app.mailjet.secret-key", ""),
                environment.getProperty("app.mailjet.from-email", ""),
                environment.getProperty("app.mailjet.from-name", "HCMUTE"));
    }

    // Package-private injection for loopback mock HTTP tests; production has no endpoint override.
    MailjetClient(HttpClient http, URI endpoint, Duration timeout, String key, String secret, String from, String name) {
        this.http=http; this.endpoint=endpoint; this.timeout=timeout;
        this.key=key; this.secret=secret; this.from=from; this.name=name;
    }

    public Accepted send(Flow flow, Long outboxId, int attempt, String to, String subject, String body) {
        String recipient=MailAddress.validOrNull(to);
        if (key.isBlank() || secret.isBlank() || key.contains(":") || hasLineBreak(key) || hasLineBreak(secret)
                || MailAddress.validOrNull(from)==null || hasLineBreak(name))
            throw new Rejected("Mailjet chưa có cấu hình API/người gửi hợp lệ.");
        if (recipient==null || outboxId==null || outboxId<=0 || attempt<=0 || subject==null || body==null || body.isBlank())
            throw new Rejected("Snapshot email không hợp lệ; chưa gọi Mailjet.");
        String reference="hcmute-"+flow.name().toLowerCase(Locale.ROOT)+"-"+outboxId+"-"+attempt;
        HttpRequest request;
        try {
            var message=Map.of("From",Map.of("Email",from,"Name",name),
                    "To",List.of(Map.of("Email",recipient)), "Subject",subject.replaceAll("[\\r\\n]+"," "),
                    "TextPart",body, "CustomID",reference);
            String payload=json.writeValueAsString(Map.of("Messages",List.of(message)));
            String authorization=Base64.getEncoder().encodeToString((key+":"+secret).getBytes(StandardCharsets.UTF_8));
            request=HttpRequest.newBuilder(endpoint).timeout(timeout)
                    .header("Authorization","Basic "+authorization)
                    .header("Content-Type","application/json; charset=UTF-8")
                    .header("Accept","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload,StandardCharsets.UTF_8)).build();
        } catch (RuntimeException ex) {
            throw new Rejected("Không thể tạo yêu cầu Mailjet; chưa gửi.");
        }
        HttpResponse<String> response;
        try {
            response=http.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new Uncertain("Mailjet bị gián đoạn; cần đối soát trước khi gửi lại.");
        } catch (IOException | RuntimeException ex) {
            // Even a lost response can follow acceptance. No retry, no original exception/cause logging.
            throw new Uncertain("Không rõ Mailjet đã nhận yêu cầu; cần đối soát trước khi gửi lại.");
        }
        int status=response.statusCode();
        log.info("Mailjet response reference={} http={}",reference,status);
        if (status>=400 && status<500 && status!=408)
            throw new Rejected("Mailjet từ chối yêu cầu (HTTP "+status+"); kiểm tra cấu hình hoặc hạn mức trước khi thử lại.");
        if (status<200 || status>=300)
            throw new Uncertain("Phản hồi Mailjet HTTP "+status+"; cần đối soát trước khi gửi lại.");
        JsonNode root;
        try {
            if (response.body()==null || response.body().length()>65536) throw new IllegalArgumentException();
            root=json.readTree(response.body());
        } catch (RuntimeException ex) {
            throw new Uncertain("Không đọc được phản hồi Mailjet; không tự gửi lại.");
        }
        JsonNode messages=root==null ? null : root.get("Messages");
        if (messages==null || !messages.isArray() || messages.size()!=1)
            throw new Uncertain("Mailjet trả số message không khớp; cần đối soát.");
        JsonNode result=messages.get(0);
        JsonNode errors=result.get("Errors");
        if ("error".equals(result.path("Status").asString()) && errors!=null && errors.isArray() && !errors.isEmpty()) {
            boolean definitelyRejected=true;
            for (JsonNode error:errors) {
                int code=error.path("StatusCode").asInt(0);
                definitelyRejected &= code>=400 && code<500 && code!=408;
                String category=error.path("ErrorCode").asString();
                if (!category.matches("(?:mj|send)-[0-9]{4}")) category="unclassified";
                log.warn("Mailjet message error reference={} status={} category={}",reference,code,category);
            }
            if (definitelyRejected) throw new Rejected("Mailjet từ chối message; kiểm tra xác thực, người gửi đã xác minh và dữ liệu.");
        }
        JsonNode recipients=result.path("To");
        if (!"success".equals(result.path("Status").asString())
                || (errors!=null && (!errors.isArray() || !errors.isEmpty()))
                || !recipients.isArray() || recipients.size()!=1
                || !recipient.equalsIgnoreCase(recipients.get(0).path("Email").asString())
                || nonempty(result.get("Cc")) || nonempty(result.get("Bcc")))
            throw new Uncertain("Phản hồi message Mailjet không xác nhận đúng người nhận; cần đối soát.");
        String id=recipients.get(0).path("MessageID").asString();
        String uuid=recipients.get(0).path("MessageUUID").asString();
        if (!id.matches("[0-9]{1,30}") || !uuid.matches("[a-zA-Z0-9-]{1,64}"))
            throw new Uncertain("Mailjet chưa trả mã tiếp nhận hợp lệ; cần đối soát.");
        // Acceptance only, not proof of Inbox delivery. CustomID correlates with the existing outbox history.
        log.info("Mailjet accepted reference={} messageId={} messageUuid={}",reference,id,uuid);
        return new Accepted(id,uuid);
    }

    private static boolean nonempty(JsonNode value) { return value!=null && (!value.isArray() || !value.isEmpty()); }
    private static boolean hasLineBreak(String value) { return value.contains("\r") || value.contains("\n"); }
}
