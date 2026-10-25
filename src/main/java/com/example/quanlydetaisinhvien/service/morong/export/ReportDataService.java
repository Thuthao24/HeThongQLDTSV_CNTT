package com.example.quanlydetaisinhvien.service.morong.export;

import com.example.quanlydetaisinhvien.dto.morong.*;
import com.example.quanlydetaisinhvien.dto.morong.ExportModels.*;
import com.example.quanlydetaisinhvien.dto.morong.StatisticsModels.StatusCount;
import com.example.quanlydetaisinhvien.exception.ExportLimitException;
import com.example.quanlydetaisinhvien.service.morong.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ReportDataService {
    private final TimkiemNangcaoService search;
    private final ThongkeService statistics;
    public ReportDataService(TimkiemNangcaoService search, ThongkeService statistics) {
        this.search = search; this.statistics = statistics;
    }
    public Report load(Dataset dataset, Object filter, Format format) {
        dataset.requireAccess();
        if (!dataset.filterType().isInstance(filter)) throw new IllegalArgumentException("Sai loại bộ lọc xuất.");
        var sections = new ArrayList<Section>();
        String title;
        var scope = new ArrayList<String>();
        ExportLinks.values(dataset, filter).forEach((field, value) -> scope.add(field + " = "
                + (value instanceof LocalDate date ? date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : value)));
        switch (dataset) {
            case DETAI -> {
                title = "DANH SÁCH ĐỀ TÀI";
                var rows = search.exportDetai((DetaiFilterDTO) filter, format.limit).stream().map(r -> row(r.id(),
                        r.tendetai(), r.mota(), r.tendot(), r.loaidot(), r.tenbomon(), r.giangviendexuatHoten(), r.trangthai())).toList();
                sections.add(new Section("Đề tài", List.of(text("ID", 12), text("Tên đề tài", 38),
                        text("Mô tả", 50), text("Đợt đăng ký", 28), text("Loại đợt", 14), text("Bộ môn", 25),
                        text("Người đề xuất", 26), text("Trạng thái", 16)), rows));
            }
            case DOTDANGKY -> {
                title = "DANH SÁCH ĐỢT ĐĂNG KÝ";
                var rows = search.exportDotdangky((DotdangkyFilterDTO) filter, format.limit).stream().map(r -> row(r.id(),
                        r.tendot(), r.loaidot(), r.trangthai(), r.batdaugiangvien(), r.ketthucgiangvien(),
                        r.batdausinhvien(), r.ketthucsinhvien(), r.hanchamphanbien(), r.ngaybaocaohoidong())).toList();
                sections.add(new Section("Đợt đăng ký", List.of(text("ID", 12), text("Tên đợt", 35),
                        text("Loại đợt", 14), text("Trạng thái", 16), date("Bắt đầu giảng viên"), date("Kết thúc giảng viên"),
                        date("Bắt đầu sinh viên"), date("Kết thúc sinh viên"), date("Hạn chấm phản biện"), date("Ngày báo cáo hội đồng")), rows));
            }
            case KETQUA -> {
                title = "DANH SÁCH KẾT QUẢ";
                var rows = search.exportKetqua((KetquaFilterDTO) filter, format.limit).stream().map(r -> row(r.id(),
                        r.tendetai(), r.tennhom(), r.tendot(), r.loaidot(), r.trangthai(), r.diemcuoi(),
                        Boolean.TRUE.equals(r.dacongbo()) ? "Đã công bố" : "Chưa công bố", r.ngaycongbo())).toList();
                sections.add(new Section("Kết quả", List.of(text("ID", 12), text("Tên đề tài", 38),
                        text("Nhóm sinh viên", 26), text("Đợt đăng ký", 25), text("Loại đợt", 14), text("Trạng thái đăng ký", 18),
                        col("Điểm cuối", Kind.DECIMAL, 14), text("Công bố", 18), date("Ngày công bố")), rows));
            }
            case THONGKE -> {
                title = "THỐNG KÊ & BÁO CÁO";
                var f = (ThongkeFilterDTO) filter;
                var report = statistics.statistics(f);
                scope.add(f.getDotdangkyid() == null ? "Phạm vi: Tất cả đợt đăng ký"
                        : "Phạm vi: " + report.byRound().getFirst().tendot() + " (#" + f.getDotdangkyid() + ")");
                var s = report.summary();
                var kpis = new ArrayList<>(List.of(row("Tổng số đề tài", s.tongDetai()), row("Tổng số nhóm sinh viên", s.tongNhom()),
                        row("Tổng số đăng ký đề tài", s.tongDangky()), row("Tổng số kết quả", s.tongKetqua()),
                        row("Số kết quả đã công bố", s.daCongbo()), row("Số kết quả chưa công bố", s.chuaCongbo()),
                        row("Nhóm chưa đăng ký", s.nhomChuaDangky()), row("Kết quả thiếu điểm cuối", s.ketquaThieuDiemCuoi()),
                        row("Đăng ký lệch đợt", s.dangkyLechDot())));
                sections.add(new Section("Tổng hợp", List.of(text("Chỉ số", 42), col("Số lượng", Kind.INTEGER, 20)), kpis));
                Object ratio = s.tongKetqua() == 0 ? null : BigDecimal.valueOf(s.daCongbo()).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(s.tongKetqua()), 2, RoundingMode.HALF_UP);
                sections.add(new Section("Tỷ lệ công bố", List.of(text("Chỉ số", 42), col("Tỷ lệ (%)", Kind.DECIMAL, 20)),
                        List.of(row("Đã công bố / tổng kết quả", ratio))));
                sections.add(statuses("Trạng thái đề tài", report.detaiByStatus()));
                sections.add(statuses("Trạng thái đăng ký", report.dangkyByStatus()));
                sections.add(statuses("Công bố kết quả", report.ketquaByStatus()));
                sections.add(new Section("Theo đợt", List.of(text("ID đợt", 12), text("Tên đợt", 30), text("Loại đợt", 14),
                        count("Đề tài"), count("Nhóm"), count("Đăng ký"), count("Kết quả"), count("Đã công bố"),
                        count("Nhóm chưa đăng ký"), count("Thiếu điểm cuối"), count("Đăng ký lệch đợt")),
                        report.byRound().stream().map(r -> row(r.id(), r.tendot(), r.loaidot(), r.soDetai(), r.soNhom(),
                                r.soDangky(), r.soKetqua(), r.soDaCongbo(), r.soNhomChuaDangky(), r.soKetquaThieuDiemCuoi(), r.soDangkyLechDot())).toList()));
            }
            default -> throw new IllegalArgumentException("Dataset không hỗ trợ.");
        }
        if (scope.isEmpty()) scope.add("Không áp dụng bộ lọc");
        var report = new Report(title, ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")), List.copyOf(scope), List.copyOf(sections));
        ExportLimitException.check(report.rowCount(), format.limit);
        return report;
    }
    private Section statuses(String title, List<StatusCount> values) {
        return new Section(title, List.of(text("Mã trạng thái", 22), text("Trạng thái", 28), count("Số lượng"),
                col("Tỷ trọng (%)", Kind.DECIMAL, 18)), values.stream().map(v -> row(v.code(), v.label(), v.count(), v.percentage())).toList());
    }
    private static List<Object> row(Object... values) { return Collections.unmodifiableList(Arrays.asList(values)); }
    private static Column col(String label, Kind kind, int width) { return new Column(label, kind, width); }
    private static Column text(String label, int width) { return col(label, Kind.TEXT, width); }
    private static Column count(String label) { return col(label, Kind.INTEGER, 17); }
    private static Column date(String label) { return col(label, Kind.DATE, 24); }
}
