package com.example.quanlydetaisinhvien.config;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;
import java.util.Set;

@Validated
@ConfigurationProperties("app.thongbao.email")
public record NotificationEmailSettings(
        @DefaultValue("false") boolean outboxEnabled,
        @DefaultValue("false") boolean deliveryEnabled,
        @DefaultValue("false") boolean scheduleEnabled,
        @DefaultValue("ALLOWLIST") Mode mode,
        Set<String> allowlist,
        @DefaultValue("0") @Min(0) long studentId,
        @DefaultValue("0") @Min(0) long noticeId,
        @DefaultValue("0") @Min(0) long minNoticeId,
        @DefaultValue("10") @Min(1) @Max(100) int batchSize,
        @DefaultValue("1000") @Min(0) @Max(60000) long minIntervalMs,
        @DefaultValue("60000") @Min(1000) long pollMs,
        @DefaultValue("900") @Min(60) @Max(86400) long claimTtlSeconds,
        @DefaultValue("") String baseUrl,
        @DefaultValue("10000") @Min(1) @Max(10000) int maxRecipientsPerNotice) {
    public enum Mode { ALLOWLIST, SYSTEM }
    public NotificationEmailSettings { allowlist = allowlist == null ? Set.of() : Set.copyOf(allowlist); }
}
