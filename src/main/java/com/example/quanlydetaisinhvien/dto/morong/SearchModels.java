package com.example.quanlydetaisinhvien.dto.morong;

import com.example.quanlydetaisinhvien.entity.*;
import java.util.List;

/** Read-only DTOs shared by MVC and search, with authorized export links supplied in phase 3. */
public final class SearchModels {
    private SearchModels() {}
    public record DetaiRow(Long id, String tendetai, String mota, Long dotdangkyid, String tendot,
            Loaidot loaidot, Long bomonid, String tenbomon, Long giangviendexuatid,
            String giangviendexuatHoten, TrangthaiDetai trangthai) {}
    public record Option(String value, String label) {}
    public record FilterOptions(List<Option> dotdangky, List<Option> bomon, List<Option> loaidot,
            List<Option> trangthai, List<Option> giangviendexuat, List<Option> giangvienhuongdan,
            List<Option> sort) {}
    public record PageInfo(int number, int size, long totalElements, long totalPages,
            boolean hasPrevious, boolean hasNext) {
        public static PageInfo of(SearchFilterDTO filter, long total) {
            long pages = (total + filter.getSize() - 1) / filter.getSize();
            return new PageInfo(filter.getPage(), filter.getSize(), total, pages,
                    filter.getPage() > 0, (long) filter.getPage() + 1 < pages);
        }
    }
    public record Result<T>(List<T> items, PageInfo pageInfo) {}
    public record ExportOptions(boolean enabled, List<String> formats, String excelUrl, String pdfUrl) {
        public ExportOptions(boolean enabled, List<String> formats) { this(enabled, formats, null, null); }
        public boolean isEnabled() { return enabled; }
        public List<String> getFormats() { return formats; }
        public String getExcelUrl() { return excelUrl; }
        public String getPdfUrl() { return pdfUrl; }
        public static ExportOptions unavailable() { return new ExportOptions(false, List.of()); }
    }
}
