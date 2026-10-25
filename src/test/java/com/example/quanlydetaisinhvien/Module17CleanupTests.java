package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.service.KetquaService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class Module17CleanupTests extends MorongSearchTestSupport {
    @Autowired KetquaService resultService;
    static final List<String> REMOVED = List.of("/morong/timkiem/dotdangky", "/morong/timkiem/ketqua",
            "/morong/xuat/dotdangky.xlsx", "/morong/xuat/dotdangky.pdf", "/morong/xuat/ketqua.xlsx", "/morong/xuat/ketqua.pdf");
    @BeforeEach void realViews() {
        account("cleanup-head", Vaitro.TRUONGBOMON); em.flush();
        var real = context.getBeansOfType(ViewResolver.class).values().stream()
                .filter(v -> v.getClass().getName().contains("ThymeleafViewResolver")).findFirst().orElseThrow();
        ReflectionTestUtils.setField(mvc.getDispatcherServlet(), "viewResolvers", List.of(real));
    }
    String username(String role) {
        return switch (role) { case "SINHVIEN" -> "search-student-a"; case "GIANGVIEN" -> "search-teacher-a";
            case "TRUONGBOMON" -> "cleanup-head"; default -> "search-chief"; };
    }
    @ParameterizedTest @ValueSource(strings={"TRUONGKHOA","TRUONGBOMON","GIANGVIEN","SINHVIEN","UNKNOWN"})
    void retiredSearchAndExportsAre404ForEveryAuthenticatedRoleEvenWithForgedFilters(String role) throws Exception {
        for (String path : REMOVED) {
            var result = mvc.perform(get(path).param("username", "search-chief").param("role", "TRUONGKHOA")
                    .param("dacongbo", "false").param("dotdangkyid", roundD.getId().toString())
                    .with(user(username(role)).roles(role)))
                    .andExpect(status().isNotFound()).andReturn();
            // Spring may add inline;filename=f.txt for an unknown extension (RFD defense). No report attachment is served.
            assertThat(result.getResponse().getHeader("Content-Disposition")).satisfies(disposition -> {
                if (disposition != null) assertThat(disposition).doesNotContain("attachment");
            });
            assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                    .doesNotContain(topicA.getTendetai(), topicC.getTendetai(), topicD.getTendetai());
        }
    }
    @Test void noRetiredHandlersOrTemplatesRemainAndPostCannotWrite() throws Exception {
        for (var mapping : context.getBeansOfType(RequestMappingHandlerMapping.class).values())
            for (var entry : mapping.getHandlerMethods().entrySet())
                assertThat(entry.getKey().getPatternValues()).doesNotContainAnyElementsOf(REMOVED);
        for (String name : List.of("morong/timkiem-dotdangky.html", "morong/timkiem-ketqua.html",
                "morong-timkiem-dotdangky.html", "morong-timkiem-ketqua.html"))
            assertThat(context.getResource("classpath:/templates/" + name).exists()).isFalse();
        var before = snapshot();
        for (String path : REMOVED) mvc.perform(post(path).with(user("search-chief").roles("TRUONGKHOA")).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(snapshot()).isEqualTo(before);
    }
    @ParameterizedTest @ValueSource(strings={"TRUONGKHOA","TRUONGBOMON","GIANGVIEN","SINHVIEN"})
    void realSidebarHasOnlyStatisticsInUtilitiesAndKeepsExistingNavigation(String role) throws Exception {
        var response = mvc.perform(get("/").with(user(username(role)).roles(role))).andExpect(status().isOk()).andReturn();
        String body = response.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("/thongbao", "Đăng xuất")
                .doesNotContain("TRA CỨU &amp; TIỆN ÍCH", "Tra cứu đợt / kết quả", "Tìm kiếm nâng cao", "morong-tabs");
        for (String path : REMOVED) assertThat(body).doesNotContain("href=\"" + path);
        if (role.equals("TRUONGKHOA")) {
            var utilities = Pattern.compile("(?s)>TIỆN ÍCH</div>(.*?)<div class=\"nav-section\">TÀI KHOẢN</div>").matcher(body);
            assertThat(utilities.find()).isTrue();
            assertThat(org.springframework.web.util.HtmlUtils.htmlUnescape(utilities.group(1))).contains("/morong/thongke", "Thống kê & Báo cáo");
            assertThat(Pattern.compile("<a\\b").matcher(utilities.group(1)).results().count()).isEqualTo(1);
            assertThat(body).contains("/dotdangky", "/detai", "/hoidong", "/phanbien", "/diem", "/ketqua", "/taikhoan");
        } else assertThat(body).doesNotContain(">TIỆN ÍCH</div>", "href=\"/morong/thongke\"");
        if (!role.equals("SINHVIEN")) assertThat(body).contains("/detai", "/hoidong", "/phanbien", "/diem");
        else assertThat(body).contains("/ketqua").doesNotContain("href=\"/detai\"");
    }
    @Test void roundManagementKeepsItsOriginalSearchAndStageOneActions() throws Exception {
        var response = mvc.perform(get("/dotdangky").param("q", "Khóa luận").with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andExpect(view().name("dotdangky-list")).andReturn();
        assertThat((List<?>)response.getModelAndView().getModel().get("items")).extracting("id").containsExactly(roundB.getId());
        String body = response.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).contains("/giai-doan-1/dot/tao").doesNotContain("morong-tabs", "/morong/timkiem/");
    }
    @ParameterizedTest @ValueSource(strings={"TRUONGKHOA","TRUONGBOMON","GIANGVIEN","SINHVIEN"})
    void originalRoundAndResultRoutesKeepTheirReadPermissions(String role) throws Exception {
        mvc.perform(get("/dotdangky").with(user(username(role)).roles(role))).andExpect(status().is(role.equals("TRUONGKHOA") ? 200 : 403));
        boolean reader = role.equals("TRUONGKHOA") || role.equals("SINHVIEN");
        var response = mvc.perform(get("/ketqua").with(user(username(role)).roles(role))).andExpect(status().is(reader ? 200 : 403)).andReturn();
        if (role.equals("SINHVIEN")) {
            assertThat((List<?>)response.getModelAndView().getModel().get("items")).extracting("id").containsExactly(resultB.getId(), resultA.getId());
            assertThat(response.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains(topicA.getTendetai(), topicB.getTendetai())
                    .doesNotContain(topicC.getTendetai(), topicD.getTendetai());
        }
    }
    @ParameterizedTest @ValueSource(strings={"search-student-a","search-member","search-no-group","missing-profile"})
    void module16StillScopesPublishedResultsToMembership(String name) {
        auth(name,"SINHVIEN");
        var visible = resultService.findVisiblePage(PageRequest.of(0,20)).getContent();
        var expected = switch(name) {case "search-student-a" -> List.of(resultB.getId(),resultA.getId());
            case "search-member" -> List.of(resultA.getId()); default -> List.<Long>of();};
        assertThat(visible).extracting(Ketqua::getId).containsExactlyElementsOf(expected);
        resultA.setDacongbo(false);em.flush();
        assertThat(resultService.findVisiblePage(PageRequest.of(0,20)).getContent()).extracting(Ketqua::getId)
                .doesNotContain(resultA.getId(),otherResult.getId(),draft.getId());
    }
    @Test void removalOfMembershipAndForgedQueryParametersCannotExpandModule16Scope() throws Exception {
        em.createQuery("delete from Thanhviennhom t where t.nhomid=:g and t.sinhvienid=:s")
                .setParameter("g",registrationA.getNhomid()).setParameter("s",studentA.getId()).executeUpdate();
        var result = mvc.perform(get("/ketqua").param("role","TRUONGKHOA").param("username","search-chief").param("dacongbo","false")
                .with(user("search-student-a").roles("SINHVIEN"))).andExpect(status().isOk()).andReturn();
        assertThat((List<?>)result.getModelAndView().getModel().get("items")).extracting("id").containsExactly(resultB.getId());
    }
    Map<String,List<Map<String,Object>>> snapshot() {
        Map<String,List<Map<String,Object>>> rows = new LinkedHashMap<>();
        for (String table : jdbc.queryForList("select TABLE_NAME from INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA='PUBLIC' order by TABLE_NAME",String.class))
            rows.put(table,jdbc.queryForList("select * from " + table));
        return rows;
    }
}
