package com.example.quanlydetaisinhvien.dto.morong;

import com.example.quanlydetaisinhvien.entity.Loaidot;
import java.math.BigDecimal;
import java.util.List;

/** Immutable statistics data with explicit JavaBean getters for Thymeleaf. */
public final class StatisticsModels {
    private StatisticsModels() {}

    public record Summary(long tongDetai, long tongNhom, long tongDangky, long tongKetqua,
            long daCongbo, long chuaCongbo, long nhomChuaDangky, long ketquaThieuDiemCuoi,
            long dangkyLechDot) {
        public long getTongDetai() { return tongDetai; }
        public long getTongNhom() { return tongNhom; }
        public long getTongDangky() { return tongDangky; }
        public long getTongKetqua() { return tongKetqua; }
        public long getDaCongbo() { return daCongbo; }
        public long getChuaCongbo() { return chuaCongbo; }
        public long getNhomChuaDangky() { return nhomChuaDangky; }
        public long getKetquaThieuDiemCuoi() { return ketquaThieuDiemCuoi; }
        public long getDangkyLechDot() { return dangkyLechDot; }
    }

    public record RoundRow(Long id, String tendot, Loaidot loaidot, long soDetai, long soNhom,
            long soDangky, long soKetqua, long soDaCongbo, long soNhomChuaDangky,
            long soKetquaThieuDiemCuoi, long soDangkyLechDot) {
        public Long getId() { return id; }
        public String getTendot() { return tendot; }
        public Loaidot getLoaidot() { return loaidot; }
        public long getSoDetai() { return soDetai; }
        public long getSoNhom() { return soNhom; }
        public long getSoDangky() { return soDangky; }
        public long getSoKetqua() { return soKetqua; }
        public long getSoDaCongbo() { return soDaCongbo; }
        public long getSoNhomChuaDangky() { return soNhomChuaDangky; }
        public long getSoKetquaThieuDiemCuoi() { return soKetquaThieuDiemCuoi; }
        public long getSoDangkyLechDot() { return soDangkyLechDot; }
    }

    public record StatusCount(String code, String label, long count, BigDecimal percentage) {
        public String getCode() { return code; }
        public String getLabel() { return label; }
        public long getCount() { return count; }
        public BigDecimal getPercentage() { return percentage; }
    }

    public record RoundOption(Long id, String tendot, Loaidot loaidot) {
        public Long getId() { return id; }
        public String getTendot() { return tendot; }
        public Loaidot getLoaidot() { return loaidot; }
        public String getValue() { return id.toString(); }
        public String getLabel() { return tendot; }
    }

    public record FilterOptions(List<RoundOption> dotdangky) {
        public List<RoundOption> getDotdangky() { return dotdangky; }
    }

    public record ExportOptions(boolean enabled, List<String> formats, String excelUrl, String pdfUrl) {
        public ExportOptions(boolean enabled, List<String> formats) { this(enabled, formats, null, null); }
        public boolean isEnabled() { return enabled; }
        public List<String> getFormats() { return formats; }
        public String getExcelUrl() { return excelUrl; }
        public String getPdfUrl() { return pdfUrl; }
        public static ExportOptions unavailable() { return new ExportOptions(false, List.of()); }
    }

    public record Report(Summary summary, List<RoundRow> byRound, List<StatusCount> detaiByStatus,
            List<StatusCount> dangkyByStatus, List<StatusCount> ketquaByStatus, FilterOptions filterOptions) {
        public Summary getSummary() { return summary; }
        public List<RoundRow> getByRound() { return byRound; }
        public List<StatusCount> getDetaiByStatus() { return detaiByStatus; }
        public List<StatusCount> getDangkyByStatus() { return dangkyByStatus; }
        public List<StatusCount> getKetquaByStatus() { return ketquaByStatus; }
        public FilterOptions getFilterOptions() { return filterOptions; }
    }
}
