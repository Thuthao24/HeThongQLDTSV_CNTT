package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.morong.*;
import com.example.quanlydetaisinhvien.dto.morong.ExportModels.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.service.morong.ThongkeService;
import com.example.quanlydetaisinhvien.service.morong.export.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.util.HtmlUtils;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real Thymeleaf + H2 + POI/PDFBox: scope is identical across every statistics representation. */
class StatisticsReportUiTests extends MorongSearchTestSupport {
    @Autowired ThongkeService statistics;
    @Autowired ReportDataService reports;
    @Autowired PdfReportWriter pdf;
    static final Path QA = Path.of("target/module17b-qa");

    @BeforeEach void renderRealStatisticsTemplate() {
        var real = context.getBeansOfType(ViewResolver.class).values().stream()
                .filter(v -> v.getClass().getName().contains("ThymeleafViewResolver")).findFirst().orElseThrow();
        ReflectionTestUtils.setField(mvc.getDispatcherServlet(), "viewResolvers", List.of(real));
    }
    Long roundId(String scope) {
        return switch(scope) { case "A" -> roundA.getId(); case "B" -> roundB.getId();
            case "C" -> roundC.getId(); case "D" -> roundD.getId(); case "empty" -> emptyRound.getId(); default -> null; };
    }
    ThongkeFilterDTO filter(Long id) { var f=new ThongkeFilterDTO();f.setDotdangkyid(id);return f; }
    String page(Long id) throws Exception {
        var request=get("/morong/thongke").with(user("search-chief").roles("TRUONGKHOA"));
        if(id!=null)request.param("dotdangkyid",id.toString());
        return mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
    long count(String text,String regex) { return Pattern.compile(regex).matcher(text).results().count(); }
    String kpi(String html,String key) {
        var match=Pattern.compile("data-kpi=\""+key+"\"[^>]*>([^<]+)<").matcher(html);
        assertThat(match.find()).isTrue();return match.group(1);
    }
    byte[] download(String html,String format) throws Exception {
        var match=Pattern.compile("href=\"([^\"]*/morong/xuat/thongke\\."+format+"[^\"]*)\"").matcher(html);
        assertThat(match.find()).isTrue();String href=HtmlUtils.htmlUnescape(match.group(1));
        return mvc.perform(get(href).with(user("search-chief").roles("TRUONGKHOA"))).andExpect(status().isOk())
                .andExpect(content().contentType(format.equals("pdf")?Format.PDF.mime:Format.XLSX.mime))
                .andReturn().getResponse().getContentAsByteArray();
    }
    @ParameterizedTest @ValueSource(strings={"all","A","B","C","D","empty"})
    void fourKpisTwoChartsAndFiveColumnTableMatchTheSelectedRound(String scope) throws Exception {
        Long id=roundId(scope);var expected=statistics.statistics(filter(id));var s=expected.summary();String html=page(id);
        assertThat(html).contains("<!DOCTYPE html>","THỐNG KÊ &amp; BÁO CÁO", "data-statistics")
                .doesNotContain("th:replace=", "Xem thống kê", "Đặt lại", "BỘ LỌC PHẠM VI", "Phân tích số liệu", "Quy tắc nghiệp vụ");
        assertThat(count(html,"data-kpi=")).isEqualTo(4);assertThat(count(html,"data-chart=")).isEqualTo(2);
        assertThat(kpi(html,"topics")).isEqualTo(""+s.tongDetai());assertThat(kpi(html,"groups")).isEqualTo(""+s.tongNhom());
        assertThat(kpi(html,"registrations")).isEqualTo(""+s.tongDangky());
        assertThat(kpi(html,"publication")).isEqualTo(s.daCongbo()+" / "+s.tongKetqua());
        assertThat(count(html,"<th scope=\"col\"")).isEqualTo(5);
        assertThat(count(html,"data-round-id=")).isEqualTo(expected.byRound().size());
        for(var row:expected.byRound())assertThat(html).contains("data-round-id=\""+row.id()+"\"", row.tendot());
        for(var status:expected.detaiByStatus()) assertThat(html).contains("data-status=\""+status.code()+"\" data-count=\""+status.count()+"\" data-percentage=\""+status.percentage()+"\"");
    }
    @ParameterizedTest @ValueSource(strings={"all","A","B","C","D","empty"})
    void renderedExportLinksProduceTwoSheetsAndConcisePdfWithExactlyTheSameScope(String scope) throws Exception {
        Long id=roundId(scope);var expected=statistics.statistics(filter(id));String html=page(id);
        String label=id==null?"Tất cả đợt đăng ký":expected.byRound().getFirst().tendot();
        for(String format:List.of("xlsx","pdf")) {
            byte[] bytes=download(html,format);String text;
            if(format.equals("xlsx"))try(var workbook=WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
                assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
                assertThat(workbook.getSheetName(0)).isEqualTo("Tổng hợp");assertThat(workbook.getSheetName(1)).isEqualTo("Theo đợt");
                var out=new StringBuilder();workbook.forEach(sheet->sheet.forEach(row->row.forEach(cell->{
                    assertThat(cell.getCellType()).isNotEqualTo(CellType.FORMULA);out.append(cell).append('\n');
                })));text=out.toString();
                var sheet=workbook.getSheetAt(1);assertThat(sheet.getRow(4).getPhysicalNumberOfCells()).isEqualTo(5);
                assertThat(sheet.getLastRowNum()-4).isEqualTo(expected.byRound().size());
                for(int r=0;r<expected.byRound().size();r++) {
                    var row=expected.byRound().get(r);var cells=sheet.getRow(5+r);
                    assertThat(cells.getCell(0).getStringCellValue()).isEqualTo(row.tendot());
                    assertThat(cells.getCell(1).getNumericCellValue()).isEqualTo(row.soDetai());
                    assertThat(cells.getCell(2).getNumericCellValue()).isEqualTo(row.soNhom());
                    assertThat(cells.getCell(3).getNumericCellValue()).isEqualTo(row.soDangky());
                    assertThat(cells.getCell(4).getStringCellValue()).isEqualTo(row.soDaCongbo()+" / "+row.soKetqua());
                }
            } else try(var doc=Loader.loadPDF(bytes)) {
                assertThat(doc.getNumberOfPages()).isEqualTo(1);text=new PDFTextStripper().getText(doc);
                assertThat(doc.getPage(0).getMediaBox().getHeight()).isGreaterThan(doc.getPage(0).getMediaBox().getWidth());
                Files.createDirectories(QA);Files.write(QA.resolve("statistics-"+scope+".pdf"),bytes);
                ImageIO.write(new PDFRenderer(doc).renderImageWithDPI(0,110),"png",QA.resolve("statistics-"+scope+".png").toFile());
            }
            assertThat(text).contains("THỐNG KÊ & BÁO CÁO", "Phạm vi: "+label, "Thời điểm xuất:","Công bố kết quả", "Kết quả đã công bố");
            if(id!=null)assertThat(text).doesNotContain("Phạm vi: Tất cả đợt đăng ký");
            assertThat(text).contains(expected.summary().daCongbo()+" / "+expected.summary().tongKetqua());
            if(expected.summary().ketquaThieuDiemCuoi()>0)assertThat(text).contains("Kết quả thiếu điểm");
            if(expected.summary().ketquaThieuDiemCuoi()==0 && expected.summary().nhomChuaDangky()==0)assertThat(text).doesNotContain("Cần xử lý");
        }
    }
    @Test void repeatedGetReloadAndHistoryTargetsAlwaysRenderTheirOwnScope() throws Exception {
        for(String scope:List.of("all","A","B","empty","D","B","A","all","A")) {
            Long id=roundId(scope);String first=page(id);String repeated=page(id);
            assertThat(repeated.substring(repeated.indexOf("<main"),repeated.indexOf("</main>"))).isEqualTo(first.substring(first.indexOf("<main"),first.indexOf("</main>")));
            if(id!=null)assertThat(first).contains("data-selected-round=\""+id+"\"");
            else assertThat(first).contains("<option value=\"\" selected=\"selected\">");
            if(id!=null)assertThat(first).contains("/morong/xuat/thongke.pdf?dotdangkyid="+id);
        }
    }
    @Test void noRoundsAndNoResultsRenderZerosAndNoMeaninglessPublicationPercentage() throws Exception {
        for(String entity:List.of("Ketqua","Thanhviennhom","Dangkydetai","Nhomsinhvien","Giangvienhuongdan","Detai","Dotdangky"))em.createQuery("delete from "+entity).executeUpdate();
        String html=page(null);assertThat(kpi(html,"publication")).isEqualTo("0 / 0");
        assertThat(html).contains("Không có dữ liệu").doesNotContain("statistics-attention", "statistics-ratio\"");
        assertThat(count(html,"data-round-id=")).isZero();
        for(String type:List.of("pdf","xlsx"))assertThat(download(html,type)).isNotEmpty();
    }
    @Test void topicAndResultPercentagesHaveIndependentDenominatorsAndOnlyRealActionCounts() throws Exception {
        for(int i=0;i<7;i++)topic("Đề tài thêm "+i,null,roundA,departmentA,teacherA,TrangthaiDetai.DADUYET);
        var group=new Nhomsinhvien();group.setDotdangkyid(roundA.getId());group.setNhomtruongid(studentA.getId());group.setTennhom("Chưa đăng ký");em.persist(group);em.flush();
        String html=page(null);assertThat(html).contains("data-status=\"DACONGBO\"", "data-percentage=\"75.00\"", "data-percentage=\"9.09\"", "Cần xử lý");
        assertThat(kpi(html,"publication")).isEqualTo("3 / 4");
        auth("search-chief","TRUONGKHOA");
        var report=reports.load(Dataset.THONGKE,filter(null),Format.XLSX);
        assertThat(report.sections().getFirst().rows()).contains(List.of("Nhóm chưa đăng ký",1L),List.of("Kết quả thiếu điểm",1L));
    }
    @Test void manyRoundsPaginateWithoutBlankPagesOrMissingRowsAndPreserveLongUnicodeNames() throws Exception {
        for(int i=0;i<65;i++)round("Đợt bổ sung "+i+" – Tiếng Việt",Loaidot.MONHOC,TrangthaiDotdangky.NHAP,10);
        roundA.setTendot("Đợt tiếng Việt dài ".repeat(10));em.flush();
        var report=reports.load(Dataset.THONGKE,filter(null),Format.PDF);byte[] bytes=pdf.write(report);
        try(var document=Loader.loadPDF(bytes)) {
            assertThat(document.getNumberOfPages()).isBetween(3,6);var stripper=new PDFTextStripper();String text=stripper.getText(document);
            for(int i=0;i<65;i++)assertThat(text).contains("Đợt bổ sung "+i+" – Tiếng Việt");
            Files.createDirectories(QA);Files.write(QA.resolve("statistics-long.pdf"),bytes);
            for(int p=0;p<document.getNumberOfPages();p++) {
                stripper.setStartPage(p+1);stripper.setEndPage(p+1);assertThat(stripper.getText(document)).contains("Theo đợt").contains("Đợt");
                ImageIO.write(new PDFRenderer(document).renderImageWithDPI(p,90),"png",QA.resolve("statistics-long-"+(p+1)+".png").toFile());
            }
        }
    }
    @Test void untrustedRoundNameIsEscapedInHtmlAndWrittenAsTextInExcel() throws Exception {
        roundA.setTendot("=HYPERLINK(\"x\") <script>alert(1)</script> Tiếng Việt");em.flush();String html=page(roundA.getId());
        assertThat(html).doesNotContain("<script>alert(1)</script>").contains("&lt;script&gt;");
        try(var book=WorkbookFactory.create(new ByteArrayInputStream(download(html,"xlsx")))) {
            var cell=book.getSheet("Theo đợt").getRow(5).getCell(0);
            assertThat(cell.getCellType()).isEqualTo(CellType.STRING);assertThat(cell.getStringCellValue()).isEqualTo(roundA.getTendot());
        }
    }
}
