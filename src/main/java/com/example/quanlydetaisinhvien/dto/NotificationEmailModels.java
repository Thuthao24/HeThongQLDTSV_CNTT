package com.example.quanlydetaisinhvien.dto;

import com.example.quanlydetaisinhvien.entity.TrangthaiEmailthongbao;
import java.time.LocalDateTime;
import java.util.List;

public final class NotificationEmailModels {
    private NotificationEmailModels() {}
    public record Counts(long total, long pending, long sending, long sent, long failed, long missingEmail, long uncertain) {}
    public record Item(Long id, Long thongbaoid, Long thongbaoSnapshotId, Long sinhvienid, String nguoinhan,
            String email, String tieude, TrangthaiEmailthongbao trangthai, int solanthu,
            LocalDateTime ngaytao, LocalDateTime lanthu, LocalDateTime ngaygui, String loi) {}
    public record Page(int number, int size, long totalElements, long totalPages) {}
    public record Monitor(boolean schemaAvailable, boolean outboxEnabled, boolean deliveryEnabled,
            Counts counts, List<Item> items, Page pageInfo) {}
}
