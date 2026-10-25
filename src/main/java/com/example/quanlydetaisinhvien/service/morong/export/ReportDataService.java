package com.example.quanlydetaisinhvien.service.morong.export;

import com.example.quanlydetaisinhvien.dto.morong.*;
import com.example.quanlydetaisinhvien.dto.morong.ExportModels.*;
import com.example.quanlydetaisinhvien.exception.ExportLimitException;
import com.example.quanlydetaisinhvien.service.morong.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ReportDataService {
    private final TimkiemNangcaoService search;
    private final ThongkeService statistics;
    private final ReportLabels labels;
    public ReportDataService(TimkiemNangcaoService search, ThongkeService statistics, ReportLabels labels) {
        this.search = search; this.statistics = statistics; this.labels = labels;
    }
    public Report load(Dataset dataset, Object filter, Format format) {
        dataset.requireAccess();
        if (!dataset.filterType().isInstance(filter)) throw new IllegalArgumentException("Sai loại bộ lọc xuất.");
        var sections = new ArrayList<Section>();
        String title;
        var scope = new ArrayList<String>();
        switch (dataset) {
            case DETAI -> {
                title = "DANH SÁCH ĐỀ TÀI";
                var rows = search.exportDetai((DetaiFilterDTO) filter, format.limit).stream().map(r -> row(r.id(),
                        r.tendetai(), r.mota(), r.tendot(), ReportLabels.roundType(r.loaidot()), r.tenbomon(),
                        r.giangviendexuatHoten(), ReportLabels.topicStatus(r.trangthai()))).toList();
                scope.addAll(labels.topicFilters((DetaiFilterDTO) filter));
                scope.add("Tổng số đề tài: " + rows.size());
                sections.add(new Section("Đề tài", List.of(text("ID", 12), text("Tên đề tài", 38),
                        text("Mô tả", 50), text("Đợt đăng ký", 28), text("Loại đợt", 14), text("Bộ môn", 25),
                        text("Người đề xuất", 26), text("Trạng thái", 16)), rows));
            }
            case THONGKE -> {
                title = "THỐNG KÊ & BÁO CÁO";
                var f = (ThongkeFilterDTO) filter;
                var report = statistics.statistics(f);
                scope.clear();
                scope.add(f.getDotdangkyid() == null ? "Phạm vi: Tất cả đợt đăng ký"
                        : "Phạm vi: " + report.byRound().getFirst().tendot());
                var s = report.summary();
                var overview = new ArrayList<>(List.of(row("Tổng đề tài", s.tongDetai()), row("Nhóm sinh viên", s.tongNhom()),
                        row("Lượt đăng ký", s.tongDangky()), row("Kết quả đã công bố", s.daCongbo() + " / " + s.tongKetqua()),
                        row("Trạng thái đề tài", "")));
                report.detaiByStatus().forEach(v -> overview.add(row("CONGBO".equals(v.code()) ? "Đã công bố" : v.label(), v.count())));
                overview.add(row("Công bố kết quả", ""));
                report.ketquaByStatus().forEach(v -> overview.add(row(v.label(),
                        v.count() + (s.tongKetqua() == 0 ? "" : " (" + v.percentage().toPlainString() + "%)"))));
                if (s.nhomChuaDangky() > 0 || s.ketquaThieuDiemCuoi() > 0) {
                    overview.add(row("Cần xử lý", ""));
                    if (s.nhomChuaDangky() > 0) overview.add(row("Nhóm chưa đăng ký", s.nhomChuaDangky()));
                    if (s.ketquaThieuDiemCuoi() > 0) overview.add(row("Kết quả thiếu điểm", s.ketquaThieuDiemCuoi()));
                }
                sections.add(new Section("Tổng hợp", List.of(text("Chỉ số", 42), col("Giá trị", Kind.INTEGER, 24)), overview));
                sections.add(new Section("Theo đợt", List.of(text("Tên đợt đăng ký", 36), count("Số đề tài"), count("Số nhóm"),
                        count("Số lượt đăng ký"), text("Kết quả công bố", 23)), report.byRound().stream()
                        .map(r -> row(r.tendot(), r.soDetai(), r.soNhom(), r.soDangky(), r.soDaCongbo() + " / " + r.soKetqua())).toList()));
            }
            default -> throw new IllegalArgumentException("Dataset không hỗ trợ.");
        }
        if (scope.isEmpty()) scope.add("Không áp dụng bộ lọc");
        var report = new Report(title, ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")), List.copyOf(scope), List.copyOf(sections));
        ExportLimitException.check(report.rowCount(), format.limit);
        return report;
    }
    private static List<Object> row(Object... values) { return Collections.unmodifiableList(Arrays.asList(values)); }
    private static Column col(String label, Kind kind, int width) { return new Column(label, kind, width); }
    private static Column text(String label, int width) { return col(label, Kind.TEXT, width); }
    private static Column count(String label) { return col(label, Kind.INTEGER, 17); }
}
