package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.morong.*;
import com.example.quanlydetaisinhvien.dto.morong.ExportModels.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.service.morong.export.*;
import com.example.quanlydetaisinhvien.exception.ExportLimitException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;
import javax.imageio.ImageIO;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExportBackendTests extends MorongSearchTestSupport {
    @Autowired ReportExportService exports;
    @Autowired ReportDataService data;
    static final Path QA = Path.of("target/module17-export-qa");

    static Stream<Arguments> outputs() {
        return Arrays.stream(Dataset.values()).flatMap(d -> Arrays.stream(Format.values()).map(f -> Arguments.of(d, f)));
    }
    Object filter(Dataset d) {
        return switch (d) { case DETAI -> new DetaiFilterDTO(); case DOTDANGKY -> new DotdangkyFilterDTO();
            case KETQUA -> new KetquaFilterDTO(); case THONGKE -> new ThongkeFilterDTO(); };
    }
    String url(Dataset d, Format f) { return "/morong/xuat/" + d.slug() + "." + f.name().toLowerCase(Locale.ROOT); }

    @ParameterizedTest @MethodSource("outputs")
    void allEightDownloadsHaveRealReadableContentHeadersAndMatchVerifiedSourceData(Dataset dataset, Format format) throws Exception {
        var expected = data.load(dataset, filter(dataset), format);
        var response = mvc.perform(get(url(dataset, format)).with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andExpect(content().contentType(format.mime))
                .andExpect(header().string("Cache-Control", "no-store")).andReturn().getResponse();
        assertThat(response.getHeader("Content-Disposition")).matches("attachment; filename=\"" + dataset.slug() + "_\\d{8}_\\d{6}\\.(xlsx|pdf)\"");
        byte[] bytes = response.getContentAsByteArray();
        Files.createDirectories(QA); Files.write(QA.resolve(dataset.slug() + "." + format.name().toLowerCase(Locale.ROOT)), bytes);
        if (format == Format.XLSX) assertWorkbook(bytes, expected);
        else try (var document = Loader.loadPDF(bytes)) {
            var text = new PDFTextStripper().getText(document);
            assertThat(text).contains(expected.title(), "Thời điểm xuất:", "Trang 1 /");
            assertThat(document.getNumberOfPages()).isPositive();
            ImageIO.write(new PDFRenderer(document).renderImageWithDPI(0, 90), "png", QA.resolve(dataset.slug() + "-page1.png").toFile());
        }
    }

    void assertWorkbook(byte[] bytes, Report expected) throws Exception {
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(expected.sections().size());
            for (int i = 0; i < expected.sections().size(); i++) {
                var section = expected.sections().get(i); var sheet = workbook.getSheetAt(i);
                assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo(expected.title());
                int header = 3 + expected.filters().size();
                assertThat(sheet.getPaneInformation().isFreezePane()).isTrue();
                for (int j = 0; j < section.rows().size(); j++) {
                    var row = sheet.getRow(header + 1 + j);
                    for (int c = 0; c < section.columns().size(); c++) {
                        Object value = section.rows().get(j).get(c); var cell = row.getCell(c);
                        assertThat(cell.getCellType()).isNotEqualTo(CellType.FORMULA);
                        if (value == null) assertThat(cell.getCellType()).isEqualTo(CellType.BLANK);
                        else if (value instanceof Number n && section.columns().get(c).kind() != Kind.TEXT) {
                            assertThat(cell.getCellType()).isEqualTo(CellType.NUMERIC);
                            assertThat(cell.getNumericCellValue()).isEqualTo(n.doubleValue());
                            if (section.columns().get(c).kind() == Kind.DECIMAL) assertThat(cell.getCellStyle().getDataFormatString()).isEqualTo("0.00");
                        } else if (value instanceof LocalDateTime date) {
                            assertThat(DateUtil.isCellDateFormatted(cell)).isTrue();
                            assertThat(cell.getLocalDateTimeCellValue()).isEqualTo(date);
                        } else assertThat(cell.getStringCellValue()).isEqualTo(value.toString());
                    }
                }
            }
        }
    }

    static Stream<Arguments> roles() {
        return outputs().flatMap(args -> Stream.of("TRUONGKHOA", "TRUONGBOMON", "GIANGVIEN", "SINHVIEN", "UNKNOWN")
                .map(role -> Arguments.of(args.get()[0], args.get()[1], role)));
    }
    @ParameterizedTest @MethodSource("roles")
    void strictHttpAndDirectServiceRoleMatrix(Dataset dataset, Format format, String role) throws Exception {
        boolean allowed = role.equals("TRUONGKHOA") || dataset == Dataset.DETAI && Set.of("TRUONGBOMON", "GIANGVIEN").contains(role)
                || dataset == Dataset.KETQUA && role.equals("SINHVIEN");
        String username = role.equals("SINHVIEN") ? "search-student-a" : "search-chief";
        auth(username, role);
        if (!allowed) {
            assertThatThrownBy(() -> exports.export(dataset, filter(dataset), format)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> data.load(dataset, filter(dataset), format)).isInstanceOf(AccessDeniedException.class);
            if (dataset == Dataset.DETAI) assertThatThrownBy(() -> repository.exportDetai(new DetaiFilterDTO(), format.limit)).isInstanceOf(AccessDeniedException.class);
            if (dataset == Dataset.DOTDANGKY) assertThatThrownBy(() -> repository.exportDotdangky(new DotdangkyFilterDTO(), format.limit)).isInstanceOf(AccessDeniedException.class);
            if (dataset == Dataset.KETQUA) assertThatThrownBy(() -> repository.exportKetqua(new KetquaFilterDTO(), format.limit)).isInstanceOf(AccessDeniedException.class);
        }
        mvc.perform(get(url(dataset, format)).with(user(username).roles(role))).andExpect(status().is(allowed ? 200 : 403));
    }

    @ParameterizedTest @EnumSource(Format.class)
    void studentsOnlyExportPublishedMembershipResultsAndCannotForgeScope(Format format) throws Exception {
        auth("search-student-a", "SINHVIEN");
        var rows = repository.exportKetqua(new KetquaFilterDTO(), format.limit);
        assertThat(rows).extracting(SearchModels.KetquaRow::id).containsExactly(resultB.getId(), resultA.getId());
        var expected = data.load(Dataset.KETQUA, new KetquaFilterDTO(), format);
        var response = mvc.perform(get(url(Dataset.KETQUA, format)).with(user("search-student-a").roles("SINHVIEN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        if (format == Format.XLSX) assertWorkbook(response, expected);
        else try (var doc = Loader.loadPDF(response)) {
            assertThat(new PDFTextStripper().getText(doc)).doesNotContain(topicC.getTendetai(), topicD.getTendetai());
        }
        for (String[] param : List.of(new String[]{"dotdangkyid", roundC.getId().toString()},
                new String[]{"dotdangkyid", roundD.getId().toString()}, new String[]{"dacongbo", "false"})) {
            var bytes = mvc.perform(get(url(Dataset.KETQUA, format)).param(param[0], param[1])
                            .with(user("search-student-a").roles("SINHVIEN"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
            assertThat(contentText(bytes, format)).contains("Không có dữ liệu");
        }
        for (String param : List.of("username", "role", "sinhvienid", "nhomid", "ids")) {
            mvc.perform(get(url(Dataset.KETQUA, format)).param(param, "1").with(user("search-student-a").roles("SINHVIEN")))
                    .andExpect(status().isBadRequest());
        }
        auth("search-member", "SINHVIEN");
        assertThat(repository.exportKetqua(new KetquaFilterDTO(), format.limit)).extracting(SearchModels.KetquaRow::id).containsExactly(resultA.getId());
    }

    @ParameterizedTest @EnumSource(Format.class)
    void searchExportIncludesEveryMatchingRowInChosenOrderRegardlessOfPagination(Format format) throws Exception {
        var extra = topic("Hệ thống bổ sung", "Tiếng Việt", roundA, departmentA, teacherA, TrangthaiDetai.CONGBO);
        guide(extra, teacherB); em.flush();
        var f = new DetaiFilterDTO(); f.setKeyword("Hệ thống"); f.setDotdangkyid(roundA.getId()); f.setBomonid(departmentA.getId());
        f.setLoaidot(Loaidot.MONHOC); f.setTrangthai(TrangthaiDetai.CONGBO); f.setGiangviendexuatid(teacherA.getId());
        f.setGiangvienhuongdanid(teacherB.getId()); f.setSort("id,asc"); f.setPage(1); f.setSize(1);
        assertThat(service.detai(f).items()).hasSize(1);
        assertThat(service.exportDetai(f, format.limit)).extracting(SearchModels.DetaiRow::id).containsExactly(topicA.getId(), extra.getId());
        var expected = data.load(Dataset.DETAI, f, format);
        var request = get(url(Dataset.DETAI, format)).with(user("search-chief").roles("TRUONGKHOA"));
        com.example.quanlydetaisinhvien.service.morong.ExportLinks.values(Dataset.DETAI, f).forEach((k, v) -> request.param(k, v.toString()));
        var bytes = mvc.perform(request.param("page", "1").param("size", "1")).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        if (format == Format.XLSX) assertWorkbook(bytes, expected);
        else {
            var text = contentText(bytes, format);
            assertThat(text).contains("Hệ thống bổ sung", "sort = id,asc", "giangvienhuongdanid = " + teacherB.getId());
            assertThat(text.indexOf(topicA.getTendetai())).isLessThan(text.indexOf(extra.getTendetai()));
        }
    }

    @ParameterizedTest @EnumSource(Format.class)
    void roundAndResultFiltersUseTheSameValidationAndQueriesAsSearch(Format format) throws Exception {
        var rounds = new DotdangkyFilterDTO(); rounds.setKeyword("Môn học"); rounds.setLoaidot(Loaidot.MONHOC);
        rounds.setTrangthai(TrangthaiDotdangky.DONG); rounds.setMocThoiGian(DotdangkyFilterDTO.MocThoiGian.BATDAUSINHVIEN);
        rounds.setThoiGianTu(roundA.getBatdausinhvien().toLocalDate()); rounds.setThoiGianDen(roundA.getBatdausinhvien().toLocalDate()); rounds.setSort("id,asc");
        var results = new KetquaFilterDTO(); results.setKeyword("Nhóm Một"); results.setDotdangkyid(roundA.getId());
        results.setLoaidot(Loaidot.MONHOC); results.setTrangthai(TrangthaiDangkydetai.DADUYET); results.setDacongbo(true);
        results.setDiemTu(new java.math.BigDecimal("8.25")); results.setDiemDen(new java.math.BigDecimal("8.25"));
        results.setNgayCongboTu(resultA.getNgaycongbo().toLocalDate()); results.setNgayCongboDen(resultA.getNgaycongbo().toLocalDate()); results.setSort("id,asc");
        assertThat(service.exportDotdangky(rounds, format.limit)).isEqualTo(service.dotdangky(rounds).items());
        assertThat(service.exportKetqua(results, format.limit)).isEqualTo(service.ketqua(results).items());
        for (Dataset dataset : List.of(Dataset.DOTDANGKY, Dataset.KETQUA)) {
            auth("search-chief", "TRUONGKHOA"); Object filter = dataset == Dataset.DOTDANGKY ? rounds : results;
            var expected = data.load(dataset, filter, format);
            var request = get(url(dataset, format)).with(user("search-chief").roles("TRUONGKHOA"));
            com.example.quanlydetaisinhvien.service.morong.ExportLinks.values(dataset, filter).forEach((k, v) -> request.param(k, v.toString()));
            byte[] bytes = mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
            if (format == Format.XLSX) assertWorkbook(bytes, expected);
            else assertThat(contentText(bytes, format)).contains(expected.title());
        }
    }

    @ParameterizedTest @EnumSource(Format.class)
    void statisticsExportIsExactlyTheVerifiedReportForSelectedRound(Format format) throws Exception {
        var filter = new ThongkeFilterDTO(); filter.setDotdangkyid(roundA.getId());
        var expected = data.load(Dataset.THONGKE, filter, format);
        var bytes = mvc.perform(get(url(Dataset.THONGKE, format)).param("dotdangkyid", roundA.getId().toString())
                        .with(user("search-chief").roles("TRUONGKHOA"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        if (format == Format.XLSX) assertWorkbook(bytes, expected);
        else assertThat(contentText(bytes, format)).contains("THỐNG KÊ & BÁO CÁO", "Tỷ lệ công bố", "Phạm vi:", roundA.getTendot());
    }

    @ParameterizedTest @MethodSource("outputs")
    void emptyDatasetsStillProduceValidReports(Dataset dataset, Format format) throws Exception {
        var request = get(url(dataset, format)).with(user("search-chief").roles("TRUONGKHOA"));
        if (dataset == Dataset.THONGKE) request.param("dotdangkyid", emptyRound.getId().toString());
        else request.param("keyword", "KHÔNG KHỚP BẤT KỲ DỮ LIỆU NÀO");
        var bytes = mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        if (dataset != Dataset.THONGKE) assertThat(contentText(bytes, format)).contains("Không có dữ liệu");
        else assertThat(contentText(bytes, format)).contains("Tổng số kết quả", "Nhóm chưa đăng ký");
    }

    static Stream<Arguments> invalidFilters() {
        return Stream.of(Arguments.of("detai", "bomonid", "0"), Arguments.of("detai", "trangthai", "BAD"),
                Arguments.of("detai", "sort", "password,asc"), Arguments.of("dotdangky", "loaidot", "BAD"),
                Arguments.of("dotdangky", "mocThoiGian", "BAD"), Arguments.of("dotdangky", "thoiGianTu", "2026-02-30"),
                Arguments.of("ketqua", "diemTu", "11"), Arguments.of("ketqua", "dacongbo", "BAD"),
                Arguments.of("ketqua", "trangthai", "CONGBO"), Arguments.of("ketqua", "ngayCongboDen", "BAD"),
                Arguments.of("thongke", "dotdangkyid", "abc"), Arguments.of("thongke", "dotdangkyid", "0"),
                Arguments.of("thongke", "dotdangkyid", "-1"), Arguments.of("thongke", "dotdangkyid", "999999999999999999999"));
    }
    @ParameterizedTest @MethodSource("invalidFilters")
    void malformedParametersAre400(String dataset, String field, String value) throws Exception {
        for (String format : List.of("xlsx", "pdf")) mvc.perform(get("/morong/xuat/" + dataset + "." + format)
                        .param(field, value).with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isBadRequest()).andExpect(header().doesNotExist("Content-Disposition"));
    }

    @Test void invalidRangesDuplicatesUnknownDatasetAndMissingStatisticsRoundHaveExplicitErrors() throws Exception {
        mvc.perform(get("/morong/xuat/ketqua.xlsx").param("diemTu", "9").param("diemDen", "1").with(user("search-chief").roles("TRUONGKHOA"))).andExpect(status().isBadRequest());
        mvc.perform(get("/morong/xuat/dotdangky.pdf").param("thoiGianTu", "2026-10-20").param("thoiGianDen", "2026-10-01").with(user("search-chief").roles("TRUONGKHOA"))).andExpect(status().isBadRequest());
        mvc.perform(get("/morong/xuat/detai.xlsx").param("sort", "id,asc", "id,desc").with(user("search-chief").roles("TRUONGKHOA"))).andExpect(status().isBadRequest());
        mvc.perform(get("/morong/xuat/taikhoan.xlsx").with(user("search-chief").roles("TRUONGKHOA"))).andExpect(status().isNotFound());
        mvc.perform(get("/morong/xuat/detai.csv").with(user("search-chief").roles("TRUONGKHOA"))).andExpect(status().isNotFound());
        mvc.perform(get("/morong/xuat/thongke.pdf").param("dotdangkyid", Long.toString(Long.MAX_VALUE)).with(user("search-chief").roles("TRUONGKHOA"))).andExpect(status().isNotFound());
    }

    @Test void limitsAreCheckedAtDatabaseBeforeMaterializingFullResultSet() throws Exception {
        em.flush();
        jdbc.update("insert into detai (Dotdangkyid,Bomonid,Tendetai,Giangviendexuatid,Trangthai) "
                + "select ?,?,'Bản ghi kiểm thử giới hạn',?,'CONGBO' from system_range(1,10001)", roundA.getId(), departmentA.getId(), teacherA.getId());
        for (Format f : Format.values()) mvc.perform(get(url(Dataset.DETAI, f)).with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().is(422)).andExpect(content().string(org.hamcrest.Matchers.containsString("vượt giới hạn")))
                .andExpect(header().doesNotExist("Content-Disposition"));
    }

    @Test void linksKeepEveryAppliedFilterAndExposeOnlyRealDownloadRoutes() throws Exception {
        var result = mvc.perform(get("/morong/timkiem/detai").param("keyword", "Tiếng Việt + 100% & ' =").param("sort", "tendetai,asc")
                        .param("page", "2").param("size", "1").with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andReturn();
        var options = (SearchModels.ExportOptions) result.getModelAndView().getModel().get("exportOptions");
        assertThat(options.isEnabled()).isTrue(); assertThat(options.getFormats()).containsExactly("xlsx", "pdf");
        assertThat(options.getExcelUrl()).startsWith("/morong/xuat/detai.xlsx?").doesNotContain("page=", "size=");
        var params = UriComponentsBuilder.fromUriString(options.getExcelUrl()).build().getQueryParams();
        assertThat(java.net.URLDecoder.decode(params.getFirst("keyword"), java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("Tiếng Việt + 100% & ' =");
        mvc.perform(get(URI.create(options.getExcelUrl())).with(user("search-chief").roles("TRUONGKHOA"))).andExpect(status().isOk());
        var stats = mvc.perform(get("/morong/thongke").param("dotdangkyid", roundA.getId().toString()).with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andReturn();
        var statsOptions = (StatisticsModels.ExportOptions) stats.getModelAndView().getModel().get("exportOptions");
        assertThat(statsOptions.isEnabled()).isTrue();
        assertThat(statsOptions.getPdfUrl()).isEqualTo("/morong/xuat/thongke.pdf?dotdangkyid=" + roundA.getId());
    }

    @Test void exportingAllDatasetsInBothFormatsDoesNotMutateAnyTable() throws Exception {
        em.flush(); var before = snapshot();
        for (var dataset : Dataset.values()) for (var format : Format.values()) {
            mvc.perform(get(url(dataset, format)).with(user("search-chief").roles("TRUONGKHOA"))).andExpect(status().isOk());
        }
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test void directExportDataServiceRejectsMissingOrWrongFilterTypes() {
        assertThatThrownBy(() -> data.load(Dataset.THONGKE, new DetaiFilterDTO(), Format.PDF)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> data.load(Dataset.DETAI, null, Format.XLSX)).isInstanceOf(IllegalArgumentException.class);
    }
    Map<String, List<Map<String, Object>>> snapshot() {
        var tables = jdbc.queryForList("select TABLE_NAME from INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA='PUBLIC' order by TABLE_NAME", String.class);
        Map<String, List<Map<String, Object>>> rows = new LinkedHashMap<>();
        for (String t : tables) rows.put(t, jdbc.queryForList("select * from " + t)); return rows;
    }
    @ParameterizedTest @MethodSource("outputs")
    void anonymousUsesExistingLoginFlowAndPostIsDenied(Dataset dataset, Format format) throws Exception {
        mvc.perform(get(url(dataset, format)).with(anonymous())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(post(url(dataset, format)).with(user("search-chief").roles("TRUONGKHOA")).with(csrf())).andExpect(status().isForbidden());
    }
    String contentText(byte[] bytes, Format format) throws Exception {
        if (format == Format.PDF) try (var doc = Loader.loadPDF(bytes)) { return new PDFTextStripper().getText(doc); }
        try (var wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            StringBuilder result = new StringBuilder(); var formatter = new DataFormatter(Locale.ROOT);
            for (var sheet : wb) for (var row : sheet) for (var cell : row) result.append(formatter.formatCellValue(cell)).append('\n');
            return result.toString();
        }
    }
}
