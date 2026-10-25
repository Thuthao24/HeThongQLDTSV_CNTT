package com.example.quanlydetaisinhvien.dto.morong;

import com.example.quanlydetaisinhvien.security.MorongPermissions;
import com.example.quanlydetaisinhvien.security.ThongkePermissions;
import java.time.ZonedDateTime;
import java.util.List;

public final class ExportModels {
    private ExportModels() {}
    public enum Format {
        XLSX(10000, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        PDF(2000, "application/pdf");
        public final int limit;
        public final String mime;
        Format(int limit, String mime) { this.limit = limit; this.mime = mime; }
    }
    public enum Dataset {
        DETAI, DOTDANGKY, KETQUA, THONGKE;
        public void requireAccess() {
            if (this == THONGKE) ThongkePermissions.requireChief();
            else MorongPermissions.require(MorongPermissions.Dataset.valueOf(name()));
        }
        public String slug() { return name().toLowerCase(java.util.Locale.ROOT); }
        public Class<?> filterType() {
            return switch (this) {
                case DETAI -> DetaiFilterDTO.class;
                case DOTDANGKY -> DotdangkyFilterDTO.class;
                case KETQUA -> KetquaFilterDTO.class;
                case THONGKE -> ThongkeFilterDTO.class;
            };
        }
    }
    public enum Kind { TEXT, INTEGER, DECIMAL, DATE }
    public record Column(String label, Kind kind, int width) {}
    public record Section(String title, List<Column> columns, List<List<Object>> rows) {}
    public record Report(String title, ZonedDateTime exportedAt, List<String> filters, List<Section> sections) {
        public long rowCount() { return sections.stream().mapToLong(s -> s.rows().size()).sum(); }
    }
    public record Download(byte[] bytes, String filename, String mime) {}
}
