package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.ThongbaoViewDTO;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Database/model/security contract only. All fixtures are rolled back in the existing H2 test DB. */
@TestPropertySource(properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector="
        + "com.example.quanlydetaisinhvien.ThongbaoHeaderTests$SqlCapture")
class ThongbaoHeaderTests extends ThongbaoTestSupport {
    public static class SqlCapture implements StatementInspector {
        static final ThreadLocal<List<String>> SQL = ThreadLocal.withInitial(ArrayList::new);

        @Override public String inspect(String sql) {
            SQL.get().add(sql.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT));
            return sql;
        }
    }

    @org.junit.jupiter.api.BeforeEach void recipientForHeader() { authenticate("notice-student","SINHVIEN"); }
    @AfterEach void clearSql() { SqlCapture.SQL.remove(); }

    @Test void latestFiveUseDateThenIdAndSqlLimitInsteadOfLoadingAllNotices() {
        var newest = notice("Mới nhất", "Nội dung", chief.getId(), oldDate.plusDays(1));
        var sameTime = new ArrayList<Long>();
        for (int i = 0; i < 8; i++) {
            sameTime.add(notice("Cùng thời gian " + i, "Nội dung", chief.getId(), oldDate).getId());
        }
        notice("Cũ nhất nhưng Id lớn nhất", "Nội dung", chief.getId(), oldDate.minusDays(1));
        em.clear();
        SqlCapture.SQL.get().clear();

        assertThat(service.findLatestForHeader()).extracting(ThongbaoViewDTO::id).containsExactly(
                newest.getId(), sameTime.get(7), sameTime.get(6), sameTime.get(5), sameTime.get(4));
        var noticeSql = SqlCapture.SQL.get().stream().filter(sql -> sql.contains("fetch first")).toList();
        assertThat(noticeSql).hasSize(1);
        assertThat(noticeSql.getFirst())
                .containsPattern("order by \\w+\\.ngaydang desc,\\s*\\w+\\.id desc")
                .contains("fetch first ? rows only");
        // A single batch resolves the authors; no count query or query per notice.
        assertThat(SqlCapture.SQL.get()).hasSize(3).allMatch(sql -> sql.startsWith("select "));
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 4, 5, 6})
    void repositoryAndServiceReturnAtMostFiveIncludingAnEmptyDatabase(int count) {
        for (int i = 0; i < count; i++) notice("Thông báo " + i, "Nội dung", chief.getId(), oldDate.plusMinutes(i));
        assertThat(repository.findTop5ByOrderByNgaydangDescIdDesc()).hasSize(Math.min(count, 5));
        var items = service.findLatestForHeader();
        assertThat(items).hasSize(Math.min(count, 5));
        if (count > 0) assertThat(items.getFirst().tieude()).isEqualTo("Thông báo " + (count - 1));
    }

    @Test void dtoContainsTheActualAuthorAndDateAndReadDoesNotModifyNotices() {
        var author = accounts.findByTendangnhap("notice-lecturer").orElseThrow();
        var row = notice("Lịch báo cáo", "Chi tiết", author.getId(), oldDate);
        var before = noticeSnapshot();
        assertThat(service.findLatestForHeader()).containsExactly(new ThongbaoViewDTO(
                row.getId(), row.getTieude(), row.getNoidung(), author.getHoten(), oldDate));
        assertThat(noticeSnapshot()).isEqualTo(before);
    }

    @Test void missingAndBlankAuthorNamesKeepTheExistingSafeDtoFallback() {
        chief.setHoten(" "); em.flush();
        notice("Tên trống", "Nội dung", chief.getId(), oldDate);
        notice("Không tìm thấy tác giả", "Nội dung", Long.MAX_VALUE, oldDate);
        assertThat(service.findLatestForHeader()).hasSize(2)
                .extracting(ThongbaoViewDTO::nguoidangHoten).containsOnly("Không xác định");
    }

    @ParameterizedTest @ValueSource(strings = {"TRUONGKHOA", "TRUONGBOMON", "GIANGVIEN", "SINHVIEN"})
    void allReadersReceiveHeaderDataAndCanFollowDetailAndViewAllLinks(String role) throws Exception {
        var row = notice("Thông báo chung", "Nội dung", chief.getId(), oldDate);
        authenticate("reader", role);
        if(role.equals("TRUONGKHOA")) assertThat(service.findLatestForHeader()).isEmpty();
        else assertThat(service.findLatestForHeader()).extracting(ThongbaoViewDTO::id).containsExactly(row.getId());
        var expected = service.findLatestForHeader();
        var detail=service.findById(row.getId());
        mvc.perform(get("/").with(user("reader").roles(role)))
                .andExpect(status().isOk()).andExpect(model().attribute("headerThongbaos", expected));
        mvc.perform(get("/thongbao/" + row.getId()).with(user("reader").roles(role)))
                .andExpect(status().isOk()).andExpect(model().attribute("headerThongbaos", expected))
                .andExpect(model().attribute("item", detail));
        mvc.perform(get("/thongbao").with(user("reader").roles(role)))
                .andExpect(status().isOk()).andExpect(model().attribute("headerThongbaos", expected))
                .andExpect(model().attribute("items", List.of(detail)));
    }

    @Test void authenticatedHeaderReceivesAnEmptyListWhenNoNoticesExist() throws Exception {
        mvc.perform(get("/thongbao").with(user("notice-student").roles("SINHVIEN")))
                .andExpect(status().isOk()).andExpect(model().attribute("headerThongbaos", List.of()));
    }

    @Test void listAndSearchStillReturnEveryMatchingNoticeWhileHeaderIsLimitedToFive() throws Exception {
        for (int i = 0; i < 8; i++) notice("Tìm kiếm " + i, "Nội dung", chief.getId(), oldDate);
        assertThat(service.findAll()).hasSize(8);
        assertThat(service.search("Tìm kiếm")).hasSize(8);
        var result = mvc.perform(get("/thongbao").param("keyword", "Tìm kiếm")
                        .with(user("notice-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andReturn();
        assertThat((List<?>) result.getModelAndView().getModel().get("items")).hasSize(8);
        assertThat((List<?>) result.getModelAndView().getModel().get("headerThongbaos")).hasSize(0);
    }

    @ParameterizedTest @ValueSource(strings = {"/detai", "/ketqua", "/thongbao/create",
            "/morong/timkiem/detai", "/morong/timkiem/dotdangky", "/morong/timkiem/ketqua"})
    void existingModulePagesAlsoReceiveTheSharedHeader(String path) throws Exception {
        notice("Dữ liệu chung", "Nội dung", chief.getId(), oldDate); authenticate("notice-chief","TRUONGKHOA");
        var expected = service.findLatestForHeader();
        mvc.perform(get(path).with(user("notice-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("headerThongbaos", expected));
    }

    @Test void deniedReadersCannotCallTheNewServiceOrExistingNoticeRoutes() throws Exception {
        authenticate("reader", "UNKNOWN");
        assertThatThrownBy(service::findLatestForHeader).isInstanceOf(AccessDeniedException.class);
        SecurityContextHolder.clearContext();
        assertThatThrownBy(service::findLatestForHeader).isInstanceOf(AccessDeniedException.class);
        for (String path : List.of("/", "/thongbao", "/thongbao/1")) {
            mvc.perform(get(path).with(anonymous())).andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }
        for (String path : List.of("/thongbao", "/thongbao/1")) {
            mvc.perform(get(path).with(user("reader").roles("UNKNOWN"))).andExpect(status().isForbidden());
        }
    }

    @ParameterizedTest @ValueSource(strings = {"/login", "/403", "/thongbao/9223372036854775807"})
    void pagesWithoutHeaderDoNotRunTheLatestNoticesQuery(String path) throws Exception {
        SqlCapture.SQL.get().clear();
        mvc.perform(get(path).with(user("notice-chief").roles("TRUONGKHOA")))
                .andExpect(model().attributeDoesNotExist("headerThongbaos"));
        assertThat(SqlCapture.SQL.get()).noneMatch(sql -> sql.contains("fetch first"));
    }

    @Test void successfulCrudRedirectDoesNotLoadHeaderData() throws Exception {
        SqlCapture.SQL.get().clear();
        mvc.perform(post("/thongbao").with(user("notice-chief").roles("TRUONGKHOA")).with(csrf())
                        .param("recipientRoles","SINHVIEN","GIANGVIEN","TRUONGBOMON").param("tieude", "Thông báo mới").param("noidung", "Nội dung"))
                .andExpect(status().is3xxRedirection())
                .andExpect(model().attributeDoesNotExist("headerThongbaos"));
        assertThat(SqlCapture.SQL.get()).noneMatch(sql -> sql.contains("fetch first"));
    }
}
