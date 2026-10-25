package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.morong.ExportModels.*;
import com.example.quanlydetaisinhvien.service.morong.export.PdfReportWriter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.*;

class PdfRowPaginationTests {
    static final Path QA = ExportPresentationTests.QA;
    Report report(List<List<Object>> rows) {
        return new Report("DANH SÁCH ĐỀ TÀI", ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")),
                List.of("Tổng số đề tài: " + rows.size()), List.of(new Section("Đề tài",
                List.of(new Column("ID", Kind.TEXT, 12), new Column("Tên đề tài / Mô tả", Kind.TEXT, 90)), rows)));
    }
    String page(org.apache.pdfbox.pdmodel.PDDocument doc, int page) throws Exception {
        var stripper = new PDFTextStripper(); stripper.setStartPage(page); stripper.setEndPage(page);
        return stripper.getText(doc);
    }
    @ParameterizedTest @ValueSource(ints = {10, 11, 12, 13, 14, 15, 16, 17})
    void ordinaryRowIsNeverSplitEvenWhenOnlyASmallAmountOfHeightIsMissing(int fillers) throws Exception {
        var rows = new ArrayList<List<Object>>();
        for (int i = 0; i < fillers; i++) rows.add(List.of((Object) (i + 1), "Đề tài ngắn " + i));
        String description = "BẮT ĐẦU HÀNG 23\n" + "Tên đề tài dài - Nguyễn Thị Ánh & Đặng Quốc Việt\n".repeat(10) + "KẾT THÚC HÀNG 23";
        rows.add(List.of(23L, description)); rows.add(List.of(24L, "Bản ghi cuối cùng"));
        byte[] bytes = new PdfReportWriter().write(report(rows));
        try (var doc = Loader.loadPDF(bytes)) {
            int containing = 0;
            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                String text = page(doc, p);
                if (text.contains("BẮT ĐẦU HÀNG 23") || text.contains("KẾT THÚC HÀNG 23")) {
                    containing++;
                    assertThat(text).contains("BẮT ĐẦU HÀNG 23", "KẾT THÚC HÀNG 23", "23 BẮT ĐẦU");
                }
                assertThat(text).contains("Tên đề tài / Mô tả", "Trang " + p + " / " + doc.getNumberOfPages());
                assertThat(text).doesNotContain("Tiếp theo");
            }
            assertThat(containing).isEqualTo(1);
            assertThat(new PDFTextStripper().getText(doc)).contains("Bản ghi cuối cùng");
            if (fillers == 12) {
                assertThat(doc.getNumberOfPages()).isEqualTo(2);
                assertThat(page(doc, 1)).doesNotContain("BẮT ĐẦU HÀNG 23");
                Files.createDirectories(QA); Files.write(QA.resolve("detai-boundary-id23.pdf"), bytes);
                render(doc, "boundary-id23");
            }
            assertTextWithinMargins(doc);
        }
    }
    @Test void oversizedRowContinuesWithItsIdAndHeaderWithoutLosingOrRepeatingContent() throws Exception {
        String description = IntStream.range(0, 220).mapToObj(i -> String.format("MỤC%04d - Dữ liệu tiếng Việt & ký tự đặc biệt", i))
                .collect(java.util.stream.Collectors.joining("\n"));
        var bytes = new PdfReportWriter().write(report(List.of(List.of(23L, description), List.of(24L, "Bản ghi tiếp sau"))));
        try (var doc = Loader.loadPDF(bytes)) {
            assertThat(doc.getNumberOfPages()).isBetween(5, 12);
            String full = new PDFTextStripper().getText(doc);
            for (int i = 0; i < 220; i++) assertThat(full.split(String.format("MỤC%04d", i), -1)).hasSize(2);
            assertThat(full).contains("Bản ghi tiếp sau");
            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                String text = page(doc, p);
                assertThat(text).contains("Tên đề tài / Mô tả", "Trang " + p + " / " + doc.getNumberOfPages());
                if (p > 1 && text.contains("MỤC")) assertThat(text).contains("Tiếp theo - Mã đề tài: 23");
            }
            Files.createDirectories(QA); Files.write(QA.resolve("detai-oversized-id23.pdf"), bytes);
            render(doc, "oversized-id23"); assertTextWithinMargins(doc);
        }
    }
    void render(org.apache.pdfbox.pdmodel.PDDocument doc, String name) throws Exception {
        var renderer = new PDFRenderer(doc);
        for (int p : new LinkedHashSet<>(List.of(0, doc.getNumberOfPages() / 2, doc.getNumberOfPages() - 1)))
            ImageIO.write(renderer.renderImageWithDPI(p, 110), "png", QA.resolve(name + "-page" + (p + 1) + ".png").toFile());
    }
    void assertTextWithinMargins(org.apache.pdfbox.pdmodel.PDDocument doc) throws Exception {
        var stripper = new PDFTextStripper() {
            @Override protected void processTextPosition(TextPosition t) {
                assertThat(t.getXDirAdj()).isGreaterThanOrEqualTo(29);
                assertThat(t.getXDirAdj() + t.getWidthDirAdj()).isLessThanOrEqualTo(t.getPageWidth() - 28);
                assertThat(t.getYDirAdj()).isBetween(29f, t.getPageHeight() - 18);
                super.processTextPosition(t);
            }
        };
        stripper.getText(doc);
    }
}
