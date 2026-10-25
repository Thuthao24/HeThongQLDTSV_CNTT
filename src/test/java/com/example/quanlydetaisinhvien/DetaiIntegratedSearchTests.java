package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.morong.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.DetaiRepository;
import com.example.quanlydetaisinhvien.service.DetaiService;
import com.example.quanlydetaisinhvien.service.morong.ExportLinks;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.ViewResolver;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DetaiIntegratedSearchTests extends MorongSearchTestSupport {
    @Autowired DetaiRepository topics;
    @Autowired DetaiService detaiService;
    @BeforeEach void realTemplates() {
        var real = context.getBeansOfType(ViewResolver.class).values().stream()
                .filter(v -> v.getClass().getName().contains("ThymeleafViewResolver")).findFirst().orElseThrow();
        ReflectionTestUtils.setField(mvc.getDispatcherServlet(), "viewResolvers", List.of(real));
    }
    String html(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
    @ParameterizedTest @ValueSource(strings={"sinh viên", "100%", "%", "_", "!", "' OR '1'='1", "  ", "\\"})
    void legacyQMatchesOriginalLikeQueryAndRoundTrips(String q) throws Exception {
        var expected = q.trim().isEmpty() ? topics.findAll(Sort.by("id").descending())
                : topics.searchPage(q.trim(), PageRequest.of(0, 25, Sort.by("id").descending())).getContent();
        var result = mvc.perform(get("/detai").param("q", q).with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andReturn();
        assertThat((List<?>)result.getModelAndView().getModel().get("items")).extracting("id")
                .containsExactlyElementsOf(expected.stream().map(Detai::getId).toList());
        if (!q.trim().isEmpty()) assertThat(html(result)).contains("name=\"q\"")
                .doesNotContain("name=\"keyword\"");
    }
    @Test void aliasesHaveAnExplicitValidatedContractIncludingExports() throws Exception {
        for (String path : List.of("/detai", "/morong/timkiem/detai", "/morong/xuat/detai.xlsx", "/morong/xuat/detai.pdf")) {
            mvc.perform(get(path).param("q", "%").param("keyword", "different")
                    .with(user("search-chief").roles("TRUONGKHOA"))).andExpect(status().isBadRequest());
        }
        auth("search-chief", "TRUONGKHOA");
        var q = new DetaiFilterDTO(); q.setQ("%"); q.setKeyword("%");
        assertThat(service.detai(q).items()).hasSize(4);
        var literal = new DetaiFilterDTO(); literal.setKeyword("%");
        assertThat(service.detai(literal).items()).extracting(SearchModels.DetaiRow::id).containsExactly(topicA.getId());
    }
    @Test void combinedFiltersAndBothExportsShareScopeAndOrderBeyondCurrentPage() throws Exception {
        var second = topic("Thêm sinh viên Tiếng Việt", "Dữ liệu dài " + "rất dài ".repeat(28), roundA, departmentA, teacherA, TrangthaiDetai.CONGBO);
        guide(second, teacherB); em.flush();
        var result = mvc.perform(get("/detai").param("keyword", "sinh viên").param("dotdangkyid", roundA.getId().toString())
                .param("bomonid", departmentA.getId().toString()).param("loaidot", "MONHOC").param("trangthai", "CONGBO")
                .param("giangviendexuatid", teacherA.getId().toString()).param("giangvienhuongdanid", teacherB.getId().toString())
                .param("sort", "tendetai,asc").param("size", "1").with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andReturn();
        var model = result.getModelAndView().getModel();
        assertThat((List<?>)model.get("items")).extracting("id").containsExactly(topicA.getId());
        assertThat((Map<Long, String>) model.get("roundNames")).containsEntry(roundA.getId(), roundA.getTendot());
        assertThat((Map<Long, String>) model.get("departmentNames")).containsEntry(departmentA.getId(), departmentA.getTenbomon());
        assertThat((Map<Long, String>) model.get("lecturerNames")).containsEntry(teacherA.getId(), "Kiểm thử search-teacher-a");
        var next = (String)model.get("nextUrl");
        for (String field : ExportLinks.fields(ExportModels.Dataset.DETAI)) {
            if (!Set.of("q").contains(field)) assertThat(next).contains(field + "=");
        }
        var following = mvc.perform(get(URI.create(next)).with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andReturn();
        assertThat((List<?>)following.getModelAndView().getModel().get("items")).extracting("id").containsExactly(second.getId());
        var exports = (SearchModels.ExportOptions)model.get("exportOptions");
        for (String url : List.of(exports.excelUrl(), exports.pdfUrl())) {
            assertThat(url).doesNotContain("page=", "size=");
            var bytes = mvc.perform(get(URI.create(url)).with(user("search-chief").roles("TRUONGKHOA")))
                    .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                    .andReturn().getResponse().getContentAsByteArray();
            String text = text(bytes, url.contains(".pdf?"));
            assertThat(text).contains(topicA.getTendetai(), second.getTendetai()).doesNotContain(topicB.getTendetai(), topicC.getTendetai(), topicD.getTendetai());
            assertThat(text.indexOf(topicA.getTendetai())).isLessThan(text.indexOf(second.getTendetai()));
        }
    }
    @ParameterizedTest @ValueSource(strings={"TRUONGKHOA", "TRUONGBOMON", "GIANGVIEN", "SINHVIEN", "UNKNOWN"})
    void mainReadRolesAndExistingStageOneButtonsArePreserved(String role) throws Exception {
        var a = account("integrated-" + role, Vaitro.valueOf(role.equals("UNKNOWN") ? "GIANGVIEN" : role));
        if (role.equals("GIANGVIEN") || role.equals("TRUONGBOMON")) {
            var t = new Giangvien(); t.setTaikhoanid(a.getId()); t.setBomonid(departmentA.getId()); t.setMagiangvien("i-" + role); em.persist(t);
            topicD.setGiangviendexuatid(t.getId());
        }
        em.flush();
        boolean allowed = Set.of("TRUONGKHOA", "TRUONGBOMON", "GIANGVIEN").contains(role);
        var response = mvc.perform(get("/detai").with(user(a.getTendangnhap()).roles(role)))
                .andExpect(status().is(allowed ? 200 : 403)).andReturn();
        for (String format : List.of("xlsx", "pdf")) mvc.perform(get("/morong/xuat/detai." + format)
                .with(user(a.getTendangnhap()).roles(role))).andExpect(status().is(allowed ? 200 : 403));
        if (!allowed) return;
        String body = html(response);
        assertThat(body).contains("/giai-doan-1/detai/" + topicD.getId() + "/sua", "/giai-doan-1/detai/" + topicD.getId() + "/xoa", "name=\"_csrf\"");
        for (String label : List.of("Duyệt", "Từ chối", "Công bố")) {
            boolean visible = Pattern.compile(">\\s*" + Pattern.quote(label) + "\\s*</button>").matcher(body).find();
            assertThat(visible).as("Button %s for %s", label, role).isEqualTo(role.equals("TRUONGKHOA"));
        }
        if (!role.equals("TRUONGKHOA")) assertThat(body).doesNotContain("/giai-doan-1/detai/" + topicC.getId() + "/sua");
        assertThat(Pattern.compile("<a[^>]*href=\"/detai\"[^>]*class=\"nav-item").matcher(body).results().count()).isEqualTo(1);
        assertThat(body).doesNotContain("href=\"/morong/timkiem/detai");
    }
    @Test void csrfAndForgedStageOneActionsRemainDenied() throws Exception {
        for (String action : List.of("xoa", "duyet", "congbo")) {
            var path = "/giai-doan-1/detai/" + topicD.getId() + "/" + action;
            mvc.perform(post(path).param("chapnhan", "true").with(user("search-chief").roles("TRUONGKHOA")))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/giai-doan-1/detai/" + topicD.getId() + "/sua").with(user("search-teacher-b").roles("GIANGVIEN")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/giai-doan-1/detai/" + topicD.getId() + "/duyet").param("chapnhan", "true")
                .with(user("search-teacher-a").roles("GIANGVIEN")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/giai-doan-1/detai/" + topicD.getId() + "/xoa")
                .with(user("search-teacher-b").roles("GIANGVIEN")).with(csrf())).andExpect(status().isForbidden());
        assertThat(topics.findById(topicD.getId()).orElseThrow().getTrangthai()).isEqualTo(TrangthaiDetai.CHODUYET);
    }
    @Test void oldUrlRedirectPreservesEncodedFiltersPageAndContextWithoutOpenRedirect() throws Exception {
        for (String prefix : List.of("", "/truong")) {
            var request = get(prefix + "/morong/timkiem/detai").contextPath(prefix)
                    .param("keyword", "Tiếng Việt + % _ !").param("dotdangkyid", roundA.getId().toString())
                    .param("page", "2").param("size", "37").param("sort", "tendetai,asc")
                    .with(user("search-chief").roles("TRUONGKHOA"));
            var location = mvc.perform(request).andExpect(status().isFound()).andReturn().getResponse().getRedirectedUrl();
            assertThat(location).startsWith(prefix + "/detai?").contains("page=2", "size=37", "%2B", "%25", "sort=tendetai%2Casc");
            mvc.perform(get(URI.create(location)).contextPath(prefix).with(user("search-chief").roles("TRUONGKHOA")))
                    .andExpect(status().isOk());
        }
        mvc.perform(get("/morong/timkiem/detai").param("redirect", "https://evil.invalid")
                .with(user("search-chief").roles("TRUONGKHOA"))).andExpect(status().isBadRequest());
    }
    @ParameterizedTest @ValueSource(strings={"page", "size", "sort", "keyword", "q", "dotdangkyid"})
    void duplicateParametersAreRejectedOnListLegacyAndExport(String field) throws Exception {
        for (String path : List.of("/detai", "/morong/timkiem/detai", "/morong/xuat/detai.xlsx"))
            mvc.perform(get(path).param(field, "1", "2").with(user("search-chief").roles("TRUONGKHOA")))
                    .andExpect(status().isBadRequest());
    }
    @Test void defaultPagingAndBoundedAdapterDoNotWriteOrLoadEveryTopic() throws Exception {
        for (int i = 0; i < 32; i++) topic("Phân trang " + i, "", roundA, departmentA, teacherA, TrangthaiDetai.CONGBO);
        em.flush(); em.clear();
        var statistics = em.getEntityManagerFactory().unwrap(org.hibernate.SessionFactory.class).getStatistics();
        var f = new DetaiFilterDTO(); f.setSize(25); statistics.clear();
        var page = detaiService.searchPage(f);
        assertThat(page.getContent()).hasSize(25).isSortedAccordingTo(Comparator.comparing(Detai::getId).reversed());
        assertThat(page.getTotalElements()).isEqualTo(36);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
        assertThat(statistics.getEntityInsertCount() + statistics.getEntityUpdateCount() + statistics.getEntityDeleteCount()).isZero();
        var response = mvc.perform(get("/detai").with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andReturn();
        var info = (SearchModels.PageInfo)response.getModelAndView().getModel().get("pageInfo");
        assertThat(info.size()).isEqualTo(25);
        assertThat(html(response)).contains("id%2Cdesc", "page=1", "size=25");
    }
    @ParameterizedTest @ValueSource(ints={3,4,5})
    void groupMembershipDoesNotMultiplyTopicsStatisticsOrExports(int members) throws Exception {
        long existing = jdbc.queryForObject("select count(*) from Thanhviennhom where nhomid=?", Long.class, registrationA.getNhomid());
        for (int i = (int)existing; i < members; i++) {
            var student = student("integrated-member-" + i);
            var member = new Thanhviennhom(); member.setNhomid(registrationA.getNhomid()); member.setSinhvienid(student.getId()); em.persist(member);
        }
        em.flush();
        var filter = new DetaiFilterDTO(); filter.setGiangvienhuongdanid(teacherB.getId());
        assertThat(service.detai(filter).items()).extracting(SearchModels.DetaiRow::id).containsExactly(topicB.getId(), topicA.getId());
        var stats = context.getBean(com.example.quanlydetaisinhvien.service.morong.ThongkeService.class).statistics(new ThongkeFilterDTO());
        assertThat(stats.summary().tongDetai()).isEqualTo(4);
        assertThat(stats.summary().tongNhom()).isEqualTo(4);
        assertThat(stats.summary().tongDangky()).isEqualTo(4);
        assertThat(stats.summary().tongKetqua()).isEqualTo(4);
        for (String format : List.of("xlsx", "pdf")) {
            var bytes = mvc.perform(get("/morong/xuat/detai." + format).param("q", "%").with(user("search-chief").roles("TRUONGKHOA")))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
            String text = text(bytes, format.equals("pdf"));
            assertThat(text.split(Pattern.quote(topicA.getTendetai()), -1)).hasSize(2);
        }
    }
    @Test void searchPaginationRedirectAndExportsLeaveEveryTableUnchanged() throws Exception {
        em.flush();
        var before = snapshot();
        for (String path : List.of("/detai", "/morong/timkiem/detai", "/morong/xuat/detai.xlsx", "/morong/xuat/detai.pdf")) {
            mvc.perform(get(path).param("q", "%").param("sort", "id,asc").param("page", "1").param("size", "1")
                    .with(user("search-chief").roles("TRUONGKHOA"))).andExpect(status().is(path.contains("timkiem") ? 302 : 200));
        }
        em.flush();
        assertThat(snapshot()).isEqualTo(before);
    }
    Map<String, List<Map<String, Object>>> snapshot() {
        var tables = jdbc.queryForList("select TABLE_NAME from INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA='PUBLIC' order by TABLE_NAME", String.class);
        Map<String, List<Map<String, Object>>> rows = new LinkedHashMap<>();
        for (String table : tables) rows.put(table, jdbc.queryForList("select * from " + table));
        return rows;
    }
    String text(byte[] bytes, boolean pdf) throws Exception {
        if (pdf) try (var doc = org.apache.pdfbox.Loader.loadPDF(bytes)) { return new org.apache.pdfbox.text.PDFTextStripper().getText(doc); }
        try (var book = org.apache.poi.ss.usermodel.WorkbookFactory.create(new java.io.ByteArrayInputStream(bytes))) {
            var out = new StringBuilder(); book.forEach(sheet -> sheet.forEach(row -> row.forEach(cell -> out.append(cell).append('\n')))); return out.toString();
        }
    }
}
