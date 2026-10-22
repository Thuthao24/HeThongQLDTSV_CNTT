package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.morong.ExportModels.*;
import com.example.quanlydetaisinhvien.service.morong.export.*;
import com.example.quanlydetaisinhvien.exception.ExportLimitException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import javax.imageio.ImageIO;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.*;

class ReportWriterTests {
    final ExcelReportWriter excel = new ExcelReportWriter();
    final PdfReportWriter pdf = new PdfReportWriter();
    Report report(List<Column> columns, List<List<Object>> rows) {
        return new Report("THỐNG KÊ & BÁO CÁO", ZonedDateTime.of(2026, 10, 16, 20, 30, 0, 0, ZoneId.of("Asia/Ho_Chi_Minh")),
                List.of("Phạm vi: Đợt Nghiên cứu khoa học"), List.of(new Section("Dữ liệu", columns, rows)));
    }
    List<Object> row(Object... values) { return Arrays.asList(values); }

    @ParameterizedTest @ValueSource(strings = {"=SUM(1,2)", "+1+1", "-1+1", "@SUM(A1:A2)", "\t=HYPERLINK(\"bad\")"})
    void userTextIsAlwaysAStringAndNeverAnExcelFormula(String payload) throws Exception {
        try (var wb = WorkbookFactory.create(new ByteArrayInputStream(excel.write(report(
                List.of(new Column("Tiêu đề", Kind.TEXT, 40)), List.of(row(payload))))))) {
            var cell = wb.getSheetAt(0).getRow(5).getCell(0);
            assertThat(cell.getCellType()).isEqualTo(CellType.STRING);
            assertThat(cell.getStringCellValue()).isEqualTo(payload);
        }
    }

    @Test void excelPreservesTypedDatesScoresNullsAndLargeIdentifiers() throws Exception {
        var date = LocalDateTime.of(2026, 10, 16, 20, 30);
        var columns = List.of(new Column("ID", Kind.TEXT, 24), new Column("Điểm", Kind.DECIMAL, 15),
                new Column("Ngày", Kind.DATE, 24), new Column("Trống", Kind.TEXT, 20));
        byte[] bytes = excel.write(report(columns, List.of(row(Long.MAX_VALUE, new BigDecimal("8.50"), date, null))));
        try (var wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            var row = wb.getSheetAt(0).getRow(5);
            assertThat(row.getCell(0).getStringCellValue()).isEqualTo(Long.toString(Long.MAX_VALUE));
            assertThat(row.getCell(1).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(row.getCell(1).getNumericCellValue()).isEqualTo(8.50);
            assertThat(row.getCell(1).getCellStyle().getDataFormatString()).isEqualTo("0.00");
            assertThat(row.getCell(2).getLocalDateTimeCellValue()).isEqualTo(date);
            assertThat(row.getCell(2).getCellStyle().getDataFormatString()).isEqualTo("dd/MM/yyyy HH:mm");
            assertThat(row.getCell(3).getCellType()).isEqualTo(CellType.BLANK);
        }
    }

    @Test void pdfVietnameseUnicodeLongWordsAndARowSpanningPagesAreNotClippedOrLost() throws Exception {
        String vietnamese = "Quản lý đề tài sinh viên HCMUTE – Nguyễn Thị Ánh, Đặng Quốc Việt. ";
        String longText = vietnamese.repeat(200) + "X".repeat(1500) + " KẾT THÚC DỮ LIỆU";
        var columns = List.of(new Column("Nội dung tiếng Việt", Kind.TEXT, 70), new Column("Điểm cuối", Kind.DECIMAL, 15), new Column("Trống", Kind.TEXT, 15));
        byte[] bytes = pdf.write(report(columns, List.of(row(longText, new BigDecimal("8.50"), null), row("Unicode 😀 漢字", BigDecimal.ZERO, null))));
        Files.createDirectories(ExportBackendTests.QA); Files.write(ExportBackendTests.QA.resolve("long-unicode.pdf"), bytes);
        try (var doc = Loader.loadPDF(bytes)) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(2);
            var text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("Quản lý đề tài sinh viên", "Nguyễn Thị Ánh", "Đặng Quốc Việt", "KẾT THÚC DỮ LIỆU", "8.50", "0.00", "[U+1F600]");
            assertThat(text.chars().filter(c -> c == 'X').count()).isEqualTo(1500);
            assertThat(text.split("Nội dung tiếng Việt", -1).length - 1).isEqualTo(doc.getNumberOfPages());
            var renderer = new PDFRenderer(doc);
            for (int page : new int[]{0, doc.getNumberOfPages() / 2, doc.getNumberOfPages() - 1}) {
                var image = renderer.renderImageWithDPI(page, 90);
                assertThat(image.getWidth()).isGreaterThan(image.getHeight());
                ImageIO.write(image, "png", ExportBackendTests.QA.resolve("long-page-" + (page + 1) + ".png").toFile());
            }
        }
    }

    @Test void bothWritersRejectOverLimitAndExcelRejectsOversizeCellsInsteadOfTruncating() {
        var col = List.of(new Column("Dữ liệu", Kind.TEXT, 40));
        var rows = IntStream.range(0, 10001).mapToObj(i -> row("Dòng " + i)).toList();
        assertThatThrownBy(() -> excel.write(report(col, rows))).isInstanceOf(ExportLimitException.class);
        assertThatThrownBy(() -> pdf.write(report(col, rows.subList(0, 2001)))).isInstanceOf(ExportLimitException.class);
        assertThatThrownBy(() -> excel.write(report(col, List.of(row("N".repeat(32768)))))).isInstanceOf(ExportLimitException.class);
    }

    @Test void streamingExcelAcceptsTheExactTenThousandRowBoundaryAndReleasesTemporarySheets() throws Exception {
        var directory = Path.of(System.getProperty("java.io.tmpdir"), "poifiles");
        long before = temporarySheets(directory);
        var rows = IntStream.range(0, 10000).mapToObj(i -> row("Dòng " + i)).toList();
        var bytes = excel.write(report(List.of(new Column("Dữ liệu", Kind.TEXT, 30)), rows));
        try (var wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(10004);
            assertThat(wb.getSheetAt(0).getRow(10004).getCell(0).getStringCellValue()).isEqualTo("Dòng 9999");
        }
        assertThat(temporarySheets(directory)).isEqualTo(before);
    }
    long temporarySheets(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) return 0;
        try (var files = Files.list(directory)) { return files.filter(p -> p.getFileName().toString().startsWith("poi-sxssf-sheet")).count(); }
    }

    @Test void pdfAcceptsExactlyTwoThousandRowsAndRetainsTheLastRowOnAPaginatedReport() throws Exception {
        var rows = IntStream.range(0, 2000).mapToObj(i -> row("Dòng số " + i)).toList();
        try (var document = Loader.loadPDF(pdf.write(report(List.of(new Column("Dữ liệu", Kind.TEXT, 60)), rows)))) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            var text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Dòng số 0", "Dòng số 1999", "Trang " + document.getNumberOfPages() + " / " + document.getNumberOfPages());
        }
    }

    @Test void excelFailureAlsoReleasesItsTemporarySheetFiles() throws Exception {
        var directory = Path.of(System.getProperty("java.io.tmpdir"), "poifiles");
        long before = temporarySheets(directory);
        assertThatThrownBy(() -> excel.write(report(List.of(new Column("Dữ liệu", Kind.TEXT, 30)),
                List.of(row("Dữ liệu hợp lệ"), row("N".repeat(32768)))))).isInstanceOf(ExportLimitException.class);
        assertThat(temporarySheets(directory)).isEqualTo(before);
    }
}
