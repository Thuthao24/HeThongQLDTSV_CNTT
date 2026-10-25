package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.morong.*;
import com.example.quanlydetaisinhvien.dto.morong.ExportModels.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.service.morong.export.*;
import org.hibernate.SessionFactory;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExportPresentationTests extends MorongSearchTestSupport {
    @Autowired ReportDataService data;
    @Autowired ReportExportService exports;
    static final Path QA = Path.of("target/module17-export-refinement");
    final ExportBackendTests reader = new ExportBackendTests();

    @ParameterizedTest @EnumSource(Format.class)
    void labelsUseCurrentDatabaseNamesAndBothWritersHaveTheSameVietnameseScope(Format format) throws Exception {
        departmentA.setTenbomon("Bộ môn Dữ liệu & Trí tuệ");
        roundA.setTendot("Đợt thực hành mùa thu");
        em.find(Taikhoan.class, teacherA.getTaikhoanid()).setHoten("Nguyễn Thị Ánh");
        em.find(Taikhoan.class, teacherB.getTaikhoanid()).setHoten("Đặng Quốc Việt"); em.flush();
        var f = completeFilter();
        var report = data.load(Dataset.DETAI, f, format);
        assertThat(report.filters()).containsExactly("Từ khóa: Hệ thống", "Đợt đăng ký: Đợt thực hành mùa thu",
                "Loại đợt: Môn học", "Bộ môn: Bộ môn Dữ liệu & Trí tuệ", "Trạng thái đề tài: Đã công bố",
                "Giảng viên đề xuất: Nguyễn Thị Ánh", "Giảng viên hướng dẫn: Đặng Quốc Việt",
                "Sắp xếp: Mã đề tài tăng dần", "Tổng số đề tài: 1");
        assertThat(report.sections().getFirst().rows()).hasSize(1);
        byte[] bytes = exports.export(Dataset.DETAI, f, format).bytes();
        String text = reader.contentText(bytes, format);
        for (String label : report.filters()) assertThat(text).contains(label);
        assertThat(text).doesNotContain("sort =", "bomonid", "dotdangkyid", "giangviendexuatid", "giangvienhuongdanid", "MONHOC", "CONGBO");
        if (format == Format.XLSX) reader.assertWorkbook(bytes, report);
        Files.createDirectories(QA); Files.write(QA.resolve("detai-filtered." + format.name().toLowerCase(Locale.ROOT)), bytes);
    }

    DetaiFilterDTO completeFilter() {
        var f = new DetaiFilterDTO(); f.setKeyword("Hệ thống"); f.setDotdangkyid(roundA.getId());
        f.setLoaidot(Loaidot.MONHOC); f.setBomonid(departmentA.getId()); f.setTrangthai(TrangthaiDetai.CONGBO);
        f.setGiangviendexuatid(teacherA.getId()); f.setGiangvienhuongdanid(teacherB.getId()); f.setSort("id,asc");
        return f;
    }

    @ParameterizedTest @EnumSource(Loaidot.class)
    void typeFilterRetainsExactDataWhileOnlyTheExportRepresentationChanges(Loaidot type) throws Exception {
        var f = new DetaiFilterDTO(); f.setLoaidot(type);
        var expected = jdbc.queryForList("select t.id from detai t join dotdangky d on t.dotdangkyid=d.id where d.loaidot=? order by t.id desc", Long.class, type.name());
        var report = data.load(Dataset.DETAI, f, Format.XLSX);
        assertThat(report.sections().getFirst().rows()).extracting(r -> (Long) r.getFirst()).containsExactlyElementsOf(expected);
        assertThat(report.filters()).containsExactly("Loại đợt: " + ReportLabels.roundType(type), "Tổng số đề tài: " + expected.size());
        reader.assertWorkbook(exports.export(Dataset.DETAI, f, Format.XLSX).bytes(), report);
    }

    @ParameterizedTest @EnumSource(Format.class)
    void unfilteredAllRowsHaveTranslatedStatusesTypesNoDefaultNoiseAndStableIds(Format format) throws Exception {
        var f = new DetaiFilterDTO(); var report = data.load(Dataset.DETAI, f, format);
        assertThat(report.filters()).containsExactly("Tổng số đề tài: 4");
        assertThat(report.sections().getFirst().rows()).extracting(r -> (Long) r.getFirst())
                .containsExactly(topicD.getId(), topicC.getId(), topicB.getId(), topicA.getId());
        byte[] bytes = exports.export(Dataset.DETAI, f, format).bytes();
        String text = reader.contentText(bytes, format);
        // PDF wraps long cell labels; compare semantic words independently of visual line breaks.
        assertThat(text.replaceAll("\\s+", " ")).contains("Chờ duyệt", "Đã duyệt", "Từ chối", "Đã công bố", "Môn học", "Tiểu luận chuyên ngành", "Khóa luận tốt nghiệp", "Nghiên cứu khoa học")
                .doesNotContain("CHODUYET", "DADUYET", "TUCHOI", "CONGBO", "MONHOC", "TLCN", "KLTN", "NCKH", "Sắp xếp:", "size", "page");
        if (format == Format.XLSX) reader.assertWorkbook(bytes, report);
        Files.createDirectories(QA); Files.write(QA.resolve("detai-all." + format.name().toLowerCase(Locale.ROOT)), bytes);
    }

    @ParameterizedTest @EnumSource(Format.class)
    void intersectionAndAllMatchingRowsIgnorePageSizeAndHaveNoMissingOrDuplicateIds(Format format) throws Exception {
        for (int i = 0; i < 35; i++) topic("Bổ sung " + i, "Dữ liệu tiếng Việt", roundA, departmentB, teacherA, TrangthaiDetai.DADUYET);
        em.flush();
        var f = new DetaiFilterDTO(); f.setBomonid(departmentB.getId()); f.setGiangviendexuatid(teacherA.getId());
        f.setPage(2); f.setSize(1); f.setSort("tendetai,asc");
        var ids = jdbc.queryForList("select id from detai where bomonid=? and giangviendexuatid=? order by tendetai asc,id desc",
                Long.class, departmentB.getId(), teacherA.getId());
        var report = data.load(Dataset.DETAI, f, format);
        assertThat(report.sections().getFirst().rows()).extracting(r -> (Long) r.getFirst()).containsExactlyElementsOf(ids).doesNotHaveDuplicates();
        assertThat(report.filters()).contains("Tổng số đề tài: 36", "Sắp xếp: Tên đề tài tăng dần");
        byte[] bytes = exports.export(Dataset.DETAI, f, format).bytes();
        if (format == Format.XLSX) reader.assertWorkbook(bytes, report);
        else for (var row : report.sections().getFirst().rows()) assertThat(reader.contentText(bytes, format)).contains(row.get(1).toString());
    }

    @ParameterizedTest @ValueSource(strings = {"dotdangkyid", "bomonid", "giangviendexuatid", "giangvienhuongdanid"})
    void missingLookupDoesNotInventANameOrLeakRawIds(String field) throws Exception {
        for (String format : List.of("pdf", "xlsx")) mvc.perform(get("/morong/xuat/detai." + format)
                .param(field, Long.toString(Long.MAX_VALUE)).with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isBadRequest()).andExpect(header().doesNotExist("Content-Disposition"));
    }

    @Test void validLookupWithNoMatchingTopicsStillProducesAnEmptyReport() {
        var f = new DetaiFilterDTO(); f.setDotdangkyid(emptyRound.getId());
        assertThat(data.load(Dataset.DETAI, f, Format.PDF).filters()).containsExactly("Đợt đăng ký: " + emptyRound.getTendot(), "Tổng số đề tài: 0");
    }

    @Test void lookupQueryCountIsBoundedWhenRowsGrowAndBothTeachersAreResolvedTogether() {
        var f = completeFilter(); em.flush(); em.clear();
        var stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        stats.clear(); data.load(Dataset.DETAI, f, Format.XLSX); long small = stats.getPrepareStatementCount();
        for (int i = 0; i < 40; i++) { var t = topic("Hệ thống " + i, "Dữ liệu", roundA, departmentA, teacherA, TrangthaiDetai.CONGBO); guide(t, teacherB); }
        em.flush(); em.clear(); stats.clear();
        assertThat(data.load(Dataset.DETAI, f, Format.XLSX).sections().getFirst().rows()).hasSize(41);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(small).isLessThanOrEqualTo(5);
    }

    @Test void realEightColumnReportKeepsLongTopicDescriptionAndTeacherTogetherAcrossPages() throws Exception {
        em.find(Taikhoan.class, teacherA.getTaikhoanid()).setHoten("Nguyễn Thị Ánh - Giảng viên bộ môn Công nghệ phần mềm và Khoa học dữ liệu");
        var ids = new ArrayList<Long>();
        for (int i = 0; i < 20; i++) ids.add(topic("Đề tài chuyên sâu về hệ thống quản lý dữ liệu sinh viên và kiểm thử phần mềm " + i,
                String.format("BẮTĐẦU%03d\n", i) + "Mô tả nhiều dòng, tiếng Việt & ký tự đặc biệt.\n".repeat(5) + String.format("KẾTTHÚC%03d", i),
                roundA, departmentA, teacherA, TrangthaiDetai.CONGBO).getId());
        em.flush();
        var snapshotReader = new ExportBackendTests(); snapshotReader.jdbc = jdbc;
        var before = snapshotReader.snapshot();
        var f = new DetaiFilterDTO(); f.setDotdangkyid(roundA.getId()); f.setSort("id,asc"); f.setPage(2); f.setSize(1);
        var report = data.load(Dataset.DETAI, f, Format.PDF);
        assertThat(report.rowCount()).isEqualTo(21);
        var bytes = exports.export(Dataset.DETAI, f, Format.PDF).bytes();
        try (var doc = Loader.loadPDF(bytes)) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(2);
            var renderer = new PDFRenderer(doc);
            for (int i = 0; i < ids.size(); i++) {
                int found = 0;
                for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                    var stripper = new PDFTextStripper(); stripper.setStartPage(p); stripper.setEndPage(p);
                    String text = stripper.getText(doc);
                    if (text.contains(String.format("BẮTĐẦU%03d", i))) {
                        found++; assertThat(text).contains(String.format("KẾTTHÚC%03d", i), ids.get(i).toString(), "ID Tên đề tài");
                    }
                }
                assertThat(found).isEqualTo(1);
            }
            for (int p : new LinkedHashSet<>(List.of(0, doc.getNumberOfPages() / 2, doc.getNumberOfPages() - 1)))
                ImageIO.write(renderer.renderImageWithDPI(p, 110), "png", QA.resolve("detai-multipage-page" + (p + 1) + ".png").toFile());
        }
        Files.write(QA.resolve("detai-multipage.pdf"), bytes);
        var excel = exports.export(Dataset.DETAI, f, Format.XLSX).bytes();
        reader.assertWorkbook(excel, report); Files.write(QA.resolve("detai-multipage.xlsx"), excel);
        assertThat(snapshotReader.snapshot()).isEqualTo(before);
    }
}
